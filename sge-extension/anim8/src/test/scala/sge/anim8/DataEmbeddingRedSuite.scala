package sge
package anim8

/** Red test for ISS-527: anim8's large constant binary blobs ([[ConstantData.ENCODED_SNUGGLY]] and the three `TRI_BLUE_NOISE*` arrays) were externalized into `.bin` classpath resources that are
  * loaded with `getClass.getResourceAsStream`. That mechanism does NOT work on Scala.js (the resource is absent / the stream is null) and fails on plain Scala Native, so on those platforms the
  * `ConstantData` object init throws `RuntimeException("anim8: Missing resource: ...")`. The data must instead be embedded in source (as upstream `com.github.tommyettinger.anim8.ConstantData` does)
  * so it is available on every platform.
  *
  * Upstream reference: original-src/anim8-gdx/.../com/github/tommyettinger/anim8/ConstantData.java (commit 89e05579a6eee076644fce57798cc772281fb05e) — ENCODED_SNUGGLY, TRI_BLUE_NOISE,
  * TRI_BLUE_NOISE_B, TRI_BLUE_NOISE_C embedded as string literals, decoded ISO-8859-1.
  *
  * Expected byte values are upstream java's at that commit (user ruling 2026-09-26, ISS-904), so the assertions pin the ACTUAL data, not merely its length, on every platform.
  */
class DataEmbeddingRedSuite extends munit.FunSuite {

  test("ENCODED_SNUGGLY has the correct length and known bytes") {
    val data = ConstantData.ENCODED_SNUGGLY
    assertEquals(data.length, 32768, "ENCODED_SNUGGLY must be 32768 bytes (Snuggly255 palette preload)")
    assertEquals(data(0).toInt, 1, "ENCODED_SNUGGLY[0]")
    assertEquals(data(1000).toInt, 90, "ENCODED_SNUGGLY[1000]")
    assertEquals(data(32767).toInt, 20, "ENCODED_SNUGGLY[32767]")
  }

  test("TRI_BLUE_NOISE has the correct length and known bytes") {
    val data = ConstantData.TRI_BLUE_NOISE
    assertEquals(data.length, 4096, "TRI_BLUE_NOISE must be 4096 bytes (64x64 grid)")
    assertEquals(data(0).toInt, -19, "TRI_BLUE_NOISE[0]")
    assertEquals(data(100).toInt, 60, "TRI_BLUE_NOISE[100]")
    assertEquals(data(3000).toInt, -4, "TRI_BLUE_NOISE[3000]")
    assertEquals(data(4095).toInt, -34, "TRI_BLUE_NOISE[4095]")
  }

  test("TRI_BLUE_NOISE_B has the correct length and known bytes") {
    val data = ConstantData.TRI_BLUE_NOISE_B
    assertEquals(data.length, 4096, "TRI_BLUE_NOISE_B must be 4096 bytes")
    assertEquals(data(0).toInt, -76, "TRI_BLUE_NOISE_B[0]")
    assertEquals(data(100).toInt, -36, "TRI_BLUE_NOISE_B[100]")
    assertEquals(data(4095).toInt, 60, "TRI_BLUE_NOISE_B[4095]")
  }

  test("TRI_BLUE_NOISE_C has the correct length and known bytes") {
    val data = ConstantData.TRI_BLUE_NOISE_C
    assertEquals(data.length, 4096, "TRI_BLUE_NOISE_C must be 4096 bytes")
    assertEquals(data(0).toInt, -124, "TRI_BLUE_NOISE_C[0]")
    assertEquals(data(100).toInt, -55, "TRI_BLUE_NOISE_C[100]")
    assertEquals(data(4095).toInt, 89, "TRI_BLUE_NOISE_C[4095]")
  }

  test("TRI_BLUE_NOISE_MULTIPLIERS are derived and non-trivial") {
    val m = ConstantData.TRI_BLUE_NOISE_MULTIPLIERS
    assertEquals(m.length, 4096, "TRI_BLUE_NOISE_MULTIPLIERS must mirror TRI_BLUE_NOISE length")
    assert(m.exists(_ != 0f), "TRI_BLUE_NOISE_MULTIPLIERS must not be all-zero")
    assert(ConstantData.TRI_BLUE_NOISE_MULTIPLIERS_B.exists(_ != 0f), "TRI_BLUE_NOISE_MULTIPLIERS_B must not be all-zero")
    assert(ConstantData.TRI_BLUE_NOISE_MULTIPLIERS_C.exists(_ != 0f), "TRI_BLUE_NOISE_MULTIPLIERS_C must not be all-zero")
  }
}
