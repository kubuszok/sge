// SGE — Integration test: GL20Ops / GL30Ops API interfaces
//
// Tests that the GL ops traits and provider factory methods have expected shapes.

package sge
package platform
package android

import munit.FunSuite
import java.nio.{ Buffer, FloatBuffer, IntBuffer, LongBuffer }

class AndroidOpsGLTest extends FunSuite {

  // ── GL20Ops ─────────────────────────────────────────────────────────

  test("GL20Ops trait has core texture methods") {
    val cls = classOf[GL20Ops]
    assert(cls.getMethod("glActiveTexture", classOf[Int]) != null)
    assert(cls.getMethod("glBindTexture", classOf[Int], classOf[Int]) != null)
    assert(cls.getMethod("glGenTexture") != null)
    assert(cls.getMethod("glDeleteTexture", classOf[Int]) != null)
    assert(
      cls.getMethod(
        "glTexImage2D",
        classOf[Int],
        classOf[Int],
        classOf[Int],
        classOf[Int],
        classOf[Int],
        classOf[Int],
        classOf[Int],
        classOf[Int],
        classOf[Buffer]
      ) != null
    )
  }

  test("GL20Ops trait has shader/program methods") {
    val cls = classOf[GL20Ops]
    assert(cls.getMethod("glCreateProgram") != null)
    assert(cls.getMethod("glCreateShader", classOf[Int]) != null)
    assert(cls.getMethod("glCompileShader", classOf[Int]) != null)
    assert(cls.getMethod("glLinkProgram", classOf[Int]) != null)
    assert(cls.getMethod("glUseProgram", classOf[Int]) != null)
    assert(cls.getMethod("glDeleteProgram", classOf[Int]) != null)
    assert(cls.getMethod("glShaderSource", classOf[Int], classOf[String]) != null)
    assert(cls.getMethod("glGetProgramInfoLog", classOf[Int]) != null)
    assert(cls.getMethod("glGetShaderInfoLog", classOf[Int]) != null)
  }

  test("GL20Ops trait has buffer methods") {
    val cls = classOf[GL20Ops]
    assert(cls.getMethod("glGenBuffer") != null)
    assert(cls.getMethod("glDeleteBuffer", classOf[Int]) != null)
    assert(cls.getMethod("glBindBuffer", classOf[Int], classOf[Int]) != null)
    assert(cls.getMethod("glBufferData", classOf[Int], classOf[Int], classOf[Buffer], classOf[Int]) != null)
  }

  test("GL20Ops trait has framebuffer methods") {
    val cls = classOf[GL20Ops]
    assert(cls.getMethod("glGenFramebuffer") != null)
    assert(cls.getMethod("glDeleteFramebuffer", classOf[Int]) != null)
    assert(cls.getMethod("glBindFramebuffer", classOf[Int], classOf[Int]) != null)
    assert(cls.getMethod("glCheckFramebufferStatus", classOf[Int]) != null)
  }

  test("GL20Ops trait has uniform methods") {
    val cls = classOf[GL20Ops]
    assert(cls.getMethod("glUniform1f", classOf[Int], classOf[Float]) != null)
    assert(cls.getMethod("glUniform1i", classOf[Int], classOf[Int]) != null)
    assert(cls.getMethod("glUniform4f", classOf[Int], classOf[Float], classOf[Float], classOf[Float], classOf[Float]) != null)
    assert(cls.getMethod("glUniformMatrix4fv", classOf[Int], classOf[Int], classOf[Boolean], classOf[FloatBuffer]) != null)
    assert(cls.getMethod("glGetUniformLocation", classOf[Int], classOf[String]) != null)
  }

  test("GL20Ops trait has vertex attrib methods") {
    val cls = classOf[GL20Ops]
    assert(cls.getMethod("glEnableVertexAttribArray", classOf[Int]) != null)
    assert(cls.getMethod("glDisableVertexAttribArray", classOf[Int]) != null)
    assert(
      cls.getMethod("glVertexAttribPointer", classOf[Int], classOf[Int], classOf[Int], classOf[Boolean], classOf[Int], classOf[Buffer]) != null
    )
    assert(
      cls.getMethod("glVertexAttribPointer", classOf[Int], classOf[Int], classOf[Int], classOf[Boolean], classOf[Int], classOf[Int]) != null
    )
  }

  // ── GL30Ops ─────────────────────────────────────────────────────────

  test("GL30Ops extends GL20Ops") {
    assert(classOf[GL20Ops].isAssignableFrom(classOf[GL30Ops]))
  }

  test("GL30Ops trait has GL30-specific methods") {
    val cls = classOf[GL30Ops]
    assert(cls.getMethod("glReadBuffer", classOf[Int]) != null)
    assert(cls.getMethod("glBindVertexArray", classOf[Int]) != null)
    assert(cls.getMethod("glGenVertexArrays", classOf[Int], classOf[IntBuffer]) != null)
    assert(cls.getMethod("glDeleteVertexArrays", classOf[Int], classOf[IntBuffer]) != null)
    assert(cls.getMethod("glDrawArraysInstanced", classOf[Int], classOf[Int], classOf[Int], classOf[Int]) != null)
    assert(cls.getMethod("glDrawElementsInstanced", classOf[Int], classOf[Int], classOf[Int], classOf[Int], classOf[Int]) != null)
    assert(cls.getMethod("glBeginTransformFeedback", classOf[Int]) != null)
    assert(cls.getMethod("glEndTransformFeedback") != null)
    assert(cls.getMethod("glMapBufferRange", classOf[Int], classOf[Int], classOf[Int], classOf[Int]) != null)
    assert(cls.getMethod("glGetStringi", classOf[Int], classOf[Int]) != null)
  }

  test("GL30Ops has sampler methods") {
    val cls = classOf[GL30Ops]
    assert(cls.getMethod("glGenSamplers", classOf[Int], classOf[IntBuffer]) != null)
    assert(cls.getMethod("glDeleteSamplers", classOf[Int], classOf[IntBuffer]) != null)
    assert(cls.getMethod("glBindSampler", classOf[Int], classOf[Int]) != null)
    assert(cls.getMethod("glSamplerParameteri", classOf[Int], classOf[Int], classOf[Int]) != null)
  }

  test("GL30Ops has uniform block methods") {
    val cls = classOf[GL30Ops]
    assert(cls.getMethod("glGetUniformBlockIndex", classOf[Int], classOf[String]) != null)
    assert(cls.getMethod("glUniformBlockBinding", classOf[Int], classOf[Int], classOf[Int]) != null)
  }

  // ── Provider factory methods ───────────────────────────────────────

  test("AndroidPlatformProvider has GL factory methods") {
    val cls = classOf[AndroidPlatformProvider]
    assert(cls.getMethod("createGL20") != null)
    assert(cls.getMethod("createGL30") != null)
  }
}
