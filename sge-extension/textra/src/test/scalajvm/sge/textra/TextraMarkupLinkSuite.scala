/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-525 markup-path test (fixer-authored, NOT one of the frozen red
 * suites). It exercises the PRIMARY use case the auditor flagged: a
 * `{LINK=url}...{ENDLINK}` markup span parsed through the real Parser, then
 * clicked, must open the URL through the label's per-application Sge net.
 * This is upstream's documented usage example
 * (LinkEffect.java javadoc: `{LINK=https://github.com/...}Text effects!{ENDLINK}`)
 * and the path the registered `LINK` effect factory drives
 * (TypingConfig.scala: `(l, p) => new LinkEffect(l, p)`), which reads the
 * Sge captured by `TextraLabel(using Sge)`.
 *
 * JVM-only on purpose: the markup parse path goes through
 * TypingLabel.setText -> restart -> parseTokens -> Parser, whose
 * look-behind regexes (Parser.scala:37,52,67-81, `(?<!\[)...`) are rejected
 * by Scala.js (PatternSyntaxException: "Look-behind group is not
 * supported...") and misparsed by Scala Native (then NoClassDefFoundError
 * for sge.textra.Parser$). That is the SAME pre-existing platform gap
 * (tracked as ISS-642) that confines TextraFieldWiringRedSuite to scalajvm;
 * a plain `new TypingLabel()` constructs everywhere, but a markup `setText`
 * does not. The shared-across-platforms coverage of the click-opens-URL
 * wiring lives in TextraWiringRedSuite (which drives the effect directly,
 * without the Parser); this suite proves the produce-site wiring
 * (TypingConfig LINK factory -> label.sgeContext) that the shared suite
 * does not reach.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer

import sge.files.{ FileHandle, FileType }
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }

class TextraMarkupLinkSuite extends munit.FunSuite {

  // --- Recording fixtures (the TextraWiringRedSuite pattern) ----------------

  /** Net stub recording every openURI call (the side effect upstream LinkEffect.java:58 performs). */
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

  private def sgeWith(net: Net): Sge =
    Sge(StubApplication, new NoopGraphics(), new NoopAudio(), NoFiles, new NoopInput(), net)

  /** A Font whose mapping covers the link-text chars, so markup layout produces real glyphs for the link span. */
  private def plainFont(chars: String): Font = {
    val font = new Font()
    font.mapping.put(' '.toInt, new Font.GlyphRegion(0f, 0f, 8f))
    chars.foreach(c => font.mapping.put(c.toInt, new Font.GlyphRegion(0f, 0f, 8f)))
    font
  }

  // --- ISS-525 markup path: {LINK=url}...{ENDLINK} --------------------------

  test(
    "ISS-525 markup LINK: parsing {LINK=url}...{ENDLINK} and clicking the span opens the URL via the label's Sge net (upstream usage example; produce-site TypingConfig LINK factory -> label.sgeContext)"
  ) {
    val net = new RecordingNet
    given sge: Sge = sgeWith(net)
    assert(sge.net eq net, "fixture self-check: the given Sge exposes the recording net")

    val url   = "https://github.com/tommyettinger/textratypist"
    val style = new Styles.LabelStyle(plainFont("clickme"), lowlevel.Nullable.empty)
    val label = new TypingLabel("", style)
    // Parse the markup through the real Parser; this is the path the LINK effect factory drives.
    label.setText("{LINK=" + url + "}click me{ENDLINK}")
    // Reveal every glyph so the produced LinkEffect is active across its span.
    label.skipToTheEnd()

    // Clicking a glyph in the linked span is registered by TypingLabel as lastTouchedIndex; glyph 0
    // ('c' of "click") is inside the {LINK} span because the default initial text is empty.
    label.lastTouchedIndex = 0
    label.act(0.016f)

    assertEquals(
      net.opened.toList,
      List(url),
      "the parsed {LINK=url} span must open its URL through the label's Sge net (Gdx.net.openURI upstream); the markup wiring runs TypingConfig LINK -> new LinkEffect(l, p) -> l.sgeContext.net.openURI"
    )
    assertEquals(label.lastTouchedIndex, -1, "the click is consumed exactly as upstream LinkEffect.java:57 does")
  }

  test(
    "ISS-525 markup LINK: an untouched markup span opens nothing (control — the click, not mere parsing, triggers openURI)"
  ) {
    val net = new RecordingNet
    given sge: Sge = sgeWith(net)
    assert(sge.net eq net, "fixture self-check: the given Sge exposes the recording net")

    val style = new Styles.LabelStyle(plainFont("clickme"), lowlevel.Nullable.empty)
    val label = new TypingLabel("", style)
    label.setText("{LINK=https://example.com/untouched}click me{ENDLINK}")
    label.skipToTheEnd()

    // No glyph is marked as touched, so the LinkEffect must not open anything.
    label.act(0.016f)

    assertEquals(
      net.opened.toList,
      Nil,
      "parsing and revealing a {LINK} span must not open its URL until a glyph in the span is actually clicked"
    )
  }
}
