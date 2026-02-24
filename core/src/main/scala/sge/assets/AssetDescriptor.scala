/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/AssetDescriptor.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets

import sge.files.FileHandle
import sge.utils.Nullable

/** Describes an asset to be loaded by its filename, type and AssetLoaderParameters. Instances of this are used in AssetLoadingTask to load the actual asset.
  * @author
  *   mzechner (original implementation)
  */
case class AssetDescriptor[T](
  fileName: String,
  `type`:   Class[T],
  params:   Nullable[AssetLoaderParameters[T]] = Nullable.empty,
  /** The resolved file. May be null if the fileName has not been resolved yet. */
  var file: Nullable[FileHandle] = Nullable.empty
) {

  def this(fileName: String, assetType: Class[T]) = {
    this(fileName, assetType, Nullable.empty, Nullable.empty)
  }

  /** Creates an AssetDescriptor with an already resolved name. */
  def this(file: FileHandle, assetType: Class[T]) = {
    this(file.path(), assetType, Nullable.empty, Nullable(file))
  }

  def this(fileName: String, assetType: Class[T], params: AssetLoaderParameters[T]) = {
    this(fileName, assetType, Nullable(params), Nullable.empty)
  }

  /** Creates an AssetDescriptor with an already resolved name. */
  def this(file: FileHandle, assetType: Class[T], params: AssetLoaderParameters[T]) = {
    this(file.path(), assetType, Nullable(params), Nullable(file))
  }

  override def toString: String =
    s"$fileName, ${`type`.getName}"
}
