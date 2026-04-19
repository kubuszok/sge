/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/TextureProvider.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Java interface -> Scala trait
 *   - Inner static classes (FileTextureProvider, AssetTextureProvider) -> companion object classes
 *   - FileTextureProvider: Gdx.files.internal -> Sge().files.internal (using Sge)
 *   - FileTextureProvider: fields promoted to constructor params (val)
 *   - AssetTextureProvider: assetManager.get(fileName, Texture.class) -> assetManager.get[Texture](...)
 *   - All methods fully ported
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 60
 * Covenant-baseline-methods: AssetTextureProvider,FileTextureProvider,TextureProvider,load,magFilter,minFilter,this,uWrap,useMipMaps,vWrap
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/TextureProvider.java
 * Covenant-verified: 2026-04-19
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

  final class FileTextureProvider(
    val minFilter:  TextureFilter,
    val magFilter:  TextureFilter,
    val uWrap:      TextureWrap,
    val vWrap:      TextureWrap,
    val useMipMaps: Boolean
  )(using Sge)
      extends TextureProvider {

    def this()(using Sge) =
      this(TextureFilter.Linear, TextureFilter.Linear, TextureWrap.Repeat, TextureWrap.Repeat, false)

    override def load(fileName: String): Texture = {
      val result = Texture(Sge().files.internal(fileName), useMipMaps)
      result.setFilter(minFilter, magFilter)
      result.setWrap(uWrap, vWrap)
      result
    }
  }

  final class AssetTextureProvider(val assetManager: sge.assets.AssetManager) extends TextureProvider {
    override def load(fileName: String): Texture =
      assetManager[Texture](fileName)
  }
}
