package sge.port.full

import balticporter.tir.*

/** A library-specific rule — a phase living OUTSIDE the engine, in `corpus`, because `com.badlogic.gdx.utils.Array.iterator()` returns a CACHED iterator reset in place, so nested iteration over the
  * same collection silently terminates the outer loop early — a libGDX allocation invariant, not a Java/Scala fact. Enters the pipeline as an ordinary `Phase` element of `PortRun(phases = …)`.
  * REPORTS rather than rewrites (deferred).
  */
final class GdxSharedIteratorRule extends Phase:

  def name: String = "gdx-shared-iterator"

  /** libGDX collections whose `iterator()` returns a CACHED instance. Not a configuration knob — a fact about this library's `utils` package, listed because it is finite and known.
    */
  private val cachedIteratorCollections: Set[String] = Set(
    "com.badlogic.gdx.utils.Array",
    "com.badlogic.gdx.utils.SnapshotArray",
    "com.badlogic.gdx.utils.DelayedRemovalArray",
    "com.badlogic.gdx.utils.ArrayMap",
    "com.badlogic.gdx.utils.ObjectMap",
    "com.badlogic.gdx.utils.ObjectSet",
    "com.badlogic.gdx.utils.OrderedMap",
    "com.badlogic.gdx.utils.OrderedSet",
    "com.badlogic.gdx.utils.IntMap",
    "com.badlogic.gdx.utils.IntSet",
    "com.badlogic.gdx.utils.LongMap",
    "com.badlogic.gdx.utils.IdentityMap",
    "com.badlogic.gdx.utils.Queue"
  )

  final case class Finding(collection: String, receiver: String, outer: Origin, inner: Origin):
    def render: String =
      s"nested iteration over the same $collection — the cached iterator is reset by the inner " +
        s"loop and the outer loop ends early  (outer ${outer.javaPath}:${outer.line}, inner " +
        s"${inner.javaPath}:${inner.line})"

  private val found = collection.mutable.ListBuffer.empty[Finding]

  def findings: List[Finding] = found.toList

  /** Full-control entry point: a whole-program analysis, then the program returned UNCHANGED. Uses `StandardTraversal.scanTerm` rather than a private recursion (a walk that misses a node kind reports
    * zero hazards from a program that has them).
    */
  override def run(program: Program): Program =
    given Program = program
    found.clear()
    val outerScan = new Phase:
      def name:                                           String = "gdx-shared-iterator/outer"
      override def transformTerm(t: Term)(using Program): Term   =
        t match
          case fe: Tree.ForEach =>
            for
              coll <- collectionOf(program, fe)
              recv <- receiverOf(program, fe)
              inner <- nestedOver(program, fe, recv)
            do found += Finding(coll, recv, fe.origin, inner.origin)
          case _ => ()
        t
    program.units.foreach(u => StandardTraversal.mapClassDef(outerScan, u))

    val fs = findings
    println(s"[gdx-shared-iterator] ${fs.size} nested-iteration hazard(s) over a cached libGDX iterator")
    if fs.nonEmpty then
      println(
        "  [library-specific rule: rewrite the INNER loop to `new Array.ArrayIterator<>(a)`. " +
          "The engine cannot know this — it is libGDX's allocation strategy, not a Java/Scala fact.]"
      )
      fs.foreach(f => println("  " + f.render))
    // Registered even when empty, so `counts.tsv` can tell "found nothing" from "never ran".
    CheckReport.record(
      name,
      fs.map { f =>
        CheckReport.Finding(name, "nested-cached-iterator", f.receiver, CheckReport.relativise(f.inner.javaPath), f.inner.line, f.render)
      }
    )
    program

  /** the libGDX collection FQN this loop iterates, if it is one with a cached iterator. */
  private def collectionOf(program: Program, fe: Tree.ForEach): Option[String] =
    headSymbol(fe.iterable.tpe).flatMap(program.symbolOf).map(_.fullName).filter(cachedIteratorCollections.contains)

  /** the SYMBOL being iterated, named so two loops can be compared. Only a plain reference qualifies: `for (X x : a)` and `for (Y y : a)` share `a`'s iterator, while `for (X x : a.copy())` does not —
    * and treating a call result as the same receiver would manufacture hazards that do not exist.
    */
  private def receiverOf(program: Program, fe: Tree.ForEach): Option[String] =
    (fe.iterable match
      case i: Tree.Ident  => Some(i.sym)
      case s: Tree.Select => Some(s.sym)
      case _ => scala.None
    ).flatMap(program.symbolOf).map(_.fullName)

  /** a for-each INSIDE `fe`'s body over the same receiver. */
  private def nestedOver(program: Program, fe: Tree.ForEach, receiver: String)(using Program): Option[Tree.ForEach] =
    StandardTraversal
      .scanTerm(fe.body, List.empty[Tree.ForEach]) { (acc, t) =>
        t match
          case inner: Tree.ForEach if receiverOf(program, inner).contains(receiver) => inner :: acc
          case _ => acc
      }
      .lastOption

  private def headSymbol(t: TypeRepr): Option[SymId] = t match
    case TypeRepr.TypeRef(_, s)      => Some(s)
    case TypeRepr.AppliedType(tc, _) => headSymbol(tc)
    case _                           => scala.None
