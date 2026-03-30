/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui

import sge.files.FileHandle
import sge.scenes.scene2d.ui.Skin
import sge.utils.{ Align, Nullable }

/** Allows to easily load VisUI skin and change default title alignment and I18N bundles. Contains static field with VisUI version.
  * @author
  *   Kotcrab
  */
object VisUI {

  private var _defaultTitleAlign: Align               = Align.left
  private var _scale:             Nullable[SkinScale] = Nullable.empty
  private var _skin:              Nullable[Skin]      = Nullable.empty
  // Cached Sge instance for use by Locales and other utilities that need it after load
  private[visui] var _sgeInstance: Nullable[Sge] = Nullable.empty

  /** Defines possible built-in skin scales. */
  enum SkinScale(val classpath: String, val sizesName: String) {

    /** Standard VisUI skin */
    case X1 extends SkinScale("com/kotcrab/vis/ui/skin/x1/uiskin.json", "default")

    /** VisUI skin 2x upscaled */
    case X2 extends SkinScale("com/kotcrab/vis/ui/skin/x2/uiskin.json", "x2")

    def getSkinFile(using Sge): FileHandle = Sge().files.classpath(classpath)
  }

  /** Loads default VisUI skin with [[SkinScale.X1]]. */
  def load()(using Sge): Unit = load(SkinScale.X1)

  /** Loads default VisUI skin for given [[SkinScale]]. */
  def load(scale: SkinScale)(using Sge): Unit = {
    _scale = Nullable(scale)
    load(scale.getSkinFile)
  }

  /** Loads skin from provided internal file path. Skin must be compatible with default VisUI skin. */
  def load(internalVisSkinPath: String)(using Sge): Unit =
    load(Sge().files.internal(internalVisSkinPath))

  /** Loads skin from provided file. Skin must be compatible with default VisUI skin. */
  def load(visSkinFile: FileHandle)(using Sge): Unit = {
    checkBeforeLoad()
    _sgeInstance = Nullable(summon[Sge])
    _skin = Nullable(new Skin(visSkinFile))
  }

  /** Sets provided skin as default for every VisUI widget. Skin must be compatible with default VisUI skin. This can be used if you prefer to load skin manually for example by using AssetManager.
    */
  def load(skin: Skin)(using Sge): Unit = {
    checkBeforeLoad()
    _sgeInstance = Nullable(summon[Sge])
    _skin = Nullable(skin)
  }

  private def checkBeforeLoad()(using Sge): Unit =
    if (_skin.isDefined) throw new IllegalStateException("VisUI cannot be loaded twice")

  /** Unloads VisUI. */
  def dispose(): Unit = dispose(true)

  /** Unloads VisUI.
    * @param disposeSkin
    *   if true then internal skin instance will be disposed
    */
  def dispose(disposeSkin: Boolean): Unit = {
    _skin.foreach { s =>
      if (disposeSkin) s.close()
    }
    _skin = Nullable.empty
    _sgeInstance = Nullable.empty
  }

  def getSkin: Skin = {
    if (_skin.isEmpty) throw new IllegalStateException("VisUI is not loaded!")
    _skin.get
  }

  def isLoaded: Boolean = _skin.isDefined

  def getSizes: Sizes =
    if (_scale.isEmpty) getSkin.get[Sizes]
    else getSkin.get[Sizes](_scale.get.sizesName)

  /** @return int value from [[Align]] */
  def defaultTitleAlign: Align = _defaultTitleAlign

  /** Sets default title align used for VisWindow and VisDialog.
    * @param align
    *   value from [[Align]]
    */
  def defaultTitleAlign_=(align: Align): Unit = _defaultTitleAlign = align

  // Alias for Java-style access
  def getDefaultTitleAlign:               Align = _defaultTitleAlign
  def setDefaultTitleAlign(align: Align): Unit  = _defaultTitleAlign = align

  /** Returns the cached Sge instance from when VisUI was loaded. */
  private[visui] def sgeInstance: Sge = {
    if (_sgeInstance.isEmpty) throw new IllegalStateException("VisUI is not loaded!")
    _sgeInstance.get
  }
}
