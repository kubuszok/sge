/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/AssetManager.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java class -> stub trait (pending full implementation); Disposable not yet extended
 *   Idiom: split packages
 *   Status: STUB — only 4 of ~30+ public methods ported; see TODO
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets

import sge.utils.DynamicArray

// TODO: stub until we have: loaders, g2d, g3d, etc
trait AssetManager {
  def getLogLevel:                                                                        Int
  def addDependencies(fileName:  String, dependencies: DynamicArray[AssetDescriptor[?]]): Unit
  def get[T](fileName:           String, `type`:       Class[T]):                         T
  def getAssetFileName[T](asset: T):                                                      String
}
