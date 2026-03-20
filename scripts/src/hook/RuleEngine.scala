package sgedev
package hook

import hook.BashParser.{BashExpr, Redirect}

/** Rule evaluation engine for bash commands. */
object RuleEngine {

  enum Decision {
    case Allow
    case Deny(reason: String)
    case Pass(suggestion: String)
  }

  /** Evaluate a single Simple command. */
  def evaluateCommand(cmd: BashExpr.Simple): Decision = {
    val program = normalizeProgramName(cmd.program)
    val args = cmd.args

    // Check redirects to system dirs
    val redirectDeny = cmd.redirects.find { r =>
      r.target.startsWith("/etc/") || r.target.startsWith("/usr/") ||
      r.target.startsWith("/System/") || r.target.startsWith("/Library/")
    }
    if (redirectDeny.isDefined) {
      Decision.Deny(s"Write to system directory: ${redirectDeny.get.target}")
    } else {
      // Check secret file access
      val allWords = (program :: args) ++ cmd.redirects.map(_.target)
      val secretWord = allWords.find { w =>
        val lower = w.toLowerCase
        (lower.endsWith(".env") || lower.contains("/.env") ||
         lower.endsWith(".pem") || lower.endsWith(".key") ||
         lower.contains("credentials.") || lower.contains("secret")) &&
        !program.contains("sge-dev")
      }
      if (secretWord.isDefined) {
        Decision.Deny(s"Potential secret file access: ${secretWord.get}")
      } else {
        evaluateProgram(cmd)
      }
    }
  }

  /** Normalize a program name: extract basename from full paths.
    * e.g. "/opt/homebrew/bin/rg" → "rg", "/usr/bin/python3" → "python3" */
  private def normalizeProgramName(program: String): String = {
    if (program.contains("/")) {
      program.split("/").last
    } else {
      program
    }
  }

  private def evaluateProgram(cmd: BashExpr.Simple): Decision = {
    val program = normalizeProgramName(cmd.program)
    val args = cmd.args
    program match {
      // Category 1: Trusted programs
      case "sge-dev" => Decision.Allow
      case "cargo" => Decision.Allow
      case "scala-cli" => Decision.Allow
      case "cs" | "coursier" => Decision.Allow
      case "java" if args.headOption.contains("-version") => Decision.Allow
      case "npx" | "npm" => Decision.Allow
      case "sbt" =>
        if (args.headOption.contains("--client")) {
          Decision.Allow
        } else {
          Decision.Deny("Use 'sbt --client' instead of bare 'sbt' (hangs on build.sbt errors)")
        }
      case "git" =>
        evaluateGit(args)

      // Category 2: Destructive operations
      case "rm" =>
        if (args.exists(a => a.contains("-r") || a.contains("-f"))) {
          Decision.Deny(s"Destructive: rm ${args.mkString(" ")}")
        } else {
          Decision.Deny("rm requires confirmation — use sge-dev or just recipe")
        }
      case "curl" | "wget" =>
        if (args.exists(a => a == "-X" || a == "--request" || a == "--data" || a == "-d")) {
          Decision.Deny("HTTP mutation via curl/wget — use sge-dev git gh instead")
        } else {
          Decision.Allow
        }
      case "kill" | "killall" | "pkill" =>
        Decision.Deny(s"Process kill — use 'sge-dev proc list' and 'sge-dev proc kill' for safe process management")

      // Category 4: Suboptimal tools — suggest dedicated tools
      case "grep" | "rg" | "ripgrep" =>
        Decision.Deny("Use the Grep tool instead of grep/rg")
      case "find" =>
        Decision.Deny("Use the Glob tool instead of find")
      case "cat" | "head" | "tail" | "less" | "more" =>
        Decision.Deny(s"Use the Read tool instead of $program")
      case "ls" =>
        Decision.Deny("Use the Glob tool instead of ls")
      case "echo" if cmd.redirects.exists(r => r.op == ">" || r.op == ">>") =>
        Decision.Deny("Use the Write or Edit tool instead of echo redirect")
      case "echo" | "printf" =>
        Decision.Allow
      case "sed" | "awk" | "perl" =>
        Decision.Deny(s"Use the Edit tool instead of $program")
      case "sort" | "wc" | "uniq" | "cut" | "tr" =>
        Decision.Deny(s"Use dedicated tools or 'sge-dev' instead of $program")
      case "xargs" =>
        Decision.Deny("Use a just recipe or sge-dev command instead of xargs")
      case "python" | "python3" =>
        Decision.Deny(s"Ad-hoc Python scripts not allowed — use sge-dev utilities instead")
      case "ruby" | "node" =>
        Decision.Deny(s"Scripting with $program not allowed — use sge-dev commands")

      // Category 5: GitHub CLI
      case "gh" =>
        evaluateGh(args)
      case "adb" =>
        Decision.Deny("Use 'sge-dev test android' instead of direct adb")

      // Category 7: Safe utilities
      case "cd" | "pwd" | "which" | "type" | "command" | "test" | "[" | "true" | "false" =>
        Decision.Allow
      case "mkdir" =>
        Decision.Allow
      case "env" | "printenv" =>
        Decision.Allow
      case "date" | "uname" | "arch" | "hostname" | "file" | "otool" | "ldd" | "dumpbin" =>
        Decision.Allow
      case "tar" | "unzip" | "gzip" | "gunzip" =>
        Decision.Allow
      case "mv" | "cp" | "ln" | "touch" | "chmod" =>
        Decision.Allow
      case "codesign" | "install_name_tool" =>
        Decision.Allow
      case "sleep" =>
        Decision.Allow
      case "tee" =>
        Decision.Allow

      // Shell builtins that are harmless
      case "export" | "source" | "." | "set" | "unset" | "local" | "declare" | "readonly" =>
        Decision.Allow
      case "if" | "then" | "else" | "fi" | "for" | "while" | "do" | "done" | "case" | "esac" =>
        Decision.Allow

      // Unknown — pass through (let Claude Code's own permissions handle it)
      case _ =>
        Decision.Allow
    }
  }

  private def evaluateGit(rawArgs: List[String]): Decision = {
    // Strip -C <dir> prefix (changes git working directory, doesn't affect permissions)
    val args = rawArgs match {
      case "-C" :: _ :: tail => tail
      case _ => rawArgs
    }
    val rest = args.drop(1)
    args.headOption match {
      // Pure read-only operations (no flags can make these destructive)
      case Some("status") | Some("diff") | Some("log") | Some("show") |
           Some("blame") | Some("ls-files") | Some("ls-tree") | Some("cat-file") |
           Some("rev-parse") | Some("describe") | Some("shortlog") | Some("reflog") |
           Some("name-rev") | Some("merge-base") | Some("grep") =>
        Decision.Allow

      // Read-only when querying, destructive when writing
      case Some("config") =>
        if (rest.exists(a => a == "--get" || a == "--get-all" || a == "--list" || a == "-l")) {
          Decision.Allow
        } else {
          Decision.Deny("git config overwrites settings — read with --get or --list only")
        }
      case Some("branch") =>
        if (rest.exists(a => a == "-d" || a == "-D" || a == "--delete" || a == "-m" || a == "-M" || a == "--move")) {
          Decision.Deny("git branch delete/rename overwrites data")
        } else {
          Decision.Allow
        }
      case Some("tag") =>
        if (rest.exists(a => a == "-d" || a == "--delete" || a == "-f" || a == "--force")) {
          Decision.Deny("git tag delete/force overwrites data")
        } else if (rest.isEmpty || rest.forall(a => a.startsWith("-") || a == "--list")) {
          Decision.Allow // listing tags
        } else {
          Decision.Allow // creating a tag is append-only
        }
      case Some("remote") =>
        if (rest.exists(a => a == "remove" || a == "rm" || a == "rename" || a == "set-url")) {
          Decision.Deny("git remote modification overwrites config")
        } else {
          Decision.Allow
        }
      case Some("stash") =>
        rest.headOption match {
          case None | Some("list") | Some("show") => Decision.Allow
          case Some("push") | Some("save") => Decision.Allow // append-only
          case Some("pop") | Some("apply") => Decision.Allow // restores data
          case Some("drop") | Some("clear") =>
            Decision.Deny("git stash drop/clear deletes stashed data permanently")
          case _ => Decision.Allow
        }
      case Some("symbolic-ref") =>
        if (rest.isEmpty || rest.length == 1) {
          Decision.Allow // reading: git symbolic-ref HEAD
        } else {
          Decision.Deny("git symbolic-ref with 2+ args overwrites a ref")
        }

      // Append-only safe operations
      case Some("add") =>
        Decision.Allow
      case Some("commit") =>
        if (args.contains("--amend")) {
          Decision.Deny("git commit --amend overwrites the previous commit — create a new commit instead")
        } else {
          Decision.Allow
        }
      case Some("push") =>
        if (args.contains("--force") || args.contains("-f") || args.contains("--force-with-lease")) {
          Decision.Deny("Force push overwrites remote history — use regular push")
        } else {
          Decision.Allow
        }
      case Some("fetch") | Some("pull") | Some("clone") | Some("init") =>
        Decision.Allow
      case Some("cherry-pick") | Some("merge") =>
        Decision.Allow // append-only (creates new commits)
      case Some("switch") =>
        Decision.Allow

      // Known destructive operations — deny
      case Some("reset") =>
        Decision.Deny("git reset overwrites staged state or working tree")
      case Some("checkout") =>
        if (rest.exists(a => a == "--") || rest.exists(a => a == "." || a.contains("*"))) {
          Decision.Deny("git checkout with paths overwrites working tree files")
        } else {
          Decision.Allow
        }
      case Some("restore") =>
        Decision.Deny("git restore overwrites working tree or staged files")
      case Some("clean") =>
        Decision.Deny("git clean deletes untracked files permanently")
      case Some("rebase") =>
        Decision.Deny("git rebase rewrites commit history")

      // Unknown git subcommands — ask (we don't know what they do)
      case _ =>
        Decision.Pass(s"Unknown git subcommand '${args.headOption.getOrElse("")}' — confirm with user")
    }
  }

  private def evaluateGh(args: List[String]): Decision = {
    args match {
      case "pr" :: "list" :: _ => Decision.Allow
      case "pr" :: "view" :: _ => Decision.Allow
      case "pr" :: "diff" :: _ => Decision.Allow
      case "pr" :: "checks" :: _ => Decision.Allow
      case "pr" :: "status" :: _ => Decision.Allow
      case "issue" :: "list" :: _ => Decision.Allow
      case "issue" :: "view" :: _ => Decision.Allow
      case "issue" :: "status" :: _ => Decision.Allow
      case "release" :: "list" :: _ => Decision.Allow
      case "release" :: "view" :: _ => Decision.Allow
      case "run" :: "list" :: _ => Decision.Allow
      case "run" :: "view" :: _ => Decision.Allow
      case "repo" :: "view" :: _ => Decision.Allow
      case "api" :: _ => Decision.Allow

      case "pr" :: "create" :: _ => Decision.Deny("PR creation — use 'sge-dev git gh pr create' for safety")
      case "pr" :: "merge" :: _ => Decision.Deny("PR merge — do this manually on GitHub")
      case "pr" :: "close" :: _ => Decision.Deny("PR close — do this manually on GitHub")
      case "issue" :: "create" :: _ => Decision.Deny("Issue creation — do this manually on GitHub")
      case "issue" :: "close" :: _ => Decision.Deny("Issue close — do this manually on GitHub")

      case _ => Decision.Allow
    }
  }

  /** Evaluate a full bash expression. Returns the worst decision.
    * Pipeline-aware: detects `sge-dev ... | head/tail/grep/wc` patterns
    * and gives specific guidance instead of generic tool suggestions. */
  def evaluate(expr: BashExpr): Decision = {
    // First check structural patterns (pipelines with sge-dev, unnecessary redirects)
    val structural = evaluateStructural(expr)
    if (structural.isDefined) return structural.get

    val commands = BashParser.allCommands(expr)
    if (commands.isEmpty) {
      Decision.Allow
    } else {
      var worstDeny: Option[Decision.Deny] = None
      var worstPass: Option[Decision.Pass] = None

      for (cmd <- commands) {
        evaluateCommand(cmd) match {
          case d: Decision.Deny =>
            worstDeny = Some(d)
          case p: Decision.Pass =>
            if (worstPass.isEmpty) worstPass = Some(p)
          case Decision.Allow => // ok
        }
      }

      worstDeny.orElse(worstPass).getOrElse(Decision.Allow)
    }
  }

  /** Check for structural patterns in the AST before per-command evaluation. */
  private def evaluateStructural(expr: BashExpr): Option[Decision] = {
    expr match {
      case BashExpr.Pipe(left, right) =>
        if (isSgeDevCommand(left)) {
          evaluateSgeDevPipe(left, right)
        } else {
          // Check both sides
          evaluateStructural(left).orElse(evaluateStructural(right))
        }
      case BashExpr.Chain(left, _, right) =>
        evaluateStructural(left).orElse(evaluateStructural(right))
      case cmd: BashExpr.Simple if isSgeDevCommand(cmd) =>
        evaluateSgeDevRedirects(cmd)
      case BashExpr.Sub(inner) =>
        evaluateStructural(inner)
      case _ => None
    }
  }

  private def isSgeDevCommand(expr: BashExpr): Boolean = {
    expr match {
      case BashExpr.Simple(program, _, _) =>
        val name = normalizeProgramName(program)
        name == "sge-dev"
      case _ => false
    }
  }

  /** Check if the sge-dev command supports pagination (db list commands). */
  private def sgeDevSupportsPagination(cmd: BashExpr.Simple): Boolean = {
    cmd.args match {
      case "db" :: _ :: "list" :: _ => true
      case _ => false
    }
  }

  /** Evaluate piping sge-dev output to a filter command. */
  private def evaluateSgeDevPipe(left: BashExpr, right: BashExpr): Option[Decision] = {
    right match {
      case BashExpr.Simple(prog, _, _) =>
        val name = normalizeProgramName(prog)
        val hasPagination = left match {
          case cmd: BashExpr.Simple => sgeDevSupportsPagination(cmd)
          case _ => false
        }
        name match {
          case "head" | "tail" =>
            if (hasPagination) {
              Some(Decision.Deny(
                s"Use --limit and --offset flags instead of piping to $name. " +
                "Example: sge-dev db issues list --status open --limit 20 --offset 0"
              ))
            } else {
              Some(Decision.Deny(
                s"Don't pipe sge-dev output to $name — the output is already concise. " +
                "Read it directly."
              ))
            }
          case "grep" =>
            if (hasPagination) {
              Some(Decision.Deny(
                "Use sge-dev's built-in filters instead of piping to grep. " +
                "Example: sge-dev db issues list --status open --severity critical --file <pattern>"
              ))
            } else {
              Some(Decision.Deny(
                "Don't pipe sge-dev output to grep — the output is already concise. " +
                "Read it directly or use dedicated tools for further searching."
              ))
            }
          case "wc" =>
            Some(Decision.Deny(
              "Use 'sge-dev db <table> stats' for counts instead of piping to wc."
            ))
          case "sort" | "uniq" | "cut" | "tr" | "awk" | "sed" =>
            Some(Decision.Deny(
              s"Don't pipe sge-dev output to $name — the output is already structured. " +
              "Use sge-dev's built-in flags for filtering and pagination."
            ))
          case _ => None
        }

      // Nested pipe: sge-dev | foo | bar — check first pipe segment
      case BashExpr.Pipe(innerLeft, _) =>
        evaluateSgeDevPipe(left, innerLeft)

      case _ => None
    }
  }

  /** Check for unnecessary redirects on sge-dev commands. */
  private def evaluateSgeDevRedirects(cmd: BashExpr.Simple): Option[Decision] = {
    val has2to1 = cmd.redirects.exists(r => r.op == "2>&1")
    if (has2to1) {
      Some(Decision.Deny(
        "2>&1 is unnecessary with sge-dev — all output goes to stdout."
      ))
    } else {
      None
    }
  }
}
