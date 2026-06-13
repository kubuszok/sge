/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
// RED tests for ISS-534 — Skin JSON style loading swallows errors / registry closed.
//
// These pin the ORIGINAL libGDX Skin.java contract for the JSON skin loader:
//
//  (1) A style that references a NON-EXISTENT resource (e.g. a LabelStyle naming
//      a font that is not defined) must raise a clear LOAD-time error naming the
//      style/resource — NOT silently load an uninitialized style that later NPEs
//      in draw(). libGDX: Skin.readNamedObjects (Skin.java ~530-544) wraps the
//      GdxRuntimeException thrown by get(name, type) (Skin.java ~152-166, reached
//      via readValue at ~480-485) into a SerializationException
//      ("Error reading <Type>: <name>"), and Skin.load (~102-108) rethrows it.
//
//  (2) An UNKNOWN top-level style TYPE NAME surfaces an error rather than being
//      silently skipped. libGDX: the Skin ReadOnlySerializer (Skin.java ~516-528)
//      resolves the type via json.getClass(name) then ClassReflection.forName,
//      and throws a SerializationException (wrapping ReflectionException) when the
//      name cannot be resolved — it does not drop the section.
//
//  (3) getJsonClassTags returns a MUTABLE map whose mutations are honored by the
//      subsequent load(). libGDX: getJsonClassTags (Skin.java ~622-627) returns the
//      live jsonClassTags ObjectMap; the doc says "The map can be modified before
//      calling load(FileHandle)" and getJsonLoader (~616-617) installs every entry
//      as a class tag.
//
//  (4) A CUSTOM widget style class can be registered (through the public class-tag
//      registration path) and loaded from JSON. libGDX's reflection-based loader
//      lets any class on the classpath be named in skin JSON; the SGE port must
//      expose an OPEN registration seam so custom widget styles load, not a closed
//      registry that drops them.
//
// Each test drives the REAL Skin.load(FileHandle) path over an in-memory skin JSON
// written to a temp file (FileType.Absolute), mirroring the libGDX skin-loading
// suites. They are expected to FAIL on the current (broken) code.
package sge
package scenes
package scene2d
package ui

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files as JFiles

import sge.Sge
import sge.SgeTestFixture
import sge.files.FileHandle
import sge.files.FileType
import sge.graphics.Color
import sge.scenes.scene2d.utils.BaseDrawable
import sge.scenes.scene2d.utils.Drawable
import sge.utils.Json
import sge.utils.SgeError
import lowlevel.Nullable

class SkinJsonStyleLoadingRedTest extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  /** Writes the given skin JSON to a temp file and returns an Absolute FileHandle over it, so that Skin.load reads the real bytes through the normal file path. */
  private def skinFileOf(json: String): FileHandle = {
    val tmp = File.createTempFile("iss534-skin", ".json")
    tmp.deleteOnExit()
    JFiles.write(tmp.toPath, json.getBytes(StandardCharsets.UTF_8))
    new FileHandle(tmp, FileType.Absolute)
  }

  // ---------------------------------------------------------------------------
  // (1) Missing referenced resource -> clear LOAD-time error (not a later NPE,
  //     not silently swallowed).
  //     Encodes Skin.java ~480-485 (readValue string lookup), ~152-166 (get
  //     throws when missing), ~530-544 (readNamedObjects wraps), ~102-108 (load
  //     rethrows).
  // ---------------------------------------------------------------------------

  test("(1) a style referencing a missing resource throws a clear load-time error") {
    // A LabelStyle whose `font` names a BitmapFont that was never defined in the
    // skin. libGDX raises at LOAD time; the current port swallows the lookup
    // failure in SkinStyleReader.withFont and leaves LabelStyle.font
    // uninitialized, deferring to a draw-time NPE.
    val skinFile = skinFileOf(
      """{
        |  "LabelStyle": {
        |    "default": { "font": "no-such-font" }
        |  }
        |}""".stripMargin
    )

    val ex = intercept[Throwable] {
      new Skin(skinFile)
    }
    // Must be a real error surfaced from load (an SgeError, matching the port's
    // GdxRuntimeException/SerializationException equivalent), and the message
    // must identify the offending style/resource — not an unrelated NPE.
    assert(
      ex.isInstanceOf[SgeError],
      s"expected an SgeError raised at load, got ${ex.getClass.getName}: ${ex.getMessage}"
    )
    val msg = Option(ex.getMessage).getOrElse("") + Option(ex.getCause).map(c => " | " + c.getMessage).getOrElse("")
    assert(
      msg.contains("no-such-font") || msg.toLowerCase.contains("font") || msg.contains("default"),
      s"error must name the missing resource / style; got: $msg"
    )
  }

  // ---------------------------------------------------------------------------
  // (2) Unknown top-level style TYPE NAME -> error, not a silent skip.
  //     Encodes Skin.java ~516-528 (getClass/forName + SerializationException).
  // ---------------------------------------------------------------------------

  test("(2) an unknown style type name surfaces an error rather than being silently skipped") {
    // `TotallyUnknownStyle` is not a registered type. libGDX would fail to resolve
    // it (ReflectionException -> SerializationException). The current port hits
    // `case None => ()` in Skin.load and silently drops the whole section.
    val skinFile = skinFileOf(
      """{
        |  "TotallyUnknownStyle": {
        |    "default": { "foo": "bar" }
        |  }
        |}""".stripMargin
    )

    val ex = intercept[Throwable] {
      new Skin(skinFile)
    }
    assert(
      ex.isInstanceOf[SgeError],
      s"expected an SgeError for unknown type, got ${ex.getClass.getName}: ${ex.getMessage}"
    )
    val msg = Option(ex.getMessage).getOrElse("") + Option(ex.getCause).map(c => " | " + c.getMessage).getOrElse("")
    assert(
      msg.contains("TotallyUnknownStyle"),
      s"error must name the unknown type; got: $msg"
    )
  }

  // ---------------------------------------------------------------------------
  // (3) getJsonClassTags returns a MUTABLE map and its mutations are honored.
  //     Encodes Skin.java ~622-627 (live jsonClassTags map, doc says modifiable)
  //     and ~616-617 (entries installed as class tags before load).
  // ---------------------------------------------------------------------------

  test("(3) getJsonClassTags is mutable and a registered tag is retained") {
    val skin = new Skin()

    // The map returned by getJsonClassTags must be modifiable, exactly as the
    // libGDX doc promises ("The map can be modified before calling
    // load(FileHandle)"). The current port returns a fresh *immutable* Scala Map
    // (classTagMap ++ extensionClassTags), so mutations cannot be applied and are
    // never observed. We probe through the mutable interface at runtime so the
    // suite compiles against the current code yet fails RED on it.
    val customClass: Class[?] = classOf[CustomStyle]
    val tags:        Any      = skin.getJsonClassTags
    tags match {
      case _: scala.collection.mutable.Map[?, ?] =>
        val m = tags.asInstanceOf[scala.collection.mutable.Map[String, Class[?]]]
        m.put("MyCustomStyle", customClass)
        assertEquals(
          skin.getJsonClassTags.get("MyCustomStyle").map(_.getName),
          Some(classOf[CustomStyle].getName),
          "custom tag put into getJsonClassTags must be retained / visible on read-back"
        )
      case other =>
        fail(s"getJsonClassTags must return a mutable map (libGDX: live jsonClassTags), got ${other.getClass.getName}")
    }
  }

  // ---------------------------------------------------------------------------
  // (4) A CUSTOM widget style class registered through the public class-tag path
  //     can be loaded from JSON (open registry).
  //     Encodes libGDX's reflection-based open loading (~516-528, ~480-485) that
  //     the SGE port must keep open for out-of-tree style classes.
  // ---------------------------------------------------------------------------

  test("(4) a custom widget style class can be registered and loaded from JSON") {
    val skin = new Skin()
    skin.add("bg", new BaseDrawable(), classOf[Drawable])
    skin.add("white", new Color(1, 1, 1, 1), classOf[Color])

    // Register the custom style's reader and its JSON tag name through the public
    // registration surface, then load a skin JSON that uses it. The class-tag
    // registration must go through getJsonClassTags (the libGDX public path); on
    // the current closed registry that map is immutable, so the registration is
    // lost and the section is dropped.
    SkinStyleReader.register(Map(classOf[CustomStyle] -> CustomStyle.reader))
    val customClass: Class[?] = classOf[CustomStyle]
    val tags:        Any      = skin.getJsonClassTags
    tags match {
      case _: scala.collection.mutable.Map[?, ?] =>
        val m = tags.asInstanceOf[scala.collection.mutable.Map[String, Class[?]]]
        m.put("MyCustomStyle", customClass)
      case other =>
        fail(s"getJsonClassTags must return a mutable map to register a custom style, got ${other.getClass.getName}")
    }

    val skinFile = skinFileOf(
      """{
        |  "MyCustomStyle": {
        |    "default": { "background": "bg", "tint": "white" }
        |  }
        |}""".stripMargin
    )
    skin.load(skinFile)

    val loaded = skin.get("default", classOf[CustomStyle])
    assert(loaded.background.isDefined, "custom style background must load from JSON")
    assert(loaded.tint.isDefined, "custom style tint must load from JSON")
  }
}

/** Minimal out-of-tree widget style + reader used only by the ISS-534 red tests. */
final class CustomStyle {
  var background: Nullable[Drawable] = Nullable.empty
  var tint:       Nullable[Color]    = Nullable.empty
}

object CustomStyle {
  val reader: SkinStyleReader[CustomStyle] = new SkinStyleReader[CustomStyle] {
    def create():                                   CustomStyle = new CustomStyle()
    def copyFrom(source: Any, target: CustomStyle): Unit        = source match {
      case s: CustomStyle =>
        target.background = s.background
        target.tint = s.tint
      case _ => ()
    }
    def setField(obj: CustomStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background" => obj.background = SkinStyleReader.resolveNullableDrawable(skin, json)
      case "tint"       => obj.tint = SkinStyleReader.resolveNullableColor(skin, json, readColor)
      case _            => ()
    }
  }
}
