/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/TextureProvider.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils

import sge.graphics.Texture
import sge.graphics.Texture.{ TextureFilter, TextureWrap }

/** Used by {@link Model} to load textures from {@link ModelData}.
  * @author
  *   badlogic
  */
trait TextureProvider {
  def load(fileName: String): Texture
}

object TextureProvider {

  class FileTextureProvider(
    val minFilter:  TextureFilter,
    val magFilter:  TextureFilter,
    val uWrap:      TextureWrap,
    val vWrap:      TextureWrap,
    val useMipMaps: Boolean
  )(using sge: Sge)
      extends TextureProvider {

    def this()(using sge: Sge) =
      this(TextureFilter.Linear, TextureFilter.Linear, TextureWrap.Repeat, TextureWrap.Repeat, false)

    override def load(fileName: String): Texture = {
      val result = new Texture(sge.files.internal(fileName), useMipMaps)
      result.setFilter(minFilter, magFilter)
      result.setWrap(uWrap, vWrap)
      result
    }
  }

  class AssetTextureProvider(val assetManager: sge.assets.AssetManager) extends TextureProvider {
    override def load(fileName: String): Texture =
      assetManager.get[Texture](fileName, classOf[Texture])
  }
}
