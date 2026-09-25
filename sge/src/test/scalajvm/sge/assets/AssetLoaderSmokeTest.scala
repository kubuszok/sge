/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package assets

import munit.FunSuite
import sge.assets.loaders._
import sge.files.{ FileHandle, FileType }
import sge.graphics.g2d.TextureAtlas
import sge.utils.SgeError
import lowlevel.Nullable

/** Behavioral tests for the core asset loaders.
  *
  * These exercise observable loader behavior without a GL/audio context: the dependency lists loaders declare for the [[AssetManager]] (`getDependencies`), the parameter defaults callers rely on, the
  * built-in [[FileHandleResolver]] dispatch, and the error paths taken when `loadSync`/`load` runs without the async/dependency phase having populated the loader. Assertions check parsed fields,
  * dependency file names + types, and thrown errors — never mere constructibility or non-nullness.
  */
class AssetLoaderSmokeTest extends FunSuite {

  given Sge = SgeTestFixture.testSge()

  /** Resolver that returns a FileHandle wrapping the filename (no real I/O). */
  private val stubResolver: FileHandleResolver = new FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      FileHandle(new java.io.File(fileName), FileType.Absolute)
  }

  private def handle(name: String): FileHandle = stubResolver.resolve(name)

  private def bareManager: AssetManager = AssetManager(stubResolver, defaultLoaders = false)

  // ─── FileHandleResolver dispatch ────────────────────────────────────

  /** Records which [[Files]] method a resolver delegated to, so resolver→Files dispatch is observable. */
  final private class RecordingFiles extends Files {
    var lastMethod:                               String     = ""
    var lastPath:                                 String     = ""
    private def mk(method: String, path: String): FileHandle = {
      lastMethod = method
      lastPath = path
      FileHandle(new java.io.File(path), FileType.Absolute)
    }
    def getFileHandle(path: String, fileType: sge.files.FileType): FileHandle = mk("getFileHandle", path)
    def classpath(path:     String):                               FileHandle = mk("classpath", path)
    def internal(path:      String):                               FileHandle = mk("internal", path)
    def external(path:      String):                               FileHandle = mk("external", path)
    def absolute(path:      String):                               FileHandle = mk("absolute", path)
    def local(path:         String):                               FileHandle = mk("local", path)
    def externalStoragePath:                                       String     = ""
    def isExternalStorageAvailable:                                Boolean    = false
    def localStoragePath:                                          String     = ""
    def isLocalStorageAvailable:                                   Boolean    = false
  }

  test("built-in resolvers delegate resolve() to the matching Files method with the given name") {
    val files = new RecordingFiles
    val sgeWithFiles: Sge = SgeTestFixture.testSge(files = files)

    FileHandleResolver.Internal(using sgeWithFiles).resolve("a.png")
    assertEquals(files.lastMethod, "internal")
    assertEquals(files.lastPath, "a.png")

    FileHandleResolver.External(using sgeWithFiles).resolve("b.png")
    assertEquals(files.lastMethod, "external")

    FileHandleResolver.Classpath(using sgeWithFiles).resolve("c.png")
    assertEquals(files.lastMethod, "classpath")

    FileHandleResolver.Absolute(using sgeWithFiles).resolve("d.png")
    assertEquals(files.lastMethod, "absolute")

    FileHandleResolver.Local(using sgeWithFiles).resolve("e.png")
    assertEquals(files.lastMethod, "local")
    assertEquals(files.lastPath, "e.png")
  }

  test("Prefix resolver prepends the prefix before delegating to the base resolver") {
    val prefix = FileHandleResolver.Prefix(stubResolver, "assets/")
    assertEquals(prefix.resolve("test.png").path, "assets/test.png")
    // Prefix is added as-is (no separator inserted).
    assertEquals(FileHandleResolver.Prefix(stubResolver, "ui-").resolve("skin.json").path, "ui-skin.json")
  }

  test("Resolution captures width, height and folder") {
    val res = FileHandleResolver.Resolution(1920, 1080, "1920x1080")
    assertEquals(res.portraitWidth, 1920)
    assertEquals(res.portraitHeight, 1080)
    assertEquals(res.folder, "1920x1080")
  }

  // ─── getDependencies: SkinLoader ────────────────────────────────────

  test("SkinLoader.getDependencies defaults to the sibling .atlas of the skin file") {
    val loader = SkinLoader(stubResolver)
    val deps   = loader.getDependencies("ui.json", handle("ui.json"), SkinLoader.SkinParameter())
    assertEquals(deps.size, 1)
    assertEquals(deps(0).fileName, "ui.atlas")
    assert(deps(0).`type` == classOf[TextureAtlas])
  }

  test("SkinLoader.getDependencies honors an explicit textureAtlasPath") {
    val loader = SkinLoader(stubResolver)
    val param  = SkinLoader.SkinParameter(textureAtlasPath = Nullable("skins/custom.atlas"))
    val deps   = loader.getDependencies("ui.json", handle("ui.json"), param)
    assertEquals(deps.size, 1)
    assertEquals(deps(0).fileName, "skins/custom.atlas")
    assert(deps(0).`type` == classOf[TextureAtlas])
  }

  test("SkinLoader.getDependencies with a null parameter falls back to the default .atlas") {
    // AssetManager passes a null parameter when none is supplied; the loader must treat it as absent.
    val loader = SkinLoader(stubResolver)
    val deps   = loader.getDependencies("hud/ui.json", handle("hud/ui.json"), null)
    assertEquals(deps.size, 1)
    assertEquals(deps(0).fileName, "hud/ui.atlas")
  }

  // ─── getDependencies: ParticleEffectLoader ──────────────────────────

  test("ParticleEffectLoader.getDependencies is empty without an atlas file") {
    val loader = ParticleEffectLoader(stubResolver)
    assertEquals(loader.getDependencies("fx.p", handle("fx.p"), ParticleEffectLoader.ParticleEffectParameter()).size, 0)
    // A null parameter is likewise dependency-free.
    assertEquals(loader.getDependencies("fx.p", handle("fx.p"), null).size, 0)
  }

  test("ParticleEffectLoader.getDependencies declares the atlas file as a TextureAtlas dependency") {
    val loader = ParticleEffectLoader(stubResolver)
    val param  = ParticleEffectLoader.ParticleEffectParameter()
    param.atlasFile = Nullable("fx/particles.atlas")
    val deps = loader.getDependencies("fx.p", handle("fx.p"), param)
    assertEquals(deps.size, 1)
    assertEquals(deps(0).fileName, "fx/particles.atlas")
    assert(deps(0).`type` == classOf[TextureAtlas])
  }

  test("ParticleEffectLoader.getDependencies with only an images dir declares no dependency") {
    val loader = ParticleEffectLoader(stubResolver)
    val param  = ParticleEffectLoader.ParticleEffectParameter()
    param.imagesDir = Nullable(handle("fx/images"))
    assertEquals(loader.getDependencies("fx.p", handle("fx.p"), param).size, 0)
  }

  // ─── getDependencies: loaders with no dependencies ──────────────────

  test("dependency-free loaders declare an empty dependency list") {
    assertEquals(
      ShaderProgramLoader(stubResolver).getDependencies("s.vert", handle("s.vert"), ShaderProgramLoader.ShaderProgramParameter()).size,
      0
    )
    assertEquals(
      I18NBundleLoader(stubResolver).getDependencies("i18n/msg", handle("i18n/msg"), I18NBundleLoader.I18NBundleParameter()).size,
      0
    )
    assertEquals(TextureLoader(stubResolver).getDependencies("t.png", handle("t.png"), TextureLoader.TextureParameter()).size, 0)
    assertEquals(CubemapLoader(stubResolver).getDependencies("c.ktx", handle("c.ktx"), CubemapLoader.CubemapParameter()).size, 0)
    assertEquals(SoundLoader(stubResolver).getDependencies("s.ogg", handle("s.ogg"), SoundLoader.SoundParameter()).size, 0)
    assertEquals(MusicLoader(stubResolver).getDependencies("m.ogg", handle("m.ogg"), MusicLoader.MusicParameter()).size, 0)
    assertEquals(PixmapLoader(stubResolver).getDependencies("p.png", handle("p.png"), PixmapLoader.PixmapParameter()).size, 0)
  }

  // ─── Error paths: loadSync/load without the preceding phase ──────────

  test("SoundLoader.loadSync fails when loadAsync has not run") {
    val e = intercept[SgeError.SerializationError] {
      SoundLoader(stubResolver).loadSync(bareManager, "s.ogg", handle("s.ogg"), SoundLoader.SoundParameter())
    }
    assert(e.getMessage.contains("Sound not loaded"), e.getMessage)
  }

  test("MusicLoader.loadSync fails when loadAsync has not run") {
    val e = intercept[SgeError.SerializationError] {
      MusicLoader(stubResolver).loadSync(bareManager, "m.ogg", handle("m.ogg"), MusicLoader.MusicParameter())
    }
    assert(e.getMessage.contains("Music not loaded"), e.getMessage)
  }

  test("PixmapLoader.loadSync fails when loadAsync has not run") {
    val e = intercept[SgeError.SerializationError] {
      PixmapLoader(stubResolver).loadSync(bareManager, "p.png", handle("p.png"), PixmapLoader.PixmapParameter())
    }
    assert(e.getMessage.contains("Pixmap not loaded"), e.getMessage)
  }

  test("I18NBundleLoader.loadSync fails when loadAsync has not run") {
    val e = intercept[SgeError.SerializationError] {
      I18NBundleLoader(stubResolver).loadSync(bareManager, "i18n/msg", handle("i18n/msg"), I18NBundleLoader.I18NBundleParameter())
    }
    assert(e.getMessage.contains("I18NBundle not loaded"), e.getMessage)
  }

  test("TextureAtlasLoader.load fails when getDependencies has not parsed the atlas data") {
    val e = intercept[SgeError.SerializationError] {
      TextureAtlasLoader(stubResolver).load(bareManager, "a.atlas", handle("a.atlas"), TextureAtlasLoader.TextureAtlasParameter())
    }
    assert(e.getMessage.contains("TextureAtlasData not loaded"), e.getMessage)
  }

  // ─── ISS-830 (Also-clause): guard paths for the remaining loaders ────

  test("ShaderProgramLoader accepts the (resolver, vertexSuffix, fragmentSuffix) convenience ctor [coverage]") {
    // Re-adds the ShaderProgramLoader(resolver, ".vs", ".fs") construction pin
    // dropped in the ISS-722 rewrite. The 3-arg ctor exists in both the port
    // (ShaderProgramLoader.scala:32) and the original (ShaderProgramLoader.java:45,
    // `public ShaderProgramLoader (FileHandleResolver, String, String)`). A
    // custom-suffix loader is still a functioning AssetLoader: it resolves names
    // through its resolver and declares no dependencies (loadSync's suffix
    // resolution + ShaderProgram compile is GL-bound, out of scope here).
    val loader = ShaderProgramLoader(stubResolver, ".vs", ".fs")
    assertEquals(loader.resolve("basic.vs").path, "basic.vs")
    assertEquals(
      loader.getDependencies("basic.vs", handle("basic.vs"), ShaderProgramLoader.ShaderProgramParameter()).size,
      0
    )
  }

  test("SkinLoader.loadSync fails when the atlas dependency has not been loaded [coverage]") {
    // SkinLoader.loadSync looks the sibling .atlas up in the AssetManager
    // (SkinLoader.scala:73). Without the dependency phase the manager holds no
    // such asset, so the lookup throws before any GL work (newSkin / skin.load)
    // is reached — a headless-feasible guard contract.
    val e = intercept[SgeError.InvalidInput] {
      SkinLoader(stubResolver).loadSync(bareManager, "ui.json", handle("ui.json"), SkinLoader.SkinParameter())
    }
    assert(e.getMessage.contains("Asset not loaded"), e.getMessage)
    assert(e.getMessage.contains("ui.atlas"), e.getMessage)
  }

  test("BitmapFontLoader.loadSync fails when getDependencies has not parsed the font data [coverage]") {
    // In this port BitmapFontData is populated by getDependencies (loadAsync is a
    // no-op, BitmapFontLoader.scala:75). Calling loadSync first leaves `data`
    // empty, so the default (non-atlas) branch throws
    // GraphicsError("BitmapFontData not loaded") (BitmapFontLoader.scala:93)
    // before touching the manager or any GL. (Assembling the actual font is
    // GL-bound, out of scope.)
    val e = intercept[SgeError.GraphicsError] {
      BitmapFontLoader(stubResolver).loadSync(bareManager, "font.fnt", handle("font.fnt"), BitmapFontLoader.BitmapFontParameter())
    }
    assert(e.getMessage.contains("BitmapFontData not loaded"), e.getMessage)
  }

  // ─── Parameter defaults ─────────────────────────────────────────────

  test("AssetLoaderParameters defaults to an empty loaded-callback") {
    assert(AssetLoaderParameters[String]().loadedCallback.isEmpty)
  }

  test("BitmapFontParameter defaults") {
    val p = BitmapFontLoader.BitmapFontParameter()
    assertEquals(p.flip, false)
    assertEquals(p.genMipMaps, false)
    assert(p.bitmapFontData.isEmpty)
    assert(p.atlasName.isEmpty)
  }

  test("TextureParameter defaults") {
    val p = TextureLoader.TextureParameter()
    assertEquals(p.genMipMaps, false)
    assert(p.format.isEmpty)
    assert(p.texture.isEmpty)
    assert(p.textureData.isEmpty)
  }

  test("TextureAtlasParameter flip defaults false and is honored when set") {
    assertEquals(TextureAtlasLoader.TextureAtlasParameter().flip, false)
    assertEquals(TextureAtlasLoader.TextureAtlasParameter(flip = true).flip, true)
  }

  test("CubemapParameter defaults") {
    val p = CubemapLoader.CubemapParameter()
    assertEquals(p.genMipMaps, false)
    assert(p.format.isEmpty)
    assert(p.cubemap.isEmpty)
    assert(p.cubemapData.isEmpty)
  }

  test("SkinParameter defaults empty and carries an atlas path when set") {
    assert(SkinLoader.SkinParameter().textureAtlasPath.isEmpty)
    assert(SkinLoader.SkinParameter().resources.isEmpty)
    assertEquals(SkinLoader.SkinParameter(textureAtlasPath = Nullable("ui.atlas")).textureAtlasPath.getOrElse(""), "ui.atlas")
  }

  test("ShaderProgramParameter defaults") {
    val p = ShaderProgramLoader.ShaderProgramParameter()
    assertEquals(p.logOnCompileFailure, true)
    assert(p.vertexFile.isEmpty)
    assert(p.fragmentFile.isEmpty)
    assert(p.prependVertexCode.isEmpty)
    assert(p.prependFragmentCode.isEmpty)
  }

  test("I18NBundleParameter defaults empty and carries a locale when set") {
    val p = I18NBundleLoader.I18NBundleParameter()
    assert(p.locale.isEmpty)
    assert(p.encoding.isEmpty)
    assertEquals(
      I18NBundleLoader.I18NBundleParameter(Nullable(java.util.Locale.FRENCH)).locale.getOrElse(java.util.Locale.ROOT),
      java.util.Locale.FRENCH
    )
  }

  test("ParticleEffectParameter defaults") {
    val p = ParticleEffectLoader.ParticleEffectParameter()
    assert(p.atlasFile.isEmpty)
    assert(p.atlasPrefix.isEmpty)
    assert(p.imagesDir.isEmpty)
  }

  test("ModelParameters supplies a defaulted texture parameter") {
    val p = ModelLoader.ModelParameters()
    assertEquals(p.textureParameter.genMipMaps, false)
    assert(p.textureParameter.format.isEmpty)
  }

  // ─── resolve() on all loaders ───────────────────────────────────────

  test("every loader resolves a file name through its resolver") {
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
    for (loader <- loaders)
      assertEquals(loader.resolve("test.bin").path, "test.bin")
  }
}
