/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-509: CUBICSPLINE glTF animations always throw
 * ClassCastException.
 *
 * Problem: in upstream gdx-gltf, CubicVector3 extends Vector3 and
 * CubicQuaternion extends Quaternion, so cubic keyframe values are stored
 * directly in NodeKeyframe<Vector3>/NodeKeyframe<Quaternion>
 * (original-src/gdx-gltf/gltf/src/net/mgsx/gltf/loaders/shared/animation/
 * AnimationLoader.java:85-92 translation, :107-114 rotation, :129-136 scale)
 * and cast back during evaluation
 * (net/mgsx/gltf/scene3d/animation/AnimationControllerHack.java:196-209,
 * :281-296, :325-340). In SGE, CubicVector3/CubicQuaternion are standalone
 * wrapper case classes UNRELATED to Vector3/Quaternion
 * (sge/gltf/scene3d/model/CubicVector3.scala:31, CubicQuaternion.scala:27),
 * yet AnimationLoader force-casts them via
 * `.asInstanceOf[AnyRef].asInstanceOf[Vector3]` /
 * `.asInstanceOf[AnyRef].asInstanceOf[Quaternion]`
 * (AnimationLoader.scala:102, :108, :133, :139, :164, :170) and
 * AnimationControllerHack casts back
 * (AnimationControllerHack.scala:201-202, :272-273, :313-314). Both casts
 * are checkcasts against unrelated classes, so loading ANY cubic-spline
 * animation throws ClassCastException at AnimationLoader.scala:108 (first
 * keyframe of the first CUBICSPLINE channel; :102 when input(0) > 0).
 *
 * Fixture: a minimal embedded glTF 2.0 document (pure JSON string with a
 * base64 `data:` buffer, no file system, no GL) with one animation playing
 * three CUBICSPLINE samplers on one node: translation (Vector3 path),
 * rotation (Quaternion path) and scale (Vector3 path). Per the glTF spec
 * (and GLTFTypes.java:89-99) each cubic keyframe is an
 * (in-tangent, value, out-tangent) triplet.
 *
 * Design rationale (implementation-direction-agnostic):
 *   - We do NOT assert the runtime class of keyframe values — the fix may
 *     restore subtyping, change the keyframe payload, or store tangents
 *     elsewhere. Instead we drive the narrowest real load path
 *     (GLTFJsonParser.parse -> DataResolver -> AnimationLoader.load, the
 *     exact wiring of GLTFLoaderBase.scala:109) and the real evaluation
 *     path (AnimationControllerHack.applyAnimationPlus in direct mode, as
 *     applyAnimation does at AnimationControllerHack.scala:72), then assert
 *     the node's evaluated localTransform against values hand-derived from
 *     the original Java math:
 *       cubic Hermite p(t) = (2t^3-3t^2+1)p0 + (t^3-2t^2+t)m0
 *                          + (-2t^3+3t^2)p1 + (t^3-t^2)m1
 *     with p0/p1 the anchor values, m0 the first keyframe's out-tangent and
 *     m1 the second keyframe's in-tangent
 *     (AnimationControllerHack.java:205, :215-220 for Vector3;
 *     :231-245 for Quaternion, where tangents are additionally multiplied
 *     by delta = -(t1 - t0) and the sum is normalized).
 *   - The keyframe times are 0 and 1, so delta == 1 and the spec-vs-port
 *     question of tangent scaling by delta cannot skew the expected values.
 *   - Unused tangents (first keyframe's in-tangent, last keyframe's
 *     out-tangent) are filled with junk so that any fix that misindexes the
 *     in/value/out triplet produces wrong numbers instead of passing.
 *   - A LINEAR control test runs the identical harness without any cubic
 *     cast involved, proving the fixture, parser, resolver and controller
 *     wiring work at red-sha.
 *
 * Platform scope: shared test scope (JVM + JS + Native). The whole path is
 * data-level: jsoniter parsing, java.nio buffers and java.util.Base64, all
 * already used cross-platform by main sources (SeparatedDataFileResolver).
 */
package sge
package gltf

import java.nio.{ ByteBuffer, ByteOrder }

import sge.gltf.data.GLTF
import sge.gltf.data.texture.GLTFImage
import sge.gltf.loaders.gltf.GLTFJsonParser
import sge.gltf.loaders.shared.animation.AnimationLoader
import sge.gltf.loaders.shared.data.{ DataFileResolver, DataResolver }
import sge.gltf.loaders.shared.scene.NodeResolver
import sge.gltf.scene3d.animation.AnimationControllerHack
import sge.graphics.Pixmap
import sge.graphics.g3d.model.{ Animation, Node }
import sge.math.{ Matrix4, Quaternion, Vector3 }

class CubicSplineAnimationRedSuite extends munit.FunSuite {

  // -- Embedded glTF fixture -------------------------------------------------

  /** In-memory DataFileResolver decoding base64 `data:` buffer URIs exactly like SeparatedDataFileResolver.loadBuffers (SeparatedDataFileResolver.scala:45-69), with no file system access so the
    * fixture stays a pure string on every platform.
    */
  final private class EmbeddedDataFileResolver(model: GLTF) extends DataFileResolver {
    private val bufferMap: Map[Int, ByteBuffer] =
      model.buffers.fold(Map.empty[Int, ByteBuffer]) { buffers =>
        buffers.zipWithIndex.map { case (glBuffer, i) =>
          val buffer = ByteBuffer.allocate(glBuffer.byteLength)
          buffer.order(ByteOrder.LITTLE_ENDIAN)
          glBuffer.uri.foreach { uri =>
            // data:application/octet-stream;base64,
            val body = uri.split(",", 2)(1)
            buffer.put(java.util.Base64.getDecoder.decode(body))
          }
          i -> buffer
        }.toMap
      }

    override def load(file: files.FileHandle): Unit = throw new UnsupportedOperationException("fixture is in-memory")
    override def getRoot:                      GLTF = model

    override def getBuffer(buffer: Int): ByteBuffer = bufferMap(buffer)

    override def load(glImage: GLTFImage): Pixmap = throw new UnsupportedOperationException("fixture has no images")
  }

  private def base64Floats(data: Array[Float]): String = {
    val bb = ByteBuffer.allocate(data.length * 4)
    bb.order(ByteOrder.LITTLE_ENDIAN)
    data.foreach(f => bb.putFloat(f))
    java.util.Base64.getEncoder.encodeToString(bb.array())
  }

  /** Builds a complete glTF 2.0 JSON document: one node, one animation with translation + rotation + scale channels sharing one time accessor. */
  private def fixtureJson(
    name:           String,
    interpolation:  String,
    times:          Array[Float],
    translationOut: Array[Float],
    rotationOut:    Array[Float],
    scaleOut:       Array[Float]
  ): String = {
    def buffer(data: Array[Float]): String =
      s"""{"byteLength":${data.length * 4},"uri":"data:application/octet-stream;base64,${base64Floats(data)}"}"""
    def bufferView(index: Int, data: Array[Float]): String =
      s"""{"buffer":$index,"byteOffset":0,"byteLength":${data.length * 4}}"""
    s"""{
       |  "asset": {"version": "2.0"},
       |  "nodes": [{"name": "animated"}],
       |  "buffers": [${buffer(times)}, ${buffer(translationOut)}, ${buffer(rotationOut)}, ${buffer(scaleOut)}],
       |  "bufferViews": [${bufferView(0, times)}, ${bufferView(1, translationOut)}, ${bufferView(2, rotationOut)}, ${bufferView(3, scaleOut)}],
       |  "accessors": [
       |    {"bufferView":0,"componentType":5126,"count":${times.length},"type":"SCALAR","min":[${times.head}],"max":[${times.last}]},
       |    {"bufferView":1,"componentType":5126,"count":${translationOut.length / 3},"type":"VEC3"},
       |    {"bufferView":2,"componentType":5126,"count":${rotationOut.length / 4},"type":"VEC4"},
       |    {"bufferView":3,"componentType":5126,"count":${scaleOut.length / 3},"type":"VEC3"}
       |  ],
       |  "animations": [{
       |    "name": "$name",
       |    "samplers": [
       |      {"input":0,"output":1,"interpolation":"$interpolation"},
       |      {"input":0,"output":2,"interpolation":"$interpolation"},
       |      {"input":0,"output":3,"interpolation":"$interpolation"}
       |    ],
       |    "channels": [
       |      {"sampler":0,"target":{"node":0,"path":"translation"}},
       |      {"sampler":1,"target":{"node":0,"path":"rotation"}},
       |      {"sampler":2,"target":{"node":0,"path":"scale"}}
       |    ]
       |  }]
       |}""".stripMargin
  }

  private val z71 = 0.70710678f // sin(45 deg) == cos(45 deg): keyframe 1 rotates 90 deg about Z

  /** CUBICSPLINE output accessors: per keyframe an (in-tangent, value, out-tangent) triplet (GLTFTypes.java:89-99, glTF 2.0 appendix C). Unused tangents (k0 in, k1 out) are junk on purpose: a fix
    * that misindexes the triplet must fail the value assertions.
    */
  private val cubicTimes          = Array(0f, 1f)
  private val cubicTranslationOut = Array(
    7f, 8f, 9f, /* k0 in (unused junk) */ 0f, 0f, 0f, /* k0 value */ 1f, 2f, 3f, /* k0 out */
    4f, 5f, 6f, /* k1 in */ 10f, 20f, 30f, /* k1 value */ 11f, 12f, 13f /* k1 out (unused junk) */
  )
  private val cubicRotationOut = Array(
    9f,
    9f,
    9f,
    9f, /* k0 in (unused junk) */ 0f,
    0f,
    0f,
    1f, /* k0 value: identity */ 0.1f,
    0f,
    0f,
    0f, /* k0 out */
    0f,
    0.2f,
    0f,
    0f, /* k1 in */ 0f,
    0f,
    z71,
    z71, /* k1 value: 90 deg about Z */ 8f,
    8f,
    8f,
    8f /* k1 out (unused junk) */
  )
  private val cubicScaleOut = Array(
    0.5f, 0.5f, 0.5f, /* k0 in (unused junk) */ 1f, 1f, 1f, /* k0 value */ 2f, 0f, 0f, /* k0 out */
    0f, 4f, 0f, /* k1 in */ 3f, 3f, 3f, /* k1 value */ 6f, 6f, 6f /* k1 out (unused junk) */
  )

  private def cubicJson: String =
    fixtureJson("cubicAnim", "CUBICSPLINE", cubicTimes, cubicTranslationOut, cubicRotationOut, cubicScaleOut)

  /** LINEAR output accessors: plain per-keyframe values (no tangent triplets). */
  private def linearJson: String =
    fixtureJson(
      "linearAnim",
      "LINEAR",
      Array(0f, 1f),
      Array(0f, 0f, 0f, 10f, 20f, 30f),
      Array(0f, 0f, 0f, 1f, 0f, 0f, z71, z71),
      Array(1f, 1f, 1f, 3f, 3f, 3f)
    )

  // -- Harness: the narrowest real load + evaluation path -------------------

  /** Mirrors GLTFLoaderBase.scala:109 wiring (`animationLoader.load(model.animations, nodeResolver, dataResolver)`) without meshes/textures/GL. */
  private def loadFixture(json: String): (AnimationLoader, Node) = {
    val model        = GLTFJsonParser.parse(json)
    val dataResolver = new DataResolver(model, new EmbeddedDataFileResolver(model))
    val nodeResolver = new NodeResolver()
    val node         = new Node()
    node.id = "animated"
    nodeResolver.put(0, node)
    val loader = new AnimationLoader()
    loader.load(model.animations, nodeResolver, dataResolver)
    (loader, node)
  }

  /** Applies the animation directly to the node (AnimationControllerHack.scala:70-74 / applyAnimationPlus direct mode) and returns its evaluated localTransform. */
  private def evaluatedTransform(animation: Animation, node: Node, time: Float): Matrix4 = {
    AnimationControllerHack.applyAnimationPlus(null, null, 1f, animation, time) // null out/pool = direct apply, as applyAnimation does (AnimationControllerHack.scala:72)
    node.localTransform
  }

  private def assertMatrixEquals(actual: Matrix4, expected: Matrix4, clue: String): Unit = {
    var i = 0
    while (i < 16) {
      assertEqualsFloat(
        actual.values(i),
        expected.values(i),
        0.0001f,
        s"$clue (matrix element $i): expected ${expected.values.mkString(",")} got ${actual.values.mkString(",")}"
      )
      i += 1
    }
  }

  // -- (1) Loading a CUBICSPLINE glTF must not throw -------------------------

  test(
    "ISS-509: CUBICSPLINE glTF animation loads (today: ClassCastException at AnimationLoader.scala:108/139/170 force-casts)"
  ) {
    val (loader, _) = loadFixture(cubicJson)
    assertEquals(loader.animations.size, 1)
    val anim = loader.animations(0)
    // duration from the input accessor max (AnimationLoader.java:76-77)
    assertEqualsFloat(anim.duration, 1f, 0f)
    assertEquals(anim.id, "cubicAnim")
    assertEquals(anim.nodeAnimations.size, 1)
    val na = anim.nodeAnimations(0)
    // one keyframe per input time; input(0) == 0 so no extra first-frame copy (AnimationLoader.java:86-92)
    assertEquals(na.translation.get.size, 2)
    assertEquals(na.rotation.get.size, 2)
    assertEquals(na.scaling.get.size, 2)
    assertEqualsFloat(na.translation.get(0).keytime, 0f, 0f)
    assertEqualsFloat(na.translation.get(1).keytime, 1f, 0f)
  }

  // -- (2) Cubic spline evaluation must match the gdx-gltf Hermite math ------

  test("ISS-509: cubic spline evaluates per gdx-gltf Hermite math at t=0.5 (AnimationControllerHack.java:215-220,231-245)") {
    val (loader, node) = loadFixture(cubicJson)
    val actual         = evaluatedTransform(loader.animations(0), node, 0.5f)

    // Hermite basis at t = 0.5 (keytimes 0..1 so t == time):
    //   h00 = 2t^3-3t^2+1 = 0.5, h10 = t^3-2t^2+t = 0.125,
    //   h01 = -2t^3+3t^2 = 0.5,  h11 = t^3-t^2    = -0.125
    // translation = h00*v0 + h10*out0 + h01*v1 + h11*in1                 (AnimationControllerHack.java:205,215-220)
    //             = 0.125*(1,2,3) + 0.5*(10,20,30) - 0.125*(4,5,6) = (4.625, 9.625, 14.625)   [exact in Float]
    val expectedTranslation = Vector3(4.625f, 9.625f, 14.625f)
    // scale       = 0.5*(1,1,1) + 0.125*(2,0,0) + 0.5*(3,3,3) - 0.125*(0,4,0) = (2.25, 1.5, 2.0)
    val expectedScale = Vector3(2.25f, 1.5f, 2f)
    // rotation: tangents are additionally multiplied by delta = -(t1-t0) = -1 (AnimationControllerHack.java:234,239-244), sum normalized:
    val d                = -1f
    val ux               = 0.125f * d * 0.1f // h10 * d * out0.x
    val uy               = -0.125f * d * 0.2f // h11 * d * in1.y
    val uz               = 0.5f * z71 // h01 * v1.z
    val uw               = 0.5f * 1f + 0.5f * z71 // h00 * v0.w + h01 * v1.w
    val len              = scala.math.sqrt((ux * ux + uy * uy + uz * uz + uw * uw).toDouble).toFloat
    val expectedRotation = Quaternion(ux / len, uy / len, uz / len, uw / len)

    val expected = Matrix4().set(expectedTranslation, expectedRotation, expectedScale)
    assertMatrixEquals(actual, expected, "cubic spline TRS at t=0.5")
  }

  test("ISS-509: cubic spline keyframe anchor is the middle vector of each in/value/out triplet (t=0 evaluates to v0)") {
    val (loader, node) = loadFixture(cubicJson)
    val actual         = evaluatedTransform(loader.animations(0), node, 0f)
    // Hermite basis at t=0 is (1,0,0,0), so the transform is exactly the first keyframe's ANCHOR values
    // (translation (0,0,0), rotation identity, scale (1,1,1)) — junk tangents around them must not leak in.
    val expected = Matrix4().set(Vector3(0f, 0f, 0f), Quaternion(0f, 0f, 0f, 1f), Vector3(1f, 1f, 1f))
    assertMatrixEquals(actual, expected, "cubic spline TRS at t=0 (anchor extraction)")
  }

  // -- (3) GREEN control: identical harness, LINEAR interpolation ------------

  test("ISS-509 control: LINEAR animation from the same harness loads and evaluates (no cubic cast involved)") {
    val (loader, node) = loadFixture(linearJson)
    assertEquals(loader.animations.size, 1)
    val anim = loader.animations(0)
    assertEquals(anim.id, "linearAnim")
    assertEqualsFloat(anim.duration, 1f, 0f)
    val na = anim.nodeAnimations(0)
    assertEquals(na.translation.get.size, 2)
    assertEquals(na.rotation.get.size, 2)
    assertEquals(na.scaling.get.size, 2)
    // loader-level value check (LINEAR keyframe values are plain Vector3)
    assertEqualsFloat(na.translation.get(1).value.x, 10f, 0f)
    assertEqualsFloat(na.translation.get(1).value.y, 20f, 0f)
    assertEqualsFloat(na.translation.get(1).value.z, 30f, 0f)

    val actual = evaluatedTransform(anim, node, 0.5f)
    // lerp midpoints; rotation slerp midpoint of identity -> 90 deg about Z is 45 deg about Z
    val expected = Matrix4().set(
      Vector3(5f, 10f, 15f),
      Quaternion(0f, 0f, 0.38268343f, 0.92387953f),
      Vector3(2f, 2f, 2f)
    )
    assertMatrixEquals(actual, expected, "LINEAR TRS at t=0.5")
  }
}
