// SGE — Desktop integration test: glGetActiveUniformBlockName buffer overflow (ISS-540)
//
// Pins the crash-safety contract of the BUFFER overload
//   GL30.glGetActiveUniformBlockName(program, uniformBlockIndex, length, uniformBlockName)
// which must NEVER write more than `uniformBlockName.remaining()` bytes into the
// caller's buffer. The 3rd native argument to glGetActiveUniformBlockName is the
// GLsizei `bufSize` — the maximum number of bytes GL is allowed to write into the
// destination. The buggy AngleGL30 / AngleGL30Native hardcode this to 1024
// regardless of the caller's buffer capacity, so a small caller buffer is
// overrun → native heap corruption.
//
// Crash-safe sentinel design (so the buggy 1024-byte write cannot crash the JVM):
//   1. Compile + link a real ES3 program with a uniform block whose name is
//      LONGER than the small `remaining()` we will request (block name
//      "ISS540LongUniformBlockName", 26 chars).
//   2. Allocate the uniformBlockName ByteBuffer with LARGE capacity (2048,
//      direct/native) so even a buggy 1024-byte write stays in-bounds. Fill ALL
//      bytes with sentinel 0xAA, then set limit so remaining() == 8 at position 0.
//   3. Call the buffer overload.
//   4. Assert every byte at index [8 .. blockNameLen] is STILL the 0xAA sentinel.
//      - Correct (bufSize = remaining() = 8): GL writes ≤ 8 bytes → sentinel intact → PASS.
//      - Buggy   (bufSize = 1024):            GL writes the full 26-char name → bytes
//        [8..26] overwritten → sentinel gone → FAIL (the red).

package sge.it.desktop.checks

import sge.Sge
import sge.graphics.{ GL20, GL30, ShaderType }
import sge.it.desktop.CheckResult
import sge.utils.BufferUtils

/** Verifies that the glGetActiveUniformBlockName buffer overload never writes past uniformBlockName.remaining(). */
object UniformBlockNameCheck {

  // A uniform block whose name (26 chars) is far longer than the 8-byte
  // `remaining()` we will grant the destination buffer. The buggy bufSize=1024
  // lets GL write all 26 chars; the fix (bufSize=remaining()) caps it at 8.
  private val blockName: String = "ISS540LongUniformBlockName"

  private val requestedRemaining: Int = 8
  private val bufCapacity:        Int = 2048
  private val sentinel:           Byte = 0xaa.toByte

  // A minimal ES3 program declaring `blockName` as a std140 uniform block so the
  // block name is preserved in the linked program and queryable by index.
  private val vertexSrc: String =
    s"""#version 300 es
       |uniform $blockName {
       |  vec4 uColor;
       |  vec4 uOffset;
       |};
       |in vec4 aPos;
       |void main() {
       |  gl_Position = aPos + uOffset + uColor * 0.0;
       |}
       |""".stripMargin

  private val fragmentSrc: String =
    s"""#version 300 es
       |precision mediump float;
       |uniform $blockName {
       |  vec4 uColor;
       |  vec4 uOffset;
       |};
       |out vec4 fragColor;
       |void main() {
       |  fragColor = uColor + uOffset;
       |}
       |""".stripMargin

  private def compileShader(gl30: GL30, kind: ShaderType, src: String): Either[String, Int] = {
    val shader = gl30.glCreateShader(kind)
    gl30.glShaderSource(shader, src)
    gl30.glCompileShader(shader)
    val status = BufferUtils.newIntBuffer(1)
    gl30.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, status)
    if (status.get(0) == 0) {
      val log = gl30.glGetShaderInfoLog(shader)
      gl30.glDeleteShader(shader)
      Left(s"shader compile failed: $log")
    } else {
      Right(shader)
    }
  }

  def run()(using Sge): CheckResult =
    try {
      val graphics  = Sge().graphics
      val maybeGl30 = graphics.gl30
      if (maybeGl30.isEmpty) {
        CheckResult(
          "uniformblockname",
          passed = false,
          "gl30 unavailable — cannot compile a UBO program (no ES 3.0 context)"
        )
      } else {
        val gl30 = maybeGl30.get
        compileShader(gl30, ShaderType.Vertex, vertexSrc) match {
          case Left(msg) =>
            CheckResult("uniformblockname", passed = false, s"vertex $msg")
          case Right(vs) =>
            compileShader(gl30, ShaderType.Fragment, fragmentSrc) match {
              case Left(msg) =>
                gl30.glDeleteShader(vs)
                CheckResult("uniformblockname", passed = false, s"fragment $msg")
              case Right(fs) =>
                val program = gl30.glCreateProgram()
                gl30.glAttachShader(program, vs)
                gl30.glAttachShader(program, fs)
                gl30.glLinkProgram(program)
                val linkStatus = BufferUtils.newIntBuffer(1)
                gl30.glGetProgramiv(program, GL20.GL_LINK_STATUS, linkStatus)
                if (linkStatus.get(0) == 0) {
                  val log = gl30.glGetProgramInfoLog(program)
                  gl30.glDeleteShader(vs)
                  gl30.glDeleteShader(fs)
                  gl30.glDeleteProgram(program)
                  CheckResult("uniformblockname", passed = false, s"program link failed: $log")
                } else {
                  val blockIndex = gl30.glGetUniformBlockIndex(program, blockName)
                  if (blockIndex == GL30.GL_INVALID_INDEX || blockIndex < 0) {
                    gl30.glDeleteShader(vs)
                    gl30.glDeleteShader(fs)
                    gl30.glDeleteProgram(program)
                    CheckResult(
                      "uniformblockname",
                      passed = false,
                      s"glGetUniformBlockIndex('$blockName') returned $blockIndex (GL_INVALID_INDEX) — " +
                        "the linked program does not expose the uniform block; cannot exercise the overflow"
                    )
                  } else {
                    val result = runSentinelCheck(gl30, program, blockIndex)
                    gl30.glDeleteShader(vs)
                    gl30.glDeleteShader(fs)
                    gl30.glDeleteProgram(program)
                    result
                  }
                }
            }
        }
      }
    } catch {
      case e: Exception =>
        CheckResult("uniformblockname", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }

  private def runSentinelCheck(gl30: GL30, program: Int, blockIndex: Int)(using Sge): CheckResult = {
    // Large direct/native buffer so even a buggy 1024-byte GL write stays
    // in-bounds (no native crash). Fill every byte with the sentinel.
    val nameBuf = BufferUtils.newByteBuffer(bufCapacity)
    var i       = 0
    while (i < bufCapacity) {
      nameBuf.put(i, sentinel)
      i += 1
    }
    // Grant the caller only `requestedRemaining` bytes: position 0, limit 8.
    nameBuf.position(0)
    nameBuf.limit(requestedRemaining)

    // length out-param (GLsizei*): number of chars actually written (excl. NUL).
    val lengthBuf = BufferUtils.newByteBuffer(4)

    // The call under test. Against the bug, GL writes the full 26-char name.
    gl30.glGetActiveUniformBlockName(program, blockIndex, lengthBuf, nameBuf)

    // Restore the limit to full capacity so the absolute get(index) reads below
    // (which are bounded by limit(), not capacity()) can inspect the bytes BEYOND
    // the 8-byte window the caller granted GL — that out-of-window region is
    // exactly where the overflow would land.
    nameBuf.limit(bufCapacity)

    // Core overflow proof: every byte at [requestedRemaining .. blockName.length]
    // must still hold the sentinel. The buffer overload may write at most
    // `remaining()` bytes; anything beyond that is an out-of-bounds write that on
    // a real (small) caller allocation would corrupt the native heap.
    val nameLen      = blockName.length
    var firstClobber = -1
    var j            = requestedRemaining
    while (j <= nameLen && firstClobber < 0) {
      if (nameBuf.get(j) != sentinel) firstClobber = j
      j += 1
    }

    if (firstClobber >= 0) {
      // Decode what GL wrote past the granted window, for diagnostics.
      val overran = new StringBuilder
      var k       = requestedRemaining
      while (k <= nameLen) {
        val b = nameBuf.get(k)
        if (b != sentinel && b != 0) overran.append(b.toChar)
        k += 1
      }
      CheckResult(
        "uniformblockname",
        passed = false,
        s"glGetActiveUniformBlockName(buffer overload) overran the caller buffer: granted remaining()=" +
          s"$requestedRemaining bytes but sentinel at index $firstClobber was overwritten (GL wrote past the " +
          s"limit; recovered overrun text='${overran.toString}'). The native bufSize is hardcoded to 1024 " +
          s"instead of uniformBlockName.remaining() — a smaller real allocation would be heap-corrupted (ISS-540)."
      )
    } else {
      CheckResult(
        "uniformblockname",
        passed = true,
        s"glGetActiveUniformBlockName(buffer overload) respected remaining()=$requestedRemaining: no byte beyond " +
          s"the granted window was modified (block name length=$nameLen)"
      )
    }
  }
}
