/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-732 (BitmapFont page images always resolved internal,
 * never relative to the .fnt FileHandle's FileType).
 *
 * Original reference: com/badlogic/gdx/graphics/g2d/BitmapFont.java
 * (original-src/libgdx). The page-loading branch of the
 * BitmapFont(BitmapFontData, Array<TextureRegion>, boolean) constructor,
 * lines 163-169:
 *
 *   FileHandle file;
 *   if (data.fontFile == null)
 *     file = Gdx.files.internal(data.imagePaths[i]);
 *   else
 *     file = Gdx.files.getFileHandle(data.imagePaths[i], data.fontFile.type());
 *   regions.add(new TextureRegion(new Texture(file, false)));
 *
 * i.e. when the BitmapFontData was loaded from a NON-null .fnt handle, each
 * page image is resolved with getFileHandle(path, fontFile.type()) — RELATIVE
 * to the .fnt's FileType (External / Local / Absolute) — and only falls back
 * to Gdx.files.internal(...) when data.fontFile == null.
 *
 * The port (BitmapFont.scala loadRegions) unconditionally does
 * `TextureRegion(Texture(paths(i)))`, and Texture(String) resolves via
 * `Sge().files.internal(internalPath)` (Texture.scala:57). So a font built
 * from an External .fnt handle looks for its .png pages on the CLASSPATH
 * instead of next to the font file.
 *
 * This test is written by the reproducer agent and MUST NOT be modified by
 * the fixer: it encodes the original Java semantics (BitmapFont.java:164-168),
 * not the port's.
 */
package sge
package graphics
package g2d

import java.io.{ ByteArrayInputStream, File, InputStream }

import scala.collection.mutable.ListBuffer

import sge.files.{ FileHandle, FileType }
import lowlevel.Nullable
import lowlevel.util.DynamicArray

class BitmapFontPagesIss732Suite extends munit.FunSuite {

  /** One recorded attempt to resolve a page image path through the Files API. */
  final private case class Resolution(method: String, path: String, fileType: Option[FileType])

  /** Marker thrown from the recording Files to abort BEFORE any real texture / GL work, at the exact point where the page FileHandle is resolved.
    */
  final private class ResolutionCaptured(val resolution: Resolution) extends RuntimeException(resolution.toString)

  /** A Files that records how the page image path is resolved. `internal` corresponds to the buggy classpath resolution; `getFileHandle` corresponds to the fontFile-relative resolution the original
    * performs (BitmapFont.java line 168). Both abort immediately via ResolutionCaptured so the test never needs a real PNG or GL context.
    */
  final private class RecordingFiles(val calls: ListBuffer[Resolution]) extends Files {
    def getFileHandle(path: String, fileType: files.FileType): files.FileHandle = {
      val r = Resolution("getFileHandle", path, Some(fileType))
      calls += r
      throw new ResolutionCaptured(r)
    }
    def internal(path: String): files.FileHandle = {
      val r = Resolution("internal", path, None)
      calls += r
      throw new ResolutionCaptured(r)
    }
    def classpath(path: String):    files.FileHandle = throw new UnsupportedOperationException
    def external(path:  String):    files.FileHandle = throw new UnsupportedOperationException
    def absolute(path:  String):    files.FileHandle = throw new UnsupportedOperationException
    def local(path:     String):    files.FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:        String           = ""
    def isExternalStorageAvailable: Boolean          = false
    def localStoragePath:           String           = ""
    def isLocalStorageAvailable:    Boolean          = false
  }

  /** An External .fnt handle whose bytes are supplied in-memory (no filesystem / classpath IO). read() is the only IO the BitmapFontData loader performs; path/parent/child arithmetic is pure File
    * manipulation. The virtual dir "/virtual/fonts" makes the fontFile-relative page path deterministic: imagePaths[0] = parent().child("page.png").path = "/virtual/fonts/page.png".
    */
  final private class InMemoryFntFile(content: String) extends FileHandle(new File("/virtual/fonts/font.fnt"), FileType.External) {
    override def read(): InputStream = new ByteArrayInputStream(content.getBytes("UTF-8"))
  }

  /** Minimal but valid .fnt: one page, one glyph (so BitmapFontData.load can synthesize the space/x/cap glyphs without hitting firstGlyph on an empty font).
    */
  private val fntContent: String =
    List(
      "info face=\"T\" size=16 padding=0,0,0,0 spacing=1,1",
      "common lineHeight=18 base=14 scaleW=64 scaleH=64 pages=1 packed=0",
      "page id=0 file=\"page.png\"",
      "chars count=1",
      "char id=97 x=0 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=9 page=0"
    ).mkString("\n")

  test(
    "ISS-732: page image of a font loaded from an External .fnt handle is resolved relative to the .fnt's FileType, not from the classpath/internal"
  ) {
    val calls = ListBuffer.empty[Resolution]
    given Sge = SgeTestFixture.testSge(files = new RecordingFiles(calls))

    // BitmapFontData loaded from a NON-internal (External) .fnt handle. Its
    // fontFile field is populated (data.fontFile != null in Java terms), which
    // is exactly the branch that must resolve pages via getFileHandle(...,
    // fontFile.type()) — BitmapFont.java lines 165-168.
    val fontFile = new InMemoryFntFile(fntContent)
    val data     = BitmapFontData(Nullable(fontFile), false)

    // Precondition: the loader recorded the page path relative to the .fnt dir.
    assertEquals(
      data.imagePaths.map(_(0)).getOrElse(""),
      "/virtual/fonts/page.png",
      "precondition: BitmapFontData.load resolves imagePaths relative to the fontFile's parent"
    )

    // Constructing the BitmapFont with no explicit regions triggers
    // loadRegions(), which resolves each page image. We abort at the resolution
    // point (RecordingFiles throws ResolutionCaptured) so no PNG/GL is needed.
    val captured =
      try {
        val _ = new BitmapFont(data, Nullable.empty[DynamicArray[TextureRegion]], true)
        None
      } catch {
        case rc: ResolutionCaptured => Some(rc.resolution)
      }

    assert(
      captured.isDefined,
      "loadRegions must attempt to resolve the page image through the Files API"
    )
    val res = captured.get

    // The observable: the page must be resolved via getFileHandle(path,
    // fontFile.type()) with the .fnt's External FileType — NOT via internal()
    // (classpath). BitmapFont.java lines 164-168:
    //   if (data.fontFile == null) file = Gdx.files.internal(...);
    //   else file = Gdx.files.getFileHandle(..., data.fontFile.type());
    // The port always takes the internal() branch (Texture(String) ->
    // Sge().files.internal, Texture.scala:57), so `res.method` comes out
    // "internal" and `res.fileType` empty, failing the two assertions below.
    assertEquals(
      res.method,
      "getFileHandle",
      "page of an External-loaded font must be resolved via getFileHandle(path, fontFile.type()), not internal()/classpath (BitmapFont.java:164-168)"
    )
    assertEquals(
      res.fileType,
      Some(FileType.External),
      "page must be resolved with the .fnt handle's External FileType, not the classpath (BitmapFont.java:164-168)"
    )
    assertEquals(
      res.path,
      "/virtual/fonts/page.png",
      "page path must be the fontFile-relative image path"
    )
  }
}
