/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/ShaderProgramLoader.java
 * Original authors: cypherdare
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Convention: logging uses manager.log.error()
 *   TODOs: test: ShaderProgramLoader file suffix resolution (.vert/.frag) and code prepend logic
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.graphics.glutils.ShaderProgram
import sge.utils.{ DynamicArray, Nullable }

/** {@link AssetLoader} for {@link ShaderProgram} instances loaded from text files. If the file suffix is ".vert", it is assumed to be a vertex shader, and a fragment shader is found using the same
  * file name with a ".frag" suffix. And vice versa if the file suffix is ".frag". These default suffixes can be changed in the ShaderProgramLoader constructor. <p> For all other file suffixes, the
  * same file is used for both (and therefore should internally distinguish between the programs using preprocessor directives and {@link ShaderProgram#prependVertexCode} and
  * {@link ShaderProgram#prependFragmentCode} ). <p> The above default behavior for finding the files can be overridden by explicitly setting the file names in a {@link ShaderProgramParameter} . The
  * parameter can also be used to prepend code to the programs.
  * @author
  *   cypherdare (original implementation)
  */
class ShaderProgramLoader(resolver: FileHandleResolver, private val vertexFileSuffix: String = ".vert", private val fragmentFileSuffix: String = ".frag")(using Sge)
    extends AsynchronousAssetLoader[ShaderProgram, ShaderProgramLoader.ShaderProgramParameter](resolver) {

  override def getDependencies(fileName: String, file: FileHandle, parameter: ShaderProgramLoader.ShaderProgramParameter): DynamicArray[AssetDescriptor[?]] =
    DynamicArray[AssetDescriptor[?]]()

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: ShaderProgramLoader.ShaderProgramParameter): Unit = {
    // Nothing to load asynchronously
  }

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: ShaderProgramLoader.ShaderProgramParameter): ShaderProgram = {
    val param = Nullable(parameter)
    var vertFileName: Nullable[String] = Nullable.empty
    var fragFileName: Nullable[String] = Nullable.empty

    param.foreach { p =>
      p.vertexFile.foreach(v => vertFileName = Nullable(v))
      p.fragmentFile.foreach(f => fragFileName = Nullable(f))
    }

    if (vertFileName.isEmpty && fileName.endsWith(fragmentFileSuffix)) {
      vertFileName = Nullable(fileName.substring(0, fileName.length() - fragmentFileSuffix.length()) + vertexFileSuffix)
    }
    if (fragFileName.isEmpty && fileName.endsWith(vertexFileSuffix)) {
      fragFileName = Nullable(fileName.substring(0, fileName.length() - vertexFileSuffix.length()) + fragmentFileSuffix)
    }

    val vertexFile   = vertFileName.map(resolve(_)).getOrElse(file)
    val fragmentFile = fragFileName.map(resolve(_)).getOrElse(file)
    var vertexCode   = vertexFile.readString()
    var fragmentCode = if (vertexFile.equals(fragmentFile)) vertexCode else fragmentFile.readString()

    param.foreach { p =>
      p.prependVertexCode.foreach(code => vertexCode = code + vertexCode)
      p.prependFragmentCode.foreach(code => fragmentCode = code + fragmentCode)
    }

    val shaderProgram = ShaderProgram(vertexCode, fragmentCode)
    if (param.forall(_.logOnCompileFailure) && !shaderProgram.compiled) {
      manager.log.error(s"ShaderProgram $fileName failed to compile:\n${shaderProgram.getLog()}")
    }

    shaderProgram
  }
}

object ShaderProgramLoader {
  class ShaderProgramParameter extends AssetLoaderParameters[ShaderProgram] {

    /** File name to be used for the vertex program instead of the default determined by the file name used to submit this asset to AssetManager.
      */
    var vertexFile: Nullable[String] = Nullable.empty

    /** File name to be used for the fragment program instead of the default determined by the file name used to submit this asset to AssetManager.
      */
    var fragmentFile: Nullable[String] = Nullable.empty

    /** Whether to log (at the error level) the shader's log if it fails to compile. Default true. */
    var logOnCompileFailure: Boolean = true

    /** Code that is always added to the vertex shader code. This is added as-is, and you should include a newline (`\n`) if needed. {@linkplain ShaderProgram#prependVertexCode} is placed before this
      * code.
      */
    var prependVertexCode: Nullable[String] = Nullable.empty

    /** Code that is always added to the fragment shader code. This is added as-is, and you should include a newline (`\n`) if needed. {@linkplain ShaderProgram#prependFragmentCode} is placed before
      * this code.
      */
    var prependFragmentCode: Nullable[String] = Nullable.empty
  }
}
