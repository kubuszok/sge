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
import sge.scenes.scene2d.ui.{ Skin, SkinStyleReader, TextButton }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Json, SgeError }
import lowlevel.Nullable
import sge.visui.widget.VisTextButton

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

  /** Registers VisUI's JSON class tags and style readers with the core Skin seam. Idempotent. Called by [[VisUI.load]] before any skin is parsed, so the VisUI-typed skin sections resolve. */
  def register(): Unit =
    if (!registered) {
      synchronized {
        if (!registered) {
          // VisTextButtonStyle's skin entries carry no JSON "parent" field, but its
          // class extends TextButtonStyle (-> ButtonStyle); register the hierarchy so a
          // future "parent" reference would resolve, mirroring the built-in styleParentTypes.
          Skin.registerJsonClassTags(
            Map(
              "com.kotcrab.vis.ui.Sizes" -> classOf[Sizes],
              "com.kotcrab.vis.ui.widget.VisTextButton$VisTextButtonStyle" -> classOf[VisTextButton.VisTextButtonStyle]
            ),
            Map(
              classOf[VisTextButton.VisTextButtonStyle] ->
                List(classOf[TextButton.TextButtonStyle], classOf[sge.scenes.scene2d.ui.Button.ButtonStyle])
            )
          )
          SkinStyleReader.register(
            Map(
              classOf[Sizes] -> sizesReader,
              classOf[VisTextButton.VisTextButtonStyle] -> visTextButtonStyleReader
            )
          )
          registered = true
        }
      }
    }
}
