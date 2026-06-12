/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-515 (VisUI extension ships NO src/main/resources at all).
 *
 * The VisUI port references classpath resources that are entirely absent from
 * sge-extension/visui/src/main/resources (the directory does not even exist):
 *
 *   - VisUI.scala line 44: SkinScale.X1 -> "com/kotcrab/vis/ui/skin/x1/uiskin.json"
 *     VisUI.scala line 47: SkinScale.X2 -> "com/kotcrab/vis/ui/skin/x2/uiskin.json"
 *     VisUI.scala line 49: getSkinFile resolves them via Sge().files.classpath
 *     VisUI.scala line 71: load(FileHandle) -> new Skin(visSkinFile)
 *   - Locales.scala lines 42/52/62/72/82/91: six I18N bundles under
 *     "com/kotcrab/vis/ui/i18n/" (Common, Dialogs, TabbedPane, ButtonBar,
 *     ColorPicker, FileChooser), loaded via Sge().files.classpath
 *     (Locales.scala lines 103-106).
 *   - PickerCommons.scala lines 66-70: loadShader resolves
 *     "com/kotcrab/vis/ui/widget/color/internal/" + file via
 *     Sge().files.classpath for the six shader files named on lines 56-62:
 *     default.vert, palette.frag, verticalBar.frag, checkerboard.frag,
 *     hsv.frag, rgb.frag.
 *
 * Every no-arg/styleName-only VisUI widget constructor routes through
 * VisUI.getSkin, so without these resources the extension cannot execute at
 * all: Skin's FileHandle ctor (Skin.scala lines 76-84) calls load(skinFile)
 * whose readJson fails on the missing classpath entry (FileHandles.scala
 * line 123 throws SgeError.FileReadError("File not found"); Skin.load line
 * 118 rethrows SgeError as-is).
 *
 * The expected CONTENT below is pinned against the original resources at
 * original-src/vis-ui/ui/src/main/resources/com/kotcrab/vis/ui/ (upstream
 * commit 820300c86a1bd907404217195a9987e5c66d2220, the commit every visui
 * header references):
 *   - skin/x1/uiskin.json line 3:    BitmapFont "default-font" {file: default.fnt}
 *   - skin/x1/uiskin.json line 13:   Color "vis-blue" {a:1, b:0.886, g:0.631, r:0.105}
 *   - skin/x1/uiskin.json line 27:   TextButton$TextButtonStyle "default"
 *   - skin/x1/uiskin.json lines 92-94: com.kotcrab.vis.ui.Sizes "default"
 *     {scaleFactor: 1, ...} and "x2" {scaleFactor: 2, ...}
 *   - skin/x1/uiskin.json line 103:  VisTextButton$VisTextButtonStyle "default"
 *   - skin/x2/uiskin.json mirrors x1 (Sizes at lines 92-94, VisTextButtonStyle
 *     at lines 102-103)
 *   - i18n/Common.properties lines 17-18: pleaseWait=Please wait... /
 *     unknownErrorOccurred=Unknown error occurred
 * The style/content assertions are folded into the load tests so the fix
 * cannot ship hollowed-out resource files: VisUI.getSizes (VisUI.scala lines
 * 115-117) and the widget styles must actually be parseable from the shipped
 * uiskin.json. Sizes/VisTextButtonStyle resolution is part of the skin
 * contract — com.kotcrab.vis.ui.VisUI.java relies on Skin parsing those
 * types from this exact JSON.
 *
 * Headless fixture: see VisUITestFixture.scala — real DesktopFiles for
 * classpath resolution, no-op GL20 for the Texture/BitmapFont uploads that
 * Skin loading performs (PNG decode goes through ImageIO on the JVM:
 * Gdx2dOpsJvm.scala), per the established NoopGL20 red-suite pattern
 * (sge/src/test/scalajvm/sge/graphics/g2d/SpriteCacheRedSuite.scala).
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original VisUI semantics, not the port's.
 */
package sge
package visui

import lowlevel.Nullable
import sge.graphics.Color
import sge.graphics.g2d.BitmapFont
import sge.scenes.scene2d.ui.TextButton
import sge.utils.I18NBundle
import sge.visui.widget.VisTextButton

class VisUIResourcesRedSuite extends munit.FunSuite {

  override def afterEach(context: AfterEach): Unit = {
    // VisUI keeps global state; load() throws "VisUI cannot be loaded twice"
    // (VisUI.scala line 83) unless disposed between tests. dispose() is safe
    // when nothing was loaded (Nullable.foreach no-op, VisUI.scala 100-106).
    VisUI.dispose()
    // Locales caches bundles in vars (Locales.scala lines 33-38); reset so
    // every test exercises the classpath load.
    Locales.setCommonBundle(Nullable.empty[I18NBundle])
  }

  // --- ISS-515: default skin (SkinScale.X1) ----------------------------------

  test("ISS-515: VisUI.load() must succeed and yield the x1 skin with the pinned original styles") {
    given Sge = VisUITestFixture.headlessSge()

    // Currently throws SgeError.FileReadError("File not found") because
    // com/kotcrab/vis/ui/skin/x1/uiskin.json (VisUI.scala line 44) is not on
    // the classpath — sge-extension/visui has no main resources.
    VisUI.load()

    assert(VisUI.isLoaded, "VisUI.isLoaded must be true after load() (VisUI.scala line 113)")
    val skin = VisUI.getSkin

    // Content pinning against original-src/vis-ui skin/x1/uiskin.json — the
    // fix must ship the real VisUI skin content, not a JSON that merely parses:
    assert(
      skin.has("default-font", classOf[BitmapFont]),
      "x1 uiskin.json line 3 declares BitmapFont 'default-font' (file: default.fnt) — missing from the loaded skin"
    )
    assert(
      skin.has("vis-blue", classOf[Color]),
      "x1 uiskin.json line 13 declares Color 'vis-blue' — missing from the loaded skin"
    )
    val visBlue = skin.getColor("vis-blue")
    // x1 uiskin.json line 13: vis-blue must be {r: 0.105, g: 0.631, b: 0.886, a: 1}
    assertEqualsFloat(visBlue.r, 0.105f, 0.001f)
    assertEqualsFloat(visBlue.g, 0.631f, 0.001f)
    assertEqualsFloat(visBlue.b, 0.886f, 0.001f)
    assertEqualsFloat(visBlue.a, 1f, 0.001f)
    assert(
      skin.has("default", classOf[TextButton.TextButtonStyle]),
      "x1 uiskin.json line 27 declares TextButton$TextButtonStyle 'default' — missing from the loaded skin"
    )
    assert(
      skin.has("default", classOf[VisTextButton.VisTextButtonStyle]),
      "x1 uiskin.json line 103 declares VisTextButton$VisTextButtonStyle 'default' — every no-arg VisTextButton ctor (VisTextButton.scala lines 55-66) needs it"
    )
    assert(
      skin.has("default", classOf[Sizes]),
      "x1 uiskin.json lines 92-93 declare Sizes 'default' — VisUI.getSizes (VisUI.scala lines 115-117) resolves it for SkinScale.X1"
    )
    assertEquals(
      VisUI.getSizes.scaleFactor,
      1f,
      "x1 uiskin.json line 93: Sizes 'default' has scaleFactor: 1"
    )
  }

  // --- ISS-515: x2 skin (SkinScale.X2) ----------------------------------------

  test("ISS-515: VisUI.load(SkinScale.X2) must succeed and yield the x2 skin with the pinned original styles") {
    given Sge = VisUITestFixture.headlessSge()

    // Currently throws because com/kotcrab/vis/ui/skin/x2/uiskin.json
    // (VisUI.scala line 47) is not on the classpath.
    VisUI.load(VisUI.SkinScale.X2)

    assert(VisUI.isLoaded, "VisUI.isLoaded must be true after load(SkinScale.X2)")
    val skin = VisUI.getSkin

    assert(
      skin.has("default-font", classOf[BitmapFont]),
      "x2 uiskin.json line 3 declares BitmapFont 'default-font' (file: default.fnt) — missing from the loaded skin"
    )
    assert(
      skin.has("default", classOf[VisTextButton.VisTextButtonStyle]),
      "x2 uiskin.json line 103 declares VisTextButton$VisTextButtonStyle 'default' — missing from the loaded skin"
    )
    assert(
      skin.has("x2", classOf[Sizes]),
      "x2 uiskin.json line 94 declares Sizes 'x2' — VisUI.getSizes (VisUI.scala lines 115-117) resolves sizesName 'x2' for SkinScale.X2 (VisUI.scala line 47)"
    )
    assertEquals(
      VisUI.getSizes.scaleFactor,
      2f,
      "x2 uiskin.json line 94: Sizes 'x2' has scaleFactor: 2"
    )
  }

  // --- ISS-515: Locales I18N bundles -------------------------------------------

  test("ISS-515: Locales.getCommonBundle must load com/kotcrab/vis/ui/i18n/Common with the original entries") {
    given Sge = VisUITestFixture.headlessSge()

    // Locales.getCommonBundle (Locales.scala lines 41-44) loads
    // "com/kotcrab/vis/ui/i18n/Common" via Sge().files.classpath +
    // I18NBundle.createBundle (lines 103-106). Currently throws because the
    // .properties bundle is not on the classpath.
    val bundle = Locales.getCommonBundle

    assertEquals(
      bundle.get("pleaseWait"),
      "Please wait...",
      "Common.properties line 17: pleaseWait=Please wait..."
    )
    assertEquals(
      bundle.get("unknownErrorOccurred"),
      "Unknown error occurred",
      "Common.properties line 18: unknownErrorOccurred=Unknown error occurred"
    )
  }

  test("ISS-515: all six Locales bundle classpath paths must resolve") {
    given sge: Sge = VisUITestFixture.headlessSge()

    // Locales.scala accessor lines: Common 42, Dialogs 52, TabbedPane 62,
    // ButtonBar 72, ColorPicker 82, FileChooser 91. I18NBundle.createBundle
    // appends ".properties" to the base FileHandle, so the base bundle file
    // each accessor needs is "<path>.properties" — all six exist at
    // original-src/vis-ui/ui/src/main/resources/com/kotcrab/vis/ui/i18n/.
    val bundles = List("Common", "Dialogs", "TabbedPane", "ButtonBar", "ColorPicker", "FileChooser")
    val missing = bundles.filterNot { name =>
      sge.files.classpath("com/kotcrab/vis/ui/i18n/" + name + ".properties").exists()
    }
    assert(
      missing.isEmpty,
      s"Locales (Locales.scala lines 42/52/62/72/82/91) requires these i18n bundles on the classpath, missing: ${missing.mkString(", ")}"
    )
  }

  // --- ISS-515: PickerCommons ColorPicker shaders ------------------------------

  test("ISS-515: all six PickerCommons shader classpath paths must resolve") {
    given sge: Sge = VisUITestFixture.headlessSge()

    // PickerCommons.loadShader (PickerCommons.scala lines 66-70) resolves
    //   Sge().files.classpath("com/kotcrab/vis/ui/widget/color/internal/" + file)
    // for the vert/frag pairs requested by loadShaders (lines 55-64):
    //   line 56: ("default.vert", "palette.frag")
    //   line 57: ("default.vert", "verticalBar.frag")
    //   line 58: ("default.vert", "checkerboard.frag")
    //   line 61: ("default.vert", "hsv.frag")
    //   line 62: ("default.vert", "rgb.frag")
    val shaders = List("default.vert", "palette.frag", "verticalBar.frag", "checkerboard.frag", "hsv.frag", "rgb.frag")
    val missing = shaders.filterNot { name =>
      sge.files.classpath("com/kotcrab/vis/ui/widget/color/internal/" + name).exists()
    }
    assert(
      missing.isEmpty,
      s"PickerCommons (PickerCommons.scala lines 56-62, 66-70) requires these shader files on the classpath, missing: ${missing.mkString(", ")}"
    )
  }
}
