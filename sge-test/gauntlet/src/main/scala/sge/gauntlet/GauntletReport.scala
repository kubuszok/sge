/*
 * SGE Gauntlet — report.json / report.md rendering and writing.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

/** Renders and writes the gauntlet report: `report.json` (the agent interface), `report.md` (human summary). Screenshot PNGs are written by the runner as probes complete. */
object GauntletReport {

  private def jsonEscape(s: String): String = {
    val sb = new StringBuilder
    s.foreach {
      case '"'               => sb.append("\\\"")
      case '\\'              => sb.append("\\\\")
      case '\n'              => sb.append("\\n")
      case '\r'              => sb.append("\\r")
      case '\t'              => sb.append("\\t")
      case c if c.toInt < 32 => sb.append(f"\\u${c.toInt}%04x")
      case c                 => sb.append(c)
    }
    sb.toString
  }

  private def q(s: String): String = "\"" + jsonEscape(s) + "\""

  def toJson(results: List[ProbeResult], mode: String): String = {
    val probes = results
      .map { r =>
        val checks     = r.checks.map(c => s"""        {"name":${q(c.name)},"passed":${c.passed},"expected":${q(c.expected)},"actual":${q(c.actual)}}""").mkString("[\n", ",\n", "\n      ]")
        val checksJson = if (r.checks.isEmpty) "[]" else checks
        val screenshot = r.screenshotPath.fold("")(p => s"""      "screenshotPath": ${q(p)},\n""")
        val logs       = r.logLines.map(l => s"        ${q(l)}").mkString("[\n", ",\n", "\n      ]")
        val logsJson   = if (r.logLines.isEmpty) "[]" else logs
        s"""    {
           |      "id": ${q(r.id)},
           |      "area": ${q(r.area)},
           |      "status": ${q(r.status.wireName)},
           |      "checks": $checksJson,
           |      "durationMs": ${r.durationMs},
           |$screenshot      "logLines": $logsJson
           |    }""".stripMargin
      }
      .mkString(",\n")
    s"""{
       |  "mode": ${q(mode)},
       |  "hardFailures": ${ProbeResult.hardFailures(results)},
       |  "probes": [
       |$probes
       |  ]
       |}""".stripMargin
  }

  def toMarkdown(results: List[ProbeResult], mode: String): String = {
    val counts = ProbeStatus.values.toList.map(s => s -> results.count(_.status == s)).filter(_._2 > 0).map { case (s, n) => s"${s.wireName}: $n" }.mkString(", ")
    val rows   = results
      .map { r =>
        val passedChecks = r.checks.count(_.passed)
        val failedNames  = r.checks.filterNot(_.passed).map(_.name).mkString(", ")
        val detail       =
          if (r.status == ProbeStatus.SkippedGpu) "requires GPU — skipped in headless mode"
          else if (failedNames.isEmpty) ""
          else s"failed: $failedNames"
        s"| ${r.id} | ${r.area} | ${r.status.wireName} | $passedChecks/${r.checks.size} | ${r.durationMs} | $detail |"
      }
      .mkString("\n")
    // NOTE: built without stripMargin — the markdown table's leading `|` would be eaten by it.
    "# SGE Gauntlet report\n\n" +
      s"Mode: `$mode` — probes: ${results.size} — $counts — hard failures: ${ProbeResult.hardFailures(results)}\n\n" +
      "| probe | area | status | checks passed | ms | detail |\n" +
      "|-------|------|--------|---------------|----|--------|\n" +
      rows + "\n\n" +
      "Statuses: `passed`, `failed` (hard), `skipped_gpu` (headless run, GPU probe not executed — NOT a pass),\n" +
      "`known_fail` (fails while its cited issue is open), `unexpected_pass` (hard: cited issue seems fixed —\n" +
      "remove the probe's knownIssue annotation with the fix).\n"
  }

  /** Writes report.json and report.md into `reportDir` (absolute path). */
  def write(results: List[ProbeResult], mode: String, reportDir: String)(using sge: Sge): Unit = {
    val dir = sge.files.absolute(reportDir)
    dir.mkdirs()
    dir.child("report.json").writeString(toJson(results, mode), false)
    dir.child("report.md").writeString(toMarkdown(results, mode), false)
  }
}
