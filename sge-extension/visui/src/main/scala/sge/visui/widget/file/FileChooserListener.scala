/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget
package file

import sge.files.FileHandle
import sge.utils.DynamicArray

/** Used to get events from [[FileChooser]].
  * @author
  *   Kotcrab
  */
trait FileChooserListener {

  /** Called when user finished selecting files. It is guaranteed that array will contain at least one file. */
  def selected(files: DynamicArray[FileHandle]): Unit

  /** Called when selection dialog was canceled by user. */
  def canceled(): Unit
}
