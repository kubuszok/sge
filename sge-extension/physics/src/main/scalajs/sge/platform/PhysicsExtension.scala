/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: Scala.js extension that loads the Rapier2D WASM module
 *   Idiom: split packages
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 75
 * Covenant-baseline-methods: PhysicsExtension,RapierModule,get,isLoaded,load,name,obtainModule,rapier
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-17
 */
package sge
package platform

import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits.thenable2future
import scala.concurrent.Future

/** Holds the initialized Rapier2D JS module so [[PhysicsOpsJs]] can read it.
  *
  * The module is bound through `js.Dynamic` facades (no `@JSImport`, no bundler — the SGE build is bundler-free and uses `ModuleKind.NoModule`). [[PhysicsExtension.load]] populates [[rapier]] exactly once.
  */
private[platform] object RapierModule {

  /** The initialized `RAPIER` namespace object (the resolved value of the compat module after `RAPIER.init()`), or `undefined` until [[PhysicsExtension.load]] resolves. */
  private[platform] var rapier: js.Dynamic = js.undefined.asInstanceOf[js.Dynamic]

  /** True once the WASM module has been initialized. */
  private[platform] def isLoaded: Boolean = !js.isUndefined(rapier)

  /** Returns the initialized module, or throws a clear error if [[PhysicsExtension.load]] has not completed yet. */
  private[platform] def get: js.Dynamic =
    if (isLoaded) rapier
    else
      throw new IllegalStateException(
        "Rapier2D WASM module is not loaded yet. Await PhysicsExtension.load() before using the physics API on Scala.js."
      )
}

/** SGE extension that asynchronously loads the Rapier2D WASM physics backend.
  *
  * On Scala.js the Rapier `@dimforge/rapier2d-compat` build inlines its WASM as base64 and exposes an async `init()` that must complete before any physics call. This extension resolves the module in
  * BOTH supported environments:
  *
  *   - Browser: a vendored Rapier script (wired by packaging) sets a global `RAPIER`; we use it directly.
  *   - Node / jsdom (the unit-test jsEnv): no global exists, so we `require("@dimforge/rapier2d-compat")`.
  *
  * After obtaining the module namespace we call `RAPIER.init()` (a JS `Promise`) and store the resolved module in [[RapierModule]].
  */
object PhysicsExtension extends sge.SgeExtension {

  override def name: String = "physics"

  override def load()(using sge.Sge): Future[Unit] = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    if (RapierModule.isLoaded) Future.unit
    else {
      val module = obtainModule
      // `RAPIER.init()` returns a JS Promise that resolves once the inlined WASM is compiled+instantiated.
      val initPromise = module.init().asInstanceOf[js.Thenable[Any]]
      initPromise.map { _ =>
        RapierModule.rapier = module
        ()
      }
    }
  }

  /** Obtains the Rapier module namespace: prefer a pre-existing browser global, else `require` it (node/jsdom).
    *
    * Note: `js.Dynamic.global` may only appear as the left-hand side of a `.`-selection (Scala.js global-scope rule), so each access selects a member directly rather than binding the global object to a
    * value.
    */
  private def obtainModule: js.Dynamic =
    if (js.typeOf(js.Dynamic.global.RAPIER) != "undefined") js.Dynamic.global.RAPIER
    else js.Dynamic.global.require("@dimforge/rapier2d-compat")
}
