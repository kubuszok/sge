/*
 * sge's DesktopFileHandle (sge/src/main/scaladesktop/sge/files/DesktopFileHandle.scala): the port's
 * FileHandle carries the external storage path as a field given at construction, so the value is
 * assigned at the head of the body instead of passed to the super constructor (ADJUSTMENTS.tsv).
 */
package sge
package files

import java.io.File
import lowlevel.Nullable

class DesktopFileHandle(internalFile: File, fileType: FileType, externalPath: String) extends FileHandle(internalFile, fileType) {
  this.externalStoragePath = Nullable(externalPath)

  def this(fileName: String, fileType: FileType, externalPath: String) =
    this(new File(fileName), fileType, externalPath)

  override def child(name: String): FileHandle =
    if (internalFile.getPath().length() == 0) DesktopFileHandle(new File(name), fileType, externalPath)
    else DesktopFileHandle(new File(internalFile, name), fileType, externalPath)

  override def sibling(name: String): FileHandle = {
    if (internalFile.getPath().length() == 0) throw utils.SgeError.FileReadError(this, "Cannot get the sibling of the root.")
    DesktopFileHandle(new File(internalFile.getParent(), name), fileType, externalPath)
  }

  override def parent(): FileHandle = {
    val p = internalFile.getParentFile()
    if (p == null) {
      if (fileType == FileType.Absolute) DesktopFileHandle(new File("/"), fileType, externalPath)
      else DesktopFileHandle(new File(""), fileType, externalPath)
    } else DesktopFileHandle(p, fileType, externalPath)
  }

  override def file: File =
    if (fileType == FileType.External) new File(externalPath, internalFile.getPath())
    else if (fileType == FileType.Local) new File(DesktopFileHandle.localPath, internalFile.getPath())
    else internalFile
}

object DesktopFileHandle {
  val localPath: String = new File("").getAbsolutePath() + File.separator
}
