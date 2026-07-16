/*
 * SGE Gauntlet — utils: lenient JSON and XML reader round-trip.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.utils.{ Json, LenientJson, XmlReader }

import scala.collection.mutable.ListBuffer

/** Parses a lenient-JSON document (unquoted names/values, the libGDX JsonReader grammar) and an XML document and asserts the resulting trees. */
object JsonXmlProbe extends FeatureProbe {

  override def id: String = "utils/json-xml"

  override def area: String = "utils"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  /** Field lookup on a parsed Json object (mirrors Skin.getField). */
  private def field(json: Json, name: String): Option[Json] = json match {
    case Json.Obj(obj) =>
      var result: Option[Json] = None
      obj.fields.foreach { case (k, v) => if (k == name) result = Some(v) }
      result
    case _ => None
  }

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    // ── lenient JSON (libGDX JsonReader grammar: unquoted names and values) ──
    val json = LenientJson.parse(
      """{
        |  name: gauntlet,
        |  count: 3,
        |  enabled: true,
        |  tags: [ alpha, beta ],
        |  nested: { pi: 3.5 }
        |}""".stripMargin
    )
    checks += Check.cond("json-root-object", json match { case _: Json.Obj => true; case _ => false }, "Json.Obj root", json.getClass.getSimpleName)
    checks += Check.eq(
      "json-string-field",
      "gauntlet",
      field(json, "name") match {
        case Some(Json.Str(s)) => s
        case other             => s"<$other>"
      }
    )
    checks += Check.eq(
      "json-number-field",
      3.0d,
      field(json, "count") match {
        case Some(Json.Num(n)) => n.toDouble.getOrElse(Double.NaN)
        case _                 => Double.NaN
      }
    )
    checks += Check.eq(
      "json-boolean-field",
      true,
      field(json, "enabled") match {
        case Some(Json.Bool(b)) => b
        case _                  => false
      }
    )
    checks += Check.eq(
      "json-array-elements",
      List("alpha", "beta"),
      field(json, "tags") match {
        case Some(Json.Arr(items)) =>
          val strings = ListBuffer.empty[String]
          items.foreach {
            case Json.Str(s) => strings += s
            case _           => ()
          }
          strings.toList
        case _ => Nil
      }
    )
    checks += Check.eq(
      "json-nested-number",
      3.5d,
      field(json, "nested").flatMap(nested => field(nested, "pi")) match {
        case Some(Json.Num(n)) => n.toDouble.getOrElse(Double.NaN)
        case _                 => Double.NaN
      }
    )

    // ── XML reader ──────────────────────────────────────────────────────
    val root = new XmlReader().parse(
      """<map version="1.9" orientation="orthogonal">
        |  <layer name="ground" width="4" height="2">
        |    <tile id="7"/>
        |    <tile id="9"/>
        |  </layer>
        |</map>""".stripMargin
    )
    checks += Check.eq("xml-root-attribute", "1.9", root.getAttribute("version"))
    val layer = root.getChildByName("layer")
    if (layer.isDefined) {
      layer.foreach { l =>
        checks += Check.eq("xml-child-attribute", "ground", l.getAttribute("name"))
        checks += Check.eq("xml-child-count", 2, l.childCount)
        checks += Check.eq("xml-grandchild-attribute", "9", l.getChild(1).getAttribute("id"))
      }
    } else {
      checks += Check.cond("xml-child-attribute", passed = false, "layer element present", "getChildByName(layer) empty")
    }
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
