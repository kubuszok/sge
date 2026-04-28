/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 39
 * Covenant-baseline-methods: ColorPickerText,format,get,getBundle,name,toString
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/internal/ColorPickerText.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package color
package internal

import sge.utils.I18NBundle
import sge.visui.Locales
import sge.visui.i18n.BundleText

/** Contains texts for chooser access via I18NBundle.
  * @author
  *   Kotcrab
  * @since 0.7.0
  */
enum ColorPickerText(val entryName: String) extends BundleText {
  case TITLE extends ColorPickerText("title")
  case RESTORE extends ColorPickerText("restore")
  case CANCEL extends ColorPickerText("cancel")
  case OK extends ColorPickerText("ok")
  case HEX extends ColorPickerText("hex")

  override def name:                       String = entryName
  override def get:                        String = ColorPickerText.getBundle.get(entryName)
  override def format():                   String = ColorPickerText.getBundle.format(entryName)
  override def format(arguments: AnyRef*): String = ColorPickerText.getBundle.format(entryName, arguments*)
  override def toString:                   String = get
}

object ColorPickerText {
  private def getBundle: I18NBundle = Locales.getColorPickerBundle(using VisUI.sgeInstance)
}
