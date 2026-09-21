package sge.port

/** The bodies of libGDX core's 59 java `native` members on the JVM: each delegates to a port-written object injected with the `natives` step (`Gdx2DNative`, `BufferUtilsNative`, `ETC1Native`);
  * `Matrix4`'s three strided loops are written out over the class's own single-vector statics. Keyed in `MethodBodyTransform`'s `owner#name(params)` grammar.
  */
object LibgdxNativeBodies {
  private val BU = "com.badlogic.gdx.utils.BufferUtils#"
  private val bu = "sge.utils.BufferUtilsNative."

  /** java's strided loop over the class's own single-vector static (`Matrix4.java` 1300–1345). */
  private def strided(single: String): String =
    s"{ var i = 0; var o = offset; while (i < numVecs) { val v = scala.Array(vecs(o), vecs(o + 1), vecs(o + 2)); " +
      s"sge.math.Matrix4.$single(mat, v); vecs(o) = v(0); vecs(o + 1) = v(1); vecs(o + 2) = v(2); o += stride; i += 1 } }"
  private val M4 = "com.badlogic.gdx.math.Matrix4#"
  val matrix4: Map[String, String] = Map(
    s"${M4}mulVec(float[],float[],int,int,int)" -> strided("mulVec"),
    s"${M4}prj(float[],float[],int,int,int)" -> strided("prj"),
    s"${M4}rot(float[],float[],int,int,int)" -> strided("rot")
  )

  val all: Map[String, String] = matrix4
}
