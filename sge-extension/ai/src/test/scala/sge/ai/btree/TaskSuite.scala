package sge
package ai
package btree

import sge.utils.Nullable

class TaskSuite extends munit.FunSuite {

  // ── Status enum ──────────────────────────────────────────────────────

  test("Status enum has exactly 5 values") {
    val values = Task.Status.values
    assertEquals(values.length, 5)
  }

  test("Status enum values are FRESH, RUNNING, FAILED, SUCCEEDED, CANCELLED") {
    assertEquals(Task.Status.FRESH.toString, "FRESH")
    assertEquals(Task.Status.RUNNING.toString, "RUNNING")
    assertEquals(Task.Status.FAILED.toString, "FAILED")
    assertEquals(Task.Status.SUCCEEDED.toString, "SUCCEEDED")
    assertEquals(Task.Status.CANCELLED.toString, "CANCELLED")
  }

  // ── Initial state ────────────────────────────────────────────────────

  test("new task starts FRESH") {
    val task = new SuccessTask[String]()
    assertEquals(task.getStatus, Task.Status.FRESH)
  }

  // ── Lifecycle: init, start, end ──────────────────────────────────────

  test("start() is called before run() in behavior tree step") {
    var startCalled = false
    val task        = new LeafTask[String] {
      override def start():   Unit        = startCalled = true
      override def execute(): Task.Status = {
        assert(startCalled, "start() should be called before execute()")
        Task.Status.SUCCEEDED
      }
      override def newInstance():                        Task[String] = throw new UnsupportedOperationException
      override protected def copyTo(task: Task[String]): Task[String] = task
    }
    val bt = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    bt.step()
    assert(startCalled, "start() should have been called")
    assertEquals(bt.getStatus, Task.Status.SUCCEEDED)
  }

  test("end() is called when task succeeds") {
    var endCalled = false
    val task      = new LeafTask[String] {
      override def end():                                Unit         = endCalled = true
      override def execute():                            Task.Status  = Task.Status.SUCCEEDED
      override def newInstance():                        Task[String] = throw new UnsupportedOperationException
      override protected def copyTo(task: Task[String]): Task[String] = task
    }
    val bt = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    bt.step()
    assert(endCalled, "end() should have been called on success")
  }

  test("end() is called when task fails") {
    var endCalled = false
    val task      = new LeafTask[String] {
      override def end():                                Unit         = endCalled = true
      override def execute():                            Task.Status  = Task.Status.FAILED
      override def newInstance():                        Task[String] = throw new UnsupportedOperationException
      override protected def copyTo(task: Task[String]): Task[String] = task
    }
    val bt = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    bt.step()
    assert(endCalled, "end() should have been called on failure")
  }

  test("end() is not called when task is running") {
    var endCalled = false
    val task      = new LeafTask[String] {
      override def end():                                Unit         = endCalled = true
      override def execute():                            Task.Status  = Task.Status.RUNNING
      override def newInstance():                        Task[String] = throw new UnsupportedOperationException
      override protected def copyTo(task: Task[String]): Task[String] = task
    }
    val bt = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    bt.step()
    assert(!endCalled, "end() should not be called when task is running")
  }

  // ── Status transitions ───────────────────────────────────────────────

  test("task transitions from FRESH to SUCCEEDED") {
    val task = new SuccessTask[String]()
    val bt   = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    assertEquals(task.getStatus, Task.Status.FRESH)
    bt.step()
    assertEquals(task.getStatus, Task.Status.SUCCEEDED)
  }

  test("task transitions from FRESH to FAILED") {
    val task = new FailTask[String]()
    val bt   = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    assertEquals(task.getStatus, Task.Status.FRESH)
    bt.step()
    assertEquals(task.getStatus, Task.Status.FAILED)
  }

  test("task transitions from FRESH to RUNNING") {
    val task = new RunningTask[String]()
    val bt   = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    assertEquals(task.getStatus, Task.Status.FRESH)
    bt.step()
    assertEquals(task.getStatus, Task.Status.RUNNING)
  }

  test("task transitions from RUNNING to SUCCEEDED on subsequent step") {
    val task = new MutableStatusTask[String]()
    task.nextStatus = Task.Status.RUNNING
    val bt = new BehaviorTree[String](Nullable(task), Nullable("bb"))

    bt.step()
    assertEquals(task.getStatus, Task.Status.RUNNING)

    task.nextStatus = Task.Status.SUCCEEDED
    bt.step()
    assertEquals(task.getStatus, Task.Status.SUCCEEDED)
  }

  test("task transitions from RUNNING to FAILED on subsequent step") {
    val task = new MutableStatusTask[String]()
    task.nextStatus = Task.Status.RUNNING
    val bt = new BehaviorTree[String](Nullable(task), Nullable("bb"))

    bt.step()
    assertEquals(task.getStatus, Task.Status.RUNNING)

    task.nextStatus = Task.Status.FAILED
    bt.step()
    assertEquals(task.getStatus, Task.Status.FAILED)
  }

  // ── Reset ────────────────────────────────────────────────────────────

  test("resetTask() returns task to FRESH") {
    val task = new SuccessTask[String]()
    val bt   = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    bt.step()
    assertEquals(task.getStatus, Task.Status.SUCCEEDED)

    task.resetTask()
    assertEquals(task.getStatus, Task.Status.FRESH)
  }

  test("resetTask() cancels running task before resetting") {
    var endCalled = false
    val task      = new LeafTask[String] {
      override def end():                                Unit         = endCalled = true
      override def execute():                            Task.Status  = Task.Status.RUNNING
      override def newInstance():                        Task[String] = throw new UnsupportedOperationException
      override protected def copyTo(task: Task[String]): Task[String] = task
    }
    val bt = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    bt.step()
    assertEquals(task.getStatus, Task.Status.RUNNING)

    task.resetTask()
    assert(endCalled, "end() should be called during cancel")
    assertEquals(task.getStatus, Task.Status.FRESH)
  }

  test("resetTask() on FRESH task is a no-op (stays FRESH)") {
    val task = new SuccessTask[String]()
    assertEquals(task.getStatus, Task.Status.FRESH)
    task.resetTask()
    assertEquals(task.getStatus, Task.Status.FRESH)
  }

  // ── Cancel ───────────────────────────────────────────────────────────

  test("cancel() sets status to CANCELLED") {
    val task = new RunningTask[String]()
    val bt   = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    bt.step()
    assertEquals(task.getStatus, Task.Status.RUNNING)

    task.cancel()
    assertEquals(task.getStatus, Task.Status.CANCELLED)
  }

  test("cancel() calls end()") {
    var endCalled = false
    val task      = new LeafTask[String] {
      override def end():                                Unit         = endCalled = true
      override def execute():                            Task.Status  = Task.Status.RUNNING
      override def newInstance():                        Task[String] = throw new UnsupportedOperationException
      override protected def copyTo(task: Task[String]): Task[String] = task
    }
    val bt = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    bt.step()
    task.cancel()
    assert(endCalled, "end() should be called by cancel()")
  }

  // ── LeafTask child management ────────────────────────────────────────

  test("LeafTask getChildCount is 0") {
    val task = new SuccessTask[String]()
    assertEquals(task.getChildCount, 0)
  }

  test("LeafTask addChild throws") {
    val task = new SuccessTask[String]()
    interceptMessage[IllegalStateException]("A leaf task cannot have any children") {
      task.addChild(new SuccessTask[String]())
    }
  }

  test("LeafTask getChild throws") {
    val task = new SuccessTask[String]()
    intercept[IndexOutOfBoundsException] {
      task.getChild(0)
    }
  }

  // ── setControl ───────────────────────────────────────────────────────

  test("setControl sets the parent task") {
    val parent = new SuccessTask[String]()
    val child  = new SuccessTask[String]()
    // setControl requires parent to have tree set, so use BehaviorTree as parent
    val bt = new BehaviorTree[String](Nullable(parent), Nullable("bb"))
    child.setControl(bt)
    // After setControl, getObject should work because tree is set
    assertEquals(child.getObject, "bb")
  }

  // ── getObject ────────────────────────────────────────────────────────

  test("getObject throws when task has never run") {
    val task = new SuccessTask[String]()
    intercept[IllegalStateException] {
      task.getObject
    }
  }

  test("getObject returns blackboard after task has run") {
    val task = new SuccessTask[String]()
    val bt   = new BehaviorTree[String](Nullable(task), Nullable("hello"))
    bt.step()
    assertEquals(task.getObject, "hello")
  }

  // ── cloneTask ────────────────────────────────────────────────────────

  test("cloneTask creates independent copy") {
    val task = new CountingTask[String](3)

    val clone = task.cloneTask()
    assert(clone ne task, "clone should be a different instance")
    assertEquals(clone.asInstanceOf[CountingTask[String]].succeedOn, 3)
    assertEquals(clone.getStatus, Task.Status.FRESH)
  }

  test("cloneTask clones guard") {
    val task = new SuccessTask[String]()
    task.guard = Nullable(new FailTask[String]())

    val clone = task.cloneTask()
    assert(clone.guard.isDefined, "clone should have a guard")
    assert(clone.guard.get ne task.guard.get, "guard should be independently cloned")
  }

  // ── Pool.Poolable reset ──────────────────────────────────────────────

  test("reset() clears control, guard, status, tree") {
    val task = new SuccessTask[String]()
    val bt   = new BehaviorTree[String](Nullable(task), Nullable("bb"))
    bt.step()
    task.guard = Nullable(new FailTask[String]())

    task.reset()
    assertEquals(task.getStatus, Task.Status.FRESH)
    assert(task.guard.isEmpty, "guard should be cleared")
  }
}
