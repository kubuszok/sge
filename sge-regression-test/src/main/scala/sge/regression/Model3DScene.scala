/*
 * SGE Regression Test — 3D rendering pipeline.
 *
 * Tests: PerspectiveCamera, ModelBatch, procedural box mesh, MVP.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

import sge.graphics.{PerspectiveCamera, VertexAttributes}
import sge.graphics.g3d.{Material, Model, ModelBatch, ModelInstance}
import sge.graphics.g3d.utils.ModelBuilder
import sge.utils.{Nullable, ScreenUtils}

/** Creates a procedural box via ModelBuilder, renders with ModelBatch, and checks for GL errors. */
object Model3DScene extends RegressionScene {

  override val name: String = "3D Model"

  private var camera:   PerspectiveCamera = scala.compiletime.uninitialized
  private var batch:    ModelBatch        = scala.compiletime.uninitialized
  private var model:    Model             = scala.compiletime.uninitialized
  private var instance: ModelInstance     = scala.compiletime.uninitialized
  private var ok:       Boolean           = false
  private var rendered: Boolean           = false

  override def init()(using Sge): Unit = {
    try {
      val w = Sge().graphics.getWidth().toFloat
      val h = Sge().graphics.getHeight().toFloat
      camera = PerspectiveCamera(67f, w, h)
      camera.near = 0.1f
      camera.far = 100f
      camera.position.set(2f, 2f, 2f)
      camera.lookAt(0f, 0f, 0f)
      camera.update()

      batch = ModelBatch(Nullable.empty, Nullable.empty, Nullable.empty)

      val builder = new ModelBuilder()
      model = builder.createBox(
        1f, 1f, 1f, Material(),
        (VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal).toLong
      )
      instance = ModelInstance(model)

      SmokeResult.logCheck("MODEL3D_SETUP", true, "Camera + ModelBatch + Box built")
      ok = true
    } catch {
      case e: Exception =>
        SmokeResult.logCheck("MODEL3D_SETUP", false, s"Exception: ${e.getMessage}")
    }
  }

  override def render(elapsed: Float)(using Sge): Unit = {
    ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f, true)
    if (!ok) return

    batch.begin(camera)
    batch.render(instance)
    batch.end()

    // Check GL error only on first render
    if (!rendered) {
      rendered = true
      val err = Sge().graphics.getGL20().glGetError()
      SmokeResult.logCheck("MODEL3D_RENDER", err == 0, if (err == 0) "no GL error" else s"0x${err.toHexString}")
    }
  }

  override def dispose()(using Sge): Unit = {
    if (batch != null) batch.close()   // scalafix:ok
    if (model != null) model.close()   // scalafix:ok
  }
}
