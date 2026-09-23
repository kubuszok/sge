package sge.port.ext.ashley

import balticporter.core.{ FrontendConfig, ParityRef, PortManifest, Provenance, RuntimeMode }
import sge.port.full.LlsClasspath
import sge.port.full.LibgdxPolicy
import balticporter.runner.{ Determinism, PortRun, SourceSet, VendoredCommit }

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** Migrate **Ashley** (`ashley/src`, 21 types — libGDX's entity-component-system) through the TIR. Second corpus library, smallest genuine DEPENDENT port: every file resolves against libGDX core,
  * exercising `ManifestAgreement` outside libGDX's own source sets. Imports three types the base drops (reflection classes); the hand port SOLVED this with a factory-registry approach. Scope:
  * `ashley/src` plus `ashley/tests` (ported by [[AshleyTestMigrate]]).
  */
object AshleyMigrate:

  def main(args: Array[String]): Unit =
    val repoRoot = Path.of(sys.props.getOrElse("balticporter.root", ".")).toAbsolutePath.normalize
    val base     = repoRoot.resolve("original-src/ashley/ashley/src").normalize
    val gdxSrc   = repoRoot.resolve("original-src/libgdx/gdx/src").normalize

    val files = Files
      .walk(base)
      .iterator()
      .asScala
      .filter(p => p.toString.endsWith(".java"))
      .map(p => base.relativize(p).toString)
      .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
      .toList
      .sorted

    PortRun(
      label = "sge-ecs",
      portRoot = repoRoot.resolve("ported/sge-ecs"),
      sourceSet = SourceSet.Main,
      // libGDX core is a RESOLUTION root: parsed so every reference resolves, never emitted here.
      // `LibgdxCoreMigrate` emits it, and the two are compiled together.
      frontend = FrontendConfig(base, files, LlsClasspath.entries(repoRoot), resolutionRoots = List(gdxSrc)),
      phases = Nil, // supplied by the manifest — the two sources are mutually exclusive
      manifest = Some(AshleyPolicy.core(repoRoot)),
      provenance = Some(
        Provenance(
          upstreamName = "Ashley",
          upstreamCommit = VendoredCommit.of(base),
          originalLicense = "Apache-2.0",
          sourcePathPrefix = "ashley/src",
          sourceRoot = base.toString
        )
      ),
      // NOT `Vendored`: `LibgdxCoreMigrate` already vendors the collection shims into the module
      // this output is compiled beside. Vendoring again would define every support type twice —
      // which the JVM tolerates only while the copies agree, and the Scala.js/Native linkers reject.
      runtimeMode = RuntimeMode.Dependency,
      determinism = Determinism.fromArgs(args.toSeq),
      nextStep = "just ashley-measure"
    ).execute()

/** Ashley's per-library policy -- a DEPENDENT of libGDX core's. The base's `dropTypes`/ `dropMethods`/`packageRenames`/signature-affecting phases are INHERITED, not restated. `inject` is NOT
  * inherited: exactly one module ships each replacement file.
  */
object AshleyPolicy:

  def core(repoRoot: Path): PortManifest =
    LibgdxPolicy
      .core(repoRoot)
      .extendedBy(
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
          // Ashley's OWN replacements. `inject` is not inherited — exactly one module ships each
          // replacement file, and libGDX core ships the ones for the types IT dropped.
          inject = List(repoRoot.resolve("sge-port/overrides-ext/ashley")),
          surface = List(
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
              ),
              // SCOPE OUT three types the hand port kept Java-style getter names on
              // (Engine/Entity/EntityManager/SystemManager/Bag) -- auto-detection would rename
              // getX() to x and break every test call site. Merged with the base's
              // `Everywhere(Set.empty)` this becomes `Everywhere(except)`.
              scope = balticporter.tir.RuleScope.Everywhere(
                Set(
                  "com.badlogic.ashley.core.Engine",
                  "com.badlogic.ashley.core.Entity",
                  "com.badlogic.ashley.core.EntityManager",
                  "com.badlogic.ashley.core.SystemManager",
                  "com.badlogic.ashley.utils.Bag"
                )
              )
            ),
            // PooledEngine.ComponentPools uses ReflectionPool as a TYPE (field, local, new,
            // cast target), so no body seam can reach it; the base drops it outright. Re-point
            // every reference at Ashley's own factory-backed pool instead.
            new balticporter.transform.TypeRedirectTransform(
              Map(
                "com.badlogic.gdx.utils.ReflectionPool" -> "com.badlogic.ashley.core.ComponentPool"
              )
            ),
            // 3.2g: createEntity() is a FACTORY (constructs and returns a new Entity), but
            // NullaryArityTransform's predicate accepted it as getter-like; sge keeps it WITH
            // parens, so it is added to the Everywhere(except) set (PooledEngine's override
            // follows automatically).
            new balticporter.transform.NullaryArityTransform(
              scope = balticporter.tir.RuleScope.Everywhere(
                Set(
                  "com.badlogic.ashley.core.Engine#createEntity"
                )
              )
            ),
            // 3.2g: Pool is dropped+injected as sge's trait in the BASE manifest;
            // ClassToTraitTransform (also in the base) rewrites subclasses -- Ashley inherits
            // both, no instance declared here.
            // SIX MEMBERS WHOSE RETURN TYPE IS NULLABLE, per sge's migration notes (no java
            // annotation carries this). Keys use the name as it exists when NullabilityTransform
            // runs (AFTER bean collapse), UPSTREAM namespace. `Entity#getComponent` names both
            // overloads since fullName carries no descriptor. MERGED with the base's
            // NullabilityTransform via `MergeablePolicy` (`nullableMembers` unions).
            new balticporter.transform.NullabilityTransform(
              nullableMembers = Set(
                "com.badlogic.ashley.core.Engine#createComponent",
                "com.badlogic.ashley.core.Engine#getSystem",
                "com.badlogic.ashley.core.Entity#getComponent",
                "com.badlogic.ashley.core.Entity#remove",
                "com.badlogic.ashley.core.EntitySystem#engine",
                "com.badlogic.ashley.core.PooledEngine#createComponent",
                // ComponentMapper.get(Entity): entity may not have the component; sge wraps
                // as Nullable[T] (wave 3.2g).
                "com.badlogic.ashley.core.ComponentMapper#get"
                // NOT SystemManager#getSystem: wrapping breaks the internal call chain --
                // .orNull unwraps to the NestedNone sentinel, not JVM null, so
                // `old != null` stays true and a cast throws (measured: 33 newly failing
                // tests; known residue, 7 drop-in errors).
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
                  miss = balticporter.transform.RegistryTransform.Miss.JvmReflect(),
                  bound = Some("sge.ecs.Component")
                )
              )
            ),
            // LAST, deliberately: reads what the BASE actually emitted; must run AFTER the
            // seams that re-point those references, or it reports the sites the next phase
            // repairs. An absent or stale base map is a loud finding, never a silent fallback.
            balticporter.transform.PortMapTransform.forBases("sge")
          ),
          // THE REFERENCE HAND PORT for sge-ecs. The hand-written Ashley port lives in sge's extension
          // tree. NOT inherited — the base's parity points at sge's own hand port, not at Ashley's.
          parity = Some(ParityRef(roots = List(repoRoot.resolve("sge-extension/ecs/src/main/scala").normalize))),
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
  def test(repoRoot: Path): PortManifest = core(repoRoot).extendedBy(
    PortManifest(
      name = "sge-ecs-test",
      // 3.3c: ComponentClassFactory extends ClassLoader (system parent), invisible to
      // sge.ecs.Component under sbt's forked test JVM; injected copy differs in parent loader
      // only.
      dropTypes = Set("com.badlogic.ashley.core.ComponentClassFactory"),
      inject = List(repoRoot.resolve("sge-port/overrides-ext/ashley-test")),
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
              """{ val array: lowlevel.util.DynamicArray[java.lang.Integer] = lowlevel.util.DynamicArray.apply[java.lang.Integer](); val immutable: sge.ecs.utils.ImmutableArray[java.lang.Integer] = new sge.ecs.utils.ImmutableArray[java.lang.Integer](array); { var i: scala.Int = 0; while (i < 10) { array.add(i.asInstanceOf[java.lang.Integer]); i = i + 1 } }; val iter = immutable.iterator; val first = iter.next(); munit.Assertions.assertEquals(first, 0.asInstanceOf[java.lang.Integer]); var count: scala.Int = 1; while (iter.hasNext) { iter.next(); count = count + 1 }; munit.Assertions.assertEquals(count, 10); munit.Assertions.assertEquals(immutable.size, 10); { var i: scala.Int = 0; while (i < 10) { munit.Assertions.assertEquals(immutable.get(i), i.asInstanceOf[java.lang.Integer]); i = i + 1 } } }"""
          )
        )
      )
    )
  )
