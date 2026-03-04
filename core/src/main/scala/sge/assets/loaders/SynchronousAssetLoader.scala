/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/SynchronousAssetLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle

abstract class SynchronousAssetLoader[T, P <: AssetLoaderParameters[T]](resolver: FileHandleResolver) extends AssetLoader[T, P](resolver) {
  def load(assetManager: AssetManager, fileName: String, file: FileHandle, parameter: P): T
}
