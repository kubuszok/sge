package sge
package math

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ConcurrentLinkedQueue

/** ISS-832 (wave 2026-07-18-I, territory I6) — RED reproduction.
  *
  * `OrthographicCamera.update` calls `view.setToLookAt(direction, up)`, which builds an orthonormal basis in the PROCESS-WIDE shared companion scratch `Matrix4.l_vex` / `Matrix4.l_vey` /
  * `Matrix4.l_vez` (see `sge.math.Matrix4.setToLookAt`, Matrices.scala). LibGDX declares the same `static final Vector3 l_vex/l_vey/l_vez` (Matrix4.java:78-80) — a thread-unsafe upstream pattern
  * faithfully copied into the port.
  *
  * ScalaTest/munit suites in the core `sge` module run with `Test / parallelExecution := true` (sbt default; the module never overrides it — build.sbt line 273 block), so multiple suites call
  * `camera.update()` concurrently in one forked JVM. Two threads interleaving inside `setToLookAt` corrupt each other's basis: when `l_vey = l_vex x l_vez` is taken of two momentarily-parallel
  * scratch vectors the cross product is the zero vector, `nor()` leaves it zero (Vector.normalize returns unchanged when len==0), the view matrix gets a zero row, `combined = proj*view` has
  * determinant exactly 0, and `Matrix4.inv()` throws `RuntimeException("non-invertible matrix")` (Matrices.scala:1131). Milder interleavings silently yield a non-orthonormal view matrix.
  *
  * This suite pins BOTH failure modes: each thread owns its Matrix4 instances and a fixed (direction, up); the only shared state is the Matrix4 companion scratch. With the shared statics it throws or
  * produces a view that differs from the single-threaded reference. A per-instance / thread-local scratch fix makes it pass.
  *
  * RED until ISS-832 is fixed.
  */
class Matrix4ConcurrentLookAtRedSuite extends munit.FunSuite {

  private val epsilon = 1e-4f

  // Distinct (direction, up) pairs so the shared basis scratch diverges between threads under a race.
  private val cases: Array[(Vector3, Vector3)] = Array(
    (Vector3(0, 0, -1), Vector3(0, 1, 0)),
    (Vector3(0, 0, 1), Vector3(0, -1, 0)),
    (Vector3(1, 0, 0), Vector3(0, 1, 0)),
    (Vector3(-1, 0, 0), Vector3(0, 1, 0)),
    (Vector3(0, 1, 0), Vector3(0, 0, 1)),
    (Vector3(0, -1, 0), Vector3(0, 0, -1)),
    (Vector3(1, 1, 1), Vector3(0, 1, 0)),
    (Vector3(-1, 1, -1), Vector3(0, 1, 0))
  )

  private def reference(dir: Vector3, up: Vector3): Matrix4 =
    new Matrix4().setToLookAt(new Vector3().set(dir), new Vector3().set(up))

  test("ISS-832: concurrent OrthographicCamera-style setToLookAt/inv must not corrupt shared Matrix4 scratch") {
    val threads    = cases.length
    val iterations = 50000
    val barrier    = new CyclicBarrier(threads)
    val errors     = new ConcurrentLinkedQueue[Throwable]()

    val workers = (0 until threads).map { t =>
      val (dir, up) = cases(t)
      val expected  = reference(dir, up) // computed single-threaded, before contention
      val thread    = new Thread(
        () => {
          val proj     = new Matrix4().setToOrtho(-100f, 100f, -100f, 100f, 0f, 100f)
          val view     = new Matrix4()
          val combined = new Matrix4()
          val d        = new Vector3().set(dir)
          val u        = new Vector3().set(up)
          barrier.await()
          var i = 0
          while (i < iterations && errors.isEmpty) {
            try {
              view.setToLookAt(d, u)
              // Silent-corruption check: a raced basis differs from the single-threaded reference.
              var j = 0
              while (j < 16) {
                if (Math.abs(view.values(j) - expected.values(j)) > epsilon)
                  throw new AssertionError(
                    s"corrupted view row at $j: ${view.values(j)} vs ${expected.values(j)} (dir=$dir up=$up)"
                  )
                j += 1
              }
              // Exact-zero-determinant check: the reported flake — inv() throws "non-invertible matrix".
              combined.set(proj).mul(view)
              combined.inv()
            } catch {
              case e: Throwable => errors.add(e)
            }
            i += 1
          }
        },
        s"iss832-lookat-$t"
      )
      thread
    }

    workers.foreach(_.start())
    workers.foreach(_.join())

    if (!errors.isEmpty)
      fail(s"concurrent camera update corrupted shared Matrix4 scratch (ISS-832): ${errors.peek()}")
  }
}
