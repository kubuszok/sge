/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-525, clause 1 of 3 (LinkEffect). The clipboard and
 * emoji clauses of ISS-525 live in TextraFieldWiringRedSuite
 * (src/test/scalajvm), because they need a TextraField and constructing
 * one headlessly is JVM-only today: TypingLabel.restart reaches Parser's
 * look-behind regexes (Parser.scala:37-81, `(?<!\[)...`), which Scala.js
 * rejects (java.util.regex.PatternSyntaxException: "Look-behind group is
 * not supported because it requires RegExp features of ECMAScript 2018")
 * and Scala Native misparses ("capturing group name does not start with a
 * Latin letter near index 0", then NoClassDefFoundError for
 * sge.textra.Parser$ on every later use). This suite needs no TextraField,
 * so it is shared and runs on JVM, JS and Native.
 *
 * Root cause being reproduced (port file:line at the red commit):
 *   LinkEffect.scala:25-26 — onApply consumes the click
 *   (lastTouchedIndex = -1) and then does `val _ = link`: the URL is
 *   never opened. Upstream LinkEffect.java:54-60 calls
 *   `Gdx.net.openURI(link)` (line 58) inside the very same branch
 *   (original-src/textratypist/src/main/java/com/github/tommyettinger/
 *   textra/effects/LinkEffect.java). SGE has the facility: sge.Net.openURI
 *   (sge/src/main/scala/sge/Net.scala line 113) reachable through the
 *   `(using Sge)` context that the project rules mandate instead of
 *   LibGDX's `Gdx.*` globals.
 *
 * Fixture pattern: same recording-Sge approach as TextraLzmaFontRedSuite —
 * the suite builds a `Sge` case class instance (accessible because the
 * test lives in package sge) from sge.noop.{NoopGraphics, NoopAudio,
 * NoopInput} plus a recording Net stub, and provides it as a `given`, so
 * the suite compiles unchanged once the fix threads `(using Sge)` through
 * the LinkEffect constructor. Each test asserts the given is the fixture
 * (fixture self-check) so the context is provably the one the fixed code
 * must consult.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified
 * by the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer

import sge.files.{ FileHandle, FileType }
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.textra.effects.LinkEffect

class TextraWiringRedSuite extends munit.FunSuite {

  // --- Recording fixtures -------------------------------------------------

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

  // --- ISS-525 clause 1: LinkEffect ---------------------------------------

  test(
    "ISS-525 LinkEffect: clicking the linked glyph opens the URL via the Sge net (upstream LinkEffect.java:58 Gdx.net.openURI)"
  ) {
    val net = new RecordingNet
    given sge: Sge = sgeWith(net)
    assert(sge.net eq net, "fixture self-check: the given Sge exposes the recording net")

    val label  = new TypingLabel()
    val effect = new LinkEffect(label, Array("https://example.com/iss-525"))
    // Simulate the click TypingLabel registers when trackingInput is on: the touched glyph index...
    label.lastTouchedIndex = 3
    // ...and the per-glyph effect application that TypingLabel performs while drawing.
    effect.apply(0L, 3, 0.016f)

    // Upstream LinkEffect.java:56-59: when lastTouchedIndex matches the glyph, consume it AND open the URL.
    assertEquals(
      net.opened.toList,
      List("https://example.com/iss-525"),
      "the {LINK=url} click must open the URL through Sge's net (Gdx.net.openURI upstream); the port's LinkEffect.scala:25-26 drops it"
    )
  }

  test("ISS-525 control (green at red commit): LinkEffect tracks input and consumes the matching click") {
    val net = new RecordingNet
    given sge: Sge = sgeWith(net)
    assert(sge.net eq net, "fixture self-check: the given Sge exposes the recording net")

    val label = new TypingLabel()
    assert(!label.trackingInput, "a fresh TypingLabel does not track input")
    val effect = new LinkEffect(label, Array.empty[String])
    assert(label.trackingInput, "constructing a LinkEffect enables input tracking (upstream LinkEffect.java:46)")

    // A non-matching glyph index leaves the pending click untouched...
    label.lastTouchedIndex = 5
    effect.apply(0L, 2, 0.016f)
    assertEquals(label.lastTouchedIndex, 5)
    // ...and the matching one consumes it (upstream LinkEffect.java:57).
    effect.apply(0L, 5, 0.016f)
    assertEquals(label.lastTouchedIndex, -1)
  }
}
