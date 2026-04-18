/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/LzmaUtils.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: GdxRuntimeException -> SgeError
 *   Convention: utility object instead of final class with private constructor
 *   Idiom: split packages; try-with-resources -> try/finally with AutoCloseable
 *   Note: SGE does not yet port libGDX's Lzma class (sge.utils.compression.Lzma).
 *     Methods are present but throw until the Lzma dependency is available.
 */
package sge
package textra

import sge.files.FileHandle
import sge.utils.SgeError

/** Simple static utilities to make using LZMA compression easier. This simply handles opening and closing compressed and decompressed files without needing to catch IOException. If something goes
  * wrong, this will throw an unchecked exception, an SgeError, with more information than the original IOException would have.
  */
object LzmaUtils {

  /** Given an {@code input} FileHandle compressed with LZMA, and an {@code output} FileHandle that will be overwritten, decompresses input into output.
    * @param input
    *   the LZMA-compressed FileHandle to read; typically the file extension ends in ".lzma"
    * @param output
    *   the FileHandle to write to; will be overwritten
    */
  def decompress(input: FileHandle, output: FileHandle): Unit =
    // LZMA: requires sge.utils.compression.Lzma (not yet ported from LibGDX)
    throw SgeError.InvalidInput("LZMA decompression is not yet supported: sge.utils.compression.Lzma is not available")

  /** Given an {@code input} FileHandle and an {@code output} FileHandle that will be overwritten, compresses input using LZMA and writes the result into output.
    * @param input
    *   the FileHandle to read; will not be modified
    * @param output
    *   the FileHandle to write LZMA-compressed output to; typically the file extension ends in ".lzma"
    */
  def compress(input: FileHandle, output: FileHandle): Unit =
    // LZMA: requires sge.utils.compression.Lzma (not yet ported from LibGDX)
    throw SgeError.InvalidInput("LZMA compression is not yet supported: sge.utils.compression.Lzma is not available")
}
