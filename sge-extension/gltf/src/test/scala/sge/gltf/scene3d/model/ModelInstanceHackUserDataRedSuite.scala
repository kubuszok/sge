/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-632 deviation (2) — ModelInstanceHack.getRenderable assigns
 * out.userData only when morphTargets is DEFINED
 * (ModelInstanceHack.scala:139-145: `npp.morphTargets.foreach { mt => ... }`),
 * whereas the original assigns it UNCONDITIONALLY.
 *
 * Original behavior (original-src/gdx-gltf, never fetched):
 *   - net/mgsx/gltf/scene3d/model/ModelInstanceHack.java:82-89 —
 *     `out.userData = ((NodePartPlus) nodePart).morphTargets;` runs for EVERY
 *     NodePartPlus, so a null morphTargets clears out.userData.
 *
 * Why it matters: the base ModelInstance.getRenderable (ModelInstance.scala:319)
 * ends with `out.userData = userData`, propagating the ModelInstance's own
 * userData onto the Renderable. For a NodePartPlus with NO morph targets, the
 * original then overwrites that with null (clears it); the port leaves the
 * instance userData in place. A Renderable rendered from such a node therefore
 * carries the instance-level userData where the original would have nulled it —
 * a behavioral divergence.
 *
 * Platform scope: shared (src/test/scala) — node/renderable copying needs no GL.
 */
package sge
package gltf
package scene3d
package model

import sge.gltf.HeadlessSgeSupport
import sge.graphics.g3d.{ Model, Renderable }
import sge.graphics.g3d.model.{ MeshPart, Node }
import lowlevel.Nullable

class ModelInstanceHackUserDataRedSuite extends munit.FunSuite {

  private given Sge = HeadlessSgeSupport()

  private def freshNodePartPlus(): NodePartPlus = {
    val npp = new NodePartPlus()
    npp.meshPart = new MeshPart() // NodePart.setRenderable copies meshPart; must be non-null
    npp
  }

  test("ISS-632(2): getRenderable with EMPTY morphTargets clears out.userData (ModelInstanceHack.java:85-87)") {
    val hack = new ModelInstanceHack(new Model())
    // Instance-level userData that the base getRenderable (ModelInstance.scala:319) propagates.
    hack.userData = Nullable("INSTANCE-USERDATA".asInstanceOf[Any])

    val npp = freshNodePartPlus() // morphTargets stays empty
    val out = new Renderable()
    val ret = hack.getRenderable(out, new Node(), npp)

    assert(
      Nullable.isEmpty(ret.userData),
      "ModelInstanceHack.java:86 assigns out.userData = morphTargets unconditionally; a null morph target must CLEAR userData, " +
        "but the port leaves the instance userData in place"
    )
  }

  // GREEN control: with a DEFINED morph target both the original and the port set
  // out.userData to that morph target — proves the harness works and isolates the
  // failure above to the missing null-assignment branch.
  test("ISS-632(2) control: getRenderable with a DEFINED morphTarget sets out.userData to it (GREEN)") {
    val hack = new ModelInstanceHack(new Model())
    hack.userData = Nullable("INSTANCE-USERDATA".asInstanceOf[Any])

    val npp = freshNodePartPlus()
    val wv  = new WeightVector()
    npp.morphTargets = Nullable(wv)
    val out = new Renderable()
    val ret = hack.getRenderable(out, new Node(), npp)

    assert(ret.userData.get.asInstanceOf[AnyRef] eq wv, "a defined morph target must land on out.userData")
  }
}
