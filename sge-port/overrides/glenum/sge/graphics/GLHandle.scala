/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 131
 * Covenant-baseline-methods: AttributeLocation,BufferHandle,FramebufferHandle,GLHandleOps,ProgramHandle,RenderbufferHandle,ShaderHandle,TextureHandle,UniformLocation,apply,bindTexture,deleteBuffer,deleteFramebuffer,deleteRenderbuffer,deleteTexture,genBuffer,genFramebuffer,genRenderbuffer,genTexture,none,notFound,toInt
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics

import lowlevel.MkArray

/** Opaque types for GL object handles, preventing accidental mixing of texture handles with buffer handles, shader handles, etc.
  */
opaque type TextureHandle = Int
object TextureHandle {
  def apply(raw: Int): TextureHandle = raw
  val none:            TextureHandle = 0

  given MkArray.OfInts[TextureHandle] = MkArray.ofIntAs[TextureHandle]

  extension (h: TextureHandle) {
    inline def toInt: Int = h
  }
}

opaque type BufferHandle = Int
object BufferHandle {
  def apply(raw: Int): BufferHandle = raw
  val none:            BufferHandle = 0

  given MkArray.OfInts[BufferHandle] = MkArray.ofIntAs[BufferHandle]

  extension (h: BufferHandle) {
    inline def toInt: Int = h
  }
}

opaque type ShaderHandle = Int
object ShaderHandle {
  def apply(raw: Int): ShaderHandle = raw
  val none:            ShaderHandle = 0

  given MkArray.OfInts[ShaderHandle] = MkArray.ofIntAs[ShaderHandle]

  extension (h: ShaderHandle) {
    inline def toInt: Int = h
  }
}

opaque type ProgramHandle = Int
object ProgramHandle {
  def apply(raw: Int): ProgramHandle = raw
  val none:            ProgramHandle = 0

  given MkArray.OfInts[ProgramHandle] = MkArray.ofIntAs[ProgramHandle]

  extension (h: ProgramHandle) {
    inline def toInt: Int = h
  }
}

opaque type FramebufferHandle = Int
object FramebufferHandle {
  def apply(raw: Int): FramebufferHandle = raw
  val none:            FramebufferHandle = 0

  given MkArray.OfInts[FramebufferHandle] = MkArray.ofIntAs[FramebufferHandle]

  extension (h: FramebufferHandle) {
    inline def toInt: Int = h
  }
}

opaque type RenderbufferHandle = Int
object RenderbufferHandle {
  def apply(raw: Int): RenderbufferHandle = raw
  val none:            RenderbufferHandle = 0

  given MkArray.OfInts[RenderbufferHandle] = MkArray.ofIntAs[RenderbufferHandle]

  extension (h: RenderbufferHandle) {
    inline def toInt: Int = h
  }
}

/** Opaque type for GL uniform locations, preventing accidental mixing with attribute locations or raw indices. */
opaque type UniformLocation = Int
object UniformLocation {
  def apply(raw: Int): UniformLocation = raw
  val notFound:        UniformLocation = -1

  given MkArray.OfInts[UniformLocation] = MkArray.ofIntAs[UniformLocation]

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

  given MkArray.OfInts[AttributeLocation] = MkArray.ofIntAs[AttributeLocation]

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
