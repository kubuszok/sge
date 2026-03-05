/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtFiles.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtFiles -> BrowserFiles
 *   Convention: Scala.js only; uses BrowserAssetLoader instead of GWT Preloader
 *   Convention: Only Internal FileType supported (browser has no filesystem)
 *   Idiom: GdxRuntimeException -> SgeError.InvalidInput
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package files

/** Browser implementation of [[Files]]. Only [[FileType.Internal]] is supported.
  *
  * Assets are served from the [[BrowserAssetLoader]]'s in-memory cache, which must be populated (via [[BrowserAssetLoader.preload]]) before use.
  *
  * @param assetLoader
  *   the preloaded asset loader
  */
class BrowserFiles(val assetLoader: BrowserAssetLoader) extends sge.Files {

  override def getFileHandle(path: String, fileType: FileType): FileHandle =
    fileType match {
      case FileType.Internal =>
        BrowserFileHandle(assetLoader, path, FileType.Internal)
      case FileType.Classpath =>
        BrowserFileHandle(assetLoader, path, FileType.Classpath)
      case other =>
        throw utils.SgeError.InvalidInput(s"FileType $other is not supported in browser")
    }

  override def classpath(path: String): FileHandle =
    BrowserFileHandle(assetLoader, path, FileType.Classpath)

  override def internal(path: String): FileHandle =
    BrowserFileHandle(assetLoader, path, FileType.Internal)

  override def external(path: String): FileHandle =
    throw utils.SgeError.InvalidInput("External files are not supported in browser")

  override def absolute(path: String): FileHandle =
    throw utils.SgeError.InvalidInput("Absolute files are not supported in browser")

  override def local(path: String): FileHandle =
    throw utils.SgeError.InvalidInput("Local files are not supported in browser")

  override def getExternalStoragePath: String = ""

  override def isExternalStorageAvailable: Boolean = false

  override def getLocalStoragePath: String = ""

  override def isLocalStorageAvailable: Boolean = false
}
