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
package internal

import sge.files.FileHandle

final case class FileHandleMetadata private (
    name: String,
    isDirectory: Boolean,
    lastModified: Long,
    length: Long,
    readableFileSize: String
)

object FileHandleMetadata {
  def of(file: FileHandle): FileHandleMetadata = {
    val len = file.length()
    FileHandleMetadata(
      name = file.name,
      isDirectory = file.isDirectory(),
      lastModified = file.lastModified(),
      length = len,
      readableFileSize = FileUtils.readableFileSize(len)
    )
  }
}
