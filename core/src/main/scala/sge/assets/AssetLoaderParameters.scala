/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/AssetLoaderParameters.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface LoadedCallback -> Scala trait in companion object; raw Class -> Class[?]
 *   Idiom: split packages; loadedCallback uses uninitialized (nullable field, TODO: Nullable[LoadedCallback])
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
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
