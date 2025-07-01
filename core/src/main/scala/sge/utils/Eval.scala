package sge
package utils

enum Eval[A] { fa =>
  private case Pure(value: A) extends Eval[A]
  private case Impure[B, A](eval: Eval[B], f: B => Eval[A]) extends Eval[A]

  import Eval.*

  // ----------------------------------------------- Monadic operations -----------------------------------------------

  final def flatMap[B](f:           A => Eval[B]):  Eval[B] = Impure(fa, f)
  final def flatten[B](implicit ev: A <:< Eval[B]): Eval[B] = flatMap(ev)
  final def flatTap[B](f:           A => Eval[B]):  Eval[A] = flatMap(a => f(a).as(a))

  final def map[B](f: A => B): Eval[B] = flatMap(a => Pure(f(a)))
  final def mapTap[B](f: A => B): Eval[A] = map { a =>
    val _ = f(a)
    a
  }

  final def map2[B, C](fb: => Eval[B])(f: (A, B) => C): Eval[C]      = flatMap(a => fb.map(b => f(a, b)))
  final def tuple[B](fb:   => Eval[B]):                 Eval[(A, B)] = map2(fb)((a, b) => (a, b))

  final def as[B](b: B): Eval[B]    = map(_ => b)
  final def void:        Eval[Unit] = as(())

  final def >>[B](fb: => Eval[B]): Eval[B] = flatMap(_ => fb)
  final def *>[B](fb: => Eval[B]): Eval[A] = map2(fb)((a, _) => a)
  final def <*[B](fb: => Eval[B]): Eval[B] = map2(fb)((_, b) => b)

  // --------------------------------------------------- Utilities ----------------------------------------------------

  final def run: A = Eval.run(fa)
}
object Eval {

  def pure[A](value: A): Eval[A]    = Pure(value)
  def void:              Eval[Unit] = pure(())

  def apply[A](value: => A):       Eval[A] = defer(pure(value))
  def defer[A](thunk: => Eval[A]): Eval[A] = void.flatMap(_ => thunk)

  // --------------------------------------------- Implementation details ---------------------------------------------

  @scala.annotation.tailrec
  private def run[A](eval: Eval[A]): A = eval match {
    case Pure(value)                 => value
    case Impure(Pure(value), f)      => run(f(value))
    case Impure(Impure(eval, f), f2) => run(Impure(eval, f andThen (_.flatMap(f2))))
  }
}
