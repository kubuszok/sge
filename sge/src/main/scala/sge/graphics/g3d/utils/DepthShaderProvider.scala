/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/DepthShaderProvider.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Same pattern as DefaultShaderProvider
 *   - Java null-safe config -> Scala no-null with direct Config() instantiation
 *   - All 4 constructors and createShader fully ported
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: DepthShaderProvider,createShader,this
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/DepthShaderProvider.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package utils

import sge.files.FileHandle
import sge.graphics.g3d.shaders.DepthShader

class DepthShaderProvider(val config: DepthShader.Config)(using Sge) extends BaseShaderProvider {

  def this(vertexShader: String, fragmentShader: String)(using Sge) =
    this(DepthShader.Config(vertexShader, fragmentShader))

  def this(vertexShader: FileHandle, fragmentShader: FileHandle)(using Sge) =
    this(vertexShader.readString(), fragmentShader.readString())

  def this()(using Sge) =
    this(DepthShader.Config())

  override protected def createShader(renderable: Renderable): Shader =
    DepthShader(renderable, config)
}
