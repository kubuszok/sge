/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-668 (milestone 2/4 of the ISS-524 textra<->scene2d
 * integration): TextraButton and ImageTextraButton must be real scene2d
 * Buttons, not standalone classes.
 *
 * Original semantics (TextraTypist, upstream commit
 * 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4 — the commit the textra headers
 * reference; original-src/textratypist/src/main/java/com/github/tommyettinger/
 * textra/{TextraButton,ImageTextraButton}.java):
 *
 *   - TextraButton.java:33 `public class TextraButton extends Button` — i.e.
 *     com.badlogic.gdx.scenes.scene2d.ui.Button, which extends Table extends
 *     WidgetGroup extends Actor. So a TextraButton IS a Button and an Actor:
 *     it carries the inherited button state (isChecked/isPressed/isOver/
 *     isDisabled), can be toggled via the Button API, can join a ButtonGroup,
 *     and can be added to a Table / Stage like any other actor.
 *   - TextraButton.java:62-69 the canonical ctor calls `super()`, then
 *     `add(label).expand().fill()` — i.e. the TextraLabel is added to the
 *     Table backing the Button and gets a real Cell.
 *   - TextraButton.java:156-158 `public Cell<TextraLabel> getTextraLabelCell()
 *     { return getCell(label); }` — a REAL Cell whose actor is the label,
 *     NOT a placeholder.
 *   - TextraButton.java:37-60 five Skin/style ctor overloads exist in addition
 *     to the (text, TextButtonStyle, Font) canonical one: (text, Skin),
 *     (text, Skin, styleName), (text, TextButtonStyle) [style-only],
 *     (text, Skin, Font), (text, Skin, styleName, Font). (folds ISS-648.)
 *   - ImageTextraButton.java:36 `public class ImageTextraButton extends Button`
 *     (NOT ImageTextButton — upstream extends Button directly and adds its own
 *     image cell + TextraLabel; corrected by orchestrator after the original
 *     reconnaissance/red mis-cited ImageTextButton).
 *
 * The port at the red commit declares both as standalone classes with no
 * scene2d base (TextraButton.scala:39 `class TextraButton(...)(using Sge) {`,
 * ImageTextraButton.scala:36 `class ImageTextraButton(...)(using Sge) {`),
 * re-creating button state by hand as private fields and stubbing
 * getTextraLabelCell as `Nullable.empty` (TextraButton.scala:139). The five
 * Skin/style overloads are absent (only (text, style, replacementFont) and
 * (text, style) exist, TextraButton.scala:39/70).
 *
 * What these tests pin (each grounded in real scene2d types; trivially-green
 * stubs are avoided):
 *   1. is-a Button / Actor (compile-level): a TextraButton value is usable
 *      where a sge.scenes.scene2d.ui.Button and a sge.scenes.scene2d.Actor
 *      are required; an ImageTextraButton where a Button is (and is NOT an
 *      ImageTextButton — upstream :36 extends Button directly). This
 *      does NOT compile at the red commit (standalone classes) — a
 *      compile-failure RED, the strongest possible proof the type
 *      relationship is absent.
 *   2. real Cell: getTextraLabelCell returns a real
 *      sge.scenes.scene2d.ui.Cell whose actor is the button's TextraLabel,
 *      not Nullable.empty / AnyRef (TextraButton.java:156 getCell(label)).
 *   3. inherited button state: the button participates as a real Button —
 *      toggling via the inherited Button.setChecked/isChecked API, joining a
 *      ButtonGroup (a behaviour only a real Button has), and being added to a
 *      real (headless) Stage.
 *   4. ISS-648 Skin ctors: each of the five missing overloads constructs.
 *
 * Harness for the staged button: the proven GL-free pattern from milestone-1's
 * TextraLabelWidgetRedSuite — a real Stage built with the 2-arg
 * Stage(viewport, batch) constructor over a no-op StubBatch (a real
 * SpriteBatch cannot be built headlessly: its constructor binds an
 * IndexBufferObject that throws "No buffer allocated!" under NoopGL20). The
 * stage never renders here, so the only Batch surface touched is the
 * constructor. This suite lives under the `sge` package so the
 * `private[sge]` begin/end on Batch are overridable.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original TextraTypist semantics (TextraButton IS
 * a scene2d Button), not the port's standalone shape.
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
import sge.scenes.scene2d.{ Actor, Stage }
import sge.scenes.scene2d.ui.{ Button, ButtonGroup, Cell, ImageTextButton, Table }
import sge.utils.Scaling
import sge.utils.viewport.ScalingViewport

class TextraButtonScene2dRedSuite extends munit.FunSuite {

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
      throw new IllegalStateException(s"StubBatch.$member must not be touched by the TextraButton test")

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

  /** A minimal textra TextButtonStyle with a font, sufficient to construct a TextraButton. */
  private def newStyle()(using Sge): Styles.TextButtonStyle = {
    val style = new Styles.TextButtonStyle()
    style.font = Nullable(new Font())
    style
  }

  // --- 1. is-a Button / Actor (compile-level) ------------------------------

  test("ISS-668: a TextraButton is a scene2d.ui.Button and an Actor (TextraButton.java:33 `extends Button`)") {
    given Sge  = headlessSge()
    val button = new TextraButton(Nullable("ok"), newStyle())

    // These widenings only compile if TextraButton extends Button (which
    // extends Table extends Actor). At the red commit TextraButton is a
    // standalone class, so this suite does not compile — a compile-failure RED.
    val asButton: Button = button
    val asActor:  Actor  = button
    assert(asButton eq button, "a TextraButton value must BE a Button, not wrap one")
    assert(asActor eq button, "a TextraButton value must BE an Actor, not wrap one")
  }

  test(
    "ISS-668: an ImageTextraButton is a scene2d.ui.Button, NOT an ImageTextButton (ImageTextraButton.java:36 `extends Button`)"
  ) {
    given Sge      = headlessSge()
    val imageStyle = new Styles.ImageTextButtonStyle()
    imageStyle.font = Nullable(new Font())
    val button = new ImageTextraButton(Nullable("ok"), imageStyle)

    val asButton: Button = button
    assert(asButton eq button, "an ImageTextraButton value must BE a Button (upstream :36 extends Button), not wrap one")
    // Upstream extends Button directly (it manages its own image cell + TextraLabel),
    // so the port must NOT extend ImageTextButton — that would drag in unused inherited
    // image/label machinery and a glyph-less-font workaround. Pin the faithful base
    // reflectively: a static isInstanceOf[ImageTextButton] is rejected by
    // -Wimplausible-patterns once the two are unrelated sibling subclasses of Button.
    assertEquals(
      classOf[ImageTextraButton].getSuperclass,
      classOf[Button],
      "ImageTextraButton must extend Button DIRECTLY (upstream :36), not ImageTextButton"
    )
    assert(
      !classOf[ImageTextButton].isAssignableFrom(button.getClass),
      "ImageTextraButton must NOT be an ImageTextButton (upstream :36 extends Button)"
    )
  }

  // --- 2. real Cell --------------------------------------------------------

  test(
    "ISS-668: getTextraLabelCell returns a real Cell whose actor is the TextraLabel (TextraButton.java:156 getCell(label))"
  ) {
    given Sge  = headlessSge()
    val button = new TextraButton(Nullable("hi"), newStyle())

    // At the red commit this returns Nullable[AnyRef] = Nullable.empty, so the
    // typed binding below does not compile (and, were it to, isEmpty holds).
    val cell: Cell[TextraLabel] = button.getTextraLabelCell
    assert(cell ne null, "getTextraLabelCell must return a real Cell, not null")
    assert(
      cell.getActor.exists(_ eq button.getTextraLabel),
      "the cell's actor must be the button's TextraLabel (label was add(label) into the Table backing)"
    )
  }

  // --- 3. inherited button state -------------------------------------------

  test("ISS-668: a TextraButton toggles via the inherited Button API and can be added to a Table") {
    given Sge  = headlessSge()
    val button = new TextraButton(Nullable("toggle"), newStyle())

    assert(!button.isChecked, "a fresh button is unchecked")
    button.setChecked(true)
    assert(button.isChecked, "setChecked(true) via the inherited Button API checks it")

    // toggle() is a Button-only method: it flips isChecked. A standalone class
    // with hand-rolled state has no toggle().
    button.toggle()
    assert(!button.isChecked, "toggle() (Button API) flips the checked state back")

    // The button is a real Actor and can be nested in a Table.
    val table = new Table()
    table.add(Nullable[Actor](button))
    assert(table.getCells.size == 1, "the button was added to the table as a real actor cell")
    assert(table.getCells(0).getActor.exists(_ eq button), "the table cell's actor is the button")
  }

  test("ISS-668: a TextraButton joins a ButtonGroup and is tracked as the checked member (Button-only behaviour)") {
    given Sge = headlessSge()
    val a     = new TextraButton(Nullable("a"), newStyle())
    val b     = new TextraButton(Nullable("b"), newStyle())

    val group = new ButtonGroup[Button]()
    group.add(a)
    group.add(b)

    a.setChecked(true)
    assert(group.checked.exists(_ eq a), "the ButtonGroup reports the checked TextraButton (only possible if it IS a Button)")

    b.setChecked(true)
    assert(group.checked.exists(_ eq b), "checking another member moves the group's checked button to it")
  }

  test("ISS-668: a TextraButton can be added to a real (headless) Stage as an actor") {
    given Sge  = headlessSge()
    val stage  = headlessStage()
    val button = new TextraButton(Nullable("staged"), newStyle())

    assert(button.stage.isEmpty, "before staging the button has no stage")
    stage.addActor(button)
    assert(button.stage.exists(_ eq stage), "once added the button's stage is the Stage it was added to (Actor.setStage)")
  }

  // --- 4. ISS-648 Skin/style ctor overloads --------------------------------

  test("ISS-648/ISS-668: the five missing TextraButton Skin/style ctor overloads construct (TextraButton.java:37-60)") {
    given Sge = headlessSge()

    val skin  = new sge.scenes.scene2d.ui.Skin()
    val style = newStyle()
    skin.add("default", style)
    skin.add("custom", newStyle())
    val font = new Font()

    // (text, Skin)
    val b1: Button = new TextraButton(Nullable("a"), skin)
    // (text, Skin, styleName)
    val b2: Button = new TextraButton(Nullable("b"), skin, "custom")
    // (text, TextButtonStyle) — style only, no replacement font argument
    val b3: Button = new TextraButton(Nullable("c"), style)
    // (text, Skin, Font)
    val b4: Button = new TextraButton(Nullable("d"), skin, font)
    // (text, Skin, styleName, Font)
    val b5: Button = new TextraButton(Nullable("e"), skin, "custom", font)

    assert(List(b1, b2, b3, b4, b5).forall(_ ne null), "all five overloads must construct a real Button")
  }
}
