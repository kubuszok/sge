/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/ImageResolver.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps

import scala.collection.mutable
import sge.assets.AssetManager
import sge.graphics.Texture
import sge.graphics.g2d.{ TextureAtlas, TextureRegion }
import sge.utils.Nullable

/** Resolves an image by a string, wrapper around a Map or AssetManager to load maps either directly or via AssetManager.
  * @author
  *   mzechner
  */
trait ImageResolver {

  /** @param name
    * @return
    *   the Texture for the given image name or null.
    */
  def getImage(name: String): Nullable[TextureRegion]
}

object ImageResolver {

  class DirectImageResolver(images: mutable.Map[String, Texture]) extends ImageResolver {
    override def getImage(name: String): Nullable[TextureRegion] =
      images.get(name).fold(Nullable.empty[TextureRegion])(t => Nullable(new TextureRegion(t)))
  }

  class AssetManagerImageResolver(assetManager: AssetManager) extends ImageResolver {
    override def getImage(name: String): Nullable[TextureRegion] =
      Nullable(new TextureRegion(assetManager.get(name, classOf[Texture])))
  }

  class TextureAtlasImageResolver(atlas: TextureAtlas) extends ImageResolver {
    override def getImage(name: String): Nullable[TextureRegion] =
      atlas.findRegion(name).map(r => r: TextureRegion)
  }
}
