// SGE Native Ops — Panama-based implementation of ETC1Ops
//
// Calls Rust C ABI functions using the PanamaProvider abstraction.
// Works on both Desktop JVM (JdkPanama) and Android (PanamaPortProvider).
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: Panama FFM delegation to Rust C ABI native lib
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-05

package sge
package platform

import java.lang.invoke.MethodHandle

private[platform] class ETC1OpsPanama(val p: PanamaProvider) extends ETC1Ops {
  import p.*

  // ─── Native library + linker setup ─────────────────────────────────────
  // Reuse the same library loaded by BufferOpsPanama

  private val linker: p.Linker = p.Linker.nativeLinker()

  private val lib: p.SymbolLookup = {
    val libName = System.mapLibraryName("sge_native_ops")
    val libPath = System.getProperty("java.library.path", "")
    val paths   = libPath.split(java.io.File.pathSeparator)
    val found   = paths.iterator
      .map(dir => java.nio.file.Path.of(dir, libName))
      .find(java.nio.file.Files.exists(_))
      .getOrElse(
        throw new UnsatisfiedLinkError(
          s"Cannot find $libName in java.library.path: $libPath"
        )
      )
    p.SymbolLookup.libraryLookup(found, p.Arena.global())
  }

  private def lookup(name: String): p.MemorySegment =
    lib.findOrThrow(name)

  // ─── Method handle cache ──────────────────────────────────────────────

  // etc1_get_encoded_data_size(width: u32, height: u32) -> u32
  private val hGetEncodedDataSize: MethodHandle = linker.downcallHandle(
    lookup("etc1_get_encoded_data_size"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_INT, p.JAVA_INT)
  )

  // etc1_pkm_format_header(header: *mut u8, width: u32, height: u32)
  private val hPkmFormatHeader: MethodHandle = linker.downcallHandle(
    lookup("etc1_pkm_format_header"),
    p.FunctionDescriptor.ofVoid(p.ADDRESS, p.JAVA_INT, p.JAVA_INT)
  )

  // etc1_pkm_get_width(header: *const u8) -> u32
  private val hPkmGetWidth: MethodHandle = linker.downcallHandle(
    lookup("etc1_pkm_get_width"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.ADDRESS)
  )

  // etc1_pkm_get_height(header: *const u8) -> u32
  private val hPkmGetHeight: MethodHandle = linker.downcallHandle(
    lookup("etc1_pkm_get_height"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.ADDRESS)
  )

  // etc1_pkm_is_valid(header: *const u8) -> i32
  private val hPkmIsValid: MethodHandle = linker.downcallHandle(
    lookup("etc1_pkm_is_valid"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.ADDRESS)
  )

  // etc1_decode_image(pIn, pOut, width, height, pixelSize, stride) -> i32
  private val hDecodeImage: MethodHandle = linker.downcallHandle(
    lookup("etc1_decode_image"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.ADDRESS, p.ADDRESS, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT)
  )

  // etc1_encode_image(pIn, width, height, pixelSize, stride, pOut) -> i32
  private val hEncodeImage: MethodHandle = linker.downcallHandle(
    lookup("etc1_encode_image"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.ADDRESS, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT, p.ADDRESS)
  )

  // ─── ETC1Ops implementation ───────────────────────────────────────────

  override def getCompressedDataSize(width: Int, height: Int): Int =
    hGetEncodedDataSize.invoke(width, height).asInstanceOf[Int]

  override def formatHeader(header: Array[Byte], offset: Int, width: Int, height: Int): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_BYTE, header.length.toLong)
      p.MemorySegment.copyFromBytes(header, 0, seg, 0L, header.length)
      val atOffset = seg.segSlice(offset.toLong)
      hPkmFormatHeader.invoke(atOffset, width, height)
      p.MemorySegment.copyToBytes(seg, 0L, header, 0, header.length)
    } finally arena.arenaClose()
  }

  override def getWidthPKM(header: Array[Byte], offset: Int): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_BYTE, header.length.toLong)
      p.MemorySegment.copyFromBytes(header, 0, seg, 0L, header.length)
      val atOffset = seg.segSlice(offset.toLong)
      hPkmGetWidth.invoke(atOffset).asInstanceOf[Int]
    } finally arena.arenaClose()
  }

  override def getHeightPKM(header: Array[Byte], offset: Int): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_BYTE, header.length.toLong)
      p.MemorySegment.copyFromBytes(header, 0, seg, 0L, header.length)
      val atOffset = seg.segSlice(offset.toLong)
      hPkmGetHeight.invoke(atOffset).asInstanceOf[Int]
    } finally arena.arenaClose()
  }

  override def isValidPKM(header: Array[Byte], offset: Int): Boolean = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_BYTE, header.length.toLong)
      p.MemorySegment.copyFromBytes(header, 0, seg, 0L, header.length)
      val atOffset = seg.segSlice(offset.toLong)
      val result   = hPkmIsValid.invoke(atOffset).asInstanceOf[Int]
      result != 0
    } finally arena.arenaClose()
  }

  override def decodeImage(
    compressedData:   Array[Byte],
    compressedOffset: Int,
    decodedData:      Array[Byte],
    decodedOffset:    Int,
    width:            Int,
    height:           Int,
    pixelSize:        Int
  ): Unit = {
    val stride = width * pixelSize
    val arena  = p.Arena.ofConfined()
    try {
      val compSeg = arena.allocateElems(p.JAVA_BYTE, compressedData.length.toLong)
      p.MemorySegment.copyFromBytes(compressedData, 0, compSeg, 0L, compressedData.length)
      val decSeg = arena.allocateElems(p.JAVA_BYTE, decodedData.length.toLong)
      p.MemorySegment.copyFromBytes(decodedData, 0, decSeg, 0L, decodedData.length)
      hDecodeImage.invoke(
        compSeg.segSlice(compressedOffset.toLong),
        decSeg.segSlice(decodedOffset.toLong),
        width,
        height,
        pixelSize,
        stride
      )
      p.MemorySegment.copyToBytes(decSeg, 0L, decodedData, 0, decodedData.length)
    } finally arena.arenaClose()
  }

  override def encodeImage(
    imageData: Array[Byte],
    offset:    Int,
    width:     Int,
    height:    Int,
    pixelSize: Int
  ): Array[Byte] = {
    val compressedSize = getCompressedDataSize(width, height)
    val stride         = width * pixelSize
    val arena          = p.Arena.ofConfined()
    try {
      val inSeg = arena.allocateElems(p.JAVA_BYTE, imageData.length.toLong)
      p.MemorySegment.copyFromBytes(imageData, 0, inSeg, 0L, imageData.length)
      val outSeg = arena.allocateElems(p.JAVA_BYTE, compressedSize.toLong)
      hEncodeImage.invoke(inSeg.segSlice(offset.toLong), width, height, pixelSize, stride, outSeg)
      val result = new Array[Byte](compressedSize)
      p.MemorySegment.copyToBytes(outSeg, 0L, result, 0, compressedSize)
      result
    } finally arena.arenaClose()
  }

  override def encodeImagePKM(
    imageData: Array[Byte],
    offset:    Int,
    width:     Int,
    height:    Int,
    pixelSize: Int
  ): Array[Byte] = {
    val compressedSize = getCompressedDataSize(width, height)
    val totalSize      = PKM_HEADER_SIZE + compressedSize
    val stride         = width * pixelSize
    val arena          = p.Arena.ofConfined()
    try {
      val resultSeg = arena.allocateElems(p.JAVA_BYTE, totalSize.toLong)
      // Write PKM header
      hPkmFormatHeader.invoke(resultSeg, width, height)
      // Encode image data after header
      val inSeg = arena.allocateElems(p.JAVA_BYTE, imageData.length.toLong)
      p.MemorySegment.copyFromBytes(imageData, 0, inSeg, 0L, imageData.length)
      hEncodeImage.invoke(
        inSeg.segSlice(offset.toLong),
        width,
        height,
        pixelSize,
        stride,
        resultSeg.segSlice(PKM_HEADER_SIZE.toLong)
      )
      val result = new Array[Byte](totalSize)
      p.MemorySegment.copyToBytes(resultSeg, 0L, result, 0, totalSize)
      result
    } finally arena.arenaClose()
  }
}
