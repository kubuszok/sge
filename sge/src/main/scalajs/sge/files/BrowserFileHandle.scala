/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtFileHandle.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtFileHandle -> BrowserFileHandle
 *   Convention: Scala.js only; reads from BrowserAssetLoader's in-memory cache
 *   Convention: Write operations throw SgeError (browser has no writable filesystem)
 *   Idiom: Overrides all I/O methods from FileHandle base class
 *   Idiom: No java.io.File dependency — passes null to parent constructor and overrides
 *     all methods that would touch the file field (path, name, extension, etc.)
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package files

import java.io.{ BufferedInputStream, ByteArrayInputStream, File, InputStream, Reader }
import java.io.InputStreamReader
import sge.utils.Nullable

/** Browser implementation of [[FileHandle]]. Reads from the [[BrowserAssetLoader]]'s in-memory cache. Write operations are not supported and will throw.
  *
  * @param assetLoader
  *   the preloaded asset loader
  * @param filePath
  *   the asset path (forward-slash separated)
  * @param fileType
  *   the file type (only Internal and Classpath are supported)
  */
class BrowserFileHandle(
  private val assetLoader: BrowserAssetLoader,
  filePath:                String,
  fileType:                FileType
) extends FileHandle(BrowserFileHandle.noFile, fileType) {

  // ─── Path/name methods (override to avoid touching null file field) ──

  override def path: String =
    filePath.replace('\\', '/')

  override def name: String = {
    val p   = path
    val idx = p.lastIndexOf('/')
    if (idx < 0) p else p.substring(idx + 1)
  }

  override def extension: String = {
    val n        = name
    val dotIndex = n.lastIndexOf('.')
    if (dotIndex == -1) ""
    else n.substring(dotIndex + 1)
  }

  override def nameWithoutExtension: String = {
    val n        = name
    val dotIndex = n.lastIndexOf('.')
    if (dotIndex == -1) n
    else n.substring(0, dotIndex)
  }

  override def pathWithoutExtension: String = {
    val p        = path
    val dotIndex = p.lastIndexOf('.')
    if (dotIndex == -1) p
    else p.substring(0, dotIndex)
  }

  override def file: File =
    throw new UnsupportedOperationException("BrowserFileHandle does not support java.io.File")

  // ─── Read methods ─────────────────────────────────────────────────────

  override def read(): InputStream = {
    val p = path
    if (assetLoader.isText(p)) {
      new ByteArrayInputStream(assetLoader.readText(p).getBytes("UTF-8"))
    } else if (assetLoader.isBinary(p)) {
      new ByteArrayInputStream(assetLoader.readBytes(p))
    } else {
      throw utils.SgeError.FileReadError(this, s"Asset not preloaded: $p")
    }
  }

  override def read(bufferSize: Int): BufferedInputStream =
    new BufferedInputStream(read(), bufferSize)

  override def reader(): Reader =
    new InputStreamReader(read())

  override def reader(charset: String): Reader =
    new InputStreamReader(read(), charset)

  override def readString(charset: Nullable[String] = Nullable.empty): String = {
    val p = path
    if (assetLoader.isText(p)) {
      assetLoader.readText(p)
    } else if (assetLoader.isBinary(p)) {
      charset.fold(new String(assetLoader.readBytes(p)))(cs => new String(assetLoader.readBytes(p), cs))
    } else {
      throw utils.SgeError.FileReadError(this, s"Asset not preloaded: $p")
    }
  }

  override def readBytes(): Array[Byte] = {
    val p = path
    if (assetLoader.isBinary(p)) {
      assetLoader.readBytes(p)
    } else if (assetLoader.isText(p)) {
      assetLoader.readText(p).getBytes("UTF-8")
    } else {
      throw utils.SgeError.FileReadError(this, s"Asset not preloaded: $p")
    }
  }

  // ─── Directory/listing ────────────────────────────────────────────────

  override def exists(): Boolean =
    assetLoader.contains(path)

  override def isDirectory(): Boolean =
    assetLoader.isDirectory(path)

  override def length(): Long =
    assetLoader.length(path)

  override def list(): Array[FileHandle] =
    assetLoader.list(path).map(p => BrowserFileHandle(assetLoader, p, fileType))

  override def list(suffix: String): Array[FileHandle] =
    assetLoader.list(path).filter(_.endsWith(suffix)).map(p => BrowserFileHandle(assetLoader, p, fileType))

  // ─── Navigation ───────────────────────────────────────────────────────

  override def child(name: String): FileHandle = {
    val p         = path
    val childPath = if (p.isEmpty) name else if (p.endsWith("/")) p + name else p + "/" + name
    BrowserFileHandle(assetLoader, childPath, fileType)
  }

  override def sibling(name: String): FileHandle = {
    val p = path
    if (p.isEmpty) throw utils.SgeError.FileReadError(this, "Cannot get the sibling of the root")
    val parentPath = {
      val idx = p.lastIndexOf('/')
      if (idx < 0) "" else p.substring(0, idx)
    }
    val siblingPath = if (parentPath.isEmpty) name else parentPath + "/" + name
    BrowserFileHandle(assetLoader, siblingPath, fileType)
  }

  override def parent(): FileHandle = {
    val p          = path
    val idx        = p.lastIndexOf('/')
    val parentPath = if (idx < 0) "" else p.substring(0, idx)
    BrowserFileHandle(assetLoader, parentPath, fileType)
  }

  override def toString: String = path
}

private object BrowserFileHandle {

  // java.io.File is not available in Scala.js — pass null since all File-dependent methods are overridden below
  val noFile: File = null.asInstanceOf[File] // scalastyle:ignore null
}
