/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/DefaultShaderProvider.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Java null-safe config: `(config == null) ? new Config() : config` -> Scala no-null: requires Sge context
 *   - No-arg constructor: Java passes null -> Scala creates new Config() directly
 *   - All 4 constructors and createShader fully ported
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: DefaultShaderProvider,createShader,this
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/DefaultShaderProvider.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package utils

import sge.files.FileHandle
import sge.graphics.g3d.shaders.DefaultShader

class DefaultShaderProvider(val config: DefaultShader.Config)(using Sge) extends BaseShaderProvider {

  def this(vertexShader: String, fragmentShader: String)(using Sge) =
    this(DefaultShader.Config(vertexShader, fragmentShader))

  def this(vertexShader: FileHandle, fragmentShader: FileHandle)(using Sge) =
    this(vertexShader.readString(), fragmentShader.readString())

  def this()(using Sge) =
    this(DefaultShader.Config())

  override protected def createShader(renderable: Renderable): Shader =
    DefaultShader(renderable, config)
}
