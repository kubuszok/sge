/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-android/.../AndroidFileHandle.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AndroidFileHandle (same name, different package)
 *   Convention: uses FilesOps for AssetManager operations; child/sibling/parent return AndroidFileHandle
 *   Idiom: split packages; Nullable; no return (boundary/break)
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package files

import java.io.{ File, FileInputStream, IOException, InputStream }
import java.nio.{ ByteBuffer, ByteOrder }
import java.nio.channels.FileChannel
import sge.platform.android.FilesOps
import sge.utils.Nullable

/** A [[FileHandle]] backed by Android's AssetManager for Internal files and standard file I/O for other types.
  *
  * Delegates Internal (asset) file operations to [[FilesOps]], which abstracts the Android AssetManager. Other file types behave identically to the base [[FileHandle]].
  *
  * @param filesOps
  *   the Android files operations (AssetManager + storage paths)
  */
class AndroidFileHandle(internalFile: File, fileType: FileType, private val filesOps: FilesOps)
    extends FileHandle(
      internalFile,
      fileType,
      Nullable(filesOps.externalStoragePath)
    ) {

  def this(fileName: String, fileType: FileType, filesOps: FilesOps) = {
    this(new File(fileName.replace('\\', '/')), fileType, filesOps)
  }

  override def child(name: String): FileHandle = {
    val n = name.replace('\\', '/')
    if (internalFile.getPath().length() == 0) AndroidFileHandle(new File(n), fileType, filesOps)
    else AndroidFileHandle(new File(internalFile, n), fileType, filesOps)
  }

  override def sibling(name: String): FileHandle = {
    val n = name.replace('\\', '/')
    if (internalFile.getPath().length() == 0) throw utils.SgeError.FileReadError(this, "Cannot get the sibling of the root.")
    AndroidFileHandle(new File(internalFile.getParent(), n), fileType, filesOps)
  }

  override def parent(): FileHandle =
    Nullable(internalFile.getParentFile()).fold {
      if (fileType == FileType.Absolute) AndroidFileHandle(new File("/"), fileType, filesOps)
      else AndroidFileHandle(new File(""), fileType, filesOps)
    } { parent =>
      AndroidFileHandle(parent, fileType, filesOps)
    }

  override def read(): InputStream =
    if (fileType == FileType.Internal) {
      try
        filesOps.openInternal(internalFile.getPath())
      catch {
        case ex: IOException =>
          throw utils.SgeError.FileReadError(this, s"Error reading file: $internalFile ($fileType)", Some(ex))
      }
    } else {
      super.read()
    }

  override def map(mode: FileChannel.MapMode): ByteBuffer =
    if (fileType == FileType.Internal) {
      val fdResult = filesOps.openInternalFd(internalFile.getPath())
      if (fdResult == null) {
        throw utils.SgeError.FileReadError(this, s"Error memory mapping file: $this ($fileType)")
      }
      val (fd, startOffset, declaredLength) = fdResult
      var input: FileInputStream = null // scalafix:ok
      try {
        input = new FileInputStream(fd)
        val mapped = input.getChannel().map(mode, startOffset, declaredLength)
        mapped.order(ByteOrder.nativeOrder())
        mapped
      } catch {
        case ex: Exception =>
          throw utils.SgeError.FileReadError(this, s"Error memory mapping file: $this ($fileType)", Some(ex))
      } finally
        utils.StreamUtils.closeQuietly(input)
    } else {
      super.map(mode)
    }

  override def list(): Array[FileHandle] =
    if (fileType == FileType.Internal) {
      try {
        val relativePaths = filesOps.listInternal(internalFile.getPath())
        relativePaths.map(name => AndroidFileHandle(new File(internalFile, name), fileType, filesOps): FileHandle)
      } catch {
        case ex: Exception =>
          throw utils.SgeError.FileReadError(this, s"Error listing children: $internalFile ($fileType)", Some(ex))
      }
    } else {
      super.list()
    }

  override def list(suffix: String): Array[FileHandle] =
    if (fileType == FileType.Internal) {
      try {
        val relativePaths = filesOps.listInternal(internalFile.getPath())
        relativePaths.filter(_.endsWith(suffix)).map(name => AndroidFileHandle(new File(internalFile, name), fileType, filesOps): FileHandle)
      } catch {
        case ex: Exception =>
          throw utils.SgeError.FileReadError(this, s"Error listing children: $internalFile ($fileType)", Some(ex))
      }
    } else {
      super.list(suffix)
    }

  override def isDirectory(): Boolean =
    if (fileType == FileType.Internal) {
      try
        filesOps.listInternal(internalFile.getPath()).length > 0
      catch {
        case _: IOException => false
      }
    } else {
      super.isDirectory()
    }

  override def exists(): Boolean =
    if (fileType == FileType.Internal) {
      try {
        filesOps.openInternal(internalFile.getPath()).close()
        true
      } catch {
        case _: Exception =>
          // Slow fallback for directories
          try
            filesOps.listInternal(internalFile.getPath()).length > 0
          catch {
            case _: Exception => false
          }
      }
    } else {
      super.exists()
    }

  override def length(): Long =
    if (fileType == FileType.Internal) {
      val len = filesOps.internalFileLength(internalFile.getPath())
      if (len >= 0) len else 0L
    } else {
      super.length()
    }

  override def file: File =
    if (fileType == FileType.External) {
      val ext = filesOps.externalStoragePath
      if (ext != null) new File(ext, internalFile.getPath())
      else internalFile
    } else if (fileType == FileType.Local) {
      new File(filesOps.localStoragePath, internalFile.getPath())
    } else {
      internalFile
    }
}
