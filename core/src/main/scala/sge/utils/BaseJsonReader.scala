/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/BaseJsonReader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.InputStream

/** Interface for JSON readers that can parse InputStreams and FileHandles. */
trait BaseJsonReader {
  def parse(input: InputStream): JsonValue
  def parse(file: files.FileHandle): JsonValue
}
