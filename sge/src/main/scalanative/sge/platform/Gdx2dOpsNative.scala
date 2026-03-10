// SGE Native Ops — Scala Native image decoder via Rust `image` crate
//
// Delegates to sge_native_ops Rust library for PNG/JPEG/BMP decoding.
// The Rust side uses the pure-Rust `image` crate (no C dependencies).
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: Scala Native @extern bindings to Rust C ABI for image decoding
//   Idiom: split packages
//   Audited: 2026-03-10

package sge
package platform

import java.nio.ByteBuffer
import scala.scalanative.unsafe.*

@link("sge_native_ops")
@extern
private object Gdx2dC {
  def sge_image_decode(data: Ptr[Byte], offset: CInt, len: CInt): Ptr[Byte] = extern
  def sge_image_free(result: Ptr[Byte]):                          Unit      = extern
  def sge_image_failure():                                        Ptr[Byte] = extern
}

private[platform] object Gdx2dOpsNative extends Gdx2dOps {

  @volatile private var _failureReason: String = "No error"

  override def decodeImage(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] = {
    if (data == null || len <= 0) {
      _failureReason = "Null data or non-positive length"
      return None
    }

    val resultPtr = Gdx2dC.sge_image_decode(data.at(0), offset, len)
    if (resultPtr == null) {
      val reasonPtr = Gdx2dC.sge_image_failure()
      _failureReason = if (reasonPtr != null) fromCString(reasonPtr) else "Unknown decode error"
      return None
    }

    // SgeImageResult layout: width(i32), height(i32), format(i32), pixels(ptr), pixel_size(i32)
    // On 64-bit: offsets are 0, 4, 8, 16 (pointer at offset 16 due to alignment), 24
    // Actually repr(C) with i32, i32, i32, *mut u8, i32:
    // offset 0: width (4 bytes)
    // offset 4: height (4 bytes)
    // offset 8: format (4 bytes)
    // offset 12: padding (4 bytes for 8-byte pointer alignment)
    // offset 16: pixels pointer (8 bytes on 64-bit)
    // offset 24: pixel_size (4 bytes)
    val width     = !(resultPtr + 0).asInstanceOf[Ptr[CInt]]
    val height    = !(resultPtr + 4).asInstanceOf[Ptr[CInt]]
    val format    = !(resultPtr + 8).asInstanceOf[Ptr[CInt]]
    val pixelsPtr = !(resultPtr + 16).asInstanceOf[Ptr[Ptr[Byte]]]
    val pixelSize = !(resultPtr + 24).asInstanceOf[Ptr[CInt]]

    if (pixelsPtr == null || pixelSize <= 0) {
      Gdx2dC.sge_image_free(resultPtr)
      _failureReason = "Decoded image has no pixel data"
      return None
    }

    // Copy pixel data into a direct ByteBuffer
    val buf = ByteBuffer.allocateDirect(pixelSize)
    var i   = 0
    while (i < pixelSize) {
      buf.put(i, !(pixelsPtr + i))
      i += 1
    }

    // Free the Rust-allocated result (including pixel data)
    Gdx2dC.sge_image_free(resultPtr)

    _failureReason = "No error"
    Some(Gdx2dOps.DecodeResult(width, height, format, buf))
  }

  override def failureReason: String = _failureReason
}
