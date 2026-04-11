/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/loaders/shared/SceneAssetLoaderParameters.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package shared

import sge.assets.AssetLoaderParameters
import sge.gltf.scene3d.scene.SceneAsset

class SceneAssetLoaderParameters extends AssetLoaderParameters[SceneAsset] {

  /** load scene asset with underlying GLTF {@link SceneAsset#data} structure */
  var withData: Boolean = false
}
