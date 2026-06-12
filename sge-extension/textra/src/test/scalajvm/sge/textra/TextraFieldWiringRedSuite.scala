/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-525, clauses 2 and 3 of 3 (TextraField clipboard and
 * emoji wiring). Clause 1 (LinkEffect) lives in TextraWiringRedSuite
 * (src/test/scala, shared).
 *
 * Root causes being reproduced (port file:line at the red commit):
 *  2. TextraField.scala:107-108 — `clipboard: Nullable[Clipboard]` claims
 *     to be "connected via initialize() or manually", but initialize()
 *     (lines 192-194) only creates the input listener, so the
 *     application clipboard is never connected and copy()/cut() silently
 *     drop the selection. Upstream TextraField.java:215-220 initialize()
 *     does `clipboard = Gdx.app.getClipboard();` (line 218) and copy()
 *     (TextraField.java:539-547) puts the selection there via
 *     `clipboard.setContents(toCopy)` (line 545)
 *     (original-src/textratypist/src/main/java/com/github/tommyettinger/
 *     textra/TextraField.java). SGE has the facility:
 *     sge.Application.clipboard returning sge.utils.Clipboard.
 *  3. TextraField.scala:192-194 — "Emoji replacer requires EmojiProcessor
 *     integration (deferred)": upstream initialize() builds
 *     `emojiReplacer = EmojiProcessor.getReplacer(label.font)` whenever
 *     `label.font.nameLookup != null` (TextraField.java:216-217,
 *     EmojiProcessor.java:66), paste() rewrites incoming text with it
 *     (TextraField.java:563-565: `content = emojiReplacer.replace(content)`)
 *     and setMessageText() does the same (TextraField.java:709-711). SGE
 *     has the facility: this very module ships
 *     EmojiProcessor.replaceEmoji(text, font)
 *     (EmojiProcessor.scala:334-348), fully ported and proven green by the
 *     control test below — it just has no caller in TextraField.
 *
 * Fixture pattern: same recording-Sge approach as TextraLzmaFontRedSuite —
 * the suite builds a `Sge` case class instance (accessible because the
 * test lives in package sge) from sge.noop.{NoopGraphics, NoopAudio,
 * NoopInput} plus recording Application/Net stubs, and provides it as a
 * `given`, so the suite compiles unchanged once the fix threads
 * `(using Sge)` through the TextraField constructors. Each test asserts
 * the given is the fixture (fixture self-check) so the context is provably
 * the one the fixed code must consult.
 *
 * Platform placement: JVM only — NOT silently skipped elsewhere, but
 * genuinely not reproducible there today: constructing a TextraField
 * headlessly reaches TypingLabel.restart -> Parser's look-behind regexes
 * (Parser.scala:37-81, `(?<!\[)...`), which Scala.js rejects
 * (java.util.regex.PatternSyntaxException: "Look-behind group is not
 * supported because it requires RegExp features of ECMAScript 2018") and
 * Scala Native misparses (PatternSyntaxException: "capturing group name
 * does not start with a Latin letter near index 0", after which every use
 * of sge.textra.Parser$ dies with NoClassDefFoundError). That pre-existing
 * platform gap breaks even the green control (a manually connected
 * clipboard), so off-JVM these clauses cannot be exercised at all; it is
 * a separate defect from ISS-525's wiring.
 *
 * The emoji fixture deliberately uses a BMP emoji (U+2614, present both in
 * upstream's regex — EmojiProcessor.java:45, literal in the BMP char
 * class — and in the port's, EmojiProcessor.scala:74 `☔`): the port's
 * java.util.regex matcher is codepoint-based, so the lone-surrogate ranges
 * it inherited from RegExodus do not match supplementary-plane emoji like
 * U+1F600 — another separate latent defect that must not keep these wiring
 * tests red after the ISS-525 fix.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified
 * by the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer

import lowlevel.Nullable
import sge.files.{ FileHandle, FileType }
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.textra.utils.CaseInsensitiveIntMap
import sge.utils.Clipboard

class TextraFieldWiringRedSuite extends munit.FunSuite {

  // --- Recording fixtures -------------------------------------------------

  /** In-memory clipboard recording every write, standing in for the platform clipboard returned by Application.clipboard. */
  final private class RecordingClipboard extends Clipboard {
    var stored:                                Nullable[String] = Nullable.empty
    var setCount:                              Int              = 0
    def hasContents:                           Boolean          = stored.isDefined
    def contents:                              Nullable[String] = stored
    def contents_=(content: Nullable[String]): Unit             = {
      stored = content
      setCount += 1
    }
    def storedOrEmpty: String = Nullable.getOrElse(stored)("<nothing on the clipboard>")
  }

  /** Net stub recording every openURI call. */
  final private class RecordingNet extends Net {
    import Net.*
    val opened:                                                                                             ArrayBuffer[String] = ArrayBuffer.empty
    def httpClient:                                                                                         net.SgeHttpClient   = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: sge.net.ServerSocketHints): net.ServerSocket    = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   sge.net.ServerSocketHints):             net.ServerSocket    = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: sge.net.SocketHints):       net.Socket          = throw new UnsupportedOperationException
    def openURI(URI: String):                                                                               Boolean             = {
      opened += URI
      true
    }
  }

  /** Application stub exposing the recording clipboard, like upstream's Gdx.app.getClipboard() (TextraField.java:218). */
  final private class StubApplication(cb: Clipboard) extends Application {
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
    def clipboard:                                            sge.utils.Clipboard         = cb
    def postRunnable(runnable:            Runnable):          Unit                        = ()
    def exit():                                               Unit                        = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
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

  private def sgeWith(clipboard: Clipboard, net: Net): Sge =
    Sge(new StubApplication(clipboard), new NoopGraphics(), new NoopAudio(), NoFiles, new NoopInput(), net)

  // --- Font fixtures (pure in-memory, the FontSuite pattern) ---------------

  /** A Font whose mapping covers the given chars, so TextraField's paste path keeps them verbatim. */
  private def plainFont(chars: String): Font = {
    val font = new Font()
    font.mapping.put(' '.toInt, new Font.GlyphRegion(0f, 0f, 8f))
    chars.foreach(c => font.mapping.put(c.toInt, new Font.GlyphRegion(0f, 0f, 8f)))
    font
  }

  /** U+2614 UMBRELLA WITH RAIN DROPS — a BMP emoji matched by both upstream's and the port's regex (see the header comment). */
  private val Emoji: String = "☔"

  /** The PUA char (U+E000) our fixture font uses to render the emoji, like KnownFonts.addEmoji would assign. */
  private val Pua: Char = ''

  /** A Font with a nameLookup mapping the emoji to its PUA char — the exact precondition upstream TextraField.java:216 checks before wiring the replacer. */
  private def emojiFont(): Font = {
    val font = plainFont("hiok")
    val nl   = new CaseInsensitiveIntMap()
    nl.put(Emoji, Pua.toInt)
    font.nameLookup = Nullable(nl)
    font.mapping.put(Pua.toInt, new Font.GlyphRegion(0f, 0f, 8f))
    font
  }

  private def fieldWith(font: Font)(using Sge): TextraField = {
    val style = new Styles.TextFieldStyle()
    style.font = Nullable(font)
    val field = new TextraField(Nullable.empty, style)
    field.setOnlyFontChars(false)
    field
  }

  // --- ISS-525 clause 2: clipboard -----------------------------------------

  test(
    "ISS-525 clipboard: initialize() connects the application clipboard (upstream TextraField.java:218 clipboard = Gdx.app.getClipboard())"
  ) {
    val cb = new RecordingClipboard
    given sge: Sge = sgeWith(cb, new RecordingNet)
    assert(sge.application.clipboard eq cb, "fixture self-check: the given Sge exposes the recording clipboard")

    val field = fieldWith(plainFont("sge"))
    assert(
      field.getClipboard.isDefined,
      "TextraField must come out of its constructor with the application clipboard connected; the port's TextraField.scala:107-108 field stays empty because initialize() (lines 192-194) never assigns it"
    )
  }

  test("ISS-525 clipboard: copy() puts the selection on the application clipboard (upstream TextraField.java:539-547)") {
    val cb = new RecordingClipboard
    given sge: Sge = sgeWith(cb, new RecordingNet)
    assert(sge.application.clipboard eq cb, "fixture self-check: the given Sge exposes the recording clipboard")

    val field = fieldWith(plainFont("copied"))
    field.setText(Nullable("copied"))
    field.selectAll()
    field.copy()

    assertEquals(
      cb.storedOrEmpty,
      "copied",
      "copy() must place the selection on the clipboard provided by Sge's application (Gdx.app.getClipboard() upstream); today nothing is ever connected so the text is dropped"
    )
  }

  test("ISS-525 control (green at red commit): a manually connected clipboard receives copy()") {
    val cb = new RecordingClipboard
    given sge: Sge = sgeWith(cb, new RecordingNet)
    assert(sge.application.clipboard eq cb, "fixture self-check: the given Sge exposes the recording clipboard")

    val manual = new RecordingClipboard
    val field  = fieldWith(plainFont("copied"))
    field.setClipboard(manual)
    field.setText(Nullable("copied"))
    field.selectAll()
    field.copy()

    // Proves the whole selection/copy path works headlessly: the ONLY missing piece is the default wiring.
    assertEquals(manual.storedOrEmpty, "copied")
    assertEquals(manual.setCount, 1)
  }

  // --- ISS-525 clause 3: emoji replacement ----------------------------------

  test(
    "ISS-525 emoji: pasted text has Unicode emoji replaced with the font's PUA char (upstream TextraField.java:216-217 and 563-565)"
  ) {
    val cb = new RecordingClipboard
    given sge: Sge = sgeWith(cb, new RecordingNet)
    assert(sge.application.clipboard eq cb, "fixture self-check: the given Sge exposes the recording clipboard")

    val field = fieldWith(emojiFont())
    field.appendText(Nullable("hi " + Emoji))

    // Upstream: initialize() saw label.font.nameLookup != null, built the replacer (TextraField.java:216-217),
    // and paste() rewrote the content before insertion (TextraField.java:565: content = emojiReplacer.replace(content)).
    assertEquals(
      field.getText,
      "hi " + Pua,
      "paste/appendText must replace the emoji with the font's PUA char; the port's initialize() (TextraField.scala:192-194) defers the EmojiProcessor integration so the raw emoji survives"
    )
  }

  test("ISS-525 emoji: setMessageText replaces emoji too (upstream TextraField.java:709-711)") {
    val cb = new RecordingClipboard
    given sge: Sge = sgeWith(cb, new RecordingNet)
    assert(sge.application.clipboard eq cb, "fixture self-check: the given Sge exposes the recording clipboard")

    val field = fieldWith(emojiFont())
    field.setMessageText(Nullable("ok " + Emoji))

    assertEquals(
      Nullable.getOrElse(field.getMessageText)("<no message text>"),
      "ok " + Pua,
      "setMessageText must run the emoji replacement (upstream TextraField.java:710); the port stores the raw text"
    )
  }

  test("ISS-525 control (green at red commit): EmojiProcessor.replaceEmoji already performs the replacement") {
    // Proves the facility exists and works in this module (EmojiProcessor.scala:334-348) — wiring it into
    // TextraField's initialize()/paste()/setMessageText is pure plumbing.
    val font = emojiFont()
    assertEquals(EmojiProcessor.replaceEmoji("hi " + Emoji, font), "hi " + Pua)
    // A font without a nameLookup leaves text unchanged, matching upstream's null guard (TextraField.java:216).
    assertEquals(EmojiProcessor.replaceEmoji("hi " + Emoji, plainFont("hi")), "hi " + Emoji)
  }
}
