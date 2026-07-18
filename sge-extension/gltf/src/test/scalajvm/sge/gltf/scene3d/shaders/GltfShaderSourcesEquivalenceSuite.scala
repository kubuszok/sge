/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-768 correctness guard (JVM): proves the embedded shader source constants
 * in GltfShaderSources are BYTE-IDENTICAL to what the old classpath-based path
 * produced, so Option B (inlining) is a behaviour-preserving change on the
 * platforms where classpath resources already worked (JVM/Android), and the
 * exact same shader string is what now loads on Scala Native.
 *
 *   - For leaf shaders (read via readString()): GltfShaderSources.source(path)
 *     must equal files.classpath(path).readString().
 *   - For the PBR shaders (read via ShaderParser.parse with #include):
 *     GltfShaderSources.parse(path) must equal ShaderParser.parse(classpath).
 *
 * This also permanently guards against drift: if a bundled .glsl file is edited
 * without regenerating GltfShaderSources, this suite goes red.
 *
 * Platform scope: JVM only (src/test/scalajvm) — it deliberately compares
 * against the CLASSPATH path, which only resolves on the JVM.
 */
package sge
package gltf
package scene3d
package shaders

import sge.files.DesktopFiles
import sge.gltf.scene3d.utils.ShaderParser
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }

class GltfShaderSourcesEquivalenceSuite extends munit.FunSuite {

  private given Sge = {
    val app = new Application {
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
    Sge(app, new NoopGraphics(), new NoopAudio(), new DesktopFiles(), new NoopInput(), null.asInstanceOf[Net]) // @nowarn — net unused in these tests
  }

  private val leafShaders = List(
    "sge/gltf/shaders/depth.vs.glsl",
    "sge/gltf/shaders/depth.fs.glsl",
    "sge/gltf/shaders/emissive-only.fs.glsl",
    "sge/gltf/shaders/ibl-sun.vs.glsl",
    "sge/gltf/shaders/ibl-sun.fs.glsl",
    "sge/gltf/shaders/skybox.vs.glsl",
    "sge/gltf/shaders/skybox.fs.glsl",
    "sge/gltf/shaders/pbr/compat.vs.glsl",
    "sge/gltf/shaders/pbr/compat.fs.glsl",
    "sge/gltf/shaders/pbr/functions.glsl",
    "sge/gltf/shaders/pbr/iridescence.glsl",
    "sge/gltf/shaders/pbr/material.glsl",
    "sge/gltf/shaders/pbr/env.glsl",
    "sge/gltf/shaders/pbr/lights.glsl",
    "sge/gltf/shaders/pbr/shadows.glsl",
    "sge/gltf/shaders/pbr/ibl.glsl"
  )

  leafShaders.foreach { path =>
    test(s"ISS-768: embedded source matches classpath readString for $path") {
      assertEquals(GltfShaderSources.source(path), Sge().files.classpath(path).readString())
    }
  }

  private val includeShaders = List(
    "sge/gltf/shaders/pbr/pbr.vs.glsl",
    "sge/gltf/shaders/pbr/pbr.fs.glsl"
  )

  includeShaders.foreach { path =>
    test(s"ISS-768: embedded #include-resolved parse matches ShaderParser(classpath) for $path") {
      assertEquals(GltfShaderSources.parse(path), ShaderParser.parse(Sge().files.classpath(path)))
    }
  }
}
