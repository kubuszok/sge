/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * JSON serialization for GLTF export. Replaces the LibGDX Json.prettyPrint(root)
 * with a purpose-built writer that serializes the GLTF data model to spec-compliant
 * JSON output with pretty-printing and prototype-aware field omission.
 */
package sge
package gltf
package exporters

import scala.collection.mutable.ArrayBuffer

import sge.gltf.data.{ GLTF, GLTFAsset, GLTFExtensions, GLTFObject }
import sge.gltf.data.animation.{ GLTFAnimation, GLTFAnimationChannel, GLTFAnimationSampler, GLTFAnimationTarget }
import sge.gltf.data.camera.{ GLTFCamera, GLTFOrthographic, GLTFPerspective }
import sge.gltf.data.data.{ GLTFAccessor, GLTFBuffer, GLTFBufferView }
import sge.gltf.data.extensions._
import sge.gltf.data.extensions.KHRLightsPunctual.{ GLTFLight, GLTFLightNode, GLTFLights, GLTFSpotLight }
import sge.gltf.data.geometry.{ GLTFMesh, GLTFMorphTarget, GLTFPrimitive }
import sge.gltf.data.material.{ GLTFMaterial, GLTFpbrMetallicRoughness }
import sge.gltf.data.scene.{ GLTFNode, GLTFScene, GLTFSkin }
import sge.gltf.data.texture.{ GLTFImage, GLTFNormalTextureInfo, GLTFOcclusionTextureInfo, GLTFSampler, GLTFTexture, GLTFTextureInfo }
import sge.utils.Nullable

/** Minimal JSON writer for GLTF export. Produces pretty-printed, spec-compliant JSON
  * matching the behavior of LibGDX's Json.prettyPrint with setUsePrototypes(true).
  */
private[exporters] object GLTFExporterJson {

  def writeGltf(root: GLTF): String = {
    val sb = new StringBuilder()
    writeGltfObj(sb, root, 0)
    sb.toString
  }

  // ── Primitives ──────────────────────────────────────────────────────

  private def indent(sb: StringBuilder, level: Int): Unit = {
    var i = 0
    while (i < level) {
      sb.append('\t')
      i += 1
    }
  }

  private def writeString(sb: StringBuilder, s: String): Unit = {
    sb.append('"')
    var i = 0
    while (i < s.length) {
      val c = s.charAt(i)
      c match {
        case '"'  => sb.append("\\\"")
        case '\\' => sb.append("\\\\")
        case '\n' => sb.append("\\n")
        case '\r' => sb.append("\\r")
        case '\t' => sb.append("\\t")
        case _ =>
          if (c < ' ') sb.append("\\u%04x".format(c.toInt))
          else sb.append(c)
      }
      i += 1
    }
    sb.append('"')
  }

  private def writeFloat(sb: StringBuilder, v: Float): Unit = {
    if (v == v.toLong.toFloat && !v.isInfinite) sb.append(v.toLong)
    else sb.append(v)
  }

  private def writeFloatArray(sb: StringBuilder, arr: Array[Float]): Unit = {
    sb.append('[')
    var i = 0
    while (i < arr.length) {
      if (i > 0) sb.append(", ")
      writeFloat(sb, arr(i))
      i += 1
    }
    sb.append(']')
  }

  private def writeIntArray(sb: StringBuilder, arr: ArrayBuffer[Int], lvl: Int): Unit = {
    sb.append('[')
    var i = 0
    while (i < arr.size) {
      if (i > 0) sb.append(", ")
      sb.append(arr(i))
      i += 1
    }
    sb.append(']')
  }

  private def writeStringArray(sb: StringBuilder, arr: ArrayBuffer[String]): Unit = {
    sb.append('[')
    var i = 0
    while (i < arr.size) {
      if (i > 0) sb.append(", ")
      writeString(sb, arr(i))
      i += 1
    }
    sb.append(']')
  }

  // ── Key-value helpers ────────────────────────────────────────────────

  private def key(sb: StringBuilder, name: String, lvl: Int): Unit = {
    indent(sb, lvl)
    writeString(sb, name)
    sb.append(": ")
  }

  private def comma(sb: StringBuilder, first: Boolean): Boolean = {
    if (!first) sb.append(",\n")
    false
  }

  // ── Object writers ──────────────────────────────────────────────────

  private def writeGltfObj(sb: StringBuilder, g: GLTF, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    g.asset.foreach { a =>
      first = comma(sb, first); key(sb, "asset", lvl + 1); writeAsset(sb, a, lvl + 1)
    }
    if (g.scene != 0 || g.scenes.exists(_.nonEmpty)) {
      first = comma(sb, first); key(sb, "scene", lvl + 1); sb.append(g.scene)
    }
    g.scenes.foreach { s =>
      first = comma(sb, first); key(sb, "scenes", lvl + 1); writeArray(sb, s, lvl + 1)(writeScene)
    }
    g.nodes.foreach { n =>
      first = comma(sb, first); key(sb, "nodes", lvl + 1); writeArray(sb, n, lvl + 1)(writeNode)
    }
    g.cameras.foreach { c =>
      first = comma(sb, first); key(sb, "cameras", lvl + 1); writeArray(sb, c, lvl + 1)(writeCamera)
    }
    g.meshes.foreach { m =>
      first = comma(sb, first); key(sb, "meshes", lvl + 1); writeArray(sb, m, lvl + 1)(writeMesh)
    }
    g.images.foreach { i =>
      first = comma(sb, first); key(sb, "images", lvl + 1); writeArray(sb, i, lvl + 1)(writeImage)
    }
    g.samplers.foreach { s =>
      first = comma(sb, first); key(sb, "samplers", lvl + 1); writeArray(sb, s, lvl + 1)(writeSampler)
    }
    g.textures.foreach { t =>
      first = comma(sb, first); key(sb, "textures", lvl + 1); writeArray(sb, t, lvl + 1)(writeTexture)
    }
    g.animations.foreach { a =>
      first = comma(sb, first); key(sb, "animations", lvl + 1); writeArray(sb, a, lvl + 1)(writeAnimation)
    }
    g.skins.foreach { s =>
      first = comma(sb, first); key(sb, "skins", lvl + 1); writeArray(sb, s, lvl + 1)(writeSkin)
    }
    g.accessors.foreach { a =>
      first = comma(sb, first); key(sb, "accessors", lvl + 1); writeArray(sb, a, lvl + 1)(writeAccessor)
    }
    g.materials.foreach { m =>
      first = comma(sb, first); key(sb, "materials", lvl + 1); writeArray(sb, m, lvl + 1)(writeMaterial)
    }
    g.bufferViews.foreach { bv =>
      first = comma(sb, first); key(sb, "bufferViews", lvl + 1); writeArray(sb, bv, lvl + 1)(writeBufferView)
    }
    g.buffers.foreach { b =>
      first = comma(sb, first); key(sb, "buffers", lvl + 1); writeArray(sb, b, lvl + 1)(writeBuffer)
    }
    g.extensionsUsed.foreach { eu =>
      first = comma(sb, first); key(sb, "extensionsUsed", lvl + 1); writeStringArray(sb, eu)
    }
    g.extensionsRequired.foreach { er =>
      first = comma(sb, first); key(sb, "extensionsRequired", lvl + 1); writeStringArray(sb, er)
    }
    g.extensions.foreach { ext =>
      first = comma(sb, first); key(sb, "extensions", lvl + 1); writeExtensions(sb, ext, lvl + 1)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeArray[T](sb: StringBuilder, arr: ArrayBuffer[T], lvl: Int)(w: (StringBuilder, T, Int) => Unit): Unit = {
    sb.append("[\n")
    var i = 0
    while (i < arr.size) {
      if (i > 0) sb.append(",\n")
      indent(sb, lvl + 1)
      w(sb, arr(i), lvl + 1)
      i += 1
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append(']')
  }

  private def writeAsset(sb: StringBuilder, a: GLTFAsset, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    a.version.foreach { v =>
      first = comma(sb, first); key(sb, "version", lvl + 1); writeString(sb, v)
    }
    a.generator.foreach { g =>
      first = comma(sb, first); key(sb, "generator", lvl + 1); writeString(sb, g)
    }
    a.copyright.foreach { c =>
      first = comma(sb, first); key(sb, "copyright", lvl + 1); writeString(sb, c)
    }
    a.minVersion.foreach { v =>
      first = comma(sb, first); key(sb, "minVersion", lvl + 1); writeString(sb, v)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeScene(sb: StringBuilder, s: GLTFScene, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    s.name.foreach { n =>
      first = comma(sb, first); key(sb, "name", lvl + 1); writeString(sb, n)
    }
    s.nodes.foreach { n =>
      first = comma(sb, first); key(sb, "nodes", lvl + 1); writeIntArray(sb, n, lvl + 1)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeNode(sb: StringBuilder, n: GLTFNode, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    n.name.foreach { nm =>
      first = comma(sb, first); key(sb, "name", lvl + 1); writeString(sb, nm)
    }
    n.translation.foreach { t =>
      first = comma(sb, first); key(sb, "translation", lvl + 1); writeFloatArray(sb, t)
    }
    n.rotation.foreach { r =>
      first = comma(sb, first); key(sb, "rotation", lvl + 1); writeFloatArray(sb, r)
    }
    n.scale.foreach { s =>
      first = comma(sb, first); key(sb, "scale", lvl + 1); writeFloatArray(sb, s)
    }
    n.matrix.foreach { m =>
      first = comma(sb, first); key(sb, "matrix", lvl + 1); writeFloatArray(sb, m)
    }
    n.mesh.foreach { m =>
      first = comma(sb, first); key(sb, "mesh", lvl + 1); sb.append(m)
    }
    n.camera.foreach { c =>
      first = comma(sb, first); key(sb, "camera", lvl + 1); sb.append(c)
    }
    n.skin.foreach { s =>
      first = comma(sb, first); key(sb, "skin", lvl + 1); sb.append(s)
    }
    n.children.foreach { c =>
      first = comma(sb, first); key(sb, "children", lvl + 1); writeIntArray(sb, c, lvl + 1)
    }
    n.weights.foreach { w =>
      first = comma(sb, first); key(sb, "weights", lvl + 1); writeFloatArray(sb, w)
    }
    n.extensions.foreach { ext =>
      first = comma(sb, first); key(sb, "extensions", lvl + 1); writeExtensions(sb, ext, lvl + 1)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeCamera(sb: StringBuilder, c: GLTFCamera, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    c.name.foreach { n =>
      first = comma(sb, first); key(sb, "name", lvl + 1); writeString(sb, n)
    }
    c.`type`.foreach { t =>
      first = comma(sb, first); key(sb, "type", lvl + 1); writeString(sb, t)
    }
    c.perspective.foreach { p =>
      first = comma(sb, first); key(sb, "perspective", lvl + 1); writePerspective(sb, p, lvl + 1)
    }
    c.orthographic.foreach { o =>
      first = comma(sb, first); key(sb, "orthographic", lvl + 1); writeOrthographic(sb, o, lvl + 1)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writePerspective(sb: StringBuilder, p: GLTFPerspective, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    first = comma(sb, first); key(sb, "yfov", lvl + 1); writeFloat(sb, p.yfov)
    first = comma(sb, first); key(sb, "znear", lvl + 1); writeFloat(sb, p.znear)
    p.zfar.foreach { z =>
      first = comma(sb, first); key(sb, "zfar", lvl + 1); writeFloat(sb, z)
    }
    p.aspectRatio.foreach { ar =>
      first = comma(sb, first); key(sb, "aspectRatio", lvl + 1); writeFloat(sb, ar)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeOrthographic(sb: StringBuilder, o: GLTFOrthographic, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    o.znear.foreach { z =>
      first = comma(sb, first); key(sb, "znear", lvl + 1); writeFloat(sb, z)
    }
    o.zfar.foreach { z =>
      first = comma(sb, first); key(sb, "zfar", lvl + 1); writeFloat(sb, z)
    }
    o.xmag.foreach { x =>
      first = comma(sb, first); key(sb, "xmag", lvl + 1); writeFloat(sb, x)
    }
    o.ymag.foreach { y =>
      first = comma(sb, first); key(sb, "ymag", lvl + 1); writeFloat(sb, y)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeMesh(sb: StringBuilder, m: GLTFMesh, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    m.name.foreach { n =>
      first = comma(sb, first); key(sb, "name", lvl + 1); writeString(sb, n)
    }
    m.primitives.foreach { p =>
      first = comma(sb, first); key(sb, "primitives", lvl + 1); writeArray(sb, p, lvl + 1)(writePrimitive)
    }
    m.weights.foreach { w =>
      first = comma(sb, first); key(sb, "weights", lvl + 1); writeFloatArray(sb, w)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writePrimitive(sb: StringBuilder, p: GLTFPrimitive, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    p.attributes.foreach { attrs =>
      first = comma(sb, first); key(sb, "attributes", lvl + 1)
      sb.append("{\n")
      var attrFirst = true
      for ((k, v) <- attrs) {
        attrFirst = comma(sb, attrFirst); key(sb, k, lvl + 2); sb.append(v)
      }
      sb.append('\n')
      indent(sb, lvl + 1)
      sb.append('}')
    }
    p.indices.foreach { idx =>
      first = comma(sb, first); key(sb, "indices", lvl + 1); sb.append(idx)
    }
    p.mode.foreach { m =>
      first = comma(sb, first); key(sb, "mode", lvl + 1); sb.append(m)
    }
    p.material.foreach { m =>
      first = comma(sb, first); key(sb, "material", lvl + 1); sb.append(m)
    }
    p.targets.foreach { t =>
      first = comma(sb, first); key(sb, "targets", lvl + 1); writeArray(sb, t, lvl + 1)(writeMorphTarget)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeMorphTarget(sb: StringBuilder, t: GLTFMorphTarget, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    for ((k, v) <- t) {
      first = comma(sb, first); key(sb, k, lvl + 1); sb.append(v)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeImage(sb: StringBuilder, img: GLTFImage, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    img.name.foreach { n =>
      first = comma(sb, first); key(sb, "name", lvl + 1); writeString(sb, n)
    }
    img.uri.foreach { u =>
      first = comma(sb, first); key(sb, "uri", lvl + 1); writeString(sb, u)
    }
    img.mimeType.foreach { m =>
      first = comma(sb, first); key(sb, "mimeType", lvl + 1); writeString(sb, m)
    }
    img.bufferView.foreach { bv =>
      first = comma(sb, first); key(sb, "bufferView", lvl + 1); sb.append(bv)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeSampler(sb: StringBuilder, s: GLTFSampler, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    s.minFilter.foreach { mf =>
      first = comma(sb, first); key(sb, "minFilter", lvl + 1); sb.append(mf)
    }
    s.magFilter.foreach { mf =>
      first = comma(sb, first); key(sb, "magFilter", lvl + 1); sb.append(mf)
    }
    s.wrapS.foreach { ws =>
      first = comma(sb, first); key(sb, "wrapS", lvl + 1); sb.append(ws)
    }
    s.wrapT.foreach { wt =>
      first = comma(sb, first); key(sb, "wrapT", lvl + 1); sb.append(wt)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeTexture(sb: StringBuilder, t: GLTFTexture, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    t.source.foreach { s =>
      first = comma(sb, first); key(sb, "source", lvl + 1); sb.append(s)
    }
    t.sampler.foreach { s =>
      first = comma(sb, first); key(sb, "sampler", lvl + 1); sb.append(s)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeAnimation(sb: StringBuilder, a: GLTFAnimation, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    a.name.foreach { n =>
      first = comma(sb, first); key(sb, "name", lvl + 1); writeString(sb, n)
    }
    a.channels.foreach { c =>
      first = comma(sb, first); key(sb, "channels", lvl + 1); writeArray(sb, c, lvl + 1)(writeAnimationChannel)
    }
    a.samplers.foreach { s =>
      first = comma(sb, first); key(sb, "samplers", lvl + 1); writeArray(sb, s, lvl + 1)(writeAnimationSampler)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeAnimationChannel(sb: StringBuilder, c: GLTFAnimationChannel, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    c.sampler.foreach { s =>
      first = comma(sb, first); key(sb, "sampler", lvl + 1); sb.append(s)
    }
    c.target.foreach { t =>
      first = comma(sb, first); key(sb, "target", lvl + 1); writeAnimationTarget(sb, t, lvl + 1)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeAnimationTarget(sb: StringBuilder, t: GLTFAnimationTarget, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    t.node.foreach { n =>
      first = comma(sb, first); key(sb, "node", lvl + 1); sb.append(n)
    }
    t.path.foreach { p =>
      first = comma(sb, first); key(sb, "path", lvl + 1); writeString(sb, p)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeAnimationSampler(sb: StringBuilder, s: GLTFAnimationSampler, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    s.input.foreach { i =>
      first = comma(sb, first); key(sb, "input", lvl + 1); sb.append(i)
    }
    s.output.foreach { o =>
      first = comma(sb, first); key(sb, "output", lvl + 1); sb.append(o)
    }
    s.interpolation.foreach { i =>
      first = comma(sb, first); key(sb, "interpolation", lvl + 1); writeString(sb, i)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeSkin(sb: StringBuilder, s: GLTFSkin, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    s.name.foreach { n =>
      first = comma(sb, first); key(sb, "name", lvl + 1); writeString(sb, n)
    }
    s.joints.foreach { j =>
      first = comma(sb, first); key(sb, "joints", lvl + 1); writeIntArray(sb, j, lvl + 1)
    }
    s.inverseBindMatrices.foreach { ibm =>
      first = comma(sb, first); key(sb, "inverseBindMatrices", lvl + 1); sb.append(ibm)
    }
    s.skeleton.foreach { sk =>
      first = comma(sb, first); key(sb, "skeleton", lvl + 1); sb.append(sk)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeAccessor(sb: StringBuilder, a: GLTFAccessor, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    a.bufferView.foreach { bv =>
      first = comma(sb, first); key(sb, "bufferView", lvl + 1); sb.append(bv)
    }
    if (a.byteOffset != 0) {
      first = comma(sb, first); key(sb, "byteOffset", lvl + 1); sb.append(a.byteOffset)
    }
    first = comma(sb, first); key(sb, "componentType", lvl + 1); sb.append(a.componentType)
    first = comma(sb, first); key(sb, "count", lvl + 1); sb.append(a.count)
    a.`type`.foreach { t =>
      first = comma(sb, first); key(sb, "type", lvl + 1); writeString(sb, t)
    }
    if (a.normalized) {
      first = comma(sb, first); key(sb, "normalized", lvl + 1); sb.append("true")
    }
    a.min.foreach { m =>
      first = comma(sb, first); key(sb, "min", lvl + 1); writeFloatArray(sb, m)
    }
    a.max.foreach { m =>
      first = comma(sb, first); key(sb, "max", lvl + 1); writeFloatArray(sb, m)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeMaterial(sb: StringBuilder, m: GLTFMaterial, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    m.name.foreach { n =>
      first = comma(sb, first); key(sb, "name", lvl + 1); writeString(sb, n)
    }
    m.pbrMetallicRoughness.foreach { pbr =>
      first = comma(sb, first); key(sb, "pbrMetallicRoughness", lvl + 1); writePbr(sb, pbr, lvl + 1)
    }
    m.normalTexture.foreach { nt =>
      first = comma(sb, first); key(sb, "normalTexture", lvl + 1); writeNormalTexInfo(sb, nt, lvl + 1)
    }
    m.occlusionTexture.foreach { ot =>
      first = comma(sb, first); key(sb, "occlusionTexture", lvl + 1); writeOcclusionTexInfo(sb, ot, lvl + 1)
    }
    m.emissiveTexture.foreach { et =>
      first = comma(sb, first); key(sb, "emissiveTexture", lvl + 1); writeTexInfo(sb, et, lvl + 1)
    }
    m.emissiveFactor.foreach { ef =>
      first = comma(sb, first); key(sb, "emissiveFactor", lvl + 1); writeFloatArray(sb, ef)
    }
    m.alphaMode.foreach { am =>
      first = comma(sb, first); key(sb, "alphaMode", lvl + 1); writeString(sb, am)
    }
    m.alphaCutoff.foreach { ac =>
      first = comma(sb, first); key(sb, "alphaCutoff", lvl + 1); writeFloat(sb, ac)
    }
    m.doubleSided.foreach { ds =>
      first = comma(sb, first); key(sb, "doubleSided", lvl + 1); sb.append(ds)
    }
    m.extensions.foreach { ext =>
      first = comma(sb, first); key(sb, "extensions", lvl + 1); writeExtensions(sb, ext, lvl + 1)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writePbr(sb: StringBuilder, p: GLTFpbrMetallicRoughness, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    p.baseColorFactor.foreach { bcf =>
      first = comma(sb, first); key(sb, "baseColorFactor", lvl + 1); writeFloatArray(sb, bcf)
    }
    if (p.metallicFactor != 1f) {
      first = comma(sb, first); key(sb, "metallicFactor", lvl + 1); writeFloat(sb, p.metallicFactor)
    }
    if (p.roughnessFactor != 1f) {
      first = comma(sb, first); key(sb, "roughnessFactor", lvl + 1); writeFloat(sb, p.roughnessFactor)
    }
    p.baseColorTexture.foreach { bct =>
      first = comma(sb, first); key(sb, "baseColorTexture", lvl + 1); writeTexInfo(sb, bct, lvl + 1)
    }
    p.metallicRoughnessTexture.foreach { mrt =>
      first = comma(sb, first); key(sb, "metallicRoughnessTexture", lvl + 1); writeTexInfo(sb, mrt, lvl + 1)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeTexInfo(sb: StringBuilder, ti: GLTFTextureInfo, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    ti.index.foreach { idx =>
      first = comma(sb, first); key(sb, "index", lvl + 1); sb.append(idx)
    }
    if (ti.texCoord != 0) {
      first = comma(sb, first); key(sb, "texCoord", lvl + 1); sb.append(ti.texCoord)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeNormalTexInfo(sb: StringBuilder, ti: GLTFNormalTextureInfo, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    ti.index.foreach { idx =>
      first = comma(sb, first); key(sb, "index", lvl + 1); sb.append(idx)
    }
    if (ti.texCoord != 0) {
      first = comma(sb, first); key(sb, "texCoord", lvl + 1); sb.append(ti.texCoord)
    }
    if (ti.scale != 1f) {
      first = comma(sb, first); key(sb, "scale", lvl + 1); writeFloat(sb, ti.scale)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeOcclusionTexInfo(sb: StringBuilder, ti: GLTFOcclusionTextureInfo, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    ti.index.foreach { idx =>
      first = comma(sb, first); key(sb, "index", lvl + 1); sb.append(idx)
    }
    if (ti.texCoord != 0) {
      first = comma(sb, first); key(sb, "texCoord", lvl + 1); sb.append(ti.texCoord)
    }
    if (ti.strength != 1f) {
      first = comma(sb, first); key(sb, "strength", lvl + 1); writeFloat(sb, ti.strength)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeBufferView(sb: StringBuilder, bv: GLTFBufferView, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    bv.buffer.foreach { b =>
      first = comma(sb, first); key(sb, "buffer", lvl + 1); sb.append(b)
    }
    if (bv.byteOffset != 0) {
      first = comma(sb, first); key(sb, "byteOffset", lvl + 1); sb.append(bv.byteOffset)
    }
    first = comma(sb, first); key(sb, "byteLength", lvl + 1); sb.append(bv.byteLength)
    bv.byteStride.foreach { bs =>
      first = comma(sb, first); key(sb, "byteStride", lvl + 1); sb.append(bs)
    }
    bv.target.foreach { t =>
      first = comma(sb, first); key(sb, "target", lvl + 1); sb.append(t)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeBuffer(sb: StringBuilder, b: GLTFBuffer, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    b.uri.foreach { u =>
      first = comma(sb, first); key(sb, "uri", lvl + 1); writeString(sb, u)
    }
    first = comma(sb, first); key(sb, "byteLength", lvl + 1); sb.append(b.byteLength)
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  // ── Extensions ────────────────────────────────────────────────────────

  private def writeExtensions(sb: StringBuilder, ext: GLTFExtensions, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    // Check for known extension types and serialize them
    ext.get(classOf[KHRMaterialsUnlit], KHRMaterialsUnlit.EXT).foreach { _ =>
      first = comma(sb, first); key(sb, KHRMaterialsUnlit.EXT, lvl + 1); sb.append("{}")
    }
    ext.get(classOf[KHRMaterialsTransmission], KHRMaterialsTransmission.EXT).foreach { t =>
      first = comma(sb, first); key(sb, KHRMaterialsTransmission.EXT, lvl + 1)
      writeTransmission(sb, t, lvl + 1)
    }
    ext.get(classOf[KHRMaterialsVolume], KHRMaterialsVolume.EXT).foreach { v =>
      first = comma(sb, first); key(sb, KHRMaterialsVolume.EXT, lvl + 1)
      writeVolume(sb, v, lvl + 1)
    }
    ext.get(classOf[KHRMaterialsIOR], KHRMaterialsIOR.EXT).foreach { ior =>
      first = comma(sb, first); key(sb, KHRMaterialsIOR.EXT, lvl + 1)
      sb.append("{\n"); indent(sb, lvl + 2); writeString(sb, "ior"); sb.append(": "); writeFloat(sb, ior.ior); sb.append('\n'); indent(sb, lvl + 1); sb.append('}')
    }
    ext.get(classOf[KHRMaterialsSpecular], KHRMaterialsSpecular.EXT).foreach { s =>
      first = comma(sb, first); key(sb, KHRMaterialsSpecular.EXT, lvl + 1)
      writeSpecular(sb, s, lvl + 1)
    }
    ext.get(classOf[KHRMaterialsIridescence], KHRMaterialsIridescence.EXT).foreach { i =>
      first = comma(sb, first); key(sb, KHRMaterialsIridescence.EXT, lvl + 1)
      writeIridescence(sb, i, lvl + 1)
    }
    ext.get(classOf[KHRMaterialsEmissiveStrength], KHRMaterialsEmissiveStrength.EXT).foreach { e =>
      first = comma(sb, first); key(sb, KHRMaterialsEmissiveStrength.EXT, lvl + 1)
      sb.append("{\n"); indent(sb, lvl + 2); writeString(sb, "emissiveStrength"); sb.append(": "); writeFloat(sb, e.emissiveStrength); sb.append('\n'); indent(sb, lvl + 1); sb.append('}')
    }
    ext.get(classOf[GLTFLights], KHRLightsPunctual.EXT).foreach { lights =>
      first = comma(sb, first); key(sb, KHRLightsPunctual.EXT, lvl + 1)
      writeLights(sb, lights, lvl + 1)
    }
    ext.get(classOf[GLTFLightNode], KHRLightsPunctual.EXT).foreach { ln =>
      first = comma(sb, first); key(sb, KHRLightsPunctual.EXT, lvl + 1)
      sb.append("{\n"); indent(sb, lvl + 2)
      ln.light.foreach { l =>
        writeString(sb, "light"); sb.append(": "); sb.append(l)
      }
      sb.append('\n'); indent(sb, lvl + 1); sb.append('}')
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeTransmission(sb: StringBuilder, t: KHRMaterialsTransmission, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    if (t.transmissionFactor != 0f) {
      first = comma(sb, first); key(sb, "transmissionFactor", lvl + 1); writeFloat(sb, t.transmissionFactor)
    }
    t.transmissionTexture.foreach { tt =>
      first = comma(sb, first); key(sb, "transmissionTexture", lvl + 1); writeTexInfo(sb, tt, lvl + 1)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeVolume(sb: StringBuilder, v: KHRMaterialsVolume, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    if (v.thicknessFactor != 0f) {
      first = comma(sb, first); key(sb, "thicknessFactor", lvl + 1); writeFloat(sb, v.thicknessFactor)
    }
    v.thicknessTexture.foreach { tt =>
      first = comma(sb, first); key(sb, "thicknessTexture", lvl + 1); writeTexInfo(sb, tt, lvl + 1)
    }
    v.attenuationDistance.foreach { ad =>
      first = comma(sb, first); key(sb, "attenuationDistance", lvl + 1); writeFloat(sb, ad)
    }
    first = comma(sb, first); key(sb, "attenuationColor", lvl + 1); writeFloatArray(sb, v.attenuationColor)
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeSpecular(sb: StringBuilder, s: KHRMaterialsSpecular, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    if (s.specularFactor != 1f) {
      first = comma(sb, first); key(sb, "specularFactor", lvl + 1); writeFloat(sb, s.specularFactor)
    }
    s.specularTexture.foreach { st =>
      first = comma(sb, first); key(sb, "specularTexture", lvl + 1); writeTexInfo(sb, st, lvl + 1)
    }
    first = comma(sb, first); key(sb, "specularColorFactor", lvl + 1); writeFloatArray(sb, s.specularColorFactor)
    s.specularColorTexture.foreach { sct =>
      first = comma(sb, first); key(sb, "specularColorTexture", lvl + 1); writeTexInfo(sb, sct, lvl + 1)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeIridescence(sb: StringBuilder, i: KHRMaterialsIridescence, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    if (i.iridescenceFactor != 0f) {
      first = comma(sb, first); key(sb, "iridescenceFactor", lvl + 1); writeFloat(sb, i.iridescenceFactor)
    }
    i.iridescenceTexture.foreach { it =>
      first = comma(sb, first); key(sb, "iridescenceTexture", lvl + 1); writeTexInfo(sb, it, lvl + 1)
    }
    if (i.iridescenceIor != 1.3f) {
      first = comma(sb, first); key(sb, "iridescenceIor", lvl + 1); writeFloat(sb, i.iridescenceIor)
    }
    if (i.iridescenceThicknessMinimum != 100f) {
      first = comma(sb, first); key(sb, "iridescenceThicknessMinimum", lvl + 1); writeFloat(sb, i.iridescenceThicknessMinimum)
    }
    if (i.iridescenceThicknessMaximum != 400f) {
      first = comma(sb, first); key(sb, "iridescenceThicknessMaximum", lvl + 1); writeFloat(sb, i.iridescenceThicknessMaximum)
    }
    i.iridescenceThicknessTexture.foreach { itt =>
      first = comma(sb, first); key(sb, "iridescenceThicknessTexture", lvl + 1); writeTexInfo(sb, itt, lvl + 1)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeLights(sb: StringBuilder, lights: GLTFLights, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    lights.lights.foreach { ls =>
      first = comma(sb, first); key(sb, "lights", lvl + 1); writeArray(sb, ls, lvl + 1)(writeLight)
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }

  private def writeLight(sb: StringBuilder, l: GLTFLight, lvl: Int): Unit = {
    sb.append("{\n")
    var first = true
    if (l.name.nonEmpty) {
      first = comma(sb, first); key(sb, "name", lvl + 1); writeString(sb, l.name)
    }
    l.`type`.foreach { t =>
      first = comma(sb, first); key(sb, "type", lvl + 1); writeString(sb, t)
    }
    first = comma(sb, first); key(sb, "color", lvl + 1); writeFloatArray(sb, l.color)
    if (l.intensity != 1f) {
      first = comma(sb, first); key(sb, "intensity", lvl + 1); writeFloat(sb, l.intensity)
    }
    l.range.foreach { r =>
      first = comma(sb, first); key(sb, "range", lvl + 1); writeFloat(sb, r)
    }
    l.spot.foreach { s =>
      first = comma(sb, first); key(sb, "spot", lvl + 1)
      sb.append("{\n")
      var sf = true
      sf = comma(sb, sf); key(sb, "innerConeAngle", lvl + 2); writeFloat(sb, s.innerConeAngle)
      sf = comma(sb, sf); key(sb, "outerConeAngle", lvl + 2); writeFloat(sb, s.outerConeAngle)
      sb.append('\n')
      indent(sb, lvl + 1)
      sb.append('}')
    }
    sb.append('\n')
    indent(sb, lvl)
    sb.append('}')
  }
}
