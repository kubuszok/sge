/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Origin: SGE-original (no single upstream file). The upstream VisUI relies on
 *     libGDX Skin's reflection-based JSON reader to resolve its FQCN-keyed style
 *     entries (com.kotcrab.vis.ui.Sizes, com.kotcrab.vis.ui.widget.VisTextButton$VisTextButtonStyle, ...)
 *     and to set their fields. The SGE Skin replaced reflection with a closed
 *     classTagMap + SkinStyleReader registry (Scala.js/Native compatibility), so
 *     VisUI must register its own JSON class tags and style readers through the
 *     extension seam added for ISS-515 (Skin.registerJsonClassTags +
 *     SkinStyleReader.register; see ISS-534 for the general closed-registry redesign).
 *   Idiom: split packages; type-class style readers mirroring sge.scenes.scene2d.ui.SkinStyleReader.
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui

import sge.graphics.Color
import sge.scenes.scene2d.ui.{ Button, Label, ScrollPane, Skin, SkinStyleReader, SplitPane, TextButton, TextField, Window }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Json, SgeError }
import lowlevel.Nullable
import sge.visui.util.adapter.SimpleListAdapter
import sge.visui.util.form.SimpleFormValidator
import sge.visui.widget.{
  BusyBar,
  LinkLabel,
  ListViewStyle,
  Menu,
  MenuBar,
  MenuItem,
  MultiSplitPane,
  PopupMenu,
  Separator,
  Tooltip,
  VisCheckBox,
  VisImageButton,
  VisImageTextButton,
  VisSplitPane,
  VisTextButton,
  VisTextField
}
import sge.visui.widget.color.{ ColorPickerStyle, ColorPickerWidgetStyle }
import sge.visui.widget.file.FileChooserStyle
import sge.visui.widget.spinner.Spinner
import sge.visui.widget.tabbedpane.TabbedPane
import sge.visui.widget.toast.Toast

/** Registers VisUI's skin JSON class tags and style readers with the core [[Skin]] / [[SkinStyleReader]] extension seam.
  *
  * The upstream VisUI skin JSON keys its style sections by Java fully-qualified class names (e.g. {@code com.kotcrab.vis.ui.Sizes}). libGDX's reflection-based Skin reader resolves those names and
  * sets fields reflectively; the SGE Skin uses an explicit, reflection-free registry instead, so VisUI registers the mappings here before any [[Skin]] is constructed (mirrors the implicit
  * registration libGDX got for free via reflection).
  *
  * This currently wires the style types the VisUI runtime relies on for its core skin contract: [[Sizes]] (resolved by [[VisUI.getSizes]] for both [[VisUI.SkinScale.X1]] and [[VisUI.SkinScale.X2]])
  * and [[VisTextButton.VisTextButtonStyle]] (every no-arg / style-name [[VisTextButton]] constructor resolves it from the skin). Additional VisUI widget styles get their readers added incrementally
  * under the same seam (see ISS-534 for the general mechanism).
  */
private[visui] object VisUISkinReaders {

  // Idempotent guard: registration mutates shared static maps on the core Skin /
  // SkinStyleReader companions; register only once per process.
  @volatile private var registered: Boolean = false

  /** Resolves a non-nullable [[Float]] from a JSON number, mirroring [[SkinStyleReader]]'s private {@code resolveFloat} (0f for non-numbers). */
  private def resolveFloat(json: Json): Float = json match {
    case Json.Num(n) => n.toDouble.map(_.toFloat).getOrElse(0f)
    case _           => 0f
  }

  /** Resolves a [[Nullable]] [[Drawable]] from a JSON string name via the skin, mirroring [[SkinStyleReader]]'s private {@code resolveNullableDrawable} (empty on lookup failure). */
  private def resolveNullableDrawable(skin: Skin, json: Json): Nullable[Drawable] = json match {
    case Json.Str(name) =>
      try Nullable(skin.getDrawable(name))
      catch { case _: SgeError => Nullable.empty }
    case _ => Nullable.empty
  }

  /** Sets a non-nullable [[Drawable]] field from a JSON named reference, mirroring [[SkinStyleReader]]'s private {@code withDrawable}: a named-but-missing resource is a hard error (propagated as
    * [[SgeError]] so [[Skin]] wraps it); a non-string JSON value leaves the field unset.
    */
  private inline def withDrawable(skin: Skin, json: Json)(setter: Drawable => Unit): Unit = json match {
    case Json.Str(name) => setter(skin.getDrawable(name))
    case _              => ()
  }

  /** Resolves a non-nullable [[Color]] from JSON (string reference via the skin, or inline object via {@code readColor}), mirroring [[SkinStyleReader]]'s private {@code resolveColor}. */
  private def resolveColor(skin: Skin, json: Json, readColor: Json => Color): Color = json match {
    case Json.Str(name) => skin.get[Color](name, classOf[Color])
    case _              => readColor(json)
  }

  /** Resolves a non-nullable [[Boolean]] from a JSON value (false for non-booleans), mirroring libGDX's reflective primitive-boolean field set. */
  private def resolveBoolean(json: Json): Boolean = json match {
    case Json.Bool(b) => b
    case _            => false
  }

  /** Resolves a nested style of type {@code styleClass} from JSON (string reference via the skin, or inline object via {@code readStyle}), mirroring [[SkinStyleReader]]'s private
    * {@code resolveStyle}.
    */
  private def resolveStyle[S](skin: Skin, json: Json, styleClass: Class[S], readStyle: (Class[?], Json) => Any): S = json match {
    case Json.Str(name) => skin.get(name, styleClass)
    case _              => readStyle(styleClass, json).asInstanceOf[S]
  }

  /** Sets a nested style field, mirroring [[SkinStyleReader]]'s private {@code withStyle}: a string value resolves through the skin (hard error if missing), an inline object via {@code readStyle}. */
  private inline def withStyle[S](skin: Skin, json: Json, styleClass: Class[S], readStyle: (Class[?], Json) => Any)(setter: S => Unit): Unit =
    setter(resolveStyle(skin, json, styleClass, readStyle))

  /** Reader for VisUI [[Sizes]]: a plain holder of float padding/size values loaded from the skin. Mirrors libGDX reading each named field reflectively onto the Sizes instance. */
  private val sizesReader: SkinStyleReader[Sizes] = new SkinStyleReader[Sizes] {

    def create(): Sizes = new Sizes()

    def copyFrom(source: Any, target: Sizes): Unit = source match {
      case s: Sizes =>
        target.scaleFactor = s.scaleFactor
        target.spacingTop = s.spacingTop
        target.spacingBottom = s.spacingBottom
        target.spacingRight = s.spacingRight
        target.spacingLeft = s.spacingLeft
        target.buttonBarSpacing = s.buttonBarSpacing
        target.menuItemIconSize = s.menuItemIconSize
        target.borderSize = s.borderSize
        target.spinnerButtonHeight = s.spinnerButtonHeight
        target.spinnerFieldSize = s.spinnerFieldSize
        target.fileChooserViewModeBigIconsSize = s.fileChooserViewModeBigIconsSize
        target.fileChooserViewModeMediumIconsSize = s.fileChooserViewModeMediumIconsSize
        target.fileChooserViewModeSmallIconsSize = s.fileChooserViewModeSmallIconsSize
        target.fileChooserViewModeListWidthSize = s.fileChooserViewModeListWidthSize
      case _ => ()
    }

    def setField(obj: Sizes, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "scaleFactor"                        => obj.scaleFactor = resolveFloat(json)
      case "spacingTop"                         => obj.spacingTop = resolveFloat(json)
      case "spacingBottom"                      => obj.spacingBottom = resolveFloat(json)
      case "spacingRight"                       => obj.spacingRight = resolveFloat(json)
      case "spacingLeft"                        => obj.spacingLeft = resolveFloat(json)
      case "buttonBarSpacing"                   => obj.buttonBarSpacing = resolveFloat(json)
      case "menuItemIconSize"                   => obj.menuItemIconSize = resolveFloat(json)
      case "borderSize"                         => obj.borderSize = resolveFloat(json)
      case "spinnerButtonHeight"                => obj.spinnerButtonHeight = resolveFloat(json)
      case "spinnerFieldSize"                   => obj.spinnerFieldSize = resolveFloat(json)
      case "fileChooserViewModeBigIconsSize"    => obj.fileChooserViewModeBigIconsSize = resolveFloat(json)
      case "fileChooserViewModeMediumIconsSize" => obj.fileChooserViewModeMediumIconsSize = resolveFloat(json)
      case "fileChooserViewModeSmallIconsSize"  => obj.fileChooserViewModeSmallIconsSize = resolveFloat(json)
      case "fileChooserViewModeListWidthSize"   => obj.fileChooserViewModeListWidthSize = resolveFloat(json)
      case _                                    => () // Unknown field, skip
    }
  }

  /** Reader for [[VisTextButton.VisTextButtonStyle]], which extends [[TextButton.TextButtonStyle]]. The only field VisUI adds is {@code focusBorder}; all inherited TextButton/Button fields are
    * delegated to the core [[SkinStyleReader.textButtonStyleReader]], mirroring libGDX's reflective walk up the superclass chain.
    */
  private val visTextButtonStyleReader: SkinStyleReader[VisTextButton.VisTextButtonStyle] = new SkinStyleReader[VisTextButton.VisTextButtonStyle] {

    def create(): VisTextButton.VisTextButtonStyle = new VisTextButton.VisTextButtonStyle()

    def copyFrom(source: Any, target: VisTextButton.VisTextButtonStyle): Unit = source match {
      case s: VisTextButton.VisTextButtonStyle =>
        target.focusBorder = s.focusBorder
        SkinStyleReader.textButtonStyleReader.copyFrom(s, target)
      case s: TextButton.TextButtonStyle =>
        SkinStyleReader.textButtonStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: VisTextButton.VisTextButtonStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "focusBorder" => obj.focusBorder = resolveNullableDrawable(skin, json)
      case _             => SkinStyleReader.textButtonStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  /** Reader for [[SimpleListAdapter.SimpleListAdapterStyle]]: plain holder of {@code background} / {@code selection} drawables. */
  private val simpleListAdapterStyleReader: SkinStyleReader[SimpleListAdapter.SimpleListAdapterStyle] = new SkinStyleReader[SimpleListAdapter.SimpleListAdapterStyle] {

    def create(): SimpleListAdapter.SimpleListAdapterStyle = new SimpleListAdapter.SimpleListAdapterStyle()

    def copyFrom(source: Any, target: SimpleListAdapter.SimpleListAdapterStyle): Unit = source match {
      case s: SimpleListAdapter.SimpleListAdapterStyle =>
        target.background = s.background
        target.selection = s.selection
      case _ => ()
    }

    def setField(obj: SimpleListAdapter.SimpleListAdapterStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background" => withDrawable(skin, json)(obj.background = _)
      case "selection"  => withDrawable(skin, json)(obj.selection = _)
      case _            => () // Unknown field, skip
    }
  }

  /** Reader for [[SimpleFormValidator.FormValidatorStyle]]: error/valid label colors plus an optional color transition duration. */
  private val formValidatorStyleReader: SkinStyleReader[SimpleFormValidator.FormValidatorStyle] = new SkinStyleReader[SimpleFormValidator.FormValidatorStyle] {

    def create(): SimpleFormValidator.FormValidatorStyle = new SimpleFormValidator.FormValidatorStyle()

    def copyFrom(source: Any, target: SimpleFormValidator.FormValidatorStyle): Unit = source match {
      case s: SimpleFormValidator.FormValidatorStyle =>
        target.errorLabelColor = s.errorLabelColor
        target.validLabelColor = s.validLabelColor
        target.colorTransitionDuration = s.colorTransitionDuration
      case _ => ()
    }

    def setField(obj: SimpleFormValidator.FormValidatorStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "errorLabelColor"         => obj.errorLabelColor = resolveColor(skin, json, readColor)
      case "validLabelColor"         => obj.validLabelColor = resolveColor(skin, json, readColor)
      case "colorTransitionDuration" => obj.colorTransitionDuration = resolveFloat(json)
      case _                         => () // Unknown field, skip
    }
  }

  /** Reader for [[BusyBar.BusyBarStyle]]: a segment drawable plus integer overflow/width/height. */
  private val busyBarStyleReader: SkinStyleReader[BusyBar.BusyBarStyle] = new SkinStyleReader[BusyBar.BusyBarStyle] {

    def create(): BusyBar.BusyBarStyle = new BusyBar.BusyBarStyle()

    def copyFrom(source: Any, target: BusyBar.BusyBarStyle): Unit = source match {
      case s: BusyBar.BusyBarStyle =>
        target.segment = s.segment
        target.segmentOverflow = s.segmentOverflow
        target.segmentWidth = s.segmentWidth
        target.height = s.height
      case _ => ()
    }

    def setField(obj: BusyBar.BusyBarStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "segment"         => withDrawable(skin, json)(obj.segment = _)
      case "segmentOverflow" => obj.segmentOverflow = resolveFloat(json).toInt
      case "segmentWidth"    => obj.segmentWidth = resolveFloat(json).toInt
      case "height"          => obj.height = resolveFloat(json).toInt
      case _                 => () // Unknown field, skip
    }
  }

  /** Reader for [[LinkLabel.LinkLabelStyle]], which extends [[Label.LabelStyle]]. Adds an optional {@code underline} drawable; inherited font/color/background fields delegate to the core
    * [[SkinStyleReader.labelStyleReader]].
    */
  private val linkLabelStyleReader: SkinStyleReader[LinkLabel.LinkLabelStyle] = new SkinStyleReader[LinkLabel.LinkLabelStyle] {

    def create(): LinkLabel.LinkLabelStyle = new LinkLabel.LinkLabelStyle()

    def copyFrom(source: Any, target: LinkLabel.LinkLabelStyle): Unit = source match {
      case s: LinkLabel.LinkLabelStyle =>
        target.underline = s.underline
        SkinStyleReader.labelStyleReader.copyFrom(s, target)
      case s: Label.LabelStyle =>
        SkinStyleReader.labelStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: LinkLabel.LinkLabelStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "underline" => obj.underline = resolveNullableDrawable(skin, json)
      case _           => SkinStyleReader.labelStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  /** Reader for [[ListViewStyle]]: holds an optional nested [[ScrollPane.ScrollPaneStyle]]. */
  private val listViewStyleReader: SkinStyleReader[ListViewStyle] = new SkinStyleReader[ListViewStyle] {

    def create(): ListViewStyle = new ListViewStyle()

    def copyFrom(source: Any, target: ListViewStyle): Unit = source match {
      case s: ListViewStyle => target.scrollPaneStyle = s.scrollPaneStyle
      case _ => ()
    }

    def setField(obj: ListViewStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "scrollPaneStyle" => withStyle(skin, json, classOf[ScrollPane.ScrollPaneStyle], readStyle)(s => obj.scrollPaneStyle = Nullable(s))
      case _                 => () // Unknown field, skip
    }
  }

  /** Reader for [[PopupMenu.PopupMenuStyle]]: a {@code background} drawable and an optional {@code border}. */
  private val popupMenuStyleReader: SkinStyleReader[PopupMenu.PopupMenuStyle] = new SkinStyleReader[PopupMenu.PopupMenuStyle] {

    def create(): PopupMenu.PopupMenuStyle = new PopupMenu.PopupMenuStyle()

    def copyFrom(source: Any, target: PopupMenu.PopupMenuStyle): Unit = source match {
      case s: PopupMenu.PopupMenuStyle =>
        target.background = s.background
        target.border = s.border
      case _ => ()
    }

    def setField(obj: PopupMenu.PopupMenuStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background" => withDrawable(skin, json)(obj.background = _)
      case "border"     => obj.border = resolveNullableDrawable(skin, json)
      case _            => () // Unknown field, skip
    }
  }

  /** Reader for [[Menu.MenuStyle]], which extends [[PopupMenu.PopupMenuStyle]]. Adds {@code openButtonStyle} (a referenced [[VisTextButton.VisTextButtonStyle]]); inherited fields delegate to
    * [[popupMenuStyleReader]].
    */
  private val menuStyleReader: SkinStyleReader[Menu.MenuStyle] = new SkinStyleReader[Menu.MenuStyle] {

    def create(): Menu.MenuStyle = new Menu.MenuStyle()

    def copyFrom(source: Any, target: Menu.MenuStyle): Unit = source match {
      case s: Menu.MenuStyle =>
        target.openButtonStyle = s.openButtonStyle
        popupMenuStyleReader.copyFrom(s, target)
      case s: PopupMenu.PopupMenuStyle =>
        popupMenuStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: Menu.MenuStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "openButtonStyle" => withStyle(skin, json, classOf[VisTextButton.VisTextButtonStyle], readStyle)(obj.openButtonStyle = _)
      case _                 => popupMenuStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  /** Reader for [[MenuBar.MenuBarStyle]]: a single {@code background} drawable. */
  private val menuBarStyleReader: SkinStyleReader[MenuBar.MenuBarStyle] = new SkinStyleReader[MenuBar.MenuBarStyle] {

    def create(): MenuBar.MenuBarStyle = new MenuBar.MenuBarStyle()

    def copyFrom(source: Any, target: MenuBar.MenuBarStyle): Unit = source match {
      case s: MenuBar.MenuBarStyle => target.background = s.background
      case _ => ()
    }

    def setField(obj: MenuBar.MenuBarStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background" => withDrawable(skin, json)(obj.background = _)
      case _            => () // Unknown field, skip
    }
  }

  /** Reader for [[MenuItem.MenuItemStyle]], which extends [[TextButton.TextButtonStyle]]. Adds a {@code subMenu} drawable; inherited Button/TextButton fields delegate to the core
    * [[SkinStyleReader.textButtonStyleReader]].
    */
  private val menuItemStyleReader: SkinStyleReader[MenuItem.MenuItemStyle] = new SkinStyleReader[MenuItem.MenuItemStyle] {

    def create(): MenuItem.MenuItemStyle = new MenuItem.MenuItemStyle()

    def copyFrom(source: Any, target: MenuItem.MenuItemStyle): Unit = source match {
      case s: MenuItem.MenuItemStyle =>
        target.subMenu = s.subMenu
        SkinStyleReader.textButtonStyleReader.copyFrom(s, target)
      case s: TextButton.TextButtonStyle =>
        SkinStyleReader.textButtonStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: MenuItem.MenuItemStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "subMenu" => withDrawable(skin, json)(obj.subMenu = _)
      case _         => SkinStyleReader.textButtonStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  /** Reader for [[Separator.SeparatorStyle]]: a {@code background} drawable and an integer {@code thickness}. */
  private val separatorStyleReader: SkinStyleReader[Separator.SeparatorStyle] = new SkinStyleReader[Separator.SeparatorStyle] {

    def create(): Separator.SeparatorStyle = new Separator.SeparatorStyle()

    def copyFrom(source: Any, target: Separator.SeparatorStyle): Unit = source match {
      case s: Separator.SeparatorStyle =>
        target.background = s.background
        target.thickness = s.thickness
      case _ => ()
    }

    def setField(obj: Separator.SeparatorStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background" => withDrawable(skin, json)(obj.background = _)
      case "thickness"  => obj.thickness = resolveFloat(json).toInt
      case _            => () // Unknown field, skip
    }
  }

  /** Reader for [[Tooltip.TooltipStyle]]: a single {@code background} drawable. */
  private val tooltipStyleReader: SkinStyleReader[Tooltip.TooltipStyle] = new SkinStyleReader[Tooltip.TooltipStyle] {

    def create(): Tooltip.TooltipStyle = new Tooltip.TooltipStyle()

    def copyFrom(source: Any, target: Tooltip.TooltipStyle): Unit = source match {
      case s: Tooltip.TooltipStyle => target.background = s.background
      case _ => ()
    }

    def setField(obj: Tooltip.TooltipStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background" => withDrawable(skin, json)(obj.background = _)
      case _            => () // Unknown field, skip
    }
  }

  /** Reader for [[VisCheckBox.VisCheckBoxStyle]], which extends [[TextButton.TextButtonStyle]]. Adds VisUI focus/error borders and the check-background / tick drawables; inherited fields delegate to
    * the core [[SkinStyleReader.textButtonStyleReader]].
    */
  private val visCheckBoxStyleReader: SkinStyleReader[VisCheckBox.VisCheckBoxStyle] = new SkinStyleReader[VisCheckBox.VisCheckBoxStyle] {

    def create(): VisCheckBox.VisCheckBoxStyle = new VisCheckBox.VisCheckBoxStyle()

    def copyFrom(source: Any, target: VisCheckBox.VisCheckBoxStyle): Unit = source match {
      case s: VisCheckBox.VisCheckBoxStyle =>
        target.focusBorder = s.focusBorder
        target.errorBorder = s.errorBorder
        target.checkBackground = s.checkBackground
        target.checkBackgroundOver = s.checkBackgroundOver
        target.checkBackgroundDown = s.checkBackgroundDown
        target.tick = s.tick
        target.tickDisabled = s.tickDisabled
        SkinStyleReader.textButtonStyleReader.copyFrom(s, target)
      case s: TextButton.TextButtonStyle =>
        SkinStyleReader.textButtonStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: VisCheckBox.VisCheckBoxStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "focusBorder"         => obj.focusBorder = resolveNullableDrawable(skin, json)
      case "errorBorder"         => obj.errorBorder = resolveNullableDrawable(skin, json)
      case "checkBackground"     => obj.checkBackground = resolveNullableDrawable(skin, json)
      case "checkBackgroundOver" => obj.checkBackgroundOver = resolveNullableDrawable(skin, json)
      case "checkBackgroundDown" => obj.checkBackgroundDown = resolveNullableDrawable(skin, json)
      case "tick"                => obj.tick = resolveNullableDrawable(skin, json)
      case "tickDisabled"        => obj.tickDisabled = resolveNullableDrawable(skin, json)
      case _                     => SkinStyleReader.textButtonStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  /** Reader for [[VisImageButton.VisImageButtonStyle]], which extends [[Button.ButtonStyle]]. Adds the image-state drawables and a {@code focusBorder}; inherited Button fields delegate to the core
    * [[SkinStyleReader.buttonStyleReader]].
    */
  private val visImageButtonStyleReader: SkinStyleReader[VisImageButton.VisImageButtonStyle] = new SkinStyleReader[VisImageButton.VisImageButtonStyle] {

    def create(): VisImageButton.VisImageButtonStyle = new VisImageButton.VisImageButtonStyle()

    def copyFrom(source: Any, target: VisImageButton.VisImageButtonStyle): Unit = source match {
      case s: VisImageButton.VisImageButtonStyle =>
        target.imageUp = s.imageUp
        target.imageDown = s.imageDown
        target.imageOver = s.imageOver
        target.imageChecked = s.imageChecked
        target.imageCheckedOver = s.imageCheckedOver
        target.imageDisabled = s.imageDisabled
        target.focusBorder = s.focusBorder
        SkinStyleReader.buttonStyleReader.copyFrom(s, target)
      case s: Button.ButtonStyle =>
        SkinStyleReader.buttonStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: VisImageButton.VisImageButtonStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "imageUp"          => obj.imageUp = resolveNullableDrawable(skin, json)
      case "imageDown"        => obj.imageDown = resolveNullableDrawable(skin, json)
      case "imageOver"        => obj.imageOver = resolveNullableDrawable(skin, json)
      case "imageChecked"     => obj.imageChecked = resolveNullableDrawable(skin, json)
      case "imageCheckedOver" => obj.imageCheckedOver = resolveNullableDrawable(skin, json)
      case "imageDisabled"    => obj.imageDisabled = resolveNullableDrawable(skin, json)
      case "focusBorder"      => obj.focusBorder = resolveNullableDrawable(skin, json)
      case _                  => SkinStyleReader.buttonStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  /** Reader for [[VisImageTextButton.VisImageTextButtonStyle]], which extends [[VisTextButton.VisTextButtonStyle]]. Adds the image-state drawables; inherited fields (including VisUI's
    * {@code focusBorder}) delegate to [[visTextButtonStyleReader]].
    */
  private val visImageTextButtonStyleReader: SkinStyleReader[VisImageTextButton.VisImageTextButtonStyle] = new SkinStyleReader[VisImageTextButton.VisImageTextButtonStyle] {

    def create(): VisImageTextButton.VisImageTextButtonStyle = new VisImageTextButton.VisImageTextButtonStyle()

    def copyFrom(source: Any, target: VisImageTextButton.VisImageTextButtonStyle): Unit = source match {
      case s: VisImageTextButton.VisImageTextButtonStyle =>
        target.imageUp = s.imageUp
        target.imageDown = s.imageDown
        target.imageOver = s.imageOver
        target.imageChecked = s.imageChecked
        target.imageCheckedOver = s.imageCheckedOver
        target.imageDisabled = s.imageDisabled
        visTextButtonStyleReader.copyFrom(s, target)
      case s: VisTextButton.VisTextButtonStyle =>
        visTextButtonStyleReader.copyFrom(s, target)
      case s: TextButton.TextButtonStyle =>
        SkinStyleReader.textButtonStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: VisImageTextButton.VisImageTextButtonStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "imageUp"          => obj.imageUp = resolveNullableDrawable(skin, json)
      case "imageDown"        => obj.imageDown = resolveNullableDrawable(skin, json)
      case "imageOver"        => obj.imageOver = resolveNullableDrawable(skin, json)
      case "imageChecked"     => obj.imageChecked = resolveNullableDrawable(skin, json)
      case "imageCheckedOver" => obj.imageCheckedOver = resolveNullableDrawable(skin, json)
      case "imageDisabled"    => obj.imageDisabled = resolveNullableDrawable(skin, json)
      case _                  => visTextButtonStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  /** Reader for [[VisSplitPane.VisSplitPaneStyle]], which extends [[SplitPane.SplitPaneStyle]]. Adds an optional {@code handleOver}; the inherited {@code handle} delegates to the core
    * [[SkinStyleReader.splitPaneStyleReader]].
    */
  private val visSplitPaneStyleReader: SkinStyleReader[VisSplitPane.VisSplitPaneStyle] = new SkinStyleReader[VisSplitPane.VisSplitPaneStyle] {

    def create(): VisSplitPane.VisSplitPaneStyle = new VisSplitPane.VisSplitPaneStyle()

    def copyFrom(source: Any, target: VisSplitPane.VisSplitPaneStyle): Unit = source match {
      case s: VisSplitPane.VisSplitPaneStyle =>
        target.handleOver = s.handleOver
        SkinStyleReader.splitPaneStyleReader.copyFrom(s, target)
      case s: SplitPane.SplitPaneStyle =>
        SkinStyleReader.splitPaneStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: VisSplitPane.VisSplitPaneStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "handleOver" => obj.handleOver = resolveNullableDrawable(skin, json)
      case _            => SkinStyleReader.splitPaneStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  /** Reader for [[MultiSplitPane.MultiSplitPaneStyle]], which extends [[VisSplitPane.VisSplitPaneStyle]] without adding fields; delegates entirely to [[visSplitPaneStyleReader]]. */
  private val multiSplitPaneStyleReader: SkinStyleReader[MultiSplitPane.MultiSplitPaneStyle] = new SkinStyleReader[MultiSplitPane.MultiSplitPaneStyle] {

    def create(): MultiSplitPane.MultiSplitPaneStyle = new MultiSplitPane.MultiSplitPaneStyle()

    def copyFrom(source: Any, target: MultiSplitPane.MultiSplitPaneStyle): Unit = source match {
      case s: VisSplitPane.VisSplitPaneStyle => visSplitPaneStyleReader.copyFrom(s, target)
      case s: SplitPane.SplitPaneStyle       => SkinStyleReader.splitPaneStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: MultiSplitPane.MultiSplitPaneStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit =
      visSplitPaneStyleReader.setField(obj, name, json, skin, readColor, readStyle)
  }

  /** Reader for [[VisTextField.VisTextFieldStyle]], which extends [[TextField.TextFieldStyle]]. Adds VisUI's {@code focusBorder}, {@code errorBorder} and {@code backgroundOver}; inherited TextField
    * fields delegate to the core [[SkinStyleReader.textFieldStyleReader]].
    */
  private val visTextFieldStyleReader: SkinStyleReader[VisTextField.VisTextFieldStyle] = new SkinStyleReader[VisTextField.VisTextFieldStyle] {

    def create(): VisTextField.VisTextFieldStyle = new VisTextField.VisTextFieldStyle()

    def copyFrom(source: Any, target: VisTextField.VisTextFieldStyle): Unit = source match {
      case s: VisTextField.VisTextFieldStyle =>
        target.focusBorder = s.focusBorder
        target.errorBorder = s.errorBorder
        target.backgroundOver = s.backgroundOver
        SkinStyleReader.textFieldStyleReader.copyFrom(s, target)
      case s: TextField.TextFieldStyle =>
        SkinStyleReader.textFieldStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: VisTextField.VisTextFieldStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "focusBorder"    => obj.focusBorder = resolveNullableDrawable(skin, json)
      case "errorBorder"    => obj.errorBorder = resolveNullableDrawable(skin, json)
      case "backgroundOver" => obj.backgroundOver = resolveNullableDrawable(skin, json)
      case _                => SkinStyleReader.textFieldStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  /** Reader for [[ColorPickerWidgetStyle]]: the bar/cross/selector picker drawables plus an arrow icon. */
  private val colorPickerWidgetStyleReader: SkinStyleReader[ColorPickerWidgetStyle] = new SkinStyleReader[ColorPickerWidgetStyle] {

    def create(): ColorPickerWidgetStyle = new ColorPickerWidgetStyle()

    def copyFrom(source: Any, target: ColorPickerWidgetStyle): Unit = source match {
      case s: ColorPickerWidgetStyle =>
        target.barSelector = s.barSelector
        target.cross = s.cross
        target.verticalSelector = s.verticalSelector
        target.horizontalSelector = s.horizontalSelector
        target.iconArrowRight = s.iconArrowRight
      case _ => ()
    }

    def setField(obj: ColorPickerWidgetStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "barSelector"        => obj.barSelector = resolveNullableDrawable(skin, json)
      case "cross"              => obj.cross = resolveNullableDrawable(skin, json)
      case "verticalSelector"   => obj.verticalSelector = resolveNullableDrawable(skin, json)
      case "horizontalSelector" => obj.horizontalSelector = resolveNullableDrawable(skin, json)
      case "iconArrowRight"     => obj.iconArrowRight = resolveNullableDrawable(skin, json)
      case _                    => () // Unknown field, skip
    }
  }

  /** Reader for [[ColorPickerStyle]], which extends [[Window.WindowStyle]]. Adds an inline {@code pickerStyle} ([[ColorPickerWidgetStyle]]); inherited Window fields delegate to the core
    * [[SkinStyleReader.windowStyleReader]].
    */
  private val colorPickerStyleReader: SkinStyleReader[ColorPickerStyle] = new SkinStyleReader[ColorPickerStyle] {

    def create(): ColorPickerStyle = new ColorPickerStyle()

    def copyFrom(source: Any, target: ColorPickerStyle): Unit = source match {
      case s: ColorPickerStyle =>
        target.pickerStyle = s.pickerStyle
        SkinStyleReader.windowStyleReader.copyFrom(s, target)
      case s: Window.WindowStyle =>
        SkinStyleReader.windowStyleReader.copyFrom(s, target)
      case _ => ()
    }

    def setField(obj: ColorPickerStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "pickerStyle" => withStyle(skin, json, classOf[ColorPickerWidgetStyle], readStyle)(s => obj.pickerStyle = Nullable(s))
      case _             => SkinStyleReader.windowStyleReader.setField(obj, name, json, skin, readColor, readStyle)
    }
  }

  /** Reader for [[FileChooserStyle]]: an optional referenced {@code popupMenuStyle} plus a large set of icon drawables. */
  private val fileChooserStyleReader: SkinStyleReader[FileChooserStyle] = new SkinStyleReader[FileChooserStyle] {

    def create(): FileChooserStyle = new FileChooserStyle()

    def copyFrom(source: Any, target: FileChooserStyle): Unit = source match {
      case s: FileChooserStyle =>
        target.popupMenuStyle = s.popupMenuStyle
        target.highlight = s.highlight
        target.iconArrowLeft = s.iconArrowLeft
        target.iconArrowRight = s.iconArrowRight
        target.iconFolder = s.iconFolder
        target.iconFolderParent = s.iconFolderParent
        target.iconFolderStar = s.iconFolderStar
        target.iconFolderNew = s.iconFolderNew
        target.iconDrive = s.iconDrive
        target.iconTrash = s.iconTrash
        target.iconStar = s.iconStar
        target.iconStarOutline = s.iconStarOutline
        target.iconRefresh = s.iconRefresh
        target.iconListSettings = s.iconListSettings
        target.iconFileText = s.iconFileText
        target.iconFileImage = s.iconFileImage
        target.iconFilePdf = s.iconFilePdf
        target.iconFileAudio = s.iconFileAudio
        target.contextMenuSelectedItem = s.contextMenuSelectedItem
        target.expandDropdown = s.expandDropdown
      case _ => ()
    }

    def setField(obj: FileChooserStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "popupMenuStyle"          => withStyle(skin, json, classOf[PopupMenu.PopupMenuStyle], readStyle)(s => obj.popupMenuStyle = Nullable(s))
      case "highlight"               => obj.highlight = resolveNullableDrawable(skin, json)
      case "iconArrowLeft"           => obj.iconArrowLeft = resolveNullableDrawable(skin, json)
      case "iconArrowRight"          => obj.iconArrowRight = resolveNullableDrawable(skin, json)
      case "iconFolder"              => obj.iconFolder = resolveNullableDrawable(skin, json)
      case "iconFolderParent"        => obj.iconFolderParent = resolveNullableDrawable(skin, json)
      case "iconFolderStar"          => obj.iconFolderStar = resolveNullableDrawable(skin, json)
      case "iconFolderNew"           => obj.iconFolderNew = resolveNullableDrawable(skin, json)
      case "iconDrive"               => obj.iconDrive = resolveNullableDrawable(skin, json)
      case "iconTrash"               => obj.iconTrash = resolveNullableDrawable(skin, json)
      case "iconStar"                => obj.iconStar = resolveNullableDrawable(skin, json)
      case "iconStarOutline"         => obj.iconStarOutline = resolveNullableDrawable(skin, json)
      case "iconRefresh"             => obj.iconRefresh = resolveNullableDrawable(skin, json)
      case "iconListSettings"        => obj.iconListSettings = resolveNullableDrawable(skin, json)
      case "iconFileText"            => obj.iconFileText = resolveNullableDrawable(skin, json)
      case "iconFileImage"           => obj.iconFileImage = resolveNullableDrawable(skin, json)
      case "iconFilePdf"             => obj.iconFilePdf = resolveNullableDrawable(skin, json)
      case "iconFileAudio"           => obj.iconFileAudio = resolveNullableDrawable(skin, json)
      case "contextMenuSelectedItem" => obj.contextMenuSelectedItem = resolveNullableDrawable(skin, json)
      case "expandDropdown"          => obj.expandDropdown = resolveNullableDrawable(skin, json)
      case _                         => () // Unknown field, skip
    }
  }

  /** Reader for [[Spinner.SpinnerStyle]]: {@code up} / {@code down} button drawables. */
  private val spinnerStyleReader: SkinStyleReader[Spinner.SpinnerStyle] = new SkinStyleReader[Spinner.SpinnerStyle] {

    def create(): Spinner.SpinnerStyle = new Spinner.SpinnerStyle()

    def copyFrom(source: Any, target: Spinner.SpinnerStyle): Unit = source match {
      case s: Spinner.SpinnerStyle =>
        target.up = s.up
        target.down = s.down
      case _ => ()
    }

    def setField(obj: Spinner.SpinnerStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "up"   => withDrawable(skin, json)(obj.up = _)
      case "down" => withDrawable(skin, json)(obj.down = _)
      case _      => () // Unknown field, skip
    }
  }

  /** Reader for [[TabbedPane.TabbedPaneStyle]]: a {@code background} drawable, an inline {@code buttonStyle} ([[VisTextButton.VisTextButtonStyle]]), an optional {@code separatorBar} and the
    * {@code vertical} / {@code draggable} flags.
    */
  private val tabbedPaneStyleReader: SkinStyleReader[TabbedPane.TabbedPaneStyle] = new SkinStyleReader[TabbedPane.TabbedPaneStyle] {

    def create(): TabbedPane.TabbedPaneStyle = new TabbedPane.TabbedPaneStyle()

    def copyFrom(source: Any, target: TabbedPane.TabbedPaneStyle): Unit = source match {
      case s: TabbedPane.TabbedPaneStyle =>
        target.background = s.background
        target.buttonStyle = s.buttonStyle
        target.separatorBar = s.separatorBar
        target.vertical = s.vertical
        target.draggable = s.draggable
      case _ => ()
    }

    def setField(obj: TabbedPane.TabbedPaneStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background"   => withDrawable(skin, json)(obj.background = _)
      case "buttonStyle"  => withStyle(skin, json, classOf[VisTextButton.VisTextButtonStyle], readStyle)(obj.buttonStyle = _)
      case "separatorBar" => obj.separatorBar = resolveNullableDrawable(skin, json)
      case "vertical"     => obj.vertical = resolveBoolean(json)
      case "draggable"    => obj.draggable = resolveBoolean(json)
      case _              => () // Unknown field, skip
    }
  }

  /** Reader for [[Toast.ToastStyle]]: a {@code background} drawable and a referenced {@code closeButtonStyle} ([[VisImageButton.VisImageButtonStyle]]). */
  private val toastStyleReader: SkinStyleReader[Toast.ToastStyle] = new SkinStyleReader[Toast.ToastStyle] {

    def create(): Toast.ToastStyle = new Toast.ToastStyle()

    def copyFrom(source: Any, target: Toast.ToastStyle): Unit = source match {
      case s: Toast.ToastStyle =>
        target.background = s.background
        target.closeButtonStyle = s.closeButtonStyle
      case _ => ()
    }

    def setField(obj: Toast.ToastStyle, name: String, json: Json, skin: Skin, readColor: Json => Color, readStyle: (Class[?], Json) => Any): Unit = name match {
      case "background"       => withDrawable(skin, json)(obj.background = _)
      case "closeButtonStyle" => withStyle(skin, json, classOf[VisImageButton.VisImageButtonStyle], readStyle)(obj.closeButtonStyle = _)
      case _                  => () // Unknown field, skip
    }
  }

  /** Registers VisUI's JSON class tags and style readers with the core Skin seam. Idempotent. Called by [[VisUI.load]] before any skin is parsed, so the VisUI-typed skin sections resolve. */
  def register(): Unit =
    if (!registered) {
      synchronized {
        if (!registered) {
          // VisTextButtonStyle's skin entries carry no JSON "parent" field, but its
          // class extends TextButtonStyle (-> ButtonStyle); register the hierarchy so a
          // future "parent" reference would resolve, mirroring the built-in styleParentTypes.
          val buttonStyleChain   = List(classOf[Button.ButtonStyle])
          val textButtonChain    = classOf[TextButton.TextButtonStyle] :: buttonStyleChain
          val visTextButtonChain = classOf[VisTextButton.VisTextButtonStyle] :: textButtonChain
          val splitPaneChain     = List(classOf[SplitPane.SplitPaneStyle])
          val visSplitPaneChain  = classOf[VisSplitPane.VisSplitPaneStyle] :: splitPaneChain
          val popupMenuChain     = List(classOf[PopupMenu.PopupMenuStyle])
          val labelChain         = List(classOf[Label.LabelStyle])
          val windowChain        = List(classOf[Window.WindowStyle])
          val textFieldChain     = List(classOf[TextField.TextFieldStyle])

          Skin.registerJsonClassTags(
            Map(
              "com.kotcrab.vis.ui.Sizes" -> classOf[Sizes],
              "com.kotcrab.vis.ui.util.adapter.SimpleListAdapter$SimpleListAdapterStyle" -> classOf[SimpleListAdapter.SimpleListAdapterStyle],
              "com.kotcrab.vis.ui.util.form.SimpleFormValidator$FormValidatorStyle" -> classOf[SimpleFormValidator.FormValidatorStyle],
              "com.kotcrab.vis.ui.widget.BusyBar$BusyBarStyle" -> classOf[BusyBar.BusyBarStyle],
              "com.kotcrab.vis.ui.widget.LinkLabel$LinkLabelStyle" -> classOf[LinkLabel.LinkLabelStyle],
              "com.kotcrab.vis.ui.widget.ListViewStyle" -> classOf[ListViewStyle],
              "com.kotcrab.vis.ui.widget.Menu$MenuStyle" -> classOf[Menu.MenuStyle],
              "com.kotcrab.vis.ui.widget.MenuBar$MenuBarStyle" -> classOf[MenuBar.MenuBarStyle],
              "com.kotcrab.vis.ui.widget.MenuItem$MenuItemStyle" -> classOf[MenuItem.MenuItemStyle],
              "com.kotcrab.vis.ui.widget.MultiSplitPane$MultiSplitPaneStyle" -> classOf[MultiSplitPane.MultiSplitPaneStyle],
              "com.kotcrab.vis.ui.widget.PopupMenu$PopupMenuStyle" -> classOf[PopupMenu.PopupMenuStyle],
              "com.kotcrab.vis.ui.widget.Separator$SeparatorStyle" -> classOf[Separator.SeparatorStyle],
              "com.kotcrab.vis.ui.widget.Tooltip$TooltipStyle" -> classOf[Tooltip.TooltipStyle],
              "com.kotcrab.vis.ui.widget.VisCheckBox$VisCheckBoxStyle" -> classOf[VisCheckBox.VisCheckBoxStyle],
              "com.kotcrab.vis.ui.widget.VisImageButton$VisImageButtonStyle" -> classOf[VisImageButton.VisImageButtonStyle],
              "com.kotcrab.vis.ui.widget.VisImageTextButton$VisImageTextButtonStyle" -> classOf[VisImageTextButton.VisImageTextButtonStyle],
              "com.kotcrab.vis.ui.widget.VisSplitPane$VisSplitPaneStyle" -> classOf[VisSplitPane.VisSplitPaneStyle],
              "com.kotcrab.vis.ui.widget.VisTextButton$VisTextButtonStyle" -> classOf[VisTextButton.VisTextButtonStyle],
              "com.kotcrab.vis.ui.widget.VisTextField$VisTextFieldStyle" -> classOf[VisTextField.VisTextFieldStyle],
              "com.kotcrab.vis.ui.widget.color.ColorPickerStyle" -> classOf[ColorPickerStyle],
              "com.kotcrab.vis.ui.widget.color.ColorPickerWidgetStyle" -> classOf[ColorPickerWidgetStyle],
              "com.kotcrab.vis.ui.widget.file.FileChooserStyle" -> classOf[FileChooserStyle],
              "com.kotcrab.vis.ui.widget.spinner.Spinner$SpinnerStyle" -> classOf[Spinner.SpinnerStyle],
              "com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane$TabbedPaneStyle" -> classOf[TabbedPane.TabbedPaneStyle],
              "com.kotcrab.vis.ui.widget.toast.Toast$ToastStyle" -> classOf[Toast.ToastStyle]
            ),
            // Style-class parent hierarchies, mirroring the built-in styleParentTypes.
            // The VisUI skin JSON does not use "parent" today, but registering the
            // hierarchies keeps a future "parent" reference resolvable.
            Map(
              classOf[VisTextButton.VisTextButtonStyle] -> textButtonChain,
              classOf[LinkLabel.LinkLabelStyle] -> labelChain,
              classOf[Menu.MenuStyle] -> popupMenuChain,
              classOf[MenuItem.MenuItemStyle] -> textButtonChain,
              classOf[MultiSplitPane.MultiSplitPaneStyle] -> visSplitPaneChain,
              classOf[VisCheckBox.VisCheckBoxStyle] -> textButtonChain,
              classOf[VisImageButton.VisImageButtonStyle] -> buttonStyleChain,
              classOf[VisImageTextButton.VisImageTextButtonStyle] -> visTextButtonChain,
              classOf[VisSplitPane.VisSplitPaneStyle] -> splitPaneChain,
              classOf[VisTextField.VisTextFieldStyle] -> textFieldChain,
              classOf[ColorPickerStyle] -> windowChain
            )
          )
          SkinStyleReader.register(
            Map(
              classOf[Sizes] -> sizesReader,
              classOf[SimpleListAdapter.SimpleListAdapterStyle] -> simpleListAdapterStyleReader,
              classOf[SimpleFormValidator.FormValidatorStyle] -> formValidatorStyleReader,
              classOf[BusyBar.BusyBarStyle] -> busyBarStyleReader,
              classOf[LinkLabel.LinkLabelStyle] -> linkLabelStyleReader,
              classOf[ListViewStyle] -> listViewStyleReader,
              classOf[Menu.MenuStyle] -> menuStyleReader,
              classOf[MenuBar.MenuBarStyle] -> menuBarStyleReader,
              classOf[MenuItem.MenuItemStyle] -> menuItemStyleReader,
              classOf[MultiSplitPane.MultiSplitPaneStyle] -> multiSplitPaneStyleReader,
              classOf[PopupMenu.PopupMenuStyle] -> popupMenuStyleReader,
              classOf[Separator.SeparatorStyle] -> separatorStyleReader,
              classOf[Tooltip.TooltipStyle] -> tooltipStyleReader,
              classOf[VisCheckBox.VisCheckBoxStyle] -> visCheckBoxStyleReader,
              classOf[VisImageButton.VisImageButtonStyle] -> visImageButtonStyleReader,
              classOf[VisImageTextButton.VisImageTextButtonStyle] -> visImageTextButtonStyleReader,
              classOf[VisSplitPane.VisSplitPaneStyle] -> visSplitPaneStyleReader,
              classOf[VisTextButton.VisTextButtonStyle] -> visTextButtonStyleReader,
              classOf[VisTextField.VisTextFieldStyle] -> visTextFieldStyleReader,
              classOf[ColorPickerStyle] -> colorPickerStyleReader,
              classOf[ColorPickerWidgetStyle] -> colorPickerWidgetStyleReader,
              classOf[FileChooserStyle] -> fileChooserStyleReader,
              classOf[Spinner.SpinnerStyle] -> spinnerStyleReader,
              classOf[TabbedPane.TabbedPaneStyle] -> tabbedPaneStyleReader,
              classOf[Toast.ToastStyle] -> toastStyleReader
            )
          )
          registered = true
        }
      }
    }
}
