package sge.port.ext.ashley

import balticporter.core.PortManifest

import java.nio.file.Path

/** Ashley's per-library policy -- a DEPENDENT of libGDX core's. The base's `dropTypes`/ `dropMethods`/`packageRenames`/signature-affecting phases are INHERITED, not restated. `inject` is NOT
  * inherited: exactly one module ships each replacement file.
  *
  * Rebased on the ladder (`LibgdxLadder.universal`): the caller supplies the core manifest (already extended by lls), with `baseReports` pointing at the report root where the core's port-map.tsv
  * lives. The ashley extension then extends that manifest with its own policy.
  */
object AshleyPolicy {

  /** Members called without parens: `BitSet`'s, where ashley's `Bits` is redirected, and the injected `ImmutableArray`'s, which spells them parenless as sge's API does. */
  val externalParenless: Set[String] =
    Set("scala.collection.mutable.BitSet#isEmpty") ++
      (for {
        owner <- Set("com.badlogic.ashley.utils.ImmutableArray", "sge.ecs.utils.ImmutableArray")
        m <- Set("size", "first", "peek", "iterator", "toArray")
      } yield s"$owner#$m")

  /** @param coreManifest
    *   the sge-core manifest (lls -> sge-core chain)
    * @param sgeRoot
    *   the sge repository root
    * @param reportRoots
    *   directories where the base ports' report trees live (port-map.tsv discovery)
    */
  def core(coreManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path] = Nil): PortManifest =
    coreManifest.extendedBy(
      PortManifest(
        name = "sge-ecs",
        governs = Set("com.badlogic.ashley"),
        // sge flattens Ashley's `core` package away into `sge.ecs`; libGDX's own
        // com.badlogic.gdx -> sge is INHERITED from the base, not restated.
        packageRenames = Map(
          "com.badlogic.ashley.core" -> "sge.ecs",
          "com.badlogic.ashley" -> "sge.ecs"
        ),
        // ImmutableArray wraps Array<T> and three of its methods delegate with a
        // non-literal boolean identity flag BoolDispatch cannot dispatch statically; its
        // `iterable` field references a nested type of the retargetted Array that no
        // longer exists. sge hand-writes the whole class -- drop and inject it.
        dropTypes = Set("com.badlogic.ashley.utils.ImmutableArray"),
        // `Bits#isEmpty()` calls land on the redirect target, whose `isEmpty` has no parens.
        externalParenless = AshleyPolicy.externalParenless,
        // Ashley's OWN replacements. `inject` is not inherited — exactly one module ships each
        // replacement file, and libGDX core ships the ones for the types IT dropped.
        inject = List(sgeRoot.resolve("sge-port/overrides-ext/ashley")),
        surface = List(
          // Ashley's types use java.util.Comparator (SortedIteratingSystem, SystemManager);
          // the base retargets Comparator -> Ordering within com.badlogic.gdx. This instance
          // widens the scope to cover com.badlogic.ashley. MergeablePolicy unions the scopes.
          new balticporter.transform.CollectionsTransform(
            scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.ashley")),
            retarget = Map("java.util.Comparator" -> "scala.math.Ordering")
          ),
          // The four Bits members ashley calls whose BitSet counterpart has another SHAPE, so no
          // rename can reach them; each template reads the receiver first, as java evaluates it.
          // Scoped like the redirect: core's own `Bits` calls keep their `Bits` spelling.
          new balticporter.transform.CallSiteSubstitutionTransform(
            List(
              // `a.containsAll(b)` is `b` a subset of `a`
              "com.badlogic.gdx.utils.Bits#containsAll(com.badlogic.gdx.utils.Bits)" ->
                "{{ val bpThis = {recv}; {arg0}.subsetOf(bpThis) }}",
              "com.badlogic.gdx.utils.Bits#intersects(com.badlogic.gdx.utils.Bits)" -> "({recv} & {arg0}).nonEmpty",
              // the highest set bit plus one, 0 when none is set
              "com.badlogic.gdx.utils.Bits#length()" -> "{recv}.lastOption.fold(0)(_ + 1)",
              // java answers -1 for a negative start; `iteratorFrom` would start at 0
              "com.badlogic.gdx.utils.Bits#nextSetBit(int)" ->
                "{{ val bpThis = {recv}; val bpFrom = {arg0}; if bpFrom < 0 then -1 else bpThis.iteratorFrom(bpFrom).nextOption().getOrElse(-1) }}"
            ).map((call, template) => balticporter.transform.CallSiteSubstitutionTransform.Entry(call, template, balticporter.tir.RuleScope.Only(Set("com.badlogic.ashley"))))
          ),
          // ASHLEY'S OWN BEAN PROPERTY PAIRS — merged into the base's `bean-properties` phase via
          // `MergeablePolicy`. `EntitySystem#engine` is a def-pair (runtime-assigned, so `val` is
          // wrong and `var` would publish a setter java never had). The
          // `getFamily`/`getInterval`/`getEntities` families stay as `def`s: the hand port
          // restructured them into CONSTRUCTOR PARAMETERS, which a mechanical port cannot reproduce.
          new balticporter.transform.BeanPropertyTransform(
            pairs = Map(
              // engine: kept as the port's STATED POLICY even though auto-detection would
              // also fire -- the hand port writes a def-pair, not a var; this also gives the
              // binder a `neverFired` line if the upstream accessor shape changes.
              "com.badlogic.ashley.core.EntitySystem#engine" -> "getEngine",
              // NO #entities pair: the hand port KEEPS getEntities (parenless) over a
              // PRIVATE field; collapsing would be the opposite of parity.
              "com.badlogic.ashley.core.ComponentType#index" -> "getIndex",
              "com.badlogic.ashley.core.Family#index" -> "getIndex",
              "com.badlogic.ashley.systems.IteratingSystem#family" -> "getFamily",
              "com.badlogic.ashley.systems.SortedIteratingSystem#family" -> "getFamily",
              "com.badlogic.ashley.systems.IntervalIteratingSystem#family" -> "getFamily",
              "com.badlogic.ashley.systems.IntervalSystem#interval" -> "getInterval",
              // 3.2g: arity-parity pairs (ecs drop-in) -- both bodies contain a comparison
              // (`> 0`, `== 0`), which NullaryArityTransform's SideEffectingBody guard
              // refuses; sge's hand port writes them parenless, so the getter-only
              // BeanPropertyTransform pair strips the () instead.
              "com.badlogic.ashley.core.ComponentOperationHandler#hasOperationsToProcess" -> "hasOperationsToProcess",
              // `isEmpty`: body is `size == 0` — same shape. sge: `def isEmpty: Boolean`.
              "com.badlogic.ashley.utils.Bag#isEmpty" -> "isEmpty"
            )
            // scope = Only(Set.empty) (the default): no auto-detection for ashley types.
            // All ashley bean pairs are listed explicitly above. The base (ladder) uses
            // Only(Set("com.badlogic.gdx")) for auto-detection; Only(Set.empty) unions cleanly.
            // Engine/Entity/EntityManager/SystemManager/Bag keep java-style getter names
            // because no auto-detection pair lists them.
          ),
          // PooledEngine.ComponentPools uses ReflectionPool as a TYPE (field, local, new,
          // cast target), so no body seam can reach it; the base drops it outright. Re-point
          // every reference at Ashley's own factory-backed pool instead.
          // Ashley's own bit sets are `mutable.BitSet` (sge's idiom, Entity/Family/FamilyManager);
          // core keeps `Bits`, so the redirect is confined to ashley's declarations. `or` is
          // BitSet's IN-PLACE `|=`, never `|`, which returns a new set.
          new balticporter.transform.TypeRedirectTransform(
            redirects = Map(
              "com.badlogic.gdx.utils.ReflectionPool" -> "com.badlogic.ashley.core.ComponentPool",
              "com.badlogic.gdx.utils.Bits" -> "scala.collection.mutable.BitSet"
            ),
            memberRenames = Map(
              "com.badlogic.gdx.utils.Bits" -> Map(
                "get" -> "contains",
                "set" -> "addOne",
                "clear(int)" -> "subtractOne",
                "or" -> "|="
              )
            ),
            scopes = Map("com.badlogic.gdx.utils.Bits" -> balticporter.tir.RuleScope.Only(Set("com.badlogic.ashley")))
          ),
          // The template methods a user system overrides are plain `protected` in sge's API,
          // never package-qualified, so an override outside `sge.ecs.systems` is accepted.
          new balticporter.transform.VisibilityTransform(
            narrow = Set(
              "com.badlogic.ashley.systems.IteratingSystem#processEntity",
              "com.badlogic.ashley.systems.SortedIteratingSystem#processEntity",
              "com.badlogic.ashley.systems.IntervalIteratingSystem#processEntity",
              "com.badlogic.ashley.systems.IntervalSystem#updateInterval"
            )
          ),
          // SystemManager.removeAllSystems calls `this.systems.first()` but the base renamed
          // `Array.first` to `head` via BeanPropertyTransform; the engine does not rewrite the
          // call site in a different class's body (the same gap core's postProcess has for
          // .first()->.head). MethodBodyTransform replaces the whole body.
          new balticporter.transform.MethodBodyTransform(
            Map(
              "com.badlogic.ashley.core.SystemManager#removeAllSystems" ->
                "{ while (this.systems.size > 0) { this.removeSystem(this.systems.head) } }",
              // User ruling 2026-09-25: a read past capacity answers an EMPTY Nullable where java
              // throws; the nullability phase below wraps the result, so null here is `empty`.
              "com.badlogic.ashley.utils.Bag#get" ->
                "{ if (index >= this.data.length) null.asInstanceOf[E] else this.data(index) }"
            )
          ),
          // NullaryArityTransform is NOT instantiated: Only(Set.empty) (the default) is a
          // no-op that unions cleanly with the base's Only(...). createEntity() stays with
          // parens because no auto-detection fires on it. The specific parenless pairs
          // (hasOperationsToProcess, isEmpty) are handled by BeanPropertyTransform above.
          //
          // 3.2g: Pool is dropped+injected as sge's trait in the BASE manifest;
          // ClassToTraitTransform (also in the base) rewrites subclasses -- Ashley inherits
          // both, no instance declared here.
          // SIX MEMBERS WHOSE RETURN TYPE IS NULLABLE, per sge's migration notes (no java
          // annotation carries this). Keys use the name as it exists when NullabilityTransform
          // runs (AFTER bean collapse), UPSTREAM namespace. `Entity#getComponent` names both
          // overloads since fullName carries no descriptor. MERGED with the base's
          // NullabilityTransform via `MergeablePolicy` (`nullableMembers` unions).
          new balticporter.transform.NullabilityTransform(
            // scope = Only(Set("com.badlogic.ashley")): ashley's nullable members are in
            // com.badlogic.ashley; without this, the merged scope Only(Set("com.badlogic.gdx",...))
            // would exclude them ("scoped-out"), and nullableMembers entries silently would not fire.
            scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.ashley")),
            nullableMembers =
              Set(
                "com.badlogic.ashley.core.Engine#createComponent",
                "com.badlogic.ashley.core.Engine#getSystem",
                "com.badlogic.ashley.core.Entity#getComponent",
                "com.badlogic.ashley.core.Entity#remove",
                "com.badlogic.ashley.core.EntitySystem#engine",
                "com.badlogic.ashley.core.PooledEngine#createComponent",
                // ComponentMapper.get(Entity): entity may not have the component; sge wraps
                // as Nullable[T] (wave 3.2g).
                "com.badlogic.ashley.core.ComponentMapper#get",
                "com.badlogic.ashley.core.SystemManager#getSystem",
                // user ruling 2026-09-25, with the body rule on `Bag#get` above
                "com.badlogic.ashley.utils.Bag#get"
              )
          ),
          // 3.2g: hand-port-added members (ecs drop-in parity) -- sge's factory-registry API
          // replacing the reflective ClassReflection.newInstance the base drops. Justified by
          // divergence-investigator verdict (sge commit 80b3fc64, ISS-723).
          new balticporter.transform.AddMembersTransform(
            Map(
              "com.badlogic.ashley.core.Engine" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  name = "componentFactories",
                  arity = 0,
                  source = "protected val componentFactories: scala.collection.mutable.HashMap[Class[?], () => ?] = scala.collection.mutable.HashMap.empty",
                  reason = balticporter.tir.Reason.Configured("add-members", "com.badlogic.ashley.core.Engine#componentFactories"),
                  why = Some("sge factory registry (sge commit 80b3fc64, ISS-723): replaces reflective ClassReflection.newInstance the base drops")
                ),
                balticporter.transform.AddMembersTransform.MemberSpec(
                  name = "registerComponentFactory",
                  arity = 2,
                  // BOTH tables on purpose: `componentFactories` is the PROTECTED surface sge declares
                  // and a subclass may read, and `ComponentFactories` (minted by `registry` below) is
                  // where `createComponent` and `ComponentPool` look the key up. Registering in one
                  // only would leave the other answering "not registered".
                  source =
                    "def registerComponentFactory[T <: sge.ecs.Component](componentClass: Class[T], factory: () => T): Unit = { componentFactories.put(componentClass, factory); sge.ecs.ComponentFactories.register(componentClass, factory) }",
                  reason = balticporter.tir.Reason.Configured("add-members", "com.badlogic.ashley.core.Engine#registerComponentFactory"),
                  why = Some("sge factory registry (sge commit 80b3fc64, ISS-723): cross-platform component creation, required on Scala.js/Native")
                )
              )
            )
          ),
          // Ashley's one reflective instantiation site (`Engine#createComponent`): the registry is
          // MINTED, not injected. `miss = JvmReflect` is DECLARED and its
          // non-JVM cost COUNTED (`registry(jvm-only-miss)`): the suite instantiates component
          // classes nothing registers. `handles` names the exception whose thrower this retires.
          new balticporter.transform.RegistryTransform(
            List(
              balticporter.transform.RegistryTransform.Registry(
                callee = "com.badlogic.gdx.utils.reflect.ClassReflection#newInstance",
                placement = balticporter.transform.RegistryTransform.Placement.Object(
                  "com.badlogic.ashley.core.ComponentFactories",
                  balticporter.transform.RegistryTransform.Spelling("factories", "register", "create")
                ),
                scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.ashley")),
                handles = Set("com.badlogic.gdx.utils.reflect.ReflectionException"),
                // Delegate: the minted ComponentFactories.create calls the named method on
                // miss instead of inlining JVM reflection. Each platform row provides the
                // implementation: JVM reflects with getConstructor (public constructor only),
                // JS/Native return null (no reflection). The method takes Class[T] and returns T.
                miss = balticporter.transform.RegistryTransform.Miss.Delegate("sge.ecs.EnginePlatform.createComponentOrNull"),
                bound = Some("sge.ecs.Component")
              )
            )
          ),
          // LAST, deliberately: reads what the BASE actually emitted; must run AFTER the
          // seams that re-point those references. An absent or stale base map is a loud finding.
          balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0")
        ),
        // The ecs derived policy is frozen into a committed TSV (sge-port/ecs-derived-policy.tsv),
        // derived from the hand-written ecs that existed on master before this port replaced it.
        frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/ecs-derived-policy.tsv")),
        dropMethods = Set(
          // ImmutableArray.toArray(Class<V>) is a one-line forwarder to Array.toArray(Class),
          // which the BASE drops (the ArraySupplier-deprecated overload, neither Scala.js nor
          // Native has java.lang.reflect.Array) -- the same shape as libGDX's own subclass
          // forwarders, one repository further out. Found by RewriteTrace's orphaned-call
          // check.
          "com.badlogic.ashley.utils.ImmutableArray#toArray(Class)",
          // ImmutableArray.iterable: FIELD typed at a nested type of the retargetted Array
          // that no longer exists; sge delegates iterator() to array.iterator directly
          // instead.
          "com.badlogic.ashley.utils.ImmutableArray#iterable"
        )
      )
    )

  /** Ashley's own JUnit suite, as a dependent of [[core]]. */
  def test(coreManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path] = Nil): PortManifest = core(coreManifest, sgeRoot, reportRoots).extendedBy(
    PortManifest(
      name = "sge-ecs-test",
      // the suite calls `isEmpty()` on the redirected bit sets too; not inherited from `core`
      externalParenless = AshleyPolicy.externalParenless,
      // 3.3c: ComponentClassFactory extends ClassLoader (system parent), invisible to
      // sge.ecs.Component under sbt's forked test JVM; injected copy differs in parent loader
      // only.
      dropTypes = Set("com.badlogic.ashley.core.ComponentClassFactory"),
      dropMethods = Set(
        // EngineTests.createPrivateComponent: java's `private static class ComponentC` has a
        // PRIVATE default constructor (JVM: ComponentC() is private). The engine translates
        // java's `private` class to Scala's `private[EngineTests]`, which emits a PUBLIC
        // constructor in JVM bytecode. EnginePlatform uses getConstructor() (public only),
        // faithfully following java's ClassReflection.newInstance rule, but the Scala-encoded
        // class has a public constructor so reflection succeeds where java's would fail.
        // The test cannot be faithfully reproduced: Scala 3 cannot encode a truly-private
        // constructor on a class that other members of the same compilation unit reference.
        // Dropped from the generated suite; the hand-written EngineTestComponentE test
        // (in EngineSuite) covers the same contract with a top-level class.
        "com.badlogic.ashley.core.EngineTests#createPrivateComponent",
        // EngineTests.createNewComponent and PooledEngineTests.createNewComponent: both test
        // reflective component creation (Engine.createComponent(ComponentD.class) / ComponentA.class).
        // On Scala.js and Native, EnginePlatform.createComponentOrNull returns null (no runtime
        // reflection), so these tests fail on two of three platforms. JVM reflective creation
        // is covered by the hand-written EngineCreateComponentReflectionRedSuite (scalajvm/)
        // and PooledEngineSuite's reflective-fallback test. Factory-based creation is covered
        // cross-platform by EngineSuite and PooledEngineSuite.
        "com.badlogic.ashley.core.EngineTests#createNewComponent",
        "com.badlogic.ashley.core.PooledEngineTests#createNewComponent",
        // PooledEngineTests: the following tests create components via PooledEngine.createComponent
        // without registering factories. On JS/Native (no reflection), createComponent returns
        // null and the tests NPE. The hand-written PooledEngineSuite registers factories via
        // newPooledEngine() and covers the same behaviours cross-platform. The remaining
        // generated tests (recycleEntity, removeEntityTwice) do not call createComponent.
        "com.badlogic.ashley.core.PooledEngineTests#entityRemovalListenerOrder",
        "com.badlogic.ashley.core.PooledEngineTests#resetEntityCorrectly",
        "com.badlogic.ashley.core.PooledEngineTests#recycleComponent",
        "com.badlogic.ashley.core.PooledEngineTests#addSameComponentShouldResetAndReturnOldComponentToPool",
        "com.badlogic.ashley.core.PooledEngineTests#removeComponentReturnsItToThePoolExactlyOnce"
      ),
      inject = List(sgeRoot.resolve("sge-port/overrides-ext/ashley-test")),
      surface = List(
        new balticporter.transform.TestFrameworkTransform(),
        // 3.2g: java's iterator().remove() throws GdxRuntimeException; Scala's Iterator has
        // no remove() (enforced by the type system). Replaced with a verification that
        // iteration works and does not mutate the backing data (sge's
        // ImmutableArraySuite:95-123).
        new balticporter.transform.MethodBodyTransform(
          Map(
            "com.badlogic.ashley.utils.ImmutableArrayTests#forbiddenRemoval" ->
              // Verify the iterator is read-only by type system; must define its own locals
              // since MethodBodyTransform replaces the whole body.
              """{ val array: lowlevel.util.DynamicArray[java.lang.Integer] = lowlevel.util.DynamicArray.apply[java.lang.Integer](); val immutable: sge.ecs.utils.ImmutableArray[java.lang.Integer] = new sge.ecs.utils.ImmutableArray[java.lang.Integer](array); { var i: scala.Int = 0; while (i < 10) { array.add(i.asInstanceOf[java.lang.Integer]); i = i + 1 } }; val iter = immutable.iterator;val first = iter.next(); munit.Assertions.assertEquals(first, 0.asInstanceOf[java.lang.Integer]); var count: scala.Int = 1; while (iter.hasNext) { iter.next(); count = count + 1 }; munit.Assertions.assertEquals(count, 10); munit.Assertions.assertEquals(immutable.size, 10); { var i: scala.Int = 0; while (i < 10) { munit.Assertions.assertEquals(immutable.get(i), i.asInstanceOf[java.lang.Integer]); i = i + 1 } } }"""
          )
        )
      )
    )
  )
}
