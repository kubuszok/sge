package sge
package assets
package loaders
package resolvers

import sge.files.{ FileHandle, FileType }

/** This {@link FileHandleResolver} uses a given list of {@link Resolution} s to determine the best match based on the current back buffer size. An example of how this resolver works:
  *
  * <p> Let's assume that we have only a single {@link Resolution} added to this resolver. This resolution has the following properties: </p>
  *
  * <ul> <li>{@code portraitWidth = 1920}</li> <li>{@code portraitHeight = 1080}</li> <li>{@code folder = "1920x1080"}</li> </ul>
  *
  * <p> One would now supply a file to be found to the resolver. For this example, we assume it is "{@code textures/walls/brick.png}". Since there is only a single {@link Resolution} , this will be
  * the best match for any screen size. The resolver will now try to find the file in the following ways: </p>
  *
  * <ul> <li>{@code "textures/walls/1920x1080/brick.png"}</li> <li>{@code "textures/walls/brick.png"}</li> </ul>
  *
  * <p> The files are ultimately resolved via the given {{@link #baseResolver}. In case the first version cannot be resolved, the fallback will try to search for the file without the resolution
  * folder. </p>
  */
class ResolutionFileResolver(protected val baseResolver: FileHandleResolver, protected val descriptors: Array[ResolutionFileResolver.Resolution])(using sge: Sge) extends FileHandleResolver {

  /** Creates a {@code ResolutionFileResolver} based on a given {@link FileHandleResolver} and a list of {@link Resolution} s.
    * @param baseResolver
    *   The {@link FileHandleResolver} that will ultimately used to resolve the file.
    * @param descriptors
    *   A list of {@link Resolution} s. At least one has to be supplied.
    */
  def this(baseResolver: FileHandleResolver, descriptors: ResolutionFileResolver.Resolution*)(using sge: Sge) = {
    this(baseResolver, descriptors.toArray)
    if (descriptors.isEmpty) throw new IllegalArgumentException("At least one Resolution needs to be supplied.")
  }

  override def resolve(fileName: String): FileHandle = {
    val bestResolution = ResolutionFileResolver.choose(descriptors*)
    val originalHandle = new FileHandle(new java.io.File(fileName), FileType.Internal)
    val handle         = baseResolver.resolve(resolve(originalHandle, bestResolution.folder))
    if (!handle.exists()) baseResolver.resolve(fileName) else handle
  }

  protected def resolve(originalHandle: FileHandle, suffix: String): String = {
    val parentString = originalHandle.parent() match {
      case parent if parent != null && !parent.name().equals("") => parent.path() + "/"
      case _                                                     => ""
    }
    parentString + suffix + "/" + originalHandle.name()
  }
}

object ResolutionFileResolver {
  case class Resolution(portraitWidth: Int, portraitHeight: Int, folder: String)

  def choose(descriptors: Resolution*)(using sge: Sge): Resolution =
    // For now, return the first resolution until we find the proper Graphics method
    // TODO: Replace with proper screen size detection once Graphics interface is available
    descriptors(0)
}
