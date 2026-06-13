/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-533 clause 1, Scala Native copy — the RED. Making savePNG reachable
 * from a test makes the `sge-gltf` Test / nativeLink step FAIL: Scala Native
 * implements neither java.lang.Class.forName / getMethod (GLTFBinaryExporter.
 * scala:161-162) nor java.lang.reflect.Method.invoke (:163), so linking
 * reports unreachable symbols and the WHOLE sge-gltf Native test binary cannot
 * be produced — no test in the module runs. That blast radius is the bug
 * itself: the reflection breaks the Native baseline for every downstream user
 * whose code can reach savePNG (ISS-533). The JVM copy of this suite is the
 * green control proving the test body is sound; after the fix (direct
 * sge.graphics.PixmapIO.writePNG call, PixmapIO.scala:90 — shared source on
 * all platforms) this copy must link and pass.
 *
 * See GltfBinaryExportPngRedSuiteBase for the full upstream citation
 * (GLTFBinaryExporter.java:145-159; upstream goes through LibGDX's
 * GWT-emulated ClassReflection facade, lines 152-154, NOT raw java.lang
 * reflection) and fixture rationale.
 */
package sge
package gltf
package exporters

class GltfBinaryExportPngRedSuite extends GltfBinaryExportPngRedSuiteBase
