/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/Engine.java (createComponent), com/badlogic/ashley/core/PooledEngine.java (ComponentPools)
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   SGE-original: replaces ashley's reflective ClassReflection.newInstance / ReflectionPool construction
 *   Idiom: a type class derived at compile time, so a type that cannot be built is a compile error on every platform
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 59
 * Covenant-baseline-methods: ComponentFactory,apply,create,derivedImpl
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-09-25
 */
package sge
package ecs

import scala.quoted.*

/** Builds a new `T` without reflection: `Engine.createComponent`, `PooledEngine.createComponent` and the component pools take one. An instance is derived for any concrete class with an accessible
  * no-argument constructor; any other type does not compile where one is asked for. A local `given` supplies a custom construction.
  */
trait ComponentFactory[T] {
  def create(): T
}

object ComponentFactory {

  /** A factory from a function. */
  def apply[T](f: () => T): ComponentFactory[T] = new ComponentFactory[T] {
    def create(): T = f()
  }

  inline given derived[T]: ComponentFactory[T] = ${ derivedImpl[T] }

  private def derivedImpl[T: Type](using q: Quotes): Expr[ComponentFactory[T]] = {
    import q.reflect.*
    val tpe = TypeRepr.of[T]
    val cls = tpe.classSymbol.getOrElse(report.errorAndAbort(s"${tpe.show} is not a class, so no ComponentFactory can be derived for it"))
    if (cls.flags.is(Flags.Abstract) || cls.flags.is(Flags.Trait)) report.errorAndAbort(s"${tpe.show} is abstract, so no ComponentFactory can be derived for it")
    val nullary = (cls.primaryConstructor :: cls.declarations.filter(_.isClassConstructor)).find { c =>
      !c.isNoSymbol && !c.flags.is(Flags.Private) && !c.flags.is(Flags.Protected) && c.paramSymss.forall(ps => ps.isEmpty || ps.head.isTypeParam)
    }
    val ctor = nullary.getOrElse(
      report.errorAndAbort(s"${tpe.show} has no accessible no-argument constructor, so no ComponentFactory can be derived for it")
    )
    val built = New(Inferred(tpe)).select(ctor).appliedToArgs(Nil).asExprOf[T]
    '{
      new ComponentFactory[T] {
        def create(): T = $built
      }
    }
  }
}
