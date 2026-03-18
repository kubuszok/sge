/*
 * SGE Demos — Hex grid tactics mini-game using ShapeRenderer.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.hextactics

import scala.compiletime.uninitialized

import sge.{Input, Pixels, Sge}
import sge.graphics.Color
import sge.graphics.glutils.ShapeRenderer
import sge.graphics.glutils.ShapeRenderer.ShapeType
import sge.math.MathUtils
import sge.math.Vector2
import sge.math.Vector3
import sge.utils.ArrayMap
import sge.utils.DynamicArray
import sge.utils.Nullable
import sge.utils.ObjectMap
import sge.utils.OrderedMap
import sge.utils.ScreenUtils
import sge.utils.viewport.FitViewport
import demos.shared.DemoScene

/** Hex grid tactics demo: two teams, terrain, unit movement. */
object HexTacticsGame extends DemoScene {

  override def name: String = "Hex Tactics"

  // Grid size
  private val Cols = 8
  private val Rows = 6
  private val HexSize = 32f
  private val HexW = HexSize * 2f
  private val HexH = HexSize * scala.math.sqrt(3.0).toFloat

  // World dimensions — enough to fit the grid with margin
  private val W = (Cols * HexW * 0.75f) + HexW + 40f
  private val H = (Rows * HexH) + HexH + 40f

  // Unit data
  final case class HexUnit(var col: Int, var row: Int, var hp: Int, var team: Int)

  // State
  private var shapeRenderer: ShapeRenderer = uninitialized
  private var viewport: FitViewport = uninitialized

  private var terrain: ArrayMap[Int, String] = uninitialized
  private var units: OrderedMap[String, HexUnit] = uninitialized
  private var hexCenters: ObjectMap[Int, Vector2] = uninitialized

  private var selectedHex: Int = -1       // encoded col*100+row, -1 = none
  private var selectedUnit: String = ""   // empty = none
  private var currentTurn: Int = 0        // index into orderedKeys

  override def init()(using Sge): Unit = {
    shapeRenderer = ShapeRenderer()
    viewport = FitViewport(W, H)
    terrain = ArrayMap[Int, String]()
    units = OrderedMap[String, HexUnit]()
    hexCenters = ObjectMap[Int, Vector2]()
    generateMap()
    placeUnits()
    cacheHexCenters()
    currentTurn = 0
  }

  override def render(dt: Float)(using Sge): Unit = {
    // Input: R to regenerate
    if (Sge().input.isKeyJustPressed(Input.Keys.R)) {
      generateMap()
      placeUnits()
      cacheHexCenters()
      selectedHex = -1
      selectedUnit = ""
      currentTurn = 0
    }

    // Input: TAB to advance turn
    if (Sge().input.isKeyJustPressed(Input.Keys.TAB)) {
      val keys = units.orderedKeys
      if (keys.size > 0) {
        currentTurn = (currentTurn + 1) % keys.size
      }
      selectedHex = -1
      selectedUnit = ""
    }

    // Input: click to select/move
    if (Sge().input.justTouched()) {
      val touch = Vector3(Sge().input.x.toFloat, Sge().input.y.toFloat, 0f)
      viewport.camera.unproject(touch)
      val hex = pixelToHex(touch.x, touch.y)
      val clickKey = hex._1 * 100 + hex._2
      if (hex._1 >= 0 && hex._1 < Cols && hex._2 >= 0 && hex._2 < Rows) {
        val clickedUnitName = findUnitAt(hex._1, hex._2)
        if (selectedUnit.nonEmpty && clickedUnitName.isEmpty) {
          // Try to move selected unit
          val su: Nullable[HexUnit] = units.get(selectedUnit)
          if (su.isDefined) {
            val u = su.get
            if (isAdjacent(u.col, u.row, hex._1, hex._2) && isPassable(hex._1, hex._2)) {
              u.col = hex._1
              u.row = hex._2
              // Advance turn after move
              val keys = units.orderedKeys
              if (keys.size > 0) {
                currentTurn = (currentTurn + 1) % keys.size
              }
            }
          }
          selectedUnit = ""
          selectedHex = -1
        } else if (clickedUnitName.nonEmpty) {
          selectedUnit = clickedUnitName
          selectedHex = clickKey
        } else {
          selectedHex = clickKey
          selectedUnit = ""
        }
      }
    }

    // Draw
    ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f)
    viewport.apply()
    shapeRenderer.setProjectionMatrix(viewport.camera.combined)

    // Filled hexagons
    shapeRenderer.drawing(ShapeType.Filled) {
      var c = 0
      while (c < Cols) {
        var r = 0
        while (r < Rows) {
          val center = getHexCenter(c, r)
          val key = c * 100 + r
          val t: Nullable[String] = terrain.get(key)
          val terrainType = t.getOrElse("plains")
          setTerrainColor(terrainType)
          drawHexFilled(center.x, center.y)

          // Terrain markers for visual distinction
          terrainType match {
            case "forest" =>
              // Tree: dark green triangle
              shapeRenderer.setColor(ForestMarker)
              shapeRenderer.triangle(
                center.x, center.y + HexSize * 0.35f,
                center.x - HexSize * 0.2f, center.y - HexSize * 0.1f,
                center.x + HexSize * 0.2f, center.y - HexSize * 0.1f
              )
            case "mountain" =>
              // Peak: dark brown triangle with lighter tip
              shapeRenderer.setColor(MtPeak)
              shapeRenderer.triangle(
                center.x, center.y + HexSize * 0.4f,
                center.x - HexSize * 0.25f, center.y - HexSize * 0.15f,
                center.x + HexSize * 0.25f, center.y - HexSize * 0.15f
              )
              shapeRenderer.setColor(MtTip)
              shapeRenderer.triangle(
                center.x, center.y + HexSize * 0.4f,
                center.x - HexSize * 0.1f, center.y + HexSize * 0.2f,
                center.x + HexSize * 0.1f, center.y + HexSize * 0.2f
              )
            case "water" =>
              // Wave lines (two small rectangles)
              shapeRenderer.setColor(WaveCol)
              shapeRenderer.rectangle(center.x - HexSize * 0.3f, center.y + 2f, HexSize * 0.3f, 2f)
              shapeRenderer.rectangle(center.x, center.y - 4f, HexSize * 0.3f, 2f)
            case _ => () // plains: no marker
          }

          // HP bar for unit at this hex
          val uName = findUnitAt(c, r)
          if (uName.nonEmpty) {
            val nu: Nullable[HexUnit] = units.get(uName)
            if (nu.isDefined) {
              val u = nu.get
              val teamColor = if (u.team == 0) Color.RED else Color.BLUE
              // Draw unit marker (small filled circle area via triangles)
              shapeRenderer.setColor(teamColor)
              shapeRenderer.circle(center.x, center.y, HexSize * 0.35f, 12)
              // HP bar
              shapeRenderer.setColor(Color.GREEN)
              val barW = HexSize * 0.8f * (u.hp / 100f)
              shapeRenderer.rectangle(center.x - HexSize * 0.4f, center.y - HexSize * 0.6f, barW, 4f)
            }
          }
          r += 1
        }
        c += 1
      }

      // Turn indicator bar at top
      val keys = units.orderedKeys
      if (keys.size > 0) {
        val turnName = keys(currentTurn % keys.size)
        val tu: Nullable[HexUnit] = units.get(turnName)
        if (tu.isDefined) {
          val teamColor = if (tu.get.team == 0) Color.RED else Color.BLUE
          shapeRenderer.setColor(teamColor)
          shapeRenderer.rectangle(10f, H - 20f, 120f, 14f)
        }
      }
    }

    // Hex outlines + selection highlight
    shapeRenderer.drawing(ShapeType.Line) {
      var c = 0
      while (c < Cols) {
        var r = 0
        while (r < Rows) {
          val center = getHexCenter(c, r)
          val key = c * 100 + r
          if (key == selectedHex) {
            shapeRenderer.setColor(Color.YELLOW)
          } else {
            shapeRenderer.setColor(Color.DARK_GRAY)
          }
          drawHexOutline(center.x, center.y)
          r += 1
        }
        c += 1
      }
    }
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = {
    viewport.update(width, height, true)
  }

  override def dispose()(using Sge): Unit = {
    shapeRenderer.close()
  }

  // --- Hex coordinate helpers ---

  private def getHexCenter(col: Int, row: Int): Vector2 = {
    val key = col * 100 + row
    val cached: Nullable[Vector2] = hexCenters.get(key)
    if (cached.isDefined) cached.get
    else {
      val v = computeHexCenter(col, row)
      hexCenters.put(key, v)
      v
    }
  }

  private def computeHexCenter(col: Int, row: Int): Vector2 = {
    val x = 40f + col * HexW * 0.75f + HexSize
    val yOffset = if (col % 2 == 0) 0f else HexH * 0.5f
    val y = 40f + row * HexH + HexH * 0.5f + yOffset
    Vector2(x, y)
  }

  private def pixelToHex(px: Float, py: Float): (Int, Int) = {
    // Brute-force nearest hex center
    var bestCol = -1
    var bestRow = -1
    var bestDist = Float.MaxValue
    var c = 0
    while (c < Cols) {
      var r = 0
      while (r < Rows) {
        val center = getHexCenter(c, r)
        val dx = px - center.x
        val dy = py - center.y
        val dist = dx * dx + dy * dy
        if (dist < bestDist) {
          bestDist = dist
          bestCol = c
          bestRow = r
        }
        r += 1
      }
      c += 1
    }
    // Only accept if within hex radius
    if (bestDist < HexSize * HexSize) (bestCol, bestRow)
    else (-1, -1)
  }

  private def isAdjacent(c1: Int, r1: Int, c2: Int, r2: Int): Boolean = {
    val center1 = getHexCenter(c1, r1)
    val center2 = getHexCenter(c2, r2)
    val dist = center1.distance(center2)
    dist < HexH * 1.2f && !(c1 == c2 && r1 == r2)
  }

  private def isPassable(col: Int, row: Int): Boolean = {
    val key = col * 100 + row
    val t: Nullable[String] = terrain.get(key)
    val terrainType = t.getOrElse("plains")
    terrainType != "water" && terrainType != "mountain"
  }

  // --- Drawing helpers ---

  private def hexCorner(cx: Float, cy: Float, i: Int): (Float, Float) = {
    val angle = MathUtils.PI / 3f * i
    (cx + HexSize * MathUtils.cos(angle), cy + HexSize * MathUtils.sin(angle))
  }

  private def drawHexFilled(cx: Float, cy: Float): Unit = {
    var i = 0
    while (i < 6) {
      val (x1, y1) = hexCorner(cx, cy, i)
      val (x2, y2) = hexCorner(cx, cy, (i + 1) % 6)
      shapeRenderer.triangle(cx, cy, x1, y1, x2, y2)
      i += 1
    }
  }

  private def drawHexOutline(cx: Float, cy: Float): Unit = {
    var i = 0
    while (i < 6) {
      val (x1, y1) = hexCorner(cx, cy, i)
      val (x2, y2) = hexCorner(cx, cy, (i + 1) % 6)
      shapeRenderer.line(x1, y1, x2, y2)
      i += 1
    }
  }

  // --- Terrain and units ---

  // Pre-allocated terrain colors — must use setColor(Color) overload, not setColor(r,g,b,a)
  private val PlainsCol   = Color(0.55f, 0.82f, 0.35f, 1f)
  private val ForestCol   = Color(0.18f, 0.45f, 0.12f, 1f)
  private val MountainCol = Color(0.55f, 0.50f, 0.45f, 1f)
  private val WaterCol    = Color(0.15f, 0.35f, 0.80f, 1f)
  private val ForestMarker   = Color(0.1f, 0.35f, 0.1f, 1f)
  private val MtPeak         = Color(0.35f, 0.30f, 0.25f, 1f)
  private val MtTip          = Color(0.75f, 0.70f, 0.65f, 1f)
  private val WaveCol        = Color(0.4f, 0.6f, 0.95f, 1f)

  private def setTerrainColor(t: String): Unit = t match {
    case "plains"   => shapeRenderer.setColor(PlainsCol)
    case "forest"   => shapeRenderer.setColor(ForestCol)
    case "mountain" => shapeRenderer.setColor(MountainCol)
    case "water"    => shapeRenderer.setColor(WaterCol)
    case _          => shapeRenderer.setColor(PlainsCol)
  }

  private def generateMap(): Unit = {
    terrain = ArrayMap[Int, String]()

    var c = 0
    while (c < Cols) {
      var r = 0
      while (r < Rows) {
        val key = c * 100 + r
        val roll = MathUtils.random()
        val t =
          if (roll < 0.40f) "plains"
          else if (roll < 0.65f) "forest"
          else if (roll < 0.80f) "mountain"
          else "water"
        terrain.put(key, t)
        r += 1
      }
      c += 1
    }
  }

  private def placeUnits(): Unit = {
    units = OrderedMap[String, HexUnit]()
    // Red team on left side
    units.put("Red Scout", HexUnit(0, 1, 100, 0))
    units.put("Red Knight", HexUnit(0, 4, 100, 0))
    // Blue team on right side
    units.put("Blue Scout", HexUnit(Cols - 1, 1, 100, 1))
    units.put("Blue Knight", HexUnit(Cols - 1, 4, 100, 1))
    // Ensure unit hexes are passable plains
    units.foreachEntry { (_, u) =>
      terrain.put(u.col * 100 + u.row, "plains")
    }
  }

  private def cacheHexCenters(): Unit = {
    hexCenters = ObjectMap[Int, Vector2]()
    var c = 0
    while (c < Cols) {
      var r = 0
      while (r < Rows) {
        val key = c * 100 + r
        hexCenters.put(key, computeHexCenter(c, r))
        r += 1
      }
      c += 1
    }
  }

  private def findUnitAt(col: Int, row: Int): String = {
    var found = ""
    units.foreachEntry { (name, u) =>
      if (u.col == col && u.row == row) {
        found = name
      }
    }
    found
  }
}
