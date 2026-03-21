// SGE — Android files operations implementation
//
// Uses android.content.res.AssetManager for internal (asset) files
// and ContextWrapper for storage paths.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.DefaultAndroidFiles
//   Renames: DefaultAndroidFiles → AndroidFilesOpsImpl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.content.ContextWrapper
import _root_.android.content.res.AssetManager

class AndroidFilesOpsImpl(assets: AssetManager, contextWrapper: ContextWrapper, useExternalFiles: Boolean) extends FilesOps {

  val localStoragePath: String = {
    val p = contextWrapper.getFilesDir.getAbsolutePath
    if (p.endsWith("/")) p else p + "/"
  }

  val externalStoragePath: String | Null =
    if (useExternalFiles) {
      val dir = contextWrapper.getExternalFilesDir(null)
      if (dir != null) {
        val p = dir.getAbsolutePath
        if (p.endsWith("/")) p else p + "/"
      } else null
    } else null

  override def openInternal(path: String): java.io.InputStream =
    assets.open(path)

  override def listInternal(path: String): Array[String] =
    assets.list(path) match {
      case null  => Array.empty
      case names => names
    }

  override def openInternalFd(path: String): (java.io.FileDescriptor, Long, Long) | Null =
    try {
      val afd    = assets.openFd(path)
      val result = (afd.getFileDescriptor, afd.getStartOffset, afd.getDeclaredLength)
      afd.close()
      result
    } catch {
      case _: java.io.IOException => null
    }

  override def internalFileLength(path: String): Long = {
    var fd: _root_.android.content.res.AssetFileDescriptor | Null = null
    try {
      fd = assets.openFd(path)
      fd.nn.getLength
    } catch {
      case _: java.io.IOException => -1L
    } finally
      if (fd != null) {
        try fd.nn.close()
        catch { case _: java.io.IOException => () }
      }
  }
}
