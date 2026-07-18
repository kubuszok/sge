/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Compile-shape red test — ISS-847 (minor), wave 2026-07-18-G, territory G4.
 * Reproducer-authored: MUST NOT be modified by the fixer.
 *
 * DEFECT. The ObjLoader no-arg constructor threads a resolver through a
 * NON-Nullable parameter by force-unwrapping an empty Nullable:
 *
 *   // ObjLoader.scala:77-79
 *   @scala.annotation.nowarn("msg=deprecated") // Java interop: ModelLoader base class accepts null resolver
 *   def this()(using Sge) =
 *     this(Nullable.empty[FileHandleResolver].orNull)
 *
 * i.e. it manufactures a raw `null` (via the deprecated `orNull`) and pushes it
 * through `ObjLoader(resolver: FileHandleResolver)` — an orNull smell, and the
 * only reason the `@nowarn("msg=deprecated")` exists.
 *
 * The original (com/badlogic/gdx/graphics/g3d/loader/ObjLoader.java, lines
 * 89-95) is:
 *   public ObjLoader ()              { this(null); }
 *   public ObjLoader (FileHandleResolver resolver) { super(resolver); }
 * The no-arg ctor passes `null` straight to `super(resolver)`, which flows up
 * AsynchronousAssetLoader -> AssetLoader, where the resolver FIELD is nullable
 * (AssetLoader.java:31 `private FileHandleResolver resolver;`, dereferenced only
 * in `resolve(String)` = `resolver.resolve(fileName)`, ObjLoader never calling
 * `resolve` on itself).
 *
 * FAITHFUL Nullable-typed chain: type the resolver as
 * Nullable[FileHandleResolver] end to end
 *   AssetLoader          (resolver: Nullable[FileHandleResolver]) — field + resolve() -> resolver.get.resolve(name)
 *   AsynchronousAssetLoader / SynchronousAssetLoader (resolver: Nullable[FileHandleResolver]) — pass through
 *   ModelLoader          (resolver: Nullable[FileHandleResolver]) — pass through
 *   ObjLoader            (resolver: Nullable[FileHandleResolver]) — no-arg = this(Nullable.empty)
 * so the no-arg ctor needs neither orNull nor @nowarn, and `resolve()` on a
 * no-arg loader raises NPE via Nullable.get (mirroring Java's NPE on a null
 * resolver). Concrete-resolver callers stay source-compatible through the
 * `given Conversion[A, Nullable[A]]` (e.g. AssetManager.scala:99
 * `ObjLoader(resolver)`), so the fix is non-breaking.
 *
 * FORM: COMPILE-LEVEL assertion (typeCheckErrors). The load-bearing red pins
 * that ObjLoader's ctor accepts `Nullable[FileHandleResolver]` directly. Today
 * the primary param is a non-Nullable FileHandleResolver, and `Nullable[A]` does
 * not convert down to `A`, so the snippet fails to type-check (RED). Once the
 * chain carries Nullable it type-checks (GREEN). A `given Sge` is in scope, and
 * the control test proves the mechanism is not merely rejecting a broken snippet.
 */
package sge
package graphics
package g3d
package loader

import scala.compiletime.testing.*

import sge.assets.loaders.FileHandleResolver
import sge.files.{ FileHandle, FileType }

class ObjLoaderResolverIss847RedSuite extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  // A concrete resolver, used only by the control snippet below.
  private val concreteResolver: FileHandleResolver = new FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      FileHandle(new java.io.File(fileName), FileType.Absolute)
  }

  test(
    "ISS-847: ObjLoader ctor must accept Nullable[FileHandleResolver] so the no-arg ctor needs no orNull (ObjLoader.java:89-95)"
  ) {
    // TARGET: ObjLoader(Nullable.empty[FileHandleResolver]) type-checks — the
    // faithful no-null chain types the resolver as Nullable[FileHandleResolver]
    // through AssetLoader -> AsynchronousAssetLoader -> ModelLoader -> ObjLoader.
    // CURRENT: the primary ctor param is a non-Nullable FileHandleResolver and
    // Nullable[A] does not convert down to A, so this fails to type-check, which
    // is why the port force-unwraps with Nullable.empty.orNull (ObjLoader.scala:77-79).
    val errors: List[Error] = typeCheckErrors(
      "sge.graphics.g3d.loader.ObjLoader(lowlevel.Nullable.empty[sge.assets.loaders.FileHandleResolver])"
    )
    assert(
      errors.isEmpty,
      s"ObjLoader must accept Nullable[FileHandleResolver] (faithful no-null ctor chain, ObjLoader.java:89-95); " +
        s"today the param is a non-Nullable FileHandleResolver, forcing Nullable.empty.orNull + @nowarn. Errors: ${errors.map(_.message)}"
    )
  }

  test("ISS-847 (control): a concrete FileHandleResolver still constructs an ObjLoader") {
    // Passes on both the current tree and the fixed version: today via the exact
    // FileHandleResolver param, and after the fix via the given Conversion
    // [A, Nullable[A]]. Proves the `given Sge` is in scope and typeCheckErrors is
    // not just rejecting an ill-formed snippet — so the red above is a genuine
    // resolver-type mismatch, not a false red.
    val errors: List[Error] = typeCheckErrors(
      "sge.graphics.g3d.loader.ObjLoader(concreteResolver)"
    )
    assert(errors.isEmpty, s"ObjLoader(concreteResolver) must stay constructible; errors: ${errors.map(_.message)}")
  }

  // ISS-851: zinc-visible dependency anchor. The compile-shape assertion above
  // exercises ObjLoader's ctor ONLY inside a typeCheckErrors string literal, which
  // zinc's incremental compiler cannot see — so a change to that ctor's signature
  // would NOT recompile this suite, leaving a stale pass (the wave-G false-green
  // trap). A bare `classOf[ObjLoader]` is insufficient: zinc name-hashing only
  // invalidates dependents that use the *changed member's* name, and the ctor is
  // referenced solely in the string. This anchor therefore MIRRORS the asserted
  // surface in real code — `ObjLoader(Nullable.empty[FileHandleResolver])`, the
  // exact ctor the primary assertion pins — so any regression of that ctor's
  // parameter type invalidates this method's body and recompiles the suite
  // (turning the stale pass into a compile error / re-evaluated assertion). It is
  // a type-level reference in an uncalled method: never executed, no side effects.
  def zincAnchor: ObjLoader = ObjLoader(lowlevel.Nullable.empty[FileHandleResolver])
}
