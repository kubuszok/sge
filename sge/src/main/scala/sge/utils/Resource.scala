/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

opaque type Resource[A] = Eval[(A, Eval[Unit])]
object Resource {

  def make[A](create: => A)(cleanup: A => Unit): Resource[A] = Eval {
    val a = create
    val c = Eval(cleanup(a))
    (a, c)
  }

  def fromCloseable[A <: java.io.Closeable](create: => A): Resource[A] = make(create)(_.close())

  def pure[A](a: A): Resource[A] = Eval.pure((a, Eval.void))

  extension [A](res: Resource[A]) {
    def map[B](f: A => B):               Resource[B] = flatMap(a => Eval.pure(f(a._1) -> a._2))
    def flatMap[B](f: A => Resource[B]): Resource[B] =
      res.flatMap { case (aValue, aCleanup) =>
        try
          f(aValue).map { case (bValue, bCleanup) =>
            bValue -> (bCleanup >> aCleanup)
          }
        catch {
          case e: Throwable =>
            aCleanup.run
            throw e
        }
      }

    def eval: Eval[(A, Eval[Unit])] = res

    def run(): (A, () => Unit) = {
      val (value, f) = res.run
      (value, () => f.run)
    }
  }
}
