/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/Styles.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: @Null → Nullable, Disposable → Closeable pattern, ButtonStyle → removed
 *     (scene2d.ui parent style hierarchy flattened for cross-platform use)
 *   Convention: getX()/setX() → public var; deprecated BitmapFont constructors omitted.
 *   Idiom: Nullable[A] for nullable fields. No-arg constructors preserved for skin JSON.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 403
 * Covenant-baseline-methods: CheckBoxStyle,ImageTextButtonStyle,LabelStyle,ListStyle,SelectBoxStyle,Styles,TextButtonStyle,TextFieldStyle,TextTooltipStyle,WindowStyle,background,backgroundDisabled,backgroundOpen,backgroundOver,checkboxOff,checkboxOffDisabled,checkboxOn,checkboxOnDisabled,checkboxOnOver,checkboxOver,checked,checkedDown,checkedDownFontColor,checkedFocused,checkedFocusedFontColor,checkedFontColor,checkedOffsetX,checkedOffsetY,checkedOver,checkedOverFontColor,close,cursor,disabled,disabledBackground,disabledFontColor,down,downFontColor,focused,focusedBackground,focusedFontColor,font,fontColor,fontColorSelected,fontColorUnselected,imageChecked,imageCheckedDown,imageCheckedOver,imageDisabled,imageDown,imageOver,imageUp,label,listStyle,messageFontColor,over,overFontColor,pressedOffsetX,pressedOffsetY,scrollStyle,selection,stageBackground,this,titleFont,titleFontColor,unpressedOffsetX,unpressedOffsetY,up,wrapWidth
 * Covenant-source-reference: com/github/tommyettinger/textra/Styles.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import sge.graphics.Color
import sge.utils.Nullable

/** An outer object that holds all styles for TextraTypist widgets. These are each named to match a scene2d.ui style. These styles are typically loaded from a skin JSON file using FWSkin, but can also
  * be created on their own.
  */
object Styles {

  /** The style for a TextraLabel or TypingLabel. */
  class LabelStyle {
    var font:       Nullable[Font]   = Nullable.empty
    var fontColor:  Nullable[Color]  = Nullable.empty
    var background: Nullable[AnyRef] = Nullable.empty // Drawable in scene2d; AnyRef for cross-platform

    def this(font: Font, fontColor: Nullable[Color]) = {
      this()
      this.font = Nullable(font)
      this.fontColor = fontColor
    }

    def this(font: Font, fontColor: Nullable[Color], background: Nullable[AnyRef]) = {
      this()
      this.font = Nullable(font)
      this.fontColor = fontColor
      this.background = background
    }

    def this(style: LabelStyle) = {
      this()
      font = style.font
      Nullable.foreach(style.fontColor)(c => fontColor = Nullable(new Color(c)))
      background = style.background
    }
  }

  /** The style for a text button, see TextraButton. */
  class TextButtonStyle {
    var font:                    Nullable[Font]  = Nullable.empty
    var fontColor:               Nullable[Color] = Nullable.empty
    var downFontColor:           Nullable[Color] = Nullable.empty
    var overFontColor:           Nullable[Color] = Nullable.empty
    var focusedFontColor:        Nullable[Color] = Nullable.empty
    var disabledFontColor:       Nullable[Color] = Nullable.empty
    var checkedFontColor:        Nullable[Color] = Nullable.empty
    var checkedDownFontColor:    Nullable[Color] = Nullable.empty
    var checkedOverFontColor:    Nullable[Color] = Nullable.empty
    var checkedFocusedFontColor: Nullable[Color] = Nullable.empty
    // ButtonStyle fields (flattened from scene2d.ui hierarchy)
    var up:               Nullable[AnyRef] = Nullable.empty
    var down:             Nullable[AnyRef] = Nullable.empty
    var checked:          Nullable[AnyRef] = Nullable.empty
    var checkedDown:      Nullable[AnyRef] = Nullable.empty
    var checkedOver:      Nullable[AnyRef] = Nullable.empty
    var checkedFocused:   Nullable[AnyRef] = Nullable.empty
    var over:             Nullable[AnyRef] = Nullable.empty
    var focused:          Nullable[AnyRef] = Nullable.empty
    var disabled:         Nullable[AnyRef] = Nullable.empty
    var pressedOffsetX:   Float            = 0f
    var pressedOffsetY:   Float            = 0f
    var unpressedOffsetX: Float            = 0f
    var unpressedOffsetY: Float            = 0f
    var checkedOffsetX:   Float            = 0f
    var checkedOffsetY:   Float            = 0f

    def this(up: Nullable[AnyRef], down: Nullable[AnyRef], checked: Nullable[AnyRef], font: Nullable[Font]) = {
      this()
      this.up = up
      this.down = down
      this.checked = checked
      this.font = font
    }

    def this(style: TextButtonStyle) = {
      this()
      font = style.font
      Nullable.foreach(style.fontColor)(c => fontColor = Nullable(new Color(c)))
      Nullable.foreach(style.downFontColor)(c => downFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.overFontColor)(c => overFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.focusedFontColor)(c => focusedFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.disabledFontColor)(c => disabledFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedFontColor)(c => checkedFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedDownFontColor)(c => checkedDownFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedOverFontColor)(c => checkedOverFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedFocusedFontColor)(c => checkedFocusedFontColor = Nullable(new Color(c)))
      up = style.up
      down = style.down
      checked = style.checked
      checkedDown = style.checkedDown
      checkedOver = style.checkedOver
      checkedFocused = style.checkedFocused
      over = style.over
      focused = style.focused
      disabled = style.disabled
      pressedOffsetX = style.pressedOffsetX
      pressedOffsetY = style.pressedOffsetY
      unpressedOffsetX = style.unpressedOffsetX
      unpressedOffsetY = style.unpressedOffsetY
      checkedOffsetX = style.checkedOffsetX
      checkedOffsetY = style.checkedOffsetY
    }
  }

  /** The style for an image text button, see ImageTextraButton. */
  class ImageTextButtonStyle extends TextButtonStyle {
    var imageUp:          Nullable[AnyRef] = Nullable.empty
    var imageDown:        Nullable[AnyRef] = Nullable.empty
    var imageOver:        Nullable[AnyRef] = Nullable.empty
    var imageDisabled:    Nullable[AnyRef] = Nullable.empty
    var imageChecked:     Nullable[AnyRef] = Nullable.empty
    var imageCheckedDown: Nullable[AnyRef] = Nullable.empty
    var imageCheckedOver: Nullable[AnyRef] = Nullable.empty

    def this(
      up:      Nullable[AnyRef],
      down:    Nullable[AnyRef],
      checked: Nullable[AnyRef],
      font:    Nullable[Font]
    ) = {
      this()
      this.up = up
      this.down = down
      this.checked = checked
      this.font = font
    }

    def this(style: ImageTextButtonStyle) = {
      this()
      // copy TextButtonStyle fields
      font = style.font
      Nullable.foreach(style.fontColor)(c => fontColor = Nullable(new Color(c)))
      Nullable.foreach(style.downFontColor)(c => downFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.overFontColor)(c => overFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.focusedFontColor)(c => focusedFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.disabledFontColor)(c => disabledFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedFontColor)(c => checkedFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedDownFontColor)(c => checkedDownFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedOverFontColor)(c => checkedOverFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedFocusedFontColor)(c => checkedFocusedFontColor = Nullable(new Color(c)))
      imageUp = style.imageUp
      imageDown = style.imageDown
      imageOver = style.imageOver
      imageDisabled = style.imageDisabled
      imageChecked = style.imageChecked
      imageCheckedDown = style.imageCheckedDown
      imageCheckedOver = style.imageCheckedOver
    }
  }

  /** The style for a checkbox, see TextraCheckBox. */
  class CheckBoxStyle extends TextButtonStyle {
    var checkboxOn:          Nullable[AnyRef] = Nullable.empty
    var checkboxOff:         Nullable[AnyRef] = Nullable.empty
    var checkboxOnOver:      Nullable[AnyRef] = Nullable.empty
    var checkboxOver:        Nullable[AnyRef] = Nullable.empty
    var checkboxOnDisabled:  Nullable[AnyRef] = Nullable.empty
    var checkboxOffDisabled: Nullable[AnyRef] = Nullable.empty

    def this(
      checkboxOff: Nullable[AnyRef],
      checkboxOn:  Nullable[AnyRef],
      font:        Nullable[Font],
      fontColor:   Nullable[Color]
    ) = {
      this()
      this.checkboxOff = checkboxOff
      this.checkboxOn = checkboxOn
      this.font = font
      this.fontColor = fontColor
    }

    def this(style: CheckBoxStyle) = {
      this()
      font = style.font
      Nullable.foreach(style.fontColor)(c => this.fontColor = Nullable(new Color(c)))
      Nullable.foreach(style.downFontColor)(c => downFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.overFontColor)(c => overFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.focusedFontColor)(c => focusedFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.disabledFontColor)(c => disabledFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedFontColor)(c => checkedFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedDownFontColor)(c => checkedDownFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedOverFontColor)(c => checkedOverFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.checkedFocusedFontColor)(c => checkedFocusedFontColor = Nullable(new Color(c)))
      checkboxOff = style.checkboxOff
      checkboxOn = style.checkboxOn
      checkboxOnOver = style.checkboxOnOver
      checkboxOver = style.checkboxOver
      checkboxOnDisabled = style.checkboxOnDisabled
      checkboxOffDisabled = style.checkboxOffDisabled
    }
  }

  /** The style for a window, see TextraWindow. */
  class WindowStyle {
    var background:      Nullable[AnyRef] = Nullable.empty
    var titleFont:       Nullable[Font]   = Nullable.empty
    var titleFontColor:  Nullable[Color]  = Nullable(new Color(1f, 1f, 1f, 1f))
    var stageBackground: Nullable[AnyRef] = Nullable.empty

    def this(titleFont: Font, titleFontColor: Color, background: Nullable[AnyRef]) = {
      this()
      this.titleFont = Nullable(titleFont)
      Nullable.foreach(Nullable(titleFontColor))(c => this.titleFontColor = Nullable(new Color(c)))
      this.background = background
    }

    def this(
      titleFont:       Font,
      titleFontColor:  Color,
      background:      Nullable[AnyRef],
      stageBackground: Nullable[AnyRef]
    ) = {
      this()
      this.titleFont = Nullable(titleFont)
      Nullable.foreach(Nullable(titleFontColor))(c => this.titleFontColor = Nullable(new Color(c)))
      this.background = background
      this.stageBackground = stageBackground
    }

    def this(style: WindowStyle) = {
      this()
      titleFont = style.titleFont
      Nullable.foreach(style.titleFontColor)(c => titleFontColor = Nullable(new Color(c)))
      background = style.background
      stageBackground = style.stageBackground
    }
  }

  /** The style for a ListBox, see TextraListBox. */
  class ListStyle {
    var font:                Nullable[Font]   = Nullable.empty
    var fontColorSelected:   Color            = new Color(1f, 1f, 1f, 1f)
    var fontColorUnselected: Color            = new Color(1f, 1f, 1f, 1f)
    var selection:           Nullable[AnyRef] = Nullable.empty
    var down:                Nullable[AnyRef] = Nullable.empty
    var over:                Nullable[AnyRef] = Nullable.empty
    var background:          Nullable[AnyRef] = Nullable.empty

    def this(font: Font, fontColorSelected: Color, fontColorUnselected: Color, selection: Nullable[AnyRef]) = {
      this()
      this.font = Nullable(font)
      this.fontColorSelected.set(fontColorSelected)
      this.fontColorUnselected.set(fontColorUnselected)
      this.selection = selection
    }

    def this(
      font:                Font,
      fontColorSelected:   Color,
      fontColorUnselected: Color,
      selection:           Nullable[AnyRef],
      down:                Nullable[AnyRef],
      over:                Nullable[AnyRef],
      background:          Nullable[AnyRef]
    ) = {
      this()
      this.font = Nullable(font)
      this.fontColorSelected.set(fontColorSelected)
      this.fontColorUnselected.set(fontColorUnselected)
      this.selection = selection
      this.down = down
      this.over = over
      this.background = background
    }

    def this(style: ListStyle) = {
      this()
      font = style.font
      fontColorSelected.set(style.fontColorSelected)
      fontColorUnselected.set(style.fontColorUnselected)
      selection = style.selection
      down = style.down
      over = style.over
      background = style.background
    }
  }

  /** The style for a select box, see TextraSelectBox. */
  class SelectBoxStyle {
    var font:               Nullable[Font]      = Nullable.empty
    var fontColor:          Color               = new Color(1f, 1f, 1f, 1f)
    var overFontColor:      Nullable[Color]     = Nullable.empty
    var disabledFontColor:  Nullable[Color]     = Nullable.empty
    var background:         Nullable[AnyRef]    = Nullable.empty
    var scrollStyle:        Nullable[AnyRef]    = Nullable.empty // ScrollPane.ScrollPaneStyle
    var listStyle:          Nullable[ListStyle] = Nullable.empty
    var backgroundOver:     Nullable[AnyRef]    = Nullable.empty
    var backgroundOpen:     Nullable[AnyRef]    = Nullable.empty
    var backgroundDisabled: Nullable[AnyRef]    = Nullable.empty

    def this(
      font:        Font,
      fontColor:   Color,
      background:  Nullable[AnyRef],
      scrollStyle: Nullable[AnyRef],
      listStyle:   ListStyle
    ) = {
      this()
      this.font = Nullable(font)
      this.fontColor.set(fontColor)
      this.background = background
      this.scrollStyle = scrollStyle
      this.listStyle = Nullable(listStyle)
    }

    def this(style: SelectBoxStyle) = {
      this()
      font = style.font
      fontColor.set(style.fontColor)
      Nullable.foreach(style.overFontColor)(c => overFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.disabledFontColor)(c => disabledFontColor = Nullable(new Color(c)))
      background = style.background
      scrollStyle = style.scrollStyle
      Nullable.foreach(style.listStyle)(ls => listStyle = Nullable(new ListStyle(ls)))
      backgroundOver = style.backgroundOver
      backgroundOpen = style.backgroundOpen
      backgroundDisabled = style.backgroundDisabled
    }
  }

  /** The style for a text tooltip, see TextraTooltip. */
  class TextTooltipStyle {
    var label:      Nullable[LabelStyle] = Nullable.empty
    var background: Nullable[AnyRef]     = Nullable.empty

    /** 0 means don't wrap. */
    var wrapWidth: Float = 0f

    def this(style: LabelStyle, background: Nullable[AnyRef]) = {
      this()
      this.label = Nullable(style)
      this.background = background
    }

    def this(style: TextTooltipStyle) = {
      this()
      Nullable.foreach(style.label)(ls => label = Nullable(new LabelStyle(ls)))
      background = style.background
      wrapWidth = style.wrapWidth
    }
  }

  /** The style for a text field, see TextraField. This is AutoCloseable because it always copies any Font it is given (unless font is directly assigned, which should be avoided). To avoid the copy
    * becoming inaccessible while still holding native resources, you should close this style when you are completely finished using it (and don't intend to use it again). You can also just close the
    * font, which does the same thing as calling close() on the style.
    */
  class TextFieldStyle extends AutoCloseable {
    var font:               Nullable[Font]   = Nullable.empty
    var fontColor:          Nullable[Color]  = Nullable.empty
    var focusedFontColor:   Nullable[Color]  = Nullable.empty
    var disabledFontColor:  Nullable[Color]  = Nullable.empty
    var background:         Nullable[AnyRef] = Nullable.empty
    var focusedBackground:  Nullable[AnyRef] = Nullable.empty
    var disabledBackground: Nullable[AnyRef] = Nullable.empty
    var cursor:             Nullable[AnyRef] = Nullable.empty
    var selection:          Nullable[AnyRef] = Nullable.empty
    var messageFontColor:   Nullable[Color]  = Nullable.empty

    def this(font: Font, fontColor: Color, cursor: Nullable[AnyRef], selection: Nullable[AnyRef], background: Nullable[AnyRef]) = {
      this()
      this.font = Nullable(new Font(font))
      this.fontColor = Nullable(fontColor)
      this.cursor = cursor
      this.selection = selection
      this.background = background
    }

    def this(style: TextFieldStyle) = {
      this()
      Nullable.foreach(style.font)(f => font = Nullable(new Font(f)))
      Nullable.foreach(style.fontColor)(c => fontColor = Nullable(new Color(c)))
      Nullable.foreach(style.focusedFontColor)(c => focusedFontColor = Nullable(new Color(c)))
      Nullable.foreach(style.disabledFontColor)(c => disabledFontColor = Nullable(new Color(c)))
      background = style.background
      focusedBackground = style.focusedBackground
      disabledBackground = style.disabledBackground
      cursor = style.cursor
      selection = style.selection
      Nullable.foreach(style.messageFontColor)(c => messageFontColor = Nullable(new Color(c)))
    }

    /** Releases all resources of this object. Calls Font.close() on font, but does nothing else. This style is AutoCloseable because it creates a copy of any Font it is given. If you are using the
      * same TextFieldStyle for multiple TextraFields, then you should only close the TextFieldStyle when you are no longer using the style for any current or future TextraFields.
      */
    override def close(): Unit =
      Nullable.foreach(font)(_.close())
  }
}
