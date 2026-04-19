/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/NodeKeyframe.java
 * Original authors: badlogic, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Constructor params (keytime, value) match Java fields exactly
 * - `value` is val (matching Java `final`), `keytime` is var (matching Java mutable)
 * - No API differences from Java source
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 29
 * Covenant-baseline-methods: NodeKeyframe,keytime,value
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/NodeKeyframe.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package model

/** A NodeKeyframe specifies the a value (e.g. the translation, rotation or scale) of a frame within a {@link NodeAnimation}.
  * @author
  *   badlogic, Xoppa
  */
class NodeKeyframe[T](
  /** the timestamp of this keyframe * */
  var keytime: Float,
  /** the value of this keyframe at the specified timestamp * */
  val value: T
) {}
