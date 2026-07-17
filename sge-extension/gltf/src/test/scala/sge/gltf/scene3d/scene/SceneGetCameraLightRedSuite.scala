/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-628 — Scene.getCamera / Scene.getLight lost the
 * first-match-and-stop semantics of the original.
 *
 * Original behavior (original-src/gdx-gltf, never fetched):
 *   - net/mgsx/gltf/scene3d/scene/Scene.java:174-181 (getCamera) and 183-190
 *     (getLight) iterate the map and `return e.value` on the FIRST entry whose
 *     node id matches, stopping immediately.
 * Port (Scene.scala:157-171): both methods iterate the WHOLE map with
 *   `cameras.foreachEntry { (node, camera) => if (name == node.id) result = ... }`
 * keeping the LAST matching entry. With two nodes sharing an id the two differ:
 * the original yields the first entry in iteration order, the port yields the
 * last.
 *
 * The assertions are iteration-order-agnostic: the test collects the matching
 * entries in the map's own iteration order and pins getCamera/getLight to the
 * HEAD of that list (what a first-match-and-break returns). The port returns the
 * LAST element instead, so with two distinct matches the assertion fails today.
 *
 * Platform scope: shared (src/test/scala) — Scene/Model/ObjectMap are pure data
 * structures; no GL is touched (the model has no meshes/animations).
 */
package sge
package gltf
package scene3d
package scene

import scala.collection.mutable.ListBuffer
import sge.gltf.HeadlessSgeSupport
import sge.graphics.{ Camera, PerspectiveCamera }
import sge.graphics.g3d.Model
import sge.graphics.g3d.environment.{ BaseLight, DirectionalLight }
import sge.graphics.g3d.model.Node

class SceneGetCameraLightRedSuite extends munit.FunSuite {

  private given Sge = HeadlessSgeSupport()

  /** A Scene with no meshes/animations, so getCamera/getLight can be exercised headlessly. */
  private def emptyScene(): Scene = new Scene(new Model())

  private def node(id: String): Node = {
    val n = new Node()
    n.id = id
    n
  }

  test("ISS-628: Scene.getCamera returns the FIRST id match in iteration order, not the last (Scene.java:174-181)") {
    val scene = emptyScene()
    val camA  = new PerspectiveCamera()
    val camB  = new PerspectiveCamera()
    // Two DISTINCT node keys sharing the same id — duplicate node ids are legal in glTF.
    scene.cameras.put(node("dup"), camA)
    scene.cameras.put(node("dup"), camB)

    // Collect the matches in the map's own iteration order; first-match-and-break returns the head.
    val matches = ListBuffer.empty[Camera]
    scene.cameras.foreachEntry { (n, c) =>
      if (n.id == "dup") matches += c
    }
    assertEquals(matches.size, 2, "fixture must produce two id matches")
    assert(matches.head ne matches.last, "the two matches must be distinct objects")

    assert(
      scene.getCamera("dup").get eq matches.head,
      "Scene.java:176-178 returns the FIRST matching camera and stops; the port keeps the last match"
    )
  }

  test("ISS-628: Scene.getLight returns the FIRST id match in iteration order, not the last (Scene.java:183-190)") {
    val scene  = emptyScene()
    val lightA = new DirectionalLight()
    val lightB = new DirectionalLight()
    scene.lights.put(node("dup"), lightA)
    scene.lights.put(node("dup"), lightB)

    val matches = ListBuffer.empty[BaseLight[?]]
    scene.lights.foreachEntry { (n, l) =>
      if (n.id == "dup") matches += l
    }
    assertEquals(matches.size, 2, "fixture must produce two id matches")
    assert(matches.head ne matches.last, "the two matches must be distinct objects")

    assert(
      scene.getLight("dup").get eq matches.head,
      "Scene.java:185-187 returns the FIRST matching light and stops; the port keeps the last match"
    )
  }

  // GREEN control: a unique id resolves to its single entry — proves the harness and the
  // ObjectMap population work, isolating the failures above to the first-vs-last divergence.
  test("ISS-628 control: a unique id resolves to its only camera (GREEN)") {
    val scene = emptyScene()
    val cam   = new PerspectiveCamera()
    scene.cameras.put(node("solo"), cam)
    assert(scene.getCamera("solo").get eq cam)
    assert(scene.getLight("missing").isEmpty)
  }
}
