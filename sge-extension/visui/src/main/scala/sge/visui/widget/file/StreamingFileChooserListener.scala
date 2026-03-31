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

/** Implementation of [[FileChooserListener]] that streams chooser selection. Provides convenient [[selected(FileHandle)]] method that will be called for every selected file after user finished
  * choosing files. Before streaming starts [[begin]] is called, after streaming has finished [[end]] is called.
  * @author
  *   Kotcrab
  * @since 1.0.0
  */
abstract class StreamingFileChooserListener extends FileChooserListener {
  final override def selected(files: DynamicArray[FileHandle]): Unit = {
    begin()
    val iter = files.iterator
    while (iter.hasNext)
      selected(iter.next())
    end()
  }

  /** Called after user finished selecting files. If user picked multiple files this will be called separately for every selected file. */
  def selected(file: FileHandle): Unit

  /** Called after user finished selecting files, before streaming started. */
  def begin(): Unit = ()

  /** Called after file selection streaming has finished. */
  def end(): Unit = ()

  override def canceled(): Unit = ()
}
