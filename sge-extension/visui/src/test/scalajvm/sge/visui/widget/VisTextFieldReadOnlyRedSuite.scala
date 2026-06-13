/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-531 (VisTextField.setReadOnly / setEnterKeyFocusTraversal
 * store flags that NOTHING reads — the input path never consults them).
 *
 * In the original VisUI (original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/
 * ui/widget/VisTextField.java, upstream commit
 * 820300c86a1bd907404217195a9987e5c66d2220) the two flags are wired into the
 * TextFieldClickListener that VisTextField installs:
 *
 *   - readOnly (VisTextField.java field at line 122) gates every text-mutating
 *     path while leaving reading intact:
 *       * keyTyped (line 1193): `if (disabled || readOnly) return false;`
 *         — a read-only field rejects ALL typed characters.
 *       * keyDown Ctrl+V paste (line 1084): `keycode == Keys.V && readOnly == false`
 *       * keyDown Ctrl+X cut   (line 1092): `keycode == Keys.X && readOnly == false`
 *       * keyDown Ctrl+Z undo  (line 1100): `keycode == Keys.Z && readOnly == false`
 *       * keyDown Shift+Insert paste (line 1113) / Shift+Del cut (line 1114)
 *       * BUT Ctrl+C copy (line 1088) and Ctrl+A selectAll (line 1096) are
 *         NOT gated — read-only must still allow selecting and copying.
 *   - enterKeyFocusTraversal (VisTextField.java field at line 91) changes how
 *     the Enter key is handled in keyTyped (line 1211):
 *       `if (focusTraversal && (character == TAB ||
 *            (character == ENTER_ANDROID && enterKeyFocusTraversal))) next(...)`
 *     i.e. when enterKeyFocusTraversal is true, pressing Enter moves keyboard
 *     focus to the next focus-traversable text field (via next()) instead of
 *     being handled as a normal character.
 *
 * The SGE port (sge-extension/visui/src/main/scala/sge/visui/widget/
 * VisTextField.scala) extends core sge TextField but overrides NEITHER
 * keyTyped NOR keyDown. _readOnly (line 47/127-128) and _enterKeyFocusTraversal
 * (line 50/146-149) are dead fields: core TextField.keyTyped (TextField.scala
 * line 1023) only guards on `disabled`, never readOnly, and core
 * checkFocusTraversal (TextField.scala line 1019) consults _focusTraversal +
 * TAB / (Android|iOS Enter) — never _enterKeyFocusTraversal. So on the current
 * code:
 *   - a read-only VisTextField still accepts typed characters and pastes;
 *   - Enter on a single-line field with enterKeyFocusTraversal=true does NOT
 *     move focus to the next field.
 *
 * These tests drive the REAL input path: a Stage routes keyTyped/keyDown to the
 * focused VisTextField's installed input listener (Stage.keyTyped/keyDown ->
 * keyboardFocus.fire -> TextFieldClickListener), exactly as a running app does.
 * They encode the ORIGINAL VisUI semantics and MUST NOT be edited by the fixer.
 *
 * Harness: see ReadOnlyFixture below. Reuses the VisUITestFixture skin-loading
 * approach (real DesktopFiles + no-op GL) so VisUI.load() yields a real skin
 * with a real BitmapFont (default-font); the GL additionally fakes shader
 * compile/link status so a real Stage + SpriteBatch construct headlessly
 * (mirroring the ShaderCompilingGL20 pattern in
 * sge/src/test/scalajvm/sge/graphics/g3d/decals/DecalSortRedSuite.scala). A
 * mutable key-state Input lets the suite simulate Ctrl/Cmd being held for the
 * paste path; a real in-memory Clipboard backs copy/paste.
 */
package sge
package visui
package widget

import java.nio.IntBuffer

import lowlevel.Nullable
import sge.Input
import sge.Input.{ Button, Key }
import sge.graphics.{ GL20, ShaderType }
import sge.files.DesktopFiles
import sge.noop.{ NoopAudio, NoopGraphics }
import sge.scenes.scene2d.Stage
import sge.utils.Clipboard
import sge.visui.VisUI

class VisTextFieldReadOnlyRedSuite extends munit.FunSuite {

  // 'A' (char id=65) has a glyph in default-font (skin/x1/default.fnt), so it is
  // an addable character through the real keyTyped path.
  private val typedChar: Char = 'A'
  private val newline:   Char = '\n'

  override def afterEach(context: AfterEach): Unit =
    // VisUI is global; dispose so each test re-loads the skin cleanly
    // (VisUI.load throws "cannot be loaded twice" otherwise).
    VisUI.dispose()

  /** A VisTextFieldStyle backed by the loaded skin's real `default-font` (which has glyphs for the typed characters, so the real keyTyped add-path runs). Built explicitly rather than via
    * Skin.get[VisTextFieldStyle] because the shipped uiskin.json registers the style under the Java class name and the SGE skin parser does not map it to the SGE VisTextFieldStyle type.
    */
  private def visStyle()(using Sge): VisTextField.VisTextFieldStyle = {
    val style = new VisTextField.VisTextFieldStyle()
    style.font = VisUI.getSkin.getFont("default-font")
    style.fontColor = sge.graphics.Color.WHITE
    style
  }

  private def newField(initialText: String)(using Sge): VisTextField = {
    val field = new VisTextField(Nullable(initialText), visStyle())
    field.setSize(150f, 30f)
    field
  }

  /** Builds a focused VisTextField on a real Stage. Returns (ctx, stage, field). */
  private def focusedField(initialText: String): (ReadOnlyFixture.Ctx, Stage, VisTextField) = {
    val ctx   = ReadOnlyFixture.ctx()
    given Sge = ctx.sge
    VisUI.load()
    val stage = ReadOnlyFixture.headlessStage()
    val field = newField(initialText)
    field.setPosition(0f, 100f)
    stage.addActor(field)
    stage.setKeyboardFocus(Nullable(field))
    // Mirror touchDown/focusGained: set drawBorder so keyDown runs its full body
    // (core keyDown early-returns when drawBorder == false).
    field.focusGained()
    (ctx, stage, field)
  }

  // --- ISS-531: read-only rejects typed characters --------------------------

  test("ISS-531: a read-only VisTextField must REJECT typed characters (keyTyped no-op)") {
    val (_, stage, field) = focusedField("abc")

    field.setReadOnly(true)
    val before = field.text
    stage.keyTyped(typedChar)

    assertEquals(
      field.text,
      before,
      "read-only VisTextField must not change its text on keyTyped — original VisTextField.java line 1193: " +
        "`if (disabled || readOnly) return false;`"
    )
  }

  test("ISS-531: clearing read-only re-enables typing (proves the test drives the real keyTyped path)") {
    val (_, stage, field) = focusedField("abc")

    // With read-only OFF, the exact same simulated keystroke MUST modify the
    // text. This guards against a fix that "passes" the read-only test by
    // breaking the input path entirely.
    field.setReadOnly(false)
    val before = field.text
    stage.keyTyped(typedChar)

    assertNotEquals(
      field.text,
      before,
      "a non-read-only VisTextField must accept typed characters — keyTyped is expected to insert 'A'"
    )
    assert(
      field.text.contains(typedChar),
      s"expected typed character '$typedChar' to appear in '${field.text}'"
    )
  }

  // --- ISS-531: read-only rejects paste (Ctrl/Cmd+V) ------------------------

  test("ISS-531: a read-only VisTextField must REJECT paste via Ctrl/Cmd+V (keyDown no-op)") {
    val (ctx, stage, field) = focusedField("abc")

    field.clipboard.contents = Nullable("PASTED")
    field.setReadOnly(true)
    val before = field.text

    // Hold the platform paste modifier, then press V. On macOS UIUtils.ctrl()
    // reads SYM (Cmd); elsewhere CONTROL_LEFT.
    ctx.input.press(Input.Keys.SYM)
    ctx.input.press(Input.Keys.CONTROL_LEFT)
    stage.keyDown(Input.Keys.V)

    assertEquals(
      field.text,
      before,
      "read-only VisTextField must not paste — original VisTextField.java line 1084: " +
        "`if (keycode == Keys.V && readOnly == false) paste(...)`"
    )
  }

  // --- ISS-531: read-only rejects paste via Shift+Insert --------------------

  test("ISS-531: a read-only VisTextField must REJECT paste via Shift+Insert (keyDown shift block no-op)") {
    val (ctx, stage, field) = focusedField("abc")

    field.clipboard.contents = Nullable("PASTED")
    field.setReadOnly(true)
    val before = field.text

    // Hold Shift, then press INSERT. The original VisTextField gates this on
    // readOnly inside the UIUtils.shift() block; the SGE core TextField shift
    // block runs paste UNCONDITIONALLY and VisTextField's listener only
    // intercepts the ctrl block, so the read-only flag is ignored here.
    ctx.input.press(Input.Keys.SHIFT_LEFT)
    stage.keyDown(Input.Keys.INSERT)

    assertEquals(
      field.text,
      before,
      "read-only VisTextField must not paste via Shift+Insert — original VisTextField.java line 1113: " +
        "`if (keycode == Keys.INSERT && readOnly == false) paste(clipboard.getContents(), true);`"
    )
  }

  // --- ISS-531: read-only rejects cut via Shift+ForwardDel ------------------

  test("ISS-531: a read-only VisTextField must REJECT cut via Shift+ForwardDel (keyDown shift block no-op)") {
    val (ctx, stage, field) = focusedField("abc")

    // Start with an empty clipboard so a leaked cut is observable both in the
    // (unchanged) field text and in the clipboard receiving the cut text.
    field.clipboard.contents = Nullable.empty
    field.setReadOnly(true)
    field.selectAll()
    val before = field.text

    // Hold Shift, then press FORWARD_DEL. Original VisTextField.java line 1114
    // gates this on readOnly; the SGE core shift block cuts unconditionally.
    ctx.input.press(Input.Keys.SHIFT_LEFT)
    stage.keyDown(Input.Keys.FORWARD_DEL)

    assertEquals(
      field.text,
      before,
      "read-only VisTextField must not cut via Shift+ForwardDel — original VisTextField.java line 1114: " +
        "`if (keycode == Keys.FORWARD_DEL && readOnly == false) cut(true);`"
    )
    assertEquals(
      field.clipboard.contents.toOption,
      Option.empty[String],
      "read-only cut must not push the selected text onto the clipboard — cut(true) is gated on readOnly == false"
    )
  }

  // --- ISS-531: clearing read-only re-enables Shift+Insert paste ------------

  test("ISS-531: clearing read-only re-enables Shift+Insert paste (proves the test drives the real shift path)") {
    val (ctx, stage, field) = focusedField("abc")

    // With read-only OFF, the same Shift+Insert keystroke MUST paste. This
    // guards against a fix that "passes" the read-only tests by breaking the
    // shift paste path entirely.
    field.clipboard.contents = Nullable("PASTED")
    field.setReadOnly(false)
    val before = field.text

    ctx.input.press(Input.Keys.SHIFT_LEFT)
    stage.keyDown(Input.Keys.INSERT)

    assertNotEquals(
      field.text,
      before,
      "a non-read-only VisTextField must paste on Shift+Insert — the clipboard 'PASTED' is expected to be inserted"
    )
    assert(
      field.text.contains("PASTED"),
      s"expected pasted text 'PASTED' to appear in '${field.text}'"
    )
  }

  // --- ISS-531: read-only still allows selectAll / copy ---------------------

  test("ISS-531: a read-only VisTextField must STILL allow selectAll via Ctrl/Cmd+A (reading is not blocked)") {
    val (ctx, stage, field) = focusedField("abc")

    field.setReadOnly(true)
    ctx.input.press(Input.Keys.SYM)
    ctx.input.press(Input.Keys.CONTROL_LEFT)
    stage.keyDown(Input.Keys.A)

    assertEquals(
      field.selection,
      "abc",
      "read-only must NOT disable selectAll — original VisTextField.java line 1096 (`keycode == Keys.A`) is not gated on readOnly"
    )
  }

  // --- ISS-531: enterKeyFocusTraversal moves focus on Enter -----------------

  test("ISS-531: Enter on a single-line field with enterKeyFocusTraversal=true must move focus to the next field") {
    val ctx   = ReadOnlyFixture.ctx()
    given Sge = ctx.sge
    VisUI.load()
    val stage = ReadOnlyFixture.headlessStage()

    // Two focus-traversable single-line fields; field2 sits below field1 so the
    // non-up traversal (next(false)) lands on it.
    val field1 = newField("one")
    val field2 = newField("two")
    field1.setPosition(0f, 100f)
    field2.setPosition(0f, 10f)
    stage.addActor(field1)
    stage.addActor(field2)
    stage.setKeyboardFocus(Nullable(field1))
    field1.focusGained()

    // field is single-line by default (writeEnters == false), so a normal Enter
    // would not insert anything; the distinguishing behaviour of
    // enterKeyFocusTraversal is that Enter moves focus instead.
    field1.setEnterKeyFocusTraversal(true)

    val textBefore = field1.text
    stage.keyTyped(newline)

    assert(
      stage.keyboardFocus.exists(_ eq field2),
      "enterKeyFocusTraversal=true must move keyboard focus to the next field on Enter — " +
        "original VisTextField.java line 1211 routes Enter through next() when enterKeyFocusTraversal is set"
    )
    assertEquals(
      field1.text,
      textBefore,
      "Enter used for focus traversal must NOT insert a newline into the originating field"
    )
  }
}

/** Headless harness for ISS-531: a working Clipboard, a key-state Input, and a Stage whose SpriteBatch compiles under a shader-faking GL.
  */
object ReadOnlyFixture {

  final class Ctx(val sge: Sge, val input: KeyStateInput)

  def ctx(): Ctx = {
    val fakeGl = new ShaderCompilingNoopGL20
    val in     = new KeyStateInput
    val sge    = Sge(
      application = new ClipboardApplication,
      graphics = new NoopGraphics() {
        override def gl20: GL20 = fakeGl
      },
      audio = new NoopAudio(),
      files = new DesktopFiles(),
      input = in,
      net = StubNet
    )
    new Ctx(sge, in)
  }

  /** Real Stage; its default SpriteBatch links under the shader-faking GL. */
  def headlessStage()(using Sge): Stage =
    new Stage()

  private def unused(member: String): Nothing =
    throw new IllegalStateException(s"$member must not be touched by the ISS-531 read-only tests")

  /** Application whose clipboard is a real in-memory store (VisTextField copies Sge().application.clipboard into the field at construction).
    */
  final private class ClipboardApplication extends Application {
    private val _clipboard: Clipboard = new InMemoryClipboard

    def applicationListener:                                  ApplicationListener         = unused("Application.applicationListener")
    def graphics:                                             Graphics                    = unused("Application.graphics")
    def audio:                                                Audio                       = unused("Application.audio")
    def input:                                                Input                       = unused("Application.input")
    def files:                                                Files                       = unused("Application.files")
    def net:                                                  Net                         = unused("Application.net")
    def applicationType:                                      Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
    def version:                                              Int                         = 0
    def javaHeap:                                             Long                        = 0L
    def nativeHeap:                                           Long                        = 0L
    def getPreferences(name:              String):            Preferences                 = unused("Application.getPreferences")
    def clipboard:                                            Clipboard                   = _clipboard
    def postRunnable(runnable:            Runnable):          Unit                        = ()
    def exit():                                               Unit                        = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
  }

  final private class InMemoryClipboard extends Clipboard {
    private var stored:                        Nullable[String] = Nullable.empty
    def hasContents:                           Boolean          = stored.isDefined
    def contents:                              Nullable[String] = stored
    def contents_=(content: Nullable[String]): Unit             = stored = content
  }

  private object StubNet extends Net {
    import Net._
    def httpClient:                                                                                     net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: net.ServerSocketHints): net.ServerSocket  = unused("Net.newServerSocket")
    def newServerSocket(protocol: Protocol, port:     Int, hints:   net.ServerSocketHints):             net.ServerSocket  = unused("Net.newServerSocket")
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: net.SocketHints):       net.Socket        = unused("Net.newClientSocket")
    def openURI(URI:              String):                                                              Boolean           = false
  }

  /** Input that reports a mutable set of keys as pressed (so UIUtils.ctrl()/shift() can be driven). All other members mirror the no-op defaults.
    */
  final class KeyStateInput extends Input {
    import sge.Input.*

    private val pressed:         scala.collection.mutable.Set[Int] = scala.collection.mutable.Set.empty
    private var _inputProcessor: InputProcessor                    = new InputProcessor {}

    def press(key:   Key): Unit = pressed += key.toInt
    def release(key: Key): Unit = pressed -= key.toInt

    override def isKeyPressed(key: Key): Boolean = pressed.contains(key.toInt)

    override def accelerometerX: Float = 0.0f
    override def accelerometerY: Float = 0.0f
    override def accelerometerZ: Float = 0.0f
    override def gyroscopeX:     Float = 0.0f
    override def gyroscopeY:     Float = 0.0f
    override def gyroscopeZ:     Float = 0.0f
    override def maxPointers:    Int   = 1

    override def x:                    Pixels = Pixels.zero
    override def x(pointer:      Int): Pixels = Pixels.zero
    override def deltaX:               Pixels = Pixels.zero
    override def deltaX(pointer: Int): Pixels = Pixels.zero
    override def y:                    Pixels = Pixels.zero
    override def y(pointer:      Int): Pixels = Pixels.zero
    override def deltaY:               Pixels = Pixels.zero
    override def deltaY(pointer: Int): Pixels = Pixels.zero

    override def touched:                             Boolean = false
    override def justTouched():                       Boolean = false
    override def isTouched(pointer:          Int):    Boolean = false
    override def pressure:                            Float   = 0.0f
    override def pressure(pointer:           Int):    Float   = 0.0f
    override def isButtonPressed(button:     Button): Boolean = false
    override def isButtonJustPressed(button: Button): Boolean = false
    override def isKeyJustPressed(key:       Key):    Boolean = false

    override def getTextInput(listener:              TextInputListener, title: String, text: String, hint: String):                               Unit = {}
    override def getTextInput(listener:              TextInputListener, title: String, text: String, hint: String, `type`: OnscreenKeyboardType): Unit = {}
    override def setOnscreenKeyboardVisible(visible: Boolean):                                                                                    Unit = {}
    override def setOnscreenKeyboardVisible(visible: Boolean, `type`:          OnscreenKeyboardType):                                             Unit = {}
    override def openTextInputField(configuration:   input.NativeInputConfiguration):                                                             Unit = {}
    override def closeTextInputField(sendReturn:     Boolean):                                                                                    Unit = {}
    override def setKeyboardHeightObserver(observer: KeyboardHeightObserver):                                                                     Unit = {}

    override def vibrate(milliseconds:  Int):                                    Unit = {}
    override def vibrate(milliseconds:  Int, fallback:  Boolean):                Unit = {}
    override def vibrate(milliseconds:  Int, amplitude: Int, fallback: Boolean): Unit = {}
    override def vibrate(vibrationType: VibrationType):                          Unit = {}

    override def azimuth:                                 Float           = 0.0f
    override def pitch:                                   Float           = 0.0f
    override def roll:                                    Float           = 0.0f
    override def getRotationMatrix(matrix: Array[Float]): Unit            = {}
    override def currentEventTime:                        sge.utils.Nanos = sge.utils.Nanos.zero

    override def setCatchKey(keycode: Key, catchKey: Boolean): Unit    = {}
    override def isCatchKey(keycode:  Key):                    Boolean = false

    override def setInputProcessor(processor: InputProcessor): Unit           = _inputProcessor = processor
    override def inputProcessor:                               InputProcessor = _inputProcessor

    override def isPeripheralAvailable(peripheral: Peripheral): Boolean     = false
    override def rotation:                                      Int         = 0
    override def nativeOrientation:                             Orientation = Orientation.Landscape

    override def setCursorCatched(catched: Boolean):           Unit    = {}
    override def cursorCatched:                                Boolean = false
    override def setCursorPosition(x:      Pixels, y: Pixels): Unit    = {}
  }

  /** No-op GL20 (replicating HeadlessNoopGL20) with shader compile/link status faked to succeed so SpriteBatch's default shader links (mirrors DecalSortRedSuite.ShaderCompilingGL20). All other calls
    * stay no-ops.
    */
  final class ShaderCompilingNoopGL20 extends GL20 {
    private val underlying: GL20 = HeadlessNoopGL20
    export underlying.{ glCreateProgram as _, glCreateShader as _, glGenBuffer as _, glGetProgramiv as _, glGetShaderiv as _, * }

    def glCreateShader(`type`: ShaderType): Int = 1
    def glCreateProgram():                  Int = 1

    // Nonzero buffer handle so SpriteBatch's IndexBufferObject/VertexBufferObject
    // pass their `bufferHandle == 0` guard (IndexBufferObject.scala line 166).
    def glGenBuffer(): Int = 1

    def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit =
      if (pname == GL20.GL_COMPILE_STATUS) {
        params.put(0, GL20.GL_TRUE)
        ()
      }

    def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit =
      if (pname == GL20.GL_LINK_STATUS) {
        params.put(0, GL20.GL_TRUE)
        ()
      }
  }
}
