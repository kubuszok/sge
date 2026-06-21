/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Test coverage for ISS-561 (batch F): glutils FrameBuffer — the last core
 * glutils GL state machine that had zero unit tests. A FrameBuffer's whole
 * contract lives in GLFrameBuffer.build(): it generates a framebuffer object,
 * binds it, creates a color Texture (via GLOnlyTextureData -> glGenTexture /
 * glBindTexture / glTexImage2D), attaches that texture, then asks GL whether
 * the assembled FBO is complete. If GL reports anything other than
 * GL_FRAMEBUFFER_COMPLETE the constructor must tear everything down and throw.
 *
 * Every expected behaviour below is hand-traced from the original
 * com/badlogic/gdx/graphics/glutils/{FrameBuffer,GLFrameBuffer}.java
 * (original-src/libgdx). Java line numbers cited refer to those files; the
 * Scala port lines refer to the files under test.
 *
 *   - GLFrameBuffer.build() (Java GLFrameBuffer.java 133-310 / port
 *     GLFrameBuffer.scala 153-377):
 *       framebufferHandle = glGenFramebuffer()            (Java 145 / port 170)
 *       glBindFramebuffer(GL_FRAMEBUFFER, framebufferHandle) (Java 146 / port 171)
 *       no depth/stencil/packed render buffers for a plain color FBO (skipped)
 *       single-texture branch: createTexture(spec); textureAttachments.add;
 *         glBindTexture(texture.glTarget, handle)         (Java 194-197 / port 252-256)
 *       attachFrameBufferColorTexture(first)              (Java 222-223 / port 285-287)
 *         -> glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
 *            GL_TEXTURE_2D, handle, 0)                    (FrameBuffer.java 99-101 / port 110-111)
 *       glBindRenderbuffer(GL_RENDERBUFFER, 0); glBindTexture(target, 0) x texture
 *                                                          (Java 239-242 / port 301-304)
 *       result = glCheckFramebufferStatus(GL_FRAMEBUFFER) (Java 244 / port 306)
 *       glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebufferHandle=0)
 *                                                          (Java 280 / port 343)
 *       if result != GL_FRAMEBUFFER_COMPLETE -> dispose textures + render
 *         buffers, glDeleteFramebuffer(framebufferHandle), then throw an
 *         IllegalStateException whose message depends on `result`
 *                                                          (Java 282-307 / port 345-374)
 *   - FrameBuffer.createTexture (FrameBuffer.java 79-91 / port 88-105): builds a
 *     Texture backed by GLOnlyTextureData. Texture's own constructor calls
 *     glGenTexture() for the handle and then load(): glBindTexture +
 *     uploadImageData (-> glTexImage2D for the Custom GLOnlyTextureData) +
 *     filter/wrap (glTexParameteri). The FBO's handle for the color attachment
 *     is therefore the glGenTexture() value, NOT the glGenFramebuffer() value.
 *   - bind() (Java 369-371 / port 402-403): glBindFramebuffer(GL_FRAMEBUFFER,
 *     framebufferHandle).
 *   - begin() (Java 379-382 / port 406-409): bind() then setFrameBufferViewport()
 *     -> glViewport(0, 0, width, height).
 *   - end() (Java 389-403 / port 416-433): unbind() ->
 *     glBindFramebuffer(GL_FRAMEBUFFER, 0) then glViewport(0, 0,
 *     backBufferWidth, backBufferHeight).
 *   - close()/dispose() (Java 350-366 / port 380-399): disposeColorTexture(each)
 *     -> Texture.close() -> glDeleteTexture(handle); glDeleteRenderbuffer x3 (0
 *     for an unused handle); glDeleteFramebuffer(framebufferHandle).
 *
 * Headless strategy: a recording GL20 below extends GL20, delegates everything
 * to NoopGL20 via `export`, and overrides ONLY the FBO/texture state-machine
 * entry points. It returns success values so a FrameBuffer constructs cleanly:
 * a fixed framebuffer handle, a fixed texture handle, and a configurable
 * glCheckFramebufferStatus result (GL_FRAMEBUFFER_COMPLETE by default). Each
 * interesting call is appended to `calls` so we can pin the exact ORDER and the
 * exact handles, and the constructor-time status check.
 *
 * Mutations these tests catch (campaign requirement, stated per-test below):
 *   - "ignore glCheckFramebufferStatus result" (always treat as complete): the
 *     INCOMPLETE-status test fails because the constructor would NOT throw.
 *   - "bind to 0 in begin()/bind()" (use defaultFramebufferHandle instead of
 *     framebufferHandle): the begin()/bind() test fails because the recorded
 *     glBindFramebuffer argument would be 0, not the framebuffer handle 40.
 *   - "skip glDeleteFramebuffer on dispose": the close() test fails because the
 *     glDeleteFramebuffer(40) call would be missing.
 */
package sge
package graphics
package glutils

import sge.noop.{ NoopGL20, NoopGraphics }

class FrameBufferISS561Suite extends munit.FunSuite {

  // --- Fixed handles the recording GL hands back -----------------------------
  private val FramebufferHandle = 40
  private val TextureHandle     = 60

  // A small color FBO. 4x3 so width != height pins glViewport arguments.
  private val FbWidth  = 4
  private val FbHeight = 3

  /** A single recorded GL call we care about pinning: the GL function name plus the integer arguments (handle / target / attachment).
    */
  final private case class Call(name: String, args: List[Int])

  /** Recording GL20 that lets a FrameBuffer construct successfully.
    *
    * @param framebufferStatus
    *   value returned by glCheckFramebufferStatus — GL_FRAMEBUFFER_COMPLETE to build successfully, anything else (e.g. GL_FRAMEBUFFER_UNSUPPORTED) to drive the constructor's error/cleanup path.
    */
  final private class RecordingGL20(framebufferStatus: Int = GL20.GL_FRAMEBUFFER_COMPLETE) extends GL20 {

    val calls: scala.collection.mutable.ListBuffer[Call] = scala.collection.mutable.ListBuffer.empty

    // Delegate every un-overridden method to the no-op implementation. The
    // overridden glGen*/glBind*/glFramebuffer*/glCheck*/glDelete* below shadow
    // the exported ones.
    private val underlying: GL20 = NoopGL20
    export underlying.{
      glBindFramebuffer as _,
      glBindRenderbuffer as _,
      glBindTexture as _,
      glCheckFramebufferStatus as _,
      glDeleteFramebuffer as _,
      glDeleteRenderbuffer as _,
      glDeleteTexture as _,
      glFramebufferTexture2D as _,
      glGenFramebuffer as _,
      glGenRenderbuffer as _,
      glGenTexture as _,
      glViewport as _,
      *
    }

    override def glGenFramebuffer(): Int = {
      calls += Call("glGenFramebuffer", Nil)
      FramebufferHandle
    }

    override def glGenRenderbuffer(): Int = {
      // Distinct from the framebuffer/texture handles so a stray renderbuffer
      // would be visible; a plain color FBO must NOT call this.
      calls += Call("glGenRenderbuffer", Nil)
      50
    }

    override def glGenTexture(): Int = {
      calls += Call("glGenTexture", Nil)
      TextureHandle
    }

    override def glBindFramebuffer(target: Int, framebuffer: Int): Unit =
      calls += Call("glBindFramebuffer", List(target, framebuffer))

    override def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit =
      calls += Call("glBindRenderbuffer", List(target, renderbuffer))

    override def glBindTexture(target: TextureTarget, texture: Int): Unit =
      calls += Call("glBindTexture", List(target.toInt, texture))

    override def glFramebufferTexture2D(target: Int, attachment: Int, textarget: TextureTarget, texture: Int, level: Int): Unit =
      calls += Call("glFramebufferTexture2D", List(target, attachment, textarget.toInt, texture, level))

    override def glCheckFramebufferStatus(target: Int): Int = {
      calls += Call("glCheckFramebufferStatus", List(target))
      framebufferStatus
    }

    override def glViewport(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit =
      calls += Call("glViewport", List(x.toInt, y.toInt, width.toInt, height.toInt))

    override def glDeleteFramebuffer(framebuffer: Int): Unit =
      calls += Call("glDeleteFramebuffer", List(framebuffer))

    override def glDeleteRenderbuffer(renderbuffer: Int): Unit =
      calls += Call("glDeleteRenderbuffer", List(renderbuffer))

    override def glDeleteTexture(texture: Int): Unit =
      calls += Call("glDeleteTexture", List(texture))
  }

  /** A NoopGraphics whose back buffer is a fixed, distinct size so end()'s default-viewport restore (backBufferWidth/Height) is pinned to a value other than the FBO's own width/height.
    */
  private def makeSge(glImpl: GL20): Sge =
    SgeTestFixture.testSge(graphics = new NoopGraphics(noopWidth = 800, noopHeight = 600) {
      override def gl20: GL20 = glImpl
    })

  /** Build a plain color-only FrameBuffer using addColorTextureAttachment (raw GL enums) rather than addBasicColorTextureAttachment, so the test never routes through Gdx2DPixmap native format
    * translation — only the GL state machine under test runs.
    */
  private def makeBuilder(using Sge): GLFrameBuffer.FrameBufferBuilder = {
    val builder = GLFrameBuffer.FrameBufferBuilder(Pixels(FbWidth), Pixels(FbHeight))
    builder.addColorTextureAttachment(GL20.GL_RGBA, PixelFormat(GL20.GL_RGBA), DataType(GL20.GL_UNSIGNED_BYTE))
    builder
  }

  // --- successful build: handles, order, dimensions, color texture ------------

  test("ISS561: a successful FrameBuffer build generates the FBO, attaches the color texture, and checks status COMPLETE") {
    val gl    = new RecordingGL20() // status defaults to GL_FRAMEBUFFER_COMPLETE
    given Sge = makeSge(gl)

    val fb = new FrameBuffer(makeBuilder)

    // The framebuffer handle is the glGenFramebuffer() value, not the texture's.
    assertEquals(
      fb.getFramebufferHandle,
      FramebufferHandle,
      "framebufferHandle must be the glGenFramebuffer() result (port GLFrameBuffer.scala line 170)"
    )
    assertEquals(fb.width.toInt, FbWidth, "width comes from the builder")
    assertEquals(fb.height.toInt, FbHeight, "height comes from the builder")

    // The color texture exists and is the Texture created from the attachment
    // spec; its GL handle is the glGenTexture() value.
    assertEquals(fb.textureAttachments.size, 1, "exactly one color texture attachment was created")
    assertEquals(
      fb.colorBufferTexture.textureObjectHandle.toInt,
      TextureHandle,
      "the color texture's GL handle is the glGenTexture() result"
    )

    // The salient FBO state-machine calls, in the order build() issues them.
    // (Texture filter/wrap glTexParameteri / glTexImage2D are intentionally not
    // recorded — they are not part of the FBO contract; we pin the FBO frame.)
    val fboFrame = gl.calls.toList.filter(c =>
      c.name == "glGenFramebuffer" || c.name == "glGenTexture" ||
        c.name == "glFramebufferTexture2D" || c.name == "glCheckFramebufferStatus" ||
        (c.name == "glBindFramebuffer")
    )
    assertEquals(
      fboFrame,
      List(
        Call("glGenFramebuffer", Nil),
        Call("glBindFramebuffer", List(GL20.GL_FRAMEBUFFER, FramebufferHandle)),
        Call("glGenTexture", Nil),
        Call(
          "glFramebufferTexture2D",
          List(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, TextureTarget.Texture2D.toInt, TextureHandle, 0)
        ),
        Call("glCheckFramebufferStatus", List(GL20.GL_FRAMEBUFFER)),
        Call("glBindFramebuffer", List(GL20.GL_FRAMEBUFFER, 0))
      ),
      "build() must gen+bind the FBO, gen the color texture, attach it to COLOR_ATTACHMENT0, check status, then rebind the default framebuffer (0)"
    )

    // A plain color FBO has no depth/stencil render buffers.
    assert(!gl.calls.exists(_.name == "glGenRenderbuffer"), "a color-only FrameBuffer must not allocate any renderbuffer")
  }

  // --- INCOMPLETE status -> constructor throws AND cleans up (HIGH VALUE) ------

  test("ISS561: a non-COMPLETE glCheckFramebufferStatus makes the FrameBuffer constructor throw and tear the FBO down") {
    // GL reports GL_FRAMEBUFFER_UNSUPPORTED. build() (port lines 345-374) must
    // NOT register the buffer; it disposes the color texture, deletes the
    // framebuffer object, and throws the documented IllegalStateException.
    val gl    = new RecordingGL20(framebufferStatus = GL20.GL_FRAMEBUFFER_UNSUPPORTED)
    given Sge = makeSge(gl)

    val ex = intercept[IllegalStateException] {
      new FrameBuffer(makeBuilder)
    }
    assert(
      ex.getMessage.contains("unsupported combination of formats"),
      s"GL_FRAMEBUFFER_UNSUPPORTED must surface the documented message (port line 369-370); got: ${ex.getMessage}"
    )

    // Cleanup: the color texture is disposed (glDeleteTexture(60)) and the
    // framebuffer object is deleted (glDeleteFramebuffer(40)). Both must run
    // BEFORE the throw — a mutation that "ignores the status result" would skip
    // both this cleanup and the throw entirely.
    assert(
      gl.calls.contains(Call("glDeleteTexture", List(TextureHandle))),
      s"incomplete build must dispose the color texture (glDeleteTexture(60)); calls were: ${gl.calls.toList}"
    )
    assert(
      gl.calls.contains(Call("glDeleteFramebuffer", List(FramebufferHandle))),
      s"incomplete build must delete the framebuffer object (glDeleteFramebuffer(40)); calls were: ${gl.calls.toList}"
    )
    // And it must have actually consulted the status (the call we made fail).
    assert(
      gl.calls.contains(Call("glCheckFramebufferStatus", List(GL20.GL_FRAMEBUFFER))),
      "build() must query glCheckFramebufferStatus(GL_FRAMEBUFFER) — this is the value the throw hinges on"
    )
    // CATCHES MUTATION "ignore glCheckFramebufferStatus result": if build()
    // treated every status as complete, intercept[IllegalStateException] above
    // would fail (no throw) and the FBO would be left registered, not deleted.
  }

  // --- begin()/bind(): bind the FBO + set its viewport ------------------------

  test("ISS561: begin() binds the framebuffer handle and sets the viewport to the FBO size; bind() binds without a viewport") {
    val gl    = new RecordingGL20()
    given Sge = makeSge(gl)
    val fb    = new FrameBuffer(makeBuilder)
    gl.calls.clear() // drop construction noise; isolate begin()/bind()

    fb.begin()
    assertEquals(
      gl.calls.toList,
      List(
        Call("glBindFramebuffer", List(GL20.GL_FRAMEBUFFER, FramebufferHandle)),
        Call("glViewport", List(0, 0, FbWidth, FbHeight))
      ),
      "begin() must bind(framebufferHandle=40) then glViewport(0, 0, 4, 3) — NOT bind 0 (port lines 402-413)"
    )
    // CATCHES MUTATION "bind to 0 in begin()": if bind() used
    // defaultFramebufferHandle (0) instead of framebufferHandle, the recorded
    // argument would be 0 and this assertion would fail.

    gl.calls.clear()
    fb.bind()
    assertEquals(
      gl.calls.toList,
      List(Call("glBindFramebuffer", List(GL20.GL_FRAMEBUFFER, FramebufferHandle))),
      "bind() alone must only glBindFramebuffer(framebufferHandle), no viewport (port line 402-403)"
    )
  }

  // --- end()/unbind(): rebind default framebuffer + restore default viewport --

  test("ISS561: end() unbinds to the default framebuffer (0) and restores the back-buffer viewport") {
    val gl    = new RecordingGL20()
    given Sge = makeSge(gl)
    val fb    = new FrameBuffer(makeBuilder)
    gl.calls.clear()

    fb.end()
    assertEquals(
      gl.calls.toList,
      List(
        Call("glBindFramebuffer", List(GL20.GL_FRAMEBUFFER, 0)),
        Call("glViewport", List(0, 0, 800, 600))
      ),
      "end() must bind the default framebuffer (0) then glViewport over the full back buffer 800x600 (port lines 416-433)"
    )
  }

  // --- dispose/close: delete texture + (zeroed) renderbuffers + framebuffer ---

  test("ISS561: close() disposes the color texture and deletes the framebuffer object") {
    val gl    = new RecordingGL20()
    given Sge = makeSge(gl)
    val fb    = new FrameBuffer(makeBuilder)
    gl.calls.clear()

    fb.close()

    // Java dispose() (port close() lines 380-399): for each texture
    // disposeColorTexture (Texture.close() -> glDeleteTexture(handle)); three
    // glDeleteRenderbuffer (depthStencilPacked/depth/stencil, all 0 here since
    // unused); then glDeleteFramebuffer(framebufferHandle).
    assertEquals(
      gl.calls.toList,
      List(
        Call("glDeleteTexture", List(TextureHandle)),
        Call("glDeleteRenderbuffer", List(0)),
        Call("glDeleteRenderbuffer", List(0)),
        Call("glDeleteRenderbuffer", List(0)),
        Call("glDeleteFramebuffer", List(FramebufferHandle))
      ),
      "close() must delete the color texture (60), the three (unused, 0) renderbuffers, then the framebuffer object (40) in this order"
    )
    // CATCHES MUTATION "skip glDeleteFramebuffer on dispose": dropping the
    // glDeleteFramebuffer(framebufferHandle) line would make this exact-list
    // assertion fail (the final Call would be missing).
  }
}
