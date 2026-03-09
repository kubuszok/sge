// SGE — Desktop integration test: File I/O check
//
// Reads a known internal asset, writes to a temp local file,
// reads back, and compares.

package sge.it.desktop.checks

import sge.Sge
import sge.it.desktop.CheckResult

/** Verifies FileHandle read/write/readback cycle. */
object FileIOCheck {

  def run()(using Sge): CheckResult =
    try {
      val files = Sge().files

      // Read internal asset
      val jsonFile = files.internal("test.json")
      if (!jsonFile.exists()) {
        return CheckResult("fileio", passed = false, "test.json not found in internal resources")
      }
      val content = jsonFile.readString()
      if (!content.contains("\"sge\"")) {
        return CheckResult("fileio", passed = false, s"test.json content unexpected: ${content.take(100)}")
      }

      // Write to temp file and read back
      val tmpDir  = System.getProperty("java.io.tmpdir")
      val tmpFile = files.absolute(s"$tmpDir/sge-it-test-${System.nanoTime()}.txt")
      val testStr = "SGE integration test write/read"
      tmpFile.writeString(testStr, false)

      val readBack = tmpFile.readString()
      tmpFile.delete()

      if (readBack == testStr) {
        CheckResult("fileio", passed = true, "Internal read + temp write/readback OK")
      } else {
        CheckResult("fileio", passed = false, s"Readback mismatch: expected '$testStr', got '$readBack'")
      }
    } catch {
      case e: Exception =>
        CheckResult("fileio", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
