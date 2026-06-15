/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-667 (milestone 1/4 of the ISS-524 textra<->scene2d
 * integration): TextraLabel must be a real scene2d.ui Widget, not a
 * standalone class.
 *
 * Original semantics (TextraTypist, upstream commit
 * 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4 — the commit the textra headers
 * reference; original-src/textratypist/src/main/java/com/github/tommyettinger/
 * textra/TextraLabel.java):
 *
 *   - TextraLabel.java:52 `public class TextraLabel extends Widget` —
 *     i.e. com.badlogic.gdx.scenes.scene2d.ui.Widget, which extends
 *     com.badlogic.gdx.scenes.scene2d.Actor. So a TextraLabel IS an Actor
 *     and participates in the scene graph: adding it to a Group makes the
 *     Group its parent (Actor.setParent, called from Group.addActor), and
 *     when the Group is on a Stage the label's stage is that Stage
 *     (Actor.setStage, called from Group.addActor / Group.setStage
 *     recursion). Removing it clears both.
 *   - TextraLabel.java:218-220 setStage(Stage) and :228-230 setParent(Group)
 *     in the Java source are *overrides* of Actor's framework hooks — they
 *     widen the visibility to public but still run the inherited bookkeeping
 *     (super is the scene2d Actor). They are NOT no-ops.
 *   - TypingLabel.java:71 `public class TypingLabel extends TextraLabel`, so
 *     a TypingLabel is transitively a Widget and an Actor too.
 *
 * The port (sge-extension/textra/src/main/scala/sge/textra/TextraLabel.scala
 * at the red commit) declares `class TextraLabel(using Sge)` with NO scene2d
 * base (Migration notes line 9: "Widget -> standalone class"). It re-creates
 * Widget-like fields by hand (TextraLabel.scala:60-99) and stubs the two
 * framework hooks as no-ops:
 *   - setStage(stage: AnyRef): Unit = ()           (TextraLabel.scala:211)
 *   - setParent(parent: AnyRef): Unit = ()         (TextraLabel.scala:218)
 * Because TextraLabel is not a sge.scenes.scene2d.Actor, it cannot be added
 * to a Group, has no `parent` / `stage` tracking, and the no-op setStage
 * never records anything. TypingLabel (TypingLabel.scala:51 extends
 * TextraLabel) inherits the same standalone shape.
 *
 * What these tests pin (each grounded in real scene2d types; trivially-green
 * stubs are avoided):
 *   1. is-a Widget / Actor (compile-level): a TextraLabel value is usable
 *      where a sge.scenes.scene2d.ui.Widget and a sge.scenes.scene2d.Actor
 *      are required. This does NOT compile at the red commit (standalone
 *      class) — a compile-failure RED, which is the strongest possible
 *      proof the type relationship is absent.
 *   2. parent tracking: Group.addActor(label) sets label.parent to the
 *      group (real Actor bookkeeping, not the no-op setParent);
 *      group.removeActor(label) clears it.
 *   3. stage tracking: with the label's group on a real (headless) Stage,
 *      label.stage is that Stage; removal clears it. This exercises the
 *      real setStage seam (Group.addActor propagates stage to children,
 *      Group.scala:281 / setStage recursion Group.scala:412), proving
 *      setStage is NOT a no-op.
 *   4. TypingLabel inherits the Widget/Actor chain.
 *
 * Harness for stage tracking: the proven GL-free pattern from
 * sge.visui.widget.DraggableKeepWithinParentRedSuite — a real Stage built
 * with the 2-arg Stage(viewport, batch) constructor over a no-op StubBatch
 * (a real SpriteBatch cannot be built headlessly: its constructor binds an
 * IndexBufferObject that throws "No buffer allocated!" under NoopGL20). The
 * stage never renders here, so the only Batch surface touched is the
 * constructor. This suite lives under the `sge` package so the
 * `private[sge]` begin/end on Batch are overridable.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified
 * by the fixer: they encode the original TextraTypist semantics (TextraLabel
 * is a scene2d Widget), not the port's standalone shape.
 */
package sge
package textra

import lowlevel.Nullable

import sge.WorldUnits
import sge.graphics.{ Color, OrthographicCamera, Texture }
import sge.graphics.g2d.{ Batch, TextureRegion }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Affine2, Matrix4 }
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.files.{ FileHandle, FileType }
import sge.scenes.scene2d.{ Actor, Group, Stage }
import sge.scenes.scene2d.ui.Widget
import sge.utils.Scaling
import sge.utils.viewport.ScalingViewport

class TextraLabelWidgetRedSuite extends munit.FunSuite {

  // --- Headless Sge fixture (NoopGraphics gives width/height with no GL) ---

  private object StubApplication extends Application {
    def applicationListener:                                  ApplicationListener         = throw new UnsupportedOperationException
    def graphics:                                             Graphics                    = throw new UnsupportedOperationException
    def audio:                                                Audio                       = throw new UnsupportedOperationException
    def input:                                                Input                       = throw new UnsupportedOperationException
    def files:                                                Files                       = throw new UnsupportedOperationException
    def net:                                                  Net                         = throw new UnsupportedOperationException
    def applicationType:                                      Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
    def version:                                              Int                         = 0
    def javaHeap:                                             Long                        = 0L
    def nativeHeap:                                           Long                        = 0L
    def getPreferences(name:              String):            Preferences                 = throw new UnsupportedOperationException
    def clipboard:                                            sge.utils.Clipboard         = throw new UnsupportedOperationException
    def postRunnable(runnable:            Runnable):          Unit                        = ()
    def exit():                                               Unit                        = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
  }

  private object NoNet extends Net {
    import Net.*
    def httpClient:                                                                                         net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: sge.net.ServerSocketHints): net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   sge.net.ServerSocketHints):             net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: sge.net.SocketHints):       net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                                  Boolean           = false
  }

  private object NoFiles extends Files {
    def getFileHandle(path: String, fileType: FileType): FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                     FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                     FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                             String     = ""
    def isExternalStorageAvailable:                      Boolean    = false
    def localStoragePath:                                String     = ""
    def isLocalStorageAvailable:                         Boolean    = false
  }

  private def headlessSge(): Sge =
    Sge(StubApplication, new NoopGraphics(), new NoopAudio(), NoFiles, new NoopInput(), NoNet)

  /** Stub Batch: the Stage under test never renders here (it is only constructed and used as a scene-graph host), so every Batch method is a no-op / placeholder. A real SpriteBatch cannot be built
    * headlessly — its constructor binds an IndexBufferObject which throws "No buffer allocated!" under NoopGL20. `begin`/`end` are `private[sge]`, visible (and overridable) from this `sge.textra`
    * test which is under the `sge` package.
    */
  final private class StubBatch extends Batch {
    private val _color:  Color   = new Color(1f, 1f, 1f, 1f)
    private val _projMx: Matrix4 = new Matrix4()
    private val _tranMx: Matrix4 = new Matrix4()

    private def unused(member: String): Nothing =
      throw new IllegalStateException(s"StubBatch.$member must not be touched by the TextraLabel widget test")

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

  // --- 1. is-a Widget / Actor (compile-level) ------------------------------

  test("ISS-667: a TextraLabel is a scene2d.ui.Widget and an Actor (TextraLabel.java:52 `extends Widget`)") {
    given Sge = headlessSge()
    val label = new TextraLabel()

    // These widenings only compile if TextraLabel extends Widget (which
    // extends Actor). At the red commit TextraLabel is a standalone class,
    // so this suite does not compile — a compile-failure RED.
    val asWidget: Widget = label
    val asActor:  Actor  = label
    assert(asWidget eq label, "a TextraLabel value must BE a Widget, not wrap one")
    assert(asActor eq label, "a TextraLabel value must BE an Actor, not wrap one")
  }

  // --- 2. parent tracking --------------------------------------------------

  test(
    "ISS-667: Group.addActor sets the TextraLabel's parent; removeActor clears it (real Actor bookkeeping, not the no-op setParent)"
  ) {
    given Sge = headlessSge()
    val group = new Group()
    val label = new TextraLabel()

    assert(label.parent.isEmpty, "a fresh TextraLabel has no parent")

    group.addActor(label)
    assert(label.parent.isDefined, "after addActor the label has a parent")
    assert(label.parent.exists(_ eq group), "the parent is the group it was added to (Group.addActor -> Actor.setParent)")
    assertEquals(group.children.size, 1)

    val removed = group.removeActor(label)
    assert(removed, "removeActor reports the label was present")
    assert(label.parent.isEmpty, "after removeActor the parent is cleared (the no-op setParent could never do this)")
    assertEquals(group.children.size, 0)
  }

  // --- 3. stage tracking ---------------------------------------------------

  test(
    "ISS-667: a TextraLabel on a staged Group reports that Stage; removal clears it (real setStage seam, TextraLabel.scala:211 no-op cannot)"
  ) {
    given sge: Sge = headlessSge()
    val stage = headlessStage()
    val group = new Group()
    val label = new TextraLabel()

    group.addActor(label)
    assert(label.stage.isEmpty, "before the group is staged the label has no stage")

    stage.addActor(group)
    assert(label.stage.isDefined, "once the group is on the stage, the label's stage propagates down (Group.setStage recursion)")
    assert(label.stage.exists(_ eq stage), "the label's stage is exactly the Stage hosting its group")

    val removed = group.removeActor(label)
    assert(removed)
    assert(label.stage.isEmpty, "removing the label from the group clears its stage (the no-op setStage could never do this)")
  }

  // --- 4. TypingLabel inherits the Widget/Actor chain ----------------------

  test("ISS-667: a TypingLabel is also a Widget and an Actor (TypingLabel.java:71 `extends TextraLabel`)") {
    given Sge  = headlessSge()
    val typing = new TypingLabel()

    val asWidget: Widget = typing
    val asActor:  Actor  = typing
    assert(asWidget eq typing, "a TypingLabel value must BE a Widget")
    assert(asActor eq typing, "a TypingLabel value must BE an Actor")

    // And it participates in the scene graph like any other actor.
    val group = new Group()
    group.addActor(typing)
    assert(typing.parent.exists(_ eq group), "a TypingLabel added to a group gets that group as parent")
  }
}
