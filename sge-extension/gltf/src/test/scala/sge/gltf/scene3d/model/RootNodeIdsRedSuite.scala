/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-512 — ModelInstanceHack's rootNodeIds overload delegates to
 * this(model) and DISCARDS the ids (ModelInstanceHack.scala:33-37), so
 * Scene(sceneModel, rootNodeIds*) silently loads the ENTIRE model instead of the
 * requested subtree.
 *
 * Expected behavior from the original sources:
 *   - original-src/gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/model/ModelInstanceHack.java:29-31
 *     `public ModelInstanceHack(Model model, final String... rootNodeIds){ super(model, rootNodeIds); }`
 *     — the ids are FORWARDED to the LibGDX ModelInstance constructor.
 *   - original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/g3d/ModelInstance.java:145-147
 *     `ModelInstance(Model, String...)` delegates to the (Model, Matrix4, String...) ctor,
 *     which (lines 150-159) calls `copyNodes(model.nodes, rootNodeIds)` when ids are given.
 *   - original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/g3d/ModelInstance.java:229-240
 *     `copyNodes(Array<Node> nodes, String... nodeIds)` iterates the model's ROOT nodes in
 *     model order and copies only those whose id EXACTLY equals one of the requested ids
 *     (`nodeId.equals(node.id)`); children come along only via `node.copy()`. A child id
 *     therefore matches nothing — only roots are inspected.
 *   - original-src/gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/scene/Scene.java:45-47
 *     `Scene(SceneModel, String... rootNodeIds)` builds
 *     `new ModelInstanceHack(sceneModel.model, rootNodeIds)`, so the scene contains only
 *     the requested subtree.
 */
package sge
package gltf
package scene3d
package model

import sge.gltf.scene3d.scene.{ Scene, SceneModel }
import sge.graphics.g3d.{ Model, ModelInstance }
import sge.graphics.g3d.model.Node
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import lowlevel.Nullable

class RootNodeIdsRedSuite extends munit.FunSuite {

  import RootNodeIdsRedSuite.*

  private given Sge = Sge(NoopApplicationStub, new NoopGraphics(), new NoopAudio(), NoopFilesStub, new NoopInput(), NoopNetStub)

  /** A headless model with three root nodes "nodeA", "nodeB", "nodeC" (in that order); "nodeA" has one child "childA". No meshes, materials or animations — node copying needs no GL context.
    */
  private def threeRootModel(): Model = {
    val model = new Model()
    val nodeA = new Node()
    nodeA.id = "nodeA"
    val childA = new Node()
    childA.id = "childA"
    val _     = nodeA.addChild(childA)
    val nodeB = new Node()
    nodeB.id = "nodeB"
    val nodeC = new Node()
    nodeC.id = "nodeC"
    model.nodes.add(nodeA)
    model.nodes.add(nodeB)
    model.nodes.add(nodeC)
    model
  }

  /** Ids of the instance's root nodes, in instance order. */
  private def rootIds(instance: ModelInstance): List[String] = {
    val ids = List.newBuilder[String]
    for (node <- instance.nodes)
      ids += node.id
    ids.result()
  }

  // Bug: the ids are discarded, so ALL three roots are copied instead of just "nodeB".
  test("ISS-512 (a): ModelInstanceHack(model, subset) copies ONLY the named root nodes (ModelInstance.java:156,229-240)") {
    val instance = new ModelInstanceHack(threeRootModel(), "nodeB")
    assertEquals(
      rootIds(instance),
      List("nodeB"),
      "ModelInstanceHack.java:29-31 forwards rootNodeIds to super; copyNodes(model.nodes, rootNodeIds) must keep only the named roots"
    )
  }

  // Upstream copyNodes iterates model.nodes in MODEL order (outer loop, ModelInstance.java:230),
  // so the result order follows the model, not the argument order; children survive via node.copy().
  test("ISS-512 (b): a two-of-three subset keeps model order and carries children along (ModelInstance.java:229-240)") {
    val instance = new ModelInstanceHack(threeRootModel(), "nodeC", "nodeA")
    assertEquals(
      rootIds(instance),
      List("nodeA", "nodeC"),
      "only the requested roots, in model order (outer loop over model.nodes)"
    )
    val nodeA = instance.nodes(0)
    assertEquals(nodeA.children.size, 1, "node.copy() copies the subtree — nodeA keeps its child")
    assertEquals(nodeA.children(0).id, "childA")
  }

  // Upstream copyNodes(String...) only inspects ROOT nodes — a child id matches nothing,
  // yielding an instance with zero nodes. Today the bug copies the whole model instead.
  test("ISS-512 (c): a child id matches no ROOT node — instance stays empty (ModelInstance.java:229-240)") {
    val instance = new ModelInstanceHack(threeRootModel(), "childA")
    assertEquals(
      rootIds(instance),
      List.empty[String],
      "copyNodes(nodes, nodeIds) compares ids against root nodes only — \"childA\" selects nothing"
    )
  }

  // User-facing entry point: Scene(SceneModel, rootNodeIds*) — Scene.java:45-47 builds
  // new ModelInstanceHack(sceneModel.model, rootNodeIds), so the scene must contain only
  // the requested subtree.
  test("ISS-512 (d): Scene(sceneModel, rootNodeIds*) loads only the requested subtree (Scene.java:45-47)") {
    val sceneModel = new SceneModel()
    sceneModel.model = threeRootModel()
    val scene = new Scene(sceneModel, "nodeB")
    assertEquals(
      rootIds(scene.modelInstance),
      List("nodeB"),
      "Scene.java:46: new ModelInstanceHack(sceneModel.model, rootNodeIds) — whole-model load means the ids were discarded"
    )
  }

  // GREEN control: the no-ids overload copies every root node — proves the fixture and the
  // node-copy machinery work, so (a)-(d) fail because of the discarded ids, not the harness.
  test("ISS-512 control: ModelInstanceHack(model) copies ALL root nodes (GREEN)") {
    val instance = new ModelInstanceHack(threeRootModel())
    assertEquals(rootIds(instance), List("nodeA", "nodeB", "nodeC"))
  }

  // GREEN control: the base ModelInstance honors rootNodeIds — the filtering primitive the
  // Hack overload must delegate to already works, isolating the bug to ModelInstanceHack.
  test("ISS-512 control: base ModelInstance(model, rootNodeIds) keeps only the named roots (GREEN)") {
    val instance = new ModelInstance(threeRootModel(), Nullable(Seq("nodeB")))
    assertEquals(rootIds(instance), List("nodeB"))
  }
}

object RootNodeIdsRedSuite {

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
