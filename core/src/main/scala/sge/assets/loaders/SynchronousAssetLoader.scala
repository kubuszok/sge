package sge
package assets
package loaders

import sge.files.FileHandle

abstract class SynchronousAssetLoader[T, P <: AssetLoaderParameters[T]](resolver: FileHandleResolver) extends AssetLoader[T, P](resolver) {
  def load(assetManager: AssetManager, fileName: String, file: FileHandle, parameter: P): T
}
