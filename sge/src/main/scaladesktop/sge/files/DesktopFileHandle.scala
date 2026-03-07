/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-headless/.../HeadlessFileHandle.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: HeadlessFileHandle -> DesktopFileHandle (reused by desktop backend)
 *   Convention: child/sibling/parent return DesktopFileHandle; file() resolves external/local paths
 *   Idiom: split packages; externalStoragePath passed to parent FileHandle
 *   Audited: 2026-03-05
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package files

import java.io.File
import sge.utils.Nullable

/** A [[FileHandle]] backed by `java.io.File` for desktop and headless environments.
  *
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
class DesktopFileHandle(file: File, fileType: FileType, externalStoragePath: String) extends FileHandle(file, fileType, Nullable(externalStoragePath)) {

  def this(fileName: String, fileType: FileType, externalStoragePath: String) = {
    this(new File(fileName), fileType, externalStoragePath)
  }

  override def child(name: String): FileHandle =
    if (file.getPath().length() == 0) DesktopFileHandle(new File(name), fileType, externalStoragePath)
    else DesktopFileHandle(new File(file, name), fileType, externalStoragePath)

  override def sibling(name: String): FileHandle = {
    if (file.getPath().length() == 0) throw utils.SgeError.FileReadError(this, "Cannot get the sibling of the root.")
    DesktopFileHandle(new File(file.getParent(), name), fileType, externalStoragePath)
  }

  override def parent(): FileHandle =
    Nullable(file.getParentFile()).fold {
      if (fileType == FileType.Absolute) DesktopFileHandle(new File("/"), fileType, externalStoragePath)
      else DesktopFileHandle(new File(""), fileType, externalStoragePath)
    } { parent =>
      DesktopFileHandle(parent, fileType, externalStoragePath)
    }

  override def getFile(): File =
    if (fileType == FileType.External) new File(externalStoragePath, file.getPath())
    else if (fileType == FileType.Local) new File(DesktopFileHandle.localPath, file.getPath())
    else file
}

object DesktopFileHandle {
  val localPath: String = new File("").getAbsolutePath() + File.separator
}
