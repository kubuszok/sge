// SGE Native Ops — Scala.js image decoder using browser Canvas API
//
// Images are pre-decoded during BrowserAssetLoader's async preload phase
// (HTMLImageElement + Canvas 2D getImageData), then served synchronously
// from a cache keyed by the byte array reference.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: pre-decode during async preload, serve synchronously from cache
//   Idiom: split packages
//   Audited: 2026-03-10

package sge
package platform

import java.nio.ByteBuffer
import scala.scalajs.js

// private[sge] so BrowserAssetLoader (in sge.files) can call cacheDecodedImage
private[sge] object Gdx2dOpsJs extends Gdx2dOps {

  // Use native JS Map via js.Dynamic — supports object keys with reference equality.
  // Scala.js js.Map facade doesn't expose .set/.get, so we use the raw JS API.
  private val decodedCache: js.Dynamic = js.Dynamic.newInstance(js.Dynamic.global.Map)()

  /** Cache a pre-decoded image result. Called from BrowserAssetLoader during preload.
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
      // Cache miss: the bytes were not preloaded through the browser image
      // pipeline (e.g. a Pixmap built synchronously from bytes PixmapIO.writePNG
      // just produced, or headless Node code). Fall back to the synchronous
      // pure-Scala PNG decoder (ISS-533 / ISS-651). Non-PNG inputs return None,
      // preserving the original "not pre-decoded" failure path.
      PngDecoderJs.decode(data, offset, len)
    }
  }

  override def failureReason: String =
    "Image not pre-decoded and not a synchronously decodable PNG — ensure BrowserAssetLoader preloads images before use"
}
