/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelTexture.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source exactly
 * - All USAGE_* constants match Java values (moved to companion object)
 * - Fields use scala.compiletime.uninitialized (Java null defaults)
 * - usage initialized to 0 (matches Java default)
 * - No methods in Java source, none in Scala — pure data class
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 45
 * Covenant-baseline-methods: ModelTexture,USAGE_AMBIENT,USAGE_BUMP,USAGE_DIFFUSE,USAGE_EMISSIVE,USAGE_NONE,USAGE_NORMAL,USAGE_REFLECTION,USAGE_SHININESS,USAGE_SPECULAR,USAGE_TRANSPARENCY,USAGE_UNKNOWN,fileName,id,usage,uvScaling,uvTranslation
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/data/ModelTexture.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package model
package data

import sge.math.Vector2

class ModelTexture {
  var id:            String  = scala.compiletime.uninitialized
  var fileName:      String  = scala.compiletime.uninitialized
  var uvTranslation: Vector2 = scala.compiletime.uninitialized
  var uvScaling:     Vector2 = scala.compiletime.uninitialized
  var usage:         Int     = 0
}

object ModelTexture {
  final val USAGE_UNKNOWN      = 0
  final val USAGE_NONE         = 1
  final val USAGE_DIFFUSE      = 2
  final val USAGE_EMISSIVE     = 3
  final val USAGE_AMBIENT      = 4
  final val USAGE_SPECULAR     = 5
  final val USAGE_SHININESS    = 6
  final val USAGE_NORMAL       = 7
  final val USAGE_BUMP         = 8
  final val USAGE_TRANSPARENCY = 9
  final val USAGE_REFLECTION   = 10
}
