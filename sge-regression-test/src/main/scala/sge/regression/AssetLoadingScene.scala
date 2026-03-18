/*
 * SGE Regression Test — asset loading checks.
 *
 * Tests: internal FileHandle read, Texture creation from Pixmap,
 * AssetManager load + retrieve cycle. Exercises the exact code paths
 * that fail silently at compile time but crash at runtime when file
 * resolution or GL texture upload is broken.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

import sge.assets.AssetManager
import sge.assets.loaders.FileHandleResolver
import sge.graphics.{ Pixmap, Texture, TextureHandle }
import sge.utils.ScreenUtils
import sge.Application

/** Verifies asset loading works end-to-end at runtime.
  *
  * Checks:
  *   - Internal FileHandle read (text file roundtrip)
  *   - Pixmap creation + Texture upload (procedural, no file)
  *   - AssetManager load/update/retrieve cycle (PNG texture from resources)
  *
  * These are the exact code paths that caused the asset-showcase failure: compilation + unit tests passed, but runtime file resolution was broken.
  */
object AssetLoadingScene extends RegressionScene {

  override val name: String = "AssetLoading"

  private var assetManager: AssetManager = scala.compiletime.uninitialized
  private var loadStarted:  Boolean      = false
  private var loadFinished: Boolean      = false
  private var texture:      Texture      = scala.compiletime.uninitialized
  private var pixmapTex:    Texture      = scala.compiletime.uninitialized

  override def init()(using Sge): Unit = {
    // --- Check 1: Read a text file from internal resources ---
    try {
      val textFile = Sge().files.internal("regression/test-data.txt")
      val exists   = textFile.exists()
      SmokeResult.logCheck("FILE_EXISTS", exists, s"regression/test-data.txt exists=$exists")
      if (exists) {
        val content = textFile.readString()
        val ok      = content == "SGE_REGRESSION_TEST_DATA"
        SmokeResult.logCheck("FILE_READ", ok, s"content=${if (ok) "matches" else s"'$content'"}")
      }
    } catch {
      case e: Exception =>
        SmokeResult.logCheck("FILE_READ", false, s"Exception: ${e.getMessage}")
    }

    // --- Check 2: Create Pixmap + Texture (procedural, no file) ---
    try {
      val pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888)
      pixmap.setColor(1f, 0f, 0f, 1f)
      pixmap.fill()
      pixmapTex = new Texture(pixmap)
      val handle = pixmapTex.textureObjectHandle
      val ok     = handle.toInt > 0
      SmokeResult.logCheck("PIXMAP_TEXTURE", ok, s"GL handle=$handle")
      pixmap.close()
    } catch {
      case e: Exception =>
        SmokeResult.logCheck("PIXMAP_TEXTURE", false, s"Exception: ${e.getMessage}")
    }

    // --- Check 3: AssetManager load cycle (PNG from generated resources) ---
    // On browser (WebGL), assets must be pre-fetched via the manifest system before
    // BrowserFileHandle can serve them synchronously. The AssetManager load test only
    // works on JVM and Native where internal FileHandle reads from the classpath/resources.
    val isBrowser = Sge().application.applicationType == Application.ApplicationType.WebGL
    if (isBrowser) {
      SmokeResult.logCheck("ASSET_LOAD", true, "skipped on browser (requires manifest preload)")
    } else {
      try {
        assetManager = new AssetManager(FileHandleResolver.Internal())
        assetManager.load[Texture]("regression/test-texture.png")
        loadStarted = true
      } catch {
        case e: Exception =>
          SmokeResult.logCheck("ASSET_LOAD", false, s"Exception queueing: ${e.getMessage}")
      }
    }
  }

  override def render(elapsed: Float)(using Sge): Unit = {
    // Drive AssetManager update loop across frames (the real-world pattern)
    if (loadStarted && !loadFinished) {
      try {
        val done = assetManager.update()
        if (done) {
          loadFinished = true
          texture = assetManager[Texture]("regression/test-texture.png")
          val w  = texture.width.toInt
          val h  = texture.height.toInt
          val ok = w > 0 && h > 0
          SmokeResult.logCheck("ASSET_LOAD", ok, s"Texture loaded: ${w}x${h}")
        }
      } catch {
        case e: Exception =>
          loadFinished = true
          SmokeResult.logCheck("ASSET_LOAD", false, s"Exception loading: ${e.getMessage}")
      }
    }

    // Visual feedback
    if (loadFinished) ScreenUtils.clear(0f, 0.5f, 0f, 1f) // green = done
    else ScreenUtils.clear(0.2f, 0.2f, 0.5f, 1f) // blue = loading
  }

  override def dispose()(using Sge): Unit = {
    if (pixmapTex != null) pixmapTex.close() // scalafix:ok
    if (assetManager != null) assetManager.close() // scalafix:ok — disposes loaded textures
  }
}
