/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/signals/Listener.java
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.signals` -> `sge.ecs.signals`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs
package signals

/** A simple Listener interface used to listen to a [[Signal]].
  *
  * @author
  *   Stefan Bachmann (original implementation)
  */
trait Listener[A] {

  /** @param signal The Signal that triggered event
    * @param obj The object passed on dispatch
    */
  def receive(signal: Signal[A], obj: A): Unit
}
