/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-headless/.../HeadlessFiles.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: HeadlessFiles -> DesktopFiles (reused by desktop backend)
 *   Convention: static paths -> vals on companion object; all methods delegate to DesktopFileHandle
 *   Idiom: split packages
 *   Audited: 2026-03-05
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package files

import java.io.File

/** A [[sge.Files]] implementation backed by `java.io.File` for desktop and headless environments.
  *
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
final class DesktopFiles extends sge.Files {

  override def getFileHandle(path: String, fileType: FileType): FileHandle =
    DesktopFileHandle(path, fileType, DesktopFiles.externalPath)

  override def classpath(path: String): FileHandle =
    DesktopFileHandle(path, FileType.Classpath, DesktopFiles.externalPath)

  override def internal(path: String): FileHandle =
    DesktopFileHandle(path, FileType.Internal, DesktopFiles.externalPath)

  override def external(path: String): FileHandle =
    DesktopFileHandle(path, FileType.External, DesktopFiles.externalPath)

  override def absolute(path: String): FileHandle =
    DesktopFileHandle(path, FileType.Absolute, DesktopFiles.externalPath)

  override def local(path: String): FileHandle =
    DesktopFileHandle(path, FileType.Local, DesktopFiles.externalPath)

  override def getExternalStoragePath: String = DesktopFiles.externalPath

  override def isExternalStorageAvailable: Boolean = true

  override def getLocalStoragePath: String = DesktopFileHandle.localPath

  override def isLocalStorageAvailable: Boolean = true
}

object DesktopFiles {
  val externalPath: String = System.getProperty("user.home") + File.separator
}
