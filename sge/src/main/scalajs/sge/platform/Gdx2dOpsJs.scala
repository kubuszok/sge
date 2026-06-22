// SGE Native Ops — Scala.js image decoder.
//
// Image bytes are decoded synchronously via the pure-Scala PngDecoderJs
// (ISS-533 / ISS-651). Assets are embedded at build time and served
// synchronously (multiarch.resources), so there is no async Canvas pre-decode
// phase. The reference-keyed decodedCache remains available for any caller that
// wants to register a pre-decoded result (e.g. a future Canvas-based path);
// when empty, decodeImage falls through to the synchronous PNG decoder.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: synchronous decode via PngDecoderJs; optional pre-decode cache
//   Idiom: split packages
//   Audited: 2026-03-10

package sge
package platform

import java.nio.ByteBuffer
import scala.scalajs.js

// private[sge] so a pre-decode producer (in sge.files) could call cacheDecodedImage
private[sge] object Gdx2dOpsJs extends Gdx2dOps {

  // Use native JS Map via js.Dynamic — supports object keys with reference equality.
  // Scala.js js.Map facade doesn't expose .set/.get, so we use the raw JS API.
  private val decodedCache: js.Dynamic = js.Dynamic.newInstance(js.Dynamic.global.Map)()

  /** Cache a pre-decoded image result, keyed by the raw byte array reference.
    *
    * @param rawBytes
    *   the raw encoded image bytes (same reference stored in binaryCache)
    * @param width
    *   decoded image width
    * @param height
    *   decoded image height
    * @param rgbaPixels
    *   RGBA pixel data from Canvas getImageData
    */
  private[sge] def cacheDecodedImage(rawBytes: Array[Byte], width: Int, height: Int, rgbaPixels: Array[Byte]): Unit = {
    val pixels = ByteBuffer.wrap(rgbaPixels)
    val result = Gdx2dOps.DecodeResult(width, height, 4, pixels) // 4 = GDX2D_FORMAT_RGBA8888
    decodedCache.set(rawBytes.asInstanceOf[js.Any], result.asInstanceOf[js.Any])
  }

  override def decodeImage(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] = {
    val cached = decodedCache.get(data.asInstanceOf[js.Any])
    if (!js.isUndefined(cached)) {
      val result = cached.asInstanceOf[Gdx2dOps.DecodeResult]
      // Reset buffer position for each read
      result.pixels.position(0)
      Some(result)
    } else {
      // Cache miss (the normal path now that assets are embedded and decoded
      // synchronously): decode via the pure-Scala decoders (ISS-533 / ISS-651).
      // Each decoder checks its own magic bytes first and returns None for a
      // non-matching format, so the chain is cheap and falls through cleanly to
      // the original "not decodable" failure path for unsupported inputs.
      PngDecoderJs.decode(data, offset, len).orElse(BmpDecoderJs.decode(data, offset, len)).orElse(GifDecoderJs.decode(data, offset, len)).orElse(JpegDecoderJs.decode(data, offset, len))
    }
  }

  override def failureReason: String =
    "Image is not a synchronously decodable PNG/BMP/GIF/baseline-JPEG on the Scala.js baseline"
}
