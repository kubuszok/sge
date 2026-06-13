/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-529 (CLI 4-arg form silently ignores settingsFileName).
 *
 * TexturePacker.scala lines 1078-1082 handle the 4-arg CLI form but drop
 * args(3) ("Settings file parsing would go here -- for now just use
 * defaults"), and line 1101-1102 ("TODO: parse settings from args(3) when 4
 * args provided") always constructs `Settings()` defaults before dispatching
 * to `process(s, input, output, packFileName)` (line 1104).
 *
 * Original behaviour, original-src/libgdx/extensions/gdx-tools/src/com/
 * badlogic/gdx/tools/texturepacker/TexturePacker.java:
 *   - line 1011-1012: `case 4: settings = new Json().fromJson(Settings.class,
 *     new FileReader(args[3]));` — the settings JSON file is deserialized
 *     onto a Settings instance (field names = JSON keys), then the switch
 *     falls through to pick up packFileName/output/input,
 *   - line 1031: `process(settings, input, output, packFileName);` — the
 *     loaded settings drive the pack.
 * The same JSON-onto-Settings capability already exists in the port at
 * TexturePackerFileProcessor.merge (TexturePackerFileProcessor.scala line
 * 116), which maps e.g. "filterMin"/"filterMag" string values through
 * TextureFilter.valueOf (lines 139-150).
 *
 * Observable chosen: Settings.filterMin/filterMag. Defaults are
 * TextureFilter.Nearest (TexturePacker.scala lines 974-975) and the default
 * legacy atlas writer emits them verbatim into the pack file text:
 * writePageLegacy, TexturePacker.scala line 535:
 *   `writer.write("filter: " + settings.filterMin + ", " + settings.filterMag + "\n")`
 * so a settings file overriding filterMin/filterMag MUST change the
 * "filter:" line of the generated .atlas. Today the 4-arg form produces
 * "filter: Nearest, Nearest" no matter what the settings file says.
 *
 * The tests call TexturePacker.main directly: with 1-4 arguments main never
 * reaches the System.exit(0) in the default usage branch (line 1092-1094),
 * so invoking it is safe and exercises exactly the code path that drops the
 * settings file.
 *
 * The settings JSON deliberately lives OUTSIDE the input directory so it
 * cannot be picked up by TexturePackerFileProcessor's per-directory
 * pack.json discovery (TexturePackerFileProcessor.scala lines 79-81) — only
 * the CLI argument can deliver it.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified
 * by the fixer: they encode the original CLI semantics, not the port's.
 */
package sge
package tools
package texturepacker

import java.awt.image.BufferedImage
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.imageio.ImageIO

class PackerSettingsFileRedSuite extends munit.FunSuite {

  // commonSettings forks test JVMs with -XstartOnFirstThread on macOS; AWT's
  // LWCToolkit.initAppkit then deadlocks (AWTStarter waits forever for AppKit).
  // The packer is pure offscreen BufferedImage/ImageIO work, so force headless
  // BEFORE any java.awt class triggers toolkit initialization.
  System.setProperty("java.awt.headless", "true")

  /** Writes a tiny opaque PNG — opaque so the default ignoreBlankImages=true cannot drop it. */
  private def writeTinyPng(file: File): Unit = {
    val image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
    var y     = 0
    while (y < 8) {
      var x = 0
      while (x < 8) {
        image.setRGB(x, y, 0xff2060c0) // fully opaque
        x += 1
      }
      y += 1
    }
    assert(ImageIO.write(image, "png", file), s"could not write fixture PNG $file")
  }

  /** Fresh fixture: root/input/sprite.png + root/output, settings file kept beside (not inside) input. */
  private def makeFixture(name: String): (File, File, File) = {
    val root     = Files.createTempDirectory(name).toFile
    val inputDir = new File(root, "input")
    assert(inputDir.mkdirs(), s"could not create $inputDir")
    writeTinyPng(new File(inputDir, "sprite.png"))
    val outputDir = new File(root, "output")
    (root, inputDir, outputDir)
  }

  private def readAtlas(outputDir: File): String = {
    val atlasFile = new File(outputDir, "test.atlas")
    assert(atlasFile.exists(), s"pack file ${atlasFile.getAbsolutePath()} was not written")
    new String(Files.readAllBytes(atlasFile.toPath()), StandardCharsets.UTF_8)
  }

  test("ISS-529 red: CLI 4-arg form applies the settings file (filterMin/filterMag reach the atlas)") {
    val (root, inputDir, outputDir) = makeFixture("iss529-red")
    // Same key/value shape upstream consumes via new Json().fromJson(Settings.class, new FileReader(args[3]))
    // (TexturePacker.java:1012) and the port already parses in TexturePackerFileProcessor.merge
    // (TexturePackerFileProcessor.scala:139-150): enum constant names of Texture.TextureFilter.
    val settingsFile = new File(root, "packer-settings.json")
    Files.write(
      settingsFile.toPath(),
      """{"filterMin": "MipMapLinearLinear", "filterMag": "Linear"}""".getBytes(StandardCharsets.UTF_8)
    )

    TexturePacker.main(
      Array(
        inputDir.getAbsolutePath(),
        outputDir.getAbsolutePath(),
        "test.atlas",
        settingsFile.getAbsolutePath()
      )
    )

    val atlas = readAtlas(outputDir)
    assert(
      atlas.contains("filter: MipMapLinearLinear, Linear"),
      s"settings file $settingsFile was ignored by the 4-arg CLI form: expected the .atlas page header " +
        s"to carry the overridden 'filter: MipMapLinearLinear, Linear' (TexturePacker.java:1012 loads " +
        s"args[3] into Settings; writePageLegacy emits settings.filterMin/filterMag), but the atlas was:\n$atlas"
    )
  }

  test("green control: CLI 3-arg form packs with defaults (filter: Nearest, Nearest)") {
    val (_, inputDir, outputDir) = makeFixture("iss529-green")

    TexturePacker.main(
      Array(
        inputDir.getAbsolutePath(),
        outputDir.getAbsolutePath(),
        "test.atlas"
      )
    )

    val atlas = readAtlas(outputDir)
    assert(
      atlas.contains("filter: Nearest, Nearest"),
      s"3-arg CLI form should pack with default Settings (filterMin=filterMag=Nearest), but the atlas was:\n$atlas"
    )
    assert(
      atlas.contains("sprite"),
      s"packed atlas should contain the input region 'sprite', but was:\n$atlas"
    )
  }
}
