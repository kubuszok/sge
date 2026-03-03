// SGE Native Ops — JVM implementation of ETC1Ops via Rust JNI
//
// Delegates all methods to ETC1Bridge, which loads libsge_native_ops via JNI.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: thin JNI delegation to ETC1Bridge → Rust native lib
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-03

package sge
package platform

import ETC1Bridge as ETC1Jni

private[platform] object ETC1OpsJvm extends ETC1Ops {

  override def getCompressedDataSize(width: Int, height: Int): Int =
    ETC1Jni.getCompressedDataSize(width, height)

  override def formatHeader(header: Array[Byte], offset: Int, width: Int, height: Int): Unit =
    ETC1Jni.formatHeader(header, offset, width, height)

  override def getWidthPKM(header: Array[Byte], offset: Int): Int =
    ETC1Jni.getWidthPKM(header, offset)

  override def getHeightPKM(header: Array[Byte], offset: Int): Int =
    ETC1Jni.getHeightPKM(header, offset)

  override def isValidPKM(header: Array[Byte], offset: Int): Boolean =
    ETC1Jni.isValidPKM(header, offset)

  override def decodeImage(
    compressedData:   Array[Byte],
    compressedOffset: Int,
    decodedData:      Array[Byte],
    decodedOffset:    Int,
    width:            Int,
    height:           Int,
    pixelSize:        Int
  ): Unit =
    ETC1Jni.decodeImage(compressedData, compressedOffset, decodedData, decodedOffset, width, height, pixelSize)

  override def encodeImage(
    imageData: Array[Byte],
    offset:    Int,
    width:     Int,
    height:    Int,
    pixelSize: Int
  ): Array[Byte] =
    ETC1Jni.encodeImage(imageData, offset, width, height, pixelSize)

  override def encodeImagePKM(
    imageData: Array[Byte],
    offset:    Int,
    width:     Int,
    height:    Int,
    pixelSize: Int
  ): Array[Byte] =
    ETC1Jni.encodeImagePKM(imageData, offset, width, height, pixelSize)
}
