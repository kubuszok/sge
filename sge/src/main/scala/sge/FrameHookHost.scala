/*
 * SGE-original (no LibGDX equivalent).
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Licensed under the Apache License, Version 2.0
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 57
 * Covenant-baseline-methods: FrameHookHost,_,_frameHooks,i,registerFrameHook,removeFrameHook,runFrameHooks,snapshot
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-14
 */
package sge

/** Registry of per-frame callbacks driven once per frame by a host (the running [[Application]]).
  *
  * SGE-original addition (no LibGDX equivalent): LibGDX subsystems such as gdx-controllers are event/callback driven, whereas SGE went polling-based. A polling subsystem needs a guaranteed per-frame
  * tick. This registry lets such subsystems (e.g. the controllers extension) register a callback that the host invokes exactly once per frame, at the top of the frame (after input pollEvents, before
  * the [[ApplicationListener]]'s render()), so the game observes fresh polled state that same frame.
  *
  * The registry is intentionally concrete so every [[Application]] implementation inherits identical, leak-free semantics; each concrete main loop only has to call [[runFrameHooks]] once per frame.
  * Factoring it out of [[Application]] also gives polling subsystems a small, fully-concrete capability to depend on and to drive in tests, without constructing a full [[Application]].
  */
trait FrameHookHost {

  private val _frameHooks: scala.collection.mutable.ArrayBuffer[() => Unit] =
    scala.collection.mutable.ArrayBuffer.empty

  /** Registers a callback invoked once per frame at the top of the frame (after input pollEvents, before {@link ApplicationListener#render}). SGE-original: replaces LibGDX's event-callback model for
    * polling-based subsystems such as the controllers extension. Registering the same hook instance twice registers it twice; pair every {@link registerFrameHook} with a {@link removeFrameHook} to
    * avoid leaks.
    *
    * @param hook
    *   the per-frame callback
    */
  def registerFrameHook(hook: () => Unit): Unit = _frameHooks.synchronized {
    _frameHooks += hook
  }

  /** Removes a previously registered per-frame hook. Removes a single occurrence of the given hook instance; no-op if it was never registered.
    *
    * @param hook
    *   the per-frame callback to remove
    */
  def removeFrameHook(hook: () => Unit): Unit = _frameHooks.synchronized {
    val _ = _frameHooks -= hook
  }

  /** Invokes every registered per-frame hook once, in registration order. Concrete hosts call this once per frame at the top of the frame. Iterates over a snapshot so a hook may safely
    * register/remove hooks during invocation.
    */
  protected[sge] def runFrameHooks(): Unit = {
    val snapshot = _frameHooks.synchronized {
      _frameHooks.toArray
    }
    var i = 0
    while (i < snapshot.length) {
      snapshot(i)()
      i += 1
    }
  }
}
