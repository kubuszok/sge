package sge
package ecs

class ComponentOperationHandlerSuite extends munit.FunSuite {

  private class ComponentA extends Component

  test("add notifies immediately when not delayed") {
    var notified = false
    val entity   = new Entity
    entity.componentAdded.add(new signals.Listener[Entity] {
      override def receive(signal: signals.Signal[Entity], obj: Entity): Unit =
        notified = true
    })
    val handler = new ComponentOperationHandler(() => false)
    entity.addInternal(new ComponentA)
    handler.add(entity)
    assert(notified, "Expected immediate notification when not delayed")
  }

  test("add defers notification when delayed") {
    var notified = false
    val entity   = new Entity
    entity.componentAdded.add(new signals.Listener[Entity] {
      override def receive(signal: signals.Signal[Entity], obj: Entity): Unit =
        notified = true
    })
    val handler = new ComponentOperationHandler(() => true)
    entity.addInternal(new ComponentA)
    handler.add(entity)
    assert(!notified, "Expected deferred notification when delayed")
    assert(handler.hasOperationsToProcess)
  }

  test("remove notifies immediately when not delayed") {
    var notified = false
    val entity   = new Entity
    entity.componentRemoved.add(new signals.Listener[Entity] {
      override def receive(signal: signals.Signal[Entity], obj: Entity): Unit =
        notified = true
    })
    val handler = new ComponentOperationHandler(() => false)
    handler.remove(entity)
    assert(notified, "Expected immediate notification when not delayed")
  }

  test("remove defers notification when delayed") {
    var notified = false
    val entity   = new Entity
    entity.componentRemoved.add(new signals.Listener[Entity] {
      override def receive(signal: signals.Signal[Entity], obj: Entity): Unit =
        notified = true
    })
    val handler = new ComponentOperationHandler(() => true)
    handler.remove(entity)
    assert(!notified, "Expected deferred notification when delayed")
    assert(handler.hasOperationsToProcess)
  }

  test("hasOperationsToProcess returns false when empty") {
    val handler = new ComponentOperationHandler(() => true)
    assert(!handler.hasOperationsToProcess)
  }

  test("processOperations dispatches deferred add") {
    var addNotified = false
    val entity      = new Entity
    entity.componentAdded.add(new signals.Listener[Entity] {
      override def receive(signal: signals.Signal[Entity], obj: Entity): Unit =
        addNotified = true
    })

    val handler = new ComponentOperationHandler(() => true)
    entity.addInternal(new ComponentA)
    handler.add(entity)
    assert(!addNotified)

    handler.processOperations()
    assert(addNotified, "Expected add notification after processOperations")
    assert(!handler.hasOperationsToProcess, "Operations should be cleared after processing")
  }

  test("processOperations dispatches deferred remove") {
    var removeNotified = false
    val entity         = new Entity
    entity.componentRemoved.add(new signals.Listener[Entity] {
      override def receive(signal: signals.Signal[Entity], obj: Entity): Unit =
        removeNotified = true
    })

    val handler = new ComponentOperationHandler(() => true)
    handler.remove(entity)
    assert(!removeNotified)

    handler.processOperations()
    assert(removeNotified, "Expected remove notification after processOperations")
    assert(!handler.hasOperationsToProcess, "Operations should be cleared after processing")
  }

  test("processOperations handles multiple operations in order") {
    val order  = scala.collection.mutable.ArrayBuffer[String]()
    val entity = new Entity
    entity.componentAdded.add(new signals.Listener[Entity] {
      override def receive(signal: signals.Signal[Entity], obj: Entity): Unit =
        order += "add"
    })
    entity.componentRemoved.add(new signals.Listener[Entity] {
      override def receive(signal: signals.Signal[Entity], obj: Entity): Unit =
        order += "remove"
    })

    val handler = new ComponentOperationHandler(() => true)
    entity.addInternal(new ComponentA)
    handler.add(entity)
    handler.remove(entity)
    assert(order.isEmpty)

    handler.processOperations()
    assertEquals(order.toList, List("add", "remove"))
  }

  test("processOperations clears operations so second call is no-op") {
    var count   = 0
    val entity  = new Entity
    entity.componentAdded.add(new signals.Listener[Entity] {
      override def receive(signal: signals.Signal[Entity], obj: Entity): Unit =
        count += 1
    })

    val handler = new ComponentOperationHandler(() => true)
    entity.addInternal(new ComponentA)
    handler.add(entity)
    handler.processOperations()
    assertEquals(count, 1)

    handler.processOperations()
    assertEquals(count, 1, "Second processOperations should be no-op")
  }

  test("delayed flag is re-evaluated per call") {
    var delayed     = false
    var addNotified = false
    val entity      = new Entity
    entity.componentAdded.add(new signals.Listener[Entity] {
      override def receive(signal: signals.Signal[Entity], obj: Entity): Unit =
        addNotified = true
    })

    val handler = new ComponentOperationHandler(() => delayed)

    // Not delayed: immediate
    entity.addInternal(new ComponentA)
    handler.add(entity)
    assert(addNotified)

    // Now switch to delayed
    addNotified = false
    delayed = true
    handler.add(entity)
    assert(!addNotified)
    assert(handler.hasOperationsToProcess)
  }
}
