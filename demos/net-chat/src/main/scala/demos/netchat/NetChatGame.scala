/*
 * SGE Demos — Network & serialization utilities showcase.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.netchat

import scala.compiletime.uninitialized

import sge.Input
import sge.graphics.Color
import sge.graphics.glutils.ShapeRenderer
import sge.graphics.glutils.ShapeRenderer.ShapeType
import sge.math.MathUtils
import sge.utils.Millis
import sge.utils.Seconds
import sge.utils.Nanos
import sge.utils.Nullable
import sge.utils.ScreenUtils
import sge.utils.TextFormatter
import sge.utils.TimeUtils
import sge.utils.XmlReader
import sge.utils.viewport.ScreenViewport
import demos.shared.DemoScene

/** Demonstrates XmlReader, TextFormatter, TimeUtils, and Clipboard via colored shapes. */
object NetChatGame extends DemoScene {

  override def name: String = "Net Chat"

  // Sample XML documents to cycle through
  private val XmlSamples: Array[String] = Array(
    """<config version="1.0"><window width="800" height="600"/><audio enabled="true" volume="0.8"/></config>""",
    """<level name="demo"><enemies count="5"><enemy type="slime" hp="10"/><enemy type="bat" hp="5"/></enemies></level>""",
    """<ui theme="dark"><panel id="main"><button label="Start"/><button label="Quit"/><label text="v1.0"/></panel></ui>"""
  )

  // Layout constants
  private val CardH   = 100f
  private val CardGap = 12f
  private val CardX   = 20f
  private val CardW   = 560f

  // State
  private var shapeRenderer: ShapeRenderer = uninitialized
  private var viewport: ScreenViewport     = uninitialized
  private var xmlReader: XmlReader         = uninitialized
  private var formatter: TextFormatter     = uninitialized

  private var xmlIndex: Int                     = 0
  private var parsedElementCount: Int           = 0
  private var parsedTreeDepth: Int              = 0
  private var parsedRootName: String            = ""
  private var clipboardHasContent: Boolean      = false
  private var formattedValue: Float             = 42.5f
  private var startMillis: Millis               = Millis.zero

  override def init()(using sge.Sge): Unit = {
    shapeRenderer = ShapeRenderer()
    viewport = ScreenViewport()
    xmlReader = XmlReader()
    formatter = TextFormatter(java.util.Locale.US, false)
    startMillis = TimeUtils.millis()
    parseCurrentXml()
  }

  override def render(dt: Seconds)(using sge.Sge): Unit = {
    // Input: TAB to cycle XML samples
    if (sge.Sge().input.isKeyJustPressed(Input.Keys.TAB)) {
      xmlIndex = (xmlIndex + 1) % XmlSamples.length
      parseCurrentXml()
    }

    // Input: R to re-parse current XML
    if (sge.Sge().input.isKeyJustPressed(Input.Keys.R)) {
      parseCurrentXml()
    }

    // Input: C to copy to clipboard
    if (sge.Sge().input.isKeyJustPressed(Input.Keys.C)) {
      val clip = sge.Sge().application.clipboard
      clip.contents = Nullable("SGE Demo Clipboard Test")
    }

    // Input: V to check clipboard
    if (sge.Sge().input.isKeyJustPressed(Input.Keys.V)) {
      val clip = sge.Sge().application.clipboard
      clipboardHasContent = clip.hasContents
    }

    // Time values
    val nowMillis = TimeUtils.millis()
    val elapsed = nowMillis - startMillis
    val elapsedLong = elapsed.toLong

    // Formatted number — oscillate for visual interest
    formattedValue = 50f + 45f * MathUtils.sin(elapsedLong.toFloat * 0.001f)

    // Draw
    ScreenUtils.clear(0.1f, 0.1f, 0.12f, 1f)
    viewport.apply()
    shapeRenderer.setProjectionMatrix(viewport.camera.combined)

    val screenH = sge.Sge().graphics.height.toFloat
    val baseY = screenH - 30f

    // Card 1: XML parsing results
    drawCard(baseY, 0)
    // Card 2: Time display
    drawCard(baseY, 1)
    // Card 3: Clipboard status
    drawCard(baseY, 2)
    // Card 4: Formatter output
    drawCard(baseY, 3)
  }

  override def resize(width: sge.Pixels, height: sge.Pixels)(using sge.Sge): Unit = {
    viewport.update(width, height, true)
  }

  override def dispose()(using sge.Sge): Unit = {
    shapeRenderer.close()
  }

  // --- Card rendering ---

  private def drawCard(baseY: Float, index: Int)(using sge.Sge): Unit = {
    val y = baseY - (CardH + CardGap) * (index + 1)

    // Card background
    shapeRenderer.drawing(ShapeType.Filled) {
      shapeRenderer.setColor(Color(0.18f, 0.18f, 0.22f, 1f))
      shapeRenderer.rectangle(CardX, y, CardW, CardH)

      index match {
        case 0 => drawXmlCard(y)
        case 1 => drawTimeCard(y)
        case 2 => drawClipboardCard(y)
        case 3 => drawFormatterCard(y)
        case _ => ()
      }
    }

    // Card border
    shapeRenderer.drawing(ShapeType.Line) {
      shapeRenderer.setColor(Color(0.4f, 0.4f, 0.5f, 1f))
      shapeRenderer.rectangle(CardX, y, CardW, CardH)
    }
  }

  private def drawXmlCard(y: Float): Unit = {
    // Label area — colored block for "XML" indicator
    shapeRenderer.setColor(Color(0.9f, 0.6f, 0.2f, 1f))
    shapeRenderer.rectangle(CardX + 10f, y + CardH - 25f, 40f, 15f)

    // Element count as colored blocks (one block per element)
    val blockSize = 14f
    var i = 0
    while (i < parsedElementCount && i < 20) {
      val depth = MathUtils.clamp(i.toFloat / parsedElementCount.toFloat, 0f, 1f)
      shapeRenderer.setColor(Color(0.3f + depth * 0.5f, 0.7f - depth * 0.3f, 0.9f, 1f))
      val bx = CardX + 60f + i * (blockSize + 4f)
      val by = y + CardH - 28f
      shapeRenderer.rectangle(bx, by, blockSize, blockSize)
      i += 1
    }

    // Tree depth as nested indentation bars
    var d = 0
    while (d < parsedTreeDepth) {
      val indent = d * 30f
      val barW = CardW - 80f - indent
      shapeRenderer.setColor(Color(0.25f + d * 0.12f, 0.35f + d * 0.08f, 0.5f, 0.8f))
      shapeRenderer.rectangle(CardX + 20f + indent, y + 10f + d * 14f, barW, 10f)
      d += 1
    }

    // Sample index indicator (small dots)
    var s = 0
    while (s < XmlSamples.length) {
      if (s == xmlIndex) {
        shapeRenderer.setColor(Color.WHITE)
      } else {
        shapeRenderer.setColor(Color.GRAY)
      }
      shapeRenderer.circle(CardX + CardW - 30f + s * 12f, y + CardH - 15f, 4f, 8)
      s += 1
    }
  }

  private def drawTimeCard(y: Float)(using sge.Sge): Unit = {
    // Label block for "TIME"
    shapeRenderer.setColor(Color(0.2f, 0.8f, 0.5f, 1f))
    shapeRenderer.rectangle(CardX + 10f, y + CardH - 25f, 50f, 15f)

    // Millis counter as a growing bar (wraps every 10 seconds)
    val nowMillis = TimeUtils.millis()
    val elapsed = nowMillis - startMillis
    val elapsedLong = elapsed.toLong
    val barFrac = (elapsedLong % 10000L) / 10000.0f
    val barW = (CardW - 80f) * barFrac
    shapeRenderer.setColor(Color(0.3f, 0.9f, 0.4f, 1f))
    shapeRenderer.rectangle(CardX + 70f, y + CardH - 30f, barW, 12f)

    // Nanos indicator — fast-moving small rect
    val nanoFrac = (TimeUtils.nanoTime().toLong % 1000000000L) / 1000000000.0f
    val nanoX = CardX + 70f + (CardW - 100f) * nanoFrac
    shapeRenderer.setColor(Color.YELLOW)
    shapeRenderer.rectangle(nanoX, y + 20f, 6f, 30f)

    // Seconds elapsed as tick marks
    val secs = MathUtils.clamp((elapsedLong / 1000L).toInt, 0, 30)
    var t = 0
    while (t < secs) {
      shapeRenderer.setColor(Color(0.5f, 0.7f, 0.5f, 0.6f))
      shapeRenderer.rectangle(CardX + 70f + t * 16f, y + 55f, 3f, 10f)
      t += 1
    }
  }

  private def drawClipboardCard(y: Float): Unit = {
    // Label block for "CLIP"
    shapeRenderer.setColor(Color(0.7f, 0.3f, 0.8f, 1f))
    shapeRenderer.rectangle(CardX + 10f, y + CardH - 25f, 45f, 15f)

    // Status indicator — green or red circle
    if (clipboardHasContent) {
      shapeRenderer.setColor(Color.GREEN)
    } else {
      shapeRenderer.setColor(Color.RED)
    }
    shapeRenderer.circle(CardX + 100f, y + CardH * 0.5f, 20f, 16)

    // Instruction blocks: C key and V key indicators
    shapeRenderer.setColor(Color(0.4f, 0.4f, 0.6f, 1f))
    shapeRenderer.rectangle(CardX + 160f, y + 30f, 30f, 30f) // C key
    shapeRenderer.rectangle(CardX + 210f, y + 30f, 30f, 30f) // V key

    // Arrow between them
    shapeRenderer.setColor(Color.WHITE)
    shapeRenderer.rectangle(CardX + 195f, y + 42f, 10f, 6f)
  }

  private def drawFormatterCard(y: Float): Unit = {
    // Label block for "FMT"
    shapeRenderer.setColor(Color(0.9f, 0.4f, 0.4f, 1f))
    shapeRenderer.rectangle(CardX + 10f, y + CardH - 25f, 40f, 15f)

    // Formatted value as bar width proportional to value (0-100 range)
    val barW = (CardW - 100f) * MathUtils.clamp(formattedValue / 100f, 0f, 1f)
    val hue = formattedValue / 100f
    shapeRenderer.setColor(Color(0.9f * hue, 0.3f + 0.5f * (1f - hue), 0.4f, 1f))
    shapeRenderer.rectangle(CardX + 70f, y + 35f, barW, 20f)

    // Scale markers
    shapeRenderer.setColor(Color(0.5f, 0.5f, 0.5f, 0.5f))
    var m = 0
    while (m <= 10) {
      val mx = CardX + 70f + (CardW - 100f) * (m / 10f)
      shapeRenderer.rectangle(mx, y + 30f, 1f, 30f)
      m += 1
    }

    // Use formatter to compute a display string length as block count
    val formatted = formatter.format("{0}", java.lang.Float.valueOf(formattedValue))
    val charCount = MathUtils.clamp(formatted.length, 0, 15)
    var ci = 0
    while (ci < charCount) {
      shapeRenderer.setColor(Color(0.8f, 0.5f, 0.3f, 1f))
      shapeRenderer.rectangle(CardX + 70f + ci * 10f, y + 70f, 7f, 10f)
      ci += 1
    }
  }

  // --- XML parsing ---

  private def parseCurrentXml(): Unit = {
    val root = xmlReader.parse(XmlSamples(xmlIndex))
    parsedRootName = root.name
    parsedElementCount = countElements(root)
    parsedTreeDepth = measureDepth(root)
  }

  private def countElements(elem: XmlReader.Element): Int = {
    var count = 1
    var i = 0
    while (i < elem.childCount) {
      count += countElements(elem.getChild(i))
      i += 1
    }
    count
  }

  private def measureDepth(elem: XmlReader.Element): Int = {
    var maxChildDepth = 0
    var i = 0
    while (i < elem.childCount) {
      val d = measureDepth(elem.getChild(i))
      if (d > maxChildDepth) maxChildDepth = d
      i += 1
    }
    1 + maxChildDepth
  }
}
