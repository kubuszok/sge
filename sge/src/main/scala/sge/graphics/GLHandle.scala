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

/** Opaque type for GL uniform locations, preventing accidental mixing with attribute locations or raw indices. */
opaque type UniformLocation = Int
object UniformLocation {
  def apply(raw: Int): UniformLocation = raw
  val notFound:        UniformLocation = -1

  given MkArray[UniformLocation] = utils.MkArray.mkInt.asInstanceOf[MkArray[UniformLocation]]

  extension (l: UniformLocation) {
    inline def toInt:                      Int             = l
    inline def +(offset: Int):             UniformLocation = l + offset
    inline def -(other:  UniformLocation): Int             = l - other
    inline def >=(rhs:   Int):             Boolean         = l >= rhs
    inline def <(rhs:    Int):             Boolean         = l < rhs
  }
}

/** Opaque type for GL attribute locations, preventing accidental mixing with uniform locations or raw indices. */
opaque type AttributeLocation = Int
object AttributeLocation {
  def apply(raw: Int): AttributeLocation = raw
  val notFound:        AttributeLocation = -1

  given MkArray[AttributeLocation] = utils.MkArray.mkInt.asInstanceOf[MkArray[AttributeLocation]]

  extension (l: AttributeLocation) {
    inline def toInt: Int = l
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
