/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/MipMapGenerator.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Issues: generateMipMap(target, pixmap, w, h) is an empty stub -- no GL texture upload occurs; generateMipMapChain returns only the original pixmap; missing generateMipMap 2-arg overload, setUseHardwareMipMap(), useHWMipMap field, and all private helper methods (generateMipMapCPU, generateMipMapGLES20, generateMipMapDesktop); package uses flat format instead of split
 *   TODO: uses flat package declaration — convert to split (package sge / package graphics / package glutils)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.graphics.glutils

import sge.graphics.Pixmap

object MipMapGenerator {

  /** Generates a list of mipmaps for the given pixmap and puts them into the given texture targets
    *
    * @param target
    *   the texture target to upload the mipmaps to
    * @param pixmap
    *   the pixmap to generate mipmaps from
    * @param textureWidth
    *   width of the base texture
    * @param textureHeight
    *   height of the base texture
    */
  def generateMipMap(target: Int, pixmap: Pixmap, textureWidth: Int, textureHeight: Int): Unit = {
    // Stub implementation - would generate mipmaps in real implementation
  }

  /** Generates a list of mipmaps for the given pixmap
    *
    * @param pixmap
    *   the pixmap to generate mipmaps from
    * @return
    *   a list of mipmaps including the original pixmap at index 0
    */
  def generateMipMapChain(pixmap: Pixmap): Array[Pixmap] =
    // Stub implementation - would generate mipmap chain in real implementation
    Array(pixmap)
}
