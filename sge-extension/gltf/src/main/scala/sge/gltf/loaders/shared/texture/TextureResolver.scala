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

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.language.implicitConversions
import sge.Sge
import sge.graphics.{Pixmap, Texture}
import sge.graphics.Texture.{TextureFilter, TextureWrap}
import sge.graphics.g3d.utils.TextureDescriptor
import sge.gltf.data.texture.{GLTFSampler, GLTFTexture, GLTFTextureInfo}
import sge.gltf.loaders.exceptions.GLTFRuntimeException
import sge.gltf.loaders.shared.GLTFTypes
import sge.utils.Nullable

class TextureResolver extends AutoCloseable {

  protected val texturesSimple: HashMap[Int, Texture] = HashMap.empty
  protected val texturesMipmap: HashMap[Int, Texture] = HashMap.empty
  protected var glTextures: Nullable[ArrayBuffer[GLTFTexture]] = Nullable.empty
  protected var glSamplers: Nullable[ArrayBuffer[GLTFSampler]] = Nullable.empty

  def loadTextures(
      glTextures: Nullable[ArrayBuffer[GLTFTexture]],
      glSamplers: Nullable[ArrayBuffer[GLTFSampler]],
      imageResolver: ImageResolver
  )(using Sge): Unit = {
    this.glTextures = glTextures
    this.glSamplers = glSamplers
    glTextures.foreach { textures =>
      var i = 0
      while (i < textures.size) {
        val glTexture = textures(i)

        // check if mipmap needed for this texture configuration
        var useMipMaps = false
        glTexture.sampler.foreach { samplerIdx =>
          val sampler = glSamplers.get(samplerIdx)
          if (GLTFTypes.isMipMapFilter(sampler)) {
            useMipMaps = true
          }
        }

        val textureMap = if (useMipMaps) texturesMipmap else texturesSimple

        glTexture.source.foreach { sourceIdx =>
          if (!textureMap.contains(sourceIdx)) {
            val pixmap = imageResolver.get(sourceIdx)
            val texture = new Texture(pixmap, useMipMaps)
            textureMap.put(sourceIdx, texture)
          }
        }
        i += 1
      }
    }
  }

  def getTexture(glMap: GLTFTextureInfo): TextureDescriptor[Texture] = {
    val glTexture = glTextures.get(glMap.index.get)

    val textureDescriptor = new TextureDescriptor[Texture]()

    var useMipMaps = false
    glTexture.sampler.fold {
      // default sampler options.
      // https://github.com/KhronosGroup/glTF/blob/master/specification/2.0/README.md#texture
      textureDescriptor.minFilter = TextureFilter.Linear
      textureDescriptor.magFilter = TextureFilter.Linear
      textureDescriptor.uWrap = TextureWrap.Repeat
      textureDescriptor.vWrap = TextureWrap.Repeat
    } { samplerIdx =>
      val glSampler = glSamplers.get(samplerIdx)
      GLTFTypes.mapTextureSampler(textureDescriptor, glSampler)
      useMipMaps = GLTFTypes.isMipMapFilter(glSampler)
    }

    val textureMap = if (useMipMaps) texturesMipmap else texturesSimple

    val texture = textureMap.get(glTexture.source.get) match {
      case Some(t) => t
      case scala.None => throw new GLTFRuntimeException("texture not loaded")
    }
    textureDescriptor.texture = texture
    textureDescriptor
  }

  override def close(): Unit = {
    for ((_, texture) <- texturesSimple) {
      texture.close()
    }
    texturesSimple.clear()
    for ((_, texture) <- texturesMipmap) {
      texture.close()
    }
    texturesMipmap.clear()
  }

  def getTextures(textures: ArrayBuffer[Texture]): ArrayBuffer[Texture] = {
    for ((_, texture) <- texturesSimple) {
      textures += texture
    }
    for ((_, texture) <- texturesMipmap) {
      textures += texture
    }
    textures
  }
}
