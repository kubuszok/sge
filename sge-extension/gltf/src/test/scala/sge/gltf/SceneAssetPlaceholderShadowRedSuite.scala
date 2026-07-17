/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * COMPILE-RED for ISS-723 clause c5 (wave 2026-07-17-F, territory W) — a dead placeholder class
 * `sge.gltf.SceneAsset` shadows the real `sge.gltf.scene3d.scene.SceneAsset` at the `sge.gltf`
 * package scope.
 *
 * Evidence:
 *   - sge-extension/gltf/src/main/scala/sge/gltf/SceneAsset.scala:1-21 is a documented placeholder
 *     ("Placeholder — full implementation follows", `Covenant: partial-port`) — an empty
 *     `class SceneAsset` with no members.
 *   - The REAL, full-port class is sge.gltf.scene3d.scene.SceneAsset (extends AutoCloseable, has
 *     scenes/scene/animations/textures/pixmaps/meshes/close).
 *   - Every live consumer explicitly imports the real one, e.g. GLTFLoaderBase.scala:39
 *     `import sge.gltf.scene3d.scene.{ SceneAsset, SceneModel }`; the placeholder has ZERO live
 *     references (no `import sge.gltf.SceneAsset` anywhere). It is dead code.
 *   - But because it is a member of package `sge.gltf`, any code at `sge.gltf` scope that names
 *     `SceneAsset` UNQUALIFIED binds to the dead placeholder, not the real class — the shadowing
 *     this clause reports.
 *
 * This suite lives in `package sge.gltf`, so unqualified `SceneAsset` resolves to the placeholder.
 * Calling `.close()` (a member of the REAL SceneAsset) does not compile against the placeholder —
 * proving the shadow. Fix: delete the dead placeholder (sge/gltf/SceneAsset.scala) so the name
 * `SceneAsset` is unambiguous.
 *
 * COMPILE-RED, committed LAST in the wave-F territory-W batch so earlier runnable suites stay
 * runnable until the implementer deletes the placeholder.
 */
package sge
package gltf

class SceneAssetPlaceholderShadowRedSuite extends munit.FunSuite {

  test("ISS-723c5: `SceneAsset` at sge.gltf scope must resolve to the real closeable class, not the dead placeholder") {
    // Unqualified `SceneAsset` here binds to the placeholder sge.gltf.SceneAsset (empty class).
    // `.close()` is a member of the real sge.gltf.scene3d.scene.SceneAsset only, so this fails to
    // compile while the placeholder shadows the name — the red.
    val asset = new SceneAsset()
    asset.close()
  }
}
