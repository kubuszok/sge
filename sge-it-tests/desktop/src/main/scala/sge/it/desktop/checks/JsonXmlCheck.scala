// SGE — Desktop integration test: JSON/XML parsing check
//
// Parses a JSON string via jsoniter-scala codecs and an XML string
// via XmlReader. Verifies parsed values match expected.

package sge.it.desktop.checks

import sge.Sge
import sge.utils.XmlReader
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sge.it.desktop.CheckResult

/** Verifies JSON and XML parsing. */
object JsonXmlCheck {

  final private case class TestModel(name: String, version: Int, features: List[String])
  private given JsonValueCodec[TestModel] = JsonCodecMaker.make

  def run()(using Sge): CheckResult =
    try {
      // JSON parsing via jsoniter-scala
      val jsonStr = """{"name":"sge","version":1,"features":["gl","audio","files"]}"""
      import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
      val model = readFromString[TestModel](jsonStr)

      if (model.name != "sge" || model.version != 1 || model.features.size != 3) {
        return CheckResult("json_xml", passed = false, s"JSON parse unexpected: $model")
      }

      // XML parsing via XmlReader
      val xmlStr    = """<config><width>100</width><height>100</height><title>SGE Integration Test</title></config>"""
      val xmlReader = new XmlReader()
      val root      = xmlReader.parse(xmlStr)

      if (root.name != "config") {
        return CheckResult("json_xml", passed = false, s"XML root name: ${root.name}, expected 'config'")
      }

      val widthEl = root.getChildByName("width")
      if (widthEl.isEmpty || widthEl.get.text.isEmpty || widthEl.get.text.get != "100") {
        return CheckResult("json_xml", passed = false, "XML width element missing or wrong value")
      }

      CheckResult("json_xml", passed = true, "JSON + XML parsing OK")
    } catch {
      case e: Exception =>
        CheckResult("json_xml", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
