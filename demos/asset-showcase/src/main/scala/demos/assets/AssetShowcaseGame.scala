/*
 * SGE Demos — asset loading showcase with 2D textures, 3D models, and audio.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.assets

import scala.compiletime.uninitialized
import sge.{Input, Pixels, Sge}
import sge.assets.AssetManager
import sge.assets.loaders.FileHandleResolver
import sge.audio.{Sound, SoundId, Volume}
import sge.graphics.{ClearMask, Color, CompareFunc, CullFace, EnableCap, PerspectiveCamera, StencilOp, Texture}
import sge.graphics.g3d.attributes.IntAttribute
import sge.graphics.g3d.{Environment, Model, ModelBatch, ModelInstance}
import sge.graphics.g3d.environment.DirectionalLight
import sge.graphics.g2d.SpriteBatch
import sge.graphics.glutils.ShapeRenderer
import sge.graphics.glutils.ShapeRenderer.ShapeType
import sge.utils.{Nullable, ScreenUtils}
import demos.shared.DemoScene

/** Asset loading showcase: demonstrates AssetManager loading of 2D textures,
  * 3D models (.g3dj JSON + .g3db UBJSON), and audio (Music + Sound) with interactive controls.
  *
  * Controls:
  *   - Loading phase: assets load via AssetManager with progress bar
  *   - Tab: cycle between sections (2D Textures / 3D Model / Audio)
  *   - Audio section: Up/Down adjust music volume, Space toggles music play/pause,
  *     Enter plays click sound effect
  */
object AssetShowcaseGame extends DemoScene {

  override def name: String = "Asset Showcase"

  // ── State machine ──────────────────────────────────────────────────
  private val PhaseLoading = 0
  private val PhaseShowcase = 1
  private var phase: Int = PhaseLoading

  // ── Sections ───────────────────────────────────────────────────────
  private val SectionTextures = 0
  private val Section3D = 1
  private val SectionAudio = 2
  private val SectionCount = 3
  private var currentSection: Int = SectionTextures
  private var tabWasDown: Boolean = false

  // ── Asset Manager ──────────────────────────────────────────────────
  private var assetManager: AssetManager = uninitialized

  // ── Rendering ──────────────────────────────────────────────────────
  private var shapeRenderer: ShapeRenderer = uninitialized
  private var spriteBatch: SpriteBatch = uninitialized

  // ── 2D assets ──────────────────────────────────────────────────────
  private var checkerboardTex: Texture = uninitialized
  private var gradientTex: Texture = uninitialized

  // ── 3D assets ──────────────────────────────────────────────────────
  private var camera3d: PerspectiveCamera = uninitialized
  private var modelBatch: ModelBatch = uninitialized
  private var environment: Environment = uninitialized
  private var cubeModel: Model = uninitialized
  private var cubeInstance: ModelInstance = uninitialized
  private var octahedronModel: Model = uninitialized
  private var octahedronInstance: ModelInstance = uninitialized
  private var modelAngle: Float = 0f

  // ── Audio ──────────────────────────────────────────────────────────
  private var toneSound: Sound = uninitialized
  private var clickSound: Sound = uninitialized
  private var toneSoundId: SoundId = SoundId(-1L)
  private var musicVolume: Float = 0.5f
  private var musicPlaying: Boolean = false
  private var spaceWasDown: Boolean = false
  private var enterWasDown: Boolean = false
  private var touchWasDown: Boolean = false

  // ── Asset paths ────────────────────────────────────────────────────
  private val CheckerboardPath = "textures/checkerboard.png"
  private val GradientPath = "textures/gradient.png"
  private val CubeModelPath = "models/cube.g3dj"
  private val OctahedronModelPath = "models/octahedron.g3db"
  private val TonePath = "audio/tone.wav"
  private val ClickPath = "audio/click.wav"

  override def init()(using Sge): Unit = {
    shapeRenderer = ShapeRenderer()
    spriteBatch = SpriteBatch()

    // 3D setup
    camera3d = PerspectiveCamera(67f, Sge().graphics.width.toFloat, Sge().graphics.height.toFloat)
    camera3d.near = 1f
    camera3d.far = 100f
    camera3d.position.set(5f, 3f, 5f)
    camera3d.lookAt(0f, 0f, 0f)
    camera3d.update()

    modelBatch = ModelBatch(Nullable.empty, Nullable.empty, Nullable.empty)
    environment = Environment()
    environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

    // Prevent browser from intercepting Tab (used for section cycling)
    Sge().input.setCatchKey(Input.Keys.TAB, true)

    // Start loading assets
    assetManager = AssetManager(FileHandleResolver.Internal())
    assetManager.load[Texture](CheckerboardPath)
    assetManager.load[Texture](GradientPath)
    assetManager.load[Model](CubeModelPath)
    assetManager.load[Model](OctahedronModelPath)
    assetManager.load[Sound](TonePath)
    assetManager.load[Sound](ClickPath)

    phase = PhaseLoading
  }

  override def render(dt: Float)(using Sge): Unit = {
    // Guard: on Android, render() can be called before init() completes
    if (assetManager == null) { ScreenUtils.clear(0f, 0f, 0f, 1f, true); () } else {
      ScreenUtils.clear(0.12f, 0.12f, 0.18f, 1f, true)

      if (phase == PhaseLoading) {
        renderLoading(dt)
      } else {
        handleInput(dt)
        renderShowcase(dt)
      }
    }
  }

  // ── Loading phase ──────────────────────────────────────────────────

  private def renderLoading(dt: Float)(using Sge): Unit = {
    val done = assetManager.update()
    val progress = assetManager.progress

    val w = Sge().graphics.width.toFloat
    val h = Sge().graphics.height.toFloat

    // Progress bar
    val barW = w * 0.6f
    val barH = 30f
    val barX = (w - barW) / 2f
    val barY = (h - barH) / 2f

    shapeRenderer.drawing(ShapeType.Filled) {
      // Background
      shapeRenderer.setColor(0.2f, 0.2f, 0.25f, 1f)
      shapeRenderer.rectangle(barX, barY, barW, barH)
      // Fill
      shapeRenderer.setColor(0.3f, 0.7f, 0.4f, 1f)
      shapeRenderer.rectangle(barX, barY, barW * progress, barH)
    }

    // Border
    shapeRenderer.drawing(ShapeType.Line) {
      shapeRenderer.setColor(0.5f, 0.5f, 0.6f, 1f)
      shapeRenderer.rectangle(barX, barY, barW, barH)
    }

    if (done) {
      onLoadingComplete()
    }
  }

  private def onLoadingComplete()(using Sge): Unit = {
    // Retrieve loaded assets
    checkerboardTex = assetManager.get[Texture](CheckerboardPath).get
    gradientTex = assetManager.get[Texture](GradientPath).get

    // Cube from .g3dj (JSON)
    cubeModel = assetManager.get[Model](CubeModelPath).get
    cubeInstance = ModelInstance(cubeModel)
    cubeInstance.transform.setToTranslation(-1.5f, 0f, 0f)
    disableCulling(cubeInstance)

    // Octahedron from .g3db (UBJSON)
    octahedronModel = assetManager.get[Model](OctahedronModelPath).get
    octahedronInstance = ModelInstance(octahedronModel)
    octahedronInstance.transform.setToTranslation(1.5f, 0f, 0f)
    disableCulling(octahedronInstance)

    toneSound = assetManager.get[Sound](TonePath).get
    clickSound = assetManager.get[Sound](ClickPath).get

    phase = PhaseShowcase
  }

  // ── Input handling ─────────────────────────────────────────────────

  private def handleInput(dt: Float)(using Sge): Unit = {
    val input = Sge().input

    // Tab or touch cycles sections
    val tabDown = input.isKeyPressed(Input.Keys.TAB)
    if (tabDown && !tabWasDown) {
      currentSection = (currentSection + 1) % SectionCount
    }
    tabWasDown = tabDown

    // Touch: tap to interact
    val touched = input.touched
    if (touched && !touchWasDown) {
      if (currentSection == SectionAudio) {
        // Audio section: left half = play/pause, right half = click sound
        val touchX = input.x.toFloat
        val screenW = Sge().graphics.width.toFloat
        if (touchX < screenW * 0.5f) {
          // Toggle play/pause
          if (musicPlaying) {
            toneSound.stop(toneSoundId)
            musicPlaying = false
          } else {
            toneSoundId = toneSound.loop(Volume.unsafeMake(musicVolume))
            musicPlaying = true
          }
        } else {
          // Play click sound
          clickSound.play(Volume.unsafeMake(0.8f))
        }
      } else {
        // Other sections: cycle to next
        currentSection = (currentSection + 1) % SectionCount
      }
    }
    touchWasDown = touched

    // Audio keyboard controls
    if (currentSection == SectionAudio) {
      if (input.isKeyPressed(Input.Keys.UP)) {
        musicVolume = scala.math.min(musicVolume + 0.5f * dt, 1f)
        if (musicPlaying) {
          toneSound.setVolume(toneSoundId, Volume.unsafeMake(musicVolume))
        }
      }
      if (input.isKeyPressed(Input.Keys.DOWN)) {
        musicVolume = scala.math.max(musicVolume - 0.5f * dt, 0f)
        if (musicPlaying) {
          toneSound.setVolume(toneSoundId, Volume.unsafeMake(musicVolume))
        }
      }

      val spaceDown = input.isKeyPressed(Input.Keys.SPACE)
      if (spaceDown && !spaceWasDown) {
        if (musicPlaying) {
          toneSound.stop(toneSoundId)
          musicPlaying = false
        } else {
          toneSoundId = toneSound.loop(Volume.unsafeMake(musicVolume))
          musicPlaying = true
        }
      }
      spaceWasDown = spaceDown

      val enterDown = input.isKeyPressed(Input.Keys.ENTER)
      if (enterDown && !enterWasDown) {
        clickSound.play(Volume.unsafeMake(0.8f))
      }
      enterWasDown = enterDown
    }
  }

  // ── Showcase rendering ─────────────────────────────────────────────

  private def renderShowcase(dt: Float)(using Sge): Unit = {
    val w = Sge().graphics.width.toFloat
    val h = Sge().graphics.height.toFloat

    currentSection match {
      case SectionTextures => renderTextureSection(w, h)
      case Section3D       => render3DSection(dt, w, h)
      case SectionAudio    => renderAudioSection(w, h)
      case _               => ()
    }

    // Section indicator bar at bottom
    renderSectionBar(w, h)
  }

  private def renderTextureSection(w: Float, h: Float)(using Sge): Unit = {
    val texSize = scala.math.min(w, h) * 0.3f
    val gap = 40f
    val totalW = texSize * 2 + gap
    val startX = (w - totalW) / 2f
    val texY = (h - texSize) / 2f

    spriteBatch.rendering {
      // Checkerboard texture (left)
      spriteBatch.draw(checkerboardTex, startX, texY, texSize, texSize)
      // Gradient texture (right)
      spriteBatch.draw(gradientTex, startX + texSize + gap, texY, texSize, texSize)
    }

    // Labels using shape outlines
    shapeRenderer.drawing(ShapeType.Line) {
      shapeRenderer.setColor(0.7f, 0.7f, 0.8f, 1f)
      // Outline around checkerboard
      shapeRenderer.rectangle(startX - 2, texY - 2, texSize + 4, texSize + 4)
      // Outline around gradient
      shapeRenderer.rectangle(startX + texSize + gap - 2, texY - 2, texSize + 4, texSize + 4)
    }

    // Title bar at top
    renderTitleBar(w, h, "2D Textures (AssetManager + TextureLoader)")
  }

  /** Disable back-face culling on all materials so both front+back faces render
    * (needed for the stencil volume test).
    */
  private def disableCulling(instance: ModelInstance): Unit = {
    var mi = 0
    while (mi < instance.materials.size) {
      instance.materials(mi).set(IntAttribute.createCullFace(0))
      mi += 1
    }
  }

  private def render3DSection(dt: Float, w: Float, h: Float)(using Sge): Unit = {
    val gl = Sge().graphics.gl

    // Rotate both models around their own Y axis, then translate apart
    modelAngle += 45f * dt
    cubeInstance.transform.setToRotation(0f, 1f, 0f, modelAngle)
    cubeInstance.transform.setTranslation(-2f, 0f, 0f)
    octahedronInstance.transform.setToRotation(0f, 1f, 0f, modelAngle)
    octahedronInstance.transform.setTranslation(2f, 0.5f, 0f)

    // Update camera aspect ratio
    camera3d.viewportWidth = w
    camera3d.viewportHeight = h
    camera3d.update()

    // --- Convex volume stencil test ---
    // Determines which grid pixels are inside vs outside each model's volume.
    // Front faces closer than grid → incr stencil; back faces closer → decr.
    // Stencil != 0 means grid is inside the volume.

    // Step 1: Render grid to depth buffer only (no color)
    gl.glColorMask(false, false, false, false)
    gl.glEnable(EnableCap.DepthTest)
    gl.glDepthFunc(CompareFunc.Lequal)
    gl.glDepthMask(true)
    shapeRenderer.setProjectionMatrix(camera3d.combined)
    drawGrid()
    gl.glColorMask(true, true, true, true)

    // Step 2: Render both models (both faces) to stencil — mark inside-volume pixels
    gl.glEnable(EnableCap.StencilTest)
    gl.glClear(ClearMask.StencilBufferBit)
    gl.glStencilMask(0xff)
    gl.glStencilFunc(CompareFunc.Always, 0, 0xff)
    gl.glStencilOpSeparate(CullFace.Front, StencilOp.Keep, StencilOp.Keep, StencilOp.IncrWrap)
    gl.glStencilOpSeparate(CullFace.Back, StencilOp.Keep, StencilOp.Keep, StencilOp.DecrWrap)
    gl.glColorMask(false, false, false, false)
    modelBatch.rendering(camera3d) {
      modelBatch.render(cubeInstance, environment)
      modelBatch.render(octahedronInstance, environment)
    }
    gl.glColorMask(true, true, true, true)

    // Step 3: Clear depth and render grid where stencil == 0 (outside volumes)
    gl.glClear(ClearMask.DepthBufferBit)
    gl.glStencilFunc(CompareFunc.Equal, 0, 0xff)
    gl.glStencilMask(0x00)
    gl.glEnable(EnableCap.DepthTest)
    gl.glDepthFunc(CompareFunc.Lequal)
    drawGrid()

    // Step 4: Render both models with color (on top of grid)
    gl.glDisable(EnableCap.StencilTest)
    modelBatch.rendering(camera3d) {
      modelBatch.render(cubeInstance, environment)
      modelBatch.render(octahedronInstance, environment)
    }

    gl.glDisable(EnableCap.DepthTest)

    // Reset to screen projection for title
    shapeRenderer.setProjectionMatrix(spriteBatch.projectionMatrix)
    renderTitleBar(w, h, "3D Models (.g3dj cube + .g3db octahedron)")
  }

  private def drawGrid()(using Sge): Unit = {
    shapeRenderer.drawing(ShapeType.Line) {
      shapeRenderer.setColor(0.5f, 0.5f, 0.55f, 1f)
      var gi = -20
      while (gi <= 20) {
        val g = gi.toFloat * 0.5f
        shapeRenderer.line(g, 0f, -10f, g, 0f, 10f)
        shapeRenderer.line(-10f, 0f, g, 10f, 0f, g)
        gi += 1
      }
    }
  }

  private def renderAudioSection(w: Float, h: Float)(using Sge): Unit = {
    val cx = w / 2f
    val cy = h / 2f

    // Music volume bar
    val barW = w * 0.4f
    val barH = 24f
    val barX = cx - barW / 2f
    val barY = cy + 40f

    shapeRenderer.drawing(ShapeType.Filled) {
      // Volume bar background
      shapeRenderer.setColor(0.2f, 0.2f, 0.25f, 1f)
      shapeRenderer.rectangle(barX, barY, barW, barH)
      // Volume bar fill
      val volColor = if (musicPlaying) Color(0.3f, 0.6f, 0.9f, 1f) else Color(0.4f, 0.4f, 0.45f, 1f)
      shapeRenderer.setColor(volColor)
      shapeRenderer.rectangle(barX, barY, barW * musicVolume, barH)
    }

    // Volume bar border
    shapeRenderer.drawing(ShapeType.Line) {
      shapeRenderer.setColor(0.5f, 0.5f, 0.6f, 1f)
      shapeRenderer.rectangle(barX, barY, barW, barH)
    }

    // Play/pause indicator — filled circle (playing) or two vertical bars (paused)
    val indicatorY = cy - 30f
    shapeRenderer.drawing(ShapeType.Filled) {
      if (musicPlaying) {
        // Green filled circle
        shapeRenderer.setColor(0.3f, 0.8f, 0.4f, 1f)
        shapeRenderer.circle(cx, indicatorY, 18f)
      } else {
        // Gray pause bars
        shapeRenderer.setColor(0.5f, 0.5f, 0.55f, 1f)
        shapeRenderer.rectangle(cx - 14f, indicatorY - 16f, 10f, 32f)
        shapeRenderer.rectangle(cx + 4f, indicatorY - 16f, 10f, 32f)
      }
    }

    // Click sound indicator — small square at bottom
    val clickY = cy - 90f
    shapeRenderer.drawing(ShapeType.Line) {
      shapeRenderer.setColor(0.7f, 0.5f, 0.3f, 1f)
      shapeRenderer.rectangle(cx - 12f, clickY - 12f, 24f, 24f)
    }
    // Inner filled dot
    shapeRenderer.drawing(ShapeType.Filled) {
      shapeRenderer.setColor(0.9f, 0.6f, 0.3f, 1f)
      shapeRenderer.rectangle(cx - 6f, clickY - 6f, 12f, 12f)
    }

    renderTitleBar(w, h, "Audio (Sound loop + click, Up/Down=Volume, Space=Play/Pause, Enter=Click)")
  }

  private def renderTitleBar(w: Float, h: Float, text: String)(using Sge): Unit = {
    // Simple colored bar at top to indicate section
    val barH = 6f
    shapeRenderer.drawing(ShapeType.Filled) {
      currentSection match {
        case SectionTextures => shapeRenderer.setColor(0.4f, 0.7f, 0.9f, 1f)
        case Section3D       => shapeRenderer.setColor(0.9f, 0.5f, 0.3f, 1f)
        case SectionAudio    => shapeRenderer.setColor(0.4f, 0.8f, 0.4f, 1f)
        case _               => shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f)
      }
      shapeRenderer.rectangle(0f, h - barH, w, barH)
    }
  }

  private def renderSectionBar(w: Float, h: Float)(using Sge): Unit = {
    // Three dots at bottom indicating current section
    val dotRadius = 6f
    val dotGap = 24f
    val startX = w / 2f - dotGap
    val dotY = 20f

    shapeRenderer.drawing(ShapeType.Filled) {
      var i = 0
      while (i < SectionCount) {
        val x = startX + i * dotGap
        if (i == currentSection) {
          shapeRenderer.setColor(0.9f, 0.9f, 0.95f, 1f)
          shapeRenderer.circle(x, dotY, dotRadius)
        } else {
          shapeRenderer.setColor(0.35f, 0.35f, 0.4f, 1f)
          shapeRenderer.circle(x, dotY, dotRadius * 0.6f)
        }
        i += 1
      }
    }
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = {
    camera3d.viewportWidth = width.toFloat
    camera3d.viewportHeight = height.toFloat
    camera3d.update()
  }

  override def dispose()(using Sge): Unit = {
    assetManager.close()
    shapeRenderer.close()
    spriteBatch.close()
    modelBatch.close()
  }
}
