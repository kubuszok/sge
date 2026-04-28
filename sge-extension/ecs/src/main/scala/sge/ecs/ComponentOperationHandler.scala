/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/ComponentOperationHandler.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: enum instead of inner enum class
 *   Idiom: ArrayBuffer instead of Array + Pool for operations
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 64
 * Covenant-baseline-methods: ComponentOperation,ComponentOperationHandler,Type,add,hasOperationsToProcess,i,operations,processOperations,remove
 * Covenant-source-reference: com/badlogic/ashley/core/ComponentOperationHandler.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d63d542228cd8c62cc2f7adf20055b0ac59a547e
 */
package sge
package ecs

import scala.collection.mutable.ArrayBuffer

/** Handles delayed component add/remove operations during engine update processing.
  *
  * When the engine is updating systems, component operations are delayed and batched to avoid concurrent modification.
  */
private[ecs] class ComponentOperationHandler(delayed: () => Boolean) {

  private val operations: ArrayBuffer[ComponentOperation] = ArrayBuffer.empty

  def add(entity: Entity): Unit =
    if (delayed()) {
      operations += ComponentOperation(ComponentOperation.Type.Add, entity)
    } else {
      entity.notifyComponentAdded()
    }

  def remove(entity: Entity): Unit =
    if (delayed()) {
      operations += ComponentOperation(ComponentOperation.Type.Remove, entity)
    } else {
      entity.notifyComponentRemoved()
    }

  def hasOperationsToProcess: Boolean = operations.nonEmpty

  def processOperations(): Unit = {
    var i = 0
    while (i < operations.size) {
      val operation = operations(i)
      operation.opType match {
        case ComponentOperation.Type.Add    => operation.entity.notifyComponentAdded()
        case ComponentOperation.Type.Remove => operation.entity.notifyComponentRemoved()
      }
      i += 1
    }
    operations.clear()
  }
}

private[ecs] object ComponentOperation {
  enum Type {
    case Add, Remove
  }
}

final private[ecs] class ComponentOperation(val opType: ComponentOperation.Type, val entity: Entity)
