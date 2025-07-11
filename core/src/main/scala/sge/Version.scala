package sge

import sge.utils.SgeError

/** The version of libGDX
  *
  * @author
  *   mzechner (original implementation)
  */
object Version {

  /** The current version of libGDX as a String in the major.minor.revision format * */
  final val VERSION: String = "1.13.5"

  /** The current major version of libGDX * */
  lazy val MAJOR: Int =
    try {
      val v = VERSION.split("\\.")
      if (v.length < 1) 0 else Integer.valueOf(v(0))
    } catch {
      case t: Throwable =>
        throw SgeError.MathError(s"Invalid version $VERSION", Some(t))
    }

  /** The current minor version of libGDX * */
  lazy val MINOR: Int =
    try {
      val v = VERSION.split("\\.")
      if (v.length < 2) 0 else Integer.valueOf(v(1))
    } catch {
      case t: Throwable =>
        throw SgeError.MathError(s"Invalid version $VERSION", Some(t))
    }

  /** The current revision version of libGDX * */
  lazy val REVISION: Int =
    try {
      val v = VERSION.split("\\.")
      if (v.length < 3) 0 else Integer.valueOf(v(2))
    } catch {
      case t: Throwable =>
        throw SgeError.MathError(s"Invalid version $VERSION", Some(t))
    }

  def isHigher(major: Int, minor: Int, revision: Int): Boolean =
    isHigherEqual(major, minor, revision + 1)

  def isHigherEqual(major: Int, minor: Int, revision: Int): Boolean =
    if (MAJOR != major) MAJOR > major
    else if (MINOR != minor) MINOR > minor
    else REVISION >= revision

  def isLower(major: Int, minor: Int, revision: Int): Boolean =
    isLowerEqual(major, minor, revision - 1)

  def isLowerEqual(major: Int, minor: Int, revision: Int): Boolean =
    if (MAJOR != major) MAJOR < major
    else if (MINOR != minor) MINOR < minor
    else REVISION <= revision
}
