/*
 * SGE — Native FFI Wiring Validation
 *
 * Exercises every native C ABI endpoint used by SGE on Scala Native to verify
 * correct wiring (symbol resolution + ABI compatibility). Catches:
 *   - Missing symbols (link-time)
 *   - ABI mismatches: wrong parameter types/order (runtime SIGSEGV)
 *   - Pointer/offset calculations (e.g. NativeGlHelper.bufPtr)
 *
 * NOT a behavioral test — just verifies that calling each function doesn't crash
 * and returns a plausible value.
 *
 * Run: sbt 'sge-it-native-ffi/run'  or  just it-native-ffi
 */
package sge
package it
package nativeffi

import java.nio.{ ByteBuffer, ByteOrder, FloatBuffer, IntBuffer }

import sge.platform.{ AudioOpsNative, GlOpsNative, PlatformOps, WindowingOps, WindowingOpsNative }
import sge.Pixels
import sge.graphics.{ AngleGL32Native, GL20, GL30 }
import sge.graphics.{ BlendEquation, BlendFactor, BufferTarget, BufferUsage, ClearMask }
import sge.graphics.{ CompareFunc, CullFace, EnableCap, ShaderType, StencilOp, TextureTarget }

object NativeFfiValidation {

  private var passed  = 0
  private var failed  = 0
  private var skipped = 0

  private def check(name: String)(body: => Unit): Unit =
    try {
      body
      passed += 1
      System.out.println(s"  PASS  $name")
    } catch {
      case e: Throwable =>
        failed += 1
        System.err.println(s"  FAIL  $name: ${e.getClass.getName}: ${e.getMessage}")
    }

  private def skip(name: String, reason: String): Unit = {
    skipped += 1
    System.out.println(s"  SKIP  $name ($reason)")
  }

  def main(args: Array[String]): Unit = {
    System.out.println("=== SGE Native FFI Wiring Validation ===\n")

    // ─── 1. Buffer ops (no window/GL needed) ────────────────────────────
    System.out.println("[sge_native_ops] Buffer operations")
    testBufferOps()

    // ─── 2. ETC1 ops (no window/GL needed) ──────────────────────────────
    System.out.println("\n[sge_native_ops] ETC1 codec")
    testETC1Ops()

    // ─── 3. NativeGlHelper.bufPtr (no window/GL needed) ─────────────────
    System.out.println("\n[NativeGlHelper] Buffer pointer extraction")
    testNativeGlHelper()

    // ─── 4. Audio engine (no window/GL needed) ──────────────────────────
    System.out.println("\n[sge_audio] Audio engine")
    testAudio()

    // ─── 5. GLFW + EGL + GL (needs window + GPU) ───────────────────────
    System.out.println("\n[glfw3 + EGL + GLESv2] Windowing + GL")
    testGlfwEglGl()

    // ─── Summary ────────────────────────────────────────────────────────
    val total = passed + failed + skipped
    System.out.println(s"\n=== Results: $passed passed, $failed failed, $skipped skipped (of $total) ===")
    if (failed > 0) {
      System.err.println(s"VALIDATION FAILED: $failed endpoint(s) broken")
      sys.exit(1)
    } else {
      System.out.println("All native FFI endpoints validated successfully.")
    }
  }

  // ═══════════════════════════════════════════════════════════════════════
  // Buffer ops
  // ═══════════════════════════════════════════════════════════════════════

  private def testBufferOps(): Unit = {
    val buf = PlatformOps.buffer

    check("newDisposableByteBuffer + freeMemory") {
      val bb = buf.newDisposableByteBuffer(256)
      assert(bb != null, "ByteBuffer is null")
      assert(bb.isDirect, "ByteBuffer is not direct")
      assert(bb.capacity() == 256, s"capacity=${bb.capacity()}")
      buf.freeMemory(bb)
    }

    check("getBufferAddress (unsupported on Native)") {
      val bb = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder())
      try {
        buf.getBufferAddress(bb)
        assert(false, "expected UnsupportedOperationException")
      } catch {
        case _: UnsupportedOperationException => // expected
      }
    }

    check("copy(byte[])") {
      val src = Array[Byte](10, 20, 30, 40, 50)
      val dst = new Array[Byte](5)
      buf.copy(src, 1, dst, 0, 3)
      assert(dst(0) == 20 && dst(1) == 30 && dst(2) == 40, s"dst=${dst.mkString(",")}")
    }

    check("copy(float[])") {
      val src = Array[Float](1.0f, 2.0f, 3.0f, 4.0f)
      val dst = new Array[Float](4)
      buf.copy(src, 0, dst, 0, 4)
      assert(dst(0) == 1.0f && dst(3) == 4.0f, s"dst=${dst.mkString(",")}")
    }

    check("transformV4M4 (identity)") {
      val data   = Array[Float](1.0f, 2.0f, 3.0f, 1.0f)
      val matrix = identityMatrix4()
      buf.transformV4M4(data, 4, 1, matrix, 0)
      assertFloatEq(data(0), 1.0f, "x"); assertFloatEq(data(1), 2.0f, "y")
      assertFloatEq(data(2), 3.0f, "z"); assertFloatEq(data(3), 1.0f, "w")
    }

    check("transformV3M4 (identity)") {
      val data   = Array[Float](5.0f, 6.0f, 7.0f)
      val matrix = identityMatrix4()
      buf.transformV3M4(data, 3, 1, matrix, 0)
      assertFloatEq(data(0), 5.0f, "x"); assertFloatEq(data(1), 6.0f, "y")
      assertFloatEq(data(2), 7.0f, "z")
    }

    check("transformV2M4 (identity)") {
      val data   = Array[Float](8.0f, 9.0f)
      val matrix = identityMatrix4()
      buf.transformV2M4(data, 2, 1, matrix, 0)
      assertFloatEq(data(0), 8.0f, "x"); assertFloatEq(data(1), 9.0f, "y")
    }

    check("transformV3M3 (identity)") {
      val data   = Array[Float](1.0f, 2.0f, 3.0f)
      val matrix = identityMatrix3()
      buf.transformV3M3(data, 3, 1, matrix, 0)
      assertFloatEq(data(0), 1.0f, "x"); assertFloatEq(data(1), 2.0f, "y")
      assertFloatEq(data(2), 3.0f, "z")
    }

    check("transformV2M3 (identity)") {
      val data   = Array[Float](4.0f, 5.0f)
      val matrix = identityMatrix3()
      buf.transformV2M3(data, 2, 1, matrix, 0)
      assertFloatEq(data(0), 4.0f, "x"); assertFloatEq(data(1), 5.0f, "y")
    }

    check("find (exact)") {
      val vertices = Array[Float](1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f)
      val vertex   = Array[Float](4.0f, 5.0f, 6.0f)
      val idx      = buf.find(vertex, 0, 3, vertices, 0, 2)
      assert(idx == 1L, s"expected 1, got $idx")
    }

    check("find (epsilon)") {
      val vertices = Array[Float](1.0f, 2.0f, 3.0f, 4.001f, 5.001f, 6.001f)
      val vertex   = Array[Float](4.0f, 5.0f, 6.0f)
      val idx      = buf.find(vertex, 0, 3, vertices, 0, 2, 0.01f)
      assert(idx == 1L, s"expected 1, got $idx")
    }
  }

  // ═══════════════════════════════════════════════════════════════════════
  // ETC1 ops
  // ═══════════════════════════════════════════════════════════════════════

  private def testETC1Ops(): Unit = {
    val etc1 = PlatformOps.etc1

    check("getCompressedDataSize") {
      val size = etc1.getCompressedDataSize(16, 16)
      assert(size > 0, s"size=$size")
    }

    check("formatHeader + getWidthPKM + getHeightPKM") {
      val header = new Array[Byte](16)
      etc1.formatHeader(header, 0, 32, 64)
      assert(etc1.getWidthPKM(header, 0) == 32, s"width=${etc1.getWidthPKM(header, 0)}")
      assert(etc1.getHeightPKM(header, 0) == 64, s"height=${etc1.getHeightPKM(header, 0)}")
    }

    check("isValidPKM (valid)") {
      val header = new Array[Byte](16)
      etc1.formatHeader(header, 0, 4, 4)
      assert(etc1.isValidPKM(header, 0), "expected valid PKM header")
    }

    check("isValidPKM (invalid)") {
      val header = new Array[Byte](16)
      assert(!etc1.isValidPKM(header, 0), "expected invalid PKM header")
    }

    check("encodeImage + decodeImage roundtrip") {
      val w         = 4; val h = 4; val pixelSize = 3
      val imageData = new Array[Byte](w * h * pixelSize)
      // Fill with a pattern
      for (i <- imageData.indices) imageData(i) = ((i * 37) & 0xff).toByte
      val compressed = etc1.encodeImage(imageData, 0, w, h, pixelSize)
      assert(compressed.length > 0, "compressed is empty")
      val decoded = new Array[Byte](w * h * pixelSize)
      etc1.decodeImage(compressed, 0, decoded, 0, w, h, pixelSize)
      // ETC1 is lossy — just check it doesn't crash and returns non-zero data
      assert(decoded.exists(_ != 0), "decoded is all zeros")
    }

    check("encodeImagePKM") {
      val w         = 4; val h = 4; val pixelSize = 3
      val imageData = new Array[Byte](w * h * pixelSize)
      for (i <- imageData.indices) imageData(i) = ((i * 37) & 0xff).toByte
      val result = etc1.encodeImagePKM(imageData, 0, w, h, pixelSize)
      assert(result.length >= 16, s"too small: ${result.length}")
      assert(etc1.isValidPKM(result, 0), "PKM header not valid")
    }
  }

  // ═══════════════════════════════════════════════════════════════════════
  // NativeGlHelper (buffer pointer extraction)
  // ═══════════════════════════════════════════════════════════════════════

  private def testNativeGlHelper(): Unit = {
    // NativeGlHelper is private[graphics], so we can't test it directly.
    // Instead we verify it indirectly: GL calls that pass IntBuffer/FloatBuffer
    // to @extern functions use NativeGlHelper.bufPtr internally. If bufPtr returns
    // a wrong pointer, those GL calls will SIGSEGV or corrupt memory.
    //
    // The GL tests in testGlfwEglGl() exercise bufPtr through:
    //   glGenTextures(IntBuffer), glGetShaderiv(IntBuffer), glBufferData(FloatBuffer),
    //   glUniformMatrix4fv(FloatBuffer), glGetIntegerv(IntBuffer), etc.
    //
    // Here we test the buffer allocation that feeds into those GL calls.

    check("direct ByteBuffer allocation (backing NativeGlHelper)") {
      val bb = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder())
      assert(bb.isDirect, "not direct")
      assert(bb.capacity() == 64, s"capacity=${bb.capacity()}")
      // Write and read back — verifies the buffer's native memory is accessible
      bb.putFloat(0, 42.0f)
      assert(bb.getFloat(0) == 42.0f, "float read/write mismatch")
      bb.putInt(4, 0xdeadbeef)
      assert(bb.getInt(4) == 0xdeadbeef, "int read/write mismatch")
    }

    check("direct FloatBuffer via ByteBuffer (used by GL uniform/buffer calls)") {
      val bb = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder())
      val fb = bb.asFloatBuffer()
      assert(fb.isDirect, "FloatBuffer not direct")
      fb.put(0, 1.0f); fb.put(1, 2.0f); fb.put(2, 3.0f)
      assert(fb.get(0) == 1.0f && fb.get(1) == 2.0f && fb.get(2) == 3.0f, "FloatBuffer read/write mismatch")
    }

    check("direct IntBuffer via ByteBuffer (used by GL gen/get calls)") {
      val bb = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder())
      val ib = bb.asIntBuffer()
      assert(ib.isDirect, "IntBuffer not direct")
      ib.put(0, 99); ib.put(1, 100)
      assert(ib.get(0) == 99 && ib.get(1) == 100, "IntBuffer read/write mismatch")
    }

    check("heap-allocated IntBuffer (used by GL gen calls)") {
      // Many GL calls in SGE use IntBuffer.allocate() (heap), not direct buffers.
      // NativeGlHelper.bufPtr must handle both direct and heap-backed buffers.
      val ib = IntBuffer.allocate(4)
      ib.put(0, 1); ib.put(1, 2); ib.put(2, 3); ib.put(3, 4)
      assert(ib.get(0) == 1 && ib.get(3) == 4, "heap IntBuffer read/write mismatch")
    }
  }

  // ═══════════════════════════════════════════════════════════════════════
  // Audio engine
  // ═══════════════════════════════════════════════════════════════════════

  private def testAudio(): Unit = {
    check("initEngine + shutdownEngine") {
      val engine = AudioOpsNative.initEngine(16, 4096, 3)
      assert(engine != 0L, "engine handle is 0")
      AudioOpsNative.shutdownEngine(engine)
    }

    check("initEngine + updateEngine + shutdownEngine") {
      val engine = AudioOpsNative.initEngine(16, 4096, 3)
      AudioOpsNative.updateEngine(engine)
      AudioOpsNative.shutdownEngine(engine)
    }

    check("createSound + playSound + stopSound + disposeSound") {
      val engine = AudioOpsNative.initEngine(16, 4096, 3)
      // Create minimal PCM: 100 samples of silence, mono, 16-bit, 44100Hz
      val pcmData = new Array[Byte](200)
      val sound   = AudioOpsNative.createSound(engine, pcmData, 1, 16, 44100)
      assert(sound != 0L, "sound handle is 0")
      // playSound may return 0 for degenerate PCM — wiring validated by not crashing
      val instance = AudioOpsNative.playSound(sound, 1.0f, 1.0f, 0.0f, false)
      if (instance != 0L) AudioOpsNative.stopSound(instance)
      AudioOpsNative.disposeSound(sound)
      AudioOpsNative.shutdownEngine(engine)
    }

    check("sound volume/pitch/pan/looping setters") {
      val engine  = AudioOpsNative.initEngine(16, 4096, 3)
      val pcmData = new Array[Byte](200)
      val sound   = AudioOpsNative.createSound(engine, pcmData, 1, 16, 44100)
      val inst    = AudioOpsNative.playSound(sound, 1.0f, 1.0f, 0.0f, false)
      AudioOpsNative.setSoundVolume(inst, 0.8f)
      AudioOpsNative.setSoundPitch(inst, 1.5f)
      AudioOpsNative.setSoundPan(inst, -0.5f, 0.8f)
      AudioOpsNative.setSoundLooping(inst, true)
      AudioOpsNative.stopSound(inst)
      AudioOpsNative.disposeSound(sound)
      AudioOpsNative.shutdownEngine(engine)
    }

    check("pauseSound + resumeSound") {
      val engine  = AudioOpsNative.initEngine(16, 4096, 3)
      val pcmData = new Array[Byte](200)
      val sound   = AudioOpsNative.createSound(engine, pcmData, 1, 16, 44100)
      val inst    = AudioOpsNative.playSound(sound, 1.0f, 1.0f, 0.0f, false)
      AudioOpsNative.pauseSound(inst)
      AudioOpsNative.resumeSound(inst)
      AudioOpsNative.stopSound(inst)
      AudioOpsNative.disposeSound(sound)
      AudioOpsNative.shutdownEngine(engine)
    }

    check("stopAllInstances + pauseAllInstances + resumeAllInstances") {
      val engine  = AudioOpsNative.initEngine(16, 4096, 3)
      val pcmData = new Array[Byte](200)
      val sound   = AudioOpsNative.createSound(engine, pcmData, 1, 16, 44100)
      AudioOpsNative.playSound(sound, 1.0f, 1.0f, 0.0f, false)
      AudioOpsNative.pauseAllInstances(sound)
      AudioOpsNative.resumeAllInstances(sound)
      AudioOpsNative.stopAllInstances(sound)
      AudioOpsNative.disposeSound(sound)
      AudioOpsNative.shutdownEngine(engine)
    }

    check("createAudioDevice + writeAudioDevice + disposeAudioDevice") {
      val engine = AudioOpsNative.initEngine(16, 4096, 3)
      val device = AudioOpsNative.createAudioDevice(engine, 44100, false)
      assert(device != 0L, "device handle is 0")
      val silence = new Array[Byte](4096)
      AudioOpsNative.writeAudioDevice(device, silence, 0, silence.length)
      AudioOpsNative.setAudioDeviceVolume(device, 0.5f)
      AudioOpsNative.pauseAudioDevice(device)
      AudioOpsNative.resumeAudioDevice(device)
      val latency = AudioOpsNative.getAudioDeviceLatency(device)
      assert(latency >= 0, s"latency=$latency")
      AudioOpsNative.disposeAudioDevice(device)
      AudioOpsNative.shutdownEngine(engine)
    }

    check("getAvailableOutputDevices") {
      val engine  = AudioOpsNative.initEngine(16, 4096, 3)
      val devices = AudioOpsNative.getAvailableOutputDevices(engine)
      assert(devices != null, "devices is null")
      // devices is Array[String] — no manual free needed (handled internally)
      AudioOpsNative.shutdownEngine(engine)
    }
  }

  // ═══════════════════════════════════════════════════════════════════════
  // GLFW + EGL + GL
  // ═══════════════════════════════════════════════════════════════════════

  private def testGlfwEglGl(): Unit = {
    // GLFW init
    check("glfwInit") {
      val ok = WindowingOpsNative.init()
      assert(ok, "glfwInit failed")
    }

    check("getPlatform") {
      val plat = WindowingOpsNative.getPlatform()
      assert(plat != 0, s"platform=$plat")
    }

    // Create a hidden window
    var windowHandle = 0L
    check("createWindow (hidden, no API)") {
      WindowingOpsNative.setWindowHint(WindowingOps.GLFW_VISIBLE, WindowingOps.GLFW_FALSE)
      WindowingOpsNative.setWindowHint(WindowingOps.GLFW_CLIENT_API, WindowingOps.GLFW_NO_API) // EGL provides GL context
      windowHandle = WindowingOpsNative.createWindow(320, 240, "SGE FFI Validation")
      assert(windowHandle != 0L, "createWindow returned 0")
    }

    if (windowHandle == 0L) {
      skip("EGL/GL tests", "no window created")
      return
    }

    // Window queries
    check("getWindowSize") {
      val (w, h) = WindowingOpsNative.getWindowSize(windowHandle)
      assert(w > 0 && h > 0, s"size=${w}x${h}")
    }

    check("getFramebufferSize") {
      val (w, h) = WindowingOpsNative.getFramebufferSize(windowHandle)
      assert(w > 0 && h > 0, s"fbSize=${w}x${h}")
    }

    check("getWindowPos") {
      val (_, _) = WindowingOpsNative.getWindowPos(windowHandle)
      // Just verify it doesn't crash — position can be anything
    }

    check("setWindowTitle") {
      WindowingOpsNative.setWindowTitle(windowHandle, "FFI Test Window")
    }

    check("windowShouldClose") {
      val close = WindowingOpsNative.windowShouldClose(windowHandle)
      assert(!close, "window should not be closing")
    }

    check("pollEvents") {
      WindowingOpsNative.pollEvents()
    }

    check("getTime") {
      val t = WindowingOpsNative.getTime()
      assert(t > 0.0, s"time=$t")
    }

    check("getPrimaryMonitor") {
      val mon = WindowingOpsNative.getPrimaryMonitor()
      assert(mon != 0L, "no primary monitor")
    }

    check("getMonitors") {
      val monitors = WindowingOpsNative.getMonitors()
      assert(monitors.nonEmpty, "no monitors")
    }

    check("getMonitorName") {
      val mon  = WindowingOpsNative.getPrimaryMonitor()
      val name = WindowingOpsNative.getMonitorName(mon)
      assert(name != null && name.nonEmpty, s"name='$name'")
    }

    check("getClipboardString (may be empty)") {
      WindowingOpsNative.getClipboardString(windowHandle) // just don't crash
    }

    check("createStandardCursor + setCursor + destroyCursor") {
      val cursor = WindowingOpsNative.createStandardCursor(0x00036001) // GLFW_ARROW_CURSOR
      assert(cursor != 0L, "cursor is 0")
      WindowingOpsNative.setCursor(windowHandle, cursor)
      WindowingOpsNative.destroyCursor(cursor)
    }

    // EGL context creation
    check("getNativeWindowHandle") {
      val native = WindowingOpsNative.getNativeWindowHandle(windowHandle)
      assert(native != 0L, "native handle is 0")
    }

    var glContextHandle = 0L
    check("GlOpsNative.createContext (EGL)") {
      val nativeWin = WindowingOpsNative.getNativeWindowHandle(windowHandle)
      glContextHandle = GlOpsNative.createContext(nativeWin, 8, 8, 8, 8, 16, 0, 0)
      assert(glContextHandle != 0L, "EGL context creation failed")
    }

    if (glContextHandle == 0L) {
      skip("GL tests", "no EGL context")
      WindowingOpsNative.destroyWindow(windowHandle)
      WindowingOpsNative.terminate()
      return
    }

    check("GlOpsNative.setSwapInterval") {
      GlOpsNative.setSwapInterval(1)
    }

    check("GlOpsNative.getProcAddress") {
      val addr = GlOpsNative.getProcAddress("glGetError")
      assert(addr != 0L, "glGetError proc address is 0")
    }

    // GL function tests (via AngleGL32Native)
    val gl = new AngleGL32Native()

    check("glGetError") {
      val err = gl.glGetError()
      // 0 = GL_NO_ERROR, which is expected after init
      assert(err == 0, s"glGetError=$err")
    }

    check("glGetString (GL_VERSION)") {
      val version = gl.glGetString(GL20.GL_VERSION)
      assert(version != null && version.nonEmpty, s"GL_VERSION='$version'")
      System.out.println(s"         GL_VERSION: $version")
    }

    check("glGetString (GL_RENDERER)") {
      val renderer = gl.glGetString(GL20.GL_RENDERER)
      assert(renderer != null && renderer.nonEmpty, s"GL_RENDERER='$renderer'")
      System.out.println(s"         GL_RENDERER: $renderer")
    }

    check("glGetIntegerv (GL_MAX_TEXTURE_SIZE)") {
      val buf = java.nio.IntBuffer.allocate(1)
      gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buf)
      val maxTex = buf.get(0)
      assert(maxTex >= 64, s"GL_MAX_TEXTURE_SIZE=$maxTex")
    }

    check("glEnable + glDisable + glIsEnabled") {
      gl.glEnable(EnableCap.DepthTest)
      assert(gl.glIsEnabled(EnableCap.DepthTest), "DEPTH_TEST not enabled")
      gl.glDisable(EnableCap.DepthTest)
      assert(!gl.glIsEnabled(EnableCap.DepthTest), "DEPTH_TEST not disabled")
    }

    check("glViewport + glScissor") {
      gl.glViewport(Pixels(0), Pixels(0), Pixels(320), Pixels(240))
      gl.glScissor(Pixels(0), Pixels(0), Pixels(320), Pixels(240))
    }

    check("glClearColor + glClear") {
      gl.glClearColor(0.1f, 0.2f, 0.3f, 1.0f)
      gl.glClear(ClearMask.ColorBufferBit)
      assert(gl.glGetError() == 0, "GL error after clear")
    }

    check("glGenTextures + glBindTexture + glDeleteTextures") {
      val texBuf = IntBuffer.allocate(1)
      gl.glGenTextures(1, texBuf)
      val tex = texBuf.get(0)
      assert(tex > 0, s"texture id=$tex")
      gl.glBindTexture(TextureTarget.Texture2D, tex)
      gl.glTexParameteri(TextureTarget.Texture2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST)
      gl.glTexParameteri(TextureTarget.Texture2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST)
      gl.glBindTexture(TextureTarget.Texture2D, 0)
      texBuf.clear(); texBuf.put(0, tex)
      gl.glDeleteTextures(1, texBuf)
      assert(gl.glGetError() == 0, "GL error after texture ops")
    }

    check("glGenBuffers + glBindBuffer + glBufferData + glDeleteBuffers") {
      val bufId = IntBuffer.allocate(1)
      gl.glGenBuffers(1, bufId)
      val vbo = bufId.get(0)
      assert(vbo > 0, s"buffer id=$vbo")
      gl.glBindBuffer(BufferTarget.ArrayBuffer, vbo)
      val data = FloatBuffer.allocate(12)
      for (i <- 0 until 12) data.put(i, i.toFloat)
      gl.glBufferData(BufferTarget.ArrayBuffer, 12 * 4, data, BufferUsage.StaticDraw)
      gl.glBindBuffer(BufferTarget.ArrayBuffer, 0)
      bufId.clear(); bufId.put(0, vbo)
      gl.glDeleteBuffers(1, bufId)
      assert(gl.glGetError() == 0, "GL error after buffer ops")
    }

    check("glGenFramebuffers + glBindFramebuffer + glDeleteFramebuffers") {
      val fboBuf = IntBuffer.allocate(1)
      gl.glGenFramebuffers(1, fboBuf)
      val fbo = fboBuf.get(0)
      assert(fbo > 0, s"fbo id=$fbo")
      gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fbo)
      gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0)
      fboBuf.clear(); fboBuf.put(0, fbo)
      gl.glDeleteFramebuffers(1, fboBuf)
      assert(gl.glGetError() == 0, "GL error after FBO ops")
    }

    check("glGenRenderbuffers + glBindRenderbuffer + glDeleteRenderbuffers") {
      val rboBuf = IntBuffer.allocate(1)
      gl.glGenRenderbuffers(1, rboBuf)
      val rbo = rboBuf.get(0)
      assert(rbo > 0, s"rbo id=$rbo")
      gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, rbo)
      gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, 0x8058, Pixels(64), Pixels(64)) // GL_RGBA8
      gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0)
      rboBuf.clear(); rboBuf.put(0, rbo)
      gl.glDeleteRenderbuffers(1, rboBuf)
      assert(gl.glGetError() == 0, "GL error after RBO ops")
    }

    check("glCreateShader + glShaderSource + glCompileShader + glGetShaderiv + glDeleteShader") {
      val shader = gl.glCreateShader(ShaderType.Vertex)
      assert(shader > 0, s"shader=$shader")
      gl.glShaderSource(shader, "void main() { gl_Position = vec4(0.0); }")
      gl.glCompileShader(shader)
      val statusBuf = IntBuffer.allocate(1)
      gl.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, statusBuf)
      val compiled = statusBuf.get(0)
      if (compiled == 0) {
        val log = gl.glGetShaderInfoLog(shader)
        System.err.println(s"         Shader compile log: $log")
      }
      assert(compiled != 0, "shader compile failed")
      gl.glDeleteShader(shader)
      assert(gl.glGetError() == 0, "GL error after shader ops")
    }

    check("glCreateProgram + glAttachShader + glLinkProgram + glDeleteProgram") {
      val vs = gl.glCreateShader(ShaderType.Vertex)
      gl.glShaderSource(vs, "attribute vec4 a_position; void main() { gl_Position = a_position; }")
      gl.glCompileShader(vs)
      val fs = gl.glCreateShader(ShaderType.Fragment)
      gl.glShaderSource(fs, "precision mediump float; void main() { gl_FragColor = vec4(1.0); }")
      gl.glCompileShader(fs)
      val prog = gl.glCreateProgram()
      assert(prog > 0, s"program=$prog")
      gl.glAttachShader(prog, vs)
      gl.glAttachShader(prog, fs)
      gl.glLinkProgram(prog)
      val statusBuf = IntBuffer.allocate(1)
      gl.glGetProgramiv(prog, GL20.GL_LINK_STATUS, statusBuf)
      assert(statusBuf.get(0) != 0, "program link failed")
      gl.glUseProgram(prog)
      gl.glUseProgram(0)
      gl.glDetachShader(prog, vs)
      gl.glDetachShader(prog, fs)
      gl.glDeleteShader(vs)
      gl.glDeleteShader(fs)
      gl.glDeleteProgram(prog)
      assert(gl.glGetError() == 0, "GL error after program ops")
    }

    check("glGetUniformLocation + glUniform*") {
      val vs = gl.glCreateShader(ShaderType.Vertex)
      gl.glShaderSource(vs, "uniform mat4 u_projTrans; attribute vec4 a_position; void main() { gl_Position = u_projTrans * a_position; }")
      gl.glCompileShader(vs)
      val fs = gl.glCreateShader(ShaderType.Fragment)
      gl.glShaderSource(fs, "precision mediump float; uniform vec4 u_color; void main() { gl_FragColor = u_color; }")
      gl.glCompileShader(fs)
      val prog = gl.glCreateProgram()
      gl.glAttachShader(prog, vs)
      gl.glAttachShader(prog, fs)
      gl.glLinkProgram(prog)
      gl.glUseProgram(prog)

      val colorLoc = gl.glGetUniformLocation(prog, "u_color")
      assert(colorLoc >= 0, s"u_color location=$colorLoc")
      gl.glUniform4f(colorLoc, 1.0f, 0.0f, 0.0f, 1.0f)

      val matLoc = gl.glGetUniformLocation(prog, "u_projTrans")
      assert(matLoc >= 0, s"u_projTrans location=$matLoc")
      val matBuf = FloatBuffer.allocate(16)
      for (i <- 0 until 16) matBuf.put(i, if (i % 5 == 0) 1.0f else 0.0f)
      gl.glUniformMatrix4fv(matLoc, 1, false, matBuf)

      gl.glUseProgram(0)
      gl.glDeleteShader(vs); gl.glDeleteShader(fs)
      gl.glDeleteProgram(prog)
      assert(gl.glGetError() == 0, "GL error after uniform ops")
    }

    check("glBlendFunc + glBlendFuncSeparate + glBlendEquation") {
      gl.glEnable(EnableCap.Blend)
      gl.glBlendFunc(BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha)
      gl.glBlendFuncSeparate(BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha, BlendFactor.One, BlendFactor.Zero)
      gl.glBlendEquation(BlendEquation.FuncAdd)
      gl.glDisable(EnableCap.Blend)
      assert(gl.glGetError() == 0, "GL error after blend ops")
    }

    check("glDepthFunc + glDepthMask + glDepthRangef") {
      gl.glDepthFunc(CompareFunc.Less)
      gl.glDepthMask(true)
      gl.glDepthRangef(0.0f, 1.0f)
      assert(gl.glGetError() == 0, "GL error after depth ops")
    }

    check("glStencilFunc + glStencilOp + glStencilMask") {
      gl.glStencilFunc(CompareFunc.Always, 0, 0xff)
      gl.glStencilOp(StencilOp.Keep, StencilOp.Keep, StencilOp.Keep)
      gl.glStencilMask(0xff)
      assert(gl.glGetError() == 0, "GL error after stencil ops")
    }

    check("glCullFace + glFrontFace") {
      gl.glCullFace(CullFace.Back)
      gl.glFrontFace(GL20.GL_CCW)
      assert(gl.glGetError() == 0, "GL error after cull ops")
    }

    check("glLineWidth + glPolygonOffset") {
      gl.glLineWidth(1.0f)
      gl.glPolygonOffset(1.0f, 1.0f)
      assert(gl.glGetError() == 0, "GL error after misc ops")
    }

    check("glPixelStorei") {
      gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1)
      gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1)
      assert(gl.glGetError() == 0, "GL error after pixel store")
    }

    check("glColorMask") {
      gl.glColorMask(true, true, true, true)
      assert(gl.glGetError() == 0, "GL error after color mask")
    }

    check("glFinish + glFlush") {
      gl.glFlush()
      gl.glFinish()
      assert(gl.glGetError() == 0, "GL error after flush/finish")
    }

    // GL30 functions
    check("glGenVertexArrays + glBindVertexArray + glDeleteVertexArrays (GL30)") {
      val vaoBuf = IntBuffer.allocate(1)
      gl.glGenVertexArrays(1, vaoBuf)
      val vao = vaoBuf.get(0)
      assert(vao > 0, s"vao=$vao")
      gl.glBindVertexArray(vao)
      gl.glBindVertexArray(0)
      vaoBuf.clear(); vaoBuf.put(0, vao)
      gl.glDeleteVertexArrays(1, vaoBuf)
      assert(gl.glGetError() == 0, "GL error after VAO ops")
    }

    check("glGetStringi (GL30)") {
      val numExt = IntBuffer.allocate(1)
      gl.glGetIntegerv(GL30.GL_NUM_EXTENSIONS, numExt)
      if (numExt.get(0) > 0) {
        val ext0 = gl.glGetStringi(GL20.GL_EXTENSIONS, 0)
        assert(ext0 != null, "extension 0 is null")
      }
    }

    check("glReadBuffer (GL30)") {
      gl.glReadBuffer(GL20.GL_BACK)
      assert(gl.glGetError() == 0, "GL error after readBuffer")
    }

    // Cleanup
    check("GlOpsNative.destroyContext") {
      GlOpsNative.destroyContext(glContextHandle)
    }

    check("destroyWindow") {
      WindowingOpsNative.destroyWindow(windowHandle)
    }

    check("glfwTerminate") {
      WindowingOpsNative.terminate()
    }
  }

  // ═══════════════════════════════════════════════════════════════════════
  // Helpers
  // ═══════════════════════════════════════════════════════════════════════

  private def identityMatrix4(): Array[Float] = {
    val m = new Array[Float](16)
    m(0) = 1.0f; m(5) = 1.0f; m(10) = 1.0f; m(15) = 1.0f
    m
  }

  private def identityMatrix3(): Array[Float] = {
    val m = new Array[Float](9)
    m(0) = 1.0f; m(4) = 1.0f; m(8) = 1.0f
    m
  }

  private def assertFloatEq(actual: Float, expected: Float, label: String): Unit =
    assert(Math.abs(actual - expected) < 1e-5f, s"$label: expected $expected, got $actual")
}
