package sgedev
package git

/** Git and GitHub CLI operations — self-contained, no Justfile dependency. */
object GitCmd {

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        printUsage()
      // Read-only git
      case "status" :: rest => gitPass("status" :: rest)
      case "diff" :: rest => gitDiff(rest)
      case "diff-staged" :: rest => gitPass("diff" :: "--cached" :: rest)
      case "diff-files" :: _ => gitPass(List("diff", "--name-only"))
      case "diff-files-staged" :: _ => gitPass(List("diff", "--cached", "--name-only"))
      case "diff-stat" :: rest => gitPass("diff" :: "--stat" :: rest)
      case "diff-count" :: _ => diffCount()
      case "log" :: rest => gitLog(rest)
      case "log-full" :: rest => gitLogFull(rest)
      case "show" :: rest => gitShow(rest)
      case "show-stat" :: rest => gitPass("show" :: "--stat" :: rest)
      case "branch" :: rest => gitPass("branch" :: rest)
      case "branches" :: _ => gitPass(List("branch", "-v"))
      case "branches-all" :: _ => gitPass(List("branch", "-av"))
      case "blame" :: rest => gitPass("blame" :: rest)
      case "ls" :: rest => gitLs(rest)
      case "tags" :: _ => gitPass(List("tag", "-l"))
      case "remotes" :: _ => gitPass(List("remote", "-v"))
      case "stash-list" :: _ => gitPass(List("stash", "list"))
      case "rev" :: rest =>
        val ref = if (rest.isEmpty) List("HEAD") else rest
        gitPass("rev-parse" :: ref)
      // Write operations
      case "stage" :: rest => gitPass("add" :: rest)
      case "stage-all" :: _ => gitPass(List("add", "-A"))
      case "commit" :: rest => gitCommit(rest)
      case "commit-all" :: rest => gitCommitAll(rest)
      case "push" :: rest => gitPush(rest)
      // GitHub
      case "gh" :: rest => ghCmd(rest)
      case other :: _ =>
        Term.err(s"Unknown git command: $other")
        printUsage()
        sys.exit(1)
    }
  }

  private def printUsage(): Unit = {
    println("""Usage: sge-dev git <command>
              |
              |Read-only:
              |  status, diff [file], diff-staged [file], diff-files, diff-files-staged
              |  diff-stat, diff-count, log [-n N], log-full [-n N]
              |  show [ref], show-stat [ref], branch, branches, branches-all
              |  blame <file>, ls [pattern], tags, remotes, stash-list, rev [ref]
              |
              |Write:
              |  stage <files...>, stage-all, commit <message>,
              |  commit-all <message>, push
              |
              |GitHub:
              |  gh pr {list,view,diff,checks,comments,comments-summary,comments-file,issue-comments,reviews,check-runs} [args]
              |  gh issue {list,view} [args]
              |  gh release {list,view} [args]
              |  gh run {list,view,log} [args]
              |  gh repo, gh api <endpoint>""".stripMargin)
  }

  private def gitPass(args: List[String]): Unit = {
    val code = Proc.exec("git", args, cwd = Some(Paths.projectRoot))
    if (code != 0) sys.exit(code)
  }

  private def gitDiff(args: List[String]): Unit = {
    val effectiveArgs = if (args.isEmpty) List("--stat") else args
    gitPass("diff" :: effectiveArgs)
  }

  private def gitLog(args: List[String]): Unit = {
    val effectiveArgs = if (args.isEmpty) { List("--oneline", "-20") } else { args }
    gitPass("log" :: effectiveArgs)
  }

  private def gitLogFull(args: List[String]): Unit = {
    val effectiveArgs = if (args.isEmpty) { List("--stat", "-5") } else { "--stat" :: args }
    gitPass("log" :: effectiveArgs)
  }

  private def gitShow(args: List[String]): Unit = {
    val effectiveArgs = if (args.isEmpty) List("HEAD") else args
    gitPass("show" :: effectiveArgs)
  }

  private def gitLs(args: List[String]): Unit = {
    if (args.isEmpty) {
      gitPass(List("ls-files"))
    } else {
      gitPass("ls-files" :: args)
    }
  }

  private def diffCount(): Unit = {
    val staged = Proc.run("git", List("diff", "--cached", "--name-only"), cwd = Some(Paths.projectRoot))
    val unstaged = Proc.run("git", List("diff", "--name-only"), cwd = Some(Paths.projectRoot))
    val untracked = Proc.run("git", List("ls-files", "--others", "--exclude-standard"), cwd = Some(Paths.projectRoot))
    val stagedCount = if (staged.ok) staged.stdout.linesIterator.count(_.nonEmpty) else 0
    val unstagedCount = if (unstaged.ok) unstaged.stdout.linesIterator.count(_.nonEmpty) else 0
    val untrackedCount = if (untracked.ok) untracked.stdout.linesIterator.count(_.nonEmpty) else 0
    println(s"Staged:    $stagedCount")
    println(s"Unstaged:  $unstagedCount")
    println(s"Untracked: $untrackedCount")
  }

  private def gitCommit(args: List[String]): Unit = {
    if (args.isEmpty) {
      Term.err("Commit message required: sge-dev git commit <message>")
      sys.exit(1)
    }
    val message = args.mkString(" ")
    gitPass(List("commit", "-m", message))
  }

  private def gitCommitAll(args: List[String]): Unit = {
    if (args.isEmpty) {
      Term.err("Commit message required: sge-dev git commit-all <message>")
      sys.exit(1)
    }
    val message = args.mkString(" ")
    gitPass(List("add", "-A"))
    gitPass(List("commit", "-m", message))
  }

  private def gitPush(args: List[String]): Unit = {
    // Show what will be pushed first
    val branch = Proc.run("git", List("branch", "--show-current"), cwd = Some(Paths.projectRoot))
    val branchName = if (branch.ok) branch.stdout.trim else "HEAD"
    val result = Proc.run("git", List("log", "--oneline", "@{upstream}..HEAD"),
                          cwd = Some(Paths.projectRoot))
    if (result.ok && result.stdout.nonEmpty) {
      println("Commits to push:")
      println(result.stdout)
      println()
    }
    gitPass("push" :: "origin" :: branchName :: args)
  }

  private def ghCmd(args: List[String]): Unit = {
    args match {
      case Nil =>
        println("Usage: sge-dev git gh {pr,issue,release,run,repo,api} ...")
      case "pr" :: rest => ghPr(rest)
      case "issue" :: rest => ghPass("issue" :: rest)
      case "release" :: rest => ghPass("release" :: rest)
      case "run" :: rest => ghRun(rest)
      case "repo" :: rest =>
        val effectiveRest = if (rest.isEmpty) List("view") else rest
        ghPass("repo" :: effectiveRest)
      case "api" :: rest => ghPass("api" :: rest)
      case other :: _ =>
        Term.err(s"Unknown gh command: $other")
        sys.exit(1)
    }
  }

  private def ghPr(args: List[String]): Unit = {
    args match {
      case "comments" :: rest =>
        val pr = rest.headOption.getOrElse("2")
        ghPass(List("api", s"repos/{{owner}}/{{repo}}/pulls/$pr/comments",
          "--jq", """.[] | "---\n\(.path):\(.line // .original_line) (@\(.user.login)):\n\(.body)\n""""))
      case "comments-summary" :: rest =>
        val pr = rest.headOption.getOrElse("2")
        ghPass(List("api", s"repos/{{owner}}/{{repo}}/pulls/$pr/comments",
          "--jq", """.[] | "\(.path):\(.line // .original_line) | \(.body | split("### ")[1:] | .[0] | split("\n")[0])""""))
      case "comments-file" :: rest =>
        if (rest.length < 2) { Term.err("Usage: gh pr comments-file <pr> <file>"); sys.exit(1) }
        val pr = rest(0)
        val file = rest(1)
        ghPass(List("api", s"repos/{{owner}}/{{repo}}/pulls/$pr/comments",
          "--jq", s"""[.[] | select(.path == "$file")] | .[] | "---\\nL\\(.line // .original_line) (@\\(.user.login)):\\n\\(.body)\\n""""))
      case "issue-comments" :: rest =>
        val pr = rest.headOption.getOrElse("2")
        ghPass(List("api", s"repos/{{owner}}/{{repo}}/issues/$pr/comments",
          "--jq", """.[] | "---\n@\(.user.login):\n\(.body)\n""""))
      case "reviews" :: rest =>
        val pr = rest.headOption.getOrElse("2")
        ghPass(List("api", s"repos/{{owner}}/{{repo}}/pulls/$pr/reviews",
          "--jq", """.[] | "---\n@\(.user.login) [\(.state)]:\n\(.body)\n""""))
      case "check-runs" :: rest =>
        val pr = rest.headOption.getOrElse("2")
        val headRef = Proc.run("gh", List("pr", "view", pr, "--json", "headRefOid", "--jq", ".headRefOid"),
          cwd = Some(Paths.projectRoot))
        if (headRef.ok) {
          ghPass(List("api", s"repos/{{owner}}/{{repo}}/commits/${headRef.stdout.trim}/check-runs",
            "--jq", """.check_runs[] | "\(.name): \(.conclusion // .status)""""))
        }
      case _ => ghPass("pr" :: args)
    }
  }

  private def ghRun(args: List[String]): Unit = {
    args match {
      case "log" :: rest =>
        val id = rest.headOption.getOrElse { Term.err("Run ID required"); sys.exit(1); "" }
        ghPass(List("run", "view", id, "--log-failed"))
      case _ => ghPass("run" :: args)
    }
  }

  private def ghPass(args: List[String]): Unit = {
    val code = Proc.exec("gh", args, cwd = Some(Paths.projectRoot))
    if (code != 0) sys.exit(code)
  }
}
