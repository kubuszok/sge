/*
 * SGE Demos — Hex grid tactics mini-game using ShapeRenderer.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package hextactics

import scala.compiletime.uninitialized

import _root_.sge.{Pixels, Sge}
import _root_.sge.graphics.Color
import _root_.sge.graphics.glutils.ShapeRenderer
import _root_.sge.graphics.glutils.ShapeRenderer.ShapeType
import _root_.sge.math.CumulativeDistribution
import _root_.sge.math.MathUtils
import _root_.sge.math.Vector2
import _root_.sge.math.Vector3
import _root_.sge.utils.ArrayMap
import _root_.sge.utils.DynamicArray
import _root_.sge.utils.Nullable
import _root_.sge.utils.ObjectMap
import _root_.sge.utils.OrderedMap
import _root_.sge.utils.ScreenUtils
import _root_.sge.utils.viewport.FitViewport
import sge.demos.shared.DemoScene

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
      val touch = Vector3(Sge().input.getX().toFloat, Sge().input.getY().toFloat, 0f)
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
    shapeRenderer.begin(ShapeType.Filled)
    var c = 0
    while (c < Cols) {
      var r = 0
      while (r < Rows) {
        val center = getHexCenter(c, r)
        val key = c * 100 + r
        val t: Nullable[String] = terrain.get(key)
        val terrainType = t.getOrElse("plains")
        shapeRenderer.setColor(terrainColor(terrainType))
        drawHexFilled(center.x, center.y)

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
    shapeRenderer.end()

    // Hex outlines + selection highlight
    shapeRenderer.begin(ShapeType.Line)
    c = 0
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
    shapeRenderer.end()
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = {
    viewport.update(width, height)
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

  private def terrainColor(t: String): Color = t match {
    case "plains"   => Color(0.5f, 0.8f, 0.3f, 1f)
    case "forest"   => Color(0.2f, 0.5f, 0.15f, 1f)
    case "mountain" => Color(0.6f, 0.6f, 0.6f, 1f)
    case "water"    => Color(0.2f, 0.4f, 0.85f, 1f)
    case _          => Color(0.5f, 0.8f, 0.3f, 1f)
  }

  private def generateMap(): Unit = {
    terrain = ArrayMap[Int, String]()
    val dist = CumulativeDistribution[String]()
    dist.add("plains", 40f)
    dist.add("forest", 25f)
    dist.add("mountain", 15f)
    dist.add("water", 20f)
    dist.generate()

    var c = 0
    while (c < Cols) {
      var r = 0
      while (r < Rows) {
        val key = c * 100 + r
        terrain.put(key, dist.value())
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
