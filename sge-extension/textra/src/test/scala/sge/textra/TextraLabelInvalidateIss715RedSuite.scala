/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-715 (review-fable): invalidateHierarchy dropped across the
 * textra labels — specifically TextraLabel.setText no longer marks the widget
 * as needing layout.
 *
 * Original semantics (TextraTypist, upstream commit
 * 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4;
 * original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * TextraLabel.java:712-721):
 *
 *   public void setText(String markupText) {
 *       storedText = defaultToken + markupText;
 *       if (wrap) layout.setTargetWidth(getWidth());
 *       font.markup(storedText, layout.clear());
 *       invalidateHierarchy();                       // <-- TextraLabel.java:720
 *   }
 *
 * `invalidateHierarchy()` (com.badlogic.gdx.scenes.scene2d.ui.Widget) calls
 * `invalidate()`, which sets `needsLayout = true` so the next `validate()`
 * re-runs layout for the new text. Without it, changing the text after a
 * validate() leaves the widget believing its (stale) layout is still valid.
 *
 * The port (sge-extension/textra/src/main/scala/sge/textra/TextraLabel.scala:
 * 276-280 at the red commit) drops the `invalidateHierarchy()` call:
 *
 *   def setText(markupText: String): Unit = {
 *     storedText = defaultToken + markupText
 *     if (wrap) baseLayout.setTargetWidth(width)
 *     font.markup(storedText, baseLayout.clear())
 *   }                                                // <-- no invalidateHierarchy
 *
 * SGE's Widget already provides the machinery: Widget.invalidateHierarchy /
 * invalidate set `_needsLayout = true` and Widget.needsLayout exposes it
 * (sge/src/main/scala/sge/scenes/scene2d/ui/Widget.scala:81-93), and
 * TextraLabel.invalidate is already overridden (TextraLabel.scala:288). The
 * only thing missing is the call from setText.
 *
 * (ISS-715 also covers the minimized-window guard + invalidateHierarchy in
 * TextraLabel.layout()/doLayout (orig :621,:642) and the TypingLabel guards
 * (orig :963/:980/:997); those are height-change / graphics-size paths that
 * need a glyph-bearing font to observe. This suite pins the cleanest,
 * font-independent clause — setText must invalidate — which is one of the two
 * TextraLabel omissions the issue names at :276-280.)
 *
 * This test is written by the reproducer agent and MUST NOT be modified by the
 * fixer: it encodes the original TextraTypist semantics (setText invalidates
 * the hierarchy), not the port's silent no-op.
 */
package sge
package textra

import sge.files.{ FileHandle, FileType }
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }

class TextraLabelInvalidateIss715RedSuite extends munit.FunSuite {

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

  test(
    "ISS-715: TextraLabel.setText must invalidate the layout hierarchy (orig TextraLabel.java:720); the port's setText leaves needsLayout false"
  ) {
    given Sge = headlessSge()
    val label = new TextraLabel()

    label.setText("hello")
    // validate() clears the pending-layout flag (Widget.validate -> layout()).
    label.validate()
    assert(!label.needsLayout, "control: validate() clears needsLayout")

    // Changing the text must invalidate the hierarchy again so the next
    // validate() re-lays-out the new content (orig TextraLabel.java:720).
    label.setText("a different, longer piece of text")

    assert(
      label.needsLayout,
      "ISS-715: setText must call invalidateHierarchy (orig TextraLabel.java:720); the port's setText (TextraLabel.scala:276-280) omits it, so needsLayout stays false"
    )
  }
}
