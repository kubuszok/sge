// SGE Native Ops — JVM implementation of ETC1Ops via Panama FFM
//
// Calls Rust C ABI functions directly using java.lang.foreign (Project Panama).
// Replaces the previous JNI approach (ETC1Bridge.java + jni_bridge.rs).
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: Panama FFM delegation to Rust C ABI native lib
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-05

package sge
package platform

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle

private[platform] object ETC1OpsJvm extends ETC1Ops {

  // ─── Native library + linker setup ─────────────────────────────────────
  // Reuse the same library loaded by BufferOpsJvm

  private val linker: Linker = Linker.nativeLinker()

  private val lib: SymbolLookup = {
    val libName = System.mapLibraryName("sge_native_ops")
    val libPath = System.getProperty("java.library.path", "")
    val paths   = libPath.split(java.io.File.pathSeparator)
    val found   = paths.iterator
      .map(p => java.nio.file.Path.of(p, libName))
      .find(java.nio.file.Files.exists(_))
      .getOrElse(
        throw new UnsatisfiedLinkError(
          s"Cannot find $libName in java.library.path: $libPath"
        )
      )
    SymbolLookup.libraryLookup(found, Arena.global())
  }

  private def lookup(name: String): MemorySegment =
    lib.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"Symbol not found: $name"))

  // ─── Method handle cache ──────────────────────────────────────────────

  // etc1_get_encoded_data_size(width: u32, height: u32) -> u32
  private val hGetEncodedDataSize: MethodHandle = linker.downcallHandle(
    lookup("etc1_get_encoded_data_size"),
    FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT)
  )

  // etc1_pkm_format_header(header: *mut u8, width: u32, height: u32)
  private val hPkmFormatHeader: MethodHandle = linker.downcallHandle(
    lookup("etc1_pkm_format_header"),
    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT)
  )

  // etc1_pkm_get_width(header: *const u8) -> u32
  private val hPkmGetWidth: MethodHandle = linker.downcallHandle(
    lookup("etc1_pkm_get_width"),
    FunctionDescriptor.of(JAVA_INT, ADDRESS)
  )

  // etc1_pkm_get_height(header: *const u8) -> u32
  private val hPkmGetHeight: MethodHandle = linker.downcallHandle(
    lookup("etc1_pkm_get_height"),
    FunctionDescriptor.of(JAVA_INT, ADDRESS)
  )

  // etc1_pkm_is_valid(header: *const u8) -> i32
  private val hPkmIsValid: MethodHandle = linker.downcallHandle(
    lookup("etc1_pkm_is_valid"),
    FunctionDescriptor.of(JAVA_INT, ADDRESS)
  )

  // etc1_decode_image(pIn, pOut, width, height, pixelSize, stride) -> i32
  private val hDecodeImage: MethodHandle = linker.downcallHandle(
    lookup("etc1_decode_image"),
    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT)
  )

  // etc1_encode_image(pIn, width, height, pixelSize, stride, pOut) -> i32
  private val hEncodeImage: MethodHandle = linker.downcallHandle(
    lookup("etc1_encode_image"),
    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS)
  )

  // ─── ETC1Ops implementation ───────────────────────────────────────────

  override def getCompressedDataSize(width: Int, height: Int): Int =
    hGetEncodedDataSize.invoke(width, height).asInstanceOf[Int]

  override def formatHeader(header: Array[Byte], offset: Int, width: Int, height: Int): Unit = {
    val arena = Arena.ofConfined()
    try {
      val seg = arena.allocate(JAVA_BYTE, header.length.toLong)
      MemorySegment.copy(header, 0, seg, JAVA_BYTE, 0L, header.length)
      val atOffset = seg.asSlice(offset.toLong)
      hPkmFormatHeader.invoke(atOffset, width, height)
      MemorySegment.copy(seg, JAVA_BYTE, 0L, header, 0, header.length)
    } finally arena.close()
  }

  override def getWidthPKM(header: Array[Byte], offset: Int): Int = {
    val arena = Arena.ofConfined()
    try {
      val seg = arena.allocate(JAVA_BYTE, header.length.toLong)
      MemorySegment.copy(header, 0, seg, JAVA_BYTE, 0L, header.length)
      val atOffset = seg.asSlice(offset.toLong)
      hPkmGetWidth.invoke(atOffset).asInstanceOf[Int]
    } finally arena.close()
  }

  override def getHeightPKM(header: Array[Byte], offset: Int): Int = {
    val arena = Arena.ofConfined()
    try {
      val seg = arena.allocate(JAVA_BYTE, header.length.toLong)
      MemorySegment.copy(header, 0, seg, JAVA_BYTE, 0L, header.length)
      val atOffset = seg.asSlice(offset.toLong)
      hPkmGetHeight.invoke(atOffset).asInstanceOf[Int]
    } finally arena.close()
  }

  override def isValidPKM(header: Array[Byte], offset: Int): Boolean = {
    val arena = Arena.ofConfined()
    try {
      val seg = arena.allocate(JAVA_BYTE, header.length.toLong)
      MemorySegment.copy(header, 0, seg, JAVA_BYTE, 0L, header.length)
      val atOffset = seg.asSlice(offset.toLong)
      val result   = hPkmIsValid.invoke(atOffset).asInstanceOf[Int]
      result != 0
    } finally arena.close()
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
    val arena  = Arena.ofConfined()
    try {
      val compSeg = arena.allocate(JAVA_BYTE, compressedData.length.toLong)
      MemorySegment.copy(compressedData, 0, compSeg, JAVA_BYTE, 0L, compressedData.length)
      val decSeg = arena.allocate(JAVA_BYTE, decodedData.length.toLong)
      MemorySegment.copy(decodedData, 0, decSeg, JAVA_BYTE, 0L, decodedData.length)
      hDecodeImage.invoke(
        compSeg.asSlice(compressedOffset.toLong),
        decSeg.asSlice(decodedOffset.toLong),
        width,
        height,
        pixelSize,
        stride
      )
      MemorySegment.copy(decSeg, JAVA_BYTE, 0L, decodedData, 0, decodedData.length)
    } finally arena.close()
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
    val arena          = Arena.ofConfined()
    try {
      val inSeg = arena.allocate(JAVA_BYTE, imageData.length.toLong)
      MemorySegment.copy(imageData, 0, inSeg, JAVA_BYTE, 0L, imageData.length)
      val outSeg = arena.allocate(JAVA_BYTE, compressedSize.toLong)
      hEncodeImage.invoke(inSeg.asSlice(offset.toLong), width, height, pixelSize, stride, outSeg)
      val result = new Array[Byte](compressedSize)
      MemorySegment.copy(outSeg, JAVA_BYTE, 0L, result, 0, compressedSize)
      result
    } finally arena.close()
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
    val arena          = Arena.ofConfined()
    try {
      val resultSeg = arena.allocate(JAVA_BYTE, totalSize.toLong)
      // Write PKM header
      hPkmFormatHeader.invoke(resultSeg, width, height)
      // Encode image data after header
      val inSeg = arena.allocate(JAVA_BYTE, imageData.length.toLong)
      MemorySegment.copy(imageData, 0, inSeg, JAVA_BYTE, 0L, imageData.length)
      hEncodeImage.invoke(
        inSeg.asSlice(offset.toLong),
        width,
        height,
        pixelSize,
        stride,
        resultSeg.asSlice(PKM_HEADER_SIZE.toLong)
      )
      val result = new Array[Byte](totalSize)
      MemorySegment.copy(resultSeg, JAVA_BYTE, 0L, result, 0, totalSize)
      result
    } finally arena.close()
  }
}
