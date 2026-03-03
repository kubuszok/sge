/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/DefaultShaderProvider.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Java null-safe config: `(config == null) ? new Config() : config` -> Scala no-null: requires Sge context
 *   - No-arg constructor: Java passes null -> Scala creates new Config() directly
 *   - All 4 constructors and createShader fully ported
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d
package utils

import sge.files.FileHandle
import sge.graphics.g3d.shaders.DefaultShader

class DefaultShaderProvider(val config: DefaultShader.Config)(using Sge) extends BaseShaderProvider {

  def this(vertexShader: String, fragmentShader: String)(using Sge) =
    this(new DefaultShader.Config(vertexShader, fragmentShader))

  def this(vertexShader: FileHandle, fragmentShader: FileHandle)(using Sge) =
    this(vertexShader.readString(), fragmentShader.readString())

  def this()(using Sge) =
    this(new DefaultShader.Config())

  override protected def createShader(renderable: Renderable): Shader =
    new DefaultShader(renderable, config)
}
