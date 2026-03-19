package sgedev
package hook

/** PreToolUse hook command.
  *
  * Reads JSON from stdin, parses the bash command, evaluates rules,
  * and outputs the decision as JSON on stdout.
  *
  * Usage: sge-dev hook
  *
  * Input (stdin): {"tool_name": "Bash", "tool_input": {"command": "..."}}
  * Output: JSON on stdout with hookSpecificOutput containing permissionDecision.
  *   - "allow": tool proceeds without prompt
  *   - "deny": tool is hard-blocked
  *   - "ask": user is prompted (for Pass suggestions)
  */
object HookCmd {

  def run(args: List[String]): Unit = {
    args match {
      case "--help" :: _ | "-h" :: _ =>
        println("Usage: sge-dev hook")
        println("Reads PreToolUse JSON from stdin, validates bash commands.")
        println("Outputs JSON decision on stdout (allow/deny/ask).")
      case "test" :: rest =>
        // Test mode: pass a command directly as arguments
        val command = rest.mkString(" ")
        runValidation(command)
      case _ =>
        // Read JSON from stdin
        val input = readStdin()
        val toolName = extractJsonString(input, "tool_name")
        if (toolName != "Bash") {
          // Non-Bash tools: allow
          outputDecision("allow", "")
          sys.exit(0)
        }
        val toolInput = extractJsonObject(input, "tool_input")
        val command = extractJsonString(toolInput, "command")
        if (command.isEmpty) {
          outputDecision("allow", "")
          sys.exit(0)
        }
        runValidation(command)
    }
  }

  private def runValidation(command: String): Unit = {
    val ast = BashParser.parse(command)
    val decision = RuleEngine.evaluate(ast)
    decision match {
      case RuleEngine.Decision.Allow =>
        outputDecision("allow", "")
        sys.exit(0)
      case RuleEngine.Decision.Deny(reason) =>
        outputDecision("deny", reason)
        sys.exit(0)
      case RuleEngine.Decision.Pass(suggestion) =>
        outputDecision("ask", suggestion)
        sys.exit(0)
    }
  }

  private def outputDecision(decision: String, reason: String): Unit = {
    val reasonJson = if (reason.isEmpty) "" else escapeJson(reason)
    val json =
      s"""{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"$decision","permissionDecisionReason":"$reasonJson"}}"""
    println(json)
  }

  private def escapeJson(s: String): String = {
    s.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\t", "\\t")
  }

  private def readStdin(): String = {
    val sb = new StringBuilder
    var c = System.in.read()
    while (c != -1) {
      sb.append(c.toChar)
      c = System.in.read()
    }
    sb.toString
  }

  /** Minimal JSON string extraction — no dependency needed. */
  private def extractJsonString(json: String, key: String): String = {
    val pattern = s""""$key"\\s*:\\s*"([^"\\\\]*(?:\\\\.[^"\\\\]*)*)"""".r
    pattern.findFirstMatchIn(json) match {
      case Some(m) => unescapeJson(m.group(1))
      case None => ""
    }
  }

  /** Extract a JSON object value as a raw string. */
  private def extractJsonObject(json: String, key: String): String = {
    val keyIdx = json.indexOf(s""""$key"""")
    if (keyIdx < 0) return ""
    val colonIdx = json.indexOf(':', keyIdx + key.length + 2)
    if (colonIdx < 0) return ""
    var i = colonIdx + 1
    while (i < json.length && json(i) == ' ') i += 1
    if (i >= json.length || json(i) != '{') return ""
    // Find matching brace
    var depth = 0
    var inString = false
    var escaped = false
    val start = i
    while (i < json.length) {
      val c = json(i)
      if (escaped) {
        escaped = false
      } else if (c == '\\') {
        escaped = true
      } else if (c == '"') {
        inString = !inString
      } else if (!inString) {
        if (c == '{') depth += 1
        else if (c == '}') {
          depth -= 1
          if (depth == 0) return json.substring(start, i + 1)
        }
      }
      i += 1
    }
    ""
  }

  private def unescapeJson(s: String): String = {
    s.replace("\\\"", "\"")
      .replace("\\\\", "\\")
      .replace("\\n", "\n")
      .replace("\\t", "\t")
      .replace("\\/", "/")
  }
}
