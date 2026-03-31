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

import sge.graphics.Pixmap

/** Load a [[Pixmap]] from binary image data (PNG/JPEG bytes). In LibGDX this used reflection to work around GWT; in SGE we can call the constructor directly on JVM/Native and throw on JS.
  */
object PixmapBinaryLoaderHack {

  def load(encodedData: Array[Byte], offset: Int, len: Int): Pixmap =
    new Pixmap(encodedData, offset, len)
}
