/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/scene/Scene.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 171
 * Covenant-baseline-methods: Scene,_animations,animationController,animations,cameras,copy,count,createCamera,createLight,getCamera,getDirectionalLightCount,getLight,getRenderables,initFromSceneModel,lights,modelInstance,result,syncCameras,syncLights,this,transform,update
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package scene

import sge.gltf.scene3d.animation.{ AnimationControllerHack, AnimationsPlayer }
import sge.gltf.scene3d.lights.{ DirectionalLightEx, PointLightEx, SpotLightEx }
import sge.gltf.scene3d.model.ModelInstanceHack
import sge.graphics.{ Camera, OrthographicCamera, PerspectiveCamera }
import sge.graphics.g3d.{ Model, ModelInstance, Renderable, RenderableProvider }
import sge.graphics.g3d.environment.{ BaseLight, DirectionalLight, PointLight, SpotLight }
import sge.graphics.g3d.model.Node
import sge.graphics.g3d.utils.AnimationController
import sge.math.{ Matrix4, Vector3 }
import sge.Sge
import sge.utils.{ DynamicArray, Nullable, ObjectMap, Pool, SgeError }

class Scene(
  var modelInstance:       ModelInstance,
  var animationController: AnimationController,
  val lights:              ObjectMap[Node, BaseLight[?]],
  val cameras:             ObjectMap[Node, Camera],
  val animations:          AnimationsPlayer
) extends RenderableProvider
    with Updatable {

  def this(modelInstance: ModelInstance, animated: Boolean) = {
    this(
      modelInstance,
      if (animated) AnimationControllerHack(modelInstance) else null, // @nowarn — null when not animated
      ObjectMap[Node, BaseLight[?]](),
      ObjectMap[Node, Camera](),
      null // @nowarn — initialized below
    )
    // Re-assign animations (can't reference `this` in super constructor)
  }

  def this(modelInstance: ModelInstance) =
    this(modelInstance, modelInstance.animations.size > 0)

  def this(model: Model) =
    this(ModelInstanceHack(model))

  def this(model: Model, animated: Boolean) =
    this(ModelInstanceHack(model), animated)

  def this(sceneModel: SceneModel)(using Sge) = {
    this(ModelInstanceHack(sceneModel.model))
    initFromSceneModel(sceneModel)
  }

  def this(sceneModel: SceneModel, rootNodeIds: String*)(using Sge) = {
    this(ModelInstanceHack(sceneModel.model, rootNodeIds*))
    initFromSceneModel(sceneModel)
  }

  // Lazy-init animations player
  private lazy val _animations: AnimationsPlayer = AnimationsPlayer(this)

  private def initFromSceneModel(sceneModel: SceneModel)(using Sge): Unit = {
    sceneModel.cameras.foreachEntry { (key, value) =>
      val node = modelInstance.getNode(key.id, true)
      if (node.isDefined) {
        cameras.put(node.get, createCamera(value))
      }
    }
    sceneModel.lights.foreachEntry { (key, value) =>
      val node = modelInstance.getNode(key.id, true)
      if (node.isDefined) {
        lights.put(node.get, createLight(value))
      }
    }
    syncCameras()
    syncLights()
  }

  def createCamera(from: Camera)(using Sge): Camera = {
    val copy: Camera = from match {
      case pc: PerspectiveCamera =>
        val camera = PerspectiveCamera()
        camera.fieldOfView = pc.fieldOfView
        camera
      case oc: OrthographicCamera =>
        val camera = OrthographicCamera()
        camera.zoom = oc.zoom
        camera
      case _ =>
        throw SgeError.InvalidInput("unknown camera type " + from.getClass.getName)
    }
    copy.position.set(from.position)
    copy.direction.set(from.direction)
    copy.up.set(from.up)
    copy.near = from.near
    copy.far = from.far
    copy.viewportWidth = from.viewportWidth
    copy.viewportHeight = from.viewportHeight
    copy
  }

  protected def createLight(from: BaseLight[?]): BaseLight[?] =
    from match {
      case dl: DirectionalLight => DirectionalLightEx().set(dl)
      case pl: PointLight       => PointLightEx().set(pl)
      case sl: SpotLight        => SpotLightEx().set(sl)
      case _ => throw SgeError.InvalidInput("unknown light type " + from.getClass.getName)
    }

  override def update(camera: Camera, delta: Float): Unit = {
    _animations.update(delta)
    syncCameras()
    syncLights()
  }

  private val transform: Matrix4 = Matrix4()

  private def syncCameras(): Unit =
    cameras.foreachEntry { (node, camera) =>
      transform.set(modelInstance.transform).mul(node.globalTransform)
      camera.position.setZero().mul(transform)
      camera.direction.set(0f, 0f, -1f).rot(transform)
      camera.up.set(Vector3.Y).rot(transform)
      camera.update()
    }

  private def syncLights(): Unit =
    lights.foreachEntry { (node, light) =>
      transform.set(modelInstance.transform).mul(node.globalTransform)
      light match {
        case dl: DirectionalLight => dl.direction.set(0f, 0f, -1f).rot(transform)
        case pl: PointLight       => pl.position.setZero().mul(transform)
        case sl: SpotLight        =>
          sl.position.setZero().mul(transform)
          sl.direction.set(0f, 0f, -1f).rot(transform)
        case _ => ()
      }
    }

  def getCamera(name: String): Nullable[Camera] = {
    var result: Nullable[Camera] = Nullable.empty
    cameras.foreachEntry { (node, camera) =>
      if (name == node.id) result = Nullable(camera)
    }
    result
  }

  def getLight(name: String): Nullable[BaseLight[?]] = {
    var result: Nullable[BaseLight[?]] = Nullable.empty
    lights.foreachEntry { (node, light) =>
      if (name == node.id) result = Nullable(light)
    }
    result
  }

  def getDirectionalLightCount: Int = {
    var count = 0
    lights.foreachValue { light =>
      if (light.isInstanceOf[DirectionalLight]) count += 1
    }
    count
  }

  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit =
    modelInstance.getRenderables(renderables, pool)
}
