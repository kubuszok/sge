// Minimal java.io.File stub for Scala.js.
// The Scala.js linker needs this class to exist because FileHandle declares
// `val internalFile: File`, even though BrowserFileHandle overrides all methods that
// would touch the file field. No File methods are actually called at runtime.
package java.io

class File(pathname: String) {

  def this(parent: String, child: String) =
    this(if (parent == null) child else parent + "/" + child) // scalastyle:ignore null

  def this(parent: File, child: String) =
    this(if (parent == null) child else parent.getPath() + "/" + child) // scalastyle:ignore null

  def getPath(): String = pathname

  def getName(): String = {
    val sep = math.max(pathname.lastIndexOf('/'), pathname.lastIndexOf('\\'))
    if (sep >= 0) pathname.substring(sep + 1) else pathname
  }

  def getAbsolutePath(): String = pathname

  def getParent(): String =
    throw new UnsupportedOperationException("File.getParent is not supported on Scala.js")

  def getParentFile(): File =
    throw new UnsupportedOperationException("File.getParentFile is not supported on Scala.js")

  def exists(): Boolean =
    throw new UnsupportedOperationException("File.exists is not supported on Scala.js")

  def isDirectory(): Boolean =
    throw new UnsupportedOperationException("File.isDirectory is not supported on Scala.js")

  def list(): Array[String] =
    throw new UnsupportedOperationException("File.list is not supported on Scala.js")

  def listFiles(): Array[File] =
    throw new UnsupportedOperationException("File.listFiles is not supported on Scala.js")

  def mkdirs(): Boolean =
    throw new UnsupportedOperationException("File.mkdirs is not supported on Scala.js")

  def delete(): Boolean =
    throw new UnsupportedOperationException("File.delete is not supported on Scala.js")

  def renameTo(dest: File): Boolean =
    throw new UnsupportedOperationException("File.renameTo is not supported on Scala.js")

  def length(): Long =
    throw new UnsupportedOperationException("File.length is not supported on Scala.js")

  def lastModified(): Long =
    throw new UnsupportedOperationException("File.lastModified is not supported on Scala.js")

  override def toString: String = pathname
}
