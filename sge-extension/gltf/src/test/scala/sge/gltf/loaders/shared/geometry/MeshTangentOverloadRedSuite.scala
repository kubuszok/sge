/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-533 clause 2 — MeshTangentSpaceGenerator dropped the public
 * Mesh-level computeTangentSpace overload despite carrying a Covenant: full-port
 * header (Covenant-source-reference: net/mgsx/gltf/loaders/shared/geometry/
 * MeshTangentSpaceGenerator.java).
 *
 * Upstream public API (original-src/gdx-gltf/gltf/src/net/mgsx/gltf/loaders/
 * shared/geometry/MeshTangentSpaceGenerator.java):
 *   - lines 14-40: public static void computeTangentSpace(Mesh mesh,
 *     Material material, boolean computeNormals, boolean computeTangents)
 *       - line 16: rejects non-indexed meshes (IllegalArgumentException)
 *       - lines 18-21: copies the vertex/index arrays out of the mesh
 *       - lines 23-24: requires PBRTextureAttribute.NormalTexture on the
 *         material (IllegalArgumentException otherwise)
 *       - lines 26-34: locates the UV attribute whose unit matches
 *         normalMap.uvIndex (IllegalArgumentException if absent)
 *       - line 36: delegates to the array-level overload
 *       - lines 38-39: writes the mutated arrays back via
 *         mesh.setVertices / mesh.setIndices
 *   - lines 42-45: public static void computeTangentSpace(float[] vertices,
 *     short[] indices, VertexAttributes attributesGroup, boolean computeNormals,
 *     boolean computeTangents, VertexAttribute normalMapUVs)
 *
 * The port (MeshTangentSpaceGenerator.scala) has ONLY the array-level overload
 * (line 25); the Mesh-level overload is gone. The test below references it per
 * the upstream signature, so this file FAILS TO COMPILE until the overload is
 * restored — the compile error IS the red. The green control exercises the
 * surviving array-level overload on the identical fixture, proving the fixture
 * and the expected normals/tangents are valid independent of the missing
 * overload.
 *
 * Fixture math (one CCW right triangle in the XY plane, UVs chosen so the
 * texture U axis maps to model +X after upstream's V flip at java lines
 * 162/165/168):
 *   - computeNormals (java lines 47-123): face normal = ((B-A) x (C-A)).nor()
 *     = (0,0,1) for every vertex.
 *   - computeTangents (java lines 127-227): du1=1,dv1=0,du2=0,dv2=1 => r=1,
 *     tangent=(1,0,0); biNormal=(normal x tangent)=(0,1,0), dot(tan2)=1 => w=+1
 *     (java lines 216-225, tangent is 4 components, w written at line 225).
 *
 * This test is written by the reproducer agent and MUST NOT be modified by the
 * fixer: it encodes the original Java semantics, not the port's.
 */
package sge
package gltf
package loaders
package shared
package geometry

import sge.graphics.{ Mesh, Texture, VertexAttribute, VertexAttributes }
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g3d.Material
import sge.graphics.g3d.utils.TextureDescriptor
import sge.gltf.scene3d.attributes.PBRTextureAttribute
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }

class MeshTangentOverloadRedSuite extends munit.FunSuite {

  import MeshTangentOverloadRedSuite.*

  private given Sge = Sge(NoopApplicationStub, new NoopGraphics(), new NoopAudio(), NoopFilesStub, new NoopInput(), NoopNetStub)

  // Vertex layout: pos(3) + normal(3) + uv(2) + tangent(4) = stride 12 floats.
  // Upstream writes 4 tangent components (java line 225), so the attribute is
  // built manually — VertexAttribute.Tangent() in SGE core is the 3-component
  // variant.
  private def attributes(): (VertexAttributes, VertexAttribute) = {
    val uv    = VertexAttribute.TexCoords(0)
    val group = new VertexAttributes(
      VertexAttribute.Position(),
      VertexAttribute.Normal(),
      uv,
      new VertexAttribute(Usage.Tangent, 4, "a_tangent")
    )
    (group, uv)
  }

  // A(0,0,0) uv(0,1) / B(1,0,0) uv(1,1) / C(0,1,0) uv(0,0): after the V flip
  // the triangle's UV frame is the identity, making the expected tangent space
  // exact. Normals and tangents start zeroed — the generator must fill them.
  private def vertexData(): Array[Float] = Array[Float](
    // x, y, z, nx, ny, nz, u, v, tx, ty, tz, tw
    0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f
  )

  private val indexData: Array[Short] = Array[Short](0, 1, 2)

  private val Stride = 12

  private def assertTangentSpace(vertices: Array[Float], where: String): Unit =
    for (v <- 0 until 3) {
      val base = v * Stride
      // computeNormals (java lines 47-123): the CCW XY triangle's normal is +Z.
      assertEqualsFloat(vertices(base + 3), 0f, 0.0001f, s"$where: normal.x of vertex $v")
      assertEqualsFloat(vertices(base + 4), 0f, 0.0001f, s"$where: normal.y of vertex $v")
      assertEqualsFloat(vertices(base + 5), 1f, 0.0001f, s"$where: normal.z of vertex $v")
      // computeTangents (java lines 127-227): U maps to +X, handedness w = +1.
      assertEqualsFloat(vertices(base + 8), 1f, 0.0001f, s"$where: tangent.x of vertex $v")
      assertEqualsFloat(vertices(base + 9), 0f, 0.0001f, s"$where: tangent.y of vertex $v")
      assertEqualsFloat(vertices(base + 10), 0f, 0.0001f, s"$where: tangent.z of vertex $v")
      assertEqualsFloat(vertices(base + 11), 1f, 0.0001f, s"$where: tangent.w of vertex $v")
    }

  // RED: the Mesh-level overload (MeshTangentSpaceGenerator.java:14-40) is
  // missing from the port — this call does not compile until it is restored.
  test(
    "ISS-533 (2): Mesh-level computeTangentSpace(mesh, material, computeNormals, computeTangents) computes normals + tangents in place (MeshTangentSpaceGenerator.java:14-40)"
  ) {
    val (attrs, _) = attributes()
    // VertexArray-backed mesh: pure ByteBuffer storage, no GL context needed
    // until bind() — safe headless on JVM, JS and Native.
    val mesh = new Mesh(Mesh.VertexDataType.VertexArray, false, 3, 3, attrs)
    try {
      mesh.setVertices(vertexData())
      mesh.setIndices(indexData)
      // Upstream requirement (java lines 23-24): the material carries the
      // normal map whose uvIndex (default 0) selects the UV attribute.
      val material = new Material(new PBRTextureAttribute(PBRTextureAttribute.NormalTexture, new TextureDescriptor[Texture]()))
      // RED: upstream signature, MeshTangentSpaceGenerator.java:14 —
      //   public static void computeTangentSpace(Mesh mesh, Material material,
      //     boolean computeNormals, boolean computeTangents)
      // The port dropped this overload, so the call below fails to compile.
      MeshTangentSpaceGenerator.computeTangentSpace(mesh, material, true, true)
      // Upstream writes the results back into the mesh (java lines 38-39).
      val out = new Array[Float](3 * Stride)
      mesh.getVertices(out)
      assertTangentSpace(out, "mesh overload")
    } finally
      mesh.close()
  }

  // GREEN control: the surviving array-level overload (java lines 42-45 /
  // MeshTangentSpaceGenerator.scala line 25) on the identical fixture — proves
  // the fixture and the expected tangent-space values independently of the
  // missing Mesh-level overload.
  test("ISS-533 (2) control: array-level computeTangentSpace computes normals + tangents (GREEN)") {
    val (attrs, uv) = attributes()
    val vertices    = vertexData()
    MeshTangentSpaceGenerator.computeTangentSpace(vertices, indexData, attrs, true, true, uv)
    assertTangentSpace(vertices, "array overload")
  }
}

object MeshTangentOverloadRedSuite {

  // Minimal headless Sge fixture — mirrors sge/src/test/scala/sge/SgeTestFixture.scala,
  // which is not on the gltf extension's test classpath (no test->test dependency).
  private object NoopApplicationStub extends Application {
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

  private object NoopFilesStub extends Files {
    def getFileHandle(path: String, fileType: sge.files.FileType): sge.files.FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                                       String               = ""
    def isExternalStorageAvailable:                                Boolean              = false
    def localStoragePath:                                          String               = ""
    def isLocalStorageAvailable:                                   Boolean              = false
  }

  private object NoopNetStub extends Net {
    import Net.*
    def httpClient:                                                                                         sge.net.SgeHttpClient = sge.net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: sge.net.ServerSocketHints): sge.net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   sge.net.ServerSocketHints):             sge.net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: sge.net.SocketHints):       sge.net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                                  Boolean               = false
  }
}
