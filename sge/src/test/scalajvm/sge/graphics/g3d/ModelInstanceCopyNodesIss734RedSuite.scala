/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests — ISS-734 umbrella clauses c2 and c3 (core minors #2), wave
 * 2026-07-17-F, territory X. Reproducer-authored: MUST NOT be modified by the
 * fixer; they encode the ORIGINAL LibGDX semantics
 * (com/badlogic/gdx/graphics/g3d/ModelInstance.java, original-src/libgdx), not
 * the port's.
 *
 * --- c2: copyNodesById missing `break` ---
 *
 * Original copyNodes(Array<Node>, String... nodeIds) lines 229-240:
 *   for (Node node : nodes)
 *     for (String nodeId : nodeIds)
 *       if (nodeId.equals(node.id)) { this.nodes.add(node.copy()); break; }
 * The `break` (line 235) stops after the FIRST matching id, so a node is copied
 * AT MOST ONCE even if nodeIds lists the same id several times.
 *
 * The port copyNodesById (ModelInstance.scala lines 178-184) drops the break:
 *   for (node <- nodes) for (nodeId <- nodeIds) if (nodeId == node.id) this.nodes.add(node.copy())
 * so a duplicated id copies the node once PER duplicate.
 *
 * --- c3: bindPose rebind conditional vs unconditional null-store (DESIGN-PIN) ---
 *
 * Original invalidate(Node) line 263 rebinds every bone key UNCONDITIONALLY:
 *   bindPose.keys[j] = getNode(bindPose.keys[j].id);
 * getNode returns null when the bone id is not in THIS instance's node tree, so
 * a bone that referenced a node outside the instance is SEVERED (set to null) —
 * it no longer points into the foreign (source) tree.
 *
 * The port (ModelInstance.scala lines 193-196) rebinds only WHEN found:
 *   getNode(boneNode.id).foreach { replacement => bindPose.setKeyAt(j, replacement) }
 * In the not-found edge the old key is left untouched, so the copied instance
 * keeps a STALE reference into the foreign tree.
 *
 * NOTE (design dependency, flagged for the orchestrator): faithfully storing
 * null here is not a pure ModelInstance change — (a) the port's key type is the
 * non-nullable ArrayMap[Node, Matrix4], and (b) Node.calculateBoneTransforms
 * (Node.scala:126-141, OUTSIDE territory X) dereferences bindPose.getKeyAt(i)
 * .globalTransform, so a null key NPEs there (upstream LibGDX has the same
 * latent NPE). The complete fix spans ModelInstance + Node. This red pins the
 * observable stale-reference on the CURRENT tree (which does not NPE because the
 * foreign key stays non-null).
 */
package sge
package graphics
package g3d

import sge.graphics.g3d.model.{ MeshPart, Node, NodePart }
import sge.math.Matrix4
import lowlevel.Nullable
import lowlevel.util.ArrayMap

class ModelInstanceCopyNodesIss734RedSuite extends munit.FunSuite {

  test(
    "ISS-734 c2: copyNodesById copies a matching root once even when its id is listed multiple times (orig break, ModelInstance.java:235)"
  ) {
    given Sge = SgeTestFixture.testSge()

    val model = new Model()
    val root  = new Node()
    root.id = "root"
    model.nodes.add(root)

    // rootNodeIds lists "root" twice. The original's `break` copies it once.
    val instance = new ModelInstance(model, Nullable(Seq("root", "root")))

    assertEquals(
      instance.nodes.size,
      1,
      "a duplicated root id must copy the node once (ModelInstance.java:235 break); " +
        "the port drops the break and copies it once per duplicate"
    )
  }

  test(
    "ISS-734 c3 (design-pin): a bone key referencing a node outside the instance tree must be severed on copy (orig ModelInstance.java:263 unconditional null-store)"
  ) {
    given Sge = SgeTestFixture.testSge()

    // A bone node that will NOT be part of the ModelInstance's node tree.
    val foreignBone = new Node()
    foreignBone.id = "bone"

    // A root node with one part whose bind pose keys the foreign bone.
    val root = new Node()
    root.id = "root"
    val part = new NodePart()
    part.meshPart = new MeshPart() // mesh stays null; not dereferenced during construction
    part.material = new Material("mat")
    val binds = ArrayMap[Node, Matrix4]()
    binds.put(foreignBone, Matrix4())
    part.invBoneBindTransforms = Nullable(binds)
    root.parts.add(part)

    val model = new Model()
    model.nodes.add(root)

    // Copying the model into an instance runs invalidate() on the copied nodes.
    // getNode("bone") is not found in the instance tree {root}.
    val instance = new ModelInstance(model)

    val copiedBinds = instance.nodes(0).parts(0).invBoneBindTransforms.getOrElse(fail("copied part lost its bind pose"))
    val key0        = copiedBinds.getKeyAt(0)

    assert(
      key0 ne foreignBone,
      "a not-found bone key must be severed from the foreign source tree (ModelInstance.java:263 stores getNode(id), which is null when not found); " +
        "the port's conditional rebind (ModelInstance.scala:193-196) leaves the stale foreign Node reference in place"
    )
  }
}
