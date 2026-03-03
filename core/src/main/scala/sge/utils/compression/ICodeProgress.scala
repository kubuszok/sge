/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/compression/ICodeProgress.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java interface -> Scala trait (faithful 1:1 port)
 * - All methods and signatures match the original
 * - No convention issues
 */
package sge
package utils
package compression

trait ICodeProgress {
  def SetProgress(inSize: Long, outSize: Long): Unit
}
