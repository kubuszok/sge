/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/TextureBinder.java
 * Original authors: badlogic, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Java interface -> Scala trait
 *   - bind(TextureDescriptor) uses wildcard TextureDescriptor[?] (raw type in Java)
 *   - bind(GLTexture) param type correctly widened from Texture to GLTexture (matches Java)
 *   - getBindCount/getReuseCount: parentheses dropped (Scala property style)
 *   - All 7 methods fully ported
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 58
 * Covenant-baseline-methods: TextureBinder,begin,bind,end,getBindCount,getReuseCount,resetCounts
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/TextureBinder.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package utils

/** Responsible for binding textures, may implement a strategy to avoid binding a texture unnecessarily. A TextureBinder may decide to which texture unit it binds a texture.
  * @author
  *   badlogic, Xoppa
  */
trait TextureBinder {

  /** Prepares the binder for operation, must be matched with a call to [[end]]. */
  def begin(): Unit

  /** Disables all used texture units and unbinds textures. Resets the counts. */
  def end(): Unit

  /** Binds the texture to an available unit and applies the filters in the descriptor.
    * @param textureDescriptor
    *   the [[TextureDescriptor]]
    * @return
    *   the unit the texture was bound to
    */
  def bind(textureDescriptor: TextureDescriptor[?]): Int

  /** Binds the texture to an available unit.
    * @param texture
    *   the [[GLTexture]]
    * @return
    *   the unit the texture was bound to
    */
  def bind(texture: GLTexture): Int

  /** @return the number of binds actually executed since the last call to [[resetCounts]] */
  def getBindCount: Int

  /** @return the number of binds that could be avoided by reuse */
  def getReuseCount: Int

  /** Resets the bind/reuse counts */
  def resetCounts(): Unit
}
