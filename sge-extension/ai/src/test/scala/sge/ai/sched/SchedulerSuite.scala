package sge
package ai
package sched

class SchedulerSuite extends munit.FunSuite {

  /** A simple schedulable that records how many times it has been run. */
  private class CountingSchedulable extends Schedulable {
    var runCount:      Int  = 0
    var lastTimeToRun: Long = 0L

    override def run(nanoTimeToRun: Long): Unit = {
      runCount += 1
      lastTimeToRun = nanoTimeToRun
    }
  }

  // ── LoadBalancingScheduler ───────────────────────────────────────────

  test("LoadBalancingScheduler: task with frequency 1 runs every frame") {
    val scheduler = new LoadBalancingScheduler(100)
    val task      = new CountingSchedulable()
    scheduler.add(task, 1, 0)

    // Run 5 frames
    for (_ <- 1 to 5)
      scheduler.run(1000000L)

    assertEquals(task.runCount, 5)
  }

  test("LoadBalancingScheduler: task with frequency 2 runs every other frame") {
    val scheduler = new LoadBalancingScheduler(100)
    val task      = new CountingSchedulable()
    scheduler.add(task, 2, 0)

    // Run 6 frames
    for (_ <- 1 to 6)
      scheduler.run(1000000L)

    // Frequency 2 means runs on frames where (frame + phase) % 2 == 0
    // frame starts at 1, phase is 0 -> frames 2, 4, 6 -> 3 times
    assertEquals(task.runCount, 3)
  }

  test("LoadBalancingScheduler: phase offsets task execution") {
    val scheduler = new LoadBalancingScheduler(100)
    val task      = new CountingSchedulable()
    scheduler.add(task, 2, 1) // phase 1

    // Run 4 frames
    for (_ <- 1 to 4)
      scheduler.run(1000000L)

    // (frame + phase) % frequency == 0
    // frame=1: (1+1)%2=0 -> runs; frame=2: (2+1)%2=1; frame=3: (3+1)%2=0 -> runs; frame=4: (4+1)%2=1
    assertEquals(task.runCount, 2)
  }

  test("LoadBalancingScheduler: multiple tasks with same frequency") {
    val scheduler = new LoadBalancingScheduler(100)
    val task1     = new CountingSchedulable()
    val task2     = new CountingSchedulable()
    scheduler.add(task1, 1, 0)
    scheduler.add(task2, 1, 0)

    scheduler.run(1000000L)

    assertEquals(task1.runCount, 1)
    assertEquals(task2.runCount, 1)
  }

  test("LoadBalancingScheduler: no tasks means no work") {
    val scheduler = new LoadBalancingScheduler(100)
    // Should not throw
    scheduler.run(1000000L)
  }

  test("LoadBalancingScheduler: addWithAutomaticPhasing calculates phase") {
    val scheduler = new LoadBalancingScheduler(100)
    val task1     = new CountingSchedulable()
    val task2     = new CountingSchedulable()

    scheduler.add(task1, 2, 0)
    scheduler.addWithAutomaticPhasing(task2, 2) // phase should be auto-calculated

    // Run several frames and verify both tasks get executed
    for (_ <- 1 to 10)
      scheduler.run(1000000L)

    assert(task1.runCount > 0, "task1 should have run")
    assert(task2.runCount > 0, "task2 should have run")
  }

  test("LoadBalancingScheduler: high frequency task runs less often") {
    val scheduler      = new LoadBalancingScheduler(100)
    val frequentTask   = new CountingSchedulable()
    val infrequentTask = new CountingSchedulable()
    scheduler.add(frequentTask, 1, 0) // every frame
    scheduler.add(infrequentTask, 5, 0) // every 5th frame

    for (_ <- 1 to 10)
      scheduler.run(1000000L)

    assertEquals(frequentTask.runCount, 10)
    assertEquals(infrequentTask.runCount, 2) // frames 5 and 10
  }

  // ── PriorityScheduler ───────────────────────────────────────────────

  test("PriorityScheduler: task with frequency 1 runs every frame") {
    val scheduler = new PriorityScheduler(100)
    val task      = new CountingSchedulable()
    scheduler.add(task, 1, 0)

    for (_ <- 1 to 5)
      scheduler.run(1000000L)

    assertEquals(task.runCount, 5)
  }

  test("PriorityScheduler: task with frequency 3 runs every 3rd frame") {
    val scheduler = new PriorityScheduler(100)
    val task      = new CountingSchedulable()
    scheduler.add(task, 3, 0)

    for (_ <- 1 to 9)
      scheduler.run(1000000L)

    // (frame + phase) % 3 == 0 -> frames 3, 6, 9 -> 3 times
    assertEquals(task.runCount, 3)
  }

  test("PriorityScheduler: add with default priority 1") {
    val scheduler = new PriorityScheduler(100)
    val task      = new CountingSchedulable()
    scheduler.add(task, 1, 0) // default priority should be 1

    scheduler.run(1000000L)
    assertEquals(task.runCount, 1)
  }

  test("PriorityScheduler: tasks with different priorities both run") {
    val scheduler    = new PriorityScheduler(100)
    val highPriority = new CountingSchedulable()
    val lowPriority  = new CountingSchedulable()
    scheduler.add(highPriority, 1, 0, 10.0f)
    scheduler.add(lowPriority, 1, 0, 1.0f)

    scheduler.run(1000000L)

    assertEquals(highPriority.runCount, 1)
    assertEquals(lowPriority.runCount, 1)
  }

  test("PriorityScheduler: addWithAutomaticPhasing with priority") {
    val scheduler = new PriorityScheduler(100)
    val task1     = new CountingSchedulable()
    val task2     = new CountingSchedulable()

    scheduler.add(task1, 2, 0, 1.0f)
    scheduler.addWithAutomaticPhasing(task2, 2, 2.0f)

    for (_ <- 1 to 10)
      scheduler.run(1000000L)

    assert(task1.runCount > 0, "task1 should have run")
    assert(task2.runCount > 0, "task2 should have run")
  }

  test("PriorityScheduler: addWithAutomaticPhasing without priority defaults to 1") {
    val scheduler = new PriorityScheduler(100)
    val task      = new CountingSchedulable()
    scheduler.addWithAutomaticPhasing(task, 1)

    scheduler.run(1000000L)
    assertEquals(task.runCount, 1)
  }

  // ── Scheduler as Schedulable (hierarchical scheduling) ──────────────

  test("LoadBalancingScheduler implements Schedulable interface") {
    val parentScheduler = new LoadBalancingScheduler(100)
    val childScheduler  = new LoadBalancingScheduler(100)
    val task            = new CountingSchedulable()

    childScheduler.add(task, 1, 0)
    parentScheduler.add(childScheduler, 1, 0)

    // Run the parent, which should run the child, which should run the task
    parentScheduler.run(1000000L)

    assertEquals(task.runCount, 1)
  }
}
