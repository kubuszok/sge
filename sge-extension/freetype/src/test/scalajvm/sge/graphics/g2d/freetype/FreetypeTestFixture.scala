/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Headless JVM test fixture for the FreeType extension characterization /
 * red suites (ISS-725, ISS-727, ISS-777, ISS-805).
 *
 * The sge core test fixture (sge/src/test/scala/sge/SgeTestFixture.scala) is
 * NOT on this module's test classpath — build.sbt wires
 * `sge-freetype`.dependsOn(sge) in the Compile configuration only, matching
 * every other extension module (see VisUITestFixture.scala for the same
 * precedent). The minimal headless Sge is therefore replicated here.
 *
 *   - headlessSge() uses the REAL DesktopFiles so that
 *     Sge().files.classpath(...) resolves the bundled .ttf fixtures from the
 *     test runtime classpath (src/test/resources).
 *   - Graphics is NoopGraphics (its gl20 defaults to the no-op NoopGL20).
 *     The suites never upload textures: they drive generateData with a
 *     caller-supplied PixmapPacker (so the generator does NOT own the atlas
 *     and never calls updateTextureRegions, the only GL path), and
 *     generateGlyphAndBitmap, which rasterizes without any GL at all. Pixmap
 *     is CPU-side (Gdx2DPixmap), so glyph packing works headlessly.
 *
 * FreeType glyph rasterization itself runs through the real sge_freetype
 * native library provided by the pnm-provider-sge-freetype-desktop dependency
 * on the JVM row.
 *
 * These fixtures encode the ORIGINAL FreeTypeFontGenerator/FreeType semantics,
 * not the port's — the fixer MUST NOT modify them.
 */
package sge
package graphics
package g2d
package freetype

import sge.files.DesktopFiles
import sge.files.FileHandle
import sge.graphics.g2d.PixmapPacker
import sge.graphics.Pixmap.Format
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }

object FreetypeTestFixture {

  /** Bundled scalable OUTLINE TTF fixture: Inconsolata-LGC (SIL OFL 1.1, from original-src/textratypist). It rasterizes crisp outline glyphs at arbitrary pixel sizes and reports `hasKerning == false`
    * (it kerns via GPOS, not a legacy `kern` table), which the ISS-777 kerning-mutation check requires.
    */
  val OutlineFont: String = "sge-freetype-test-Inconsolata-LGC.ttf"

  /** A headless [[Sge]] whose `files` is the real [[DesktopFiles]] (classpath resolution works) and whose GL is a no-op. */
  def headlessSge(): Sge =
    Sge(
      application = StubApplication,
      graphics = new NoopGraphics(),
      audio = new NoopAudio(),
      files = new DesktopFiles(),
      input = new NoopInput(),
      net = StubNet
    )

  /** Resolves a bundled test font from the classpath. */
  def fontHandle(name: String)(using Sge): FileHandle =
    summon[Sge].files.classpath(name)

  /** A caller-owned packer so the generator does not own the atlas and never touches GL (updateTextureRegions). 512x512 RGBA8888 is ample for the small character sets the suites request.
    */
  def scratchPacker()(using Sge): PixmapPacker =
    PixmapPacker(512, 512, Format.RGBA8888, 1, false, PixmapPacker.SkylineStrategy())

  private def unused(member: String): Nothing =
    throw new IllegalStateException(s"$member must not be touched by the headless FreeType tests")

  private object StubApplication extends Application {
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
    def clipboard:                                            sge.utils.Clipboard         = unused("Application.clipboard")
    def postRunnable(runnable:            Runnable):          Unit                        = ()
    def exit():                                               Unit                        = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
  }

  private object StubNet extends Net {
    import Net.*
    def httpClient:                                                                                     net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: net.ServerSocketHints): net.ServerSocket  = unused("Net.newServerSocket")
    def newServerSocket(protocol: Protocol, port:     Int, hints:   net.ServerSocketHints):             net.ServerSocket  = unused("Net.newServerSocket")
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: net.SocketHints):       net.Socket        = unused("Net.newClientSocket")
    def openURI(URI:              String):                                                              Boolean           = false
  }
}
