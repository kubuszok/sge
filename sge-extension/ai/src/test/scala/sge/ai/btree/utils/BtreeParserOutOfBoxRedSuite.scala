package sge
package ai
package btree
package utils

import sge.ai.btree.branch.{ Parallel, Selector, Sequence }
import sge.ai.btree.decorator.Repeat
import sge.ai.btree.leaf.{ Failure => FailureLeaf, Success => SuccessLeaf, Wait }
import sge.ai.utils.random.{ ConstantFloatDistribution, ConstantIntegerDistribution, TriangularFloatDistribution }
import lowlevel.Nullable

/** Red tests for ISS-517: the gdx-ai behavior-tree text parser is unusable out of the box.
  *
  * The original parser (original-src/gdx-ai/gdx-ai/src/com/badlogic/gdx/ai/btree/utils/BehaviorTreeParser.java) resolves the default import aliases (selector, sequence, repeat, wait, parallel, ...)
  * via reflection, reads `@TaskAttribute` metadata from the task classes, and coerces attribute values in `castValue` (lines 411-458): Number -> primitive/boxed numeric or
  * `distributionAdapters.toDistribution("constant," + numberValue, distributionType)` for Distribution-typed fields (line 431); String -> String/char, Distribution via
  * `distributionAdapters.toDistribution(stringValue, distributionType)` (line 445), or enum constant matched case-insensitively (lines 446-454).
  *
  * The SGE port replaced reflection with a TaskRegistry, but the registry starts EMPTY while `defaultImports` maps the aliases to FQCN strings (BehaviorTreeParser.scala:212-234), so parsing any
  * standard tree throws `Unknown task type: 'sge.ai.btree.branch.Selector'`. Built-in tasks also have no TaskMeta, so the standard attributes throw unknown-attribute, and DistributionAdapters —
  * although fully ported — is unreachable because the `castValue` hook was eliminated.
  *
  * The fix must pre-register all built-ins with their TaskMeta and wire DistributionAdapters — NOT restore reflection. The expectations below are pinned to the Java semantics and to the canonical
  * tree syntax of original-src/gdx-ai/tests/data/dog.tree (e.g. line 25 `parallel policy:"selector"`, line 26 `wait seconds:"triangular,2.5,5.5"`).
  *
  * NOTE: unlike BehaviorTreeParserSuite, this suite must NOT register anything — it exercises the out-of-the-box experience.
  */
class BtreeParserOutOfBoxRedSuite extends munit.FunSuite {

  /** A parser exactly as an end user gets it: no task registration whatsoever. */
  private def outOfBoxParser(): BehaviorTreeParser[String] =
    new BehaviorTreeParser[String](btReaderOpt = Nullable.empty[BehaviorTreeParser.DefaultBehaviorTreeReader[String]])

  // ── (1) Standard tree text, built-in tasks only ──────────────────────

  test("ISS-517 (1) out-of-the-box parse of a standard built-ins-only tree (root/selector/sequence/leaves)") {
    // Canonical syntax from original-src/gdx-ai/tests/data/dog.tree (root + indented branch/leaf tasks);
    // uses only tasks covered by defaultImports so no user registration is legitimately required.
    val input =
      """root
        |  selector
        |    sequence
        |      failure
        |      success
        |    success""".stripMargin
    // Currently throws SgeError.InvalidInput("Unknown task type: 'sge.ai.btree.branch.Selector'")
    // because the TaskRegistry is empty while defaultImports resolves "selector" to a FQCN string.
    val tree = outOfBoxParser().parse(input, Nullable("bb"))

    assertEquals(tree.getChildCount, 1)
    val selector = tree.getChild(0)
    assert(selector.isInstanceOf[Selector[?]], s"root task must be a Selector, got ${selector.getClass.getName}")
    assertEquals(selector.getChildCount, 2)
    val sequence = selector.getChild(0)
    assert(sequence.isInstanceOf[Sequence[?]], s"first selector child must be a Sequence, got ${sequence.getClass.getName}")
    assertEquals(sequence.getChildCount, 2)
    assert(sequence.getChild(0).isInstanceOf[FailureLeaf[?]], s"got ${sequence.getChild(0).getClass.getName}")
    assert(sequence.getChild(1).isInstanceOf[SuccessLeaf[?]], s"got ${sequence.getChild(1).getClass.getName}")
    assert(selector.getChild(1).isInstanceOf[SuccessLeaf[?]], s"got ${selector.getChild(1).getClass.getName}")

    // Behavioral pin: sequence fails on its failure leaf, selector falls through to the success leaf.
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }

  // ── (2) Built-in task attributes ─────────────────────────────────────

  test("ISS-517 (2a) built-in attribute repeat times:5 becomes ConstantIntegerDistribution(5)") {
    // Java: Repeat.times is @TaskAttribute IntegerDistribution; castValue turns the Number 5 into
    // distributionAdapters.toDistribution("constant,5", IntegerDistribution) (BehaviorTreeParser.java:414-432).
    // Currently red: empty registry ("Unknown task type: 'sge.ai.btree.decorator.Repeat'"); after mere
    // registration it would still be red with "repeat at line 1: unknown attribute 'times'" until the
    // built-in TaskMeta + Number->constant-distribution coercion are wired.
    val input =
      """repeat times:5
        |  success""".stripMargin
    val tree   = outOfBoxParser().parse(input, Nullable("bb"))
    val repeat = tree.getChild(0)
    assert(repeat.isInstanceOf[Repeat[?]], s"root task must be a Repeat, got ${repeat.getClass.getName}")
    val times = repeat.asInstanceOf[Repeat[String]].times
    assert(
      times.isInstanceOf[ConstantIntegerDistribution],
      s"times must be a ConstantIntegerDistribution, got ${times.getClass.getName}"
    )
    assertEquals(times.asInstanceOf[ConstantIntegerDistribution].value, 5)
    // Behavioral pin: the success leaf completes synchronously, so a single step exhausts all 5 repetitions.
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }

  test("ISS-517 (2b) built-in attribute wait seconds:2.5 becomes ConstantFloatDistribution(2.5)") {
    // Java: Wait.seconds is @TaskAttribute FloatDistribution; castValue turns the Number 2.5 into
    // distributionAdapters.toDistribution("constant,2.5", FloatDistribution) (BehaviorTreeParser.java:428-432).
    val tree = outOfBoxParser().parse("wait seconds:2.5", Nullable("bb"))
    val wait = tree.getChild(0)
    assert(wait.isInstanceOf[Wait[?]], s"root task must be a Wait, got ${wait.getClass.getName}")
    val seconds = wait.asInstanceOf[Wait[String]].seconds
    assert(
      seconds.isInstanceOf[ConstantFloatDistribution],
      s"seconds must be a ConstantFloatDistribution, got ${seconds.getClass.getName}"
    )
    assertEquals(seconds.asInstanceOf[ConstantFloatDistribution].value, 2.5f)
  }

  test("ISS-517 (2c) built-in attribute parallel policy:\"selector\" becomes Parallel.Policy.Selector") {
    // dog.tree line 25 uses exactly `parallel policy:"selector"`. Java: Parallel.policy is a
    // @TaskAttribute enum field; castValue matches the enum constant case-insensitively
    // (BehaviorTreeParser.java:446-454). "selector" (not the "sequence" default) is asserted so a
    // value-dropping setter cannot pass by accident.
    val input =
      """parallel policy:"selector"
        |  success
        |  failure""".stripMargin
    val tree = outOfBoxParser().parse(input, Nullable("bb"))
    val par  = tree.getChild(0)
    assert(par.isInstanceOf[Parallel[?]], s"root task must be a Parallel, got ${par.getClass.getName}")
    assertEquals(par.asInstanceOf[Parallel[String]].policy, Parallel.Policy.Selector)
    // Behavioral pin: selector policy succeeds as soon as one child succeeds.
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }

  // ── (3) Distribution-valued attribute via DistributionAdapters ───────

  test("ISS-517 (3) distribution attribute wait seconds:\"triangular,1,5,3\" goes through DistributionAdapters") {
    // dog.tree line 26 uses `wait seconds:"triangular,2.5,5.5"`. Java: String values for
    // Distribution-typed fields are converted via distributionAdapters.toDistribution(stringValue, type)
    // (BehaviorTreeParser.java:442-445); the triangular FloatDistribution adapter accepts 1, 2 or 3
    // comma-separated args (DistributionAdapters.scala:223-232 — fully ported but unreachable today).
    val tree = outOfBoxParser().parse("""wait seconds:"triangular,1,5,3"""", Nullable("bb"))
    val wait = tree.getChild(0)
    assert(wait.isInstanceOf[Wait[?]], s"root task must be a Wait, got ${wait.getClass.getName}")
    val seconds = wait.asInstanceOf[Wait[String]].seconds
    assert(
      seconds.isInstanceOf[TriangularFloatDistribution],
      s"seconds must be a TriangularFloatDistribution, got ${seconds.getClass.getName}"
    )
    val tri = seconds.asInstanceOf[TriangularFloatDistribution]
    assertEquals(tri.low, 1f)
    assertEquals(tri.high, 5f)
    assertEquals(tri.mode, 3f)
  }

  // ── (4) BehaviorTreeLibrary lazy parse-from-file fallback ────────────

  test("ISS-517 (4) BehaviorTreeLibrary must lazily parse unregistered references, not fail lookup-only") {
    // Java original (BehaviorTreeLibrary.java:121-134): on a repository miss, retrieveArchetypeTree
    // does `archetypeTree = parser.parse(resolver.resolve(treeReference), null)` and registers the
    // result. The SGE port (BehaviorTreeLibrary.scala:81-85) dropped the resolver+parser wiring
    // entirely and throws NoSuchElementException("No archetype tree registered for reference: ...").
    //
    // The current API offers no resolver seam at all, so an in-memory resolver fixture cannot even be
    // injected; this test therefore pins the reachable half of the contract: an unregistered reference
    // must trigger a load/parse attempt (whose failure for a missing file is a file/serialization
    // error), never the lookup-only NoSuchElementException.
    val library = new BehaviorTreeLibrary()
    val thrown  = intercept[Throwable] {
      library.createBehaviorTree[String]("iss517/does-not-exist.tree")
    }
    assert(
      !thrown.isInstanceOf[NoSuchElementException],
      s"retrieveArchetypeTree must attempt the lazy parse-from-file fallback (Java BehaviorTreeLibrary.java:121-134) instead of failing the repository lookup; got: $thrown"
    )
  }

  // ── (5) Control: manual registration keeps working (must stay green) ─

  test("ISS-517 (5) control: manually registered tasks still parse (no regression)") {
    val registry = new BehaviorTreeParser.TaskRegistry()
    registry.registerTask("sge.ai.btree.branch.Selector", () => new Selector[String]())
    registry.registerTask(
      "myFail",
      () =>
        new LeafTask[String] {
          override def execute():                            Task.Status  = Task.Status.FAILED
          override def newInstance():                        Task[String] = throw new UnsupportedOperationException
          override protected def copyTo(task: Task[String]): Task[String] = task
        }
    )
    registry.registerTask(
      "mySuccess",
      () =>
        new LeafTask[String] {
          override def execute():                            Task.Status  = Task.Status.SUCCEEDED
          override def newInstance():                        Task[String] = throw new UnsupportedOperationException
          override protected def copyTo(task: Task[String]): Task[String] = task
        }
    )
    val reader = new BehaviorTreeParser.DefaultBehaviorTreeReader[String](taskRegistry = registry)
    val parser = new BehaviorTreeParser[String](btReaderOpt = Nullable(reader))
    val input  =
      """selector
        |  myFail
        |  mySuccess""".stripMargin
    val tree = parser.parse(input, Nullable("bb"))
    tree.step()
    assertEquals(tree.getStatus, Task.Status.SUCCEEDED)
  }
}
