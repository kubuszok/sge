/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.utils.MkArray

/** Opaque types for GL object handles, preventing accidental mixing of texture handles with buffer handles, shader handles, etc.
  */
opaque type TextureHandle = Int
object TextureHandle {
  def apply(raw: Int): TextureHandle = raw
  val none:            TextureHandle = 0

  given MkArray[TextureHandle] = utils.MkArray.mkInt.asInstanceOf[MkArray[TextureHandle]]

  extension (h: TextureHandle) {
    inline def toInt: Int = h
  }
}

opaque type BufferHandle = Int
object BufferHandle {
  def apply(raw: Int): BufferHandle = raw
  val none:            BufferHandle = 0

  given MkArray[BufferHandle] = utils.MkArray.mkInt.asInstanceOf[MkArray[BufferHandle]]

  extension (h: BufferHandle) {
    inline def toInt: Int = h
  }
}

opaque type ShaderHandle = Int
object ShaderHandle {
  def apply(raw: Int): ShaderHandle = raw
  val none:            ShaderHandle = 0

  given MkArray[ShaderHandle] = utils.MkArray.mkInt.asInstanceOf[MkArray[ShaderHandle]]

  extension (h: ShaderHandle) {
    inline def toInt: Int = h
  }
}

opaque type ProgramHandle = Int
object ProgramHandle {
  def apply(raw: Int): ProgramHandle = raw
  val none:            ProgramHandle = 0

  given MkArray[ProgramHandle] = utils.MkArray.mkInt.asInstanceOf[MkArray[ProgramHandle]]

  extension (h: ProgramHandle) {
    inline def toInt: Int = h
  }
}

opaque type FramebufferHandle = Int
object FramebufferHandle {
  def apply(raw: Int): FramebufferHandle = raw
  val none:            FramebufferHandle = 0

  given MkArray[FramebufferHandle] = utils.MkArray.mkInt.asInstanceOf[MkArray[FramebufferHandle]]

  extension (h: FramebufferHandle) {
    inline def toInt: Int = h
  }
}

opaque type RenderbufferHandle = Int
object RenderbufferHandle {
  def apply(raw: Int): RenderbufferHandle = raw
  val none:            RenderbufferHandle = 0

  given MkArray[RenderbufferHandle] = utils.MkArray.mkInt.asInstanceOf[MkArray[RenderbufferHandle]]

  extension (h: RenderbufferHandle) {
    inline def toInt: Int = h
  }
}

/** Typed extension methods on GL20 for working with handle opaque types. */
object GLHandleOps {
  extension (gl: GL20) {
    def genTexture():                                                     TextureHandle      = TextureHandle(gl.glGenTexture())
    def deleteTexture(handle:      TextureHandle):                        Unit               = gl.glDeleteTexture(handle.toInt)
    def bindTexture(target:        TextureTarget, handle: TextureHandle): Unit               = gl.glBindTexture(target, handle.toInt)
    def genBuffer():                                                      BufferHandle       = BufferHandle(gl.glGenBuffer())
    def deleteBuffer(handle:       BufferHandle):                         Unit               = gl.glDeleteBuffer(handle.toInt)
    def genFramebuffer():                                                 FramebufferHandle  = FramebufferHandle(gl.glGenFramebuffer())
    def deleteFramebuffer(handle:  FramebufferHandle):                    Unit               = gl.glDeleteFramebuffer(handle.toInt)
    def genRenderbuffer():                                                RenderbufferHandle = RenderbufferHandle(gl.glGenRenderbuffer())
    def deleteRenderbuffer(handle: RenderbufferHandle):                   Unit               = gl.glDeleteRenderbuffer(handle.toInt)
  }
}
