/*
 * SGE Demo — Tile World
 * A top-down tile world with a walking character. All tiles and sprites are
 * procedurally generated from Pixmaps (no asset files).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.tileworld

import scala.compiletime.uninitialized

import sge.{Input, Pixels, Sge, WorldUnits}
import sge.utils.Seconds
import sge.graphics.{Color, OrthographicCamera, Pixmap, Texture}
import sge.graphics.g2d.{SpriteBatch, TextureRegion}
import sge.maps.tiled.{TiledMap, TiledMapTileLayer, TiledMapTileSet}
import sge.utils.Nullable
import sge.maps.tiled.tiles.StaticTiledMapTile
import sge.maps.tiled.renderers.OrthogonalTiledMapRenderer
import sge.math.MathUtils
import sge.utils.ScreenUtils
import sge.utils.viewport.FitViewport
import demos.shared.DemoScene

/** Tile type IDs used for both the tileset and collision checks. */
object TileType {
  val Grass = 0
  val Water = 1
  val Sand  = 2
  val Stone = 3
}

class TileWorldGame extends DemoScene {

  val name: String = "Tile World"

  private val MapW     = 20
  private val MapH     = 15
  private val TileSize = 32
  private val WorldW   = MapW * TileSize.toFloat
  private val WorldH   = MapH * TileSize.toFloat
  private val CharSize = 24f
  private val Speed    = 160f
  private val LerpRate = 8f

  private var tilesetPixmap:  Pixmap                       = uninitialized
  private var tilesetTexture: Texture                      = uninitialized
  private var charPixmap:     Pixmap                       = uninitialized
  private var charTexture:    Texture                      = uninitialized
  private var tiledMap:       TiledMap                     = uninitialized
  private var renderer:       OrthogonalTiledMapRenderer   = uninitialized
  private var batch:          SpriteBatch                  = uninitialized
  private var viewport:       FitViewport                  = uninitialized
  private var camera:         OrthographicCamera           = uninitialized
  private var layer:          TiledMapTileLayer            = uninitialized

  // Character position (center of sprite)
  private var charX: Float = WorldW / 2f
  private var charY: Float = WorldH / 2f

  // Tile IDs stored for collision lookup
  private val tileIds: Array[Array[Int]] = Array.ofDim[Int](MapW, MapH)

  override def init()(using Sge): Unit = {
    // --- Build tileset Pixmap: 128×32, four 32×32 tiles ---
    tilesetPixmap = Pixmap(128, 32, Pixmap.Format.RGBA8888)

    // Tile 0: Grass — dark green base with lighter center
    tilesetPixmap.setColor(Color(0.2f, 0.6f, 0.2f, 1f))
    tilesetPixmap.fillRectangle(0, 0, TileSize, TileSize)
    tilesetPixmap.setColor(Color(0.3f, 0.7f, 0.3f, 1f))
    tilesetPixmap.fillRectangle(4, 4, TileSize - 8, TileSize - 8)

    // Tile 1: Water — blue with lighter highlight
    tilesetPixmap.setColor(Color(0.1f, 0.3f, 0.8f, 1f))
    tilesetPixmap.fillRectangle(TileSize, 0, TileSize, TileSize)
    tilesetPixmap.setColor(Color(0.2f, 0.5f, 0.9f, 1f))
    tilesetPixmap.fillRectangle(TileSize + 6, 4, TileSize - 12, TileSize - 12)

    // Tile 2: Sand — warm yellow
    tilesetPixmap.setColor(Color(0.85f, 0.75f, 0.4f, 1f))
    tilesetPixmap.fillRectangle(TileSize * 2, 0, TileSize, TileSize)
    tilesetPixmap.setColor(Color(0.9f, 0.82f, 0.5f, 1f))
    tilesetPixmap.fillRectangle(TileSize * 2 + 4, 4, TileSize - 8, TileSize - 8)

    // Tile 3: Stone — gray
    tilesetPixmap.setColor(Color(0.5f, 0.5f, 0.5f, 1f))
    tilesetPixmap.fillRectangle(TileSize * 3, 0, TileSize, TileSize)
    tilesetPixmap.setColor(Color(0.6f, 0.6f, 0.6f, 1f))
    tilesetPixmap.fillRectangle(TileSize * 3 + 4, 4, TileSize - 8, TileSize - 8)

    tilesetTexture = Texture(tilesetPixmap)

    // Create texture regions for each tile
    val regions = Array(
      TextureRegion(tilesetTexture, 0, 0, TileSize, TileSize),
      TextureRegion(tilesetTexture, TileSize, 0, TileSize, TileSize),
      TextureRegion(tilesetTexture, TileSize * 2, 0, TileSize, TileSize),
      TextureRegion(tilesetTexture, TileSize * 3, 0, TileSize, TileSize)
    )

    // Build TiledMap
    tiledMap = TiledMap()

    val tileSet = TiledMapTileSet()
    tileSet.name = "terrain"
    for (i <- 0 until 4) {
      tileSet.putTile(i, StaticTiledMapTile(regions(i)))
    }
    tiledMap.tileSets.addTileSet(tileSet)

    // Build the tile layer with a pattern
    layer = TiledMapTileLayer(MapW, MapH, TileSize, TileSize)
    layer.name = "ground"

    for (x <- 0 until MapW; y <- 0 until MapH) {
      val tileId =
        if (x == 0 || x == MapW - 1 || y == 0 || y == MapH - 1) {
          TileType.Water
        } else if ((x >= 5 && x <= 7 && y >= 3 && y <= 5) ||
                   (x >= 12 && x <= 14 && y >= 9 && y <= 11)) {
          TileType.Sand
        } else if (x == 10 && y >= 4 && y <= 10) {
          TileType.Stone
        } else if (y == 7 && x >= 3 && x <= 16) {
          TileType.Stone
        } else {
          TileType.Grass
        }

      tileIds(x)(y) = tileId
      val cell = TiledMapTileLayer.Cell()
      cell.tile = tileSet.getTile(tileId)
      layer.setCell(x, y, Nullable(cell))
    }

    tiledMap.layers.add(layer)

    // --- Character sprite: small colored square ---
    charPixmap = Pixmap(CharSize.toInt, CharSize.toInt, Pixmap.Format.RGBA8888)
    charPixmap.setColor(Color(0.9f, 0.2f, 0.2f, 1f))
    charPixmap.fill()
    // Eyes (two white dots)
    charPixmap.setColor(Color.WHITE)
    charPixmap.fillRectangle(6, 6, 4, 4)
    charPixmap.fillRectangle(14, 6, 4, 4)
    charTexture = Texture(charPixmap)

    // --- Rendering setup ---
    batch = SpriteBatch()
    renderer = OrthogonalTiledMapRenderer(tiledMap, 1f, batch, false)
    // Viewport shows half the world so the camera can scroll to follow the player
    val viewW = WorldW * 0.5f
    val viewH = WorldH * 0.5f
    camera = OrthographicCamera()
    camera.setToOrtho(false, WorldUnits(viewW), WorldUnits(viewH))
    viewport = FitViewport(WorldUnits(viewW), WorldUnits(viewH), camera)
  }

  override def render(dt: Seconds)(using Sge): Unit = {
    val delta = dt.toFloat
    handleInput(delta)
    updateCamera(delta)

    ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f)

    viewport.apply()
    camera.update()

    renderer.setView(camera)
    renderer.render()

    batch.projectionMatrix = camera.combined
    batch.rendering {
      batch.draw(
        charTexture,
        charX - CharSize / 2f,
        charY - CharSize / 2f,
        CharSize,
        CharSize
      )
    }
  }

  private def handleInput(dt: Float)(using Sge): Unit = {
    val input = Sge().input
    var dx    = 0f
    var dy    = 0f

    // Keyboard movement
    if (input.isKeyPressed(Input.Keys.LEFT) || input.isKeyPressed(Input.Keys.A)) {
      dx -= Speed * dt
    }
    if (input.isKeyPressed(Input.Keys.RIGHT) || input.isKeyPressed(Input.Keys.D)) {
      dx += Speed * dt
    }
    if (input.isKeyPressed(Input.Keys.DOWN) || input.isKeyPressed(Input.Keys.S)) {
      dy -= Speed * dt
    }
    if (input.isKeyPressed(Input.Keys.UP) || input.isKeyPressed(Input.Keys.W)) {
      dy += Speed * dt
    }

    // Touch movement — move toward touch position in world space
    if (input.touched) {
      val touchWorldX = input.x.toFloat / Sge().graphics.width.toFloat * WorldW
      val touchWorldY = (1f - input.y.toFloat / Sge().graphics.height.toFloat) * WorldH
      val diffX = touchWorldX - charX
      val diffY = touchWorldY - charY
      val dist = scala.math.sqrt((diffX * diffX + diffY * diffY).toDouble).toFloat
      if (dist > CharSize * 0.5f) {
        dx += (diffX / dist) * Speed * dt
        dy += (diffY / dist) * Speed * dt
      }
    }

    // Try X movement, then Y movement (allows sliding along walls)
    val newX = MathUtils.clamp(charX + dx, CharSize / 2f, WorldW - CharSize / 2f)
    if (!isWaterAt(newX, charY)) {
      charX = newX
    }

    val newY = MathUtils.clamp(charY + dy, CharSize / 2f, WorldH - CharSize / 2f)
    if (!isWaterAt(charX, newY)) {
      charY = newY
    }
  }

  /** Check if the given world position overlaps a water tile. */
  private def isWaterAt(wx: Float, wy: Float): Boolean = {
    val half = CharSize / 2f - 2f // slight inset for forgiving collision
    checkTile(wx - half, wy - half) || checkTile(wx + half, wy - half) ||
    checkTile(wx - half, wy + half) || checkTile(wx + half, wy + half)
  }

  private def checkTile(wx: Float, wy: Float): Boolean = {
    val tx = (wx / TileSize).toInt
    val ty = (wy / TileSize).toInt
    if (tx < 0 || tx >= MapW || ty < 0 || ty >= MapH) true
    else tileIds(tx)(ty) == TileType.Water
  }

  private def updateCamera(dt: Float): Unit = {
    val t = MathUtils.clamp(LerpRate * dt, 0f, 1f)
    camera.position.x = MathUtils.lerp(camera.position.x, charX, t)
    camera.position.y = MathUtils.lerp(camera.position.y, charY, t)

    // Clamp camera so it doesn't show beyond map edges
    val halfW = camera.viewportWidth.toFloat / 2f
    val halfH = camera.viewportHeight.toFloat / 2f
    camera.position.x = MathUtils.clamp(camera.position.x, halfW, WorldW - halfW)
    camera.position.y = MathUtils.clamp(camera.position.y, halfH, WorldH - halfH)
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = {
    viewport.update(width, height, true)
  }

  override def dispose()(using Sge): Unit = {
    renderer.close()
    charTexture.close()
    charPixmap.close()
    tilesetTexture.close()
    tilesetPixmap.close()
  }
}
