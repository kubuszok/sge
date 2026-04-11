/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package exporters

import scala.collection.mutable.ArrayBuffer

import sge.graphics.{ Camera, OrthographicCamera, PerspectiveCamera }
import sge.graphics.g3d.model.Node
import sge.gltf.data.camera.{ GLTFCamera, GLTFOrthographic, GLTFPerspective }
import sge.gltf.data.scene.GLTFNode
import sge.gltf.loaders.exceptions.{ GLTFRuntimeException, GLTFUnsupportedException }
import sge.math.MathUtils
import sge.utils.{ Nullable, ObjectMap }

private[exporters] class GLTFCameraExporter(private val base: GLTFExporter) {

  def exportCameras(cameras: ObjectMap[Node, Camera]): Unit = {
    cameras.foreachEntry { (key, value) =>
      val nodeID = base.nodeMapping.indexOfByRef(key)
      if (nodeID < 0) throw new GLTFRuntimeException("node not found")
      val glNode = base.root.nodes.get(nodeID)
      if (base.root.cameras.isEmpty) {
        base.root.cameras = Nullable(ArrayBuffer[GLTFCamera]())
      }
      glNode.camera = Nullable(base.root.cameras.get.size)
      base.root.cameras.get += exportCamera(value)
    }
  }

  private def exportCamera(camera: Camera): GLTFCamera = {
    val glCamera = new GLTFCamera()
    camera match {
      case pcam: PerspectiveCamera =>
        glCamera.`type` = Nullable("perspective")
        glCamera.perspective = Nullable(new GLTFPerspective())
        glCamera.perspective.get.yfov = pcam.fieldOfView * MathUtils.degreesToRadians // TODO not sure
        glCamera.perspective.get.znear = camera.near
        glCamera.perspective.get.zfar = Nullable(camera.far)
        glCamera.perspective.get.aspectRatio = Nullable(camera.viewportWidth / camera.viewportHeight) // TODO not sure
        // TODO aspect ratio and fov should be recomputed...

      case ocam: OrthographicCamera =>
        glCamera.`type` = Nullable("orthographic")
        glCamera.orthographic = Nullable(new GLTFOrthographic())
        glCamera.orthographic.get.znear = Nullable(camera.near)
        glCamera.orthographic.get.zfar = Nullable(camera.far)
        glCamera.orthographic.get.xmag = Nullable((camera.viewportWidth * ocam.zoom).toFloat) // TODO not sure
        glCamera.orthographic.get.ymag = Nullable((camera.viewportHeight * ocam.zoom).toFloat) // TODO not sure

      case _ =>
        throw new GLTFUnsupportedException("unsupported camera type " + camera.getClass)
    }

    glCamera
  }
}
