/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 18
 * Covenant-baseline-methods: GLTFRuntimeException,this
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package exceptions

/** root GLTF loading error */
class GLTFRuntimeException(message: String, cause: Throwable) extends RuntimeException(message, cause) {

  def this(message: String) = this(message, null) // @nowarn — Java interop, RuntimeException expects null

  def this(cause: Throwable) = this(if (cause != null) cause.getMessage else null, cause) // @nowarn — Java interop
}
