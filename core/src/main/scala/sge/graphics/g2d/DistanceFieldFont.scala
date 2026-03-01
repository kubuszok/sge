/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/DistanceFieldFont.java
 * Original authors: Florian Falkner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.files.FileHandle
import sge.graphics.Texture
import sge.graphics.Texture.TextureFilter
import sge.graphics.glutils.ShaderProgram
import sge.utils.{ DynamicArray, Nullable }

/** Renders bitmap fonts using distance field textures, see the <a href="https://libgdx.com/wiki/graphics/2d/fonts/distance-field-fonts">Distance Field Fonts wiki article</a> for usage. Initialize the
  * SpriteBatch with the {@link #createDistanceFieldShader()} shader. <p> Attention: The batch is flushed before and after each string is rendered.
  * @author
  *   Florian Falkner
  */
class DistanceFieldFont(data: BitmapFontData, pageRegions: Nullable[DynamicArray[TextureRegion]], integer: Boolean)(using sge: Sge) extends BitmapFont(data, pageRegions, integer) {
  super.load(data)

  private var distanceFieldSmoothing: Float = scala.compiletime.uninitialized

  def this(fontFile: FileHandle, flip: Boolean)(using sge: Sge) = {
    this(new BitmapFontData(Nullable(fontFile), flip), Nullable.empty, true)
  }

  def this(fontFile: FileHandle, imageFile: FileHandle, flip: Boolean, integer: Boolean)(using sge: Sge) = {
    this(
      new BitmapFontData(Nullable(fontFile), flip),
      Nullable { val da = DynamicArray[TextureRegion](); da.add(new TextureRegion(new Texture(imageFile, false))); da },
      integer
    )
    setOwnsTexture(true)
  }

  def this(fontFile: FileHandle, imageFile: FileHandle, flip: Boolean)(using sge: Sge) = {
    this(fontFile, imageFile, flip, true)
  }

  def this(fontFile: FileHandle, region: Nullable[TextureRegion], flip: Boolean)(using sge: Sge) = {
    this(
      new BitmapFontData(Nullable(fontFile), flip),
      region.fold(Nullable.empty[DynamicArray[TextureRegion]]) { r =>
        val da = DynamicArray[TextureRegion](); da.add(r); Nullable(da)
      },
      true
    )
  }

  def this(fontFile: FileHandle, region: Nullable[TextureRegion])(using sge: Sge) = {
    this(fontFile, region, false)
  }

  def this(fontFile: FileHandle)(using sge: Sge) = {
    this(fontFile, Nullable.empty[TextureRegion])
  }

  override protected def load(data: BitmapFontData): Unit = {
    super.load(data)

    // Distance field font rendering requires font texture to be filtered linear.
    val regions = getRegions()
    for (region <- regions)
      region.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear)
  }

  override def newFontCache(): BitmapFontCache =
    new DistanceFieldFontCache(this, integer)

  /** @return The distance field smoothing factor for this font. */
  def getDistanceFieldSmoothing(): Float =
    distanceFieldSmoothing

  /** @param distanceFieldSmoothing
    *   Set the distance field smoothing factor for this font. SpriteBatch needs to have this shader set for rendering distance field fonts.
    */
  def setDistanceFieldSmoothing(distanceFieldSmoothing: Float): Unit =
    this.distanceFieldSmoothing = distanceFieldSmoothing

  /** Provides a font cache that uses distance field shader for rendering fonts. Attention: breaks batching because uniform is needed for smoothing factor, so a flush is performed before and after
    * every font rendering.
    * @author
    *   Florian Falkner
    */
  private class DistanceFieldFontCache(font: DistanceFieldFont, integer: Boolean) extends BitmapFontCache(font, integer) {
    def this(font: DistanceFieldFont) = {
      this(font, font.usesIntegerPositions())
    }

    private def getSmoothingFactor(): Float = {
      val font = super.getFont().asInstanceOf[DistanceFieldFont]
      font.getDistanceFieldSmoothing() * font.getScaleX()
    }

    private def setSmoothingUniform(spriteBatch: Batch, smoothing: Float): Unit = {
      spriteBatch.flush()
      spriteBatch.getShader().setUniformf("u_smoothing", smoothing)
    }

    override def draw(spriteBatch: Batch): Unit = {
      setSmoothingUniform(spriteBatch, getSmoothingFactor())
      super.draw(spriteBatch)
      setSmoothingUniform(spriteBatch, 0)
    }

    override def draw(spriteBatch: Batch, start: Int, end: Int): Unit = {
      setSmoothingUniform(spriteBatch, getSmoothingFactor())
      super.draw(spriteBatch, start, end)
      setSmoothingUniform(spriteBatch, 0)
    }
  }
}

object DistanceFieldFont {

  /** Returns a new instance of the distance field shader, see https://libgdx.com/wiki/graphics/2d/fonts/distance-field-fonts if the u_smoothing uniform > 0.0. Otherwise the same code as the default
    * SpriteBatch shader is used.
    */
  def createDistanceFieldShader()(using sge: Sge): ShaderProgram = {
    val vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "uniform mat4 u_projTrans;\n" +
      "varying vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "\n" +
      "void main() {\n" +
      "	v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "	v_color.a = v_color.a * (255.0/254.0);\n" +
      "	gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "}\n"

    val fragmentShader = "#ifdef GL_ES\n" +
      "	precision mediump float;\n" +
      "	precision mediump int;\n" +
      "#endif\n" +
      "\n" +
      "uniform sampler2D u_texture;\n" +
      "uniform float u_smoothing;\n" +
      "varying vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "\n" +
      "void main() {\n" +
      "	if (u_smoothing > 0.0) {\n" +
      "		float smoothing = 0.25 / u_smoothing;\n" +
      "		float distance = texture2D(u_texture, v_texCoords).a;\n" +
      "		float alpha = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);\n" +
      "		gl_FragColor = vec4(v_color.rgb, alpha * v_color.a);\n" +
      "	} else {\n" +
      "		gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" +
      "	}\n" +
      "}\n"

    val shader = new ShaderProgram(vertexShader, fragmentShader)
    if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling distance field shader: " + shader.getLog())
    shader
  }
}
