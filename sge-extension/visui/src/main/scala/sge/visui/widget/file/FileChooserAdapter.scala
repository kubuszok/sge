/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 23
 * Covenant-baseline-methods: FileChooserAdapter,canceled,selected
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/FileChooserAdapter.java
 * Covenant-verified: 2026-04-19
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
