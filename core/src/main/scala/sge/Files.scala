package sge

/** Provides standard access to the filesystem, classpath, Android app storage (internal and external), and Android assets directory.
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
trait Files {

  /** Returns a handle representing a file or directory.
    * @param type
    *   Determines how the path is resolved.
    * @throws GdxRuntimeException
    *   if the type is classpath or internal and the file does not exist.
    * @see
    *   FileType
    */
  def getFileHandle(path: String, fileType: files.FileType): files.FileHandle

  /** Convenience method that returns a {@link FileType#Classpath} file handle. */
  def classpath(path: String): files.FileHandle

  /** Convenience method that returns a {@link FileType#Internal} file handle. */
  def internal(path: String): files.FileHandle

  /** Convenience method that returns a {@link FileType#External} file handle. */
  def external(path: String): files.FileHandle

  /** Convenience method that returns a {@link FileType#Absolute} file handle. */
  def absolute(path: String): files.FileHandle

  /** Convenience method that returns a {@link FileType#Local} file handle. */
  def local(path: String): files.FileHandle

  /** Returns the external storage path directory. This is the app external storage on Android and the home directory of the current user on the desktop.
    */
  def getExternalStoragePath: String

  /** Returns true if the external storage is ready for file IO. */
  def isExternalStorageAvailable: Boolean

  /** Returns the local storage path directory. This is the private files directory on Android and the directory of the jar on the desktop.
    */
  def getLocalStoragePath: String

  /** Returns true if the local storage is ready for file IO. */
  def isLocalStorageAvailable: Boolean
}
