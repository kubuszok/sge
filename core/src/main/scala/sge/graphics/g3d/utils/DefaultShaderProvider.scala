/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/DefaultShaderProvider.java
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
import sge.graphics.g3d.shaders.DefaultShader
import sge.utils.Nullable

class DefaultShaderProvider(val config: DefaultShader.Config)(using sge: Sge) extends BaseShaderProvider {

  def this(vertexShader: String, fragmentShader: String)(using sge: Sge) = {
    this(new DefaultShader.Config(vertexShader, fragmentShader))
  }

  def this(vertexShader: FileHandle, fragmentShader: FileHandle)(using sge: Sge) = {
    this(vertexShader.readString(), fragmentShader.readString())
  }

  def this()(using sge: Sge) = {
    this(new DefaultShader.Config())
  }

  override protected def createShader(renderable: Renderable): Shader =
    new DefaultShader(renderable, config)
}
