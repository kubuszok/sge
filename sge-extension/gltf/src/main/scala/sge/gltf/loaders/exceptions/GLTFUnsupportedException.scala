/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package exceptions

/** root exception for features allowed by GLTF 2.0 specification but not supported by this implementation. */
class GLTFUnsupportedException(message: String) extends GLTFRuntimeException(message)
