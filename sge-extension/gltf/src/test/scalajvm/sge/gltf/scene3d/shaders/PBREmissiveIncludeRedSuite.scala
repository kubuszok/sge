/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-605 — PBREmissiveShaderProvider.createConfig loads
 * gdx-pbr.vs.glsl verbatim via files.classpath(...).readString() with NO
 * ShaderParser #include-expansion pass (PBREmissiveShaderProvider.scala:76). The
 * bundled sge/gltf/shaders/gdx-pbr.vs.glsl is a byte copy of pbr/pbr.vs.glsl and
 * its line 3 is `#include <compat.vs.glsl>`. So config.vertexShader carries an
 * unexpanded `#include` directive — GLSL compilers reject `#include`, and even a
 * ShaderParser pass at bundle root would miss `compat.vs.glsl` (it lives under
 * sge/gltf/shaders/pbr/). The vertex stage handed to the GPU is therefore broken.
 *
 * AC (from the issue): assert no `#include` remains in config.vertexShader. The
 * fix may flatten the copy, run ShaderParser at the call site, or point at
 * getDefaultVertexShader — this assertion passes for any of them.
 *
 * Platform scope: JVM only (src/test/scalajvm) — classpath FileHandles resolve
 * via getResourceAsStream, which needs embedded resources on Native (only
 * enabled for provider JARs) and does not exist on JS. Mirrors the scope of
 * GltfShaderResourcesRedSuite.
 */
package sge
package gltf
package scene3d
package shaders

import sge.files.DesktopFiles
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import lowlevel.Nullable

class PBREmissiveIncludeRedSuite extends munit.FunSuite {

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
    Sge(app, new NoopGraphics(), new NoopAudio(), new DesktopFiles(), new NoopInput(), null.asInstanceOf[Net]) // @nowarn — net unused
  }

  test(
    "ISS-605: PBREmissiveShaderProvider.createConfig vertex shader has no unexpanded #include (PBREmissiveShaderProvider.scala:76)"
  ) {
    val config = PBREmissiveShaderProvider.createConfig(0)
    assert(!Nullable.isEmpty(config.vertexShader), "vertex shader must be loaded")
    val vs = config.vertexShader.get
    assert(
      !vs.contains("#include"),
      s"config.vertexShader carries an unexpanded #include (gdx-pbr.vs.glsl:3 `#include <compat.vs.glsl>`); GLSL cannot compile it"
    )
  }
}
