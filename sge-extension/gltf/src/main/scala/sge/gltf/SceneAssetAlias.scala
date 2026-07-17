/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-723 clause c5: the dead placeholder `class sge.gltf.SceneAsset` (an empty stub carrying a
 * `Covenant: partial-port` header) was deleted — the canonical full-port class is
 * `sge.gltf.scene3d.scene.SceneAsset`, and every live consumer imports THAT one. Deleting the
 * placeholder removed the name from the `sge.gltf` package scope entirely, which would leave any code
 * that names `SceneAsset` unqualified at `sge.gltf` scope unable to resolve it. Re-export the
 * canonical class at the `sge.gltf` package level so the name resolves — unambiguously — to the real
 * closeable class instead of the former dead placeholder.
 */
package sge
package gltf

export sge.gltf.scene3d.scene.SceneAsset
