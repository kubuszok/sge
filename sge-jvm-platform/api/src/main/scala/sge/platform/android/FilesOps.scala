// SGE — Android file operations interface
//
// Self-contained (JDK types only). Provides access to Android's asset
// system and storage paths. Implemented in sge-jvm-platform-android
// using android.content.res.AssetManager and ContextWrapper.

package sge
package platform
package android

/** File operations for Android. Uses only JDK types.
  *
  * Abstracts Android's AssetManager and storage paths so that sge core can create FileHandle instances without depending on android.* classes.
  */
trait FilesOps {

  // ── Internal (asset) file operations ──────────────────────────────────

  /** Opens an internal (asset) file for reading.
    * @throws java.io.IOException
    *   if the file does not exist
    */
  def openInternal(path: String): java.io.InputStream

  /** Lists files in an internal (asset) directory.
    * @return
    *   array of relative file names, empty if the path is not a directory
    */
  def listInternal(path: String): Array[String]

  /** Opens an internal file descriptor for memory mapping.
    * @return
    *   (FileDescriptor, startOffset, declaredLength), or null if not available
    */
  def openInternalFd(path: String): (java.io.FileDescriptor, Long, Long) | Null

  /** Returns the length of an internal (asset) file, or -1 if unknown. */
  def internalFileLength(path: String): Long

  // ── Storage paths ─────────────────────────────────────────────────────

  /** Returns the absolute path to the app's local (internal) storage directory. Always ends with '/'. */
  def localStoragePath: String

  /** Returns the absolute path to the app's external storage directory, or null if unavailable. Ends with '/' if non-null. */
  def externalStoragePath: String | Null
}
