/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
// TODO: test: decode a real .skin file (e.g. libgdx/tests/ uiskin.json) end-to-end through Skin.load
package sge
package scenes
package scene2d
package ui

import com.github.plokhotnyuk.jsoniter_scala.core.readFromString

import sge.Sge
import sge.SgeTestFixture
import sge.graphics.Color
import sge.scenes.scene2d.utils.BaseDrawable
import sge.scenes.scene2d.utils.Drawable
import sge.utils.Json
import sge.utils.Nullable

class SkinStyleReaderTest extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  private given codec: sge.utils.JsonCodec[Json] = {
    import hearth.kindlings.jsoniterjson.codec.JsonCodec.given
    summon[sge.utils.JsonCodec[Json]]
  }

  private def parseJson(s: String): Json = readFromString[Json](s)

  /** Creates a test Skin pre-loaded with named drawables and colors. Font fields cannot be tested without file I/O, so no fonts are registered.
    */
  private def testSkin(): Skin = {
    val skin = new Skin()
    // Register test drawables
    skin.add("btn-up", new BaseDrawable(), classOf[Drawable])
    skin.add("btn-down", new BaseDrawable(), classOf[Drawable])
    skin.add("btn-checked", new BaseDrawable(), classOf[Drawable])
    skin.add("default-round", new BaseDrawable(), classOf[Drawable])
    skin.add("default-round-down", new BaseDrawable(), classOf[Drawable])
    skin.add("bg", new BaseDrawable(), classOf[Drawable])
    skin.add("default-window", new BaseDrawable(), classOf[Drawable])
    skin.add("default-pane", new BaseDrawable(), classOf[Drawable])
    skin.add("sel", new BaseDrawable(), classOf[Drawable])
    skin.add("slider-bg", new BaseDrawable(), classOf[Drawable])
    skin.add("slider-knob", new BaseDrawable(), classOf[Drawable])
    skin.add("slider-knob-over", new BaseDrawable(), classOf[Drawable])
    skin.add("handle", new BaseDrawable(), classOf[Drawable])
    skin.add("tree-plus", new BaseDrawable(), classOf[Drawable])
    skin.add("tree-minus", new BaseDrawable(), classOf[Drawable])
    skin.add("cursor", new BaseDrawable(), classOf[Drawable])
    skin.add("touch-bg", new BaseDrawable(), classOf[Drawable])
    skin.add("touch-knob", new BaseDrawable(), classOf[Drawable])
    skin.add("cb-on", new BaseDrawable(), classOf[Drawable])
    skin.add("cb-off", new BaseDrawable(), classOf[Drawable])
    skin.add("img-up", new BaseDrawable(), classOf[Drawable])
    skin.add("img-down", new BaseDrawable(), classOf[Drawable])
    skin.add("hscroll", new BaseDrawable(), classOf[Drawable])
    skin.add("hscroll-knob", new BaseDrawable(), classOf[Drawable])
    skin.add("vscroll", new BaseDrawable(), classOf[Drawable])
    skin.add("vscroll-knob", new BaseDrawable(), classOf[Drawable])
    // Register test colors
    skin.add("white", new Color(1, 1, 1, 1), classOf[Color])
    skin.add("green", new Color(0, 1, 0, 1), classOf[Color])
    skin.add("red", new Color(1, 0, 0, 1), classOf[Color])
    skin
  }

  // Helper to resolve colors through the same logic Skin uses
  private def readColorVia(skin: Skin, json: Json): Color =
    SkinStyleReader.resolveColor(
      skin,
      json,
      j =>
        Skin.getField(j, "hex") match {
          case Some(Json.Str(hex)) => Color.valueOf(hex)
          case _                   =>
            val r = Skin.getField(j, "r").map(SkinStyleReader.resolveFloat).getOrElse(0f)
            val g = Skin.getField(j, "g").map(SkinStyleReader.resolveFloat).getOrElse(0f)
            val b = Skin.getField(j, "b").map(SkinStyleReader.resolveFloat).getOrElse(0f)
            val a = Skin.getField(j, "a").map(SkinStyleReader.resolveFloat).getOrElse(1f)
            new Color(r, g, b, a)
        }
    )

  // ---------------------------------------------------------------------------
  // Color parsing
  // ---------------------------------------------------------------------------

  test("color parsing — r/g/b/a components") {
    val skin  = testSkin()
    val json  = parseJson("""{ "r": 0, "g": 1, "b": 0, "a": 1 }""")
    val color = readColorVia(skin, json)
    assertEquals(color.r, 0f)
    assertEquals(color.g, 1f)
    assertEquals(color.b, 0f)
    assertEquals(color.a, 1f)
  }

  test("color parsing — hex notation") {
    val skin     = testSkin()
    val json     = parseJson("""{ "hex": "#00ff00ff" }""")
    val color    = readColorVia(skin, json)
    val expected = Color.valueOf("#00ff00ff")
    assertEquals(color.r, expected.r, 0.01f)
    assertEquals(color.g, expected.g, 0.01f)
    assertEquals(color.b, expected.b, 0.01f)
    assertEquals(color.a, expected.a, 0.01f)
  }

  test("color parsing — string reference") {
    val skin  = testSkin()
    val json  = parseJson(""""green"""")
    val color = readColorVia(skin, json)
    assertEquals(color.r, 0f)
    assertEquals(color.g, 1f)
    assertEquals(color.b, 0f)
    assertEquals(color.a, 1f)
  }

  // ---------------------------------------------------------------------------
  // ButtonStyle
  // ---------------------------------------------------------------------------

  test("ButtonStyle — basic fields") {
    val skin  = testSkin()
    val json  = parseJson("""{ "up": "btn-up", "down": "btn-down", "pressedOffsetY": 1.0 }""")
    val style = skin.readStyleObject(classOf[Button.ButtonStyle], json).asInstanceOf[Button.ButtonStyle]
    assert(style.up.isDefined)
    assert(style.down.isDefined)
    assertEquals(style.pressedOffsetY, 1.0f)
  }

  test("ButtonStyle — parent inheritance") {
    val skin         = testSkin()
    val defaultStyle = new Button.ButtonStyle()
    defaultStyle.up = Nullable(skin.getDrawable("btn-up"))
    defaultStyle.down = Nullable(skin.getDrawable("btn-down"))
    skin.add("default", defaultStyle, classOf[Button.ButtonStyle])

    val json  = parseJson("""{ "parent": "default", "checked": "btn-checked" }""")
    val style = skin.readStyleObject(classOf[Button.ButtonStyle], json).asInstanceOf[Button.ButtonStyle]
    assert(style.up.isDefined)
    assert(style.down.isDefined)
    assert(style.checked.isDefined)
  }

  // ---------------------------------------------------------------------------
  // TextButtonStyle — drawable and color fields (font skipped, no file I/O)
  // ---------------------------------------------------------------------------

  test("TextButtonStyle — inherits ButtonStyle drawable fields + color") {
    val skin  = testSkin()
    val json  = parseJson("""{ "up": "btn-up", "fontColor": "white" }""")
    val style = skin.readStyleObject(classOf[TextButton.TextButtonStyle], json).asInstanceOf[TextButton.TextButtonStyle]
    assert(style.up.isDefined)
    assert(style.fontColor.isDefined)
  }

  // ---------------------------------------------------------------------------
  // CheckBoxStyle (two-level inheritance)
  // ---------------------------------------------------------------------------

  test("CheckBoxStyle — inherits + own drawable fields") {
    val skin = testSkin()
    // Register parent with only drawable fields (no font)
    val parentStyle = new Button.ButtonStyle()
    parentStyle.up = Nullable(skin.getDrawable("btn-up"))
    skin.add("default", parentStyle, classOf[Button.ButtonStyle])

    val json  = parseJson("""{ "parent": "default", "checkboxOn": "cb-on", "checkboxOff": "cb-off" }""")
    val style = skin.readStyleObject(classOf[CheckBox.CheckBoxStyle], json).asInstanceOf[CheckBox.CheckBoxStyle]
    // Inherited from ButtonStyle
    assert(style.up.isDefined)
    // Own fields
    assert(style.checkboxOn.isDefined)
    assert(style.checkboxOff.isDefined)
  }

  // ---------------------------------------------------------------------------
  // LabelStyle — drawable + color (font skipped)
  // ---------------------------------------------------------------------------

  test("LabelStyle — color and background") {
    val skin  = testSkin()
    val json  = parseJson("""{ "fontColor": "white", "background": "bg" }""")
    val style = skin.readStyleObject(classOf[Label.LabelStyle], json).asInstanceOf[Label.LabelStyle]
    assert(style.fontColor.isDefined)
    assert(style.background.isDefined)
  }

  // ---------------------------------------------------------------------------
  // WindowStyle — drawable + color (font skipped)
  // ---------------------------------------------------------------------------

  test("WindowStyle — drawable and color fields") {
    val skin  = testSkin()
    val json  = parseJson("""{ "background": "bg", "titleFontColor": "white", "stageBackground": "default-pane" }""")
    val style = skin.readStyleObject(classOf[Window.WindowStyle], json).asInstanceOf[Window.WindowStyle]
    assert(style.titleFontColor.isDefined)
    assert(style.stageBackground.isDefined)
  }

  // ---------------------------------------------------------------------------
  // TextTooltipStyle — nested inline style + float
  // ---------------------------------------------------------------------------

  test("TextTooltipStyle — nested label style + float") {
    val skin = testSkin()
    // Nested LabelStyle without font
    val json  = parseJson("""{ "label": { "fontColor": "white" }, "background": "bg", "wrapWidth": 150 }""")
    val style = skin.readStyleObject(classOf[TextTooltip.TextTooltipStyle], json).asInstanceOf[TextTooltip.TextTooltipStyle]
    assert(style.label.fontColor.isDefined)
    assert(style.background.isDefined)
    assertEquals(style.wrapWidth, 150f)
  }

  // ---------------------------------------------------------------------------
  // SelectBoxStyle — nested inline styles
  // ---------------------------------------------------------------------------

  test("SelectBoxStyle — inline color + nested list style") {
    val skin = testSkin()
    val json = parseJson(
      """{
      "fontColor": { "r": 1, "g": 1, "b": 1, "a": 1 },
      "listStyle": { "selection": "sel", "fontColorSelected": "white", "fontColorUnselected": "white" }
    }"""
    )
    val style = skin.readStyleObject(classOf[SelectBox.SelectBoxStyle], json).asInstanceOf[SelectBox.SelectBoxStyle]
    assertEquals(style.fontColor.r, 1f)
    assertEquals(style.fontColor.g, 1f)
  }

  // ---------------------------------------------------------------------------
  // SliderStyle — inherits ProgressBarStyle
  // ---------------------------------------------------------------------------

  test("SliderStyle — inherits ProgressBarStyle") {
    val skin  = testSkin()
    val json  = parseJson("""{ "background": "slider-bg", "knob": "slider-knob", "knobOver": "slider-knob-over" }""")
    val style = skin.readStyleObject(classOf[Slider.SliderStyle], json).asInstanceOf[Slider.SliderStyle]
    assert(style.background.isDefined)
    assert(style.knobOver.isDefined)
  }

  // ---------------------------------------------------------------------------
  // Unknown fields silently ignored
  // ---------------------------------------------------------------------------

  test("unknown fields are silently ignored") {
    val skin  = testSkin()
    val json  = parseJson("""{ "unknownField": "value", "background": "bg" }""")
    val style = skin.readStyleObject(classOf[Label.LabelStyle], json).asInstanceOf[Label.LabelStyle]
    assert(style.background.isDefined)
  }

  // ---------------------------------------------------------------------------
  // All 17 style types can be created
  // ---------------------------------------------------------------------------

  test("all 17 registered style types can be created via registry") {
    assertEquals(SkinStyleReader.registry.size, 17)
    SkinStyleReader.registry.foreach { case (cls, reader) =>
      val obj = reader.create()
      assert(obj != null, s"create() returned null for ${cls.getSimpleName}") // scalastyle:ignore null
    }
  }

  // ---------------------------------------------------------------------------
  // Float field resolution
  // ---------------------------------------------------------------------------

  test("resolveFloat — numeric JSON") {
    assertEquals(SkinStyleReader.resolveFloat(parseJson("1.5")), 1.5f)
    assertEquals(SkinStyleReader.resolveFloat(parseJson("0")), 0f)
    assertEquals(SkinStyleReader.resolveFloat(parseJson("-3.14")), -3.14f, 0.01f)
  }

  test("resolveFloat — non-numeric returns 0") {
    assertEquals(SkinStyleReader.resolveFloat(parseJson(""""text"""")), 0f)
  }

  // ---------------------------------------------------------------------------
  // Drawable resolution
  // ---------------------------------------------------------------------------

  test("resolveNullableDrawable — known name") {
    val skin   = testSkin()
    val result = SkinStyleReader.resolveNullableDrawable(skin, parseJson(""""btn-up""""))
    assert(result.isDefined)
  }

  test("resolveNullableDrawable — unknown name returns empty") {
    val skin   = testSkin()
    val result = SkinStyleReader.resolveNullableDrawable(skin, parseJson(""""nonexistent""""))
    assert(result.isEmpty)
  }

  test("resolveNullableDrawable — non-string returns empty") {
    val skin   = testSkin()
    val result = SkinStyleReader.resolveNullableDrawable(skin, parseJson("42"))
    assert(result.isEmpty)
  }

  // ---------------------------------------------------------------------------
  // ImageButtonStyle
  // ---------------------------------------------------------------------------

  test("ImageButtonStyle — own + inherited fields") {
    val skin  = testSkin()
    val json  = parseJson("""{ "up": "btn-up", "imageUp": "img-up", "imageDown": "img-down" }""")
    val style = skin.readStyleObject(classOf[ImageButton.ImageButtonStyle], json).asInstanceOf[ImageButton.ImageButtonStyle]
    assert(style.up.isDefined)
    assert(style.imageUp.isDefined)
    assert(style.imageDown.isDefined)
  }

  // ---------------------------------------------------------------------------
  // ScrollPaneStyle
  // ---------------------------------------------------------------------------

  test("ScrollPaneStyle — all drawable fields") {
    val skin  = testSkin()
    val json  = parseJson("""{ "hScroll": "hscroll", "hScrollKnob": "hscroll-knob", "vScroll": "vscroll", "vScrollKnob": "vscroll-knob" }""")
    val style = skin.readStyleObject(classOf[ScrollPane.ScrollPaneStyle], json).asInstanceOf[ScrollPane.ScrollPaneStyle]
    assert(style.hScroll.isDefined)
    assert(style.hScrollKnob.isDefined)
    assert(style.vScroll.isDefined)
    assert(style.vScrollKnob.isDefined)
  }

  // ---------------------------------------------------------------------------
  // SplitPaneStyle
  // ---------------------------------------------------------------------------

  test("SplitPaneStyle — single field") {
    val skin  = testSkin()
    val json  = parseJson("""{ "handle": "handle" }""")
    val style = skin.readStyleObject(classOf[SplitPane.SplitPaneStyle], json).asInstanceOf[SplitPane.SplitPaneStyle]
    assert(style.handle != null) // scalastyle:ignore null
  }

  // ---------------------------------------------------------------------------
  // TouchpadStyle
  // ---------------------------------------------------------------------------

  test("TouchpadStyle — both fields") {
    val skin  = testSkin()
    val json  = parseJson("""{ "background": "touch-bg", "knob": "touch-knob" }""")
    val style = skin.readStyleObject(classOf[Touchpad.TouchpadStyle], json).asInstanceOf[Touchpad.TouchpadStyle]
    assert(style.background.isDefined)
    assert(style.knob.isDefined)
  }

  // ---------------------------------------------------------------------------
  // TreeStyle
  // ---------------------------------------------------------------------------

  test("TreeStyle — required + optional fields") {
    val skin  = testSkin()
    val json  = parseJson("""{ "plus": "tree-plus", "minus": "tree-minus", "background": "bg" }""")
    val style = skin.readStyleObject(classOf[Tree.TreeStyle], json).asInstanceOf[Tree.TreeStyle]
    assert(style.plus != null) // scalastyle:ignore null
    assert(style.minus != null) // scalastyle:ignore null
    assert(style.background.isDefined)
  }

  // ---------------------------------------------------------------------------
  // TextFieldStyle — drawable + color (font skipped)
  // ---------------------------------------------------------------------------

  test("TextFieldStyle — drawable and color fields") {
    val skin  = testSkin()
    val json  = parseJson("""{ "fontColor": "white", "background": "bg", "cursor": "cursor" }""")
    val style = skin.readStyleObject(classOf[TextField.TextFieldStyle], json).asInstanceOf[TextField.TextFieldStyle]
    assert(style.background.isDefined)
    assert(style.cursor.isDefined)
  }

  // ---------------------------------------------------------------------------
  // ProgressBarStyle
  // ---------------------------------------------------------------------------

  test("ProgressBarStyle — drawable fields") {
    val skin  = testSkin()
    val json  = parseJson("""{ "background": "slider-bg", "knob": "slider-knob", "knobBefore": "slider-knob-over" }""")
    val style = skin.readStyleObject(classOf[ProgressBar.ProgressBarStyle], json).asInstanceOf[ProgressBar.ProgressBarStyle]
    assert(style.background.isDefined)
    assert(style.knobBefore.isDefined)
  }

  // ---------------------------------------------------------------------------
  // ImageTextButtonStyle
  // ---------------------------------------------------------------------------

  test("ImageTextButtonStyle — image + button drawable fields") {
    val skin  = testSkin()
    val json  = parseJson("""{ "up": "btn-up", "imageUp": "img-up", "fontColor": "red" }""")
    val style = skin.readStyleObject(classOf[ImageTextButton.ImageTextButtonStyle], json).asInstanceOf[ImageTextButton.ImageTextButtonStyle]
    assert(style.up.isDefined)
    assert(style.imageUp.isDefined)
    assert(style.fontColor.isDefined)
  }

  // ---------------------------------------------------------------------------
  // ListStyle — color + drawable (font skipped)
  // ---------------------------------------------------------------------------

  test("ListStyle — color and drawable fields") {
    val skin  = testSkin()
    val json  = parseJson("""{ "selection": "sel", "fontColorSelected": "white", "fontColorUnselected": "green", "background": "bg" }""")
    val style = skin.readStyleObject(classOf[SgeList.ListStyle], json).asInstanceOf[SgeList.ListStyle]
    assertEquals(style.fontColorSelected.g, 1f)
    assertEquals(style.fontColorUnselected.r, 0f)
    assert(style.background.isDefined)
  }

  // ---------------------------------------------------------------------------
  // copyFrom
  // ---------------------------------------------------------------------------

  test("ButtonStyle copyFrom copies all fields") {
    val source = new Button.ButtonStyle()
    source.up = Nullable(new BaseDrawable())
    source.pressedOffsetY = 5f

    val target = new Button.ButtonStyle()
    SkinStyleReader.buttonStyleReader.copyFrom(source, target)
    assert(target.up.isDefined)
    assertEquals(target.pressedOffsetY, 5f)
  }

  test("TextButtonStyle copyFrom from ButtonStyle copies parent fields only") {
    val source = new Button.ButtonStyle()
    source.up = Nullable(new BaseDrawable())
    source.pressedOffsetX = 3f

    val target = new TextButton.TextButtonStyle()
    SkinStyleReader.textButtonStyleReader.copyFrom(source, target)
    assert(target.up.isDefined)
    assertEquals(target.pressedOffsetX, 3f)
    // TextButton-specific fields should remain default
    assert(target.fontColor.isEmpty)
  }
}
