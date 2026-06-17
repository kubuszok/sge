/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package physics

import munit.FunSuite
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** Red test for ISS-676: real Rapier2D WASM physics backend on Scala.js.
  *
  * Today the JS backend (`sge.platform.PhysicsOpsJs`) is a stub that throws `UnsupportedOperationException` on every call, and there is no `sge.platform.PhysicsExtension` to asynchronously load the
  * Rapier WASM module at startup. This suite pins the intended public contract:
  *
  *   1. `sge.platform.PhysicsExtension` is an `object` extending `SgeExtension` whose `load()(using Sge)` returns a `Future[Unit]` that completes once `RAPIER.init()` has resolved.
  *   2. After loading, the SAME high-level API used by the JVM integration suite ([[PhysicsWorld]], [[PhysicsWorld.createBody]], [[RigidBody.attachCollider]], [[PhysicsWorld.step]],
  *      [[RigidBody.position]]) drives a real simulation: a dynamic body with a collider (so it has mass) falls under gravity.
  *
  * The suite is ASYNC: munit awaits the returned `Future[Unit]`. It uses the JS microtask EC — NOT `Await`/blocking, which does not link on Scala.js.
  *
  * Pre-implementation expectation: `PhysicsExtension` is absent AND `PhysicsOpsJs` throws, so this suite FAILS — it does not compile because `sge.platform.PhysicsExtension` is not found. That
  * capability-absent compile failure is the honest red for this brand-new backend.
  */
class PhysicsGravityJsRedSuite extends FunSuite {

  /** Minimal [[Sge]] context for the JS physics test. The gravity simulation does not touch graphics/audio/input; we only need a `given Sge` to call `PhysicsExtension.load()(using Sge)`. The `Sge`
    * constructor is `private[sge]`, reachable here because this suite lives in `package sge`.
    */
  private given Sge =
    Sge(StubApplication, new sge.noop.NoopGraphics(), new sge.noop.NoopAudio(), StubFiles, new sge.noop.NoopInput(), StubNet)

  test("ISS-676 JS Rapier WASM backend: dynamic body with a collider falls under gravity") {
    // Load + await RAPIER.init() once, THEN run the gravity scenario on the real backend.
    sge.platform.PhysicsExtension.load().map { _ =>
      val world = new PhysicsWorld(gravityX = 0f, gravityY = -9.81f)
      try {
        // Dynamic body at y = 10, with a box collider so it has mass (mirrors the JVM integration suite).
        val body = world.createBody(BodyType.Dynamic, x = 0f, y = 10f)
        body.attachCollider(Shape.Box(0.5f, 0.5f), density = 1f)

        // Step ~1 second of simulation.
        var i = 0
        while (i < 60) {
          world.step(1f / 60f)
          i += 1
        }

        // Strict, tolerance-free assertion: only passes if a REAL simulation moved the body down.
        val (_, y) = body.position
        assert(y < 9.0f, s"body must fall under gravity (y should drop well below 10), got y=$y")
      } finally
        world.close()
    }
  }

  // ─── Minimal stub subsystems (gravity simulation does not use them) ──────

  private object StubApplication extends Application {
    def applicationListener:                                  ApplicationListener         = throw new UnsupportedOperationException
    def graphics:                                             Graphics                    = throw new UnsupportedOperationException
    def audio:                                                Audio                       = throw new UnsupportedOperationException
    def input:                                                Input                       = throw new UnsupportedOperationException
    def files:                                                Files                       = throw new UnsupportedOperationException
    def net:                                                  Net                         = throw new UnsupportedOperationException
    def applicationType:                                      Application.ApplicationType = Application.ApplicationType.WebGL
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

  private object StubFiles extends Files {
    def getFileHandle(path: String, fileType: files.FileType): files.FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                           files.FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                           files.FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                                   String           = ""
    def isExternalStorageAvailable:                            Boolean          = false
    def localStoragePath:                                      String           = ""
    def isLocalStorageAvailable:                               Boolean          = false
  }

  private object StubNet extends Net {
    import Net._
    def httpClient:                                                                                     net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: net.ServerSocketHints): net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   net.ServerSocketHints):             net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: net.SocketHints):       net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                              Boolean           = false
  }
}
