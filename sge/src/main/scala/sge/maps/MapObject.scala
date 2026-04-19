/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/MapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: (none)
 *   Renames: getName/setName → var name, getColor/setColor → var color,
 *     getOpacity/setOpacity → var opacity, isVisible/setVisible → var visible,
 *     getProperties → val properties
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 28
 * Covenant-baseline-methods: MapObject,color,name,opacity,properties,visible
 * Covenant-source-reference: com/badlogic/gdx/maps/MapObject.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps

import sge.graphics.Color

/** Generic Map entity with basic attributes like name, opacity, color */
class MapObject {
  var name:       String        = ""
  var opacity:    Float         = 1.0f
  var visible:    Boolean       = true
  val properties: MapProperties = MapProperties()
  var color:      Color         = Color.WHITE.cpy()
}
