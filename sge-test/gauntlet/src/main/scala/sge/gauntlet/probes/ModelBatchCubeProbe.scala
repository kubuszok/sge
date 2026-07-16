/*
 * SGE Gauntlet — g3d: ModelBuilder cube rendered with the DefaultShader.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.{ Color, PerspectiveCamera, VertexAttributes }
import sge.graphics.g3d.{ Material, Model, ModelBatch, ModelInstance }
import sge.graphics.g3d.attributes.ColorAttribute

import scala.collection.mutable.ListBuffer

/** Builds a cube with ModelBuilder, renders it through ModelBatch + DefaultShader (unlit diffuse) with a perspective camera, and asserts the center pixel against the background. */
object ModelBatchCubeProbe extends FeatureProbe {

  override def id: String = "g3d/modelbatch-cube"

  override def area: String = "g3d"

  override def requiresGpu: Boolean = true

  override def frames: Int = 2

  private val checks = ListBuffer.empty[Check]

  private var model:      Option[Model]         = None
  private var instance:   Option[ModelInstance] = None
  private var modelBatch: Option[ModelBatch]    = None
  private var camera:     Option[PerspectiveCamera] = None

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    given Sge = ctx.sgeCtx
    val builder = new sge.graphics.g3d.utils.ModelBuilder()
    val m = builder.createBox(
      2f,
      2f,
      2f,
      new Material(ColorAttribute.createDiffuse(new Color(0f, 1f, 0f, 1f))),
      (VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal).toLong
    )
    model = Some(m)
    instance = Some(new ModelInstance(m))
    modelBatch = Some(new ModelBatch())
    val cam = new PerspectiveCamera(67f, WorldUnits(ctx.width.toFloat), WorldUnits(ctx.height.toFloat))
    cam.position.set(3f, 3f, 3f)
    cam.lookAt(0f, 0f, 0f)
    cam.near = 0.1f
    cam.far = 100f
    cam.update()
    camera = Some(cam)
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    (modelBatch, instance, camera) match {
      case (Some(mb), Some(inst), Some(cam)) =>
        ctx.clear(0.05f, 0.05f, 0.05f, 1f)
        mb.rendering(cam) {
          mb.render(inst)
        }
      case _ => ()
    }

  override def verify(ctx: ProbeContext): List[Check] = {
    // Unlit diffuse: every cube pixel is pure green; background stays at the dark clear color.
    val (gr, gg, gb, _) = ctx.pixelRgba(ctx.width / 2, ctx.height / 2)
    checks += Check.cond(
      "cube-center-green",
      gr < 60 && gg > 180 && gb < 60,
      "green cube pixel at center (r<60, g>180, b<60)",
      s"RGB($gr,$gg,$gb)"
    )
    val (br, bg, bb, _) = ctx.pixelRgba(60, 60)
    checks += Check.cond(
      "background-dark",
      br < 40 && bg < 40 && bb < 40,
      "dark clear color at (60,60)",
      s"RGB($br,$bg,$bb)"
    )
    modelBatch.foreach(_.close())
    model.foreach(_.close())
    modelBatch = None
    model = None
    instance = None
    camera = None
    checks.toList
  }
}
