package sge
package ai
package btree

import sge.ai.btree.branch.{ Selector, Sequence }
import sge.ai.btree.branch.Parallel
import sge.ai.btree.decorator.{ Invert, Repeat }
import sge.ai.utils.random.ConstantIntegerDistribution
import sge.utils.{ DynamicArray, Nullable }

// ── Test leaf tasks ──────────────────────────────────────────────────────

class SuccessTask[E] extends LeafTask[E] {
  override def execute():                       Task.Status = Task.Status.SUCCEEDED
  override def newInstance():                   Task[E]     = new SuccessTask[E]()
  override protected def copyTo(task: Task[E]): Task[E]     = task
}

class FailTask[E] extends LeafTask[E] {
  override def execute():                       Task.Status = Task.Status.FAILED
  override def newInstance():                   Task[E]     = new FailTask[E]()
  override protected def copyTo(task: Task[E]): Task[E]     = task
}

class RunningTask[E] extends LeafTask[E] {
  override def execute():                       Task.Status = Task.Status.RUNNING
  override def newInstance():                   Task[E]     = new RunningTask[E]()
  override protected def copyTo(task: Task[E]): Task[E]     = task
}

/** A leaf task that succeeds on the Nth call, fails on all others. */
class CountingTask[E](var succeedOn: Int) extends LeafTask[E] {
  private var callCount:  Int         = 0
  override def execute(): Task.Status = {
    callCount += 1
    if (callCount == succeedOn) Task.Status.SUCCEEDED else Task.Status.FAILED
  }
  override def newInstance():                   Task[E] = new CountingTask[E](succeedOn)
  override protected def copyTo(task: Task[E]): Task[E] = {
    task.asInstanceOf[CountingTask[E]].succeedOn = succeedOn
    task
  }
}

/** A leaf task with mutable status and execution counter, for multi-step tests. */
class MutableStatusTask[E] extends LeafTask[E] {
  var nextStatus:         Task.Status = Task.Status.RUNNING
  var executions:         Int         = 0
  override def execute(): Task.Status = {
    executions += 1
    nextStatus
  }
  override def newInstance():                   Task[E] = new MutableStatusTask[E]()
  override protected def copyTo(task: Task[E]): Task[E] = task
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
    val child  = new SuccessTask[String]()
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

  // ── Parallel Orchestrator: Resume ─────────────────────────────────────

  test("Parallel Resume + Sequence: all succeed in one step -> SUCCEEDED") {
    val parallel = new Parallel[String](
      policy = Parallel.Policy.Sequence,
      orchestrator = Parallel.Orchestrator.Resume
    )
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new SuccessTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  test("Parallel Resume + Sequence: one fails -> FAILED") {
    val parallel = new Parallel[String](
      policy = Parallel.Policy.Sequence,
      orchestrator = Parallel.Orchestrator.Resume
    )
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new FailTask[String]())
    parallel.addChild(new SuccessTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.FAILED)
  }

  test("Parallel Resume + Selector: one succeeds -> SUCCEEDED") {
    val parallel = new Parallel[String](
      policy = Parallel.Policy.Selector,
      orchestrator = Parallel.Orchestrator.Resume
    )
    parallel.addChild(new FailTask[String]())
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new FailTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  test("Parallel Resume + Sequence: running child keeps parallel RUNNING") {
    val parallel = new Parallel[String](
      policy = Parallel.Policy.Sequence,
      orchestrator = Parallel.Orchestrator.Resume
    )
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new RunningTask[String]())
    parallel.addChild(new SuccessTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.RUNNING)
  }

  // ── Parallel Orchestrator: Join ───────────────────────────────────────

  test("Parallel Join + Sequence: all succeed -> SUCCEEDED") {
    val parallel = new Parallel[String](
      policy = Parallel.Policy.Sequence,
      orchestrator = Parallel.Orchestrator.Join
    )
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new SuccessTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  test("Parallel Join + Sequence: one fails -> FAILED") {
    val parallel = new Parallel[String](
      policy = Parallel.Policy.Sequence,
      orchestrator = Parallel.Orchestrator.Join
    )
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new FailTask[String]())
    parallel.addChild(new SuccessTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.FAILED)
  }

  test("Parallel Join + Selector: one succeeds -> SUCCEEDED") {
    val parallel = new Parallel[String](
      policy = Parallel.Policy.Selector,
      orchestrator = Parallel.Orchestrator.Join
    )
    parallel.addChild(new FailTask[String]())
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new FailTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  test("Parallel Join + Selector: all fail -> FAILED") {
    val parallel = new Parallel[String](
      policy = Parallel.Policy.Selector,
      orchestrator = Parallel.Orchestrator.Join
    )
    parallel.addChild(new FailTask[String]())
    parallel.addChild(new FailTask[String]())
    parallel.addChild(new FailTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.FAILED)
  }

  test("Parallel Join + Sequence: running child keeps parallel RUNNING") {
    val parallel = new Parallel[String](
      policy = Parallel.Policy.Sequence,
      orchestrator = Parallel.Orchestrator.Join
    )
    parallel.addChild(new SuccessTask[String]())
    parallel.addChild(new RunningTask[String]())
    parallel.addChild(new SuccessTask[String]())

    val bt = makeTree(parallel, "bb")
    bt.step()
    assertEquals(bt.getStatus, Task.Status.RUNNING)
  }

  // ── Parallel multi-step tests ──────────────────────────────────────────

  test("Parallel Resume + Sequence: multi-step execution tracking") {
    val task1 = new MutableStatusTask[String]()
    val task2 = new MutableStatusTask[String]()
    val tasks = DynamicArray[Task[String]]()
    tasks.add(task1)
    tasks.add(task2)
    val parallel = new Parallel[String](Parallel.Policy.Sequence, Parallel.Orchestrator.Resume, tasks)
    val bt       = makeTree(parallel, "bb")

    // Step 1: both tasks running
    bt.step()
    assertEquals(task1.executions, 1)
    assertEquals(task2.executions, 1)
    assertEquals(parallel.getStatus, Task.Status.RUNNING)

    // Step 2: task2 succeeds, but task1 still running -> RUNNING
    task2.nextStatus = Task.Status.SUCCEEDED
    bt.step()
    assertEquals(task1.executions, 2)
    assertEquals(task2.executions, 2)
    assertEquals(parallel.getStatus, Task.Status.RUNNING)

    // Step 3: task2 reverts to running (Resume re-runs all), task1 still running
    bt.step()
    assertEquals(task1.executions, 3)
    assertEquals(task2.executions, 3)
    assertEquals(parallel.getStatus, Task.Status.RUNNING)

    // Step 4: task1 now succeeds too -> all succeeded -> SUCCEEDED
    task1.nextStatus = Task.Status.SUCCEEDED
    bt.step()
    assertEquals(task1.executions, 4)
    assertEquals(task2.executions, 4)
    assertEquals(parallel.getStatus, Task.Status.SUCCEEDED)
  }

  test("Parallel Resume + Selector: multi-step execution tracking") {
    val task1 = new MutableStatusTask[String]()
    val task2 = new MutableStatusTask[String]()
    val tasks = DynamicArray[Task[String]]()
    tasks.add(task1)
    tasks.add(task2)
    val parallel = new Parallel[String](Parallel.Policy.Selector, Parallel.Orchestrator.Resume, tasks)
    val bt       = makeTree(parallel, "bb")

    // Step 1: both tasks running
    bt.step()
    assertEquals(task1.executions, 1)
    assertEquals(task2.executions, 1)
    assertEquals(parallel.getStatus, Task.Status.RUNNING)

    // Step 2: still running
    bt.step()
    assertEquals(task1.executions, 2)
    assertEquals(task2.executions, 2)
    assertEquals(parallel.getStatus, Task.Status.RUNNING)

    // Step 3: task1 succeeds -> Selector succeeds immediately, task2 not run
    task1.nextStatus = Task.Status.SUCCEEDED
    bt.step()
    assertEquals(task1.executions, 3)
    assertEquals(task2.executions, 2)
    assertEquals(parallel.getStatus, Task.Status.SUCCEEDED)

    // Step 4: Resume restarts all; task1 still set to succeed
    bt.step()
    assertEquals(task1.executions, 4)
    assertEquals(task2.executions, 2)
    assertEquals(parallel.getStatus, Task.Status.SUCCEEDED)
  }

  test("Parallel Join + Sequence: completed tasks do not re-execute") {
    val task1 = new MutableStatusTask[String]()
    val task2 = new MutableStatusTask[String]()
    val tasks = DynamicArray[Task[String]]()
    tasks.add(task1)
    tasks.add(task2)
    val parallel = new Parallel[String](Parallel.Policy.Sequence, Parallel.Orchestrator.Join, tasks)
    val bt       = makeTree(parallel, "bb")

    // Step 1: both tasks running
    bt.step()
    assertEquals(task1.executions, 1)
    assertEquals(task2.executions, 1)
    assertEquals(parallel.getStatus, Task.Status.RUNNING)

    // Step 2: task1 succeeds
    task1.nextStatus = Task.Status.SUCCEEDED
    bt.step()
    assertEquals(task1.executions, 2)
    assertEquals(task2.executions, 2)
    assertEquals(parallel.getStatus, Task.Status.RUNNING)

    // Step 3: Join strategy - task1 already succeeded, will not execute again
    bt.step()
    assertEquals(task1.executions, 2)
    assertEquals(task2.executions, 3)
    assertEquals(parallel.getStatus, Task.Status.RUNNING)

    // Step 4: task2 also succeeds -> both done -> SUCCEEDED
    task2.nextStatus = Task.Status.SUCCEEDED
    bt.step()
    assertEquals(task1.executions, 2)
    assertEquals(task2.executions, 4)
    assertEquals(parallel.getStatus, Task.Status.SUCCEEDED)

    // Step 5: after parallel completes, reset and re-run
    task1.nextStatus = Task.Status.RUNNING
    task2.nextStatus = Task.Status.RUNNING
    bt.step()
    assertEquals(task1.executions, 3)
    assertEquals(task2.executions, 5)
    assertEquals(parallel.getStatus, Task.Status.RUNNING)
  }
}
