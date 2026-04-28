/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: SingleFileChooserListener,canceled,selected
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/SingleFileChooserListener.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package file

import sge.files.FileHandle
import sge.utils.DynamicArray

/** Implementation of [[FileChooserListener]] that can be used when user picks only one file. Provides convenient [[selected(FileHandle)]] method. If user picked more than one file (note that chooser
  * must be in multiple select mode for that to happen, see [[FileChooser.selectionMode_=]]), that method will be called only for first selected file and remaining files will be ignored.
  * @author
  *   Kotcrab
  * @since 1.0.0
  */
abstract class SingleFileChooserListener extends FileChooserListener {
  final override def selected(files: DynamicArray[FileHandle]): Unit =
    selected(files.first)

  /** Called for first file in selection. See [[SingleFileChooserListener]]. */
  protected def selected(file: FileHandle): Unit

  override def canceled(): Unit = ()
}
