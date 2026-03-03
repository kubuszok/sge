/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/AssetLoader.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: `Array<AssetDescriptor>` -> `DynamicArray[AssetDescriptor[?]]`
 *   Idiom: split packages
 *   Issues: flat `package sge.assets.loaders` instead of split form
 *   TODO: uses flat package declaration — convert to split (package sge / package assets / package loaders)
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge.assets.loaders

import sge.files.FileHandle
import sge.assets.AssetLoaderParameters
import sge.utils.DynamicArray

/** Abstract base class for asset loaders.
  * @author
  *   mzechner (original implementation)
  *
  * @tparam T
  *   the class of the asset the loader supports
  * @tparam P
  *   the class of the loading parameters the loader supports.
  */
abstract class AssetLoader[T, P <: AssetLoaderParameters[T]](private val resolver: FileHandleResolver) {

  /** @param fileName
    *   file name to resolve
    * @return
    *   handle to the file, as resolved by the FileHandleResolver set on the loader
    */
  def resolve(fileName: String): FileHandle =
    resolver.resolve(fileName)

  /** Returns the assets this asset requires to be loaded first. This method may be called on a thread other than the GL thread.
    * @param fileName
    *   name of the asset to load
    * @param file
    *   the resolved file to load
    * @param parameter
    *   parameters for loading the asset
    * @return
    *   other assets that the asset depends on and need to be loaded first or null if there are no dependencies.
    */
  def getDependencies(fileName: String, file: FileHandle, parameter: P): DynamicArray[sge.assets.AssetDescriptor[?]]
}
