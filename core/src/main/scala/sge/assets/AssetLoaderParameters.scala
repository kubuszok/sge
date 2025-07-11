package sge
package assets

class AssetLoaderParameters[T] {

  var loadedCallback: AssetLoaderParameters.LoadedCallback = scala.compiletime.uninitialized
}
object AssetLoaderParameters {

  /** Callback interface that will be invoked when the {@link AssetManager} loaded an asset.
    * @author
    *   mzechner (original implementation)
    */
  trait LoadedCallback {
    def finishedLoading(assetManager: AssetManager, fileName: String, `type`: Class[?]): Unit
  }
}
