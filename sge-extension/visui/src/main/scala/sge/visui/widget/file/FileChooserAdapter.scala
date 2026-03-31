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

/** Empty implementation of [[FileChooserListener]].
  * @author
  *   Kotcrab
  */
class FileChooserAdapter extends FileChooserListener {
  override def canceled():                                Unit = ()
  override def selected(files: DynamicArray[FileHandle]): Unit = ()
}
