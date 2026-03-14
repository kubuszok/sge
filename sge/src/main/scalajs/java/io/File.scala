// Minimal java.io.File stub for Scala.js.
// The Scala.js linker needs this class to exist because FileHandle declares
// `val file: File`, even though BrowserFileHandle overrides all methods that
// would touch the file field. No File methods are actually called at runtime.
package java.io

class File(pathname: String) {

  def this(parent: String, child: String) = {
    this(if (parent == null) child else parent + "/" + child) // scalastyle:ignore null
  }

  def this(parent: File, child: String) = {
    this(if (parent == null) child else parent.getPath() + "/" + child) // scalastyle:ignore null
  }

  def getPath(): String = pathname

  def getName(): String = {
    val sep = math.max(pathname.lastIndexOf('/'), pathname.lastIndexOf('\\'))
    if (sep >= 0) pathname.substring(sep + 1) else pathname
  }

  def getAbsolutePath(): String = pathname

  override def toString: String = pathname
}
