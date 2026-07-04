/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-709 — TransmissionSource's renderablePool is a plain sge.utils.Pool
 * with no reset-on-obtain contract, and end() calls clear() instead of flush(), so the
 * pool is decorative: it neither resets recycled Renderables nor reuses instances across
 * frames (fresh allocations every frame).
 *
 * Original source: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/scene/TransmissionSource.java
 *   - TransmissionSource.java:43   `renderablePool` is a `new FlushablePool<Renderable>(){...}`.
 *   - TransmissionSource.java:49-57 the `obtain()` override RESETS each obtained Renderable:
 *       :51 renderable.environment = null;
 *       :52 renderable.material    = null;
 *       :53 renderable.meshPart.set("", null, 0, 0, 0);
 *       :54 renderable.shader      = null;
 *       :55 renderable.userData    = null;
 *   - TransmissionSource.java:154  `renderablePool.flush();` at end of frame returns every
 *       obtained instance to the free list, so the NEXT frame's obtain() reuses the SAME
 *       instances (FlushablePool tracks obtained internally; no explicit free() is needed).
 *
 * SGE already ships the matching contract in sge.utils.Pool.Flushable (Pool.scala:149-173):
 *   - Pool.scala:152-156 obtain() records each obtained instance in `obtained`.
 *   - Pool.scala:159-162 flush() frees every obtained instance back for reuse.
 * The port (TransmissionSource.scala:49-53, :121) uses a plain Pool and clear() instead,
 * discarding the free list every frame. This suite pins the ORIGINAL's pool semantics on the
 * pool itself AND on the end() call site (begin -> render -> end -> render must reuse the
 * same instance, TransmissionSource.java:132,154).
 */
package sge
package gltf
package scene3d
package scene

import sge.graphics.{ Camera, GL20, PerspectiveCamera, PrimitiveMode }
import sge.graphics.g3d.{ Environment, Material, Renderable, RenderableProvider, Shader }
import sge.graphics.g3d.utils.{ RenderableSorter, ShaderProvider }
import sge.noop.{ NoopAudio, NoopGL20, NoopGraphics, NoopInput }
import sge.utils.Pool
import lowlevel.Nullable
import lowlevel.util.DynamicArray

import java.nio.IntBuffer

class TransmissionSourcePoolIss709RedSuite extends munit.FunSuite {

  import TransmissionSourcePoolIss709RedSuite.*

  // TransmissionSource's constructor builds a ModelBatch whose default RenderContext creates a
  // DefaultTextureBinder; that reads GL_MAX_TEXTURE_IMAGE_UNITS and rejects <1 units
  // (DefaultTextureBinder.scala:57-59). begin() additionally builds a real FrameBuffer whose
  // build() insists on a GL_FRAMEBUFFER_COMPLETE readback (GLFrameBuffer.scala:306,345). A GL
  // faking exactly those two readbacks lets the whole renderer graph run headlessly.
  private given Sge =
    Sge(
      NoopApplicationStub,
      new NoopGraphics() {
        override def gl20: GL20 = HeadlessRendererGL20
      },
      new NoopAudio(),
      NoopFilesStub,
      new NoopInput(),
      NoopNetStub
    )

  /** Builds a TransmissionSource with headless collaborators and returns the exact `renderablePool` instance it constructs. The pool is handed to a RenderableProvider by `render(provider)`
    * (TransmissionSource.scala:89-100); that overload touches no GL when the provider adds nothing, so it is fully headless.
    */
  private def renderablePool(): Pool[Renderable] = {
    val source   = new TransmissionSource(HeadlessShaderProvider, NoSortRenderableSorter)
    val capturer = new PoolCapturingProvider
    source.render(capturer)
    capturer.captured.getOrElse(fail("render(provider) must invoke getRenderables with the renderablePool"))
  }

  // Original obtain() override (TransmissionSource.java:49-57): a recycled Renderable is
  // returned with environment/material/meshPart/shader/userData reset. A plain Pool.obtain()
  // (TransmissionSource.scala:49-53) performs no reset, so the recycled instance keeps its
  // stale fields.
  test(
    "ISS-709: obtaining a recycled Renderable resets it per the original obtain() override (TransmissionSource.java:49-57)"
  ) {
    val pool = renderablePool()

    val first = pool.obtain()
    first.environment = Nullable(new Environment())
    first.material = Nullable(new Material())
    first.userData = Nullable("iss-709-stale-user-data")
    first.meshPart.id = Nullable("iss-709-stale-id")
    first.meshPart.offset = 5
    first.meshPart.size = 7
    first.meshPart.primitiveType = PrimitiveMode(4)

    // Return it to the free list so the next obtain() recycles this very instance.
    pool.free(first)
    val second = pool.obtain()
    assert(second eq first, "the pool must recycle the freed instance, not allocate a fresh one")

    assert(
      second.environment.isEmpty,
      "obtain() must clear environment (TransmissionSource.java:51: renderable.environment = null)"
    )
    assert(second.material.isEmpty, "obtain() must clear material (TransmissionSource.java:52: renderable.material = null)")
    assert(second.userData.isEmpty, "obtain() must clear userData (TransmissionSource.java:55: renderable.userData = null)")
    assert(
      second.meshPart.id.isEmpty,
      "obtain() must reset meshPart id (TransmissionSource.java:53: renderable.meshPart.set(\"\", null, 0, 0, 0))"
    )
    assertEquals(second.meshPart.offset, 0, "obtain() must reset meshPart offset (TransmissionSource.java:53)")
    assertEquals(second.meshPart.size, 0, "obtain() must reset meshPart size (TransmissionSource.java:53)")
    assertEquals(
      second.meshPart.primitiveType,
      PrimitiveMode(0),
      "obtain() must reset meshPart primitiveType (TransmissionSource.java:53)"
    )
  }

  // Original renderablePool is a FlushablePool (TransmissionSource.java:43) whose flush()
  // (invoked at TransmissionSource.java:154) returns every obtained instance for reuse,
  // WITHOUT any explicit free() — the pool tracks obtained instances itself
  // (sge.utils.Pool.Flushable, Pool.scala:149-173). A plain Pool with clear()
  // (TransmissionSource.scala:49-53, :121) neither tracks nor reuses.
  test(
    "ISS-709: renderablePool is a FlushablePool whose flush() recycles obtained instances across frames (TransmissionSource.java:43,154)"
  ) {
    val pool = renderablePool()

    assert(
      pool.isInstanceOf[Pool.Flushable[?]],
      "renderablePool must be a sge.utils.Pool.Flushable (TransmissionSource.java:43: new FlushablePool<Renderable>)"
    )
    val flushable = pool.asInstanceOf[Pool.Flushable[Renderable]]

    // Frame 1: obtain an instance. FlushablePool records it in `obtained`; no explicit free.
    val frame1 = flushable.obtain()
    // End of frame (TransmissionSource.java:154): flush() returns obtained instances for reuse.
    flushable.flush()
    // Frame 2: the next obtain() must hand back the SAME instance.
    val frame2 = flushable.obtain()

    assert(
      frame2 eq frame1,
      "flush() must return obtained instances so the next frame reuses the same instance (TransmissionSource.java:154: renderablePool.flush())"
    )
  }

  // Guards the actual end() call site: the original frame loop is begin() -> render() -> end()
  // (TransmissionSource.java:87,110,132) and end() flushes the pool (TransmissionSource.java:154)
  // so the NEXT frame's getRenderables obtains recycled instances. end() calling clear() instead
  // (TransmissionSource.scala:121) discards nothing that was obtained (nothing was ever freed)
  // and recycles nothing — every frame allocates fresh Renderables.
  test(
    "ISS-709: end() flushes the pool so the next frame's render() reuses the same Renderable instance (TransmissionSource.java:132,154)"
  ) {
    val source   = new TransmissionSource(HeadlessShaderProvider, NoSortRenderableSorter)
    val provider = new ObtainingProvider
    val camera   = PerspectiveCamera()

    // Frame 1 — the original render loop (TransmissionSource.java:87,110,132). The provider's
    // material-less renderable never sets hasTransmission, so end() skips all GL rendering and
    // only performs the end-of-frame bookkeeping incl. the pool flush (:152-156).
    source.begin(camera)
    source.render(provider)
    source.end()

    // Frame 2 — FlushablePool semantics (TransmissionSource.java:43): end()'s flush() returned
    // frame 1's obtained instance to the free list, so this frame's obtain() must reuse it.
    source.begin(camera)
    source.render(provider)

    assertEquals(provider.obtained.size, 2, "the provider obtains exactly one Renderable per frame")
    assert(
      provider.obtained(1) eq provider.obtained(0),
      "after end() the next frame's obtain() must return the SAME instance — end() must flush() the FlushablePool, not clear() it (TransmissionSource.java:154: renderablePool.flush())"
    )
  }
}

object TransmissionSourcePoolIss709RedSuite {

  /** Captures the pool passed to getRenderables; adds no renderables, so render(provider) stays headless (no shouldBeRendered / GL work).
    */
  final private class PoolCapturingProvider extends RenderableProvider {
    var captured: Nullable[Pool[Renderable]] = Nullable.empty

    override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit =
      captured = Nullable(pool)
  }

  /** Obtains exactly one Renderable from the pool per getRenderables call — the way real providers source their renderables (RenderableProvider.scala:37-44) — and records each obtained instance so
    * cross-frame reference identity can be asserted.
    */
  final private class ObtainingProvider extends RenderableProvider {
    val obtained: scala.collection.mutable.ListBuffer[Renderable] = scala.collection.mutable.ListBuffer.empty

    override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit = {
      val renderable = pool.obtain()
      obtained += renderable
      renderables.add(renderable)
    }
  }

  /** Headless ShaderProvider — TransmissionSource only constructs a ModelBatch with it; no shader is fetched without a GL render pass.
    */
  private object HeadlessShaderProvider extends ShaderProvider {
    def getShader(renderable: Renderable): Shader = throw new UnsupportedOperationException
    def close():                           Unit   = ()
  }

  /** Headless RenderableSorter — sorting is never triggered off a GL render pass. */
  private object NoSortRenderableSorter extends RenderableSorter {
    def sort(camera: Nullable[Camera], renderables: DynamicArray[Renderable]): Unit = ()
  }

  /** NoopGL20 that reports a positive GL_MAX_TEXTURE_IMAGE_UNITS so DefaultTextureBinder accepts its unit range (DefaultTextureBinder.scala:57-59) and a GL_FRAMEBUFFER_COMPLETE framebuffer status so
    * begin()'s FrameBuffer builds (GLFrameBuffer.scala:306,345). Every other GL call stays a no-op via the exported NoopGL20 (see the export pattern in DecalSortRedSuite / FrameBufferISS561Suite).
    */
  private object HeadlessRendererGL20 extends GL20 {
    private val underlying: GL20 = NoopGL20
    export underlying.{ glCheckFramebufferStatus as _, glGetIntegerv as _, * }

    def glGetIntegerv(pname: Int, params: IntBuffer): Unit =
      if (pname == GL20.GL_MAX_TEXTURE_IMAGE_UNITS) {
        params.put(0, 16)
        ()
      }

    def glCheckFramebufferStatus(target: Int): Int = GL20.GL_FRAMEBUFFER_COMPLETE
  }

  // Headless Sge fixture — mirrors SceneAnimationsRedSuite, which mirrors
  // sge/src/test/scala/sge/SgeTestFixture.scala (not on the gltf test classpath).
  private object NoopApplicationStub extends Application {
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

  private object NoopFilesStub extends Files {
    def getFileHandle(path: String, fileType: sge.files.FileType): sge.files.FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                                       String               = ""
    def isExternalStorageAvailable:                                Boolean              = false
    def localStoragePath:                                          String               = ""
    def isLocalStorageAvailable:                                   Boolean              = false
  }

  private object NoopNetStub extends Net {
    import Net.*
    def httpClient:                                                                                         sge.net.SgeHttpClient = sge.net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: sge.net.ServerSocketHints): sge.net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   sge.net.ServerSocketHints):             sge.net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: sge.net.SocketHints):       sge.net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                                  Boolean               = false
  }
}
