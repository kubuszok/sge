/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/TextureMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: 8 getter/setter pairs -> public vars
 *   Convention: nullable `TextureRegion` field typed as `Nullable[TextureRegion]`
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: TextureMapObject,originX,originY,rotation,scaleX,scaleY,textureRegion,x,y
 * Covenant-source-reference: com/badlogic/gdx/maps/objects/TextureMapObject.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps
package objects

import sge.graphics.g2d.TextureRegion
import sge.utils.Nullable

/** @brief Represents a map object containing a texture (region) */
class TextureMapObject(initialRegion: Nullable[TextureRegion] = Nullable.empty) extends MapObject {

  var x:             Float                   = 0.0f
  var y:             Float                   = 0.0f
  var originX:       Float                   = 0.0f
  var originY:       Float                   = 0.0f
  var scaleX:        Float                   = 1.0f
  var scaleY:        Float                   = 1.0f
  var rotation:      Float                   = 0.0f
  var textureRegion: Nullable[TextureRegion] = initialRegion
}
