package sge
package assets
package loaders

import sge.files.FileHandle

/** Interface for classes the can map a file name to a {@link FileHandle} . Used to allow the {@link AssetManager} to load resources from anywhere or implement caching strategies.
  * @author
  *   mzechner (original implementation)
  */
trait FileHandleResolver {
  def resolve(fileName: String): FileHandle
}
