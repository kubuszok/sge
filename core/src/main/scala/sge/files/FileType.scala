package sge
package files

/** Indicates how to resolve a path to a file.
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
enum FileType {

  /** Path relative to the root of the classpath. Classpath files are always readonly. Note that classpath files are not compatible with some functionality on Android, such as
    * {@link Audio#newSound(FileHandle)} and {@link Audio#newMusic(FileHandle)} .
    */
  case Classpath

  /** Path relative to the asset directory on Android and to the application's root directory on the desktop. On the desktop, if the file is not found, then the classpath is checked. This enables
    * files to be found when using JWS or applets. Internal files are always readonly.
    */
  case Internal

  /** Path relative to the root of the app external storage on Android and to the home directory of the current user on the desktop.
    */
  case External

  /** Path that is a fully qualified, absolute filesystem path. To ensure portability across platforms use absolute files only when absolutely (heh) necessary.
    */
  case Absolute

  /** Path relative to the private files directory on Android and to the application's root directory on the desktop. */
  case Local
}
