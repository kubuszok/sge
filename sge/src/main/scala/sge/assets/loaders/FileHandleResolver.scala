/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/FileHandleResolver.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait
 *   Convention: resolver subpackage classes merged into companion object
 *   Idiom: split packages
 *   Fixes: ForResolution.resolve used FileType.Internal instead of Absolute; validation moved to primary constructor
 *   TODOs: test: ForResolution.choose() picks best resolution for screen size; Prefix.resolve prepends prefix
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.{ FileHandle, FileType }
import sge.utils.Nullable

/** Interface for classes the can map a file name to a {@link FileHandle} . Used to allow the {@link AssetManager} to load resources from anywhere or implement caching strategies.
  * @author
  *   mzechner (original implementation)
  */
trait FileHandleResolver {
  def resolve(fileName: String): FileHandle
}

object FileHandleResolver {

  /*
   * Original source: com/badlogic/gdx/assets/loaders/resolvers/AbsoluteFileHandleResolver.java
   */
  class Absolute(using Sge) extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      Sge().files.absolute(fileName)
  }

  /*
   * Original source: com/badlogic/gdx/assets/loaders/resolvers/ClasspathFileHandleResolver.java
   */
  class Classpath(using Sge) extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      Sge().files.classpath(fileName)
  }

  /*
   * Original source: com/badlogic/gdx/assets/loaders/resolvers/ExternalFileHandleResolver.java
   */
  class External(using Sge) extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      Sge().files.external(fileName)
  }

  /*
   * Original source: com/badlogic/gdx/assets/loaders/resolvers/InternalFileHandleResolver.java
   */
  class Internal(using Sge) extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      Sge().files.internal(fileName)
  }

  /*
   * Original source: com/badlogic/gdx/assets/loaders/resolvers/LocalFileHandleResolver.java
   */
  class Local(using Sge) extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      Sge().files.local(fileName)
  }

  /*
   * Original source: com/badlogic/gdx/assets/loaders/resolvers/PrefixFileHandleResolver.java
   * Original authors: Xoppa
   *
   * Renames: getBaseResolver/setBaseResolver -> var baseResolver; getPrefix/setPrefix -> var prefix
   */

  /** {@link FileHandleResolver} that adds a prefix to the filename before passing it to the base resolver. Can be used e.g. to use a given subfolder from the base resolver. The prefix is added as is,
    * you have to include any trailing '/' character if needed.
    * @author
    *   Xoppa (original implementation)
    */
  class Prefix(var baseResolver: FileHandleResolver, var prefix: String) extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      baseResolver.resolve(prefix + fileName)
  }

  /*
   * Original source: com/badlogic/gdx/assets/loaders/resolvers/ResolutionFileResolver.java
   */

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
  class ForResolution(protected val baseResolver: FileHandleResolver, protected val descriptors: Array[Resolution])(using Sge) extends FileHandleResolver {
    if (descriptors.isEmpty) throw new IllegalArgumentException("At least one Resolution needs to be supplied.")

    /** Creates a {@code ForResolution} based on a given {@link FileHandleResolver} and a list of {@link Resolution} s.
      * @param baseResolver
      *   The {@link FileHandleResolver} that will ultimately used to resolve the file.
      * @param descriptors
      *   A list of {@link Resolution} s. At least one has to be supplied.
      */
    def this(baseResolver: FileHandleResolver, descriptors: Resolution*)(using Sge) = {
      this(baseResolver, descriptors.toArray)
    }

    override def resolve(fileName: String): FileHandle = {
      val bestResolution = ForResolution.choose(descriptors*)
      val originalHandle = FileHandle(new java.io.File(fileName), FileType.Absolute)
      val handle         = baseResolver.resolve(resolve(originalHandle, bestResolution.folder))
      if (!handle.exists()) baseResolver.resolve(fileName) else handle
    }

    protected def resolve(originalHandle: FileHandle, suffix: String): String = {
      val parentString = Nullable(originalHandle.parent())
        .map { parent =>
          if (parent.name.equals("")) "" else parent.path + "/"
        }
        .getOrElse("")
      parentString + suffix + "/" + originalHandle.name
    }
  }

  object ForResolution {
    def choose(descriptors: Resolution*)(using Sge): Resolution = {
      val w = Sge().graphics.backBufferWidth
      val h = Sge().graphics.backBufferHeight

      // Prefer the shortest side.
      var best = descriptors(0)
      if (w < h) {
        var i = 0
        val n = descriptors.length
        while (i < n) {
          val other = descriptors(i)
          if (
            w.toInt >= other.portraitWidth && other.portraitWidth >= best.portraitWidth && h.toInt >= other.portraitHeight
            && other.portraitHeight >= best.portraitHeight
          ) {
            best = descriptors(i)
          }
          i += 1
        }
      } else {
        var i = 0
        val n = descriptors.length
        while (i < n) {
          val other = descriptors(i)
          if (
            w.toInt >= other.portraitHeight && other.portraitHeight >= best.portraitHeight && h.toInt >= other.portraitWidth
            && other.portraitWidth >= best.portraitWidth
          ) {
            best = descriptors(i)
          }
          i += 1
        }
      }
      best
    }
  }

  final case class Resolution(portraitWidth: Int, portraitHeight: Int, folder: String)
}
