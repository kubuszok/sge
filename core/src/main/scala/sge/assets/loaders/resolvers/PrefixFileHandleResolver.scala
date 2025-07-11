package sge
package assets
package loaders
package resolvers

import sge.files.FileHandle

/** {@link FileHandleResolver} that adds a prefix to the filename before passing it to the base resolver. Can be used e.g. to use a given subfolder from the base resolver. The prefix is added as is,
  * you have to include any trailing '/' character if needed.
  * @author
  *   Xoppa (original implementation)
  */
class PrefixFileHandleResolver(private var baseResolver: FileHandleResolver, private var prefix: String) extends FileHandleResolver {

  def setBaseResolver(baseResolver: FileHandleResolver): Unit =
    this.baseResolver = baseResolver

  def getBaseResolver: FileHandleResolver =
    baseResolver

  def setPrefix(prefix: String): Unit =
    this.prefix = prefix

  def getPrefix: String =
    prefix

  override def resolve(fileName: String): FileHandle =
    baseResolver.resolve(prefix + fileName)
}
