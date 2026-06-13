/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-532 (Draggable mimic size stays 0 -> keep-within-parent
 * clamp keeps the WRONG bounds, letting the dragged payload escape the parent).
 *
 * Original semantics (original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/
 * widget/Draggable.java, upstream commit
 * 820300c86a1bd907404217195a9987e5c66d2220 — the commit every visui header
 * references):
 *
 *   - MimicActor.getWidth()  (Draggable.java lines 535-538) and
 *     MimicActor.getHeight() (lines 540-543) are *overrides* that delegate
 *     LIVE to the mimicked actor:
 *         return actor == null ? 0f : actor.getWidth();
 *         return actor == null ? 0f : actor.getHeight();
 *     so as soon as setActor(actor) runs in attachMimic (line 297), the mimic
 *     reports the dragged actor's real width/height.
 *   - getStageCoordinatesWithinParent (Draggable.java lines 372-397) clamps
 *     using those sizes so the WHOLE actor stays inside the parent:
 *         else if (MIMIC.x + mimic.getWidth()  > parentEndX)
 *                  MIMIC.x = parentEndX - mimic.getWidth();   (lines 384-385)
 *         else if (MIMIC.y + mimic.getHeight() > parentEndY)
 *                  MIMIC.y = parentEndY - mimic.getHeight();  (lines 389-390)
 *     With a 100x40 actor in a 300x200 parent, a drag toward the top-right
 *     corner must clamp the mimic to x = parentEndX - 100 and y = parentEndY -
 *     40, keeping the actor's full footprint within parent bounds.
 *
 * The port (sge-extension/visui/src/main/scala/sge/visui/widget/Draggable.scala)
 * does NOT override the inherited Actor width/height vars. Instead MimicActor
 * carries an updateSize() method (Draggable.scala lines 336-337) that would
 * copy actor.width/actor.height into the inherited vars — but updateSize() has
 * ZERO callers (grep across sge-extension/visui finds only its own definition
 * and the covenant header). So mimic.width and mimic.height stay 0, and the
 * clamp at Draggable.scala lines 196-198 / 171-173 uses 0:
 *         else if (MIMIC.x + mimic.width > parentEndX) MIMIC.x = parentEndX - mimic.width
 * With mimic.width == 0 the predicate (290 + 0 > 300) is false, so MIMIC.x is
 * left at 290 and the 100-wide actor overflows the parent's right edge by 90.
 * Same overflow on the top edge for height.
 *
 * Observable consequence (preferred, robust assertion): after a drag toward the
 * top-right corner, the mimic's stage position must satisfy
 *   mimic.x <= parentEndX - actor.width  and  mimic.y <= parentEndY - actor.height
 * i.e. the full actor footprint stays inside the parent. Under the bug the
 * mimic sits at (290, 190) and the actor's right/top edges land at 390/230,
 * 90/30 past the 300/200 parent bounds.
 *
 * Harness: a real headless Stage (VisUITestFixture.headlessSge() -> NoopGraphics
 * for width/height, no-op GL20) built with the 2-arg Stage(viewport, batch)
 * constructor and a no-op StubBatch. A real SpriteBatch cannot be built
 * headlessly — its constructor binds an IndexBufferObject which throws
 * "No buffer allocated!" under NoopGL20 — and the drag path never renders, so
 * the only Batch surface touched is the constructor: a do-nothing StubBatch
 * (begin/end are `private[sge]`, visible and overridable from this
 * `sge.visui.widget` test under the `sge` package) suffices. A parent Group
 * (300x200) is added at the stage origin so its localToStageCoordinates(0,0)
 * == (0,0); a 100x40 actor is added to it; the Draggable has
 * setKeepWithinParent(true) and setBlockInput(false) (so the only extra actor
 * pushed onto the stage is the MimicActor, which the assertions locate by
 * type). The drag is driven by calling the public InputListener entry points
 * directly with hand-built InputEvents (touchDown at the actor's origin so
 * offsetX/offsetY == 0, then touchDragged toward the top-right corner), exactly
 * the dispatch Stage.touchDown/touchDragged perform — Draggable.touchDown/
 * touchDragged are the production code under test.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original VisUI semantics, not the port's.
 */
package sge
package visui
package widget

import lowlevel.Nullable
import sge.Input.Button
import sge.WorldUnits
import sge.graphics.{ Color, OrthographicCamera, Texture }
import sge.graphics.g2d.{ Batch, TextureRegion }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Affine2, Matrix4 }
import sge.scenes.scene2d.{ Actor, Group, InputEvent, Stage }
import sge.utils.Scaling
import sge.utils.viewport.ScalingViewport

class DraggableKeepWithinParentRedSuite extends munit.FunSuite {

  // --- Geometry -------------------------------------------------------------
  private val ParentWidth  = 300f
  private val ParentHeight = 200f
  private val ActorWidth   = 100f
  private val ActorHeight  = 40f

  // Drag the cursor toward the top-right corner, well inside the parent on the
  // cursor itself but close enough that the FULL actor (anchored at the cursor
  // because offset == 0) would overflow unless the clamp uses the actor size.
  private val DragStageX = 290f
  private val DragStageY = 190f

  /** Stub Batch: the Stage under test never renders (we only construct it and drive input dispatch), so every Batch method is a no-op / placeholder. A real SpriteBatch cannot be built headlessly —
    * its constructor binds an IndexBufferObject which throws "No buffer allocated!" under NoopGL20 — and the only Batch surface the drag path touches is the constructor. `begin`/`end` are
    * `private[sge]`, visible (and overridable) from this `sge.visui.widget` test which is under the `sge` package.
    */
  final private class StubBatch extends Batch {
    private val _color:  Color   = new Color(1f, 1f, 1f, 1f)
    private val _projMx: Matrix4 = new Matrix4()
    private val _tranMx: Matrix4 = new Matrix4()

    private def unused(member: String): Nothing =
      throw new IllegalStateException(s"StubBatch.$member must not be touched by the Draggable drag test")

    @scala.annotation.publicInBinary
    private[sge] def begin(): Unit = ()
    @scala.annotation.publicInBinary
    private[sge] def end(): Unit = ()

    def color_=(tint:              Color):                               Unit  = ()
    def setColor(r:                Float, g: Float, b: Float, a: Float): Unit  = ()
    def color:                                                           Color = _color
    def packedColor_=(packedColor: Float):                               Unit  = ()
    def packedColor:                                                     Float = 0f

    def draw(
      texture:   Texture,
      x:         Float,
      y:         Float,
      originX:   Float,
      originY:   Float,
      width:     Float,
      height:    Float,
      scaleX:    Float,
      scaleY:    Float,
      rotation:  Float,
      srcX:      Int,
      srcY:      Int,
      srcWidth:  Int,
      srcHeight: Int,
      flipX:     Boolean,
      flipY:     Boolean
    ):                                                                                                                                                                       Unit = ()
    def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX:   Int, srcY:      Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit = ()
    def draw(texture: Texture, x: Float, y: Float, srcX:  Int, srcY:     Int, srcWidth: Int, srcHeight: Int):                                                                Unit = ()
    def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u:      Float, v:       Float, u2:     Float, v2:      Float):                               Unit = ()
    def draw(texture: Texture, x: Float, y: Float):                                                                                                                          Unit = ()
    def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float):                                                                                             Unit = ()
    def draw(texture: Texture, spriteVertices: Array[Float], offset: Int, count:     Int):                                                                                               Unit = ()
    def draw(region:  TextureRegion, x:        Float, y:             Float):                                                                                                             Unit = ()
    def draw(region:  TextureRegion, x:        Float, y:             Float, width:   Float, height:  Float):                                                                             Unit = ()
    def draw(region:  TextureRegion, x:        Float, y:             Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = ()
    def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit = ()
    def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit = ()

    def flush():           Unit = ()
    def disableBlending(): Unit = ()
    def enableBlending():  Unit = ()

    def setBlendFunction(srcFunc:              Int, dstFunc:      Int):                                       Unit = ()
    def setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int): Unit = ()
    def blendSrcFunc:                                                                                         Int  = 0
    def blendDstFunc:                                                                                         Int  = 0
    def blendSrcFuncAlpha:                                                                                    Int  = 0
    def blendDstFuncAlpha:                                                                                    Int  = 0

    def projectionMatrix:                                        Matrix4       = _projMx
    def transformMatrix:                                         Matrix4       = _tranMx
    def projectionMatrix_=(projection: Matrix4):                 Unit          = ()
    def transformMatrix_=(transform:   Matrix4):                 Unit          = ()
    def shader_=(shader:               Nullable[ShaderProgram]): Unit          = ()
    def shader:                                                  ShaderProgram = unused("shader")
    def blendingEnabled:                                         Boolean       = true
    def drawing:                                                 Boolean       = false

    def close(): Unit = ()
  }

  private def headlessStage()(using Sge): Stage = {
    val viewport = new ScalingViewport(
      Scaling.stretch,
      WorldUnits(640f),
      WorldUnits(480f),
      new OrthographicCamera()
    )
    new Stage(viewport, new StubBatch())
  }

  /** Builds a stage with a parent Group at the origin (300x200) holding a 100x40 actor, then runs a keep-within-parent drag toward the top-right corner. Returns (stage, parentGroup, actor) for
    * assertions.
    */
  private def setupAndDrag()(using Sge): (Stage, Group, Actor) = {
    val stage = headlessStage()

    val parent = new Group()
    parent.setBounds(0f, 0f, ParentWidth, ParentHeight)
    stage.addActor(parent)

    val actor = new Actor()
    actor.setBounds(0f, 0f, ActorWidth, ActorHeight)
    parent.addActor(actor)

    val draggable = new Draggable()
    draggable.setKeepWithinParent(true)
    // Keep the stage clear of the input BLOCKER so the MimicActor is the only
    // extra child added to the root by attachMimic.
    draggable.setBlockInput(false)
    draggable.attachTo(actor)

    // touchDown at the actor's local origin (x = 0, y = 0) so the drag offset is
    // zero: the mimic is anchored exactly at the cursor's stage coordinates,
    // making the clamp depend solely on the mimic's reported size.
    val down = new InputEvent()
    down.eventType = InputEvent.Type.touchDown
    down.listenerActor = Nullable(actor)
    down.stageX = 0f
    down.stageY = 0f
    val started = draggable.touchDown(down, 0f, 0f, 0, Button(0))
    assert(started, "Draggable.touchDown must start the drag (actor is on the stage, listener approves)")
    assert(draggable.isDragged, "Draggable must report an active drag after touchDown")

    // Drag toward the top-right corner.
    val drag = new InputEvent()
    drag.eventType = InputEvent.Type.touchDragged
    drag.listenerActor = Nullable(actor)
    drag.stageX = DragStageX
    drag.stageY = DragStageY
    draggable.touchDragged(drag, 0f, 0f, 0)

    (stage, parent, actor)
  }

  /** Locates the single MimicActor that attachMimic added to the stage root. */
  private def mimicOf(stage: Stage): Draggable.MimicActor = {
    val actors = stage.actors
    var found: Nullable[Draggable.MimicActor] = Nullable.empty
    var i = 0
    while (i < actors.size) {
      actors(i) match {
        case m: Draggable.MimicActor => found = Nullable(m)
        case _ => ()
      }
      i += 1
    }
    found.getOrElse(fail("attachMimic must have added a MimicActor to the stage root"))
  }

  // --- ISS-532: clamp must keep the FULL actor within the parent ------------

  test("ISS-532: keep-within-parent clamp keeps the full actor's right/top edges inside the parent") {
    given Sge = VisUITestFixture.headlessSge()

    val (stage, _, actor) = setupAndDrag()
    val mimic             = mimicOf(stage)

    val parentEndX = ParentWidth // parent at origin -> parentEndX == ParentWidth
    val parentEndY = ParentHeight

    // Original VisUI clamp (Draggable.java lines 384-385 / 389-390) uses the
    // actor's live size, so the mimic's far edges must not pass the parent.
    assert(
      mimic.x + actor.width <= parentEndX + 0.001f,
      s"actor right edge ${mimic.x + actor.width} must stay within parentEndX $parentEndX " +
        s"(mimic.x=${mimic.x}, actor.width=${actor.width}); with mimic size 0 the clamp leaves mimic.x=$DragStageX so the actor overflows by ${mimic.x + actor.width - parentEndX}"
    )
    assert(
      mimic.y + actor.height <= parentEndY + 0.001f,
      s"actor top edge ${mimic.y + actor.height} must stay within parentEndY $parentEndY " +
        s"(mimic.y=${mimic.y}, actor.height=${actor.height}); with mimic size 0 the clamp leaves mimic.y=$DragStageY so the actor overflows by ${mimic.y + actor.height - parentEndY}"
    )
  }

  test("ISS-532: keep-within-parent clamps the mimic to exactly parentEnd - actorSize at the corner") {
    given Sge = VisUITestFixture.headlessSge()

    val (stage, _, _) = setupAndDrag()
    val mimic         = mimicOf(stage)

    // Original semantics: MIMIC.x = parentEndX - mimic.getWidth() = 300 - 100 = 200,
    //                     MIMIC.y = parentEndY - mimic.getHeight() = 200 - 40 = 160.
    assertEqualsFloat(
      mimic.x,
      ParentWidth - ActorWidth,
      0.001f,
      s"mimic.x must clamp to parentEndX - actorWidth = ${ParentWidth - ActorWidth}; with mimic.width 0 the bug leaves it at $DragStageX"
    )
    assertEqualsFloat(
      mimic.y,
      ParentHeight - ActorHeight,
      0.001f,
      s"mimic.y must clamp to parentEndY - actorHeight = ${ParentHeight - ActorHeight}; with mimic.height 0 the bug leaves it at $DragStageY"
    )
  }

  // --- ISS-532: the mimic must directly mirror the dragged actor's size -----

  test("ISS-532: the mimic's reported width/height must equal the dragged actor's during a drag") {
    given Sge = VisUITestFixture.headlessSge()

    val (stage, _, actor) = setupAndDrag()
    val mimic             = mimicOf(stage)

    // In the original, MimicActor.getWidth/getHeight delegate to the actor
    // (Draggable.java lines 535-543); the port must end up reporting the same
    // sizes (whether by delegation or by calling updateSize()). With the bug,
    // mimic.width and mimic.height are still 0.
    assertEqualsFloat(
      mimic.width,
      actor.width,
      0.001f,
      s"mimic.width must equal the dragged actor's width ${actor.width}; updateSize() is never called so it is ${mimic.width}"
    )
    assertEqualsFloat(
      mimic.height,
      actor.height,
      0.001f,
      s"mimic.height must equal the dragged actor's height ${actor.height}; updateSize() is never called so it is ${mimic.height}"
    )
  }

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float, clue: String)(using munit.Location): Unit =
    assert(Math.abs(actual - expected) <= delta, s"$clue (expected $expected +/- $delta but got $actual)")
}
