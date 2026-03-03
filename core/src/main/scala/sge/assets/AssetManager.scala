/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/AssetManager.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge.assets

import sge.utils.DynamicArray

// TODO: stub until we have: loaders, g2d, g3d, etc
trait AssetManager {
  def getLogLevel:                                                                        Int
  def addDependencies(fileName:  String, dependencies: DynamicArray[AssetDescriptor[?]]): Unit
  def get[T](fileName:           String, `type`:       Class[T]):                         T
  def getAssetFileName[T](asset: T):                                                      String
}
