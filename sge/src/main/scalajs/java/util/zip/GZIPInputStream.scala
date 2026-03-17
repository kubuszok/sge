// Minimal java.util.zip.GZIPInputStream stub for Scala.js.
// The Scala.js linker needs this class to exist because ETC1.loadCompressedData()
// and KTXTextureData reference it. These compressed-texture code paths are never
// reached at runtime on the browser — the browser uses its own image decoding.
package java.util.zip

class GZIPInputStream(in: java.io.InputStream, size: Int) extends InflaterInputStream(in) {
  def this(in: java.io.InputStream) = this(in, 512)
}
