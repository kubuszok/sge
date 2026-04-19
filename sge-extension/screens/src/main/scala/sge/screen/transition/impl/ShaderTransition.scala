/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 95
 * Covenant-baseline-methods: ShaderTransition,close,currScreenLoc,getProgram,lastScreenLoc,program,progressLoc,projTransLoc,render,renderContext,resize,screenQuad,this,viewport
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen
package transition
package impl

import sge.graphics.{ Mesh, PrimitiveMode, UniformLocation }
import sge.graphics.g2d.TextureRegion
import sge.graphics.g3d.utils.{ DefaultTextureBinder, RenderContext }
import sge.graphics.glutils.ShaderProgram
import sge.math.Interpolation
import sge.screen.utils.QuadMeshGenerator
import sge.utils.{ Nullable, Seconds }
import sge.utils.viewport.{ ScreenViewport, Viewport }

/** A transition that is using a shader to render the two transitioning screens. Can be reused.
  *
  * The following uniforms are set before rendering and thus have to be specified in the shader code:
  *   - vertex shader: `uniform mat4 u_projTrans`
  *   - fragment shader: `uniform sampler2D lastScreen`, `uniform sampler2D currScreen`, `uniform float progress`
  *
  * @author
  *   damios
  *
  * @see
  *   [[GLTransitionsShaderTransition]]
  */
class ShaderTransition(
  vert:          String,
  frag:          String,
  ignorePrepend: Boolean,
  duration:      Float,
  interpolation: Nullable[Interpolation] = Nullable.empty
)(using Sge)
    extends TimedTransition(duration, interpolation) {

  protected val program:  ShaderProgram = new ShaderProgram(vert, frag)
  protected var viewport: Viewport      = new ScreenViewport() // Renders the transition over the whole screen

  private val renderContext: RenderContext = new RenderContext(
    new DefaultTextureBinder(DefaultTextureBinder.ROUNDROBIN)
  )

  /** A screen filling quad. */
  private var screenQuad: Nullable[Mesh] = Nullable.empty

  private val projTransLoc:  UniformLocation = program.getUniformLocation("u_projTrans")
  private val lastScreenLoc: UniformLocation = program.getUniformLocation("lastScreen")
  private val currScreenLoc: UniformLocation = program.getUniformLocation("currScreen")
  private val progressLoc:   UniformLocation = program.getUniformLocation("progress")

  def this(vert: String, frag: String, ignorePrepend: Boolean, duration: Float)(using Sge) =
    this(vert, frag, ignorePrepend, duration, Nullable.empty)

  override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion, progress: Float): Unit = {
    viewport.apply()

    renderContext.begin()
    program.bind()

    // Set uniforms
    program.setUniformMatrix(projTransLoc, viewport.camera.combined)
    program.setUniformf(progressLoc, progress)
    program.setUniformi(lastScreenLoc, renderContext.textureBinder.bind(lastScreen.texture))
    program.setUniformi(currScreenLoc, renderContext.textureBinder.bind(currScreen.texture))

    // Render the screens using the shader
    screenQuad.foreach { quad =>
      quad.render(program, PrimitiveMode.Triangles)
    }

    renderContext.end()
  }

  override def resize(width: Pixels, height: Pixels): Unit = {
    viewport.update(width, height, true)

    screenQuad.foreach(_.close())
    screenQuad = Nullable(QuadMeshGenerator.createFullScreenQuad(width.toFloat, height.toFloat))
  }

  override def close(): Unit = {
    program.close()
    screenQuad.foreach(_.close())
  }

  /** Returns the shader used by this transition. */
  def getProgram: ShaderProgram = program
}
