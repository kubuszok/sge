/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces Java reflection-based style field setting in Skin.scala)
 *   Convention: type class with explicit implementations for each style type
 *   Idiom: split packages
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.Color
import sge.graphics.g2d.BitmapFont
import sge.scenes.scene2d.utils.Drawable
import sge.utils.Json
import sge.utils.Nullable
import sge.utils.SgeError

/** Type class for reading widget style objects from skin JSON without reflection.
  *
  * Each registered style type has a given instance that knows how to:
  *   - create a new default instance
  *   - copy fields from a parent style (for JSON "parent" inheritance)
  *   - set individual fields from JSON values
  *
  * @tparam T
  *   the style type
  */
trait SkinStyleReader[T] {

  /** Creates a new default-initialized instance. */
  def create(): T

  /** Copies all matching fields from source to target (for "parent" style inheritance). */
  def copyFrom(source: Any, target: T): Unit

  /** Sets a single named field on the style object from a JSON value.
    * @param obj
    *   the style instance to modify
    * @param name
    *   the JSON field name
    * @param json
    *   the JSON value
    * @param skin
    *   the skin for resource lookups
    * @param readColor
    *   callback to parse inline color JSON
    * @param readStyle
    *   callback to parse nested inline style JSON
    */
  def setField(obj: T, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit
}

object SkinStyleReader {

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /** Resolves a Nullable[Drawable] from JSON (string name lookup). */
  private[ui] def resolveNullableDrawable(skin: Skin, json: Json): Nullable[Drawable] = json match {
    case Json.Str(name) =>
      try Nullable(skin.getDrawable(name))
      catch { case _: SgeError => Nullable.empty }
    case _ => Nullable.empty
  }

  /** Sets a non-nullable Drawable field from JSON. No-op if lookup fails. */
  private[ui] inline def withDrawable(skin: Skin, json: Json)(setter: Drawable => Unit): Unit = json match {
    case Json.Str(name) =>
      try setter(skin.getDrawable(name))
      catch { case _: SgeError => () }
    case _ => ()
  }

  /** Resolves a Nullable[Color] from JSON (string reference or inline object). */
  private[ui] def resolveNullableColor(skin: Skin, json: Json, readColor: Json => Color): Nullable[Color] =
    try Nullable(resolveColor(skin, json, readColor))
    catch { case _: SgeError => Nullable.empty }

  /** Resolves a Color from JSON (string reference or inline object). */
  private[ui] def resolveColor(skin: Skin, json: Json, readColor: Json => Color): Color = json match {
    case Json.Str(name) => skin.get[Color](name)
    case _              => readColor(json)
  }

  /** Sets a non-nullable Color field from JSON. No-op if lookup fails. */
  private[ui] inline def withColor(skin: Skin, json: Json, readColor: Json => Color)(setter: Color => Unit): Unit =
    try setter(resolveColor(skin, json, readColor))
    catch { case _: SgeError => () }

  /** Sets a non-nullable BitmapFont field from JSON. No-op if lookup fails. */
  private[ui] inline def withFont(skin: Skin, json: Json)(setter: BitmapFont => Unit): Unit = json match {
    case Json.Str(name) =>
      try setter(skin.getFont(name))
      catch { case _: SgeError => () }
    case _ => ()
  }

  /** Resolves a Nullable[BitmapFont] from JSON. */
  private[ui] def resolveNullableFont(skin: Skin, json: Json): Nullable[BitmapFont] = json match {
    case Json.Str(name) =>
      try Nullable(skin.getFont(name))
      catch { case _: SgeError => Nullable.empty }
    case _ => Nullable.empty
  }

  /** Resolves a Float from JSON. */
  private[ui] def resolveFloat(json: Json): Float = json match {
    case Json.Num(n) => n.toDouble.map(_.toFloat).getOrElse(0f)
    case _           => 0f
  }

  /** Resolves a nested style from JSON (string reference or inline object). */
  private[ui] def resolveStyle[S](skin: Skin, json: Json, styleClass: Class[S], readStyle: (Class[?], Json) => Any): S = json match {
    case Json.Str(name) => skin.get(name, styleClass)
    case _              => readStyle(styleClass, json).asInstanceOf[S]
  }

  /** Sets a nested style field. No-op if lookup fails. */
  private[ui] inline def withStyle[S](skin: Skin, json: Json, styleClass: Class[S], readStyle: (Class[?], Json) => Any)(setter: S => Unit): Unit =
    try setter(resolveStyle(skin, json, styleClass, readStyle))
    catch { case _: SgeError => () }

  // ---------------------------------------------------------------------------
  // 1. ButtonStyle
  // ---------------------------------------------------------------------------

  given buttonStyleReader: SkinStyleReader[Button.ButtonStyle] with {

    def create(): Button.ButtonStyle = Button.ButtonStyle()

    def copyFrom(source: Any, target: Button.ButtonStyle): Unit = source match {
      case s: Button.ButtonStyle =>
        target.up = s.up
        target.down = s.down
        target.over = s.over
        target.focused = s.focused
        target.disabled = s.disabled
        target.checked = s.checked
        target.checkedOver = s.checkedOver
        target.checkedDown = s.checkedDown
        target.checkedFocused = s.checkedFocused
        target.pressedOffsetX = s.pressedOffsetX
        target.pressedOffsetY = s.pressedOffsetY
        target.unpressedOffsetX = s.unpressedOffsetX
        target.unpressedOffsetY = s.unpressedOffsetY
        target.checkedOffsetX = s.checkedOffsetX
        target.checkedOffsetY = s.checkedOffsetY
      case _ => ()
    }

    def setField(obj: Button.ButtonStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "up"               => obj.up = resolveNullableDrawable(skin, json)
      case "down"             => obj.down = resolveNullableDrawable(skin, json)
      case "over"             => obj.over = resolveNullableDrawable(skin, json)
      case "focused"          => obj.focused = resolveNullableDrawable(skin, json)
      case "disabled"         => obj.disabled = resolveNullableDrawable(skin, json)
      case "checked"          => obj.checked = resolveNullableDrawable(skin, json)
      case "checkedOver"      => obj.checkedOver = resolveNullableDrawable(skin, json)
      case "checkedDown"      => obj.checkedDown = resolveNullableDrawable(skin, json)
      case "checkedFocused"   => obj.checkedFocused = resolveNullableDrawable(skin, json)
      case "pressedOffsetX"   => obj.pressedOffsetX = resolveFloat(json)
      case "pressedOffsetY"   => obj.pressedOffsetY = resolveFloat(json)
      case "unpressedOffsetX" => obj.unpressedOffsetX = resolveFloat(json)
      case "unpressedOffsetY" => obj.unpressedOffsetY = resolveFloat(json)
      case "checkedOffsetX"   => obj.checkedOffsetX = resolveFloat(json)
      case "checkedOffsetY"   => obj.checkedOffsetY = resolveFloat(json)
      case _                  => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 2. TextButtonStyle
  // ---------------------------------------------------------------------------

  given textButtonStyleReader: SkinStyleReader[TextButton.TextButtonStyle] with {

    def create(): TextButton.TextButtonStyle = TextButton.TextButtonStyle()

    def copyFrom(source: Any, target: TextButton.TextButtonStyle): Unit = source match {
      case s: TextButton.TextButtonStyle =>
        target.font = s.font
        target.fontColor = s.fontColor
        target.downFontColor = s.downFontColor
        target.overFontColor = s.overFontColor
        target.focusedFontColor = s.focusedFontColor
        target.disabledFontColor = s.disabledFontColor
        target.checkedFontColor = s.checkedFontColor
        target.checkedDownFontColor = s.checkedDownFontColor
        target.checkedOverFontColor = s.checkedOverFontColor
        target.checkedFocusedFontColor = s.checkedFocusedFontColor
        buttonStyleReader.copyFrom(s, target)
      case s: Button.ButtonStyle =>
        buttonStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: TextButton.TextButtonStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "font"                    => withFont(skin, json)(obj.font = _)
      case "fontColor"               => obj.fontColor = resolveNullableColor(skin, json, readColor)
      case "downFontColor"           => obj.downFontColor = resolveNullableColor(skin, json, readColor)
      case "overFontColor"           => obj.overFontColor = resolveNullableColor(skin, json, readColor)
      case "focusedFontColor"        => obj.focusedFontColor = resolveNullableColor(skin, json, readColor)
      case "disabledFontColor"       => obj.disabledFontColor = resolveNullableColor(skin, json, readColor)
      case "checkedFontColor"        => obj.checkedFontColor = resolveNullableColor(skin, json, readColor)
      case "checkedDownFontColor"    => obj.checkedDownFontColor = resolveNullableColor(skin, json, readColor)
      case "checkedOverFontColor"    => obj.checkedOverFontColor = resolveNullableColor(skin, json, readColor)
      case "checkedFocusedFontColor" => obj.checkedFocusedFontColor = resolveNullableColor(skin, json, readColor)
      case _                         => buttonStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  // ---------------------------------------------------------------------------
  // 3. CheckBoxStyle
  // ---------------------------------------------------------------------------

  given checkBoxStyleReader: SkinStyleReader[CheckBox.CheckBoxStyle] with {

    def create(): CheckBox.CheckBoxStyle = CheckBox.CheckBoxStyle()

    def copyFrom(source: Any, target: CheckBox.CheckBoxStyle): Unit = source match {
      case s: CheckBox.CheckBoxStyle =>
        target.checkboxOn = s.checkboxOn
        target.checkboxOff = s.checkboxOff
        target.checkboxOnOver = s.checkboxOnOver
        target.checkboxOver = s.checkboxOver
        target.checkboxOnDisabled = s.checkboxOnDisabled
        target.checkboxOffDisabled = s.checkboxOffDisabled
        textButtonStyleReader.copyFrom(s, target)
      case s: TextButton.TextButtonStyle =>
        textButtonStyleReader.copyFrom(s, target)
      case s: Button.ButtonStyle =>
        buttonStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: CheckBox.CheckBoxStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "checkboxOn"          => obj.checkboxOn = resolveNullableDrawable(skin, json)
      case "checkboxOff"         => obj.checkboxOff = resolveNullableDrawable(skin, json)
      case "checkboxOnOver"      => obj.checkboxOnOver = resolveNullableDrawable(skin, json)
      case "checkboxOver"        => obj.checkboxOver = resolveNullableDrawable(skin, json)
      case "checkboxOnDisabled"  => obj.checkboxOnDisabled = resolveNullableDrawable(skin, json)
      case "checkboxOffDisabled" => obj.checkboxOffDisabled = resolveNullableDrawable(skin, json)
      case _                     => textButtonStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  // ---------------------------------------------------------------------------
  // 4. ImageButtonStyle
  // ---------------------------------------------------------------------------

  given imageButtonStyleReader: SkinStyleReader[ImageButton.ImageButtonStyle] with {

    def create(): ImageButton.ImageButtonStyle = ImageButton.ImageButtonStyle()

    def copyFrom(source: Any, target: ImageButton.ImageButtonStyle): Unit = source match {
      case s: ImageButton.ImageButtonStyle =>
        target.imageUp = s.imageUp
        target.imageDown = s.imageDown
        target.imageOver = s.imageOver
        target.imageDisabled = s.imageDisabled
        target.imageChecked = s.imageChecked
        target.imageCheckedDown = s.imageCheckedDown
        target.imageCheckedOver = s.imageCheckedOver
        buttonStyleReader.copyFrom(s, target)
      case s: Button.ButtonStyle =>
        buttonStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: ImageButton.ImageButtonStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "imageUp"          => obj.imageUp = resolveNullableDrawable(skin, json)
      case "imageDown"        => obj.imageDown = resolveNullableDrawable(skin, json)
      case "imageOver"        => obj.imageOver = resolveNullableDrawable(skin, json)
      case "imageDisabled"    => obj.imageDisabled = resolveNullableDrawable(skin, json)
      case "imageChecked"     => obj.imageChecked = resolveNullableDrawable(skin, json)
      case "imageCheckedDown" => obj.imageCheckedDown = resolveNullableDrawable(skin, json)
      case "imageCheckedOver" => obj.imageCheckedOver = resolveNullableDrawable(skin, json)
      case _                  => buttonStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  // ---------------------------------------------------------------------------
  // 5. ImageTextButtonStyle
  // ---------------------------------------------------------------------------

  given imageTextButtonStyleReader: SkinStyleReader[ImageTextButton.ImageTextButtonStyle] with {

    def create(): ImageTextButton.ImageTextButtonStyle = ImageTextButton.ImageTextButtonStyle()

    def copyFrom(source: Any, target: ImageTextButton.ImageTextButtonStyle): Unit = source match {
      case s: ImageTextButton.ImageTextButtonStyle =>
        target.imageUp = s.imageUp
        target.imageDown = s.imageDown
        target.imageOver = s.imageOver
        target.imageDisabled = s.imageDisabled
        target.imageChecked = s.imageChecked
        target.imageCheckedDown = s.imageCheckedDown
        target.imageCheckedOver = s.imageCheckedOver
        textButtonStyleReader.copyFrom(s, target)
      case s: TextButton.TextButtonStyle =>
        textButtonStyleReader.copyFrom(s, target)
      case s: Button.ButtonStyle =>
        buttonStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: ImageTextButton.ImageTextButtonStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "imageUp"          => obj.imageUp = resolveNullableDrawable(skin, json)
      case "imageDown"        => obj.imageDown = resolveNullableDrawable(skin, json)
      case "imageOver"        => obj.imageOver = resolveNullableDrawable(skin, json)
      case "imageDisabled"    => obj.imageDisabled = resolveNullableDrawable(skin, json)
      case "imageChecked"     => obj.imageChecked = resolveNullableDrawable(skin, json)
      case "imageCheckedDown" => obj.imageCheckedDown = resolveNullableDrawable(skin, json)
      case "imageCheckedOver" => obj.imageCheckedOver = resolveNullableDrawable(skin, json)
      case _                  => textButtonStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  // ---------------------------------------------------------------------------
  // 6. LabelStyle
  // ---------------------------------------------------------------------------

  given labelStyleReader: SkinStyleReader[Label.LabelStyle] with {

    def create(): Label.LabelStyle = Label.LabelStyle()

    def copyFrom(source: Any, target: Label.LabelStyle): Unit = source match {
      case s: Label.LabelStyle =>
        target.font = s.font
        target.fontColor = s.fontColor
        target.background = s.background
      case _ => ()
    }

    def setField(obj: Label.LabelStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "font"       => withFont(skin, json)(obj.font = _)
      case "fontColor"  => obj.fontColor = resolveNullableColor(skin, json, readColor)
      case "background" => obj.background = resolveNullableDrawable(skin, json)
      case _            => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 7. ListStyle
  // ---------------------------------------------------------------------------

  given listStyleReader: SkinStyleReader[SgeList.ListStyle] with {

    def create(): SgeList.ListStyle = SgeList.ListStyle()

    def copyFrom(source: Any, target: SgeList.ListStyle): Unit = source match {
      case s: SgeList.ListStyle =>
        target.font = s.font
        target.fontColorSelected.set(s.fontColorSelected)
        target.fontColorUnselected.set(s.fontColorUnselected)
        target.selection = s.selection
        target.down = s.down
        target.over = s.over
        target.background = s.background
      case _ => ()
    }

    def setField(obj: SgeList.ListStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "font"                => withFont(skin, json)(obj.font = _)
      case "fontColorSelected"   => withColor(skin, json, readColor)(obj.fontColorSelected.set)
      case "fontColorUnselected" => withColor(skin, json, readColor)(obj.fontColorUnselected.set)
      case "selection"           => withDrawable(skin, json)(obj.selection = _)
      case "down"                => obj.down = resolveNullableDrawable(skin, json)
      case "over"                => obj.over = resolveNullableDrawable(skin, json)
      case "background"          => obj.background = resolveNullableDrawable(skin, json)
      case _                     => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 8. ProgressBarStyle
  // ---------------------------------------------------------------------------

  given progressBarStyleReader: SkinStyleReader[ProgressBar.ProgressBarStyle] with {

    def create(): ProgressBar.ProgressBarStyle = ProgressBar.ProgressBarStyle()

    def copyFrom(source: Any, target: ProgressBar.ProgressBarStyle): Unit = source match {
      case s: ProgressBar.ProgressBarStyle =>
        target.background = s.background
        target.disabledBackground = s.disabledBackground
        target.knob = s.knob
        target.disabledKnob = s.disabledKnob
        target.knobBefore = s.knobBefore
        target.disabledKnobBefore = s.disabledKnobBefore
        target.knobAfter = s.knobAfter
        target.disabledKnobAfter = s.disabledKnobAfter
      case _ => ()
    }

    def setField(obj: ProgressBar.ProgressBarStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background"         => obj.background = resolveNullableDrawable(skin, json)
      case "disabledBackground" => obj.disabledBackground = resolveNullableDrawable(skin, json)
      case "knob"               => withDrawable(skin, json)(obj.knob = _)
      case "disabledKnob"       => obj.disabledKnob = resolveNullableDrawable(skin, json)
      case "knobBefore"         => obj.knobBefore = resolveNullableDrawable(skin, json)
      case "disabledKnobBefore" => obj.disabledKnobBefore = resolveNullableDrawable(skin, json)
      case "knobAfter"          => obj.knobAfter = resolveNullableDrawable(skin, json)
      case "disabledKnobAfter"  => obj.disabledKnobAfter = resolveNullableDrawable(skin, json)
      case _                    => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 9. SliderStyle
  // ---------------------------------------------------------------------------

  given sliderStyleReader: SkinStyleReader[Slider.SliderStyle] with {

    def create(): Slider.SliderStyle = Slider.SliderStyle()

    def copyFrom(source: Any, target: Slider.SliderStyle): Unit = source match {
      case s: Slider.SliderStyle =>
        target.backgroundOver = s.backgroundOver
        target.backgroundDown = s.backgroundDown
        target.knobOver = s.knobOver
        target.knobDown = s.knobDown
        target.knobBeforeOver = s.knobBeforeOver
        target.knobBeforeDown = s.knobBeforeDown
        target.knobAfterOver = s.knobAfterOver
        target.knobAfterDown = s.knobAfterDown
        progressBarStyleReader.copyFrom(s, target)
      case s: ProgressBar.ProgressBarStyle =>
        progressBarStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: Slider.SliderStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "backgroundOver" => obj.backgroundOver = resolveNullableDrawable(skin, json)
      case "backgroundDown" => obj.backgroundDown = resolveNullableDrawable(skin, json)
      case "knobOver"       => obj.knobOver = resolveNullableDrawable(skin, json)
      case "knobDown"       => obj.knobDown = resolveNullableDrawable(skin, json)
      case "knobBeforeOver" => obj.knobBeforeOver = resolveNullableDrawable(skin, json)
      case "knobBeforeDown" => obj.knobBeforeDown = resolveNullableDrawable(skin, json)
      case "knobAfterOver"  => obj.knobAfterOver = resolveNullableDrawable(skin, json)
      case "knobAfterDown"  => obj.knobAfterDown = resolveNullableDrawable(skin, json)
      case _                => progressBarStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  // ---------------------------------------------------------------------------
  // 10. ScrollPaneStyle
  // ---------------------------------------------------------------------------

  given scrollPaneStyleReader: SkinStyleReader[ScrollPane.ScrollPaneStyle] with {

    def create(): ScrollPane.ScrollPaneStyle = ScrollPane.ScrollPaneStyle()

    def copyFrom(source: Any, target: ScrollPane.ScrollPaneStyle): Unit = source match {
      case s: ScrollPane.ScrollPaneStyle =>
        target.background = s.background
        target.corner = s.corner
        target.hScroll = s.hScroll
        target.hScrollKnob = s.hScrollKnob
        target.vScroll = s.vScroll
        target.vScrollKnob = s.vScrollKnob
      case _ => ()
    }

    def setField(obj: ScrollPane.ScrollPaneStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background"  => obj.background = resolveNullableDrawable(skin, json)
      case "corner"      => obj.corner = resolveNullableDrawable(skin, json)
      case "hScroll"     => obj.hScroll = resolveNullableDrawable(skin, json)
      case "hScrollKnob" => obj.hScrollKnob = resolveNullableDrawable(skin, json)
      case "vScroll"     => obj.vScroll = resolveNullableDrawable(skin, json)
      case "vScrollKnob" => obj.vScrollKnob = resolveNullableDrawable(skin, json)
      case _             => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 11. SelectBoxStyle
  // ---------------------------------------------------------------------------

  given selectBoxStyleReader: SkinStyleReader[SelectBox.SelectBoxStyle] with {

    def create(): SelectBox.SelectBoxStyle = SelectBox.SelectBoxStyle()

    def copyFrom(source: Any, target: SelectBox.SelectBoxStyle): Unit = source match {
      case s: SelectBox.SelectBoxStyle =>
        target.font = s.font
        target.fontColor.set(s.fontColor)
        s.overFontColor.foreach(c => target.overFontColor = Nullable(Color(c)))
        s.disabledFontColor.foreach(c => target.disabledFontColor = Nullable(Color(c)))
        target.background = s.background
        target.scrollStyle = s.scrollStyle
        target.listStyle = s.listStyle
        target.backgroundOver = s.backgroundOver
        target.backgroundOpen = s.backgroundOpen
        target.backgroundDisabled = s.backgroundDisabled
      case _ => ()
    }

    def setField(obj: SelectBox.SelectBoxStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "font"               => withFont(skin, json)(obj.font = _)
      case "fontColor"          => withColor(skin, json, readColor)(obj.fontColor.set)
      case "overFontColor"      => obj.overFontColor = resolveNullableColor(skin, json, readColor)
      case "disabledFontColor"  => obj.disabledFontColor = resolveNullableColor(skin, json, readColor)
      case "background"         => obj.background = resolveNullableDrawable(skin, json)
      case "backgroundOver"     => obj.backgroundOver = resolveNullableDrawable(skin, json)
      case "backgroundOpen"     => obj.backgroundOpen = resolveNullableDrawable(skin, json)
      case "backgroundDisabled" => obj.backgroundDisabled = resolveNullableDrawable(skin, json)
      case "scrollStyle"        => withStyle(skin, json, classOf[ScrollPane.ScrollPaneStyle], readStyle)(obj.scrollStyle = _)
      case "listStyle"          => withStyle(skin, json, classOf[SgeList.ListStyle], readStyle)(obj.listStyle = _)
      case _                    => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 12. SplitPaneStyle
  // ---------------------------------------------------------------------------

  given splitPaneStyleReader: SkinStyleReader[SplitPane.SplitPaneStyle] with {

    def create(): SplitPane.SplitPaneStyle = SplitPane.SplitPaneStyle()

    def copyFrom(source: Any, target: SplitPane.SplitPaneStyle): Unit = source match {
      case s: SplitPane.SplitPaneStyle =>
        target.handle = s.handle
      case _ => ()
    }

    def setField(obj: SplitPane.SplitPaneStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "handle" => withDrawable(skin, json)(obj.handle = _)
      case _        => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 13. TextFieldStyle
  // ---------------------------------------------------------------------------

  given textFieldStyleReader: SkinStyleReader[TextField.TextFieldStyle] with {

    def create(): TextField.TextFieldStyle = TextField.TextFieldStyle()

    def copyFrom(source: Any, target: TextField.TextFieldStyle): Unit = source match {
      case s: TextField.TextFieldStyle =>
        target.font = s.font
        Nullable(s.fontColor).foreach(c => target.fontColor = Color(c))
        target.focusedFontColor = s.focusedFontColor.map(c => Color(c))
        target.disabledFontColor = s.disabledFontColor.map(c => Color(c))
        target.background = s.background
        target.focusedBackground = s.focusedBackground
        target.disabledBackground = s.disabledBackground
        target.cursor = s.cursor
        target.selection = s.selection
        target.messageFont = s.messageFont
        target.messageFontColor = s.messageFontColor.map(c => Color(c))
      case _ => ()
    }

    def setField(obj: TextField.TextFieldStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "font"               => withFont(skin, json)(obj.font = _)
      case "fontColor"          => withColor(skin, json, readColor)(obj.fontColor = _)
      case "focusedFontColor"   => obj.focusedFontColor = resolveNullableColor(skin, json, readColor)
      case "disabledFontColor"  => obj.disabledFontColor = resolveNullableColor(skin, json, readColor)
      case "background"         => obj.background = resolveNullableDrawable(skin, json)
      case "focusedBackground"  => obj.focusedBackground = resolveNullableDrawable(skin, json)
      case "disabledBackground" => obj.disabledBackground = resolveNullableDrawable(skin, json)
      case "cursor"             => obj.cursor = resolveNullableDrawable(skin, json)
      case "selection"          => obj.selection = resolveNullableDrawable(skin, json)
      case "messageFont"        => obj.messageFont = resolveNullableFont(skin, json)
      case "messageFontColor"   => obj.messageFontColor = resolveNullableColor(skin, json, readColor)
      case _                    => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 14. TextTooltipStyle
  // ---------------------------------------------------------------------------

  given textTooltipStyleReader: SkinStyleReader[TextTooltip.TextTooltipStyle] with {

    def create(): TextTooltip.TextTooltipStyle = TextTooltip.TextTooltipStyle()

    def copyFrom(source: Any, target: TextTooltip.TextTooltipStyle): Unit = source match {
      case s: TextTooltip.TextTooltipStyle =>
        target.label = s.label
        target.wrapWidth = s.wrapWidth
        target.background = s.background
      case _ => ()
    }

    def setField(obj: TextTooltip.TextTooltipStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "label"      => withStyle(skin, json, classOf[Label.LabelStyle], readStyle)(obj.label = _)
      case "wrapWidth"  => obj.wrapWidth = resolveFloat(json)
      case "background" => obj.background = resolveNullableDrawable(skin, json)
      case _            => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 15. TouchpadStyle
  // ---------------------------------------------------------------------------

  given touchpadStyleReader: SkinStyleReader[Touchpad.TouchpadStyle] with {

    def create(): Touchpad.TouchpadStyle = Touchpad.TouchpadStyle()

    def copyFrom(source: Any, target: Touchpad.TouchpadStyle): Unit = source match {
      case s: Touchpad.TouchpadStyle =>
        target.background = s.background
        target.knob = s.knob
      case _ => ()
    }

    def setField(obj: Touchpad.TouchpadStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background" => obj.background = resolveNullableDrawable(skin, json)
      case "knob"       => obj.knob = resolveNullableDrawable(skin, json)
      case _            => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 16. TreeStyle
  // ---------------------------------------------------------------------------

  given treeStyleReader: SkinStyleReader[Tree.TreeStyle] with {

    def create(): Tree.TreeStyle = Tree.TreeStyle()

    def copyFrom(source: Any, target: Tree.TreeStyle): Unit = source match {
      case s: Tree.TreeStyle =>
        target.plus = s.plus
        target.minus = s.minus
        target.plusOver = s.plusOver
        target.minusOver = s.minusOver
        target.over = s.over
        target.selection = s.selection
        target.background = s.background
      case _ => ()
    }

    def setField(obj: Tree.TreeStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "plus"       => withDrawable(skin, json)(obj.plus = _)
      case "minus"      => withDrawable(skin, json)(obj.minus = _)
      case "plusOver"   => obj.plusOver = resolveNullableDrawable(skin, json)
      case "minusOver"  => obj.minusOver = resolveNullableDrawable(skin, json)
      case "over"       => obj.over = resolveNullableDrawable(skin, json)
      case "selection"  => obj.selection = resolveNullableDrawable(skin, json)
      case "background" => obj.background = resolveNullableDrawable(skin, json)
      case _            => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // 17. WindowStyle
  // ---------------------------------------------------------------------------

  given windowStyleReader: SkinStyleReader[Window.WindowStyle] with {

    def create(): Window.WindowStyle = Window.WindowStyle()

    def copyFrom(source: Any, target: Window.WindowStyle): Unit = source match {
      case s: Window.WindowStyle =>
        target.background = s.background
        target.titleFont = s.titleFont
        target.titleFontColor = s.titleFontColor
        target.stageBackground = s.stageBackground
      case _ => ()
    }

    def setField(obj: Window.WindowStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background"      => withDrawable(skin, json)(obj.background = _)
      case "titleFont"       => withFont(skin, json)(obj.titleFont = _)
      case "titleFontColor"  => obj.titleFontColor = resolveNullableColor(skin, json, readColor)
      case "stageBackground" => obj.stageBackground = resolveNullableDrawable(skin, json)
      case _                 => () // Unknown field, skip
    }
  }

  // ---------------------------------------------------------------------------
  // Registry
  // ---------------------------------------------------------------------------

  /** Maps style classes to their readers for dynamic dispatch in [[Skin.readStyleObject]]. */
  val registry: Map[Class[?], SkinStyleReader[?]] = Map(
    classOf[Button.ButtonStyle] -> buttonStyleReader,
    classOf[TextButton.TextButtonStyle] -> textButtonStyleReader,
    classOf[CheckBox.CheckBoxStyle] -> checkBoxStyleReader,
    classOf[ImageButton.ImageButtonStyle] -> imageButtonStyleReader,
    classOf[ImageTextButton.ImageTextButtonStyle] -> imageTextButtonStyleReader,
    classOf[Label.LabelStyle] -> labelStyleReader,
    classOf[SgeList.ListStyle] -> listStyleReader,
    classOf[ProgressBar.ProgressBarStyle] -> progressBarStyleReader,
    classOf[Slider.SliderStyle] -> sliderStyleReader,
    classOf[ScrollPane.ScrollPaneStyle] -> scrollPaneStyleReader,
    classOf[SelectBox.SelectBoxStyle] -> selectBoxStyleReader,
    classOf[SplitPane.SplitPaneStyle] -> splitPaneStyleReader,
    classOf[TextField.TextFieldStyle] -> textFieldStyleReader,
    classOf[TextTooltip.TextTooltipStyle] -> textTooltipStyleReader,
    classOf[Touchpad.TouchpadStyle] -> touchpadStyleReader,
    classOf[Tree.TreeStyle] -> treeStyleReader,
    classOf[Window.WindowStyle] -> windowStyleReader
  )
}
