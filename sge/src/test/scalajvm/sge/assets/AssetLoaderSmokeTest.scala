/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package assets

import munit.FunSuite
import sge.assets.loaders._
import sge.files.{ FileHandle, FileType }
import sge.utils.Nullable

/** Compile-time smoke tests for asset loaders.
  *
  * Verifies that loaders and their parameter classes can be instantiated without requiring a GL context, and that key API methods have the expected signatures. This catches signature regressions
  * early.
  */
class AssetLoaderSmokeTest extends FunSuite {

  given Sge = SgeTestFixture.testSge()

  /** Resolver that returns a FileHandle wrapping the filename (no real I/O). */
  private val stubResolver: FileHandleResolver = new FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      FileHandle(new java.io.File(fileName), FileType.Absolute)
  }

  // ─── Resolver instantiation ─────────────────────────────────────────

  test("FileHandleResolver.Internal instantiates") {
    val resolver = FileHandleResolver.Internal()
    assert(resolver.isInstanceOf[FileHandleResolver])
  }

  test("FileHandleResolver.External instantiates") {
    val resolver = FileHandleResolver.External()
    assert(resolver.isInstanceOf[FileHandleResolver])
  }

  test("FileHandleResolver.Classpath instantiates") {
    val resolver = FileHandleResolver.Classpath()
    assert(resolver.isInstanceOf[FileHandleResolver])
  }

  test("FileHandleResolver.Absolute instantiates") {
    val resolver = FileHandleResolver.Absolute()
    assert(resolver.isInstanceOf[FileHandleResolver])
  }

  test("FileHandleResolver.Local instantiates") {
    val resolver = FileHandleResolver.Local()
    assert(resolver.isInstanceOf[FileHandleResolver])
  }

  test("FileHandleResolver.Prefix instantiates and delegates") {
    val prefix = FileHandleResolver.Prefix(stubResolver, "assets/")
    val handle = prefix.resolve("test.png")
    assertEquals(handle.path, "assets/test.png")
  }

  test("FileHandleResolver.Resolution case class instantiates") {
    val res = FileHandleResolver.Resolution(1920, 1080, "1920x1080")
    assertEquals(res.portraitWidth, 1920)
    assertEquals(res.portraitHeight, 1080)
    assertEquals(res.folder, "1920x1080")
  }

  // ─── Loader instantiation ──────────────────────────────────────────

  test("BitmapFontLoader instantiates") {
    val loader = BitmapFontLoader(stubResolver)
    assert(loader.isInstanceOf[AsynchronousAssetLoader[?, ?]])
  }

  test("TextureLoader instantiates") {
    val loader = TextureLoader(stubResolver)
    assert(loader.isInstanceOf[AsynchronousAssetLoader[?, ?]])
  }

  test("TextureAtlasLoader instantiates") {
    val loader = TextureAtlasLoader(stubResolver)
    assert(loader.isInstanceOf[SynchronousAssetLoader[?, ?]])
  }

  test("CubemapLoader instantiates") {
    val loader = CubemapLoader(stubResolver)
    assert(loader.isInstanceOf[AsynchronousAssetLoader[?, ?]])
  }

  test("PixmapLoader instantiates") {
    val loader = PixmapLoader(stubResolver)
    assert(loader.isInstanceOf[AsynchronousAssetLoader[?, ?]])
  }

  test("SoundLoader instantiates") {
    val loader = SoundLoader(stubResolver)
    assert(loader.isInstanceOf[AsynchronousAssetLoader[?, ?]])
  }

  test("MusicLoader instantiates") {
    val loader = MusicLoader(stubResolver)
    assert(loader.isInstanceOf[AsynchronousAssetLoader[?, ?]])
  }

  test("SkinLoader instantiates") {
    val loader = SkinLoader(stubResolver)
    assert(loader.isInstanceOf[AsynchronousAssetLoader[?, ?]])
  }

  test("ShaderProgramLoader instantiates") {
    val loader = ShaderProgramLoader(stubResolver)
    assert(loader.isInstanceOf[AsynchronousAssetLoader[?, ?]])
  }

  test("ShaderProgramLoader accepts custom suffixes") {
    val loader = ShaderProgramLoader(stubResolver, ".vs", ".fs")
    assert(loader.isInstanceOf[AsynchronousAssetLoader[?, ?]])
  }

  test("I18NBundleLoader instantiates") {
    val loader = I18NBundleLoader(stubResolver)
    assert(loader.isInstanceOf[AsynchronousAssetLoader[?, ?]])
  }

  test("ParticleEffectLoader instantiates") {
    val loader = ParticleEffectLoader(stubResolver)
    assert(loader.isInstanceOf[SynchronousAssetLoader[?, ?]])
  }

  // ─── Parameter class instantiation ─────────────────────────────────

  test("AssetLoaderParameters instantiates with empty callback") {
    val params = AssetLoaderParameters[String]()
    assert(params.loadedCallback.isEmpty)
  }

  test("BitmapFontParameter instantiates with defaults") {
    val p = BitmapFontLoader.BitmapFontParameter()
    assertEquals(p.flip, false)
    assertEquals(p.genMipMaps, false)
    assert(p.bitmapFontData.isEmpty)
    assert(p.atlasName.isEmpty)
  }

  test("TextureParameter instantiates with defaults") {
    val p = TextureLoader.TextureParameter()
    assertEquals(p.genMipMaps, false)
    assert(p.format.isEmpty)
    assert(p.texture.isEmpty)
    assert(p.textureData.isEmpty)
  }

  test("TextureAtlasParameter instantiates with defaults") {
    val p = TextureAtlasLoader.TextureAtlasParameter()
    assertEquals(p.flip, false)
  }

  test("TextureAtlasParameter accepts flip=true") {
    val p = TextureAtlasLoader.TextureAtlasParameter(flip = true)
    assertEquals(p.flip, true)
  }

  test("CubemapParameter instantiates with defaults") {
    val p = CubemapLoader.CubemapParameter()
    assertEquals(p.genMipMaps, false)
    assert(p.format.isEmpty)
    assert(p.cubemap.isEmpty)
    assert(p.cubemapData.isEmpty)
  }

  test("PixmapParameter instantiates") {
    val p = PixmapLoader.PixmapParameter()
    assert(p.loadedCallback.isEmpty)
  }

  test("SoundParameter instantiates") {
    val p = SoundLoader.SoundParameter()
    assert(p.loadedCallback.isEmpty)
  }

  test("MusicParameter instantiates") {
    val p = MusicLoader.MusicParameter()
    assert(p.loadedCallback.isEmpty)
  }

  test("SkinParameter instantiates with defaults") {
    val p = SkinLoader.SkinParameter()
    assert(p.textureAtlasPath.isEmpty)
    assert(p.resources.isEmpty)
  }

  test("SkinParameter instantiates with atlas path") {
    val p = SkinLoader.SkinParameter(textureAtlasPath = Nullable("ui.atlas"))
    assertEquals(p.textureAtlasPath.getOrElse(""), "ui.atlas")
  }

  test("ShaderProgramParameter instantiates with defaults") {
    val p = ShaderProgramLoader.ShaderProgramParameter()
    assertEquals(p.logOnCompileFailure, true)
    assert(p.vertexFile.isEmpty)
    assert(p.fragmentFile.isEmpty)
    assert(p.prependVertexCode.isEmpty)
    assert(p.prependFragmentCode.isEmpty)
  }

  test("I18NBundleParameter instantiates with defaults") {
    val p = I18NBundleLoader.I18NBundleParameter()
    assert(p.locale.isEmpty)
    assert(p.encoding.isEmpty)
  }

  test("I18NBundleParameter instantiates with locale") {
    val p = I18NBundleLoader.I18NBundleParameter(Nullable(java.util.Locale.FRENCH))
    assertEquals(p.locale.getOrElse(java.util.Locale.ROOT), java.util.Locale.FRENCH)
  }

  test("ParticleEffectParameter instantiates with defaults") {
    val p = ParticleEffectLoader.ParticleEffectParameter()
    assert(p.atlasFile.isEmpty)
    assert(p.atlasPrefix.isEmpty)
    assert(p.imagesDir.isEmpty)
  }

  test("ModelParameters instantiates with texture defaults") {
    val p = ModelLoader.ModelParameters()
    assert(p.textureParameter != null)
  }

  // ─── resolve method exists on all loaders ──────────────────────────

  test("all loaders expose resolve method") {
    val loaders: List[AssetLoader[?, ?]] = List(
      BitmapFontLoader(stubResolver),
      TextureLoader(stubResolver),
      TextureAtlasLoader(stubResolver),
      CubemapLoader(stubResolver),
      PixmapLoader(stubResolver),
      SoundLoader(stubResolver),
      MusicLoader(stubResolver),
      SkinLoader(stubResolver),
      ShaderProgramLoader(stubResolver),
      I18NBundleLoader(stubResolver),
      ParticleEffectLoader(stubResolver)
    )
    for (loader <- loaders) {
      val handle = loader.resolve("test.bin")
      assertEquals(handle.path, "test.bin")
    }
  }
}
