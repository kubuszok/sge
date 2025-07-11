package sge
package assets
package loaders

import sge.files.FileHandle
import sge.graphics.glutils.ShaderProgram
import sge.utils.Nullable
import scala.collection.mutable.ArrayBuffer

/** {@link AssetLoader} for {@link ShaderProgram} instances loaded from text files. If the file suffix is ".vert", it is assumed to be a vertex shader, and a fragment shader is found using the same
  * file name with a ".frag" suffix. And vice versa if the file suffix is ".frag". These default suffixes can be changed in the ShaderProgramLoader constructor. <p> For all other file suffixes, the
  * same file is used for both (and therefore should internally distinguish between the programs using preprocessor directives and {@link ShaderProgram#prependVertexCode} and
  * {@link ShaderProgram#prependFragmentCode} ). <p> The above default behavior for finding the files can be overridden by explicitly setting the file names in a {@link ShaderProgramParameter} . The
  * parameter can also be used to prepend code to the programs.
  * @author
  *   cypherdare (original implementation)
  */
class ShaderProgramLoader(resolver: FileHandleResolver, private val vertexFileSuffix: String = ".vert", private val fragmentFileSuffix: String = ".frag")(using sge: Sge)
    extends AsynchronousAssetLoader[ShaderProgram, ShaderProgramLoader.ShaderProgramParameter](resolver) {

  def this(resolver: FileHandleResolver)(using sge: Sge) = this(resolver, ".vert", ".frag")

  override def getDependencies(fileName: String, file: FileHandle, parameter: ShaderProgramLoader.ShaderProgramParameter): ArrayBuffer[AssetDescriptor[?]] =
    ArrayBuffer.empty

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: ShaderProgramLoader.ShaderProgramParameter): Unit = {
    // Nothing to load asynchronously
  }

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: ShaderProgramLoader.ShaderProgramParameter): ShaderProgram = {
    var vertFileName: String = null
    var fragFileName: String = null

    if (parameter != null) {
      if (parameter.vertexFile != null) vertFileName = parameter.vertexFile
      if (parameter.fragmentFile != null) fragFileName = parameter.fragmentFile
    }

    if (vertFileName == null && fileName.endsWith(fragmentFileSuffix)) {
      vertFileName = fileName.substring(0, fileName.length() - fragmentFileSuffix.length()) + vertexFileSuffix
    }
    if (fragFileName == null && fileName.endsWith(vertexFileSuffix)) {
      fragFileName = fileName.substring(0, fileName.length() - vertexFileSuffix.length()) + fragmentFileSuffix
    }

    val vertexFile   = if (vertFileName == null) file else resolve(vertFileName)
    val fragmentFile = if (fragFileName == null) file else resolve(fragFileName)
    var vertexCode   = vertexFile.readString()
    var fragmentCode = if (vertexFile.equals(fragmentFile)) vertexCode else fragmentFile.readString()

    if (parameter != null) {
      if (parameter.prependVertexCode != null) vertexCode = parameter.prependVertexCode + vertexCode
      if (parameter.prependFragmentCode != null) fragmentCode = parameter.prependFragmentCode + fragmentCode
    }

    val shaderProgram = new ShaderProgram(vertexCode, fragmentCode)
    if ((parameter == null || parameter.logOnCompileFailure) && !shaderProgram.isCompiled()) {
      sge.application.error("ShaderProgramLoader", s"ShaderProgram $fileName failed to compile:\n${shaderProgram.getLog()}")
    }

    shaderProgram
  }
}

object ShaderProgramLoader {
  class ShaderProgramParameter extends AssetLoaderParameters[ShaderProgram] {

    /** File name to be used for the vertex program instead of the default determined by the file name used to submit this asset to AssetManager.
      */
    var vertexFile: String = null

    /** File name to be used for the fragment program instead of the default determined by the file name used to submit this asset to AssetManager.
      */
    var fragmentFile: String = null

    /** Whether to log (at the error level) the shader's log if it fails to compile. Default true. */
    var logOnCompileFailure: Boolean = true

    /** Code that is always added to the vertex shader code. This is added as-is, and you should include a newline (`\n`) if needed. {@linkplain ShaderProgram#prependVertexCode} is placed before this
      * code.
      */
    var prependVertexCode: String = null

    /** Code that is always added to the fragment shader code. This is added as-is, and you should include a newline (`\n`) if needed. {@linkplain ShaderProgram#prependFragmentCode} is placed before
      * this code.
      */
    var prependFragmentCode: String = null
  }
}
