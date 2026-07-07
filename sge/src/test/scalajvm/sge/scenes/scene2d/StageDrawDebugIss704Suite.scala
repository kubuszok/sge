/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-704 (review-fable, major): Stage.drawDebug's
 * debugTableUnderMouse SUCCESS path is INVERTED versus the original
 * com/badlogic/gdx/scenes/scene2d/Stage.java (original-src/libgdx). Cited Java
 * line numbers refer to that file; port line numbers refer to
 * sge/src/main/scala/sge/scenes/scene2d/Stage.scala.
 *
 * Original Java, drawDebug() debugTableUnderMouse != Debug.none branch
 * (Stage.java lines 152-165):
 *
 *     if (debugTableUnderMouse == Debug.none)
 *         actor.setDebug(true);
 *     else {
 *         while (actor != null) {
 *             if (actor instanceof Table) break;   // FOUND: `actor` STAYS the Table
 *             actor = actor.parent;
 *         }
 *         if (actor == null) return;               // NOT-found: bail out
 *         ((Table)actor).debug(debugTableUnderMouse);
 *     }
 *
 *     if (debugAll && actor instanceof Group) ((Group)actor).debugAll();
 *
 *     disableDebug(root, actor);                    // (line 165) run on the FOUND Table
 *
 * i.e. when a Table IS found under the mouse, `break` PRESERVES the found Table
 * in `actor`; `disableDebug(root, actor)` (Java 165) then disables debug on
 * every actor EXCEPT that Table (and its children), and the debug-shape draw
 * block (Java 170-175) proceeds.
 *
 * The port (Stage.scala lines 150-164) INVERTS this — on the SUCCESS branch it
 * clears `actor` before breaking, then that clear trips the not-found guard:
 *
 *     case t: Table =>
 *       t.debug(debugTableUnderMouse)
 *       actor = Nullable.empty                      // BUG: clears the FOUND Table
 *       scala.util.boundary.break(())
 *     ...
 *     if (actor.isEmpty) shouldDraw = false         // fires on SUCCESS => draw suppressed
 *     ...
 *     actor.foreach(a => disableDebug(_root, a))    // actor empty => disableDebug SKIPPED
 *
 * So EXACTLY when a debuggable Table is found under the mouse, the port skips
 * `disableDebug(_root, table)` (Stage.scala line 172) and sets `shouldDraw =
 * false` (line 164), suppressing the debug-shape draw block (line 178) — the
 * opposite of Java 165/170-175.
 *
 * Observable pinned here: `disableDebug(root, foundTable)` (Java 165) disables
 * debug on every actor OUTSIDE the found Table's subtree. We add a witness
 * sibling of the found Table, pre-set its debug flag to true, and after
 * drawDebug assert the sibling's debug flag was cleared — which only happens if
 * disableDebug ran (i.e. the found Table was preserved). On the current
 * (inverted) port disableDebug is skipped, so the witness stays true and the
 * SUCCESS test FAILS.
 *
 * Headless strategy: drawDebug() is reached via the public draw() with
 * Stage.debug == true (which setDebugTableUnderMouse enables, Stage.scala line
 * 890). drawDebug() unconditionally constructs a ShapeRenderer (Stage.scala
 * line 130), whose default ImmediateModeRenderer20 shader would otherwise throw
 * headlessly (createDefaultShader's `if (!program.compiled) throw`,
 * ImmediateModeRenderer20.scala line 238, because NoopGL20.glCreateShader
 * returns 0). FakeCompilingGL20 below reports successful shader compile/link
 * and hands out a nonzero buffer handle (as SpriteBatch's VertexBufferObject
 * mesh requires, mirroring SpriteBatchVertexGeometryISS561Suite's BufferGL20),
 * so ShapeRenderer / SpriteBatch construct and all draw calls are no-ops. On
 * the current code the SUCCESS path sets shouldDraw = false so the shape-draw
 * block is never entered; the RED failure is a clean assertion, not a throw.
 *
 * This test is written by the reproducer agent and MUST NOT be modified by the
 * fixer: it encodes the original Java semantics, not the port's.
 */
package sge
package scenes
package scene2d

import java.nio.IntBuffer

import sge.graphics.{ GL20, ShaderType }
import sge.noop.{ NoopGL20, NoopGraphics }
import sge.scenes.scene2d.ui.Table
import lowlevel.Nullable

class StageDrawDebugIss704Suite extends munit.FunSuite {

  /** A GL20 that makes the default ShapeRenderer/SpriteBatch shaders "compile" and "link" successfully headlessly (NoopGL20 alone reports handle 0 / uncompiled, which throws in createDefaultShader),
    * and hands out a nonzero buffer handle for the VertexBufferObject meshes. Everything else delegates to NoopGL20 as a no-op.
    */
  final private class FakeCompilingGL20 extends GL20 {
    private val underlying: GL20 = NoopGL20
    export underlying.{ glCreateProgram as _, glCreateShader as _, glGenBuffer as _, glGetProgramiv as _, glGetShaderiv as _, * }

    def glCreateShader(`type`: ShaderType): Int = 1
    def glCreateProgram():                  Int = 1
    def glGenBuffer():                      Int = 1

    def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit =
      params.put(0, if (pname == GL20.GL_COMPILE_STATUS) GL20.GL_TRUE else 0)

    def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit =
      params.put(0, if (pname == GL20.GL_LINK_STATUS) GL20.GL_TRUE else 0)
  }

  private def makeSge(): Sge =
    SgeTestFixture.testSge(graphics = new NoopGraphics() {
      override def gl20: GL20 = new FakeCompilingGL20()
    })

  /** Runs body with the global Stage.debug flag forced on (drawDebug is only invoked from draw() when Stage.debug is true, Stage.scala line 124), restoring it afterwards. */
  private def withStageDebug[A](body: => A): A = {
    val saved = Stage.debug
    try body
    finally Stage.debug = saved
  }

  // ---------------------------------------------------------------------------
  // SUCCESS path: a debuggable Table IS under the mouse.
  // ---------------------------------------------------------------------------

  test(
    "ISS-704: drawDebug with a Table under the mouse runs disableDebug on the found Table (Java Stage.java:165), clearing debug on outside actors"
  ) {
    given Sge = makeSge()
    val stage = new Stage()

    // Root subtree hit by the mouse (NoopInput reports (0,0) -> stage coords
    // land inside these huge bounds): leaf inside a Table. hit() returns the
    // leaf; drawDebug's while-loop climbs leaf -> table and finds the Table
    // (Java 155-156 / Stage.scala 152-162).
    val table = new Table()
    table.setBounds(0f, 0f, 100000f, 100000f)
    table.touchable = Touchable.enabled

    val leaf = Actor()
    leaf.setBounds(0f, 0f, 100000f, 100000f)
    leaf.touchable = Touchable.enabled
    table.addActor(leaf)

    // Witness sibling of the Table (outside the found Table's subtree). Its
    // debug is pre-set true; disableDebug(root, table) (Java 165) must clear it.
    // touchable=disabled so hit() never returns it (the mouse must resolve to
    // the Table subtree, not this sibling).
    val witness = Actor()
    witness.setBounds(0f, 0f, 10f, 10f)
    witness.touchable = Touchable.disabled

    stage.addActor(table)
    stage.addActor(witness)

    // Enable table-under-mouse debug (also sets Stage.debug = true, Stage.scala 890).
    stage.setDebugTableUnderMouse(Nullable(Table.Debug.table))
    witness.setDebug(true)

    withStageDebug {
      stage.draw()
    }

    // t.debug(...) fires on both original and port (Java 160 / Stage.scala 156),
    // so the found Table keeps debug enabled — sanity check, not the discriminator.
    assert(table.isDebug, "found Table should have debug enabled via t.debug(debugTableUnderMouse) (Java Stage.java:160)")

    // THE DISCRIMINATOR: original disableDebug(root, table) (Java Stage.java:165)
    // disables debug on the witness (outside the Table subtree). The inverted
    // port clears `actor` before break (Stage.scala:157) so disableDebug is
    // skipped (Stage.scala:172) and the witness stays debug=true.
    assertEquals(
      witness.isDebug,
      false,
      "disableDebug(root, foundTable) (Java Stage.java:165) must clear debug on actors outside the found Table's subtree; the inverted port skips it"
    )
  }

  // ---------------------------------------------------------------------------
  // CONTROL path: NO Table under the mouse (the not-found branch, unchanged).
  // ---------------------------------------------------------------------------

  test(
    "ISS-704: drawDebug with NO Table under the mouse takes the not-found branch (Java Stage.java:159) and leaves outside actors untouched"
  ) {
    given Sge = makeSge()
    val stage = new Stage()

    // A plain (non-Table) actor under the mouse: the while-loop climbs
    // plain -> root -> empty without ever hitting a Table, so `actor` ends
    // empty and drawDebug bails before disableDebug (Java 159 `if (actor==null)
    // return` / Stage.scala 164 `if (actor.isEmpty) shouldDraw = false`).
    val plain = Actor()
    plain.setBounds(0f, 0f, 100000f, 100000f)
    plain.touchable = Touchable.enabled

    val witness = Actor()
    witness.setBounds(0f, 0f, 10f, 10f)
    witness.touchable = Touchable.disabled

    stage.addActor(plain)
    stage.addActor(witness)

    stage.setDebugTableUnderMouse(Nullable(Table.Debug.table))
    witness.setDebug(true)

    withStageDebug {
      stage.draw()
    }

    // No Table found => disableDebug is NOT called in either version; the
    // witness keeps its debug flag. This branch is unchanged by the fix.
    assertEquals(
      witness.isDebug,
      true,
      "with no Table under the mouse the not-found branch (Java Stage.java:159) bails before disableDebug, so outside actors are untouched"
    )
  }
}
