/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/ImageResolver.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: ObjectMap[String,Texture] -> mutable.Map[String,Texture] (DirectImageResolver)
 *   Convention: interface -> trait; static inner classes -> companion object classes
 *   Idiom: getImage returns Nullable[TextureRegion] instead of nullable TextureRegion
 *   Idiom: DirectImageResolver uses fold on Option from Map.get instead of direct null-returning ObjectMap.get
 *   Idiom: TextureAtlasImageResolver maps AtlasRegion to TextureRegion via supertype cast
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 55
 * Covenant-baseline-methods: AssetManagerImageResolver,DirectImageResolver,ImageResolver,TextureAtlasImageResolver,getImage
 * Covenant-source-reference: com/badlogic/gdx/maps/ImageResolver.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
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

  final class DirectImageResolver(images: mutable.Map[String, Texture]) extends ImageResolver {
    override def getImage(name: String): Nullable[TextureRegion] =
      images.get(name).fold(Nullable.empty[TextureRegion])(t => Nullable(TextureRegion(t)))
  }

  final class AssetManagerImageResolver(assetManager: AssetManager) extends ImageResolver {
    override def getImage(name: String): Nullable[TextureRegion] =
      Nullable(TextureRegion(assetManager[Texture](name)))
  }

  final class TextureAtlasImageResolver(atlas: TextureAtlas) extends ImageResolver {
    override def getImage(name: String): Nullable[TextureRegion] =
      atlas.findRegion(name).map(r => r: TextureRegion)
  }
}
