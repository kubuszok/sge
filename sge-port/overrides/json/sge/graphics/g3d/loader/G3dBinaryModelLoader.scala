/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/loader/G3dModelLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Origin: SGE-original (UBJSON variant of G3dModelLoader for .g3db binary format)
 *   Convention: extends G3dModelLoader, overrides parseModel to read UBJSON via kindlings
 *   Idiom: split packages, (using Sge) context
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 46
 * Covenant-baseline-methods: G3dBinaryModelLoader,json,model,parseModel
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/loader/G3dModelLoader.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 34cc595deb4ac09ee476c6b1aba1b805f4dc81a7
 */
package sge
package graphics
package g3d
package loader

import sge.assets.loaders.FileHandleResolver

/** Loads G3D models from `.g3db` (Universal Binary JSON) files.
  *
  * Reuses all parsing logic from [[G3dModelLoader]] — only the deserialization format differs.
  */
class G3dBinaryModelLoader(resolver: FileHandleResolver)(using Sge) extends G3dModelLoader(resolver, binary = true)
