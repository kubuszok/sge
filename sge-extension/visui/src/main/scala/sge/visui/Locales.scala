/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui

import java.util.Locale

import sge.utils.{ I18NBundle, Nullable }
import sge.visui.i18n.BundleText

/** Manages VisUI's I18N bundles.
  * @author
  *   Kotcrab
  */
object Locales {
  @SuppressWarnings(Array("deprecation"))
  private var _locale:           Locale               = Locale.of("en")
  private var commonBundle:      Nullable[I18NBundle] = Nullable.empty
  private var buttonBarBundle:   Nullable[I18NBundle] = Nullable.empty
  private var dialogsBundle:     Nullable[I18NBundle] = Nullable.empty
  private var tabbedPaneBundle:  Nullable[I18NBundle] = Nullable.empty
  private var colorPickerBundle: Nullable[I18NBundle] = Nullable.empty
  private var fileChooserBundle: Nullable[I18NBundle] = Nullable.empty

  /** Returns common I18N bundle. If current bundle is null, a default bundle is set and returned. */
  def getCommonBundle(using Sge): I18NBundle = {
    if (commonBundle.isEmpty) commonBundle = Nullable(getBundle("com/kotcrab/vis/ui/i18n/Common"))
    commonBundle.get
  }

  /** Changes common bundle. Since this bundle may be used by multiple VisUI parts it should be changed before loading VisUI. If set to null then [[getCommonBundle]] will return default bundle.
    */
  def setCommonBundle(bundle: Nullable[I18NBundle]): Unit = commonBundle = bundle

  /** Returns I18N bundle used by Dialogs, if current bundle is null, a default bundle is set and returned. */
  def getDialogsBundle(using Sge): I18NBundle = {
    if (dialogsBundle.isEmpty) dialogsBundle = Nullable(getBundle("com/kotcrab/vis/ui/i18n/Dialogs"))
    dialogsBundle.get
  }

  /** Changes bundle used by Dialogs, will not affect already created dialogs. If set to null then [[getDialogsBundle]] will return default bundle.
    */
  def setDialogsBundle(bundle: Nullable[I18NBundle]): Unit = dialogsBundle = bundle

  /** Returns I18N bundle used by TabbedPane, if current bundle is null, a default bundle is set and returned. */
  def getTabbedPaneBundle(using Sge): I18NBundle = {
    if (tabbedPaneBundle.isEmpty) tabbedPaneBundle = Nullable(getBundle("com/kotcrab/vis/ui/i18n/TabbedPane"))
    tabbedPaneBundle.get
  }

  /** Changes bundle used by TabbedPane, will not affect already created TabbedPanes. If set to null then [[getTabbedPaneBundle]] will return default bundle.
    */
  def setTabbedPaneBundle(bundle: Nullable[I18NBundle]): Unit = tabbedPaneBundle = bundle

  /** Returns I18N bundle used by ButtonBar, if current bundle is null, a default bundle is set and returned. */
  def getButtonBarBundle(using Sge): I18NBundle = {
    if (buttonBarBundle.isEmpty) buttonBarBundle = Nullable(getBundle("com/kotcrab/vis/ui/i18n/ButtonBar"))
    buttonBarBundle.get
  }

  /** Changes bundle used by ButtonBar, will not affect already created bars. If set to null then [[getButtonBarBundle]] will return default bundle.
    */
  def setButtonBarBundle(bundle: Nullable[I18NBundle]): Unit = buttonBarBundle = bundle

  /** Returns I18N bundle used by ColorPicker, if current bundle is null, a default bundle is set and returned. */
  def getColorPickerBundle(using Sge): I18NBundle = {
    if (colorPickerBundle.isEmpty) colorPickerBundle = Nullable(getBundle("com/kotcrab/vis/ui/i18n/ColorPicker"))
    colorPickerBundle.get
  }

  /** Changes bundle used by ColorPicker. If set to null then [[getColorPickerBundle]] will return default bundle. */
  def setColorPickerBundle(bundle: Nullable[I18NBundle]): Unit = colorPickerBundle = bundle

  /** Returns I18N bundle used by FileChooser, if current bundle is null, a default bundle is set and returned. */
  def getFileChooserBundle(using Sge): I18NBundle = {
    if (fileChooserBundle.isEmpty) fileChooserBundle = Nullable(getBundle("com/kotcrab/vis/ui/i18n/FileChooser"))
    fileChooserBundle.get
  }

  /** Changes bundle used by FileChooser. If set to null then [[getFileChooserBundle]] will return default bundle. */
  def setFileChooserBundle(bundle: Nullable[I18NBundle]): Unit = fileChooserBundle = bundle

  /** Changes current locale, this should be done when VisUI isn't loaded yet because changing this won't affect bundles that are already loaded.
    */
  def locale:                   Locale = _locale
  def locale_=(locale: Locale): Unit   = _locale = locale

  private def getBundle(path: String)(using Sge): I18NBundle = {
    val bundleFile = Sge().files.classpath(path)
    I18NBundle.createBundle(bundleFile, _locale)
  }

  /** Common text bundle entries. */
  enum CommonText(val entryName: String) extends BundleText {
    case PleaseWait extends CommonText("pleaseWait")
    case UnknownErrorOccured extends CommonText("unknownErrorOccurred")

    override def name:                       String = entryName
    override def get:                        String = getCommonBundle(using VisUI.sgeInstance).get(entryName)
    override def format():                   String = getCommonBundle(using VisUI.sgeInstance).format(entryName)
    override def format(arguments: AnyRef*): String = getCommonBundle(using VisUI.sgeInstance).format(entryName, arguments*)
    override def toString:                   String = get
  }
}
