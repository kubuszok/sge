/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/AssetDescriptor.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java class -> final case class; null fields -> Nullable[A]; raw Class -> Class[T]
 *   Idiom: Nullable (2 null), split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package assets

import scala.reflect.ClassTag

import sge.files.FileHandle
import sge.utils.Nullable

/** Describes an asset to be loaded by its filename, type and AssetLoaderParameters. Instances of this are used in AssetLoadingTask to load the actual asset.
  * @author
  *   mzechner (original implementation)
  */
final case class AssetDescriptor[T](
  fileName: String,
  `type`:   Class[T],
  params:   Nullable[AssetLoaderParameters[T]] = Nullable.empty,
  /** The resolved file. May be null if the fileName has not been resolved yet. */
  var file: Nullable[FileHandle] = Nullable.empty
) {

  def this(fileName: String, assetType: Class[T]) =
    this(fileName, assetType, Nullable.empty, Nullable.empty)

  /** Creates an AssetDescriptor with an already resolved name. */
  def this(file: FileHandle, assetType: Class[T]) =
    this(file.path, assetType, Nullable.empty, Nullable(file))

  def this(fileName: String, assetType: Class[T], params: AssetLoaderParameters[T]) =
    this(fileName, assetType, Nullable(params), Nullable.empty)

  /** Creates an AssetDescriptor with an already resolved name. */
  def this(file: FileHandle, assetType: Class[T], params: AssetLoaderParameters[T]) =
    this(file.path, assetType, Nullable(params), Nullable(file))

  override def toString: String =
    s"$fileName, ${`type`.getName}"
}

object AssetDescriptor {

  def apply[T: ClassTag](fileName: String): AssetDescriptor[T] =
    AssetDescriptor[T](fileName, summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], Nullable.empty, Nullable.empty)

  def apply[T: ClassTag](fileName: String, params: AssetLoaderParameters[T]): AssetDescriptor[T] =
    AssetDescriptor(fileName, summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], Nullable(params))

  def apply[T: ClassTag](file: FileHandle): AssetDescriptor[T] =
    AssetDescriptor(file.path, summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], Nullable.empty, Nullable(file))
}
