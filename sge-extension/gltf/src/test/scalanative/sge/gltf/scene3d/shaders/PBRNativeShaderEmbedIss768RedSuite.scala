/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-768: the gltf PBR default shaders cannot be loaded on
 * Scala Native.
 *
 * Symptom (reproduced by the tests below): the very first thing
 * `SceneManager(maxBones)` reaches is `PBRShaderProvider.getDefaultVertexShader`
 * / `getDefaultFragmentShader`, which read `sge/gltf/shaders/pbr/pbr.vs.glsl`
 * and `pbr.fs.glsl` from the classpath (PBRShaderProvider.scala:306-320). On
 * Scala Native a classpath FileHandle resolves ONLY over link-time-embedded
 * resources (FileHandles.scala:502,508 use `getClass.getResourceAsStream`), so
 * these reads throw `SgeError.FileReadError` and no PBR scene can be created on
 * Native. Core g3d does not hit this because it embeds its GLSL as Scala string
 * constants (DefaultShader.scala:1226-1227) instead of classpath files.
 *
 * Root cause per the issue: SgeNativeProviderPlugin.scala:28-44 (sge-build/)
 * lists the Scala-Native resource-embed include patterns and OMITS `**.glsl`
 * (and `**.gltf`/`**.glb`/`**.bin`), so the bundled `.glsl` files never enter a
 * native binary.
 *
 * REPRODUCER FINDING (recorded because it steers the fix): this suite embeds a
 * probe that these two assertions replaced — a load of `brdfLUT.png`, a bundled
 * resource whose `.png` extension IS already in the embed patterns. On this
 * gltf Native TEST binary that probe ALSO fails, i.e. NOTHING embeds here. The
 * reason: nowhere in the build is `NativeProviderPlugin` actually
 * `.enablePlugins`-enabled — `nativeProviderSettings` (build.sbt:252-254) and
 * `SgeDesktopNativePlatform` (SgePlugins.scala:117-122) only INJECT
 * `NativeProviderPlugin.projectSettings`. `SgeNativeProviderPlugin`
 * (SgeNativeProviderPlugin.scala:19-22) has `requires = NativeProviderPlugin`,
 * so it never auto-triggers on the library/extension/test projects, and its
 * `withEmbedResources`/`withResourceIncludePatterns` never take effect there.
 * Real apps embed narrowly and by hand instead (regressionTest, ISS-560,
 * build.sbt:438-441 — explicitly because broad embedding "OOMs the
 * ResourceEmbedder"). Consequence for the two fix options:
 *   (A) adding `**.glsl` to SgeNativeProviderPlugin's patterns is fragile — it
 *       only helps binaries where that plugin is actually in effect (not this
 *       module's own native tests), broadens embedding toward the ISS-560 OOM
 *       risk, and can NEVER help browser (Scala.js has no classpath resources,
 *       ISS-553).
 *   (B) inlining the gltf shaders as string constants like core g3d works on
 *       every platform uniformly, needs no embed-plugin cooperation, and makes
 *       `getDefaultVertexShader`/`getDefaultFragmentShader` resolve on THIS
 *       native binary — turning these two assertions green.
 * The reproducer therefore recommends (B), and these assertions encode it: they
 * pass once the default PBR shaders no longer depend on Native resource
 * embedding. (If (A) is pursued instead, it must additionally make this
 * module's own native binary embed the `.glsl` files for this red to pass, and
 * browser would remain unaddressed.)
 *
 * Platform scope: SCALA NATIVE ONLY (src/test/scalanative). This is the only
 * platform where the bug manifests. The JVM classpath resolves these resources
 * regardless of embedding (the ISS-508 scalajvm suite pins that green); Scala.js
 * has no classpath resource mechanism (the gltf extension drops JS, ISS-553).
 */
package sge
package gltf
package scene3d
package shaders

import sge.files.DesktopFiles
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }

class PBRNativeShaderEmbedIss768RedSuite extends munit.FunSuite {

  /** Minimal headless Sge with a real (classpath-capable) Files implementation. */
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

  // The SceneManager(maxBones) shader-load path, exercised directly. Both are
  // static, GL-free (they only read + parse shader sources), so they run
  // headless. On Native at HEAD they throw SgeError.FileReadError (nothing
  // embedded); the sound fix (B, inline constants) makes them resolve.
  test(
    "ISS-768: PBRShaderProvider.getDefaultVertexShader resolves the default PBR vertex shader on Native (SceneManager(maxBones) path)"
  ) {
    assert(PBRShaderProvider.getDefaultVertexShader().nonEmpty)
  }

  test(
    "ISS-768: PBRShaderProvider.getDefaultFragmentShader resolves the default PBR fragment shader on Native (SceneManager(maxBones) path)"
  ) {
    assert(PBRShaderProvider.getDefaultFragmentShader().nonEmpty)
  }
}
