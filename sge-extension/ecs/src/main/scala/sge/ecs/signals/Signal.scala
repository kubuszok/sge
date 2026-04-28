/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/signals/Signal.java
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.signals` -> `sge.ecs.signals`
 *   Convention: split packages
 *   Idiom: ArrayBuffer with snapshot copy during dispatch (replaces SnapshotArray)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 63
 * Covenant-baseline-methods: Signal,add,dispatch,i,listeners,remove,removeAllListeners,snapshot
 * Covenant-source-reference: com/badlogic/ashley/signals/Signal.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d63d542228cd8c62cc2f7adf20055b0ac59a547e
 */
package sge
package ecs
package signals

import scala.collection.mutable.ArrayBuffer

/** A Signal is a basic event class that can dispatch an event to multiple listeners. It uses generics to allow any type of object to be passed around on dispatch.
  *
  * @author
  *   Stefan Bachmann (original implementation)
  */
class Signal[A] {

  private val listeners: ArrayBuffer[Listener[A]] = ArrayBuffer.empty

  /** Add a Listener to this Signal.
    * @param listener
    *   The Listener to be added
    */
  def add(listener: Listener[A]): Unit =
    listeners += listener

  /** Remove a listener from this Signal.
    * @param listener
    *   The Listener to remove
    */
  def remove(listener: Listener[A]): Unit =
    listeners -= listener

  /** Removes all listeners attached to this [[Signal]]. */
  def removeAllListeners(): Unit =
    listeners.clear()

  /** Dispatches an event to all Listeners registered to this Signal.
    *
    * Takes a snapshot (copy) of the listener list before iterating, so that listeners can safely add/remove themselves during dispatch.
    *
    * @param obj
    *   The object to send off
    */
  def dispatch(obj: A): Unit = {
    // Snapshot: copy the current listener list to allow safe modification during dispatch
    val snapshot = listeners.toArray
    var i        = 0
    while (i < snapshot.length) {
      snapshot(i).receive(this, obj)
      i += 1
    }
  }
}
