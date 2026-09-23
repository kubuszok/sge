/*
 * Ported from gdx-ai — https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/utils/BehaviorTreeParser.java (the reflective half)
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 */

/** INJECTED — the REDIRECT TARGET for `com.badlogic.gdx.utils.reflect.Field`
  * (`TypeRedirectTransform`, see `GdxAiPolicy`): the libGDX base drops that type outright since
  * runtime reflection is unportable, so `DefaultBehaviorTreeReader`'s three `reflect.Field`
  * signatures re-point here instead of being cut. It carries what a `Field` gave the parser —
  * coercing a value to the field's static type and storing it — as closures set at registration. */
package sge.ai.btree.utils

/** One `@TaskAttribute` slot of a task class, resolved WITHOUT reflection. */
final class TaskField private[utils] (
    fieldName: String,
    typeName: String,
    coerce: (java.lang.Object, sge.ai.btree.utils.DistributionAdapters) => java.lang.Object,
    assign: (java.lang.Object, java.lang.Object) => Unit,
) {

  /** java's `Field#getName()`. */
  def getName(): String = fieldName

  /** java's `field.getType().getSimpleName()`, precomputed — the port has no `Class` to ask. */
  def getTypeName(): String = typeName

  /** java's `castValue(field, value)` for this one field: the coerced value, or `null` where the
    * parsed value is not assignable to it. `null` and not an `Option` — this is java's own protocol
    * and `setField` tests it exactly as java did. */
  def cast(value: java.lang.Object, adapters: sge.ai.btree.utils.DistributionAdapters): java.lang.Object =
    coerce(value, adapters)

  /** java's `field.set(task, valueObject)`. */
  def set(task: java.lang.Object, value: java.lang.Object): Unit = assign(task, value)
}
