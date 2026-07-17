/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test — ISS-734 umbrella clause c1 (core minors #2), wave 2026-07-17-F,
 * territory X. Reproducer-authored: MUST NOT be modified by the fixer; it
 * encodes the ORIGINAL LibGDX semantics, not the port's.
 *
 * DEFECT. SpriteBatch.enableBlending()/disableBlending() (SpriteBatch.scala
 * lines 988-996) unconditionally flush():
 *
 *   override def disableBlending(): Unit = { if (drawing) flush(); blendingDisabled = true }
 *   override def enableBlending():  Unit = { if (drawing) flush(); blendingDisabled = false }
 *
 * The original com/badlogic/gdx/graphics/g2d/SpriteBatch.java (original-src/libgdx),
 * lines 999-1010, starts each with an ALREADY-IN-STATE early return so that
 * toggling blending to the state it is already in does NOT flush the batch:
 *
 *   public void disableBlending () { if (blendingDisabled) return; flush(); blendingDisabled = true; }
 *   public void enableBlending ()  { if (!blendingDisabled) return; flush(); blendingDisabled = false; }
 *
 * OBSERVABLE. flush() (SpriteBatch.scala line 890) increments the public
 * `renderCalls` counter whenever idx != 0. So with sprites queued (idx > 0),
 * a same-state blending toggle costs an extra render call / flush in the port
 * that the original elides. We count renderCalls across a same-state toggle.
 *
 * Headless fixture mirrors SpriteBatchVertexGeometryISS561Suite: a NoopGL20
 * that hands out a nonzero buffer handle (so the constructor's index pre-bind
 * succeeds), a directly-instantiated ShaderProgram (bypasses createDefaultShader's
 * compiled check), and a Custom-type dummy texture (identity key, no Pixmap/GL).
 */
package sge
package graphics
package g2d

import sge.graphics.glutils.ShaderProgram
import sge.noop.{ NoopGL20, NoopGraphics }

class SpriteBatchBlendingToggleIss734RedSuite extends munit.FunSuite {

  private object BufferGL20 extends GL20 {
    private val underlying: GL20 = NoopGL20
    export underlying.{ glGenBuffer as _, * }
    def glGenBuffer(): Int = 1
  }

  private def makeSge(): Sge =
    SgeTestFixture.testSge(graphics = new NoopGraphics() {
      override def gl20: GL20 = BufferGL20
    })

  final private class DummyTextureData(w: Int, h: Int) extends TextureData {
    def dataType:                                 TextureData.TextureDataType = TextureData.TextureDataType.Custom
    def isPrepared:                               Boolean                     = true
    def prepare():                                Unit                        = ()
    def consumePixmap():                          Pixmap                      = throw new UnsupportedOperationException("dummy texture has no pixmap")
    def disposePixmap:                            Boolean                     = false
    def consumeCustomData(target: TextureTarget): Unit                        = ()
    def width:                                    Int                         = w
    def height:                                   Int                         = h
    def getFormat:                                Pixmap.Format               = Pixmap.Format.RGBA8888
    def useMipMaps:                               Boolean                     = false
    def isManaged:                                Boolean                     = false
  }

  private def dummyTexture()(using Sge): Texture = new Texture(new DummyTextureData(4, 4))

  private def makeBatch()(using Sge): SpriteBatch =
    new SpriteBatch(1000, new ShaderProgram("void main(){}", "void main(){}"))

  test(
    "ISS-734 c1: enableBlending() while blending is already enabled must NOT flush queued sprites (orig SpriteBatch.java:1006-1010 early return)"
  ) {
    given Sge   = makeSge()
    val batch   = makeBatch()
    val texture = dummyTexture()

    batch.begin() // blendingDisabled starts false => blending is ENABLED
    batch.draw(texture, 0f, 0f, 1f, 1f) // switchTexture flushes empty (no renderCall), then idx -> 20
    assertEquals(batch.idx, 20, "precondition: one sprite queued")
    assertEquals(batch.renderCalls, 0, "precondition: nothing flushed yet")

    // Blending is already enabled; enableBlending() is a same-state toggle.
    batch.enableBlending()

    // Original: early return (`if (!blendingDisabled) return;`) -> no flush -> renderCalls stays 0, idx stays 20.
    // Port: unconditional `if (drawing) flush()` -> idx(20) flushed -> renderCalls becomes 1, idx 0.
    assertEquals(
      batch.renderCalls,
      0,
      "same-state enableBlending() must not flush (SpriteBatch.java:1006-1010); the port drops the early return and flushes"
    )
    assertEquals(batch.idx, 20, "queued sprite must remain buffered after a same-state enableBlending()")

    batch.close()
  }

  test(
    "ISS-734 c1: disableBlending() while blending is already disabled must NOT flush queued sprites (orig SpriteBatch.java:999-1003 early return)"
  ) {
    given Sge   = makeSge()
    val batch   = makeBatch()
    val texture = dummyTexture()

    batch.begin()
    batch.disableBlending() // enabled -> disabled: a real state change, flushes empty idx (no renderCall)
    batch.draw(texture, 0f, 0f, 1f, 1f) // idx -> 20
    assertEquals(batch.idx, 20, "precondition: one sprite queued")
    assertEquals(batch.renderCalls, 0, "precondition: nothing flushed yet")

    // Blending is already disabled; disableBlending() is a same-state toggle.
    batch.disableBlending()

    assertEquals(
      batch.renderCalls,
      0,
      "same-state disableBlending() must not flush (SpriteBatch.java:999-1003); the port drops the early return and flushes"
    )
    assertEquals(batch.idx, 20, "queued sprite must remain buffered after a same-state disableBlending()")

    batch.close()
  }

  test("ISS-734 c1 (control): a REAL blending state change while drawing DOES flush queued sprites") {
    // Positive control — passes on both the port and the fixed version. Pins
    // that the early-return fix must guard ONLY the same-state case: a genuine
    // enabled->disabled change still flushes (SpriteBatch.java:1001 flush()).
    given Sge   = makeSge()
    val batch   = makeBatch()
    val texture = dummyTexture()

    batch.begin() // enabled
    batch.draw(texture, 0f, 0f, 1f, 1f) // idx -> 20
    assertEquals(batch.renderCalls, 0)

    batch.disableBlending() // enabled -> disabled: state change, must flush the queued sprite
    assertEquals(batch.renderCalls, 1, "a real blending state change must flush the queued batch")
    assertEquals(batch.idx, 0, "flush resets idx")

    batch.close()
  }
}
