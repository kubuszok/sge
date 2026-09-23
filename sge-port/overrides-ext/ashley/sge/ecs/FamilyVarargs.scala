/*
 * Varargs bridge for the ecs drop-in gate: the engine emits java's `Class<? extends Component>...`
 * as `Array[Class[? <: Component]]`, and sge's hand-written tests call these with individual
 * arguments (`Family.all(classOf[A], classOf[B])`), which is varargs syntax. */
package sge.ecs

extension (f: sge.ecs.Family.type) {
  /** Varargs overload of `Family.all` for sge drop-in parity. */
  def all(componentTypes: java.lang.Class[? <: sge.ecs.Component]*): sge.ecs.Family.Builder =
    f.all(componentTypes.toArray)

  /** Varargs overload of `Family.one` for sge drop-in parity. */
  def one(componentTypes: java.lang.Class[? <: sge.ecs.Component]*): sge.ecs.Family.Builder =
    f.one(componentTypes.toArray)

  /** Varargs overload of `Family.exclude` for sge drop-in parity. */
  def exclude(componentTypes: java.lang.Class[? <: sge.ecs.Component]*): sge.ecs.Family.Builder =
    f.exclude(componentTypes.toArray)
}

extension (b: sge.ecs.Family.Builder) {
  /** Varargs overload of `Builder.all` for sge drop-in parity. */
  def all(componentTypes: java.lang.Class[? <: sge.ecs.Component]*): sge.ecs.Family.Builder =
    b.all(componentTypes.toArray)

  /** Varargs overload of `Builder.one` for sge drop-in parity. */
  def one(componentTypes: java.lang.Class[? <: sge.ecs.Component]*): sge.ecs.Family.Builder =
    b.one(componentTypes.toArray)

  /** Varargs overload of `Builder.exclude` for sge drop-in parity. */
  def exclude(componentTypes: java.lang.Class[? <: sge.ecs.Component]*): sge.ecs.Family.Builder =
    b.exclude(componentTypes.toArray)
}

extension (ct: sge.ecs.ComponentType.type) {
  /** Varargs overload of `ComponentType.getBitsFor` for sge drop-in parity. */
  def getBitsFor(componentTypes: java.lang.Class[? <: sge.ecs.Component]*): scala.collection.mutable.BitSet =
    ct.getBitsFor(componentTypes.toArray)
}
