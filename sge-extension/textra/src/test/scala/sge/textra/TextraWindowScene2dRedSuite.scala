/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-669 (milestone 3/4 of the ISS-524 textra<->scene2d
 * integration): TextraWindow must be a real scene2d Table, and TextraDialog
 * must use real scene2d Actions + listeners + Cell-backed content/button
 * tables, not standalone placeholders.
 *
 * Original semantics (TextraTypist, upstream commit
 * 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4 — the commit the textra headers
 * reference; original-src/textratypist/src/main/java/com/github/tommyettinger/
 * textra/{TextraWindow,TextraDialog,TypingWindow,TypingDialog}.java):
 *
 *   - TextraWindow.java:39 `public class TextraWindow extends Table` — i.e.
 *     com.badlogic.gdx.scenes.scene2d.ui.Table, which extends WidgetGroup
 *     extends Actor. So a TextraWindow IS a Table and an Actor: it can join a
 *     Stage, carries the real Actor action/listener infrastructure, and lays
 *     out children in real Cells.
 *   - TypingWindow.java `extends TextraWindow`, TextraDialog.java
 *     `extends TextraWindow`, TypingDialog.java `extends TypingWindow` — so all
 *     four inherit the Table/Actor identity transitively.
 *   - TextraWindow.java:148/:154/:157 the ctor installs the title-bar drag /
 *     resize machinery via `addCaptureListener(new InputListener(){...})` whose
 *     handler delegates to an `InternalListener extends InputListener`. So a
 *     real TextraWindow's captureListeners contain a real InputListener.
 *   - TextraDialog.java:35/:36 `import static ...Actions.fadeOut/sequence`;
 *     :347-361 `hide(Action)` -> `addAction(sequence(action, removeListener(...),
 *     removeActor()))`; :370-371 `hide()` -> `hide(fadeOut(0.4f, fade))`. So
 *     calling hide() schedules a REAL scene2d Action on the dialog (hasActions
 *     becomes true; the queued action is a real Action subtype), NOT a
 *     "fadeOut:0.4" String.
 *   - TextraDialog.java:53 `protected InputListener ignoreTouchDown = new
 *     InputListener(){...}` and :360 `addCaptureListener(ignoreTouchDown)` on
 *     hide(action) — a REAL InputListener registered as a capture listener.
 *   - TextraDialog.java:137 `focusListener = new FocusListener(){...}` and
 *     :159-164 `setStage` adds/removes it via addListener/removeListener — a
 *     REAL com.badlogic.gdx.scenes.scene2d.utils.FocusListener.
 *   - TextraDialog content/button tables are real scene2d Tables: text(...) and
 *     button(...) call contentTable.add(...)/buttonTable.add(...) producing real
 *     Cells (Table.add returns Cell<T>).
 *
 * The port at the red commit declares TextraWindow as a standalone class with
 * NO scene2d base (TextraWindow.scala:41 `class TextraWindow(...)(using Sge) {`),
 * hand-rolling x/y/width/height + a no-op invalidateHierarchy
 * (TextraWindow.scala:438), a no-op keepWithinStage() (TextraWindow.scala:444),
 * an inner `InternalListener` that extends nothing (TextraWindow.scala:162),
 * and a `TitleTable` that is not a Table (TextraWindow.scala:531). TextraDialog
 * tracks listeners/actions in `mutable.ArrayBuffer[AnyRef]`
 * (TextraDialog.scala:64-67), uses MARKER objects for the focus/ignoreTouchDown
 * listeners (TextraDialog.scala:70-73, 442-443), schedules FAKE action Strings
 * `"fadeOut:0.4"` / `s"fadeOut:$durationSeconds"` on hide
 * (TextraDialog.scala:356/363), and backs content/button by lightweight
 * non-Table holders (TextraDialog.scala:445-465).
 *
 * What these tests pin (each grounded in real scene2d types; trivially-green
 * stubs are avoided):
 *   1. is-a Table / Actor (compile-level): a TextraWindow value is usable where
 *      a sge.scenes.scene2d.ui.Table and a sge.scenes.scene2d.Actor are
 *      required; likewise TypingWindow, TextraDialog, TypingDialog (they
 *      inherit). This does NOT compile at the red commit (standalone classes) —
 *      a compile-failure RED, the strongest possible proof the type
 *      relationship is absent.
 *   2. joins the scene graph: a TextraWindow added to a real (headless) Stage
 *      reports that stage via the inherited Actor.stage (real Actor, not no-op).
 *   3. real Actions on hide: a TextraDialog shown on a Stage then hide()-d has
 *      a real queued scene2d Action (hasActions true; the queued value is a
 *      real Action), NOT a "fadeOut:0.4" String (TextraDialog.java:347-371).
 *   4. real listeners: the window's captureListeners contain a real
 *      InputListener (the drag listener; TextraWindow.java:148), and the
 *      dialog's ignoreTouchDown registered on hide is a real InputListener
 *      while its focusListener is a real FocusListener (TextraDialog.java:53/137).
 *   5. real content/button Cells: TextraDialog's content/button tables are real
 *      scene2d Tables, and text(...)/button(...) add real Cells (assert via
 *      getCells).
 *
 * Harness: the proven GL-free pattern from milestone-1/2's red suites — a real
 * Stage built with the 2-arg Stage(viewport, batch) constructor over a no-op
 * StubBatch (a real SpriteBatch cannot be built headlessly: its constructor
 * binds an IndexBufferObject that throws "No buffer allocated!" under
 * NoopGL20). The stage never renders here, so the only Batch surface touched is
 * the constructor. This suite lives under the `sge` package so the
 * `private[sge]` begin/end on Batch are overridable.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original TextraTypist semantics (TextraWindow IS a
 * scene2d Table; TextraDialog uses real Actions/listeners/Cells), not the
 * port's standalone shape.
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
import sge.scenes.scene2d.{ Action, Actor, InputListener, Stage }
import sge.scenes.scene2d.ui.{ Cell, Table }
import sge.scenes.scene2d.utils.FocusListener
import sge.utils.Scaling
import sge.utils.viewport.ScalingViewport

class TextraWindowScene2dRedSuite extends munit.FunSuite {

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
      throw new IllegalStateException(s"StubBatch.$member must not be touched by the TextraWindow test")

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

  /** A minimal textra WindowStyle with a title font, sufficient to construct a TextraWindow/TextraDialog. */
  private def newWindowStyle()(using Sge): Styles.WindowStyle = {
    val style = new Styles.WindowStyle()
    style.titleFont = Nullable(new Font())
    style
  }

  /** A Skin wired with the typed defaults a TextraDialog needs for text(...)/button(...). */
  private def newSkin()(using Sge): sge.scenes.scene2d.ui.Skin = {
    val skin = new sge.scenes.scene2d.ui.Skin()
    skin.add("default", newWindowStyle(), classOf[Styles.WindowStyle])
    val labelStyle = new Styles.LabelStyle()
    labelStyle.font = Nullable(new Font())
    skin.add("default", labelStyle, classOf[Styles.LabelStyle])
    val buttonStyle = new Styles.TextButtonStyle()
    buttonStyle.font = Nullable(new Font())
    skin.add("default", buttonStyle, classOf[Styles.TextButtonStyle])
    skin
  }

  // --- 1. is-a Table / Actor (compile-level) -------------------------------

  test("ISS-669: a TextraWindow is a scene2d.ui.Table and an Actor (TextraWindow.java:39 `extends Table`)") {
    given Sge  = headlessSge()
    val window = new TextraWindow("title", newWindowStyle())

    // These widenings only compile if TextraWindow extends Table (which extends
    // WidgetGroup extends Actor). At the red commit TextraWindow is a standalone
    // class, so this suite does not compile — a compile-failure RED.
    val asTable: Table = window
    val asActor: Actor = window
    assert(asTable eq window, "a TextraWindow value must BE a Table, not wrap one")
    assert(asActor eq window, "a TextraWindow value must BE an Actor, not wrap one")
  }

  test("ISS-669: TypingWindow, TextraDialog, TypingDialog inherit the Table/Actor identity") {
    given Sge   = headlessSge()
    val style   = newWindowStyle()
    val typingW = new TypingWindow("tw", style)
    val dialog  = new TextraDialog("td", style)
    val typingD = new TypingDialog("tyd", style)

    val tw: Table = typingW
    val td: Table = dialog
    val ty: Table = typingD
    assert(tw eq typingW, "a TypingWindow must BE a Table (it extends TextraWindow extends Table)")
    assert(td eq dialog, "a TextraDialog must BE a Table (it extends TextraWindow extends Table)")
    assert(ty eq typingD, "a TypingDialog must BE a Table (it extends TypingWindow extends Table)")

    val twActor: Actor = typingW
    val tdActor: Actor = dialog
    val tyActor: Actor = typingD
    assert((twActor eq typingW) && (tdActor eq dialog) && (tyActor eq typingD), "all three must BE Actors")
  }

  // --- 2. joins the scene graph --------------------------------------------

  test("ISS-669: a TextraWindow can be added to a real (headless) Stage and reports that stage (Actor.setStage)") {
    given Sge  = headlessSge()
    val stage  = headlessStage()
    val window = new TextraWindow("staged", newWindowStyle())

    assert(window.stage.isEmpty, "before staging the window has no stage")
    stage.addActor(window)
    assert(window.stage.exists(_ eq stage), "once added the window's stage is the Stage it was added to (real Actor, not a no-op)")
  }

  // --- 3. real Actions on hide ---------------------------------------------

  test(
    "ISS-669: hide() schedules a REAL scene2d Action on a TextraDialog, not a \"fadeOut:0.4\" String (TextraDialog.java:347-371)"
  ) {
    given Sge  = headlessSge()
    val stage  = headlessStage()
    val dialog = new TextraDialog("d", newSkin())

    // Upstream show(Stage) queues a REAL fadeIn Action
    // (TextraDialog.java:335 show(stage, sequence(alpha(0), fadeIn(0.4f)))).
    dialog.show(stage)
    assert(dialog.hasActions, "show(stage) must queue a real scene2d fadeIn Action (upstream :335), not nothing/a placeholder")
    val shown: Action = dialog.actions(0)
    assert(
      shown.getClass.getName.startsWith("sge.scenes.scene2d.actions"),
      "show(stage)'s queued value must be a real scene2d Action, not a \"fadeIn\" String"
    )

    dialog.hide()

    // Upstream hide() -> hide(fadeOut(0.4f, fade)) -> addAction(sequence(...))
    // (TextraDialog.java:371/361). So a REAL scene2d Action is queued on the dialog
    // (the inherited Actor action infrastructure), NOT a "fadeOut:0.4" String stored
    // in an AnyRef buffer (the standalone placeholder at the red commit).
    assert(dialog.hasActions, "hide() must queue a real scene2d Action on the dialog (Actor.hasActions)")
    val queued: Action = dialog.actions(dialog.actions.size - 1)
    assert(
      queued.getClass.getName.startsWith("sge.scenes.scene2d.actions"),
      "hide() must queue a real scene2d Action (a sequence/fade), not a placeholder"
    )
  }

  // --- 4. real listeners ---------------------------------------------------

  test(
    "ISS-669: a TextraWindow registers a real InputListener as a capture listener (the drag listener, TextraWindow.java:148)"
  ) {
    given Sge  = headlessSge()
    val window = new TextraWindow("drag", newWindowStyle())

    // Upstream installs the title-bar drag/resize machinery via
    // addCaptureListener(new InputListener(){...}). The standalone port has an
    // inner InternalListener that extends nothing and is never registered with
    // any Actor listener list.
    val caps = window.captureListeners
    assert(
      caps.exists(_.isInstanceOf[InputListener]),
      "the window's capture listeners must include a real scene2d InputListener (the drag listener)"
    )
  }

  test(
    "ISS-669: TextraDialog registers a real InputListener (ignoreTouchDown) as a capture listener on hide (TextraDialog.java:53/360)"
  ) {
    given Sge  = headlessSge()
    val stage  = headlessStage()
    val dialog = new TextraDialog("d", newSkin())

    // Upstream hide(action) calls addCaptureListener(ignoreTouchDown) where
    // ignoreTouchDown is `new InputListener(){...}` (TextraDialog.java:53). The
    // standalone port stores an opaque marker AnyRef in an ArrayBuffer instead.
    // Asserted via the inherited, public Actor.captureListeners (a real
    // DynamicArray[EventListener]) — present only once TextraDialog is an Actor.
    dialog.show(stage)
    // Upstream wires a real FocusListener when the dialog is shown/attached to the
    // stage (TextraDialog.java:137/161), and hide() REMOVES it (TextraDialog.java:350).
    // Pin both timings: a real FocusListener (not a marker AnyRef) is present after
    // show, and gone after hide.
    assert(
      dialog.listeners.exists(_.isInstanceOf[FocusListener]),
      "show() must wire a real scene2d FocusListener (TextraDialog.java:137/161), not a marker AnyRef"
    )

    dialog.hide()
    assert(
      dialog.captureListeners.exists(_.isInstanceOf[InputListener]),
      "hide() must register a real scene2d InputListener (ignoreTouchDown) as a capture listener (TextraDialog.java:360)"
    )
    assert(
      !dialog.listeners.exists(_.isInstanceOf[FocusListener]),
      "hide() must REMOVE the FocusListener (TextraDialog.java:350)"
    )
  }

  // --- 5. real content/button Cells ----------------------------------------

  test("ISS-669: TextraDialog content/button tables are real Tables and text(...)/button(...) add real Cells") {
    given Sge  = headlessSge()
    val dialog = new TextraDialog("d", newSkin())

    val content: Table = dialog.getContentTable
    val buttons: Table = dialog.getButtonTable

    dialog.text(Nullable("hello"))
    dialog.button(Nullable("ok"))

    assert(content.getCells.size >= 1, "text(...) must add a real Cell to the content Table")
    assert(buttons.getCells.size >= 1, "button(...) must add a real Cell to the button Table")

    val contentCell: Cell[?] = content.getCells(0)
    val buttonCell:  Cell[?] = buttons.getCells(0)
    assert(contentCell.getActor.isDefined, "the content cell holds a real actor (the added label)")
    assert(buttonCell.getActor.isDefined, "the button cell holds a real actor (the added button)")
  }
}
