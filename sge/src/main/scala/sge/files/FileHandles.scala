/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/files/FileHandle.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: GdxRuntimeException -> SgeError.FileReadError/FileWriteError, type() -> fileType (val), file() -> file (val) + file
 *   Merged with: FileHandleStream.java (abstract class appended to this file)
 *   Convention: static utility methods (tempFile, tempDirectory) omitted; static private helpers converted to private instance methods;
 *     convenience constructors FileHandle(String), FileHandle(File), no-arg FileHandle() omitted — primary constructor is (File, FileType)
 *   Idiom: Nullable (null -> Nullable for charset params, Java null returns), split packages (sge / files)
 *   Convention: file External path resolution via externalStoragePath constructor param
 *     (replaces Java's Gdx.files.getExternalStoragePath() global static)
 *   Fixes: read() uses file for resolved path; toString() uses raw file (matching Java);
 *     RuntimeException → SgeError.FileReadError in list()/sibling(); trailing semicolons removed
 *   Audited: 2026-03-04
 */
package sge
package files

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.io.Reader
import java.io.UnsupportedEncodingException
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode
import sge.utils.Nullable

/** Represents a file or directory on the filesystem, classpath, Android app storage, or Android assets directory. FileHandles are created via a {@link Files} instance.
  *
  * Because some of the file types are backed by composite files and may be compressed (for example, if they are in an Android .apk or are found via the classpath), the methods for extracting a
  * {@link #path()} or {@link #file()} may not be appropriate for all types. Use the Reader or Stream methods here to hide these dependencies from your platform independent code.
  *
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
class FileHandle(val internalFile: File, val fileType: FileType, private val externalStoragePath: Nullable[String] = Nullable.empty) {

  /** @return
    *   the path of the file as specified on construction, e.g. Gdx.files.internal("dir/file.png") -> dir/file.png. backward slashes will be replaced by forward slashes.
    */
  def path: String =
    internalFile.getPath().replace('\\', '/')

  /** @return the name of the file, without any parent paths. */
  def name: String =
    internalFile.getName()

  /** Returns the file extension (without the dot) or an empty string if the file name doesn't contain a dot. */
  def extension: String = {
    val name     = internalFile.getName()
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex == -1) ""
    else name.substring(dotIndex + 1)
  }

  /** @return the name of the file, without parent paths or the extension. */
  def nameWithoutExtension: String = {
    val name     = internalFile.getName()
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex == -1) name
    else name.substring(0, dotIndex)
  }

  /** @return
    *   the path and filename without the extension, e.g. dir/dir2/file.png -> dir/dir2/file. backward slashes will be returned as forward slashes.
    */
  def pathWithoutExtension: String = {
    val path     = internalFile.getPath().replace('\\', '/')
    val dotIndex = path.lastIndexOf('.')
    if (dotIndex == -1) path
    else path.substring(0, dotIndex)
  }

  /** Returns a java.io.File that represents this file handle. Note the returned file will only be usable for {@link FileType#Absolute} and {@link FileType#External} file handles.
    */
  def file: File =
    if (fileType == FileType.External) externalStoragePath.fold(internalFile)(path => new File(path, internalFile.getPath()))
    else internalFile

  /** Returns a stream for reading this file as bytes.
    * @throws [[sge.utils.SgeError]]
    *   if the file handle represents a directory, doesn't exist, or could not be read.
    */
  def read(): InputStream =
    if (
      fileType match {
        case FileType.Classpath                 => true
        case FileType.Internal | FileType.Local => !internalFile.exists()
        case _                                  => false
      }
    ) {
      utils.Nullable(getClass.getResourceAsStream("/" + internalFile.getPath().replace('\\', '/'))).getOrElse {
        throw utils.SgeError.FileReadError(this, "File not found")
      }
    } else {
      try
        new FileInputStream(file)
      catch {
        case ex: Exception =>
          if (file.isDirectory())
            throw utils.SgeError.FileReadError(this, "Cannot open a stream to a directory", Some(ex))
          throw utils.SgeError.FileReadError(this, "Error reading file", Some(ex))
      }
    }

  /** Returns a buffered stream for reading this file as bytes.
    * @throws [[sge.utils.SgeError]]
    *   if the file handle represents a directory, doesn't exist, or could not be read.
    */
  def read(bufferSize: Int): BufferedInputStream =
    new BufferedInputStream(read(), bufferSize)

  /** Returns a reader for reading this file as characters the platform's default charset.
    * @throws [[sge.utils.SgeError]]
    *   if the file handle represents a directory, doesn't exist, or could not be read.
    */
  def reader(): Reader =
    new InputStreamReader(read())

  /** Returns a reader for reading this file as characters.
    * @throws [[sge.utils.SgeError]]
    *   if the file handle represents a directory, doesn't exist, or could not be read.
    */
  def reader(charset: String): Reader = {
    val stream = read()
    try
      new InputStreamReader(stream, charset)
    catch {
      case ex: UnsupportedEncodingException =>
        utils.StreamUtils.closeQuietly(stream)
        throw utils.SgeError.FileReadError(this, "Error reading file", Some(ex))
    }
  }

  /** Returns a buffered reader for reading this file as characters using the platform's default charset.
    * @throws GdxRuntimeException
    *   if the file handle represents a directory, doesn't exist, or could not be read.
    */
  def reader(bufferSize: Int): BufferedReader =
    new BufferedReader(new InputStreamReader(read()), bufferSize)

  /** Returns a buffered reader for reading this file as characters.
    * @throws [[sge.utils.SgeError]]
    *   if the file handle represents a directory, doesn't exist, or could not be read.
    */
  def reader(bufferSize: Int, charset: String): BufferedReader = {
    val stream = read()
    try
      new BufferedReader(new InputStreamReader(stream, charset), bufferSize)
    catch {
      case ex: UnsupportedEncodingException =>
        utils.StreamUtils.closeQuietly(stream)
        throw utils.SgeError.FileReadError(this, "Error reading file", Some(ex))
    }
  }

  /** Reads the entire file into a string using the specified charset.
    * @param charset
    *   If null the default charset is used.
    * @throws GdxRuntimeException
    *   if the file handle represents a directory, doesn't exist, or could not be read.
    */
  def readString(charset: Nullable[String] = Nullable.empty): String = {
    val output = new StringBuilder(estimateLength)
    val reader = charset.fold(new InputStreamReader(read()))(cs => new InputStreamReader(read(), cs))
    try {
      val buffer = new Array[Char](256)
      var length = reader.read(buffer)
      while (length != -1) {
        output.appendAll(buffer, 0, length)
        length = reader.read(buffer)
      }
      output.toString()
    } catch {
      case ex: IOException =>
        throw utils.SgeError.FileReadError(this, "Error reading layout file", Some(ex))
    } finally
      utils.StreamUtils.closeQuietly(reader)
  }

  /** Reads the entire file into a byte array.
    * @throws GdxRuntimeException
    *   if the file handle represents a directory, doesn't exist, or could not be read.
    */
  def readBytes(): Array[Byte] = {
    val input = read()
    try
      utils.StreamUtils.copyStreamToByteArray(input, estimateLength)
    catch {
      case ex: IOException =>
        throw utils.SgeError.FileReadError(this, "Error reading file", Some(ex))
    } finally
      utils.StreamUtils.closeQuietly(input)
  }

  private def estimateLength: Int = {
    val length = this.length().toInt
    if (length != 0) length else 512
  }

  /** Reads the entire file into the byte array. The byte array must be big enough to hold the file's data.
    * @param bytes
    *   the array to load the file into
    * @param offset
    *   the offset to start writing bytes
    * @param size
    *   the number of bytes to read, see {@link #length()}
    * @return
    *   the number of read bytes
    */
  def readBytes(bytes: Array[Byte], offset: Int, size: Int): Int = {
    val input    = read()
    var position = 0
    try {
      var count = input.read(bytes, offset + position, size - position)
      while (count > 0) {
        position += count
        count = input.read(bytes, offset + position, size - position)
      }
      position - offset
    } catch {
      case ex: IOException =>
        throw utils.SgeError.FileReadError(this, "Error reading file", Some(ex))
    } finally
      utils.StreamUtils.closeQuietly(input)
  }

  /** Attempts to memory map this file. Android files must not be compressed.
    * @throws GdxRuntimeException
    *   if this file handle represents a directory, doesn't exist, or could not be read, or memory mapping fails, or is a {@link FileType#Classpath} file.
    */
  def map(mode: FileChannel.MapMode = MapMode.READ_ONLY): ByteBuffer = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileReadError(this, "Cannot map a classpath file")
    val f   = file
    val raf = new RandomAccessFile(f, if (mode == MapMode.READ_ONLY) "r" else "rw")
    try {
      val fileChannel  = raf.getChannel()
      val mappedBuffer = fileChannel.map(mode, 0, f.length())
      mappedBuffer.order(ByteOrder.nativeOrder())
      mappedBuffer
    } catch {
      case ex: Exception =>
        throw utils.SgeError.FileReadError(this, s"Error memory mapping file: $this ($fileType)", Some(ex))
    } finally
      utils.StreamUtils.closeQuietly(raf)
  }

  /** Returns a stream for writing to this file. Parent directories will be created if necessary.
    * @param append
    *   If false, this file will be overwritten if it exists, otherwise it will be appended.
    * @throws GdxRuntimeException
    *   if this file handle represents a directory, if it is a {@link FileType#Classpath} or {@link FileType#Internal} file, or if it could not be written.
    */
  def write(append: Boolean): OutputStream = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileReadError(this, "Cannot write to a classpath file")
    if (fileType == FileType.Internal) throw utils.SgeError.FileReadError(this, "Cannot write to an internal file")
    parent().mkdirs()
    try
      new FileOutputStream(file, append)
    catch {
      case ex: Exception =>
        if (file.isDirectory())
          throw utils.SgeError.FileReadError(this, s"Cannot open a stream to a directory: $internalFile ($fileType)", Some(ex))
        throw utils.SgeError.FileReadError(this, s"Error writing file: $internalFile ($fileType)", Some(ex))
    }
  }

  /** Returns a buffered stream for writing to this file. Parent directories will be created if necessary.
    * @param append
    *   If false, this file will be overwritten if it exists, otherwise it will be appended.
    * @param bufferSize
    *   The size of the buffer.
    * @throws GdxRuntimeException
    *   if this file handle represents a directory, if it is a {@link FileType#Classpath} or {@link FileType#Internal} file, or if it could not be written.
    */
  def write(append: Boolean, bufferSize: Int): OutputStream =
    new BufferedOutputStream(write(append), bufferSize)

  /** Reads the remaining bytes from the specified stream and writes them to this file. The stream is closed. Parent directories will be created if necessary.
    * @param append
    *   If false, this file will be overwritten if it exists, otherwise it will be appended.
    * @throws GdxRuntimeException
    *   if this file handle represents a directory, if it is a {@link FileType#Classpath} or {@link FileType#Internal} file, or if it could not be written.
    */
  def write(input: InputStream, append: Boolean): Unit = {
    val output = write(append)
    try
      utils.StreamUtils.copyStream(input, output)
    catch {
      case ex: Exception =>
        throw utils.SgeError.FileReadError(this, s"Error stream writing to file: $internalFile ($fileType)", Some(ex))
    } finally {
      utils.StreamUtils.closeQuietly(input)
      utils.StreamUtils.closeQuietly(output)
    }
  }

  /** Returns a writer for writing to this file. Parent directories will be created if necessary.
    * @param append
    *   If false, this file will be overwritten if it exists, otherwise it will be appended.
    * @param charset
    *   May be null to use the default charset.
    * @throws GdxRuntimeException
    *   if this file handle represents a directory, if it is a {@link FileType#Classpath} or {@link FileType#Internal} file, or if it could not be written.
    */
  def writer(append: Boolean, charset: Nullable[String] = Nullable.empty): Writer = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileReadError(this, "Cannot write to a classpath file")
    if (fileType == FileType.Internal) throw utils.SgeError.FileReadError(this, "Cannot write to an internal file")
    parent().mkdirs()
    try {
      val output = new FileOutputStream(file, append)
      charset.fold(new OutputStreamWriter(output): Writer)(cs => new OutputStreamWriter(output, cs))
    } catch {
      case ex: IOException =>
        if (file.isDirectory())
          throw utils.SgeError.FileReadError(this, s"Cannot open a stream to a directory: $internalFile ($fileType)", Some(ex))
        throw utils.SgeError.FileReadError(this, s"Error writing file: $internalFile ($fileType)", Some(ex))
    }
  }

  /** Writes the specified string to the file using the specified charset. Parent directories will be created if necessary.
    * @param append
    *   If false, this file will be overwritten if it exists, otherwise it will be appended.
    * @param charset
    *   May be null to use the default charset.
    * @throws GdxRuntimeException
    *   if this file handle represents a directory, if it is a {@link FileType#Classpath} or {@link FileType#Internal} file, or if it could not be written.
    */
  def writeString(string: String, append: Boolean, charset: Nullable[String] = Nullable.empty): Unit = {
    val writer = this.writer(append, charset)
    try
      writer.write(string)
    catch {
      case ex: Exception =>
        throw utils.SgeError.FileReadError(this, s"Error writing file: $internalFile ($fileType)", Some(ex))
    } finally
      utils.StreamUtils.closeQuietly(writer)
  }

  /** Writes the specified bytes to the file. Parent directories will be created if necessary.
    * @param append
    *   If false, this file will be overwritten if it exists, otherwise it will be appended.
    * @throws GdxRuntimeException
    *   if this file handle represents a directory, if it is a {@link FileType#Classpath} or {@link FileType#Internal} file, or if it could not be written.
    */
  def writeBytes(bytes: Array[Byte], append: Boolean): Unit = {
    val output = write(append)
    try
      output.write(bytes)
    catch {
      case ex: IOException =>
        throw utils.SgeError.FileReadError(this, s"Error writing file: $internalFile ($fileType)", Some(ex))
    } finally
      utils.StreamUtils.closeQuietly(output)
  }

  /** Writes the specified bytes to the file. Parent directories will be created if necessary.
    * @param append
    *   If false, this file will be overwritten if it exists, otherwise it will be appended.
    * @throws GdxRuntimeException
    *   if this file handle represents a directory, if it is a {@link FileType#Classpath} or {@link FileType#Internal} file, or if it could not be written.
    */
  def writeBytes(bytes: Array[Byte], offset: Int, length: Int, append: Boolean): Unit = {
    val output = write(append)
    try
      output.write(bytes, offset, length)
    catch {
      case ex: IOException =>
        throw utils.SgeError.FileReadError(this, s"Error writing file: $internalFile ($fileType)", Some(ex))
    } finally
      utils.StreamUtils.closeQuietly(output)
  }

  /** Returns the paths to the children of this directory. Returns an empty list if this file handle represents a file and not a directory. On the desktop, an {@link FileType#Internal} handle to a
    * directory on the classpath will return a zero length array.
    * @throws [[sge.utils.SgeError]]
    *   if this file is an {@link FileType#Classpath} file.
    */
  def list(): Array[FileHandle] = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileReadError(this, "Cannot list a classpath directory")
    Nullable(file.list()).map(_.map(child)).getOrElse(Array.empty[FileHandle])
  }

  /** Returns the paths to the children of this directory that satisfy the specified filter. Returns an empty list if this file handle represents a file and not a directory. On the desktop, an
    * {@link FileType#Internal} handle to a directory on the classpath will return a zero length array.
    * @param filter
    *   the {@link FileFilter} to filter files
    * @throws [[sge.utils.SgeError]]
    *   if this file is an {@link FileType#Classpath} file.
    */
  def list(filter: FileFilter): Array[FileHandle] = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileReadError(this, "Cannot list a classpath directory")
    Nullable(file.list())
      .map { relativePaths =>
        relativePaths.map(path => child(path)).filter(childHandle => filter.accept(childHandle.file))
      }
      .getOrElse(Array.empty[FileHandle])
  }

  /** Returns the paths to the children of this directory that satisfy the specified filter. Returns an empty list if this file handle represents a file and not a directory. On the desktop, an
    * {@link FileType#Internal} handle to a directory on the classpath will return a zero length array.
    * @param filter
    *   the {@link FilenameFilter} to filter files
    * @throws [[sge.utils.SgeError]]
    *   if this file is an {@link FileType#Classpath} file.
    */
  def list(filter: FilenameFilter): Array[FileHandle] = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileReadError(this, "Cannot list a classpath directory")
    val fileObj = file
    Nullable(fileObj.list())
      .map { relativePaths =>
        relativePaths.filter(path => filter.accept(fileObj, path)).map(path => child(path))
      }
      .getOrElse(Array.empty[FileHandle])
  }

  /** Returns the paths to the children of this directory with the specified suffix. Returns an empty list if this file handle represents a file and not a directory. On the desktop, an
    * {@link FileType#Internal} handle to a directory on the classpath will return a zero length array.
    * @throws [[sge.utils.SgeError]]
    *   if this file is an {@link FileType#Classpath} file.
    */
  def list(suffix: String): Array[FileHandle] = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileReadError(this, "Cannot list a classpath directory")
    Nullable(file.list())
      .map { relativePaths =>
        relativePaths.filter(_.endsWith(suffix)).map(child)
      }
      .getOrElse(Array.empty[FileHandle])
  }

  /** Returns true if this file is a directory. Always returns false for classpath files. On Android, an {@link FileType#Internal} handle to an empty directory will return false. On the desktop, an
    * {@link FileType#Internal} handle to a directory on the classpath will return false.
    */
  def isDirectory(): Boolean =
    if (fileType == FileType.Classpath) false
    else file.isDirectory()

  /** Returns a handle to the child with the specified name. */
  def child(name: String): FileHandle =
    if (internalFile.getPath().length() == 0) FileHandle(new File(name), fileType, externalStoragePath)
    else FileHandle(new File(internalFile, name), fileType, externalStoragePath)

  /** Returns a handle to the sibling with the specified name.
    * @throws [[sge.utils.SgeError]]
    *   if this file is the root.
    */
  def sibling(name: String): FileHandle = {
    if (internalFile.getPath().length() == 0) throw utils.SgeError.FileReadError(this, "Cannot get the sibling of the root")
    FileHandle(new File(internalFile.getParent(), name), fileType, externalStoragePath)
  }

  def parent(): FileHandle =
    Nullable(internalFile.getParentFile()).fold {
      if (fileType == FileType.Absolute) FileHandle(new File("/"), fileType, externalStoragePath)
      else FileHandle(new File(""), fileType, externalStoragePath)
    } { parent =>
      FileHandle(parent, fileType, externalStoragePath)
    }

  /** @throws [[sge.utils.SgeError]] if this file handle is a {@link FileType#Classpath} or {@link FileType#Internal} file. */
  def mkdirs(): Unit = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileWriteError(this, "Cannot mkdirs with a classpath file")
    if (fileType == FileType.Internal) throw utils.SgeError.FileWriteError(this, "Cannot mkdirs with an internal file")
    file.mkdirs()
  }

  /** Returns true if the file exists. On Android, a {@link FileType#Classpath} or {@link FileType#Internal} handle to a directory will always return false. Note that this can be very slow for
    * internal files on Android!
    */
  def exists(): Boolean =
    fileType match {
      case FileType.Internal =>
        if (file.exists()) true
        else {
          // Use getResourceAsStream instead of getResource to avoid java.net.URL (not available on Scala Native)
          val stream = classOf[FileHandle].getResourceAsStream("/" + internalFile.getPath().replace('\\', '/'))
          val found  = stream != null // scalastyle:ignore null
          if (found) stream.close()
          found
        }
      case FileType.Classpath =>
        val stream = classOf[FileHandle].getResourceAsStream("/" + internalFile.getPath().replace('\\', '/'))
        val found  = stream != null // scalastyle:ignore null
        if (found) stream.close()
        found
      case _ =>
        file.exists()
    }

  /** Deletes this file or empty directory and returns success. Will not delete a directory that has children.
    * @throws GdxRuntimeException
    *   if this file handle is a {@link FileType#Classpath} or {@link FileType#Internal} file.
    */
  def delete(): Boolean = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileWriteError(this, "Cannot delete a classpath file")
    if (fileType == FileType.Internal) throw utils.SgeError.FileWriteError(this, "Cannot delete an internal file")
    file.delete()
  }

  /** Deletes this file or directory and all children, recursively.
    * @throws GdxRuntimeException
    *   if this file handle is a {@link FileType#Classpath} or {@link FileType#Internal} file.
    */
  def deleteDirectory(): Boolean = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileWriteError(this, "Cannot delete a classpath file")
    if (fileType == FileType.Internal) throw utils.SgeError.FileWriteError(this, "Cannot delete an internal file")
    deleteDirectory(file)
  }

  /** Deletes all children of this directory, recursively. Optionally preserving the folder structure.
    * @throws GdxRuntimeException
    *   if this file handle is a {@link FileType#Classpath} or {@link FileType#Internal} file.
    */
  def emptyDirectory(preserveTree: Boolean = false): Unit = {
    if (fileType == FileType.Classpath) throw utils.SgeError.FileWriteError(this, "Cannot delete a classpath file")
    if (fileType == FileType.Internal) throw utils.SgeError.FileWriteError(this, "Cannot delete an internal file")
    emptyDirectory(file, preserveTree)
  }

  /** Copies this file or directory to the specified file or directory. If this handle is a file, then 1) if the destination is a file, it is overwritten, or 2) if the destination is a directory, this
    * file is copied into it, or 3) if the destination doesn't exist, {@link #mkdirs()} is called on the destination's parent and this file is copied into it with a new name. If this handle is a
    * directory, then 1) if the destination is a file, SgeError is thrown, or 2) if the destination is a directory, this directory is copied into it recursively, overwriting existing files, or 3) if
    * the destination doesn't exist, {@link #mkdirs()} is called on the destination and this directory is copied into it recursively.
    * @throws GdxRuntimeException
    *   if the destination file handle is a {@link FileType#Classpath} or {@link FileType#Internal} file, or copying failed.
    */
  def copyTo(dest: FileHandle): Unit =
    if (!isDirectory()) {
      val actualDest = if (dest.isDirectory()) dest.child(name) else dest
      copyFile(this, actualDest)
    } else {
      if (dest.exists()) {
        if (!dest.isDirectory()) throw utils.SgeError.FileWriteError(dest, "Destination exists but is not a directory")
      } else {
        dest.mkdirs()
        if (!dest.isDirectory()) throw utils.SgeError.FileWriteError(dest, "Destination directory cannot be created")
      }
      copyDirectory(this, dest.child(name))
    }

  /** Moves this file to the specified file, overwriting the file if it already exists.
    * @throws GdxRuntimeException
    *   if the source or destination file handle is a {@link FileType#Classpath} or {@link FileType#Internal} file.
    */
  def moveTo(dest: FileHandle): Unit =
    fileType match {
      case FileType.Classpath =>
        throw utils.SgeError.FileWriteError(this, "Cannot move a classpath file")
      case FileType.Internal =>
        throw utils.SgeError.FileWriteError(this, "Cannot move an internal file")
      case FileType.Absolute | FileType.External if file.renameTo(dest.file) =>
      // Try rename for efficiency and to change case on case-insensitive file systems.
      case _ =>
        copyTo(dest)
        delete()
        if (exists() && isDirectory()) deleteDirectory()
    }

  /** Returns the length in bytes of this file, or 0 if this file is a directory, does not exist, or the size cannot otherwise be determined.
    */
  def length(): Long =
    if (fileType == FileType.Classpath || (fileType == FileType.Internal && !file.exists())) {
      val input = read()
      try
        input.available()
      catch {
        case _: Exception => 0
      } finally
        utils.StreamUtils.closeQuietly(input)
    } else {
      file.length()
    }

  /** Returns the last modified time in milliseconds for this file. Zero is returned if the file doesn't exist. Zero is returned for {@link FileType#Classpath} files. On Android, zero is returned for
    * {@link FileType#Internal} files. On the desktop, zero is returned for {@link FileType#Internal} files on the classpath.
    */
  def lastModified(): Long =
    file.lastModified()

  override def equals(obj: Any): Boolean =
    if (!obj.isInstanceOf[FileHandle]) false
    else {
      val other = obj.asInstanceOf[FileHandle]
      fileType == other.fileType && path == other.path
    }

  override def hashCode(): Int = {
    var hash = 1
    hash = hash * 37 + fileType.hashCode()
    hash = hash * 67 + path.hashCode()
    hash
  }

  override def toString(): String =
    internalFile.getPath().replace('\\', '/')

  // Utility methods for file operations
  private def copyFile(source: FileHandle, dest: FileHandle): Unit = {
    val input = source.read()
    try
      dest.write(input, false)
    finally
      utils.StreamUtils.closeQuietly(input)
  }

  private def copyDirectory(source: FileHandle, dest: FileHandle): Unit = {
    dest.mkdirs()
    for (child <- source.list())
      if (child.isDirectory()) {
        copyDirectory(child, dest.child(child.name))
      } else {
        copyFile(child, dest.child(child.name))
      }
  }

  private def deleteDirectory(file: File): Boolean = {
    emptyDirectory(file, false)
    file.delete()
  }

  private def emptyDirectory(file: File, preserveTree: Boolean): Unit =
    if (file.isDirectory()) {
      Nullable(file.listFiles()).foreach { children =>
        for (child <- children)
          if (child.isDirectory()) {
            if (preserveTree) {
              emptyDirectory(child, preserveTree)
            } else {
              deleteDirectory(child)
            }
          } else {
            child.delete()
          }
      }
    }
}

/** A FileHandle intended to be subclassed for the purpose of implementing {@link #read()} and/or {@link #write(boolean)} . Methods that would manipulate the file instead throw
  * UnsupportedOperationException.
  * @author
  *   Nathan Sweet (original implementation)
  */
abstract class FileHandleStream(path: String) extends FileHandle(new File(path), FileType.Absolute) {

  override def isDirectory(): Boolean = false

  override def length(): Long = 0

  override def exists(): Boolean = true

  override def child(name: String): FileHandle =
    throw new UnsupportedOperationException()

  override def sibling(name: String): FileHandle =
    throw new UnsupportedOperationException()

  override def parent(): FileHandle =
    throw new UnsupportedOperationException()

  override def read(): InputStream =
    throw new UnsupportedOperationException()

  override def write(append: Boolean): OutputStream =
    throw new UnsupportedOperationException()

  override def list(): Array[FileHandle] =
    throw new UnsupportedOperationException()

  override def mkdirs(): Unit =
    throw new UnsupportedOperationException()

  override def delete(): Boolean =
    throw new UnsupportedOperationException()

  override def deleteDirectory(): Boolean =
    throw new UnsupportedOperationException()

  override def copyTo(dest: FileHandle): Unit =
    throw new UnsupportedOperationException()

  override def moveTo(dest: FileHandle): Unit =
    throw new UnsupportedOperationException()

  override def emptyDirectory(preserveTree: Boolean = false): Unit =
    throw new UnsupportedOperationException()
}
