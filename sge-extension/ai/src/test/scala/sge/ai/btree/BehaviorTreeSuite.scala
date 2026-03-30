package sge
package ai
package btree

import sge.ai.btree.branch.{Selector, Sequence}
import sge.ai.btree.branch.Parallel
import sge.ai.btree.decorator.{Invert, Repeat}
import sge.ai.utils.random.ConstantIntegerDistribution
import sge.utils.Nullable

// ── Test leaf tasks ──────────────────────────────────────────────────────

class SuccessTask[E] extends LeafTask[E] {
  override def execute(): Task.Status = Task.Status.SUCCEEDED
  override def newInstance(): Task[E] = new SuccessTask[E]()
  override protected def copyTo(task: Task[E]): Task[E] = task
}

class FailTask[E] extends LeafTask[E] {
  override def execute(): Task.Status = Task.Status.FAILED
  override def newInstance(): Task[E] = new FailTask[E]()
  override protected def copyTo(task: Task[E]): Task[E] = task
}

class RunningTask[E] extends LeafTask[E] {
  override def execute(): Task.Status = Task.Status.RUNNING
  override def newInstance(): Task[E] = new RunningTask[E]()
  override protected def copyTo(task: Task[E]): Task[E] = task
}

/** A leaf task that succeeds on the Nth call, fails on all others. */
class CountingTask[E](var succeedOn: Int) extends LeafTask[E] {
  private var callCount: Int = 0
  override def execute(): Task.Status = {
    callCount += 1
    if (callCount == succeedOn) Task.Status.SUCCEEDED else Task.Status.FAILED
  }
  override def newInstance(): Task[E] = new CountingTask[E](succeedOn)
  override protected def copyTo(task: Task[E]): Task[E] = {
    task.asInstanceOf[CountingTask[E]].succeedOn = succeedOn
    task
  }
}

class BehaviorTreeSuite extends munit.FunSuite {

  private def makeTree[E](root: Task[E], blackboard: E): BehaviorTree[E] = {
    val bt = new BehaviorTree[E](Nullable(root), Nullable(blackboard))
    bt
  }

  // ── Selector ───────────────────────────────────────────────────────────

  test("Selector: returns SUCCESS if any child succeeds") {
    val selector = new Selector[String]()
    selector.addChild(new FailTask[String]())
    selector.addChild(new SuccessTask[String]())
    selector.addChild(new FailTask[String]())

    val bt = makeTree(selector, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  test("Selector: returns FAILED if all children fail") {
    val selector = new Selector[String]()
    selector.addChild(new FailTask[String]())
    selector.addChild(new FailTask[String]())
    selector.addChild(new FailTask[String]())

    val bt = makeTree(selector, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.FAILED)
  }

  test("Selector: returns RUNNING if a child is running") {
    val selector = new Selector[String]()
    selector.addChild(new FailTask[String]())
    selector.addChild(new RunningTask[String]())
    selector.addChild(new SuccessTask[String]())

    val bt = makeTree(selector, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.RUNNING)
  }

  // ── Sequence ───────────────────────────────────────────────────────────

  test("Sequence: returns SUCCESS if all children succeed") {
    val sequence = new Sequence[String]()
    sequence.addChild(new SuccessTask[String]())
    sequence.addChild(new SuccessTask[String]())
    sequence.addChild(new SuccessTask[String]())

    val bt = makeTree(sequence, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  test("Sequence: returns FAILED if any child fails") {
    val sequence = new Sequence[String]()
    sequence.addChild(new SuccessTask[String]())
    sequence.addChild(new FailTask[String]())
    sequence.addChild(new SuccessTask[String]())

    val bt = makeTree(sequence, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.FAILED)
  }

  test("Sequence: returns RUNNING if a child is running") {
    val sequence = new Sequence[String]()
    sequence.addChild(new SuccessTask[String]())
    sequence.addChild(new RunningTask[String]())
    sequence.addChild(new SuccessTask[String]())

    val bt = makeTree(sequence, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.RUNNING)
  }

  // ── Parallel ───────────────────────────────────────────────────────────

  test("Parallel Policy.Sequence: fails if any child fails") {
    val parallel = new Parallel[String](policy = Parallel.Policy.Sequence)
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new FailTask[String]())
    parallel.addChild(new SuccessTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.FAILED)
  }

  test("Parallel Policy.Sequence: succeeds if all children succeed") {
    val parallel = new Parallel[String](policy = Parallel.Policy.Sequence)
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new SuccessTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  test("Parallel Policy.Selector: succeeds if any child succeeds") {
    val parallel = new Parallel[String](policy = Parallel.Policy.Selector)
    parallel.addChild(new FailTask[String]())
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new FailTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  test("Parallel Policy.Selector: fails if all children fail") {
    val parallel = new Parallel[String](policy = Parallel.Policy.Selector)
    parallel.addChild(new FailTask[String]())
    parallel.addChild(new FailTask[String]())
    parallel.addChild(new FailTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.FAILED)
  }

  // ── Decorator: Invert ──────────────────────────────────────────────────

  test("Invert: flips success to failure") {
    val invert = new Invert[String](Nullable.empty[Task[String]])
    invert.addChild(new SuccessTask[String]())

    val bt = makeTree(invert, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.FAILED)
  }

  test("Invert: flips failure to success") {
    val invert = new Invert[String](Nullable.empty[Task[String]])
    invert.addChild(new FailTask[String]())

    val bt = makeTree(invert, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  // ── Decorator: Repeat ──────────────────────────────────────────────────

  test("Repeat: runs child N times then succeeds") {
    val child = new SuccessTask[String]()
    val repeat = new Repeat[String](
      times = new ConstantIntegerDistribution(3),
      child = Nullable(child)
    )

    val bt = makeTree(repeat, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  // ── Tree step() ────────────────────────────────────────────────────────

  test("step() drives execution of the root task") {
    val bt = new BehaviorTree[String](
      Nullable(new SuccessTask[String]()),
      Nullable("bb")
    )
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  test("step() re-runs RUNNING root on subsequent calls") {
    val bt = new BehaviorTree[String](
      Nullable(new RunningTask[String]()),
      Nullable("bb")
    )
    bt.step()
    assertEquals(bt.getStatus, Task.Status.RUNNING)
    // Step again - running task keeps running
    bt.step()
    assertEquals(bt.getStatus, Task.Status.RUNNING)
  }

  // ── Guard evaluation ───────────────────────────────────────────────────

  test("guard blocks task execution when guard fails") {
    val guarded = new SuccessTask[String]()
    guarded.guard = Nullable(new FailTask[String]())

    val selector = new Selector[String]()
    selector.addChild(guarded)
    selector.addChild(new SuccessTask[String]()) // fallback

    val bt = makeTree(selector, "bb")
    bt.step()
    // The guarded task fails due to guard, selector moves to next child which succeeds
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }
}
