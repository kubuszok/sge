/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 91
 * Covenant-baseline-methods: PickerCommons,close,createPixmap,getBarShader,gridShader,hsvShader,loadShader,loadShaders,paletteShader,program,rgbShader,verticalChannelShader,whitePixmap,whiteTexture
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/internal/PickerCommons.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget
package color
package internal

import sge.graphics.{ Color, Pixmap, Texture }
import sge.graphics.Pixmap.Format
import sge.graphics.Texture.TextureWrap
import sge.graphics.glutils.ShaderProgram
import sge.utils.Nullable
import sge.visui.Sizes

/** @author Kotcrab */
class PickerCommons(val style: ColorPickerWidgetStyle, val sizes: Sizes, loadExtendedShaders: Boolean)(using Sge) extends AutoCloseable {

  var paletteShader:         ShaderProgram           = scala.compiletime.uninitialized
  var verticalChannelShader: ShaderProgram           = scala.compiletime.uninitialized
  var hsvShader:             Nullable[ShaderProgram] = Nullable.empty
  var rgbShader:             Nullable[ShaderProgram] = Nullable.empty
  var gridShader:            ShaderProgram           = scala.compiletime.uninitialized

  var whiteTexture: Texture = scala.compiletime.uninitialized

  {
    createPixmap()
    loadShaders()
  }

  private def createPixmap(): Unit = {
    val whitePixmap = new Pixmap(2, 2, Format.RGB888)
    whitePixmap.setColor(Color.WHITE)
    whitePixmap.drawRectangle(0, 0, 2, 2)
    whiteTexture = new Texture(whitePixmap)
    whiteTexture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat)
    whitePixmap.close()
  }

  private def loadShaders(): Unit = {
    paletteShader = loadShader("default.vert", "palette.frag")
    verticalChannelShader = loadShader("default.vert", "verticalBar.frag")
    gridShader = loadShader("default.vert", "checkerboard.frag")

    if (loadExtendedShaders) {
      hsvShader = Nullable(loadShader("default.vert", "hsv.frag"))
      rgbShader = Nullable(loadShader("default.vert", "rgb.frag"))
    }
  }

  private def loadShader(vertFile: String, fragFile: String): ShaderProgram = {
    val program = new ShaderProgram(
      Sge().files.classpath("com/kotcrab/vis/ui/widget/color/internal/" + vertFile),
      Sge().files.classpath("com/kotcrab/vis/ui/widget/color/internal/" + fragFile)
    )

    if (!program.compiled) {
      throw new IllegalStateException("ColorPicker shader compilation failed. Shader: " + vertFile + ", " + fragFile + ": " + program.log)
    }

    program
  }

  def getBarShader(mode: Int): ShaderProgram =
    mode match {
      case ChannelBar.MODE_ALPHA | ChannelBar.MODE_R | ChannelBar.MODE_G | ChannelBar.MODE_B =>
        rgbShader.get
      case ChannelBar.MODE_H | ChannelBar.MODE_S | ChannelBar.MODE_V =>
        hsvShader.get
      case _ =>
        throw new IllegalStateException("Unsupported mode: " + mode)
    }

  override def close(): Unit = {
    whiteTexture.close()
    paletteShader.close()
    verticalChannelShader.close()
    gridShader.close()

    if (loadExtendedShaders) {
      hsvShader.get.close()
      rgbShader.get.close()
    }
  }
}
