/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/utils/ShaderParser.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * ShaderParser allows to recursively load shader code split into several files.
 * It brings support for file inclusion like: #include<part.glsl>
 * Given paths are relative to the file declaring the include statement.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: ShaderParser,content,i,includeAfter,includeBefore,lines,parse,sb
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package utils

import sge.files.FileHandle
import sge.utils.SgeError

object ShaderParser {

  private val includeBefore: String = "#include <"
  private val includeAfter:  String = ">"

  def parse(file: FileHandle): String = {
    val content = file.readString()
    val lines   = content.split("\n")
    val sb      = new StringBuilder
    var i       = 0
    while (i < lines.length) {
      val line      = lines(i)
      val cleanLine = line.trim
      if (cleanLine.startsWith(includeBefore)) {
        val end = cleanLine.indexOf(includeAfter, includeBefore.length)
        if (end < 0) throw SgeError.InvalidInput("malformed include: " + cleanLine)
        val path    = cleanLine.substring(includeBefore.length, end)
        val subFile = file.sibling(path)
        sb.append("\n//////// ").append(path).append("\n")
        sb.append(parse(subFile))
      } else {
        sb.append(line).append("\n")
      }
      i += 1
    }
    sb.toString
  }
}
