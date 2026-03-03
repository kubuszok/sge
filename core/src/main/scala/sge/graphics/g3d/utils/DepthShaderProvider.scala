/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/DepthShaderProvider.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils

import sge.files.FileHandle
import sge.graphics.g3d.shaders.DepthShader

class DepthShaderProvider(val config: DepthShader.Config)(using Sge) extends BaseShaderProvider {

  def this(vertexShader: String, fragmentShader: String)(using Sge) =
    this(new DepthShader.Config(vertexShader, fragmentShader))

  def this(vertexShader: FileHandle, fragmentShader: FileHandle)(using Sge) =
    this(vertexShader.readString(), fragmentShader.readString())

  def this()(using Sge) =
    this(new DepthShader.Config())

  override protected def createShader(renderable: Renderable): Shader =
    new DepthShader(renderable, config)
}
