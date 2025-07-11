package sge.assets

import scala.collection.mutable.ArrayBuffer
import sge.utils.Logger

// TODO: stub until we have: loaders, g2d, g3d, etc
trait AssetManager {
  def getLogLevel:                                                                      Int
  def addDependencies(fileName: String, dependencies: ArrayBuffer[AssetDescriptor[?]]): Unit
}
