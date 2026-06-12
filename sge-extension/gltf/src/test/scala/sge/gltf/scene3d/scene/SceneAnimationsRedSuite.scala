/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-511 — Scene's public `animations` ctor val is passed null by the
 * common constructors ("re-assign later" comment never honored, Scene.scala:43-52) and
 * Scene.update() drives a PRIVATE lazy `_animations` instead of the public field
 * (Scene.scala:74, :125).
 *
 * Expected behavior from the original source (original-src/gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/scene/Scene.java):
 *   - Scene.java:37    `public final AnimationsPlayer animations;` — a public, always-assigned field.
 *   - Scene.java:127-134 `Scene(ModelInstance, boolean animated)` ends with
 *     `animations = new AnimationsPlayer(this);` (line 133); ALL other constructors
 *     (lines 41-47, 109-120, 135-137) funnel into it, so `animations` is never null.
 *   - Scene.java:139-144 `update(Camera, float)` delegates to THAT public field:
 *     `animations.update(delta);` (line 141).
 *   - AnimationsPlayer.java:52-60 `playAll(boolean)` plays every animation of
 *     `scene.modelInstance`; AnimationsPlayer.java:66-77 `update(float)` advances the
 *     controllers and calls `scene.modelInstance.calculateTransforms()`.
 */
package sge
package gltf
package scene3d
package scene

import sge.gltf.scene3d.animation.{ AnimationControllerHack, AnimationsPlayer }
import sge.graphics.{ Camera, PerspectiveCamera }
import sge.graphics.g3d.{ Model, ModelInstance }
import sge.graphics.g3d.environment.BaseLight
import sge.graphics.g3d.model.{ Animation, Node, NodeAnimation, NodeKeyframe }
import sge.math.Vector3
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import lowlevel.Nullable
import lowlevel.util.{ DynamicArray, ObjectMap }

class SceneAnimationsRedSuite extends munit.FunSuite {

  import SceneAnimationsRedSuite.*

  private given Sge = Sge(NoopApplicationStub, new NoopGraphics(), new NoopAudio(), NoopFilesStub, new NoopInput(), NoopNetStub)

  /** A model with one root node and one named animation holding a single constant translation keyframe at (1, 2, 3) — the cheapest fully headless animated model (no GL context).
    */
  private def animatedModel(): Model = {
    val model = new Model()
    val node  = new Node()
    node.id = "root"
    model.nodes.add(node)
    val keyframes = DynamicArray[NodeKeyframe[Vector3]]()
    keyframes.add(new NodeKeyframe[Vector3](0f, Vector3(1f, 2f, 3f)))
    val nodeAnimation = new NodeAnimation()
    nodeAnimation.node = node
    nodeAnimation.translation = Nullable(keyframes)
    val animation = new Animation()
    animation.id = "move"
    animation.duration = 1f
    animation.nodeAnimations.add(nodeAnimation)
    model.animations.add(animation)
    model
  }

  /** Translation of the instance's "root" node after animations were applied. */
  private def rootTranslation(scene: Scene): Vector3 = {
    val node = scene.modelInstance.getNode("root")
    assert(node.isDefined, "model instance must have copied the 'root' node")
    node.get.globalTransform.translation(Vector3())
  }

  // Bug (a): scene.animations is null after the common constructors, so the documented
  // gdx-gltf idiom `scene.animations.playAll()` NPEs. Per Scene.java:133 the constructor
  // must assign the public field, and per Scene.java:141 update() must drive it.
  test(
    "ISS-511 (a): common-ctor Scene.animations is usable — playAll() then update() applies the animation (Scene.java:133,141)"
  ) {
    val scene = new Scene(new ModelInstance(animatedModel()))
    assert(
      scene.animations ne null,
      "Scene.animations must be assigned by the constructor (Scene.java:133: animations = new AnimationsPlayer(this))"
    )
    // The documented idiom: play every animation of the scene's own model instance.
    scene.animations.playAll()
    scene.update(PerspectiveCamera(), 0.5f)
    // AnimationsPlayer.java:66-77 — update() advances the controllers and recalculates the
    // instance transforms, so the keyframe translation lands in the node's globalTransform.
    val pos = rootTranslation(scene)
    assertEqualsFloat(pos.x, 1f, 0.0001f, "playAll() + update() must apply the keyframe translation to the scene's own model instance")
    assertEqualsFloat(pos.y, 2f, 0.0001f)
    assertEqualsFloat(pos.z, 3f, 0.0001f)
  }

  // Bug (b): a user-supplied AnimationsPlayer passed to the primary constructor is stored in
  // the public `animations` val but silently IGNORED by update(), which drives the private
  // lazy `_animations` instead. Per Scene.java:141 update() must delegate to the public field.
  test("ISS-511 (b): update() drives the ctor-supplied AnimationsPlayer, not a hidden one (Scene.java:37,133,141)") {
    val model = animatedModel()
    val mi    = new ModelInstance(model)
    // AnimationsPlayer.java:16-18 only stores the scene reference, so a throwaway scene lets
    // us construct the counting player before the scene under test exists.
    val placeholder = new Scene(new ModelInstance(model), false)
    val counting    = new CountingAnimationsPlayer(placeholder)
    val scene       = new Scene(
      mi,
      AnimationControllerHack(mi),
      ObjectMap[Node, BaseLight[?]](),
      ObjectMap[Node, Camera](),
      counting
    )
    assert(scene.animations eq counting, "the primary constructor must store the supplied player in the public val")
    scene.update(PerspectiveCamera(), 0.25f)
    assertEquals(
      counting.updateCalls,
      1,
      "Scene.update must delegate to the supplied animations player (Scene.java:141: animations.update(delta))"
    )
    assertEqualsFloat(counting.lastDelta, 0.25f, 0.00001f, "Scene.update must pass the delta through verbatim (Scene.java:141)")
  }

  // GREEN control: the same headless fixture works when the AnimationsPlayer is constructed
  // explicitly — proves tests (a)/(b) fail because of the Scene wiring, not the harness.
  test("ISS-511 control: explicitly constructed AnimationsPlayer plays and applies animations (GREEN)") {
    val scene  = new Scene(new ModelInstance(animatedModel()), false)
    val player = new AnimationsPlayer(scene)
    player.playAll()
    player.update(0.5f)
    val pos = rootTranslation(scene)
    assertEqualsFloat(pos.x, 1f, 0.0001f)
    assertEqualsFloat(pos.y, 2f, 0.0001f)
    assertEqualsFloat(pos.z, 3f, 0.0001f)
  }
}

object SceneAnimationsRedSuite {

  /** Counts update() calls — discriminates the supplied player from any hidden one. */
  final private class CountingAnimationsPlayer(scene: Scene) extends AnimationsPlayer(scene) {
    var updateCalls: Int   = 0
    var lastDelta:   Float = -1f

    override def update(delta: Float): Unit = {
      updateCalls += 1
      lastDelta = delta
    }
  }

  // Minimal headless Sge fixture — mirrors sge/src/test/scala/sge/SgeTestFixture.scala,
  // which is not on the gltf extension's test classpath (no test->test dependency).
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
