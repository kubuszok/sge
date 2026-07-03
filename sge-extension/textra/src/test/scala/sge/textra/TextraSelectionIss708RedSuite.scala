/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-708: the textra text-selection subsystem is missing
 * end-to-end in the port. These pin the ORIGINAL TextraTypist semantics
 * (TextraTypist upstream commit referenced by the textra headers,
 * original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * TypingLabel.java) that the port does NOT yet honour.
 *
 * Root causes reproduced (port file:line at the red commit):
 *   - copySelectedText unconditionally returns false and never writes the
 *     clipboard: TypingLabel.scala:735-740 — the else branch of the guard is
 *     `false` with a comment about deferred clipboard integration. Upstream
 *     TypingLabel.java:1560-1564 returns TRUE when the label is selectable and
 *     a range is set, AND writes `substring(selectionStart, selectionEnd + 1)`
 *     to `Gdx.app.getClipboard().setContents(...)`. SGE exposes the same
 *     facility as `sge.utils.Clipboard` via the `(using Sge)` application
 *     (Application.clipboard), reachable from the context the class already
 *     carries (`class TypingLabel(using Sge)`).
 *   - setSelectable never initialises selectionDrawable:
 *     TypingLabel.scala:474-478 only sets `selectable`/`trackingInput`.
 *     Upstream TypingLabel.java:1901-1910 additionally, when the label is
 *     selectable and `font.mapping` contains `font.solidBlock`, builds a
 *     `Sprite` from that glyph and assigns `selectionDrawable =
 *     new SpriteDrawable(spr)` (lines 1904-1907). At the red commit
 *     `selectionDrawable` (TypingLabel.scala:75) stays `Nullable.empty`.
 *
 * Why these assertions are testable headlessly and cross-platform:
 *   - No GL, no rendering. copySelectedText reads glyphs straight out of the
 *     public `workingLayout` (TypingLabel.scala:65); the tests seed that layout
 *     directly (`workingLayout.lines(0).glyphs += ...`, the exact idiom of
 *     LayoutLineSuite) instead of `setText`, so Parser's look-behind regexes
 *     (Parser.scala) are never reached — that is the JS/Native limitation that
 *     forces TextraFieldWiringRedSuite onto JVM; this suite avoids it entirely.
 *   - substring over ASCII glyphs never dereferences the font
 *     (TypingLabel.scala:686-726), so `new TypingLabel()` (the Parser-free
 *     primary constructor, as used cross-platform by TextraWiringRedSuite)
 *     suffices.
 *   - setSelectable's drawable branch is exercised by seeding the label's own
 *     font mapping (via getFont, TypingLabel.scala:170) with a solidBlock
 *     GlyphRegion, so a faithful fix reaches TypingLabel.java:1904-1907.
 *     That fix's `new Sprite(region)` (Sprite.scala:112) ends in
 *     TextureRegion.setRegion(u, v, u2, v2) (TextureRegion.scala:122-124),
 *     which dereferences `texture.width` — so the seeded GlyphRegion must be
 *     texture-backed, exactly as upstream always is (Font.java:2708
 *     `new GlyphRegion(new TextureRegion(whiteBlock, 1, 1, 1, 1))`). A real
 *     Texture IS constructible with no GL context: the DummyTextureData idiom
 *     of SpriteBatchVertexGeometryISS561Suite (Custom dataType, isPrepared
 *     true, no-op consumeCustomData) makes every GL call in the Texture
 *     constructor a NoopGL20 no-op (glGenTexture returns 0, glBindTexture and
 *     GLTexture.bind are pure delegates with no handle guard).
 *
 * Fixture pattern: the recording-Sge approach of TextraWiringRedSuite — a
 * `Sge` built from `sge.noop.{NoopGraphics,NoopAudio,NoopInput}` plus stubs,
 * provided as a `given`. The application returns a RecordingClipboard so the
 * clipboard write upstream performs (TypingLabel.java:1562) is observable.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original TextraTypist semantics, not the port's.
 */
package sge
package textra

import lowlevel.Nullable

import sge.files.{ FileHandle, FileType }
import sge.graphics.{ Pixmap, Texture, TextureData, TextureTarget }
import sge.graphics.g2d.TextureRegion
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.utils.Clipboard

class TextraSelectionIss708RedSuite extends munit.FunSuite {

  // --- Recording fixtures -------------------------------------------------

  /** Clipboard recording every write (the side effect upstream TypingLabel.java:1562 performs). */
  final private class RecordingClipboard extends Clipboard {
    var stored:                                Nullable[String] = Nullable.empty
    var writes:                                Int              = 0
    def hasContents:                           Boolean          = stored.isDefined
    def contents:                              Nullable[String] = stored
    def contents_=(content: Nullable[String]): Unit             = {
      stored = content
      writes += 1
    }
  }

  private class RecordingApplication(clip: Clipboard) extends Application {
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
    def clipboard:                                            Clipboard                   = clip
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

  private def sgeWith(clip: Clipboard): Sge =
    Sge(new RecordingApplication(clip), new NoopGraphics(), new NoopAudio(), NoFiles, new NoopInput(), NoNet)

  /** Custom-type TextureData (the DummyTextureData idiom of SpriteBatchVertexGeometryISS561Suite): every GL call the Texture constructor makes is a NoopGL20 no-op, so the Texture is constructible
    * with no GL context.
    */
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

  /** A texture-backed solidBlock GlyphRegion, shaped like the one upstream builds (Font.java:2708 `new GlyphRegion(new TextureRegion(whiteBlock, 1, 1, 1, 1))`): a 3x3 texture with the centre pixel as
    * the region. Texture-backed because the fix's `new Sprite(region)` (upstream TypingLabel.java:1905) reads `texture.width` via TextureRegion.setRegion (TextureRegion.scala:122-124).
    */
  private def solidBlockGlyphRegion()(using Sge): Font.GlyphRegion =
    new Font.GlyphRegion(new TextureRegion(new Texture(new DummyTextureData(3, 3)), 1, 1, 1, 1), 0f, 0f, 8f)

  /** Seeds the public working layout with one line of ASCII glyphs (LayoutLineSuite idiom), avoiding Parser. */
  private def seedGlyphs(label: TypingLabel, text: String): Unit =
    text.foreach(c => label.workingLayout.lines(0).glyphs += c.toLong)

  private def selected(clip: RecordingClipboard): String =
    Nullable.fold(clip.stored)("<clipboard-never-written>")(identity)

  // --- copySelectedText: return value (upstream TypingLabel.java:1561-1563) ---

  test(
    "ISS-708: copySelectedText returns true when the label is selectable and a range is selected (upstream TypingLabel.java:1561-1563 `return true`)"
  ) {
    val clip = new RecordingClipboard
    given sge: Sge = sgeWith(clip)
    assert(sge.application.clipboard eq clip, "fixture self-check: the given Sge's application exposes the recording clipboard")

    val label = new TypingLabel()
    seedGlyphs(label, "Hello") // 5 glyphs, indices 0..4
    label.setSelectable(true)
    label.selectionStart = 0
    label.selectionEnd = 4

    // Upstream TypingLabel.java:1561-1563: guard passes, so it copies and returns true.
    // Port TypingLabel.scala:735-740 returns false in this very branch.
    assert(
      label.copySelectedText(),
      "copySelectedText must return true for a non-empty selection (upstream TypingLabel.java:1563)"
    )
  }

  // --- copySelectedText: clipboard receives the selected substring (upstream :1562) ---

  test(
    "ISS-708: copySelectedText writes the full selected substring to the clipboard (upstream TypingLabel.java:1562 setContents(substring(start, end+1)))"
  ) {
    val clip  = new RecordingClipboard
    given Sge = sgeWith(clip)

    val label = new TypingLabel()
    seedGlyphs(label, "Hello")
    label.setSelectable(true)
    label.selectionStart = 0
    label.selectionEnd = 4

    val copied = label.copySelectedText()
    assert(copied, "copySelectedText must report success (upstream TypingLabel.java:1563)")
    // Upstream writes substring(selectionStart, selectionEnd + 1) == substring(0, 5) == "Hello".
    // Port never touches the clipboard (TypingLabel.scala:738-739), so `writes` stays 0.
    assertEquals(clip.writes, 1, "the clipboard must be written exactly once (upstream TypingLabel.java:1562)")
    assertEquals(selected(clip), "Hello", "the clipboard must receive the selected substring (upstream TypingLabel.java:1562)")
  }

  // --- copySelectedText: offset range copies the correct sub-range (upstream :1562) ---

  test(
    "ISS-708: copySelectedText copies a mid-string selection, not the whole text (upstream TypingLabel.java:1562 substring bounds)"
  ) {
    val clip  = new RecordingClipboard
    given Sge = sgeWith(clip)

    val label = new TypingLabel()
    seedGlyphs(label, "Hello")
    label.setSelectable(true)
    label.selectionStart = 1
    label.selectionEnd = 3

    assert(label.copySelectedText(), "copySelectedText must succeed for the offset selection (upstream TypingLabel.java:1563)")
    // substring(1, 3 + 1) == substring(1, 4) == "ell".
    assertEquals(selected(clip), "ell", "the clipboard must receive exactly the selected sub-range (upstream TypingLabel.java:1562)")
  }

  // --- copySelectedText: single-glyph selection (upstream :1561 boundary) ---

  test(
    "ISS-708: copySelectedText handles a single-glyph selection (selectionStart == selectionEnd; upstream TypingLabel.java:1561 non-negative guard)"
  ) {
    val clip  = new RecordingClipboard
    given Sge = sgeWith(clip)

    val label = new TypingLabel()
    seedGlyphs(label, "Hello")
    label.setSelectable(true)
    label.selectionStart = 2
    label.selectionEnd = 2

    assert(
      label.copySelectedText(),
      "a single-glyph selection is a valid non-negative range (upstream TypingLabel.java:1561 passes)"
    )
    // substring(2, 2 + 1) == substring(2, 3) == "l".
    assertEquals(selected(clip), "l", "the clipboard must receive the single selected glyph (upstream TypingLabel.java:1562)")
  }

  // --- setSelectable: selectionDrawable initialisation (upstream :1904-1907) ---

  test(
    "ISS-708: setSelectable(true) initialises selectionDrawable when the font has a solidBlock glyph (upstream TypingLabel.java:1904-1907)"
  ) {
    val clip  = new RecordingClipboard
    given Sge = sgeWith(clip)

    val label = new TypingLabel()
    // Make the upstream branch reachable: font.mapping must contain font.solidBlock
    // (upstream TypingLabel.java:1904 `font.mapping.containsKey(font.solidBlock)`).
    val font = label.getFont
    font.mapping.put(font.solidBlock.toInt, solidBlockGlyphRegion())

    assert(label.selectionDrawable.isEmpty, "before setSelectable there is no selection drawable (TypingLabel.scala:75 default)")

    label.setSelectable(true)

    // Upstream TypingLabel.java:1904-1907 assigns selectionDrawable = new SpriteDrawable(...).
    // Port setSelectable (TypingLabel.scala:474-478) never touches selectionDrawable.
    assert(
      label.selectionDrawable.isDefined,
      "setSelectable(true) must initialise selectionDrawable from the solidBlock glyph (upstream TypingLabel.java:1904-1907)"
    )
  }
}
