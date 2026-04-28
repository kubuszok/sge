/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 57
 * Covenant-baseline-methods: Sizes,borderSize,buttonBarSpacing,fileChooserViewModeBigIconsSize,fileChooserViewModeListWidthSize,fileChooserViewModeMediumIconsSize,fileChooserViewModeSmallIconsSize,menuItemIconSize,scaleFactor,spacingBottom,spacingLeft,spacingRight,spacingTop,spinnerButtonHeight,spinnerFieldSize,this
 * Covenant-source-reference: com/kotcrab/vis/ui/Sizes.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui

/** VisUI padding, spacings and sizes. Loaded from skin, will differ between different skin sizes. If you are using custom skin it must contain "default" definition of Sizes values.
  * @author
  *   Kotcrab
  */
class Sizes() {
  var scaleFactor: Float = 0

  var spacingTop:    Float = 0
  var spacingBottom: Float = 0
  var spacingRight:  Float = 0
  var spacingLeft:   Float = 0

  var buttonBarSpacing: Float = 0

  var menuItemIconSize: Float = 0

  /** Size of focus border. 1 for standard Vis skin. This is used to avoid showing overlapping borders when two widgets have borders (for example button can have its own focus border which without
    * this padding would overlap with menu border)
    */
  var borderSize: Float = 0

  var spinnerButtonHeight: Float = 0
  var spinnerFieldSize:    Float = 0

  var fileChooserViewModeBigIconsSize:    Float = 0
  var fileChooserViewModeMediumIconsSize: Float = 0
  var fileChooserViewModeSmallIconsSize:  Float = 0
  var fileChooserViewModeListWidthSize:   Float = 0

  def this(other: Sizes) = {
    this()
    this.scaleFactor = other.scaleFactor
    this.spacingTop = other.spacingTop
    this.spacingBottom = other.spacingBottom
    this.spacingRight = other.spacingRight
    this.spacingLeft = other.spacingLeft
    this.buttonBarSpacing = other.buttonBarSpacing
    this.menuItemIconSize = other.menuItemIconSize
    this.borderSize = other.borderSize
    this.spinnerButtonHeight = other.spinnerButtonHeight
    this.spinnerFieldSize = other.spinnerFieldSize
    this.fileChooserViewModeBigIconsSize = other.fileChooserViewModeBigIconsSize
    this.fileChooserViewModeMediumIconsSize = other.fileChooserViewModeMediumIconsSize
    this.fileChooserViewModeSmallIconsSize = other.fileChooserViewModeSmallIconsSize
    this.fileChooserViewModeListWidthSize = other.fileChooserViewModeListWidthSize
  }
}
