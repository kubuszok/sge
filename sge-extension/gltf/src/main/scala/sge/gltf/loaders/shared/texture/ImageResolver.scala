/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package shared
package texture

import scala.collection.mutable.ArrayBuffer
import sge.graphics.Pixmap
import sge.gltf.data.texture.GLTFImage
import sge.gltf.loaders.shared.data.DataFileResolver
import sge.utils.Nullable

class ImageResolver(private val dataFileResolver: DataFileResolver) extends AutoCloseable {

  private val pixmaps: ArrayBuffer[Pixmap] = ArrayBuffer.empty

  def load(glImages: Nullable[ArrayBuffer[GLTFImage]]): Unit = {
    glImages.foreach { images =>
      var i = 0
      while (i < images.size) {
        val glImage = images(i)
        val pixmap = dataFileResolver.load(glImage)
        pixmaps += pixmap
        i += 1
      }
    }
  }

  def get(index: Int): Pixmap = pixmaps(index)

  override def close(): Unit = {
    for (pixmap <- pixmaps) {
      pixmap.close()
    }
    pixmaps.clear()
  }

  def clear(): Unit = {
    pixmaps.clear()
  }

  def getPixmaps(array: ArrayBuffer[Pixmap]): ArrayBuffer[Pixmap] = {
    array ++= pixmaps
    array
  }
}
