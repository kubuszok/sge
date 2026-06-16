// SGE — RED regression test for ISS-543 (Scala.js / browser only)
//
// WebGL30.copyUnsigned uses a FIXED scratch buffer:
//   private val uIntBuffer = new Uint32Array(2000 * 6)   // 12000 elements
// and never grows it. The GWT original (GwtGL30.copyUnsigned, lines 66-76)
// calls ensureCapacity(buffer) first, which reallocates the scratch buffer to
// buffer.remaining() whenever the input has more than 12000 elements
// (GwtGL20.ensureCapacity(IntBuffer), lines 170-174).
//
// Without that growth, when an unsigned-int uniform array has MORE than 12000
// elements, copyUnsigned writes past the typed array's end. JS typed-array
// out-of-bounds writes are SILENT NO-OPS, and `uIntBuffer.subarray(0, remaining)`
// clamps the view to the source length (12000), so:
//   - the recorded array length is 12000, not the requested element count, and
//   - every value at index >= 12000 is dropped entirely.
//
// This suite drives copyUnsigned through the public glUniform1uiv and pins that
// ALL element values are forwarded. It FAILS against current code (recorded
// length == 12000, tail values absent) until ensureCapacity-style growth is
// restored.

package sge
package graphics

import java.nio.IntBuffer
import munit.FunSuite
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint32Array

class WebGL30UnsignedUniformOverflowRedSuite extends FunSuite {

  // Mock WebGL2RenderingContext: records the typed array passed to uniform1uiv,
  // which is exactly the Uint32Array produced by copyUnsigned.
  final private class GlRecorder {
    var recorded: Uint32Array = new Uint32Array(0)

    val dyn: js.Dynamic = js.Dynamic.literal(
      // Init call made by the WebGL20 constructor (UNPACK_PREMULTIPLY_ALPHA_WEBGL).
      pixelStorei = ((_: Int, _: Int) => ()): js.Function2[Int, Int, Unit],
      // program / uniform plumbing so getUniformLocation(location) resolves to a
      // non-null handle without touching real WebGL.
      createProgram = (() => js.Dynamic.literal(tag = "program")):                           js.Function0[js.Dynamic],
      useProgram = ((_: js.Dynamic) => ()):                                                  js.Function1[js.Dynamic, Unit],
      getUniformLocation = ((_: js.Dynamic, _: String) => js.Dynamic.literal(tag = "uloc")): js.Function2[js.Dynamic, String, js.Dynamic],
      // The method under test: WebGL30.glUniform1uiv forwards copyUnsigned(value) here.
      uniform1uiv = (
        (_: js.Dynamic, array: Uint32Array, _: Int, _: Int) => {
          recorded = array
          ()
        }
      ): js.Function4[js.Dynamic, Uint32Array, Int, Int, Unit]
    )
  }

  test("glUniform1uiv forwards ALL unsigned-int values when count > 12000 scratch capacity") {
    val rec = new GlRecorder
    val gl  = new WebGL30(rec.dyn)

    // Register a program + uniform location so getUniformLocation(0-based id) works.
    val program = gl.glCreateProgram()
    gl.glUseProgram(program)
    val location = gl.glGetUniformLocation(program, "u")

    // 13000 > 12000 (the fixed uIntBuffer capacity). Distinct, non-trivial values
    // so dropped tail elements cannot accidentally match.
    val count  = 13000
    val buffer = IntBuffer.allocate(count)
    var k      = 0
    while (k < count) {
      buffer.put(k, 1000 + k)
      k += 1
    }
    buffer.position(0)
    buffer.limit(count)

    gl.glUniform1uiv(location, count, buffer)

    val recorded = rec.recorded

    // (1) The forwarded array must carry every element. Current code yields 12000
    //     (subarray clamps to the 12000-element scratch buffer).
    assertEquals(
      recorded.length,
      count,
      s"copyUnsigned dropped elements: forwarded ${recorded.length} of $count (scratch buffer never grew)"
    )

    // (2) Tail values at indices >= 12000 must equal the input. Under current code
    //     these indices are out of bounds / never written.
    assertEquals(recorded(12000).toLong, 1000L + 12000L, "value at index 12000 was dropped")
    assertEquals(recorded(count - 1).toLong, 1000L + (count - 1).toLong, "last value was dropped")
  }
}
