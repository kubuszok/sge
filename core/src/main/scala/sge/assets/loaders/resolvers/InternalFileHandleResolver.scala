package sge
package assets
package loaders
package resolvers

import sge.files.FileHandle

class InternalFileHandleResolver(using sge: Sge) extends FileHandleResolver {
  override def resolve(fileName: String): FileHandle =
    sge.files.internal(fileName)
}
