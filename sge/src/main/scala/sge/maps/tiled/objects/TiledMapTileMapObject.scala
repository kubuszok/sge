/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/objects/TiledMapTileMapObject.java
 * Original authors: Daniel Holderbaum
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Audited 2026-03-04 against libGDX source: all methods, fields, and constructor match 1:1
 * - Constructor body uses locally{} block for init (matches Java constructor body)
 * - textureRegion assigned with Nullable() due to SGE's TextureMapObject using Nullable
 * - Renames: isFlipHorizontally/setFlipHorizontally -> var flipHorizontally,
 *            isFlipVertically/setFlipVertically -> var flipVertically,
 *            getTile/setTile -> var tile
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 42
 * Covenant-baseline-methods: TiledMapTileMapObject,flipHorizontally,flipVertically,region,tile
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/objects/TiledMapTileMapObject.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 671b8b6b402c53b89192508405f059efe5adfe38
 */
package sge
package maps
package tiled
package objects

import sge.graphics.g2d.TextureRegion
import sge.maps.objects.TextureMapObject
import sge.utils.Nullable

/** A {@link MapObject} with a {@link TiledMapTile}. Can be both {@link StaticTiledMapTile} or {@link AnimatedTiledMapTile}. For compatibility reasons, this extends {@link TextureMapObject}. Use
  * {@link TiledMapTile#getTextureRegion()} instead of {@link #getTextureRegion()}.
  * @author
  *   Daniel Holderbaum
  */
class TiledMapTileMapObject(
  var tile:             TiledMapTile,
  var flipHorizontally: Boolean,
  var flipVertically:   Boolean
) extends TextureMapObject() {

  locally {
    val region = TextureRegion(tile.textureRegion)
    region.flip(flipHorizontally, flipVertically)
    this.textureRegion = Nullable(region)
  }
}
