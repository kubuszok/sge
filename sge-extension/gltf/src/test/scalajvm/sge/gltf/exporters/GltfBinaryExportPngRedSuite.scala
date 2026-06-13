/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-533 clause 1, JVM copy — the GREEN CONTROL. On the JVM the raw
 * reflection in GLTFBinaryExporter.savePNG (Class.forName("sge.graphics.
 * PixmapIO") at GLTFBinaryExporter.scala:161, getMethod :162, invoke :163)
 * resolves PixmapIO's static forwarders, so this suite passes BEFORE the fix —
 * proving the test body (headless 2x2 pixmap, byte-capturing FileHandleStream,
 * PNG signature + decode round-trip) is sound. The JS and Native copies of
 * this suite are the red: there the same code fails at link time because
 * neither platform implements java.lang reflection. After the fix (direct
 * sge.graphics.PixmapIO.writePNG call, PixmapIO.scala:90 — shared source on
 * all platforms) all three copies must pass.
 *
 * See GltfBinaryExportPngRedSuiteBase for the full upstream citation
 * (GLTFBinaryExporter.java:145-159) and fixture rationale.
 */
package sge
package gltf
package exporters

class GltfBinaryExportPngRedSuite extends GltfBinaryExportPngRedSuiteBase
