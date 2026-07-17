/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Hardening suite for ISS-608 + ISS-731 (wave 2026-07-17-F, territory AB).
 *
 * Pins the VisUI skin-reader round-trip through the PRODUCTION core Skin JSON
 * path. VisUISkinReaders.register() contributes VisUI's FQCN JSON class tags
 * and style readers to the reflection-free core Skin / SkinStyleReader registry
 * (the ISS-515 seam, general mechanism ISS-534). After registration, a skin
 * JSON fragment naming a VisUI-typed section
 * (com.kotcrab.vis.ui.widget.VisTextButton$VisTextButtonStyle) must parse into a
 * populated VisTextButtonStyle via the registered reader. The negative case
 * pins that an UNREGISTERED FQCN tag surfaces a load-time SgeError rather than
 * being silently dropped (core Skin.load resolveClass -> None branch).
 *
 * NOTE: this suite exercises VisUISkinReaders.register() DIRECTLY rather than
 * VisUI.load(), so the round-trip is isolated to reader registration + the JSON
 * parse with no dependency on the shipped uiskin.json asset or a GL texture
 * upload. It reuses the established headless VisUI fixture (VisUITestFixture)
 * and the in-memory-skin-JSON pattern from the core
 * SkinJsonStyleLoadingRedTest (BaseDrawable resources + Absolute FileHandle).
 */
package sge
package visui

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files as JFiles

import sge.files.{ FileHandle, FileType }
import sge.scenes.scene2d.ui.Skin
import sge.scenes.scene2d.utils.{ BaseDrawable, Drawable }
import sge.utils.SgeError
import sge.visui.widget.VisTextButton

class VisUISkinStyleRoundTripSuite extends munit.FunSuite {

  private given Sge = VisUITestFixture.headlessSge()

  // Contribute VisUI's JSON class tags + style readers to the core Skin seam.
  // register() is idempotent and mutates shared static maps, so it is safe to
  // run once here in the suite body; every subsequently constructed Skin copies
  // the registered extension tags at construction time.
  VisUISkinReaders.register()

  /** Writes the given skin JSON to a temp file and returns an Absolute FileHandle over it, so Skin.load reads the real bytes through the normal file path (mirrors SkinJsonStyleLoadingRedTest). */
  private def skinFileOf(json: String): FileHandle = {
    val tmp = File.createTempFile("iss608-visui-skin", ".json")
    tmp.deleteOnExit()
    JFiles.write(tmp.toPath, json.getBytes(StandardCharsets.UTF_8))
    new FileHandle(tmp, FileType.Absolute)
  }

  test("ISS-608/731: a VisUI-typed skin section parses into a populated style via the registered reader") {
    val skin = new Skin()
    // A Drawable the VisTextButtonStyle.focusBorder reference resolves against.
    skin.add("focus-border", new BaseDrawable(), classOf[Drawable])

    skin.load(
      skinFileOf(
        """{
          |  "com.kotcrab.vis.ui.widget.VisTextButton$VisTextButtonStyle": {
          |    "default": { "focusBorder": "focus-border" }
          |  }
          |}""".stripMargin
      )
    )

    val style = skin.get("default", classOf[VisTextButton.VisTextButtonStyle])
    assert(
      style.focusBorder.isDefined,
      "VisTextButtonStyle.focusBorder must be populated from the JSON reference via the registered VisUI reader"
    )
  }

  test("ISS-608/731: an unregistered VisUI FQCN tag surfaces a load-time SgeError (not silently dropped)") {
    val skin = new Skin()

    // A FQCN that VisUISkinReaders never registers: the core Skin.load must fail
    // to resolve it (SgeError) instead of skipping the whole section.
    val ex = intercept[Throwable] {
      skin.load(
        skinFileOf(
          """{
            |  "com.kotcrab.vis.ui.widget.TotallyBogusStyle": {
            |    "default": { "focusBorder": "nope" }
            |  }
            |}""".stripMargin
        )
      )
    }
    assert(
      ex.isInstanceOf[SgeError],
      s"expected an SgeError for the unregistered tag, got ${ex.getClass.getName}: ${ex.getMessage}"
    )
    assert(
      Option(ex.getMessage).getOrElse("").contains("TotallyBogusStyle"),
      s"error must name the unresolved type; got: ${ex.getMessage}"
    )
  }
}
