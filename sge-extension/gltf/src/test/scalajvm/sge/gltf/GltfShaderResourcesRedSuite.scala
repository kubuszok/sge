/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-508: gltf extension shader/texture classpath resources.
 *
 * Problem: every shader/texture classpath load in the gltf extension uses the
 * upstream `net/mgsx/gltf/shaders/...` paths, but the bundled resources live
 * under `sge/gltf/shaders/...` (sge-extension/gltf/src/main/resources/sge/gltf/shaders/).
 * Affected call sites (verified at red-sha):
 *   - PBRShaderProvider.scala:308  classpath("net/mgsx/gltf/shaders/pbr/pbr.vs.glsl")
 *   - PBRShaderProvider.scala:317  classpath("net/mgsx/gltf/shaders/pbr/pbr.fs.glsl")
 *   - PBRDepthShaderProvider.scala:65  classpath("net/mgsx/gltf/shaders/depth.vs.glsl")
 *   - PBRDepthShaderProvider.scala:74  classpath("net/mgsx/gltf/shaders/depth.fs.glsl")
 *   - PBREmissiveShaderProvider.scala:71  classpath("net/mgsx/gltf/shaders/gdx-pbr.vs.glsl")
 *   - PBREmissiveShaderProvider.scala:72  classpath("net/mgsx/gltf/shaders/emissive-only.fs.glsl")
 *   - SceneSkybox.scala:104-106  classpath("net/mgsx/gltf/shaders/skybox" + ".vs.glsl"/".fs.glsl")
 *   - IBLBuilder.scala:61-62  classpath("net/mgsx/gltf/shaders/ibl-sun.vs.glsl"/"ibl-sun.fs.glsl")
 * Additionally two files shipped by upstream gdx-gltf are missing from the
 * bundle entirely: `gdx-pbr.vs.glsl` (referenced by PBREmissiveShaderProvider;
 * NOTE: dangling in upstream sources too — only the published jar/GWT config
 * ships brdfLUT.png, see original-src/gdx-gltf/gltf/src/GLTF.gwt.xml) and
 * `brdfLUT.png` (upstream: gltf/src/net/mgsx/gltf/shaders/brdfLUT.png; loaded
 * by user code per gdx-gltf convention, so the bundle must carry it).
 *
 * Design rationale (implementation-direction-agnostic):
 *   - We do NOT hardcode assertions that the broken `net/mgsx/...` strings
 *     exist on the classpath; that would pin the wrong fix direction (moving
 *     resources instead of fixing code). Instead each test exercises the
 *     ACTUAL provider code path headlessly where possible and asserts the
 *     runtime-use outcome: the shader source resolves and is non-empty.
 *     These tests pass after the fix whether the code paths are repointed to
 *     `sge/gltf/...` (the mandated fix) or the resources are relocated.
 *   - PBRShaderProvider/PBRDepthShaderProvider/PBREmissiveShaderProvider
 *     expose static config/shader-source builders that only read files (no
 *     GL), so those are called directly.
 *   - SceneSkybox and IBLBuilder read their shaders inside constructors that
 *     subsequently require a GL context (which a headless test cannot
 *     provide). For those we assert that construction does not die with
 *     SgeError.FileReadError: today it does (the net/mgsx path is missing);
 *     after the fix the shader reads succeed and construction proceeds until
 *     it hits NoopGraphics.gl20 (UnsupportedOperationException) — any
 *     non-file-read outcome is accepted as "resources resolved".
 *   - The two entirely-missing files are asserted present at the canonical
 *     bundle location `sge/gltf/shaders/...` — the issue mandates adding them
 *     to the bundle; there is no code path to exercise for brdfLUT.png
 *     (it is a data resource consumed by user code).
 *   - A control test pins the existing bundle-location convention
 *     (`sge/gltf/shaders/pbr/pbr.vs.glsl` loads today), so a "fix" that moved
 *     the bundled resources to net/mgsx/... instead of fixing the code would
 *     turn this suite red again.
 *
 * Platform scope: JVM only (src/test/scalajvm). Classpath FileHandles resolve
 * via getResourceAsStream (FileHandles.scala:122,507-508); on Scala Native
 * that requires embedded resources, which are only enabled for provider JARs
 * (SgeNativeProviderPlugin.withEmbedResources), not for sge-gltf tests; on
 * Scala.js there is no classpath resource mechanism at all.
 */
package sge
package gltf

import sge.files.DesktopFiles
import sge.gltf.scene3d.scene.SceneSkybox
import sge.gltf.scene3d.shaders.{ PBRDepthShaderProvider, PBREmissiveShaderProvider, PBRShaderProvider }
import sge.gltf.scene3d.utils.IBLBuilder
import sge.graphics.Cubemap
import sge.graphics.g3d.environment.DirectionalLight
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.utils.SgeError
import lowlevel.Nullable

class GltfShaderResourcesRedSuite extends munit.FunSuite {

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

  /** Runs a provider/builder code path that needs GL after reading its shaders. Today the shader read itself fails with SgeError.FileReadError (wrong
    * classpath location); after the fix any non-file-read outcome (success, or hitting NoopGraphics' missing GL context) proves the resources resolved.
    */
  private def assertShaderResourcesResolve(label: String)(body: => Any): Unit = {
    try {
      body
      ()
    } catch {
      case e: SgeError.FileReadError =>
        fail(s"ISS-508: $label could not resolve a bundled shader resource: $e")
      case _: Throwable =>
        () // reached GL (or another non-file failure) — every shader resource resolved
    }
  }

  // -- (1) Runtime-use outcome of the provider code paths (headless) --------

  test("ISS-508: PBRShaderProvider.getDefaultVertexShader resolves bundled pbr.vs.glsl (PBRShaderProvider.scala:308)") {
    assert(PBRShaderProvider.getDefaultVertexShader().nonEmpty)
  }

  test("ISS-508: PBRShaderProvider.getDefaultFragmentShader resolves bundled pbr.fs.glsl (PBRShaderProvider.scala:317)") {
    assert(PBRShaderProvider.getDefaultFragmentShader().nonEmpty)
  }

  test("ISS-508: PBRDepthShaderProvider.getDefaultVertexShader resolves bundled depth.vs.glsl (PBRDepthShaderProvider.scala:65)") {
    assert(PBRDepthShaderProvider.getDefaultVertexShader().nonEmpty)
  }

  test("ISS-508: PBRDepthShaderProvider.getDefaultFragmentShader resolves bundled depth.fs.glsl (PBRDepthShaderProvider.scala:74)") {
    assert(PBRDepthShaderProvider.getDefaultFragmentShader().nonEmpty)
  }

  test("ISS-508: PBREmissiveShaderProvider.createConfig resolves gdx-pbr.vs.glsl + emissive-only.fs.glsl (PBREmissiveShaderProvider.scala:71-72)") {
    val config = PBREmissiveShaderProvider.createConfig(0)
    assert(!Nullable.isEmpty(config.vertexShader), "vertex shader (gdx-pbr.vs.glsl) not loaded")
    assert(!Nullable.isEmpty(config.fragmentShader), "fragment shader (emissive-only.fs.glsl) not loaded")
  }

  test("ISS-508: SceneSkybox construction resolves skybox.vs.glsl + skybox.fs.glsl (SceneSkybox.scala:104-106)") {
    // The single-cubemap constructor reads both skybox shaders BEFORE touching the cubemap or GL,
    // so a null cubemap is never dereferenced before the resource reads we are testing.
    assertShaderResourcesResolve("SceneSkybox constructor") {
      new SceneSkybox(null.asInstanceOf[Cubemap]) // @nowarn — see comment above
    }
  }

  test("ISS-508: IBLBuilder.createOutdoor resolves ibl-sun.vs.glsl + ibl-sun.fs.glsl (IBLBuilder.scala:61-62)") {
    // IBLBuilder's constructor reads both sun shaders (ShaderProgram(FileHandle, FileHandle)
    // delegates through readString()) before any GL call.
    assertShaderResourcesResolve("IBLBuilder.createOutdoor") {
      IBLBuilder.createOutdoor(new DirectionalLight())
    }
  }

  // -- (2) Files missing from the bundle entirely ---------------------------

  test("ISS-508: gdx-pbr.vs.glsl is bundled at sge/gltf/shaders/gdx-pbr.vs.glsl") {
    val handle = Sge().files.classpath("sge/gltf/shaders/gdx-pbr.vs.glsl")
    assert(handle.exists(), "gdx-pbr.vs.glsl (referenced by PBREmissiveShaderProvider.scala:71) is missing from the bundle")
  }

  test("ISS-508: brdfLUT.png is bundled at sge/gltf/shaders/brdfLUT.png") {
    val handle = Sge().files.classpath("sge/gltf/shaders/brdfLUT.png")
    assert(handle.exists(), "brdfLUT.png (upstream gltf/src/net/mgsx/gltf/shaders/brdfLUT.png) is missing from the bundle")
  }

  // -- (3) Control: pins the existing bundle location convention ------------

  test("ISS-508 control: bundled resources live under sge/gltf/shaders (pbr.vs.glsl loads today)") {
    val handle = Sge().files.classpath("sge/gltf/shaders/pbr/pbr.vs.glsl")
    assert(handle.exists(), "bundle convention changed: sge/gltf/shaders/pbr/pbr.vs.glsl no longer on classpath")
    assert(handle.readString().nonEmpty)
  }
}
