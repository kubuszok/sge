// Minimal java.util.zip.InflaterInputStream stub for Scala.js.
// The Scala.js linker needs this class to exist because PixmapIO.CIM.read()
// references it (for .cim format support). This code path is never reached
// at runtime on the browser — textures use the browser's native image decoder.
package java.util.zip

class InflaterInputStream(in: java.io.InputStream) extends java.io.InputStream {
  def read(): Int = throw new UnsupportedOperationException("InflaterInputStream is not supported on Scala.js")
}
