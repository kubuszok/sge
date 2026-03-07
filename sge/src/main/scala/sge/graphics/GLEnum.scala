/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

/** Opaque types for OpenGL enum parameters, preventing accidental mixing of unrelated GL constants.
  *
  * Each opaque type wraps `Int` and provides named constants matching the GL spec. Raw GL20/GL30 companion constants remain as `Int` for backward compatibility; prefer the typed companion constants
  * (e.g. `ShaderType.Vertex` instead of `GL20.GL_VERTEX_SHADER`).
  */

// --- ShaderType ---

opaque type ShaderType = Int
object ShaderType {
  def apply(raw: Int): ShaderType = raw

  val Vertex:   ShaderType = 0x8b31 // GL_VERTEX_SHADER
  val Fragment: ShaderType = 0x8b30 // GL_FRAGMENT_SHADER

  extension (s: ShaderType) {
    inline def toInt: Int = s
  }
}

// --- StencilOp ---

opaque type StencilOp = Int
object StencilOp {
  def apply(raw: Int): StencilOp = raw

  val Zero:     StencilOp = 0 // GL_ZERO
  val Keep:     StencilOp = 0x1e00 // GL_KEEP
  val Replace:  StencilOp = 0x1e01 // GL_REPLACE
  val Incr:     StencilOp = 0x1e02 // GL_INCR
  val Decr:     StencilOp = 0x1e03 // GL_DECR
  val Invert:   StencilOp = 0x150a // GL_INVERT
  val IncrWrap: StencilOp = 0x8507 // GL_INCR_WRAP
  val DecrWrap: StencilOp = 0x8508 // GL_DECR_WRAP

  extension (s: StencilOp) {
    inline def toInt: Int = s
  }
}

// --- CompareFunc ---

opaque type CompareFunc = Int
object CompareFunc {
  def apply(raw: Int): CompareFunc = raw

  val Never:    CompareFunc = 0x0200 // GL_NEVER
  val Less:     CompareFunc = 0x0201 // GL_LESS
  val Equal:    CompareFunc = 0x0202 // GL_EQUAL
  val Lequal:   CompareFunc = 0x0203 // GL_LEQUAL
  val Greater:  CompareFunc = 0x0204 // GL_GREATER
  val Notequal: CompareFunc = 0x0205 // GL_NOTEQUAL
  val Gequal:   CompareFunc = 0x0206 // GL_GEQUAL
  val Always:   CompareFunc = 0x0207 // GL_ALWAYS

  extension (f: CompareFunc) {
    inline def toInt: Int = f
  }
}

// --- BlendFactor ---

opaque type BlendFactor = Int
object BlendFactor {
  def apply(raw: Int): BlendFactor = raw

  val Zero:                  BlendFactor = 0 // GL_ZERO
  val One:                   BlendFactor = 1 // GL_ONE
  val SrcColor:              BlendFactor = 0x0300 // GL_SRC_COLOR
  val OneMinusSrcColor:      BlendFactor = 0x0301 // GL_ONE_MINUS_SRC_COLOR
  val SrcAlpha:              BlendFactor = 0x0302 // GL_SRC_ALPHA
  val OneMinusSrcAlpha:      BlendFactor = 0x0303 // GL_ONE_MINUS_SRC_ALPHA
  val DstAlpha:              BlendFactor = 0x0304 // GL_DST_ALPHA
  val OneMinusDstAlpha:      BlendFactor = 0x0305 // GL_ONE_MINUS_DST_ALPHA
  val DstColor:              BlendFactor = 0x0306 // GL_DST_COLOR
  val OneMinusDstColor:      BlendFactor = 0x0307 // GL_ONE_MINUS_DST_COLOR
  val SrcAlphaSaturate:      BlendFactor = 0x0308 // GL_SRC_ALPHA_SATURATE
  val ConstantColor:         BlendFactor = 0x8001 // GL_CONSTANT_COLOR
  val OneMinusConstantColor: BlendFactor = 0x8002 // GL_ONE_MINUS_CONSTANT_COLOR
  val ConstantAlpha:         BlendFactor = 0x8003 // GL_CONSTANT_ALPHA
  val OneMinusConstantAlpha: BlendFactor = 0x8004 // GL_ONE_MINUS_CONSTANT_ALPHA

  extension (f: BlendFactor) {
    inline def toInt: Int = f
  }
}

// --- BlendEquation ---

opaque type BlendEquation = Int
object BlendEquation {
  def apply(raw: Int): BlendEquation = raw

  val FuncAdd:             BlendEquation = 0x8006 // GL_FUNC_ADD
  val Min:                 BlendEquation = 0x8007 // GL_MIN (GL30+)
  val Max:                 BlendEquation = 0x8008 // GL_MAX (GL30+)
  val FuncSubtract:        BlendEquation = 0x800a // GL_FUNC_SUBTRACT
  val FuncReverseSubtract: BlendEquation = 0x800b // GL_FUNC_REVERSE_SUBTRACT

  extension (e: BlendEquation) {
    inline def toInt: Int = e
  }
}

// --- PrimitiveMode ---

opaque type PrimitiveMode = Int
object PrimitiveMode {
  def apply(raw: Int): PrimitiveMode = raw

  val Points:        PrimitiveMode = 0x0000 // GL_POINTS
  val Lines:         PrimitiveMode = 0x0001 // GL_LINES
  val LineLoop:      PrimitiveMode = 0x0002 // GL_LINE_LOOP
  val LineStrip:     PrimitiveMode = 0x0003 // GL_LINE_STRIP
  val Triangles:     PrimitiveMode = 0x0004 // GL_TRIANGLES
  val TriangleStrip: PrimitiveMode = 0x0005 // GL_TRIANGLE_STRIP
  val TriangleFan:   PrimitiveMode = 0x0006 // GL_TRIANGLE_FAN

  extension (m: PrimitiveMode) {
    inline def toInt: Int = m
  }
}

// --- BufferTarget ---

opaque type BufferTarget = Int
object BufferTarget {
  def apply(raw: Int): BufferTarget = raw

  val ArrayBuffer:             BufferTarget = 0x8892 // GL_ARRAY_BUFFER
  val ElementArrayBuffer:      BufferTarget = 0x8893 // GL_ELEMENT_ARRAY_BUFFER
  val PixelPackBuffer:         BufferTarget = 0x88eb // GL_PIXEL_PACK_BUFFER (GL30+)
  val PixelUnpackBuffer:       BufferTarget = 0x88ec // GL_PIXEL_UNPACK_BUFFER (GL30+)
  val CopyReadBuffer:          BufferTarget = 0x8f36 // GL_COPY_READ_BUFFER (GL30+)
  val CopyWriteBuffer:         BufferTarget = 0x8f37 // GL_COPY_WRITE_BUFFER (GL30+)
  val TransformFeedbackBuffer: BufferTarget = 0x8c8e // GL_TRANSFORM_FEEDBACK_BUFFER (GL30+)
  val UniformBuffer:           BufferTarget = 0x8a11 // GL_UNIFORM_BUFFER (GL30+)

  extension (t: BufferTarget) {
    inline def toInt: Int = t
  }
}

// --- BufferUsage ---

opaque type BufferUsage = Int
object BufferUsage {
  def apply(raw: Int): BufferUsage = raw

  val StreamDraw:  BufferUsage = 0x88e0 // GL_STREAM_DRAW
  val StreamRead:  BufferUsage = 0x88e1 // GL_STREAM_READ (GL30+)
  val StreamCopy:  BufferUsage = 0x88e2 // GL_STREAM_COPY (GL30+)
  val StaticDraw:  BufferUsage = 0x88e4 // GL_STATIC_DRAW
  val StaticRead:  BufferUsage = 0x88e5 // GL_STATIC_READ (GL30+)
  val StaticCopy:  BufferUsage = 0x88e6 // GL_STATIC_COPY (GL30+)
  val DynamicDraw: BufferUsage = 0x88e8 // GL_DYNAMIC_DRAW
  val DynamicRead: BufferUsage = 0x88e9 // GL_DYNAMIC_READ (GL30+)
  val DynamicCopy: BufferUsage = 0x88ea // GL_DYNAMIC_COPY (GL30+)

  extension (u: BufferUsage) {
    inline def toInt: Int = u
  }
}

// --- PixelFormat ---

opaque type PixelFormat = Int
object PixelFormat {
  def apply(raw: Int): PixelFormat = raw

  val DepthComponent: PixelFormat = 0x1902 // GL_DEPTH_COMPONENT
  val Alpha:          PixelFormat = 0x1906 // GL_ALPHA
  val RGB:            PixelFormat = 0x1907 // GL_RGB
  val RGBA:           PixelFormat = 0x1908 // GL_RGBA
  val Luminance:      PixelFormat = 0x1909 // GL_LUMINANCE
  val LuminanceAlpha: PixelFormat = 0x190a // GL_LUMINANCE_ALPHA
  val Red:            PixelFormat = 0x1903 // GL_RED (GL30+)
  val RG:             PixelFormat = 0x8227 // GL_RG (GL30+)
  val RedInteger:     PixelFormat = 0x8d94 // GL_RED_INTEGER (GL30+)
  val RGInteger:      PixelFormat = 0x8228 // GL_RG_INTEGER (GL30+)
  val RGBInteger:     PixelFormat = 0x8d98 // GL_RGB_INTEGER (GL30+)
  val RGBAInteger:    PixelFormat = 0x8d99 // GL_RGBA_INTEGER (GL30+)
  val DepthStencil:   PixelFormat = 0x84f9 // GL_DEPTH_STENCIL (GL30+)

  extension (f: PixelFormat) {
    inline def toInt: Int = f
  }
}

// --- DataType ---

opaque type DataType = Int
object DataType {
  def apply(raw: Int): DataType = raw

  val Byte:              DataType = 0x1400 // GL_BYTE
  val UnsignedByte:      DataType = 0x1401 // GL_UNSIGNED_BYTE
  val Short:             DataType = 0x1402 // GL_SHORT
  val UnsignedShort:     DataType = 0x1403 // GL_UNSIGNED_SHORT
  val Int:               DataType = 0x1404 // GL_INT
  val UnsignedInt:       DataType = 0x1405 // GL_UNSIGNED_INT
  val Float:             DataType = 0x1406 // GL_FLOAT
  val Fixed:             DataType = 0x140c // GL_FIXED
  val UnsignedShort4444: DataType = 0x8033 // GL_UNSIGNED_SHORT_4_4_4_4
  val UnsignedShort5551: DataType = 0x8034 // GL_UNSIGNED_SHORT_5_5_5_1
  val UnsignedShort565:  DataType = 0x8363 // GL_UNSIGNED_SHORT_5_6_5
  val HalfFloat:         DataType = 0x140b // GL_HALF_FLOAT (GL30+)

  extension (t: DataType) {
    inline def toInt: Int = t
  }
}

// --- ClearMask ---

opaque type ClearMask = Int
object ClearMask {
  def apply(raw: Int): ClearMask = raw

  val DepthBufferBit:   ClearMask = 0x00000100 // GL_DEPTH_BUFFER_BIT
  val StencilBufferBit: ClearMask = 0x00000400 // GL_STENCIL_BUFFER_BIT
  val ColorBufferBit:   ClearMask = 0x00004000 // GL_COLOR_BUFFER_BIT

  extension (m: ClearMask) {
    inline def toInt:        Int       = m
    def |(other: ClearMask): ClearMask = m | other
  }
}

// --- CullFace ---

opaque type CullFace = Int
object CullFace {
  def apply(raw: Int): CullFace = raw

  val Front:        CullFace = 0x0404 // GL_FRONT
  val Back:         CullFace = 0x0405 // GL_BACK
  val FrontAndBack: CullFace = 0x0408 // GL_FRONT_AND_BACK

  extension (f: CullFace) {
    inline def toInt: Int = f
  }
}

// --- EnableCap ---

opaque type EnableCap = Int
object EnableCap {
  def apply(raw: Int): EnableCap = raw

  val CullFace:               EnableCap = 0x0b44 // GL_CULL_FACE
  val Blend:                  EnableCap = 0x0be2 // GL_BLEND
  val Dither:                 EnableCap = 0x0bd0 // GL_DITHER
  val StencilTest:            EnableCap = 0x0b90 // GL_STENCIL_TEST
  val DepthTest:              EnableCap = 0x0b71 // GL_DEPTH_TEST
  val ScissorTest:            EnableCap = 0x0c11 // GL_SCISSOR_TEST
  val PolygonOffsetFill:      EnableCap = 0x8037 // GL_POLYGON_OFFSET_FILL
  val SampleAlphaToCoverage:  EnableCap = 0x809e // GL_SAMPLE_ALPHA_TO_COVERAGE
  val SampleCoverage:         EnableCap = 0x80a0 // GL_SAMPLE_COVERAGE
  val VertexProgramPointSize: EnableCap = 0x8642 // GL_VERTEX_PROGRAM_POINT_SIZE

  extension (c: EnableCap) {
    inline def toInt: Int = c
  }
}

// --- TextureTarget ---

opaque type TextureTarget = Int
object TextureTarget {
  def apply(raw: Int): TextureTarget = raw

  val Texture2D:               TextureTarget = 0x0de1 // GL_TEXTURE_2D
  val TextureCubeMap:          TextureTarget = 0x8513 // GL_TEXTURE_CUBE_MAP
  val TextureCubeMapPositiveX: TextureTarget = 0x8515 // GL_TEXTURE_CUBE_MAP_POSITIVE_X
  val TextureCubeMapNegativeX: TextureTarget = 0x8516 // GL_TEXTURE_CUBE_MAP_NEGATIVE_X
  val TextureCubeMapPositiveY: TextureTarget = 0x8517 // GL_TEXTURE_CUBE_MAP_POSITIVE_Y
  val TextureCubeMapNegativeY: TextureTarget = 0x8518 // GL_TEXTURE_CUBE_MAP_NEGATIVE_Y
  val TextureCubeMapPositiveZ: TextureTarget = 0x8519 // GL_TEXTURE_CUBE_MAP_POSITIVE_Z
  val TextureCubeMapNegativeZ: TextureTarget = 0x851a // GL_TEXTURE_CUBE_MAP_NEGATIVE_Z
  val Texture3D:               TextureTarget = 0x806f // GL_TEXTURE_3D (GL30+)
  val Texture2DArray:          TextureTarget = 0x8c1a // GL_TEXTURE_2D_ARRAY (GL30+)

  extension (t: TextureTarget) {
    inline def toInt: Int = t
  }
}
