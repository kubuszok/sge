package sge.port.full

import balticporter.core.{ FrontendConfig, ParityRef, PortManifest, Provenance, ResourceTree, RuntimeMode, Substitutions }
import balticporter.runner.{ Determinism, PortRun, SourceSet, VendoredCommit }
import balticporter.tir.{ Descriptor, Param }
import balticporter.transform.{ ClassTableTransform, CollectionsTransform, MutableParamsTransform, PanamaFfiTransform, StaticForwarderTransform, TestFrameworkTransform }

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** libGDX core's full-port policy: the manifest, transform args and provenance for porting `gdx/src` through the TIR. Engine mechanics live in [[balticporter.runner.PortRun]] — what's here is POLICY
  * ONLY.
  */
object LibgdxCoreMigrate {

  def main(args: Array[String]): Unit = {
    val raw      = args.contains("--raw")
    val repoRoot = Path.of(sys.props.getOrElse("sge.root", ".")).toAbsolutePath.normalize
    val base     = repoRoot.resolve("original-src/libgdx/gdx/src").normalize

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
      label = "sge",
      portRoot = repoRoot.resolve("ported/sge"),
      sourceSet = SourceSet.Main,
      frontend = FrontendConfig(base, files, LlsClasspath.entries(repoRoot), Nil),
      phases = Nil, // supplied by the manifest — the two sources are mutually exclusive
      // The BASE manifest. `libgdx-test` is a dependent of exactly this value, so the two runs
      // cannot disagree about the shared surface by construction, and `ManifestAgreement` verifies
      // it anyway on every run (a consumer is free to write the dependent's policy out longhand).
      manifest = Some {
        val overrides = repoRoot.resolve("sge-port/overrides-full")
        if raw then LibgdxPolicy.core(repoRoot, overrides).withoutSurface else LibgdxPolicy.core(repoRoot, overrides)
      },
      provenance = Some(
        Provenance(
          upstreamName = "libGDX",
          upstreamCommit = VendoredCommit.of(base),
          originalLicense = "Apache-2.0",
          sourcePathPrefix = "gdx/src",
          sourceRoot = base.toString
        )
      ),
      // A SINGLE source set compiled standalone by `scala-cli`, with no dependency resolution: the
      // support types the collections phase retyped onto have to ship beside the emitted code.
      // The TEST source set of the same module must NOT vendor them again — see LibgdxTestMigrate.
      runtimeMode = RuntimeMode.Vendored,
      determinism = Determinism.fromArgs(args.toSeq),
      nextStep = "sbt sge/compile"
    ).execute()
  }
}

/** libGDX's per-library policy, in one place because two source sets share it. This is the WHICH, not the mechanism.
  */
object LibgdxPolicy {

  /** `Class#getResource` answers a `java.net.URL`, which Scala Native's javalib lacks (its linker: `Unknown type java.net.URL`, from `FileHandle.exists`); the twin `getResourceAsStream` it has.
    * libGDX's only use of the URL is an existence probe (`!= null`), so the probe is respelled and the stream closed — sge's hand port's own spelling. ONE value for the full port and the ladder;
    * `PortabilityCheck` still counts the site on Scala.js, which has neither method and answers through its FileHandle platform row.
    */
  val ClasspathProbeCalls: Map[String, String] = Map(
    "java.lang.Class#getResource(String)" ->
      "({{ val bpResource = {recv}.getResourceAsStream({arg0}); if (bpResource != null) bpResource.close(); bpResource }})"
  )

  /** `Matrix4`'s `static final` scratch instances (`quat`, `l_vez`, `tmpMat`, …): java shares one per class across every thread, so two threads composing matrices corrupt each other's temporaries;
    * `ThreadConfinedStaticsTransform` confines each to the reading thread. ONE value for the full port and the ladder.
    */
  val ThreadConfinedScratch: Set[String] =
    Set("quat", "quat2", "l_vez", "l_vex", "l_vey", "tmpVec", "tmpMat", "right", "tmpForward", "tmpUp").map(f => s"com.badlogic.gdx.math.Matrix4#$f")

  /** libGDX core's policy AS A VALUE — imported and extended by every dependent module. Shared-surface policy only: drop/rename tables and the phases that reshape signatures a dependent compiles
    * against. `governs` is the namespace claim; the test suite lives inside it too, so substitution agreement works from unit origins, not a prefix.
    */
  def core(repoRoot: Path, overrides: Path = Path.of("sge-port/overrides-full")): PortManifest = {
    val s = substitutions(overrides)
    PortManifest(
      name = "sge",
      governs = Set("com.badlogic.gdx"),
      dropTypes = s.dropTypes,
      dropMethods = s.dropMethods,
      inject = s.inject,
      surface = mainPhases,
      // upstream namespace stays in every OTHER policy's keys (consulted at the frontend,
      // before this runs); packageRenames is the one map the consumer actually sees.
      packageRenames = Map("com.badlogic.gdx" -> "sge"),
      // sge renamed List to SgeList to avoid clash with scala.List (1 file)
      typeRenames = Map(
        "com.badlogic.gdx.scenes.scene2d.ui.List" -> "SgeList" // avoids clash with scala.List (1 sge file)
      ),
      resolutions = reviewedBoundaries,
      // What the emitted code READS from the classpath at run time (`PortManifest.resources`):
      // BitmapFont's default font and DefaultShader's GLSL, at the upstream paths the string
      // literals still name (the hand port renamed them with the package — not yet a mechanism).
      resources = List(
        ResourceTree(
          root = repoRoot.resolve("original-src/libgdx/gdx/res").normalize,
          files = List(
            "com/badlogic/gdx/utils/lsans-15.fnt",
            "com/badlogic/gdx/utils/lsans-15.png",
            "com/badlogic/gdx/graphics/g3d/shaders/default.vertex.glsl",
            "com/badlogic/gdx/graphics/g3d/shaders/default.fragment.glsl",
            "com/badlogic/gdx/graphics/g3d/shaders/depth.vertex.glsl",
            "com/badlogic/gdx/graphics/g3d/shaders/depth.fragment.glsl"
          )
        )
      ),
      // The artifact this module's build adds. Locale calls (I18NBundle)
      // need scala-java-locales; not inherited — each dependent declares its own if it needs it.
      dependencies = List(
        balticporter.catalog.ArtifactDep("io.github.cquiroz", "scala-java-locales", "1.5.4", balticporter.catalog.CrossKind.Platform),
        // lowlevel.Nullable's opaque wrapper — coordinate is the port's to state.
        balticporter.catalog.ArtifactDep("com.kubuszok", "lls", "0.3.0")
      ),
      // The reference hand port for sge. Not inherited.
      parity = Some(ParityRef(roots = List(repoRoot.resolve("sge/src/main/scala").normalize))),
      externalParenless = Set(
        "lowlevel.util.DynamicArray#isEmpty",
        "lowlevel.util.ObjectMap#isEmpty",
        "lowlevel.util.OrderedSet#first",
        "lowlevel.util.Pool#getFree",
        // after package rename: call-site symbols keep the renamed owner
        "sge.utils.Array#isEmpty",
        "sge.utils.ObjectMap#isEmpty",
        "sge.utils.OrderedSet#first",
        "sge.utils.Pool#getFree",
        "sge.utils.DynamicArray#isEmpty",
        "sge.utils.FloatArray#isEmpty",
        "lowlevel.util.OrderedSet#orderedItems",
        "sge.utils.OrderedSet#orderedItems"
      )
    )
  }

  /** Boundary rows this port has read and accepted — each leaves its refusal lane and moves to `remediation(resolved)`. On `core`, not a dependent: these are facts about libGDX core's own
    * declarations.
    */
  def reviewedBoundaries: Map[String, String] = Map(
    // identityHashCode reads OBJECT IDENTITY only — exactly OpaqueEgress's question.
    "java.lang.System#identityHashCode" -> "accept-opaque-egress",

    // Stage is constructed by application code, not the library; the caller owns the context.
    "com.badlogic.gdx.scenes.scene2d.Stage" -> "accept-unconstructed-thread",

    // GLErrorListener field is a static initialiser overriding an interface method; no caller
    // context exists at that point.
    "com.badlogic.gdx.graphics.profiling.GLErrorListener#LOGGING_LISTENER" -> "accept-residual-global"
  )

  /** libGDX's own JUnit suite, as a DEPENDENT of [[core]]. Adds one phase and inherits everything else.
    */
  /** watcher field is dead on every munit platform (no @Rule protocol); dropped via dropMethods/dropFields. JVM loses a diagnostic println on test failure.
    */
  private val watcherDrop = "com.badlogic.gdx.utils.JsonMatcherTests#watcher"

  def test(repoRoot: Path, overrides: Path = Path.of("sge-port/overrides-full")): PortManifest = core(repoRoot, overrides).extendedBy(
    PortManifest(
      name = "sge-test",
      dropMethods = Set(watcherDrop),
      surface = List(
        new TestFrameworkTransform(dropFields = Set(watcherDrop)),
        selfSuppliedSuites,
        // 3.1ae: sge dropped CharArray's string-builder API; replace with java.lang.StringBuilder.
        new balticporter.transform.MethodBodyTransform(
          Map(
            "com.badlogic.gdx.utils.JsonMatcherTests#toString(JsonMatcher,String[])" ->
              """{
                |  val buffer: java.lang.StringBuilder = new java.lang.StringBuilder()
                |  for (pattern <- patterns) {
                |    if (buffer.length() > 0) buffer.append(", ")
                |    if (pattern.isEmpty())
                |      buffer.append("\"\"")
                |    else
                |      buffer.append(sge.utils.PatternParser.parse(matcher, pattern, lowlevel.Nullable.empty).toString())
                |  }
                |  return buffer.toString().replace("\n", "\\n").replace("\t", "\\t")
                |}""".stripMargin,
            "com.badlogic.gdx.utils.JsonMatcherTests#toString(Array)" ->
              """{
                |  val buffer: java.lang.StringBuilder = new java.lang.StringBuilder()
                |  for (value <- values) {
                |    if (buffer.length() > 0) buffer.append(", ")
                |    buffer.append(value.toJson(sge.utils.JsonWriter.OutputType.minimal))
                |  }
                |  return buffer.toString().replace("\n", "\\n").replace("\t", "\\t")
                |}""".stripMargin
          )
        )
      ),
      // munit JS/Native Description declares these parenless; the JVM class file has ().
      // Both call forms are legal on both — Scala 3 auto-applies, the parenless def is its own match.
      externalParenless = Set(
        "org.junit.runner.Description#getTestClass",
        "org.junit.runner.Description#getMethodName",
        "org.junit.runner.Description#getAnnotations",
        "lowlevel.util.DynamicArray#isEmpty",
        "lowlevel.util.ObjectMap#isEmpty",
        "lowlevel.util.OrderedSet#first",
        "lowlevel.util.Pool#getFree"
      )
    )
  )

  /** The one `selfSupplied` entry: `AnimationControllerTest` is constructed reflectively by MUnit, so the threaded context cannot reach it as a parameter — it takes one without a clause, an emitted
    * `private given sge.Sge = sge.SgeTestFixture.testSge()`, matching the reference hand port. Lives on the dependent (test source set): the key names a test declaration the base never parses.
    */
  def selfSuppliedSuites: balticporter.transform.GlobalsToImplicitsTransform =
    new balticporter.transform.GlobalsToImplicitsTransform(
      extensions = List(
        balticporter.transform.ContextHolderExtension(
          holder = "com.badlogic.gdx.Gdx",
          selfSupplied = Map(
            "com.badlogic.gdx.graphics.g3d.utils.AnimationControllerTest" ->
              "sge.SgeTestFixture.testSge()"
          )
        )
      )
    )

  /** Typed substitution manifest: constructs sge dropped upstream, and their Scala replacements. A dropped type is still PARSED — only its OUTPUT is replaced — so references to it still resolve.
    */
  def substitutions(overrides: Path): Substitutions = Substitutions(
    // utils.reflect (java.lang.reflect wrapper) is replaced by Kindlings' Jsoniter/UBJson codecs.
    dropTypes = Set(
      // reflection-based serializer — replaced by Kindlings Jsoniter/UBJson codecs.
      "com.badlogic.gdx.utils.Json",
      // wave 3.2g: Pools/Pool -> injected trait with abstract vals (sge AD-003);
      // ClassToTraitTransform rewrites subclasses.
      "com.badlogic.gdx.utils.Pool",
      "com.badlogic.gdx.utils.Pools",
      // dropped with NO replacement — every reference eliminated, so CHECK 2 proves they are gone
      "com.badlogic.gdx.utils.ReflectionPool",
      "com.badlogic.gdx.utils.reflect.Annotation",
      "com.badlogic.gdx.utils.reflect.Field",
      "com.badlogic.gdx.utils.reflect.ArrayReflection",
      "com.badlogic.gdx.utils.reflect.ClassReflection",
      "com.badlogic.gdx.utils.reflect.Constructor",
      "com.badlogic.gdx.utils.reflect.Method",
      "com.badlogic.gdx.utils.reflect.ReflectionException",
      // NetJavaImpl (java.net.HttpURLConnection-based) has no JS/Native target and no in-corpus
      // caller — dropped with no replacement. NB: conceals a still-open
      // JDK/Scala collection boundary gap, not closed by this drop.
      "com.badlogic.gdx.net.NetJavaImpl",
      // dropped with no replacement; disableRedirect re-points references at
      // java.lang.AutoCloseable (a redirect never deletes a declaration).
      "com.badlogic.gdx.utils.Disposable",
      // Align -> injected `opaque type Align = Int` (sge convention), retyped by
      // PrimitiveToOpaqueTransform(Existing) below.
      "com.badlogic.gdx.utils.Align",
      // wave 3.1a: retargetted to mutable.BitSet (0 callers in gdx/src; serves ashley).
      "com.badlogic.gdx.utils.Bits",
      // wave 3.1a: retargetted to lowlevel.util.ObjectMap (lls). Same member API (verified via
      // javap); inner types (Entry/Keys/Values/Entries) are NOT in lls — references to them are
      // counted compile errors.
      "com.badlogic.gdx.utils.ObjectMap",
      // wave 3.1a: retargetted to lowlevel.util.ObjectSet (lls 0.3.0). Same pattern as ObjectMap.
      "com.badlogic.gdx.utils.ObjectSet",
      // wave 3.1b: OrderedMap/OrderedSet/IdentityMap retargetted to their lls equivalents
      // (lls's ObjectMap is final, so gdx subclasses become independent lls types;
      // IdentityMap -> ArrayMap with identity semantics, lls has no IdentityMap).
      "com.badlogic.gdx.utils.OrderedMap",
      "com.badlogic.gdx.utils.OrderedSet",
      "com.badlogic.gdx.utils.IdentityMap",
      // wave 3.1d: remaining MAP family retargetted to lowlevel.util.ObjectMap — lls has no
      // primitive-keyed specialisations, boxing accepted (matches sge's own choice). Static
      // members/references disappear with the type; every remaining caller is in a dropped file.
      "com.badlogic.gdx.utils.IntMap",
      "com.badlogic.gdx.utils.LongMap",
      "com.badlogic.gdx.utils.IntIntMap",
      "com.badlogic.gdx.utils.IntFloatMap",
      "com.badlogic.gdx.utils.ObjectIntMap",
      "com.badlogic.gdx.utils.ObjectFloatMap",
      "com.badlogic.gdx.utils.ObjectLongMap",
      // wave 3.1d: ArrayMap -> lowlevel.util.ArrayMap. Deprecated Class-taking ctors already
      // in dropMethods.
      "com.badlogic.gdx.utils.ArrayMap",
      // wave 3.1d: IntSet -> lowlevel.util.ObjectSet[Int] (boxing accepted; lls has no
      // primitive-element set).
      "com.badlogic.gdx.utils.IntSet",
      // wave 3.1n: Array family -> lowlevel.util.DynamicArray (unified via MkArray type class);
      // SnapshotArray/DelayedRemovalArray -> DynamicArray (begin/end support snapshotting);
      // BooleanArray -> DynamicArray[Boolean]; Queue -> mutable.ArrayDeque (stdlib, not lls).
      "com.badlogic.gdx.utils.Array",
      "com.badlogic.gdx.utils.SnapshotArray",
      "com.badlogic.gdx.utils.DelayedRemovalArray",
      "com.badlogic.gdx.utils.IntArray",
      "com.badlogic.gdx.utils.FloatArray",
      "com.badlogic.gdx.utils.LongArray",
      "com.badlogic.gdx.utils.ShortArray",
      "com.badlogic.gdx.utils.ByteArray",
      "com.badlogic.gdx.utils.CharArray",
      "com.badlogic.gdx.utils.BooleanArray",
      "com.badlogic.gdx.utils.Queue"
    ),
    // libGDX itself deprecated `setEnabledReflection` (superseded by the typed
    // `setEnabled(Styleable, Boolean)`, already ported); its private `findMethod` helper was the
    // only reflective method scan left. Dropping both removes the last use of `reflect.Method`.
    dropMethods = Set(
      "com.badlogic.gdx.scenes.scene2d.ui.Skin#setEnabledReflection",
      "com.badlogic.gdx.scenes.scene2d.ui.Skin#findMethod",
      // `ArrayReflection` — `java.lang.reflect.Array`, which neither Scala.js nor Native has.
      // Every remaining use sits inside a member libGDX ITSELF deprecated in favour of an
      // `ArraySupplier` overload that is already portable. The corpus calls none of the
      // deprecated forms, so dropping them costs no call site and removes the last JVM-only
      // dependency outright. Overload-precise keys: the `ArraySupplier` twins must survive.
      "com.badlogic.gdx.utils.Array#<init>(boolean,int,Class)",
      "com.badlogic.gdx.utils.Array#<init>(Class)",
      "com.badlogic.gdx.utils.Array#toArray(Class)",
      "com.badlogic.gdx.utils.Array#of(Class)",
      "com.badlogic.gdx.utils.Array#of(boolean,int,Class)",
      // the Array subclasses' own deprecated pairs only forward to Array's, so they go with them.
      // (`DelayedRemovalArray` was found by RewriteTrace's orphaned-call check, not by grep.)
      "com.badlogic.gdx.utils.SnapshotArray#<init>(boolean,int,Class)",
      "com.badlogic.gdx.utils.SnapshotArray#<init>(Class)",
      "com.badlogic.gdx.utils.DelayedRemovalArray#<init>(boolean,int,Class)",
      "com.badlogic.gdx.utils.DelayedRemovalArray#<init>(Class)",
      "com.badlogic.gdx.utils.ArrayMap#<init>(boolean,int,Class,Class)",
      "com.badlogic.gdx.utils.ArrayMap#<init>(Class,Class)",
      "com.badlogic.gdx.utils.Queue#<init>(int,Class)",
      "com.badlogic.gdx.graphics.g3d.particles.ParallelArray$ChannelDescriptor#<init>(int,Class,int)",
      // the one in-corpus caller of a dropped ctor: itself deprecated, with an `ArraySupplier`
      // twin, and both subclasses (`BillboardParticleBatch`, `PointSpriteParticleBatch`) already
      // call the twin — so it too goes with no call site left behind.
      "com.badlogic.gdx.graphics.g3d.particles.batches.BufferedParticleBatch#<init>(Class)"
    ),
    // `SharedLibraryLoader`/`Os` were removed in favour of a dedicated native-extraction library;
    // the corpus still references them, so we inject standalone Scala at those FQNs.
    inject = List(overrides)
  )

  /** libGDX's ONE runtime class lookup by name (`ResourceData.AssetData` resolving a persisted asset type) has no counterpart off the JVM, so it is re-pointed at an explicit name->class table —
    * injected as `particles.AssetTypeRegistry`, seeded with the types libGDX core itself stores and open for a downstream port to extend.
    */
  def classTable: ClassTableTransform = new ClassTableTransform(
    Map(
      "com.badlogic.gdx.utils.reflect.ClassReflection#forName" ->
        "com.badlogic.gdx.graphics.g3d.particles.AssetTypeRegistry#classFor"
    )
  )

  /** libGDX routes reflection through `ClassReflection` for its GWT/Android backends; sge drops the wrapper (no runtime reflection off the JVM). Most calls are plain `java.lang.Class` members both
    * Scala.js and Native DO provide -- forwarded here; genuine reflection stays in `Substitutions.dropTypes`.
    */
  def unwrapReflection: StaticForwarderTransform = new StaticForwarderTransform(
    List(
      StaticForwarderTransform.Forwarder(
        wrapper = "com.badlogic.gdx.utils.reflect.ClassReflection",
        receiver = "java.lang.Class",
        // one-arg pass-throughs to java.lang.Class the corpus reaches (getName was dead
        // policy, removed).
        members = Set(
          "getSimpleName",
          "isInstance",
          "isAssignableFrom",
          "isArray",
          "isEnum",
          "isInterface",
          "isPrimitive",
          "isAnnotation",
          "getComponentType"
        )
      )
    )
  )

  /** `java.util.Comparator` -> `scala.math.Ordering`, the port's one RETARGET entry -- moves the type at every occurrence with no coercion, licensed by `Ordering[T] extends Comparator[T]`. No call
    * site is rewritten. Shared surface, lives in [[core]] alone; a parameter of the existing `CollectionsTransform`, not a second instance.
    */
  def comparatorRetarget: Map[String, String] =
    Map("java.util.Comparator" -> "scala.math.Ordering")

  /** `com.badlogic.gdx.utils.Bits` -> `scala.collection.mutable.BitSet` (sge type-mappings.md). 0 callers in gdx/src; sits on the base so dependents (ashley) inherit it via `extendedBy`.
    */
  def bitsRetarget: Map[String, String] =
    Map("com.badlogic.gdx.utils.Bits" -> "scala.collection.mutable.BitSet")

  def bitsRetargetRewrites: Map[String, Map[(String, Int), balticporter.transform.CollectionsTransform.RetargetRewrite]] = {
    import balticporter.transform.CollectionsTransform.RetargetRewrite.*
    Map(
      "com.badlogic.gdx.utils.Bits" -> Map(
        ("get", 1) -> Rename("apply"), // bits.get(i) -> bits(i)
        ("set", 1) -> Rename("addOne"), // bits.set(i) -> bits.addOne(i), rendered bits += i
        ("clear", 1) -> Rename("subtractOne"), // bits.clear(i) -> bits -= i
        ("and", 1) -> Rename("&="), // bits.and(other) -> bits &= other
        ("or", 1) -> Rename("|="), // bits.or(other) -> bits |= other
        ("andNot", 1) -> Rename("&~="), // bits.andNot(other) -> bits &~= other
        ("xor", 1) -> Rename("^="), // bits.xor(other) -> bits ^= other (IN-PLACE, not BitSet.xor which returns a new set)
        // bits.containsAll(other) = "this is a superset of other" = other.subsetOf(this).
        // Argument and receiver SWAP — Template, not Rename.
        ("containsAll", 1) -> Template("$0.subsetOf($recv)"),
        // bits.length() -> highest set bit + 1. BitSet has no direct equivalent.
        // BitSet.last is the highest element; +1 matches java's Bits.length semantics.
        // Empty -> 0 (Bits.length returns 0 on empty, BitSet.last throws).
        ("length", 0) -> Template("(if ($recv.isEmpty) 0 else $recv.last + 1)"),
        // bits.numBits() -> count of set bits = BitSet.size
        ("numBits", 0) -> Chain(List("size")),
        // bits.flip(i) -> toggle bit i
        ("flip", 1) -> Template("(if ($recv($0)) $recv -= $0 else $recv += $0)"),
        // bits.nextSetBit(fromIndex) -> first set bit >= fromIndex, or -1
        ("nextSetBit", 1) -> Template("$recv.rangeFrom($0).headOption.getOrElse(-1)"),
        // bits.intersects(other) -> (bits & other).nonEmpty
        ("intersects", 1) -> Template("($recv & $0).nonEmpty"),
        // bits.getAndClear(i) -> { val was = bits(i); bits -= i; was }
        ("getAndClear", 1) -> Template("{ val bpWas = $recv($0); $recv -= $0; bpWas }"),
        // java's getAndSet returns true if the bit was ALREADY set (before the operation)
        ("getAndSet", 1) -> Template("{ val bpWas = $recv($0); $recv += $0; bpWas }"),
        // bits.clear() (no args) -> clear all bits. BitSet.clear() exists.
        ("clear", 0) -> Rename("clear"),
        // bits.notEmpty() -> nonEmpty (parenless on BitSet)
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        // bean-property may rename isEmpty->empty; this reverses it back to BitSet.isEmpty
        ("empty", 0) -> Chain(List("isEmpty"))
      )
    )
  }

  /** `ObjectMap`/`ObjectSet` retargetted to their lls equivalents (sge type-mappings.md). Same member API (verified via `javap`); lls's ctor is PRIVATE so `new` routes through the companion's
    * transparent inline `apply`. Inner types (Entry/Keys/Values/Entries) are NOT in lls -- references to them are counted on `collection-retarget`.
    */
  def libCollectionRetargets: Map[String, String] = Map(
    "com.badlogic.gdx.utils.ObjectMap" -> "lowlevel.util.ObjectMap",
    "com.badlogic.gdx.utils.ObjectSet" -> "lowlevel.util.ObjectSet",
    // subclasses: lls ObjectMap is final, so OrderedMap/IdentityMap/OrderedSet are standalone types
    "com.badlogic.gdx.utils.OrderedMap" -> "lowlevel.util.OrderedMap",
    "com.badlogic.gdx.utils.OrderedSet" -> "lowlevel.util.OrderedSet",
    // sge type-mappings.md maps IdentityMap to ArrayMap (lls has no IdentityMap)
    "com.badlogic.gdx.utils.IdentityMap" -> "lowlevel.util.ArrayMap",
    // ObjectMap.Entry -> Tuple2: same image as JDK Map.Entry -> Tuple2 already in the phase.
    // .key -> ._1, .value -> ._2 field rewrites handled by retargetSelectRewrite (Tree.Select arm).
    // The arity-0 constructor (java's default-constructed Entry with both fields null) is routed
    // through the Construct entry in libCollectionConstructRewrites.
    "com.badlogic.gdx.utils.ObjectMap$Entry" -> "scala.Tuple2",
    // wave 3.1d: remaining MAP family retargets
    "com.badlogic.gdx.utils.IntMap" -> "lowlevel.util.ObjectMap",
    "com.badlogic.gdx.utils.LongMap" -> "lowlevel.util.ObjectMap",
    "com.badlogic.gdx.utils.IntIntMap" -> "lowlevel.util.ObjectMap",
    "com.badlogic.gdx.utils.IntFloatMap" -> "lowlevel.util.ObjectMap",
    "com.badlogic.gdx.utils.ObjectIntMap" -> "lowlevel.util.ObjectMap",
    "com.badlogic.gdx.utils.ObjectFloatMap" -> "lowlevel.util.ObjectMap",
    "com.badlogic.gdx.utils.ObjectLongMap" -> "lowlevel.util.ObjectMap",
    "com.badlogic.gdx.utils.ArrayMap" -> "lowlevel.util.ArrayMap",
    "com.badlogic.gdx.utils.IntSet" -> "lowlevel.util.ObjectSet",
    // Array family -> DynamicArray (sge type-mappings.md: primitive arrays ->
    // "DynamicArray[T]" (unified via MkArray type class); `Array<T>` -> `DynamicArray[T]`
    // (1:1 type param). DynamicArray has the same member API (add/insert/remove/pop/peek/first/
    // clear/truncate/swap/reverse/shuffle/sort/toArray/ensureCapacity/size/items/contains/
    // indexOf/select/random/selectRanked/iterator) and supports `for (x <- da)` natively.
    "com.badlogic.gdx.utils.Array" -> "lowlevel.util.DynamicArray",
    "com.badlogic.gdx.utils.SnapshotArray" -> "lowlevel.util.DynamicArray",
    "com.badlogic.gdx.utils.DelayedRemovalArray" -> "lowlevel.util.DynamicArray",
    "com.badlogic.gdx.utils.IntArray" -> "lowlevel.util.DynamicArray",
    "com.badlogic.gdx.utils.FloatArray" -> "lowlevel.util.DynamicArray",
    "com.badlogic.gdx.utils.LongArray" -> "lowlevel.util.DynamicArray",
    "com.badlogic.gdx.utils.ShortArray" -> "lowlevel.util.DynamicArray",
    "com.badlogic.gdx.utils.ByteArray" -> "lowlevel.util.DynamicArray",
    "com.badlogic.gdx.utils.CharArray" -> "lowlevel.util.DynamicArray",
    "com.badlogic.gdx.utils.BooleanArray" -> "lowlevel.util.DynamicArray",
    "com.badlogic.gdx.utils.Queue" -> "scala.collection.mutable.ArrayDeque",
    // Inner iterator types — java's Keys/Values/Entries are live views backed by the map's
    // own table; lls has foreachKey/foreachValue/foreachEntry (inline) instead.  As TYPES these
    // are used only where java stores them in a local (`I18NBundle`) — Collect handles the call.
    // nested iterator types -> scala Iterator over a snapshot
    "com.badlogic.gdx.utils.ObjectMap$Keys" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectMap$Values" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectMap$Entries" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.IntMap$Keys" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.IntMap$Values" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.IntMap$Entries" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.LongMap$Keys" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.LongMap$Values" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.LongMap$Entries" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.IntIntMap$Keys" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.IntIntMap$Values" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.IntIntMap$Entries" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.IntFloatMap$Keys" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.IntFloatMap$Values" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.IntFloatMap$Entries" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectIntMap$Keys" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectIntMap$Values" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectIntMap$Entries" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectFloatMap$Keys" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectFloatMap$Values" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectFloatMap$Entries" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectLongMap$Keys" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectLongMap$Values" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ObjectLongMap$Entries" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ArrayMap$Keys" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ArrayMap$Values" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.ArrayMap$Entries" -> "scala.collection.Iterator",
    // Inner Entry types for the map family — same Tuple2 mapping as ObjectMap.Entry
    "com.badlogic.gdx.utils.IntMap$Entry" -> "scala.Tuple2",
    "com.badlogic.gdx.utils.LongMap$Entry" -> "scala.Tuple2",
    "com.badlogic.gdx.utils.ObjectIntMap$Entry" -> "scala.Tuple2",
    "com.badlogic.gdx.utils.ObjectFloatMap$Entry" -> "scala.Tuple2",
    "com.badlogic.gdx.utils.ObjectLongMap$Entry" -> "scala.Tuple2",
    // --- 3.1ai: missing Entry types for IntIntMap and IntFloatMap — anim8 45 errors.
    // Only ENTRY types are retargetted (to Tuple2). Keys/Values/Entries are iterator types and
    // DynamicArray is NOT an iterator, so retargeting them would introduce hasNext/next errors.
    // The Collect rewrite handles keys()/values()/entries() calls; remaining type references to
    // the iterator types are counted on the collection-retarget lane.
    "com.badlogic.gdx.utils.IntIntMap$Entry" -> "scala.Tuple2",
    "com.badlogic.gdx.utils.IntFloatMap$Entry" -> "scala.Tuple2",
    // set-iterator types -> scala.collection.Iterator: ObjectSetIterator declares toArray/hasNext/
    // next/remove; OrderedSetIterator extends it. Retargeted so chain-iterator callers (Selection#toArray)
    // resolve members on scala.collection.Iterator without body substitutions.
    "com.badlogic.gdx.utils.ObjectSet$ObjectSetIterator" -> "scala.collection.Iterator",
    "com.badlogic.gdx.utils.OrderedSet$OrderedSetIterator" -> "scala.collection.Iterator"
  )

  /** TYPE ARGUMENT MAPPING for arity-changing retargets: how to fill the target type's type arguments from the source's when arities differ, e.g. `IntMap<V>` (1 param) -> `ObjectMap[K,V]` (2 params).
    */
  def libCollectionRetargetTypeArgs: Map[String, List[balticporter.transform.CollectionsTransform.RetargetArg]] = {
    import balticporter.transform.CollectionsTransform.RetargetArg.*
    Map(
      "com.badlogic.gdx.utils.IntMap" -> List(FixedType("scala.Int"), SourceArg(0)),
      "com.badlogic.gdx.utils.LongMap" -> List(FixedType("scala.Long"), SourceArg(0)),
      "com.badlogic.gdx.utils.IntIntMap" -> List(FixedType("scala.Int"), FixedType("scala.Int")),
      "com.badlogic.gdx.utils.IntFloatMap" -> List(FixedType("scala.Int"), FixedType("scala.Float")),
      "com.badlogic.gdx.utils.ObjectIntMap" -> List(SourceArg(0), FixedType("scala.Int")),
      "com.badlogic.gdx.utils.ObjectFloatMap" -> List(SourceArg(0), FixedType("scala.Float")),
      "com.badlogic.gdx.utils.ObjectLongMap" -> List(SourceArg(0), FixedType("scala.Long")),
      "com.badlogic.gdx.utils.IntSet" -> List(FixedType("scala.Int")),
      // --- 3.1ai: Entry type arg mappings for primitive-key maps (0-param Entry -> Tuple2[K,V])
      "com.badlogic.gdx.utils.IntIntMap$Entry" -> List(FixedType("scala.Int"), FixedType("scala.Int")),
      "com.badlogic.gdx.utils.IntFloatMap$Entry" -> List(FixedType("scala.Int"), FixedType("scala.Float")),
      "com.badlogic.gdx.utils.IntMap$Entry" -> List(FixedType("scala.Int"), SourceArg(0)),
      "com.badlogic.gdx.utils.LongMap$Entry" -> List(FixedType("scala.Long"), SourceArg(0)),
      "com.badlogic.gdx.utils.ObjectIntMap$Entry" -> List(SourceArg(0), FixedType("scala.Int")),
      "com.badlogic.gdx.utils.ObjectFloatMap$Entry" -> List(SourceArg(0), FixedType("scala.Float")),
      "com.badlogic.gdx.utils.ObjectLongMap$Entry" -> List(SourceArg(0), FixedType("scala.Long")),
      // nested iterator types -> Iterator[elem]; Entries carry (K, V) as a Tuple2
      "com.badlogic.gdx.utils.ObjectMap$Entries" -> List(Applied("scala.Tuple2", List(SourceArg(0), SourceArg(1)))),
      "com.badlogic.gdx.utils.ArrayMap$Entries" -> List(Applied("scala.Tuple2", List(SourceArg(0), SourceArg(1)))),
      "com.badlogic.gdx.utils.IntMap$Entries" -> List(Applied("scala.Tuple2", List(FixedType("scala.Int"), SourceArg(0)))),
      "com.badlogic.gdx.utils.IntMap$Keys" -> List(FixedType("scala.Int")),
      "com.badlogic.gdx.utils.LongMap$Entries" -> List(Applied("scala.Tuple2", List(FixedType("scala.Long"), SourceArg(0)))),
      "com.badlogic.gdx.utils.LongMap$Keys" -> List(FixedType("scala.Long")),
      "com.badlogic.gdx.utils.IntIntMap$Entries" -> List(Applied("scala.Tuple2", List(FixedType("scala.Int"), FixedType("scala.Int")))),
      "com.badlogic.gdx.utils.IntIntMap$Keys" -> List(FixedType("scala.Int")),
      "com.badlogic.gdx.utils.IntIntMap$Values" -> List(FixedType("scala.Int")),
      "com.badlogic.gdx.utils.IntFloatMap$Entries" -> List(Applied("scala.Tuple2", List(FixedType("scala.Int"), FixedType("scala.Float")))),
      "com.badlogic.gdx.utils.IntFloatMap$Keys" -> List(FixedType("scala.Int")),
      "com.badlogic.gdx.utils.IntFloatMap$Values" -> List(FixedType("scala.Float")),
      "com.badlogic.gdx.utils.ObjectIntMap$Entries" -> List(Applied("scala.Tuple2", List(SourceArg(0), FixedType("scala.Int")))),
      "com.badlogic.gdx.utils.ObjectIntMap$Values" -> List(FixedType("scala.Int")),
      "com.badlogic.gdx.utils.ObjectFloatMap$Entries" -> List(Applied("scala.Tuple2", List(SourceArg(0), FixedType("scala.Float")))),
      "com.badlogic.gdx.utils.ObjectFloatMap$Values" -> List(FixedType("scala.Float")),
      "com.badlogic.gdx.utils.ObjectLongMap$Entries" -> List(Applied("scala.Tuple2", List(SourceArg(0), FixedType("scala.Long")))),
      "com.badlogic.gdx.utils.ObjectLongMap$Values" -> List(FixedType("scala.Long")),
      // wave 3.1n: primitive arrays — 0-param source to 1-param DynamicArray[T].
      // sge type-mappings.md: "IntArray -> DynamicArray[Int]", etc.
      "com.badlogic.gdx.utils.IntArray" -> List(FixedType("scala.Int")),
      "com.badlogic.gdx.utils.FloatArray" -> List(FixedType("scala.Float")),
      "com.badlogic.gdx.utils.LongArray" -> List(FixedType("scala.Long")),
      "com.badlogic.gdx.utils.ShortArray" -> List(FixedType("scala.Short")),
      "com.badlogic.gdx.utils.ByteArray" -> List(FixedType("scala.Byte")),
      "com.badlogic.gdx.utils.CharArray" -> List(FixedType("scala.Char")),
      "com.badlogic.gdx.utils.BooleanArray" -> List(FixedType("scala.Boolean")),
      // set-iterator types: 1-param ObjectSetIterator<T> / OrderedSetIterator<T> -> Iterator[T]
      "com.badlogic.gdx.utils.ObjectSet$ObjectSetIterator" -> List(SourceArg(0)),
      "com.badlogic.gdx.utils.OrderedSet$OrderedSetIterator" -> List(SourceArg(0))
    )
  }

  // MkArray evidence at a type-variable element: sge's own `createRef` cast (subplan 1b)
  private val mkArrayRef = Some("lowlevel.MkArray[$T0] = lowlevel.MkArray.anyRef[AnyRef].asInstanceOf[lowlevel.MkArray[$T0]]")

  def libCollectionConstructRewrites: Map[String, Map[(String, Int), balticporter.transform.CollectionsTransform.RetargetRewrite]] = {
    import balticporter.transform.CollectionsTransform.RetargetRewrite.*
    Map(
      "com.badlogic.gdx.utils.ObjectMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        // bean-property renamed isEmpty() -> empty; lls keeps isEmpty
        ("empty", 0) -> Rename("isEmpty"),
        // ForEach: for (Entry e : map.entries()) -> map.foreachEntry((k, v) => body)
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1al: lls ObjectMap.get(K) returns Nullable[V]; the 1-arg overload must be
        // selected explicitly because return-type-sensitive overload resolution picks get(K,V):V
        // when the expected type is V. `.orNull` is the null-preserving unwrap (java's map.get
        // returns null, NOT NPE). lls `orNull` is not actually deprecated — the annotation
        // triggers -Werror, so `SuppressionPhase` places @nowarn on the enclosing member.
        ("get", 1) -> Template("$recv.get($0).orNull")
      ),
      // `it.hasNext()` is parenless on scala's Iterator
      "com.badlogic.gdx.utils.ObjectMap$Keys" -> Map(
        ("hasNext", 0) -> Chain(List("hasNext")),
        ("toArray", 0) -> Template(
          "{ given lowlevel.MkArray[$T0] = lowlevel.MkArray.anyRef[AnyRef].asInstanceOf[lowlevel.MkArray[$T0]]; val bpR: lowlevel.util.DynamicArray[$T0] = lowlevel.util.DynamicArray[$T0](); $recv.foreach(bpR.add); bpR }"
        ),
        ("toArray", 1) -> Template("{ val bpA = $0; $recv.foreach(bpA.add); bpA }")
      ),
      "com.badlogic.gdx.utils.ObjectMap$Values" -> Map(
        ("hasNext", 0) -> Chain(List("hasNext")),
        ("toArray", 0) -> Template(
          "{ given lowlevel.MkArray[$T0] = lowlevel.MkArray.anyRef[AnyRef].asInstanceOf[lowlevel.MkArray[$T0]]; val bpR: lowlevel.util.DynamicArray[$T0] = lowlevel.util.DynamicArray[$T0](); $recv.foreach(bpR.add); bpR }"
        ),
        ("toArray", 1) -> Template("{ val bpA = $0; $recv.foreach(bpA.add); bpA }")
      ),
      "com.badlogic.gdx.utils.ObjectMap$Entries" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.IntMap$Keys" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.IntMap$Values" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.IntMap$Entries" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.LongMap$Keys" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.LongMap$Values" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.LongMap$Entries" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.IntIntMap$Keys" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.IntIntMap$Values" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.IntIntMap$Entries" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.IntFloatMap$Keys" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.IntFloatMap$Values" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.IntFloatMap$Entries" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ObjectIntMap$Keys" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ObjectIntMap$Values" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ObjectIntMap$Entries" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ObjectFloatMap$Keys" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ObjectFloatMap$Values" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ObjectFloatMap$Entries" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ObjectLongMap$Keys" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ObjectLongMap$Values" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ObjectLongMap$Entries" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ArrayMap$Keys" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ArrayMap$Values" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      "com.badlogic.gdx.utils.ArrayMap$Entries" -> Map(("hasNext", 0) -> Chain(List("hasNext"))),
      // set-iterator types: hasNext is parenless; toArray builds a DynamicArray from the iterator.
      // toArray(1) collects into the caller's provided array. remove left to IteratorRemove count.
      "com.badlogic.gdx.utils.ObjectSet$ObjectSetIterator" -> Map(
        ("hasNext", 0) -> Chain(List("hasNext")),
        ("toArray", 0) -> Template(
          "{ given lowlevel.MkArray[$T0] = lowlevel.MkArray.anyRef[AnyRef].asInstanceOf[lowlevel.MkArray[$T0]]; val bpR: lowlevel.util.DynamicArray[$T0] = lowlevel.util.DynamicArray[$T0](); $recv.foreach(bpR.add); bpR }"
        ),
        ("toArray", 1) -> Template("{ val bpA = $0; $recv.foreach(bpA.add); bpA }")
      ),
      "com.badlogic.gdx.utils.OrderedSet$OrderedSetIterator" -> Map(
        ("hasNext", 0) -> Chain(List("hasNext")),
        ("toArray", 0) -> Template(
          "{ given lowlevel.MkArray[$T0] = lowlevel.MkArray.anyRef[AnyRef].asInstanceOf[lowlevel.MkArray[$T0]]; val bpR: lowlevel.util.DynamicArray[$T0] = lowlevel.util.DynamicArray[$T0](); $recv.foreach(bpR.add); bpR }"
        ),
        ("toArray", 1) -> Template("{ val bpA = $0; $recv.foreach(bpA.add); bpA }")
      ),
      // Entry arity-0: java's default-constructed Entry with both fields null.
      // Construct routes `new Tuple2()` -> `Tuple2.apply(null.asInstanceOf[K], null.asInstanceOf[V])`.
      // Every Entry source needs the entry, since retargetTargetToSource maps to whichever
      // source program.symbols.all iterates last.
      "com.badlogic.gdx.utils.ObjectMap$Entry" -> Map(
        ("<init>", 0) -> Construct("scala.Tuple2", "apply", fillTypeArgs = true)
      ),
      "com.badlogic.gdx.utils.IntMap$Entry" -> Map(
        ("<init>", 0) -> Construct("scala.Tuple2", "apply", fillTypeArgs = true)
      ),
      "com.badlogic.gdx.utils.LongMap$Entry" -> Map(
        ("<init>", 0) -> Construct("scala.Tuple2", "apply", fillTypeArgs = true)
      ),
      "com.badlogic.gdx.utils.ObjectIntMap$Entry" -> Map(
        ("<init>", 0) -> Construct("scala.Tuple2", "apply", fillTypeArgs = true)
      ),
      "com.badlogic.gdx.utils.ObjectFloatMap$Entry" -> Map(
        ("<init>", 0) -> Construct("scala.Tuple2", "apply", fillTypeArgs = true)
      ),
      "com.badlogic.gdx.utils.ObjectLongMap$Entry" -> Map(
        ("<init>", 0) -> Construct("scala.Tuple2", "apply", fillTypeArgs = true)
      ),
      // --- 3.1ai: IntIntMap$Entry and IntFloatMap$Entry constructor rewrites
      "com.badlogic.gdx.utils.IntIntMap$Entry" -> Map(
        ("<init>", 0) -> Construct("scala.Tuple2", "apply", fillTypeArgs = true)
      ),
      "com.badlogic.gdx.utils.IntFloatMap$Entry" -> Map(
        ("<init>", 0) -> Construct("scala.Tuple2", "apply", fillTypeArgs = true)
      ),
      "com.badlogic.gdx.utils.ObjectSet" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ObjectSet", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ObjectSet", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ObjectSet", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        // 3.1aw: bare-set iteration. Java's ObjectSet implements Iterable<T> and iterates via
        // iterator(); lls ObjectSet has inline `foreach` but no `iterator`. The `"entries"` key
        // is the synthetic lookup key for the retargetForEach bare-ref path — it fires on
        // `for (T x : set)` the same way `("entries", 0) -> ForEach("foreachEntry", 2)` fires
        // on `for (Entry e : map)`. ForEach("foreach", 1) produces `set.foreach(x => body)`.
        ("entries", 0) -> ForEach("foreach", 1)
      ),
      "com.badlogic.gdx.utils.OrderedMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.OrderedMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.OrderedMap", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.OrderedMap", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1al: same get overload fix as ObjectMap
        ("get", 1) -> Template("$recv.get($0).orNull")
      ),
      "com.badlogic.gdx.utils.OrderedSet" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.OrderedSet", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.OrderedSet", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.OrderedSet", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("iterator", 0) -> Chain(List("orderedItems", "iterator")),
        // --- 3.1aj: static factory `OrderedSet.with(T... array)` ---
        // The vararg is packed into a scala.Array by the frontend. lls OrderedSet has no `with`;
        // create a set and add each element from the packed array.
        ("with", 1) -> Template(
          "{ val bpArr = $0; val bpSet = $Target.apply[$T0](); var bpI = 0; while (bpI < bpArr.length) { bpSet.add(bpArr(bpI)); bpI += 1 }; bpSet }"
        ),
        // 3.1aw: bare-set iteration — same as ObjectSet above.
        ("entries", 0) -> ForEach("foreach", 1)
      ),
      "com.badlogic.gdx.utils.IdentityMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ArrayMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ArrayMap", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ArrayMap", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1al: same get overload fix as ObjectMap
        ("get", 1) -> Template("$recv.get($0).orNull"),
        // --- 3.1as: IdentityMap -> ArrayMap has removeKey, not remove. Same as ArrayMap entry.
        ("remove", 1) -> Rename("removeKey")
      ),
      // wave 3.1d: remaining MAP family — all to ObjectMap, same Construct + ForEach pattern.
      // IntMap<V> -> ObjectMap[Int, V], LongMap<V> -> ObjectMap[Long, V],
      // IntIntMap -> ObjectMap[Int, Int], IntFloatMap -> ObjectMap[Int, Float],
      // ObjectIntMap<K> -> ObjectMap[K, Int], ObjectFloatMap<K> -> ObjectMap[K, Float],
      // ObjectLongMap<K> -> ObjectMap[K, Long].
      "com.badlogic.gdx.utils.IntMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1al: same get overload fix as ObjectMap
        ("get", 1) -> Template("$recv.get($0).orNull")
      ),
      "com.badlogic.gdx.utils.LongMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1al: same get overload fix as ObjectMap
        ("get", 1) -> Template("$recv.get($0).orNull")
      ),
      "com.badlogic.gdx.utils.IntIntMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1ah: dependents ---
        // getAndIncrement(key, default, increment): if key present, return old and store old+inc;
        // if absent, store default+inc and return default. ObjectMap.get(K,V) provides the default.
        ("getAndIncrement", 3) -> Template("{ val bpK = $0; val bpOld = $recv.get(bpK, $1); $recv.put(bpK, bpOld + $2); bpOld }"),
        // remove(key, default): return removed value or default if absent.
        // ObjectMap.remove(K) returns null if absent; cast the boxed result.
        ("remove", 2) -> Template("$recv.remove($0).getOrElse($1)")
      ),
      "com.badlogic.gdx.utils.IntFloatMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1ah: dependents ---
        ("getAndIncrement", 3) -> Template("{ val bpK = $0; val bpOld = $recv.get(bpK, $1); $recv.put(bpK, bpOld + $2); bpOld }"),
        ("remove", 2) -> Template("$recv.remove($0).getOrElse($1)")
      ),
      "com.badlogic.gdx.utils.ObjectIntMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1ah: dependents ---
        ("getAndIncrement", 3) -> Template("{ val bpK = $0; val bpOld = $recv.get(bpK, $1); $recv.put(bpK, bpOld + $2); bpOld }"),
        ("remove", 2) -> Template("$recv.remove($0).getOrElse($1)")
      ),
      "com.badlogic.gdx.utils.ObjectFloatMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1ah: dependents ---
        ("getAndIncrement", 3) -> Template("{ val bpK = $0; val bpOld = $recv.get(bpK, $1); $recv.put(bpK, bpOld + $2); bpOld }"),
        ("remove", 2) -> Template("$recv.remove($0).getOrElse($1)")
      ),
      "com.badlogic.gdx.utils.ObjectLongMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ObjectMap", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1ah: dependents ---
        ("getAndIncrement", 3) -> Template("{ val bpK = $0; val bpOld = $recv.get(bpK, $1); $recv.put(bpK, bpOld + $2); bpOld }"),
        ("remove", 2) -> Template("$recv.remove($0).getOrElse($1)")
      ),
      // wave 3.1d: gdx's ArrayMap -> lowlevel.util.ArrayMap (same as IdentityMap's target)
      "com.badlogic.gdx.utils.ArrayMap" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ArrayMap", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ArrayMap", "apply"),
        // arity-2: ArrayMap(MkArray, MkArray) — two array factory lambdas lls does not need;
        // sge uses the no-args constructor: `ArrayMap[Node, Matrix4]()`. dropTrailing = 2.
        ("<init>", 2) -> Construct("lowlevel.util.ArrayMap", "apply", dropTrailing = 2),
        // arity-4: ArrayMap(boolean, int, Class, Class) — last 2 are Class tokens lls does not need
        ("<init>", 4) -> Construct("lowlevel.util.ArrayMap", "apply", dropTrailing = 2),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreachEntry", 2),
        ("iterator", 0) -> ForEach("foreachEntry", 2), // java's iterator() is Entries
        ("keys", 0) -> Collect("foreachKey", "lowlevel.util.DynamicArray"),
        ("values", 0) -> Collect("foreachValue", "lowlevel.util.DynamicArray"),
        // --- 3.1al: same get overload fix as ObjectMap
        ("get", 1) -> Template("$recv.get($0).orNull"),
        // --- 3.1aj: ArrayMap.remove(K) -> removeKey(K). lls ArrayMap has removeKey, not remove.
        ("remove", 1) -> Rename("removeKey")
      ),
      // wave 3.1d: IntSet -> ObjectSet. Sets iterate through themselves (Iterable<Integer>).
      // 3.1aw: bare-set iteration via ForEach("foreach", 1) — lls ObjectSet has inline `foreach`.
      "com.badlogic.gdx.utils.IntSet" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.ObjectSet", "apply"),
        ("<init>", 1) -> Construct("lowlevel.util.ObjectSet", "apply"),
        ("<init>", 2) -> Construct("lowlevel.util.ObjectSet", "apply"),
        ("notEmpty", 0) -> Rename("nonEmpty"),
        ("entries", 0) -> ForEach("foreach", 1)
      ),
      // wave 3.1n: Array family -> DynamicArray. A type-parameter T at a construction needs
      // MkArray[T] threaded: COUNTED. BoolDispatch: Array's `identity` boolean at flagIndex=1
      // dispatches to ByRef/non-ByRef. No ForEach: DynamicArray supports `for (x <- da)` natively.
      "com.badlogic.gdx.utils.Array" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 1) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef), // fallback — desc keys win
        ("<init>", 2) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        // arity 3: Array(boolean, int, ArraySupplier) — drop the ArraySupplier (lls uses MkArray).
        // Array(boolean, int, Class) is in dropMethods.
        ("<init>", 3) -> Construct("lowlevel.util.DynamicArray", "apply", dropTrailing = 1, typeVarEvidence = mkArrayRef),
        ("get", 1) -> Rename("apply"),
        ("set", 2) -> Rename("update"),
        ("removeValue", 2) -> BoolDispatch(1, "removeValueByRef", "removeValue"),
        ("contains", 2) -> BoolDispatch(1, "containsByRef", "contains"),
        ("indexOf", 2) -> BoolDispatch(1, "indexOfByRef", "indexOf"),
        ("lastIndexOf", 2) -> BoolDispatch(1, "lastIndexOfByRef", "lastIndexOf"),
        ("containsAll", 2) -> BoolDispatch(1, "containsAllByRef", "containsAll"),
        ("containsAny", 2) -> BoolDispatch(1, "containsAnyByRef", "containsAny"),
        ("removeAll", 2) -> BoolDispatch(1, "removeAllByRef", "removeAll"),
        ("replaceFirst", 3) -> BoolDispatch(1, "replaceFirstByRef", "replaceFirst"),
        ("replaceAll", 3) -> BoolDispatch(1, "replaceAllByRef", "replaceAll"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        // bean-property renamed isEmpty() -> empty; lls keeps isEmpty
        ("empty", 0) -> Rename("isEmpty"),
        // parameterless methods: DynamicArray declares peek, first, iterator, nonEmpty as
        // parameterless (no ()) but java calls them with (). Chain with empty parens set
        // produces arr.peek without (). lls methods are parameterless.
        ("iterator", 0) -> Chain(List("iterator")),
        // wave 3.1o: field-write images. DynamicArray exposes `size` as a METHOD (getter only),
        // so `arr.size = n` must become `arr.setSize(n)`. `setSize` handles both growing (pads
        // with defaults) and shrinking (truncates), which is java's Array.size field semantics.
        // `arr.ordered` read -> `arr.preserveOrder`; write dropped (immutable ctor param).
        ("size", 0) -> FieldWrite("size", "setSize"),
        ("ordered", 0) -> DropWrite("ordered", "preserveOrder", "DynamicArray always preserves order; preserveOrder is a constructor parameter"),
        ("items", 0) -> IndexedField("items"),
        // wave 3.1p: toArray. DynamicArray.toArray is parenless (def toArray: Array[A], ClassTag-based).
        // Java's Array.toArray() -> da.toArray (parenless). Java's Array.toArray(Class) drops the
        // Class arg — DynamicArray uses inline ClassTag, not a Class parameter.
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        // CharArray.append(char[], off, len) -> DynamicArray.addAll(raw, off, len)
        ("append", 3) -> Rename("addAll"),
        // --- 3.1aj: static factory `Array.with(T... array)` -> create + addAll from packed array.
        // The vararg is packed into a scala.Array by the frontend. DynamicArray.from takes a
        // DynamicArray, not a scala.Array, so create + addAll(Object, Int, Int).
        ("with", 1) -> Template("{ val bpW = $0; val bpWd = $Target.apply[$T0](bpW.length); bpWd.addAll(bpW, 0, bpW.length); bpWd }"),
        // --- 3.1aj: static factory `Array.of(boolean, int, ArraySupplier/Class)` and `Array.of(Class/Supplier)`.
        // DynamicArray uses inline MkArray; the supplier/class arg is dropped.
        ("of", 3) -> Template("$Target.apply[$T0]($1)"),
        ("of", 1) -> Template("$Target.apply[$T0]()"),
        // wave 3.1au: toString(sep) needs no brackets (java joins bare) -- iterator.mkString matches
        ("toString", 1) -> Template("$recv.iterator.mkString($0)"),
        // peek/first/pop restate java's IllegalStateException (lls throws IndexOutOfBounds)
        ("peek", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.peek }"),
        ("first", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.first }"),
        ("pop", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.pop() }"),
        // removeRange: java's end is INCLUSIVE and refuses end>=size / start>end; lls is exclusive
        ("removeRange", 2) -> Template(
          "{ val bpS = $0; val bpE = $1; if (bpE >= $recv.size) throw new java.lang.IndexOutOfBoundsException(\"end can't be >= size: \" + bpE + \" >= \" + $recv.size); if (bpS > bpE) throw new java.lang.IndexOutOfBoundsException(\"start can't be > end: \" + bpS + \" > \" + bpE); $recv.removeRange(bpS, bpE + 1) }"
        )
      ),
      // SnapshotArray extends Array — same rewrites. lls DynamicArray has begin()/end() for
      // snapshot support (sge type-mappings.md: "SnapshotArray -> ArrayBuffer with copy-on-modify";
      // lls DynamicArray has begin/end built in).
      "com.badlogic.gdx.utils.SnapshotArray" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 1) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef), // fallback — desc keys win
        ("<init>", 2) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 3) -> Construct("lowlevel.util.DynamicArray", "apply", dropTrailing = 1, typeVarEvidence = mkArrayRef),
        ("get", 1) -> Rename("apply"),
        ("set", 2) -> Rename("update"),
        ("removeValue", 2) -> BoolDispatch(1, "removeValueByRef", "removeValue"),
        ("contains", 2) -> BoolDispatch(1, "containsByRef", "contains"),
        ("indexOf", 2) -> BoolDispatch(1, "indexOfByRef", "indexOf"),
        ("lastIndexOf", 2) -> BoolDispatch(1, "lastIndexOfByRef", "lastIndexOf"),
        ("containsAll", 2) -> BoolDispatch(1, "containsAllByRef", "containsAll"),
        ("containsAny", 2) -> BoolDispatch(1, "containsAnyByRef", "containsAny"),
        ("removeAll", 2) -> BoolDispatch(1, "removeAllByRef", "removeAll"),
        ("replaceFirst", 3) -> BoolDispatch(1, "replaceFirstByRef", "replaceFirst"),
        ("replaceAll", 3) -> BoolDispatch(1, "replaceAllByRef", "replaceAll"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        ("empty", 0) -> Rename("isEmpty"),
        ("iterator", 0) -> Chain(List("iterator")),
        ("size", 0) -> FieldWrite("size", "setSize"),
        ("ordered", 0) -> DropWrite("ordered", "preserveOrder", "DynamicArray always preserves order; preserveOrder is a constructor parameter"),
        ("items", 0) -> IndexedField("items"),
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        ("append", 3) -> Rename("addAll"),
        ("with", 1) -> Template("{ val bpW = $0; val bpWd = $Target.apply[$T0](bpW.length); bpWd.addAll(bpW, 0, bpW.length); bpWd }"),
        // wave 3.1au: toString(sep) needs no brackets (java joins bare) -- iterator.mkString matches
        ("toString", 1) -> Template("$recv.iterator.mkString($0)"),
        // peek/first/pop restate java's IllegalStateException (lls throws IndexOutOfBounds)
        ("peek", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.peek }"),
        ("first", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.first }"),
        ("pop", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.pop() }"),
        // removeRange: java's end is INCLUSIVE and refuses end>=size / start>end; lls is exclusive
        ("removeRange", 2) -> Template(
          "{ val bpS = $0; val bpE = $1; if (bpE >= $recv.size) throw new java.lang.IndexOutOfBoundsException(\"end can't be >= size: \" + bpE + \" >= \" + $recv.size); if (bpS > bpE) throw new java.lang.IndexOutOfBoundsException(\"start can't be > end: \" + bpS + \" > \" + bpE); $recv.removeRange(bpS, bpE + 1) }"
        )
      ),
      "com.badlogic.gdx.utils.DelayedRemovalArray" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 1) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef), // fallback — desc keys win
        ("<init>", 2) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 3) -> Construct("lowlevel.util.DynamicArray", "apply", dropTrailing = 1, typeVarEvidence = mkArrayRef),
        ("get", 1) -> Rename("apply"),
        ("set", 2) -> Rename("update"),
        ("removeValue", 2) -> BoolDispatch(1, "removeValueByRef", "removeValue"),
        ("contains", 2) -> BoolDispatch(1, "containsByRef", "contains"),
        ("indexOf", 2) -> BoolDispatch(1, "indexOfByRef", "indexOf"),
        ("lastIndexOf", 2) -> BoolDispatch(1, "lastIndexOfByRef", "lastIndexOf"),
        ("containsAll", 2) -> BoolDispatch(1, "containsAllByRef", "containsAll"),
        ("containsAny", 2) -> BoolDispatch(1, "containsAnyByRef", "containsAny"),
        ("removeAll", 2) -> BoolDispatch(1, "removeAllByRef", "removeAll"),
        ("replaceFirst", 3) -> BoolDispatch(1, "replaceFirstByRef", "replaceFirst"),
        ("replaceAll", 3) -> BoolDispatch(1, "replaceAllByRef", "replaceAll"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        ("empty", 0) -> Rename("isEmpty"),
        ("iterator", 0) -> Chain(List("iterator")),
        ("size", 0) -> FieldWrite("size", "setSize"),
        ("ordered", 0) -> DropWrite("ordered", "preserveOrder", "DynamicArray always preserves order; preserveOrder is a constructor parameter"),
        ("items", 0) -> IndexedField("items"),
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        ("append", 3) -> Rename("addAll"),
        ("with", 1) -> Template("{ val bpW = $0; val bpWd = $Target.apply[$T0](bpW.length); bpWd.addAll(bpW, 0, bpW.length); bpWd }"),
        // wave 3.1au: toString(sep) needs no brackets (java joins bare) -- iterator.mkString matches
        ("toString", 1) -> Template("$recv.iterator.mkString($0)"),
        // peek/first/pop restate java's IllegalStateException (lls throws IndexOutOfBounds)
        ("peek", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.peek }"),
        ("first", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.first }"),
        ("pop", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.pop() }"),
        // removeRange: java's end is INCLUSIVE and refuses end>=size / start>end; lls is exclusive
        ("removeRange", 2) -> Template(
          "{ val bpS = $0; val bpE = $1; if (bpE >= $recv.size) throw new java.lang.IndexOutOfBoundsException(\"end can't be >= size: \" + bpE + \" >= \" + $recv.size); if (bpS > bpE) throw new java.lang.IndexOutOfBoundsException(\"start can't be > end: \" + bpS + \" > \" + bpE); $recv.removeRange(bpS, bpE + 1) }"
        )
      ),
      // Primitive arrays: no identity flag (no BoolDispatch needed), same get->apply, set->update.
      // sge type-mappings.md: "IntArray -> DynamicArray[Int]", etc.
      "com.badlogic.gdx.utils.IntArray" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 1) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef), // fallback — desc keys win
        ("<init>", 2) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("get", 1) -> Rename("apply"),
        ("set", 2) -> Rename("update"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        ("empty", 0) -> Rename("isEmpty"),
        ("iterator", 0) -> Chain(List("iterator")),
        ("size", 0) -> FieldWrite("size", "setSize"),
        ("ordered", 0) -> DropWrite("ordered", "preserveOrder", "DynamicArray always preserves order; preserveOrder is a constructor parameter"),
        ("items", 0) -> IndexedField("items"),
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        // IntArray.incr(index, value) -> { val i = index; da(i) = da(i) + value }
        // $recv appears twice — Template binds it to a temp.
        ("incr", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = $recv(bpIdx) + $1 }"),
        // IntArray.add(4 args): DynamicArray has up to 3-arg add; split into two calls.
        ("add", 4) -> Template("{ $recv.add($0, $1); $recv.add($2, $3) }"),
        ("with", 1) -> Template("{ val bpW = $0; val bpWd = $Target.apply[$T0](bpW.length); bpWd.addAll(bpW, 0, bpW.length); bpWd }"),
        // wave 3.1au: toString(sep) needs no brackets (java joins bare) -- iterator.mkString matches
        ("toString", 1) -> Template("$recv.iterator.mkString($0)"),
        // peek/first/pop restate java's IllegalStateException (lls throws IndexOutOfBounds)
        ("peek", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.peek }"),
        ("first", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.first }"),
        ("pop", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.pop() }"),
        // removeRange: java's end is INCLUSIVE and refuses end>=size / start>end; lls is exclusive
        ("removeRange", 2) -> Template(
          "{ val bpS = $0; val bpE = $1; if (bpE >= $recv.size) throw new java.lang.IndexOutOfBoundsException(\"end can't be >= size: \" + bpE + \" >= \" + $recv.size); if (bpS > bpE) throw new java.lang.IndexOutOfBoundsException(\"start can't be > end: \" + bpS + \" > \" + bpE); $recv.removeRange(bpS, bpE + 1) }"
        ),
        // --- 3.1ah: dependents ---
        // shrink/resize/ensureCapacity/setSize return int[] in java; DynamicArray returns Unit.
        // Return the backing array after each, matching java's return type.
        ("shrink", 0) -> Template("{ $recv.shrink(); $recv }.items"),
        ("resize", 1) -> Template("{ $recv.setSize($0); $recv }.items"),
        ("ensureCapacity", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"additionalCapacity must be >= 0: \" + bpN); val bpNeeded = $recv.size + bpN; if (bpNeeded > $recv.items.length) $recv.ensureCapacity(java.lang.Math.max(java.lang.Math.max(8, bpNeeded), ($recv.size * 1.75f).toInt) - $recv.size); $recv }.items"
        ),
        ("setSize", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"newSize must be >= 0: \" + bpN); $recv.setSize(bpN); $recv }.items"
        ),
        ("incr", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = $recv(bpI) + $0; bpI += 1 } }"),
        ("mul", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = $recv(bpIdx) * $1 }"),
        ("mul", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = $recv(bpI) * $0; bpI += 1 } }")
      ),
      "com.badlogic.gdx.utils.FloatArray" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 1) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef), // fallback — desc keys win
        ("<init>", 2) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("get", 1) -> Rename("apply"),
        ("set", 2) -> Rename("update"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        ("empty", 0) -> Rename("isEmpty"),
        ("iterator", 0) -> Chain(List("iterator")),
        ("size", 0) -> FieldWrite("size", "setSize"),
        ("ordered", 0) -> DropWrite("ordered", "preserveOrder", "DynamicArray always preserves order; preserveOrder is a constructor parameter"),
        ("items", 0) -> IndexedField("items"),
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        ("with", 1) -> Template("{ val bpW = $0; val bpWd = $Target.apply[$T0](bpW.length); bpWd.addAll(bpW, 0, bpW.length); bpWd }"),
        // wave 3.1au: toString(sep) needs no brackets (java joins bare) -- iterator.mkString matches
        ("toString", 1) -> Template("$recv.iterator.mkString($0)"),
        // peek/first/pop restate java's IllegalStateException (lls throws IndexOutOfBounds)
        ("peek", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.peek }"),
        ("first", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.first }"),
        ("pop", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.pop() }"),
        // removeRange: java's end is INCLUSIVE and refuses end>=size / start>end; lls is exclusive
        ("removeRange", 2) -> Template(
          "{ val bpS = $0; val bpE = $1; if (bpE >= $recv.size) throw new java.lang.IndexOutOfBoundsException(\"end can't be >= size: \" + bpE + \" >= \" + $recv.size); if (bpS > bpE) throw new java.lang.IndexOutOfBoundsException(\"start can't be > end: \" + bpS + \" > \" + bpE); $recv.removeRange(bpS, bpE + 1) }"
        ),
        // --- 3.1ah: dependents ---
        ("shrink", 0) -> Template("{ $recv.shrink(); $recv }.items"),
        ("resize", 1) -> Template("{ $recv.setSize($0); $recv }.items"),
        ("ensureCapacity", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"additionalCapacity must be >= 0: \" + bpN); val bpNeeded = $recv.size + bpN; if (bpNeeded > $recv.items.length) $recv.ensureCapacity(java.lang.Math.max(java.lang.Math.max(8, bpNeeded), ($recv.size * 1.75f).toInt) - $recv.size); $recv }.items"
        ),
        ("setSize", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"newSize must be >= 0: \" + bpN); $recv.setSize(bpN); $recv }.items"
        ),
        ("incr", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = $recv(bpIdx) + $1 }"),
        ("incr", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = $recv(bpI) + $0; bpI += 1 } }"),
        ("mul", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = $recv(bpIdx) * $1 }"),
        ("mul", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = $recv(bpI) * $0; bpI += 1 } }"),
        ("add", 4) -> Template("{ $recv.add($0, $1); $recv.add($2, $3) }")
      ),
      "com.badlogic.gdx.utils.LongArray" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 1) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef), // fallback — desc keys win
        ("<init>", 2) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("get", 1) -> Rename("apply"),
        ("set", 2) -> Rename("update"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        ("empty", 0) -> Rename("isEmpty"),
        ("iterator", 0) -> Chain(List("iterator")),
        ("size", 0) -> FieldWrite("size", "setSize"),
        ("ordered", 0) -> DropWrite("ordered", "preserveOrder", "DynamicArray always preserves order; preserveOrder is a constructor parameter"),
        ("items", 0) -> IndexedField("items"),
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        // LongArray.incr(index, value) -> { val i = index; da(i) = da(i) + value }
        ("incr", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = $recv(bpIdx) + $1 }"),
        // LongArray.incr(value) -> add value to ALL elements
        ("incr", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = $recv(bpI) + $0; bpI += 1 } }"),
        // LongArray.mul(index, value) -> { val i = index; da(i) = da(i) * value }
        ("mul", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = $recv(bpIdx) * $1 }"),
        // LongArray.mul(value) -> multiply ALL elements
        ("mul", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = $recv(bpI) * $0; bpI += 1 } }"),
        ("with", 1) -> Template("{ val bpW = $0; val bpWd = $Target.apply[$T0](bpW.length); bpWd.addAll(bpW, 0, bpW.length); bpWd }"),
        // --- 3.1ae: gdx-test residue ---
        // LongArray.add(4 args): DynamicArray has up to 3-arg add; split into two calls.
        ("add", 4) -> Template("{ $recv.add($0, $1); $recv.add($2, $3) }"),
        // LongArray.shrink() returns long[] in java; DynamicArray.shrink() returns Unit.
        // Return the backing array after shrink, matching java's return type.
        ("shrink", 0) -> Template("{ $recv.shrink(); $recv }.items"),
        // LongArray.resize(int) is protected, returns long[]; DynamicArray has no resize.
        // setSize + items is the faithful image: allocate to newSize, pad with zeros, return array.
        ("resize", 1) -> Template("{ $recv.setSize($0); $recv }.items"),
        // wave 3.1au: toString(sep) needs no brackets (java joins bare) -- iterator.mkString matches
        ("toString", 1) -> Template("$recv.iterator.mkString($0)"),
        // peek/first/pop restate java's IllegalStateException (lls throws IndexOutOfBounds)
        ("peek", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.peek }"),
        ("first", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.first }"),
        ("pop", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.pop() }"),
        // removeRange: java's end is INCLUSIVE and refuses end>=size / start>end; lls is exclusive
        ("removeRange", 2) -> Template(
          "{ val bpS = $0; val bpE = $1; if (bpE >= $recv.size) throw new java.lang.IndexOutOfBoundsException(\"end can't be >= size: \" + bpE + \" >= \" + $recv.size); if (bpS > bpE) throw new java.lang.IndexOutOfBoundsException(\"start can't be > end: \" + bpS + \" > \" + bpE); $recv.removeRange(bpS, bpE + 1) }"
        ),
        // ensureCapacity(n) RETURNS the backing array in java and refuses n < 0; it grows only
        // when size + n exceeds the array, and then to max(max(8, size + n), size * 1.75)
        // (LongArray.java:347-351). lls returns Unit and grows to exactly size + n, so the
        // returned array's LENGTH — the thing LongArrayTest.ensureCapacityTest asserts — differs.
        // Java's own growth rule is restated; the `.items` read is the java return.
        ("ensureCapacity", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"additionalCapacity must be >= 0: \" + bpN); val bpNeeded = $recv.size + bpN; if (bpNeeded > $recv.items.length) $recv.ensureCapacity(java.lang.Math.max(java.lang.Math.max(8, bpNeeded), ($recv.size * 1.75f).toInt) - $recv.size); $recv }.items"
        ),
        // setSize(n) returns the backing array and refuses n < 0 (LongArray.java:356-361); lls
        // returns Unit and sets a negative size silently. Growth agrees: both go to max(8, n).
        ("setSize", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"newSize must be >= 0: \" + bpN); $recv.setSize(bpN); $recv }.items"
        )
      ),
      "com.badlogic.gdx.utils.ShortArray" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 1) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef), // fallback — desc keys win
        ("<init>", 2) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("get", 1) -> Rename("apply"),
        ("set", 2) -> Rename("update"),
        // wave 3.1t: Java implicitly narrows int->short at ShortArray.add(short). After retarget,
        // DynamicArray[Short].add(Short) does not accept Int. Insert .toShort cast.
        ("add", 1) -> Template("$recv.add($0.toShort)"),
        ("add", 2) -> Template("$recv.add($0.toShort, $1.toShort)"),
        ("add", 3) -> Template("$recv.add($0.toShort, $1.toShort, $2.toShort)"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        ("empty", 0) -> Rename("isEmpty"),
        ("iterator", 0) -> Chain(List("iterator")),
        ("size", 0) -> FieldWrite("size", "setSize"),
        ("ordered", 0) -> DropWrite("ordered", "preserveOrder", "DynamicArray always preserves order; preserveOrder is a constructor parameter"),
        ("items", 0) -> IndexedField("items"),
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        ("with", 1) -> Template("{ val bpW = $0; val bpWd = $Target.apply[$T0](bpW.length); bpWd.addAll(bpW, 0, bpW.length); bpWd }"),
        // wave 3.1au: toString(sep) needs no brackets (java joins bare) -- iterator.mkString matches
        ("toString", 1) -> Template("$recv.iterator.mkString($0)"),
        // peek/first/pop restate java's IllegalStateException (lls throws IndexOutOfBounds)
        ("peek", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.peek }"),
        ("first", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.first }"),
        ("pop", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.pop() }"),
        // removeRange: java's end is INCLUSIVE and refuses end>=size / start>end; lls is exclusive
        ("removeRange", 2) -> Template(
          "{ val bpS = $0; val bpE = $1; if (bpE >= $recv.size) throw new java.lang.IndexOutOfBoundsException(\"end can't be >= size: \" + bpE + \" >= \" + $recv.size); if (bpS > bpE) throw new java.lang.IndexOutOfBoundsException(\"start can't be > end: \" + bpS + \" > \" + bpE); $recv.removeRange(bpS, bpE + 1) }"
        ),
        // --- 3.1ah: dependents ---
        ("shrink", 0) -> Template("{ $recv.shrink(); $recv }.items"),
        ("resize", 1) -> Template("{ $recv.setSize($0); $recv }.items"),
        ("ensureCapacity", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"additionalCapacity must be >= 0: \" + bpN); val bpNeeded = $recv.size + bpN; if (bpNeeded > $recv.items.length) $recv.ensureCapacity(java.lang.Math.max(java.lang.Math.max(8, bpNeeded), ($recv.size * 1.75f).toInt) - $recv.size); $recv }.items"
        ),
        ("setSize", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"newSize must be >= 0: \" + bpN); $recv.setSize(bpN); $recv }.items"
        ),
        ("incr", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = ($recv(bpIdx) + $1).toShort }"),
        ("incr", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = ($recv(bpI) + $0).toShort; bpI += 1 } }"),
        ("mul", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = ($recv(bpIdx) * $1).toShort }"),
        ("mul", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = ($recv(bpI) * $0).toShort; bpI += 1 } }"),
        ("add", 4) -> Template("{ $recv.add($0.toShort, $1.toShort); $recv.add($2.toShort, $3.toShort) }")
      ),
      "com.badlogic.gdx.utils.ByteArray" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 1) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef), // fallback — desc keys win
        ("<init>", 2) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("get", 1) -> Rename("apply"),
        ("set", 2) -> Rename("update"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        ("empty", 0) -> Rename("isEmpty"),
        ("iterator", 0) -> Chain(List("iterator")),
        ("size", 0) -> FieldWrite("size", "setSize"),
        ("ordered", 0) -> DropWrite("ordered", "preserveOrder", "DynamicArray always preserves order; preserveOrder is a constructor parameter"),
        ("items", 0) -> IndexedField("items"),
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        ("with", 1) -> Template("{ val bpW = $0; val bpWd = $Target.apply[$T0](bpW.length); bpWd.addAll(bpW, 0, bpW.length); bpWd }"),
        // wave 3.1au: toString(sep) needs no brackets (java joins bare) -- iterator.mkString matches
        ("toString", 1) -> Template("$recv.iterator.mkString($0)"),
        // peek/first/pop restate java's IllegalStateException (lls throws IndexOutOfBounds)
        ("peek", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.peek }"),
        ("first", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.first }"),
        ("pop", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.pop() }"),
        // removeRange: java's end is INCLUSIVE and refuses end>=size / start>end; lls is exclusive
        ("removeRange", 2) -> Template(
          "{ val bpS = $0; val bpE = $1; if (bpE >= $recv.size) throw new java.lang.IndexOutOfBoundsException(\"end can't be >= size: \" + bpE + \" >= \" + $recv.size); if (bpS > bpE) throw new java.lang.IndexOutOfBoundsException(\"start can't be > end: \" + bpS + \" > \" + bpE); $recv.removeRange(bpS, bpE + 1) }"
        ),
        // ensureCapacity(n) RETURNS the backing array in java and refuses n < 0; it grows only
        // when size + n exceeds the array, and then to max(max(8, size + n), size * 1.75)
        // (LongArray.java:347-351). lls returns Unit and grows to exactly size + n, so the
        // returned array's LENGTH — the thing LongArrayTest.ensureCapacityTest asserts — differs.
        // Java's own growth rule is restated; the `.items` read is the java return.
        ("ensureCapacity", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"additionalCapacity must be >= 0: \" + bpN); val bpNeeded = $recv.size + bpN; if (bpNeeded > $recv.items.length) $recv.ensureCapacity(java.lang.Math.max(java.lang.Math.max(8, bpNeeded), ($recv.size * 1.75f).toInt) - $recv.size); $recv }.items"
        ),
        // --- 3.1ah: dependents ---
        ("shrink", 0) -> Template("{ $recv.shrink(); $recv }.items"),
        ("resize", 1) -> Template("{ $recv.setSize($0); $recv }.items"),
        ("setSize", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"newSize must be >= 0: \" + bpN); $recv.setSize(bpN); $recv }.items"
        ),
        ("incr", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = ($recv(bpIdx) + $1).toByte }"),
        ("incr", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = ($recv(bpI) + $0).toByte; bpI += 1 } }"),
        ("mul", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = ($recv(bpIdx) * $1).toByte }"),
        ("mul", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = ($recv(bpI) * $0).toByte; bpI += 1 } }"),
        ("add", 4) -> Template("{ $recv.add($0, $1); $recv.add($2, $3) }")
      ),
      "com.badlogic.gdx.utils.CharArray" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 1) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef), // fallback — desc keys win
        ("<init>", 2) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("get", 1) -> Rename("apply"),
        ("set", 2) -> Rename("update"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        ("empty", 0) -> Rename("isEmpty"),
        ("iterator", 0) -> Chain(List("iterator")),
        ("size", 0) -> FieldWrite("size", "setSize"),
        ("ordered", 0) -> DropWrite("ordered", "preserveOrder", "DynamicArray always preserves order; preserveOrder is a constructor parameter"),
        ("items", 0) -> IndexedField("items"),
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        ("append", 3) -> Rename("addAll"),
        ("with", 1) -> Template("{ val bpW = $0; val bpWd = $Target.apply[$T0](bpW.length); bpWd.addAll(bpW, 0, bpW.length); bpWd }"),
        // wave 3.1au: toString(sep) needs no brackets (java joins bare) -- iterator.mkString matches
        ("toString", 1) -> Template("$recv.iterator.mkString($0)"),
        // item 7: CharArray.toString() returns the chars, not DynamicArray's own toString.
        ("toString", 0) -> Template("new java.lang.String($recv.toArray)"),
        // peek/first/pop restate java's IllegalStateException (lls throws IndexOutOfBounds)
        ("peek", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.peek }"),
        ("first", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.first }"),
        ("pop", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.pop() }"),
        // removeRange: java's end is INCLUSIVE and refuses end>=size / start>end; lls is exclusive
        ("removeRange", 2) -> Template(
          "{ val bpS = $0; val bpE = $1; if (bpE >= $recv.size) throw new java.lang.IndexOutOfBoundsException(\"end can't be >= size: \" + bpE + \" >= \" + $recv.size); if (bpS > bpE) throw new java.lang.IndexOutOfBoundsException(\"start can't be > end: \" + bpS + \" > \" + bpE); $recv.removeRange(bpS, bpE + 1) }"
        ),
        // --- 3.1ah: dependents ---
        ("shrink", 0) -> Template("{ $recv.shrink(); $recv }.items"),
        ("resize", 1) -> Template("{ $recv.setSize($0); $recv }.items"),
        ("ensureCapacity", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"additionalCapacity must be >= 0: \" + bpN); val bpNeeded = $recv.size + bpN; if (bpNeeded > $recv.items.length) $recv.ensureCapacity(java.lang.Math.max(java.lang.Math.max(8, bpNeeded), ($recv.size * 1.75f).toInt) - $recv.size); $recv }.items"
        ),
        ("setSize", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"newSize must be >= 0: \" + bpN); $recv.setSize(bpN); $recv }.items"
        ),
        ("incr", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = ($recv(bpIdx) + $1).toChar }"),
        ("incr", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = ($recv(bpI) + $0).toChar; bpI += 1 } }"),
        ("mul", 2) -> Template("{ val bpIdx = $0; $recv(bpIdx) = ($recv(bpIdx) * $1).toChar }"),
        ("mul", 1) -> Template("{ var bpI = 0; while (bpI < $recv.size) { $recv(bpI) = ($recv(bpI) * $0).toChar; bpI += 1 } }"),
        ("add", 4) -> Template("{ $recv.add($0, $1); $recv.add($2, $3) }"),
        // CharArray.append(char) -> DynamicArray.add(char)
        ("append", 1) -> Rename("add")
      ),
      "com.badlogic.gdx.utils.BooleanArray" -> Map(
        ("<init>", 0) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("<init>", 1) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef), // fallback — desc keys win
        ("<init>", 2) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
        ("get", 1) -> Rename("apply"),
        ("set", 2) -> Rename("update"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        ("empty", 0) -> Rename("isEmpty"),
        ("iterator", 0) -> Chain(List("iterator")),
        ("size", 0) -> FieldWrite("size", "setSize"),
        ("ordered", 0) -> DropWrite("ordered", "preserveOrder", "DynamicArray always preserves order; preserveOrder is a constructor parameter"),
        ("items", 0) -> IndexedField("items"),
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        ("with", 1) -> Template("{ val bpW = $0; val bpWd = $Target.apply[$T0](bpW.length); bpWd.addAll(bpW, 0, bpW.length); bpWd }"),
        // wave 3.1au: toString(sep) needs no brackets (java joins bare) -- iterator.mkString matches
        ("toString", 1) -> Template("$recv.iterator.mkString($0)"),
        // peek/first/pop restate java's IllegalStateException (lls throws IndexOutOfBounds)
        ("peek", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.peek }"),
        ("first", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.first }"),
        ("pop", 0) -> Template("{ if ($recv.isEmpty) throw new java.lang.IllegalStateException(\"Array is empty.\"); $recv.pop() }"),
        // removeRange: java's end is INCLUSIVE and refuses end>=size / start>end; lls is exclusive
        ("removeRange", 2) -> Template(
          "{ val bpS = $0; val bpE = $1; if (bpE >= $recv.size) throw new java.lang.IndexOutOfBoundsException(\"end can't be >= size: \" + bpE + \" >= \" + $recv.size); if (bpS > bpE) throw new java.lang.IndexOutOfBoundsException(\"start can't be > end: \" + bpS + \" > \" + bpE); $recv.removeRange(bpS, bpE + 1) }"
        ),
        // --- 3.1ah: dependents ---
        ("shrink", 0) -> Template("{ $recv.shrink(); $recv }.items"),
        ("resize", 1) -> Template("{ $recv.setSize($0); $recv }.items"),
        ("ensureCapacity", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"additionalCapacity must be >= 0: \" + bpN); val bpNeeded = $recv.size + bpN; if (bpNeeded > $recv.items.length) $recv.ensureCapacity(java.lang.Math.max(java.lang.Math.max(8, bpNeeded), ($recv.size * 1.75f).toInt) - $recv.size); $recv }.items"
        ),
        ("setSize", 1) -> Template(
          "{ val bpN = $0; if (bpN < 0) throw new java.lang.IllegalArgumentException(\"newSize must be >= 0: \" + bpN); $recv.setSize(bpN); $recv }.items"
        )
      ),
      // Queue -> mutable.ArrayDeque (sge type-mappings.md: "Queue -> Scala stdlib queues";
      // sge's QueueBitsTest confirms mutable.ArrayDeque). addLast -> addOne, addFirst -> prepend,
      // removeLast -> removeLast, removeFirst -> removeHead. indexOf/removeValue/contains take
      // the identity boolean — dispatched via Template since ArrayDeque has no ByRef variants.
      "com.badlogic.gdx.utils.Queue" -> Map(
        ("<init>", 0) -> Template("scala.collection.mutable.ArrayDeque[$T0]()"),
        ("<init>", 1) -> Template("scala.collection.mutable.ArrayDeque[$T0]($0)"),
        ("get", 1) -> Rename("apply"),
        ("addLast", 1) -> Rename("addOne"),
        ("addFirst", 1) -> Rename("prepend"),
        ("removeLast", 0) -> Template("$recv.removeLast()"),
        ("removeFirst", 0) -> Template("$recv.removeHead()"),
        ("removeIndex", 1) -> Rename("remove"),
        ("removeValue", 2) -> Template(
          "{ val bpIdx = (if ($1) $recv.indexWhere(_ eq $0) else $recv.indexOf($0)); if (bpIdx >= 0) { $recv.remove(bpIdx); true } else false }"
        ),
        ("indexOf", 2) -> Template("(if ($1) $recv.indexWhere(_ eq $0) else $recv.indexOf($0))"),
        ("contains", 2) -> Template("(if ($1) $recv.exists(_ eq $0) else $recv.contains($0))"),
        ("notEmpty", 0) -> Chain(List("nonEmpty")),
        ("empty", 0) -> Rename("isEmpty"),
        ("first", 0) -> Chain(List("head")),
        ("last", 0) -> Chain(List("last")),
        ("peek", 0) -> Chain(List("head")),
        ("iterator", 0) -> Chain(List("iterator")),
        ("toArray", 0) -> Chain(List("toArray")),
        ("toArray", 1) -> Chain(List("toArray"), dropArgs = true),
        // toString format is part of QueueTest's contract; ONE template call, not a `+` chain
        // (a `+` hands a following `.equals` to the `"]"` literal -- 3 E007, wave 3.1af).
        ("toString", 0) -> Template("$recv.mkString(\"[\", \", \", \"]\")"),
        // Queue.toString(sep) -> elements joined by sep, no brackets.
        ("toString", 1) -> Template("$recv.mkString($0)")
      )
    )
  }

  /** DESCRIPTOR-KEYED retarget rewrites — for arity-1 constructors where `(name, arity)` is ambiguous. `Array` has four surviving arity-1 constructors: `(int)` capacity, `(ArraySupplier)` factory,
    * `(Array)` copy, `(T[])` from-array. A map from an over-approximate key to a single value is a choice nobody made.
    */
  def libCollectionConstructRewritesByDesc: Map[String, Map[(String, Descriptor), balticporter.transform.CollectionsTransform.RetargetRewrite]] = {
    import balticporter.transform.CollectionsTransform.RetargetRewrite.*
    val intDesc      = Descriptor(List(Param.Prim("int")))
    val arrayDesc    = Descriptor(List(Param.Named("Array")))
    val supplierDesc = Descriptor(List(Param.Named("ArraySupplier")))
    // wave 3.1t: Array(T[]) — copy-construct from a raw array. DynamicArray.from takes a
    // DynamicArray, not a raw scala.Array, so a Template constructs and addAll's. The $T0
    // placeholder is an AST hole (Tree.Ident with the type arg's head symbol), so
    // PackageRenameTransform reaches and renames the FQN correctly.
    val tArrDesc     = Descriptor(List(Param.Arr(Param.Named("T"))))
    val intArrDesc   = Descriptor(List(Param.Arr(Param.Prim("int"))))
    val floatArrDesc = Descriptor(List(Param.Arr(Param.Prim("float"))))
    val longArrDesc  = Descriptor(List(Param.Arr(Param.Prim("long"))))
    val shortArrDesc = Descriptor(List(Param.Arr(Param.Prim("short"))))
    val byteArrDesc  = Descriptor(List(Param.Arr(Param.Prim("byte"))))
    val charArrDesc  = Descriptor(List(Param.Arr(Param.Prim("char"))))
    val boolArrDesc  = Descriptor(List(Param.Arr(Param.Prim("boolean"))))
    // wave 3.1t: addAll(Array/Self, int, int) desc — the 3-arg addAll copies a range from
    // another Array/Self. After retarget, the first arg is DynamicArray, but
    // DynamicArray.addAll(Object, int, int) takes the raw BACKING ARRAY, not a DynamicArray.
    // Extract .items to pass the raw array.
    val addAllArrayDesc        = Descriptor(List(Param.Named("Array"), Param.Prim("int"), Param.Prim("int")))
    def genericArrayInitByDesc = Map(
      ("<init>", intDesc) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
      ("<init>", arrayDesc) -> Construct("lowlevel.util.DynamicArray", "from"),
      // Array(T[]) -> DynamicArray.from(array) — exact capacity. `.from` copies with
      // `items.length == array.length`, matching java's `Array(T[])` (items=clone, size=length);
      // the previous apply()+addAll left default capacity 16, breaking SortTest (8 failures from
      // trailing nulls). `$T0` resolves from the constructor's applied type; raw constructors
      // reach the supplier-derived path in `retargetConstruct` (engine, 3.1af).
      ("<init>", tArrDesc) -> Template("$Target.from[$T0]($0)"),
      ("<init>", supplierDesc) -> Construct("lowlevel.util.DynamicArray", "apply", dropTrailing = 1, typeVarEvidence = mkArrayRef),
      // Cast .items to Array[$T0] to handle wildcard argument types — java arrays are covariant,
      // scala arrays are invariant, so `DynamicArray[? <: T].items` is `Array[? <: T]` which
      // does not conform to `Array[T]`. The asInstanceOf is safe: items really IS an Array[T]
      // at erasure and addAll only reads from it.
      ("addAll", addAllArrayDesc) -> Template("$recv.addAll($0.items.asInstanceOf[scala.Array[$T0]], $1, $2)")
    )
    def primArrayInitByDesc(selfDesc: Descriptor, rawArrDesc: Descriptor, elemType: String) = Map(
      ("<init>", intDesc) -> Construct("lowlevel.util.DynamicArray", "apply", typeVarEvidence = mkArrayRef),
      ("<init>", selfDesc) -> Construct("lowlevel.util.DynamicArray", "from"),
      // wave 3.1af: PrimArray(prim[]) -> DynamicArray.from for exact capacity (LongArrayTest).
      ("<init>", rawArrDesc) -> Template(s"$$Target.from[$elemType]($$0)"),
      ("addAll", Descriptor(List(selfDesc.params.head, Param.Prim("int"), Param.Prim("int")))) ->
        Template("$recv.addAll($0.items, $1, $2)")
    )
    Map(
      "com.badlogic.gdx.utils.Array" -> genericArrayInitByDesc,
      "com.badlogic.gdx.utils.SnapshotArray" -> genericArrayInitByDesc,
      "com.badlogic.gdx.utils.DelayedRemovalArray" -> genericArrayInitByDesc,
      "com.badlogic.gdx.utils.IntArray" -> primArrayInitByDesc(Descriptor(List(Param.Named("IntArray"))), intArrDesc, "scala.Int"),
      "com.badlogic.gdx.utils.FloatArray" -> primArrayInitByDesc(Descriptor(List(Param.Named("FloatArray"))), floatArrDesc, "scala.Float"),
      "com.badlogic.gdx.utils.LongArray" -> primArrayInitByDesc(Descriptor(List(Param.Named("LongArray"))), longArrDesc, "scala.Long"),
      "com.badlogic.gdx.utils.ShortArray" -> primArrayInitByDesc(Descriptor(List(Param.Named("ShortArray"))), shortArrDesc, "scala.Short"),
      "com.badlogic.gdx.utils.ByteArray" -> primArrayInitByDesc(Descriptor(List(Param.Named("ByteArray"))), byteArrDesc, "scala.Byte"),
      // CharArray: init-by-desc AND append overloads (arity 1 is ambiguous — char/CharSequence/
      // String/Object/int, each a separate descriptor row below).
      "com.badlogic.gdx.utils.CharArray" -> (primArrayInitByDesc(Descriptor(List(Param.Named("CharArray"))), charArrDesc, "scala.Char") ++ Map(
        // CharArray(String) -> construct from string's char array.
        ("<init>", Descriptor(List(Param.Named("String")))) ->
          Template("{ val bpStr = $0; val bpDa = $Target[scala.Char](); bpDa.addAll(bpStr.toCharArray, 0, bpStr.length); bpDa }"),
        // CharArray(CharSequence) -> same as String, via toString.
        ("<init>", Descriptor(List(Param.Named("CharSequence")))) ->
          Template("{ val bpCs = $0.toString; val bpDa = $Target[scala.Char](); bpDa.addAll(bpCs.toCharArray, 0, bpCs.length); bpDa }"),
        ("append", Descriptor(List(Param.Prim("char")))) -> Rename("add"),
        // append(CharArray) -> addAll(other) — copies all chars from another CharArray.
        // After retarget both are DynamicArray[Char], and DynamicArray.addAll(DynamicArray) exists.
        ("append", Descriptor(List(Param.Named("CharArray")))) -> Rename("addAll"),
        // append(CharSequence/String) -> addAll(cs.toString.toCharArray, 0, len).
        // Template: $0 appears once, $recv appears once — no temp binding needed.
        ("append", Descriptor(List(Param.Named("CharSequence")))) ->
          Template("{ val bpCa = $0.toString.toCharArray; $recv.addAll(bpCa, 0, bpCa.length) }"),
        ("append", Descriptor(List(Param.Named("String")))) ->
          Template("{ val bpCa = $0.toString.toCharArray; $recv.addAll(bpCa, 0, bpCa.length) }"),
        // append(int) -> add(c.toChar) — the int is a codepoint, cast to Char.
        ("append", Descriptor(List(Param.Prim("int")))) ->
          Template("$recv.add($0.toChar)"),
        // item 7: append(Object) is java's own String.valueOf(obj) then the chars.
        ("append", Descriptor(List(Param.Named("Object")))) ->
          Template("{ val bpCa = java.lang.String.valueOf($0).toCharArray; $recv.addAll(bpCa, 0, bpCa.length); $recv }"),
        // indexOf(String) -> indexOf(Char): DynamicArray.indexOf takes a single element.
        // Java's CharArray.indexOf(String) is a substring search; the only occurrence
        // in gdx/src is indexOf("\n"), a single-char string, so .charAt(0) is faithful.
        ("indexOf", Descriptor(List(Param.Named("String")))) ->
          Template("$recv.indexOf($0.charAt(0))")
      )),
      "com.badlogic.gdx.utils.BooleanArray" -> primArrayInitByDesc(Descriptor(List(Param.Named("BooleanArray"))), boolArrDesc, "scala.Boolean"),
      // Queue: arity-1 is ambiguous — Queue(int) capacity vs Queue(int, Class) / Queue(int, ArraySupplier).
      // Queue(int) -> new ArrayDeque[T](capacity). Queue(int, Class) already in dropMethods.
      // Queue(int, ArraySupplier) already in dropMethods.
      "com.badlogic.gdx.utils.Queue" -> Map(
        ("<init>", intDesc) -> Template("new scala.collection.mutable.ArrayDeque[$T0]($0)"),
        ("<init>", Descriptor(List(Param.Prim("int"), Param.Named("Class")))) ->
          Template("new scala.collection.mutable.ArrayDeque[$T0]($0)"),
        ("<init>", Descriptor(List(Param.Prim("int"), Param.Named("ArraySupplier")))) ->
          Template("new scala.collection.mutable.ArrayDeque[$T0]($0)")
      ),
      // --- 3.1ae: gdx-test residue ---
      // Bits(Bits) copy constructor -> $0.clone(). arity 1 is ambiguous with Bits(int) capacity.
      // BitSet.clone() returns BitSet; the cast is safe because clone() on a mutable.BitSet
      // returns mutable.BitSet at runtime, but the static return type is Object.
      "com.badlogic.gdx.utils.Bits" -> Map(
        ("<init>", Descriptor(List(Param.Named("Bits")))) ->
          Template("$0.clone().asInstanceOf[scala.collection.mutable.BitSet]")
      ),
      // --- 3.1as: map copy-constructor descriptor keys. ObjectMap(ObjectMap other) at arity 1
      // is ambiguous with ObjectMap(int capacity). The copy-constructor creates a new map with the
      // source's capacity and copies all entries via putAll. lls ObjectMap.putAll exists.
      // Descriptor keys are in the UPSTREAM namespace (the frontend records from original Java).
      "com.badlogic.gdx.utils.ObjectMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("ObjectMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      "com.badlogic.gdx.utils.IntMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("IntMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      "com.badlogic.gdx.utils.LongMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("LongMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      "com.badlogic.gdx.utils.OrderedMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("OrderedMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      "com.badlogic.gdx.utils.ArrayMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("ArrayMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      "com.badlogic.gdx.utils.IntIntMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("IntIntMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      "com.badlogic.gdx.utils.IntFloatMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("IntFloatMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      "com.badlogic.gdx.utils.ObjectIntMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("ObjectIntMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      "com.badlogic.gdx.utils.ObjectFloatMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("ObjectFloatMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      "com.badlogic.gdx.utils.ObjectLongMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("ObjectLongMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      "com.badlogic.gdx.utils.IdentityMap" -> Map(
        ("<init>", Descriptor(List(Param.Named("IdentityMap")))) ->
          Template("{ val bpSrc = $0; val bpM = $Target.apply[$T0, $T1](bpSrc.size); bpM.putAll(bpSrc); bpM }")
      ),
      // 3.1aw: ObjectSet/OrderedSet/IntSet copy constructors — lls sets have `foreach`, not
      // `foreachKey` (that is a MAP method). lls `foreach` is `inline def foreach(inline f: A => Unit)`,
      // and `add` returns Boolean, so passing `bpS.add` as a method ref may not satisfy the inline
      // parameter. A lambda avoids the concern. OrderedSet.add is additionally overloaded (arity 1 and 2).
      "com.badlogic.gdx.utils.ObjectSet" -> Map(
        ("<init>", Descriptor(List(Param.Named("ObjectSet")))) ->
          Template("{ val bpSrc = $0; val bpS = $Target.apply[$T0](bpSrc.size); bpSrc.foreach(bpX => bpS.add(bpX)); bpS }"),
        // 3.1aw-2: addAll(T...) — the frontend packs the vararg into a scala.Array[T]. lls
        // ObjectSet has addAll(ObjectSet) and addAll(DynamicArray) but NOT addAll(Array).
        // Iterate the packed array and add each element.
        ("addAll", tArrDesc) ->
          Template("{ val bpArr = $0; var bpI = 0; while (bpI < bpArr.length) { $recv.add(bpArr(bpI)); bpI += 1 } }")
      ),
      "com.badlogic.gdx.utils.OrderedSet" -> Map(
        ("<init>", Descriptor(List(Param.Named("OrderedSet")))) ->
          Template("{ val bpSrc = $0; val bpS = $Target.apply[$T0](bpSrc.size); bpSrc.foreach(bpX => bpS.add(bpX)); bpS }")
      ),
      "com.badlogic.gdx.utils.IntSet" -> Map(
        ("<init>", Descriptor(List(Param.Named("IntSet")))) ->
          Template("{ val bpSrc = $0; val bpS = $Target.apply[$T0](bpSrc.size); bpSrc.foreach(bpX => bpS.add(bpX)); bpS }")
      )
    )
  }

  /** `com.badlogic.gdx.utils.Disposable` -> `java.lang.AutoCloseable`, `dispose` -> `close` — the JDK's own type under a different name. `memberRenames` renames the whole PRE-REDIRECT override
    * component (66 declarations) so unrelated `void dispose()` members elsewhere keep their name. Paired `dropTypes` entry is required. Shared surface, lives in [[core]]; `MergeablePolicy` folds
    * dependents' own redirects in.
    */
  def disposableRedirect: balticporter.transform.TypeRedirectTransform =
    new balticporter.transform.TypeRedirectTransform(
      redirects = Map("com.badlogic.gdx.utils.Disposable" -> "java.lang.AutoCloseable"),
      memberRenames = Map("com.badlogic.gdx.utils.Disposable" -> Map("dispose" -> "close"))
    )

  /** libGDX's JavaBean accessor pairs the reference hand port turned into Scala properties (`def x`/`def x_=`), harvested from sge's `Renames:` file headers. An include list, not a pattern: sge
    * converts only ~14% of get/set methods, inconsistently by type. Per-component, not per-implementor (would duplicate). Shared surface, lives in [[core]] alone; runs first in the pipeline.
    */
  def beanProperties: balticporter.transform.BeanPropertyTransform =
    // WHOLE-PROGRAM detection (Everywhere()); dependents follow the base's published shape
    // via PortMapTransform.followMemberRenames rather than re-deciding.
    new balticporter.transform.BeanPropertyTransform(beanPropertyPairs, beanPropertyTargets, scope = balticporter.tir.RuleScope.Everywhere())

  /** Which pairs collapse to a plain `var`/`val` instead of a `def` pair — `def-pair` is the default for everything not named here. The phase refuses a mismatch rather than picking (a counted
    * `idiom(refused)` row). Declared even for PERMANENT refusals so the run's denominator stays honest. `MapLayer#opacity` deliberately absent: its getter is computed, never a stored value.
    */
  def beanPropertyTargets: Map[String, balticporter.transform.BeanPropertyTransform.Target] = {
    import balticporter.transform.BeanPropertyTransform.Target
    Map(
      // -- `var`: a get/set pair, where a public `var` is exactly the surface java published
      "com.badlogic.gdx.graphics.profiling.GLProfiler#listener" -> Target.Var,
      "com.badlogic.gdx.maps.MapLayer#name" -> Target.Var,
      "com.badlogic.gdx.maps.MapLayer#parallaxX" -> Target.Var,
      "com.badlogic.gdx.maps.MapLayer#parallaxY" -> Target.Var,
      "com.badlogic.gdx.maps.MapLayer#visible" -> Target.Var,
      "com.badlogic.gdx.maps.MapObject#color" -> Target.Var,
      "com.badlogic.gdx.maps.MapObject#name" -> Target.Var,
      "com.badlogic.gdx.maps.MapObject#opacity" -> Target.Var,
      "com.badlogic.gdx.maps.MapObject#visible" -> Target.Var,
      "com.badlogic.gdx.maps.objects.PolygonMapObject#polygon" -> Target.Var,
      "com.badlogic.gdx.maps.objects.PolylineMapObject#polyline" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#bold" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#fontFamily" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#horizontalAlign" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#italic" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#kerning" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#pixelSize" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#rotation" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#strikeout" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#text" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#underline" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#verticalAlign" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#wrap" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#originX" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#originY" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#rotation" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#scaleX" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#scaleY" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#textureRegion" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#x" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#y" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#region" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#repeatX" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#repeatY" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#x" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#y" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapTileSet#name" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#flipHorizontally" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#flipVertically" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#tile" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#hexSideLength" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#staggerAxisX" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#staggerIndexEven" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#button" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#tapCount" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#tapSquareSize" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#dragTime" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.DragListener#button" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.DragListener#tapSquareSize" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.Selection#multiple" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.Selection#programmaticChangeEvents" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.Selection#required" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.Selection#toggle" -> Target.Var,
      // -- `val`: a get-only property over storage the declaration fills and nothing reassigns
      "com.badlogic.gdx.maps.Map#layers" -> Target.Val,
      "com.badlogic.gdx.maps.Map#properties" -> Target.Val,
      "com.badlogic.gdx.maps.MapLayer#objects" -> Target.Val,
      "com.badlogic.gdx.maps.MapLayer#properties" -> Target.Val,
      "com.badlogic.gdx.maps.MapObject#properties" -> Target.Val,
      "com.badlogic.gdx.math.collision.OrientedBoundingBox#bounds" -> Target.Val,
      "com.badlogic.gdx.math.collision.OrientedBoundingBox#vertices" -> Target.Val,
      // -- and the get-only ones the engine REFUSES under `MutableStorage`, declared ANYWAY:
      //    the port's answer for the whole collapsible population then lives in one place,
      //    and the lane says WHY each of these is still a `def` pair rather than saying
      //    nothing about it. `idiom(refused)` is a DENOMINATOR and not a work list, which is
      //    what makes a permanent refusal belong in it.
      "com.badlogic.gdx.graphics.Cubemap#cubemapData" -> Target.Val,
      "com.badlogic.gdx.graphics.Texture#textureData" -> Target.Val,
      "com.badlogic.gdx.graphics.g2d.SpriteCache#customShader" -> Target.Val,
      "com.badlogic.gdx.graphics.profiling.GLProfiler#enabled" -> Target.Val,
      "com.badlogic.gdx.maps.objects.CircleMapObject#circle" -> Target.Val,
      "com.badlogic.gdx.maps.objects.EllipseMapObject#ellipse" -> Target.Val,
      "com.badlogic.gdx.maps.objects.PointMapObject#point" -> Target.Val,
      "com.badlogic.gdx.maps.objects.RectangleMapObject#rectangle" -> Target.Val,
      "com.badlogic.gdx.maps.objects.TextMapObject#rectangle" -> Target.Val,
      "com.badlogic.gdx.maps.tiled.TiledMapTileSet#properties" -> Target.Val,
      "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#frameTiles" -> Target.Val,
      "com.badlogic.gdx.math.Polygon#rotation" -> Target.Val,
      "com.badlogic.gdx.math.Polygon#vertices" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.ui.List#cullingArea" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#fadeScrollBars" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#overscrollDistance" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#variableSizeKnobs" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.ui.SelectBox#clickListener" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable#name" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressed" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressedButton" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressedPointer" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#touchDownX" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#touchDownY" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentDragActor" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentPayload" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentSource" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.DragListener#dragging" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable#align" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable#scale" -> Target.Val
    )
  }

  /** the harvested pairs. The key is the emitted property in the upstream namespace (the package rename runs last); the value names the accessors explicitly, because a hand port's names are not
    * always bean-derivable (`getDragActor` -> `currentDragActor`) and a never-fired report needs them as data.
    */
  def beanPropertyPairs: Map[String, String] = Map(
    // -- com.badlogic.gdx.audio --
    "com.badlogic.gdx.audio.AudioDevice#latency" -> "getLatency",
    "com.badlogic.gdx.audio.Music#playing" -> "isPlaying",
    "com.badlogic.gdx.audio.Music#looping" -> "isLooping/setLooping",
    "com.badlogic.gdx.audio.Music#volume" -> "getVolume/setVolume",
    "com.badlogic.gdx.audio.Music#position" -> "getPosition/setPosition",
    // -- com.badlogic.gdx.Graphics -- The four GL accessors are here for a MECHANICAL reason:
    // [[globalsToContext]]'s member map re-points `Gdx.gl20` at the path `graphics.gl20`, and a
    // path segment is an IDENTIFIER — without these four the five `gl*` statics would have
    // nowhere to go. (`Gdx.gl` aliases `gl20` upstream and maps to the same path, so four pairs
    // serve five statics.)
    "com.badlogic.gdx.Graphics#gl20" -> "getGL20/setGL20",
    "com.badlogic.gdx.Graphics#gl30" -> "getGL30/setGL30",
    "com.badlogic.gdx.Graphics#gl31" -> "getGL31/setGL31",
    "com.badlogic.gdx.Graphics#gl32" -> "getGL32/setGL32",
    // -- com.badlogic.gdx.graphics --
    "com.badlogic.gdx.graphics.Cubemap#cubemapData" -> "getCubemapData",
    // `isManaged` is declared ABSTRACT on `GLTexture` and the phase renames the whole override
    // COMPONENT, so the interface entry covers `Cubemap`, `Texture` and `TextureArray` — three
    // per-implementor entries said the same thing three times.
    "com.badlogic.gdx.graphics.GLTexture#managed" -> "isManaged",
    "com.badlogic.gdx.graphics.Texture#textureData" -> "getTextureData",
    "com.badlogic.gdx.graphics.VertexAttribute#key" -> "getKey",
    "com.badlogic.gdx.graphics.VertexAttributes#mask" -> "getMask",
    "com.badlogic.gdx.graphics.VertexAttributes#maskWithSizePacked" -> "getMaskWithSizePacked",
    // -- com.badlogic.gdx.graphics.g2d --
    "com.badlogic.gdx.graphics.g2d.SpriteCache#customShader" -> "getCustomShader",
    // -- com.badlogic.gdx.graphics.profiling --
    "com.badlogic.gdx.graphics.profiling.GLProfiler#listener" -> "getListener/setListener",
    "com.badlogic.gdx.graphics.profiling.GLProfiler#enabled" -> "isEnabled",
    // -- com.badlogic.gdx.maps --
    "com.badlogic.gdx.maps.Map#layers" -> "getLayers",
    "com.badlogic.gdx.maps.Map#properties" -> "getProperties",
    "com.badlogic.gdx.maps.MapLayer#name" -> "getName/setName",
    "com.badlogic.gdx.maps.MapLayer#visible" -> "isVisible/setVisible",
    "com.badlogic.gdx.maps.MapLayer#objects" -> "getObjects",
    "com.badlogic.gdx.maps.MapLayer#properties" -> "getProperties",
    "com.badlogic.gdx.maps.MapLayer#parallaxX" -> "getParallaxX/setParallaxX",
    "com.badlogic.gdx.maps.MapLayer#parallaxY" -> "getParallaxY/setParallaxY",
    "com.badlogic.gdx.maps.MapLayer#opacity" -> "getOpacity/setOpacity",
    "com.badlogic.gdx.maps.MapLayer#combinedTintColor" -> "getCombinedTintColor",
    "com.badlogic.gdx.maps.MapLayer#tintColor" -> "getTintColor/setTintColor",
    "com.badlogic.gdx.maps.MapLayer#offsetX" -> "getOffsetX/setOffsetX",
    "com.badlogic.gdx.maps.MapLayer#offsetY" -> "getOffsetY/setOffsetY",
    "com.badlogic.gdx.maps.MapLayer#renderOffsetX" -> "getRenderOffsetX",
    "com.badlogic.gdx.maps.MapLayer#renderOffsetY" -> "getRenderOffsetY",
    "com.badlogic.gdx.maps.MapLayer#parent" -> "getParent/setParent",
    "com.badlogic.gdx.maps.MapObject#name" -> "getName/setName",
    "com.badlogic.gdx.maps.MapObject#color" -> "getColor/setColor",
    "com.badlogic.gdx.maps.MapObject#opacity" -> "getOpacity/setOpacity",
    "com.badlogic.gdx.maps.MapObject#visible" -> "isVisible/setVisible",
    "com.badlogic.gdx.maps.MapObject#properties" -> "getProperties",
    // -- com.badlogic.gdx.maps.objects --
    "com.badlogic.gdx.maps.objects.CircleMapObject#circle" -> "getCircle",
    "com.badlogic.gdx.maps.objects.EllipseMapObject#ellipse" -> "getEllipse",
    "com.badlogic.gdx.maps.objects.PointMapObject#point" -> "getPoint",
    "com.badlogic.gdx.maps.objects.PolygonMapObject#polygon" -> "getPolygon/setPolygon",
    "com.badlogic.gdx.maps.objects.PolylineMapObject#polyline" -> "getPolyline/setPolyline",
    "com.badlogic.gdx.maps.objects.RectangleMapObject#rectangle" -> "getRectangle",
    "com.badlogic.gdx.maps.objects.TextMapObject#rectangle" -> "getRectangle",
    "com.badlogic.gdx.maps.objects.TextMapObject#rotation" -> "getRotation/setRotation",
    "com.badlogic.gdx.maps.objects.TextMapObject#text" -> "getText/setText",
    "com.badlogic.gdx.maps.objects.TextMapObject#pixelSize" -> "getPixelSize/setPixelSize",
    "com.badlogic.gdx.maps.objects.TextMapObject#fontFamily" -> "getFontFamily/setFontFamily",
    "com.badlogic.gdx.maps.objects.TextMapObject#bold" -> "isBold/setBold",
    "com.badlogic.gdx.maps.objects.TextMapObject#italic" -> "isItalic/setItalic",
    "com.badlogic.gdx.maps.objects.TextMapObject#underline" -> "isUnderline/setUnderline",
    "com.badlogic.gdx.maps.objects.TextMapObject#strikeout" -> "isStrikeout/setStrikeout",
    "com.badlogic.gdx.maps.objects.TextMapObject#kerning" -> "isKerning/setKerning",
    "com.badlogic.gdx.maps.objects.TextMapObject#wrap" -> "isWrap/setWrap",
    "com.badlogic.gdx.maps.objects.TextMapObject#horizontalAlign" -> "getHorizontalAlign/setHorizontalAlign",
    "com.badlogic.gdx.maps.objects.TextMapObject#verticalAlign" -> "getVerticalAlign/setVerticalAlign",
    "com.badlogic.gdx.maps.objects.TextureMapObject#x" -> "getX/setX",
    "com.badlogic.gdx.maps.objects.TextureMapObject#y" -> "getY/setY",
    "com.badlogic.gdx.maps.objects.TextureMapObject#originX" -> "getOriginX/setOriginX",
    "com.badlogic.gdx.maps.objects.TextureMapObject#originY" -> "getOriginY/setOriginY",
    "com.badlogic.gdx.maps.objects.TextureMapObject#scaleX" -> "getScaleX/setScaleX",
    "com.badlogic.gdx.maps.objects.TextureMapObject#scaleY" -> "getScaleY/setScaleY",
    "com.badlogic.gdx.maps.objects.TextureMapObject#rotation" -> "getRotation/setRotation",
    "com.badlogic.gdx.maps.objects.TextureMapObject#textureRegion" -> "getTextureRegion/setTextureRegion",
    // -- com.badlogic.gdx.maps.tiled --
    "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#region" -> "getTextureRegion/setTextureRegion",
    "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#x" -> "getX/setX",
    "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#y" -> "getY/setY",
    "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#repeatX" -> "isRepeatX/setRepeatX",
    "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#repeatY" -> "isRepeatY/setRepeatY",
    "com.badlogic.gdx.maps.tiled.TiledMapTileSet#name" -> "getName/setName",
    "com.badlogic.gdx.maps.tiled.TiledMapTileSet#properties" -> "getProperties",
    // `TiledMapTile` DECLARES all seven, and the phase renames the whole override component, so
    // one entry each covers `AnimatedTiledMapTile` and `StaticTiledMapTile` — thirteen
    // per-implementor entries said the same thing twice over.
    "com.badlogic.gdx.maps.tiled.TiledMapTile#id" -> "getId/setId",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#blendMode" -> "getBlendMode/setBlendMode",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#textureRegion" -> "getTextureRegion/setTextureRegion",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#offsetX" -> "getOffsetX/setOffsetX",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#offsetY" -> "getOffsetY/setOffsetY",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#properties" -> "getProperties",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#objects" -> "getObjects",
    // -- com.badlogic.gdx.maps.tiled.objects --
    "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#flipHorizontally" -> "isFlipHorizontally/setFlipHorizontally",
    "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#flipVertically" -> "isFlipVertically/setFlipVertically",
    "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#tile" -> "getTile/setTile",
    // -- com.badlogic.gdx.maps.tiled.renderers --
    "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#staggerAxisX" -> "isStaggerAxisX/setStaggerAxisX",
    "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#staggerIndexEven" -> "isStaggerIndexEven/setStaggerIndexEven",
    "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#hexSideLength" -> "getHexSideLength/setHexSideLength",
    // -- com.badlogic.gdx.maps.tiled.tiles --
    "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#currentFrameIndex" -> "getCurrentFrameIndex",
    "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#currentFrame" -> "getCurrentFrame",
    "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#animationIntervals" -> "getAnimationIntervals/setAnimationIntervals",
    "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#frameTiles" -> "getFrameTiles",
    // -- com.badlogic.gdx.math --
    "com.badlogic.gdx.math.Polygon#vertices" -> "getVertices",
    "com.badlogic.gdx.math.Polygon#transformedVertices" -> "getTransformedVertices",
    "com.badlogic.gdx.math.Polygon#vertexCount" -> "getVertexCount",
    "com.badlogic.gdx.math.Polygon#boundingRectangle" -> "getBoundingRectangle",
    "com.badlogic.gdx.math.Polygon#rotation" -> "getRotation",
    // -- com.badlogic.gdx.math.collision --
    "com.badlogic.gdx.math.collision.OrientedBoundingBox#vertices" -> "getVertices",
    "com.badlogic.gdx.math.collision.OrientedBoundingBox#bounds" -> "getBounds",
    // -- com.badlogic.gdx.scenes.scene2d.ui --
    "com.badlogic.gdx.scenes.scene2d.ui.List#cullingArea" -> "getCullingArea",
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#scrollX" -> "getScrollX",
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#scrollY" -> "getScrollY",
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#overscrollDistance" -> "getOverscrollDistance",
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#fadeScrollBars" -> "getFadeScrollBars",
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#variableSizeKnobs" -> "getVariableSizeKnobs",
    "com.badlogic.gdx.scenes.scene2d.ui.SelectBox#clickListener" -> "getClickListener",
    // -- com.badlogic.gdx.scenes.scene2d.utils --
    "com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable#name" -> "getName",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#tapSquareSize" -> "getTapSquareSize/setTapSquareSize",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#tapCount" -> "getTapCount/setTapCount",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#button" -> "getButton/setButton",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressedButton" -> "getPressedButton",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressedPointer" -> "getPressedPointer",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressed" -> "isPressed",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#over" -> "isOver",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#touchDownX" -> "getTouchDownX",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#touchDownY" -> "getTouchDownY",
    "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentDragActor" -> "getDragActor",
    "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentPayload" -> "getDragPayload",
    "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentSource" -> "getDragSource",
    "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#dragTime" -> "getDragTime/setDragTime",
    "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#dragging" -> "isDragging",
    "com.badlogic.gdx.scenes.scene2d.utils.DragListener#tapSquareSize" -> "getTapSquareSize/setTapSquareSize",
    "com.badlogic.gdx.scenes.scene2d.utils.DragListener#button" -> "getButton/setButton",
    "com.badlogic.gdx.scenes.scene2d.utils.DragListener#dragDistance" -> "getDragDistance",
    "com.badlogic.gdx.scenes.scene2d.utils.DragListener#dragging" -> "isDragging",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#leftWidth" -> "getLeftWidth/setLeftWidth",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#rightWidth" -> "getRightWidth/setRightWidth",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#topHeight" -> "getTopHeight/setTopHeight",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#bottomHeight" -> "getBottomHeight/setBottomHeight",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#minWidth" -> "getMinWidth/setMinWidth",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#minHeight" -> "getMinHeight/setMinHeight",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#lastSelected" -> "getLastSelected",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#toggle" -> "getToggle/setToggle",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#multiple" -> "getMultiple/setMultiple",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#required" -> "getRequired/setRequired",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#disabled" -> "isDisabled/setDisabled",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#programmaticChangeEvents" -> "getProgrammaticChangeEvents/setProgrammaticChangeEvents",
    "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable#scale" -> "getScale",
    "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable#align" -> "getAlign"
  )

  /** RETARGET COERCIONS — boundary wraps inserted by `coerce` when a retarget target meets a slot whose head type is a different family.
    *
    * Keyed by (actual head FQN, expected head FQN), both in the TARGET namespace (post-retarget). `$0` in the template is the actual value, inserted as an AST hole.
    */
  def libRetargetCoercions: Map[(String, String), String] = Map(
    // DynamicArray -> JavaIterable: wrap with fromIterator so the shim delegates to DA's iterator.
    ("lowlevel.util.DynamicArray", "balticporter.runtime.JavaIterable") ->
      "balticporter.runtime.JavaIterable.fromIterator(() => $0.iterator)",
    // DynamicArray -> JavaIterator: wrap DA's iterator into a JavaIterator.
    // MapProperties.keys/values return a DynamicArray where the method declares JavaIterator.
    ("lowlevel.util.DynamicArray", "balticporter.runtime.JavaIterator") ->
      "balticporter.runtime.JavaIterator.from($0.iterator)",
    // scala.collection.Iterator -> JavaIterator: wrap with JavaIterator.from.
    // Retarget target's .iterator returns scala.collection.Iterator; the method declares JavaIterator.
    ("scala.collection.Iterator", "balticporter.runtime.JavaIterator") ->
      "balticporter.runtime.JavaIterator.from($0)",
    // DynamicArray[Char] -> CharSequence: build a String from the backing char array.
    ("lowlevel.util.DynamicArray", "java.lang.CharSequence") ->
      "new java.lang.String($0.toArray)",
    // DynamicArray -> scala.Array: extract a sized copy via toArray.
    ("lowlevel.util.DynamicArray", "scala.Array") ->
      "$0.toArray",
    // scala.Array -> DynamicArray: wrap the array (zero-copy) into a DynamicArray.
    ("scala.Array", "lowlevel.util.DynamicArray") ->
      "lowlevel.util.DynamicArray.wrap($0)"
  )

  /** Indexed field rewrites for retarget sources where a field and a method of the same name need different rewrite kinds (ArrayMap `keys`/`values`).
    */
  def libRetargetIndexedFields: Map[String, Map[String, CollectionsTransform.RetargetRewrite.IndexedField]] = {
    import balticporter.transform.CollectionsTransform.RetargetRewrite.IndexedField
    Map(
      "com.badlogic.gdx.utils.ArrayMap" -> Map(
        "keys" -> IndexedField("keys", via = "getKeyAt", viaWrite = "setKeyAt"),
        "values" -> IndexedField("values", via = "getValueAt", viaWrite = "setValueAt")
      )
    )
  }

  /** the `gdx/src` pipeline. Universal phases first, then the three parameterised phases configured above, then the one library-specific rule libGDX plugs in from outside the engine
    * ([[GdxSharedIteratorRule]]).
    */
  def mainPhases: List[balticporter.tir.Phase] =
    List(
      new CollectionsTransform(
        retarget = comparatorRetarget ++ bitsRetarget ++ libCollectionRetargets,
        retargetRewrites = bitsRetargetRewrites ++ libCollectionConstructRewrites,
        retargetRewritesByDesc = libCollectionConstructRewritesByDesc,
        retargetTypeArgs = libCollectionRetargetTypeArgs,
        retargetCoercions = libRetargetCoercions,
        retargetIndexedFields = libRetargetIndexedFields
      ),
      new MutableParamsTransform,
      new balticporter.transform.CallSiteSubstitutionTransform(LibgdxPolicy.ClasspathProbeCalls),
      new balticporter.transform.ThreadConfinedStaticsTransform(LibgdxPolicy.ThreadConfinedScratch),
      new PanamaFfiTransform(),
      unwrapReflection,
      classTable,
      new GdxSharedIteratorRule,
      memberRenames,
      disposableRedirect,
      textureHandle,
      align,
      uniformLocation,
      nullability,
      // screenWidth body substituted BEFORE globals->implicits so the phase sees no Gdx.graphics read
      new balticporter.transform.MethodBodyTransform(
        Map(
          "com.badlogic.gdx.graphics.g3d.particles.ParticleShader$Setters#screenWidth" ->
            "new sge.graphics.g3d.shaders.BaseShader.GlobalSetter() { override def set(shader: sge.graphics.g3d.shaders.BaseShader, inputID: scala.Int, renderable: sge.graphics.g3d.Renderable, combinedAttributes: sge.graphics.g3d.Attributes): scala.Unit = shader.set(inputID, shader.sgeContext.graphics.width.asInstanceOf[scala.Float]) }"
        )
      ),
      globalsToContext,
      beanProperties,
      nullaryArity,
      new balticporter.transform.AddMembersTransform(
        Map(
          "com.badlogic.gdx.graphics.g3d.shaders.BaseShader" -> List(
            balticporter.transform.AddMembersTransform.MemberSpec(
              "sgeContext",
              0,
              "val sgeContext: sge.Sge = scala.Predef.summon[sge.Sge]",
              balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.shaders.BaseShader#sgeContext"),
              Some("sge's sgeContext: Sge exposed to setters (replaces Gdx.graphics static)"),
              false
            )
          )
        )
      ),
      new balticporter.transform.MethodBodyTransform(
        Map(
          // SelectBox.selectedIndex — OrderedSet vs ObjectSet (broken subtyping edge).
          // sge: val sel = selection.items (inferred OrderedSet). Fix: widen the type annotation
          // from ObjectSet to OrderedSet. collection-internal seam — java's OrderedSet <: ObjectSet
          // has no image in lls.
          "com.badlogic.gdx.scenes.scene2d.ui.SelectBox#getSelectedIndex" ->
            """{
              |  val selected: lowlevel.util.OrderedSet[T] = this.selection$field.items()
              |  return if (selected.size == 0) -1 else this.items$field.indexOf(selected.first)
              |}""".stripMargin,
          // wave 3.1m: SgeList.selectedIndex — same OrderedSet vs ObjectSet pattern.
          "com.badlogic.gdx.scenes.scene2d.ui.List#getSelectedIndex" ->
            """{
              |  val selected: lowlevel.util.OrderedSet[T] = this.selection$field.items()
              |  return if (selected.size == 0) -1 else this.items$field.indexOf(selected.first)
              |}""".stripMargin,
          // --- Nullable unwrapping at external lls method returns (ObjectMap.put/remove/findKey, DynamicArray.random, OrderedSet.first) ---
          "com.badlogic.gdx.graphics.Colors#put" -> "{ return Colors.map.put(name, color).orNull }",
          "com.badlogic.gdx.graphics.g3d.decals.PluggableGroupStrategy#unPlug" -> "{ return this.plugs.remove(group).orNull }",
          "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#removeSource" -> "{ val dragListener: sge.scenes.scene2d.utils.DragListener = this.sourceListeners.remove(source).get; source.actor$field.removeCaptureListener(dragListener) }",
          "com.badlogic.gdx.scenes.scene2d.ui.Skin#find" -> "{ if (resource == null) { throw new java.lang.IllegalArgumentException(\"style cannot be null.\") }; val typeResources = this.resources.get(resource.getClass()).orNull; if (typeResources == null) { return lowlevel.Nullable.empty }; return typeResources.findKey(resource, true) }",
          "com.badlogic.gdx.utils.PoolManager#addPool(Class,Pool)" -> "{ val oldPool = this.typePools.put(poolClass, pool).orNull; if (oldPool != null) { throw new sge.utils.GdxRuntimeException(\"Attempt to add pool with already existing class: \" + poolClass + \", use PoolManager#set instead\") } }",
          "com.badlogic.gdx.utils.PoolManager#addPool(Class,DefaultPool$PoolSupplier)" -> "{ val p = new sge.utils.DefaultPool[T](poolSupplier); val oldPool = this.typePools.put(poolClass, p).orNull; if (oldPool != null) { throw new sge.utils.GdxRuntimeException(\"Attempt to add pool with already existing class: \" + poolClass) } }",
          // DynamicArray.random() returns Nullable[T] in lls; these sites use the value as T
          "com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer$Random$ModelInstancePool#newObject" ->
            "{ return new sge.graphics.g3d.ModelInstance(models.random().get) }",
          "com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer$Random$ParticleControllerPool#newObject" ->
            "{ val controller = templates.random().get.copy(); controller.init(); return controller }",
          "com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer$Random#activateParticles" ->
            """{
              |  var i = startIndex * this.regionChannel.strideSize; val c = i + (count * this.regionChannel.strideSize)
              |  while (i < c) {
              |    val region = regions.random().get
              |    this.regionChannel.data$shadow(i + sge.graphics.g3d.particles.ParticleChannels.UOffset) = region.u
              |    this.regionChannel.data$shadow(i + sge.graphics.g3d.particles.ParticleChannels.VOffset) = region.v
              |    this.regionChannel.data$shadow(i + sge.graphics.g3d.particles.ParticleChannels.U2Offset) = region.u2
              |    this.regionChannel.data$shadow(i + sge.graphics.g3d.particles.ParticleChannels.V2Offset) = region.v2
              |    this.regionChannel.data$shadow(i + sge.graphics.g3d.particles.ParticleChannels.HalfWidthOffset) = 0.5f
              |    this.regionChannel.data$shadow(i + sge.graphics.g3d.particles.ParticleChannels.HalfHeightOffset) = region.halfInvAspectRatio
              |    i = i + this.regionChannel.strideSize
              |  }
              |}""".stripMargin,
          "com.badlogic.gdx.graphics.g2d.ParticleEmitter#setSprites" ->
            """{
              |  this.sprites$field = sprites
              |  if (sprites.size == 0) { return } else ()
              |  scala.util.boundary { { var i: scala.Int = 0; val n: scala.Int = this.particles$field.length; while (i < n) { {
              |    val particle: sge.graphics.g2d.ParticleEmitter.Particle = this.particles$field(i)
              |    if (particle == null) { scala.util.boundary.break(()) } else ()
              |    var sprite: sge.graphics.g2d.Sprite = null
              |    this.spriteMode$field match {
              |      case null => throw new java.lang.NullPointerException("switch selector was null")
              |      case sge.graphics.g2d.ParticleEmitter.SpriteMode.single =>
              |        sprite = { val bp$tpl26 = sprites; { if (bp$tpl26.isEmpty) throw new java.lang.IllegalStateException("Array is empty."); bp$tpl26.first } }
              |      case sge.graphics.g2d.ParticleEmitter.SpriteMode.random =>
              |        sprite = sprites.random().get
              |      case sge.graphics.g2d.ParticleEmitter.SpriteMode.animated =>
              |        val percent: scala.Float = 1 - (particle.currentLife / particle.life.asInstanceOf[scala.Float])
              |        particle.frame = java.lang.Math.min((percent * sprites.size).asInstanceOf[scala.Int], sprites.size - 1)
              |        sprite = sprites.apply(particle.frame)
              |      case _ => ()
              |    }
              |    particle.setRegion(sprite)
              |    particle.setOrigin(sprite.originX, sprite.originY)
              |  }; i = i + 1 } } }
              |}""".stripMargin,
          "com.badlogic.gdx.graphics.g2d.ParticleEmitter#activateParticle" ->
            """{
              |  var sprite: sge.graphics.g2d.Sprite = null
              |  this.spriteMode$field match {
              |    case null => throw new java.lang.NullPointerException("switch selector was null")
              |    case sge.graphics.g2d.ParticleEmitter.SpriteMode.single | sge.graphics.g2d.ParticleEmitter.SpriteMode.animated => {
              |      sprite = { val bp$tpl25 = this.sprites$field; { if (bp$tpl25.isEmpty) throw new java.lang.IllegalStateException("Array is empty."); bp$tpl25.first } }
              |    }
              |    case sge.graphics.g2d.ParticleEmitter.SpriteMode.random => {
              |      sprite = this.sprites$field.random().get
              |    }
              |    case _ => ()
              |  }
              |  var particle: sge.graphics.g2d.ParticleEmitter.Particle = this.particles$field(index)
              |  if (particle == null) {
              |    this.particles$field(index) = { particle = this.newParticle(sprite); particle }
              |    particle.flip(this.flipX, this.flipY$field)
              |  } else { particle.set(sprite) }
              |  val percent: scala.Float = this.durationTimer / this.duration$field.asInstanceOf[scala.Float]
              |  val updateFlags: scala.Int = this.updateFlags
              |  if (this.lifeValue.independent$field) { this.generateLifeValues() } else ()
              |  if (this.lifeOffsetValue.independent$field) { this.generateLifeOffsetValues() } else ()
              |  particle.currentLife = { particle.life = this.life$field + (this.lifeDiff * this.lifeValue.getScale(percent)).asInstanceOf[scala.Int]; particle.life }
              |  if (this.velocityValue.active$field) {
              |    particle.velocity = this.velocityValue.newLowValue()
              |    particle.velocityDiff = this.velocityValue.newHighValue()
              |    if (!this.velocityValue.relative$field) { particle.velocityDiff = particle.velocityDiff - particle.velocity } else ()
              |  } else ()
              |  particle.angle = this.angleValue.newLowValue()
              |  particle.angleDiff = this.angleValue.newHighValue()
              |  if (!this.angleValue.relative$field) { particle.angleDiff = particle.angleDiff - particle.angle } else ()
              |  var angle: scala.Float = 0.toFloat
              |  if ((updateFlags & ParticleEmitter.UPDATE_ANGLE) == 0) {
              |    angle = particle.angle + (particle.angleDiff * this.angleValue.getScale(0.toFloat))
              |    particle.angle = angle
              |    particle.angleCos = sge.math.MathUtils.cosDeg(angle)
              |    particle.angleSin = sge.math.MathUtils.sinDeg(angle)
              |  } else ()
              |  val spriteWidth: scala.Float = sprite.width
              |  val spriteHeight: scala.Float = sprite.height
              |  particle.xScale = this.xScaleValue.newLowValue() / spriteWidth
              |  particle.xScaleDiff = this.xScaleValue.newHighValue() / spriteWidth
              |  if (!this.xScaleValue.relative$field) { particle.xScaleDiff = particle.xScaleDiff - particle.xScale } else ()
              |  if (this.yScaleValue.active$field) {
              |    particle.yScale = this.yScaleValue.newLowValue() / spriteHeight
              |    particle.yScaleDiff = this.yScaleValue.newHighValue() / spriteHeight
              |    if (!this.yScaleValue.relative$field) { particle.yScaleDiff = particle.yScaleDiff - particle.yScale } else ()
              |    particle.setScale(particle.xScale + (particle.xScaleDiff * this.xScaleValue.getScale(0.toFloat)), particle.yScale + (particle.yScaleDiff * this.yScaleValue.getScale(0.toFloat)))
              |  } else { particle.setScale(particle.xScale + (particle.xScaleDiff * this.xScaleValue.getScale(0.toFloat))) }
              |  if (this.rotationValue.active$field) {
              |    particle.rotation$shadow = this.rotationValue.newLowValue()
              |    particle.rotationDiff = this.rotationValue.newHighValue()
              |    if (!this.rotationValue.relative$field) { particle.rotationDiff = particle.rotationDiff - particle.rotation$shadow } else ()
              |    var rotation: scala.Float = particle.rotation$shadow + (particle.rotationDiff * this.rotationValue.getScale(0.toFloat))
              |    if (this.aligned$field) { rotation = rotation + angle } else ()
              |    particle.setRotation(rotation)
              |  } else ()
              |  if (this.windValue.active$field) {
              |    particle.wind = this.windValue.newLowValue()
              |    particle.windDiff = this.windValue.newHighValue()
              |    if (!this.windValue.relative$field) { particle.windDiff = particle.windDiff - particle.wind } else ()
              |  } else ()
              |  if (this.gravityValue.active$field) {
              |    particle.gravity = this.gravityValue.newLowValue()
              |    particle.gravityDiff = this.gravityValue.newHighValue()
              |    if (!this.gravityValue.relative$field) { particle.gravityDiff = particle.gravityDiff - particle.gravity } else ()
              |  } else ()
              |  var color: scala.Array[scala.Float] = particle.tint
              |  if (color == null) { particle.tint = { color = new scala.Array[scala.Float](3); color } } else ()
              |  val temp: scala.Array[scala.Float] = this.tintValue.getColor(0.toFloat)
              |  color(0) = temp(0); color(1) = temp(1); color(2) = temp(2)
              |  particle.transparency = this.transparencyValue.newLowValue()
              |  particle.transparencyDiff = this.transparencyValue.newHighValue() - particle.transparency
              |  var x: scala.Float = this.x$field
              |  if (this.xOffsetValue.active$field) { x = x + this.xOffsetValue.newLowValue() } else ()
              |  var y: scala.Float = this.y$field
              |  if (this.yOffsetValue.active$field) { y = y + this.yOffsetValue.newLowValue() } else ()
              |  this.spawnShapeValue.shape$field match {
              |    case null => throw new java.lang.NullPointerException("switch selector was null")
              |    case sge.graphics.g2d.ParticleEmitter.SpawnShape.square => {
              |      val width: scala.Float = this.spawnWidth$field + (this.spawnWidthDiff * this.spawnWidthValue.getScale(percent))
              |      val height: scala.Float = this.spawnHeight$field + (this.spawnHeightDiff * this.spawnHeightValue.getScale(percent))
              |      x = x + (sge.math.MathUtils.random(width) - (width * 0.5f))
              |      y = y + (sge.math.MathUtils.random(height) - (height * 0.5f))
              |    }
              |    case sge.graphics.g2d.ParticleEmitter.SpawnShape.ellipse => scala.util.boundary { (case$39: scala.util.boundary.Label[scala.Unit]) ?=> {
              |      val width: scala.Float = this.spawnWidth$field + (this.spawnWidthDiff * this.spawnWidthValue.getScale(percent))
              |      val height: scala.Float = this.spawnHeight$field + (this.spawnHeightDiff * this.spawnHeightValue.getScale(percent))
              |      val radiusX: scala.Float = width * 0.5f
              |      val radiusY: scala.Float = height * 0.5f
              |      if ((radiusX == 0) || (radiusY == 0)) { scala.util.boundary.break(())(using case$39) } else ()
              |      val scaleY: scala.Float = radiusX / radiusY.asInstanceOf[scala.Float]
              |      if (this.spawnShapeValue.edges$field) {
              |        var spawnAngle: scala.Float = 0.0f
              |        this.spawnShapeValue.side$field match {
              |          case null => throw new java.lang.NullPointerException("switch selector was null")
              |          case sge.graphics.g2d.ParticleEmitter.SpawnEllipseSide.top => { spawnAngle = -sge.math.MathUtils.random(179.0f) }
              |          case sge.graphics.g2d.ParticleEmitter.SpawnEllipseSide.bottom => { spawnAngle = sge.math.MathUtils.random(179.0f) }
              |          case _ => { spawnAngle = sge.math.MathUtils.random(360.0f) }
              |        }
              |        val cosDeg: scala.Float = sge.math.MathUtils.cosDeg(spawnAngle)
              |        val sinDeg: scala.Float = sge.math.MathUtils.sinDeg(spawnAngle)
              |        x = x + (cosDeg * radiusX)
              |        y = y + ((sinDeg * radiusX) / scaleY)
              |        if ((updateFlags & ParticleEmitter.UPDATE_ANGLE) == 0) {
              |          particle.angle = spawnAngle; particle.angleCos = cosDeg; particle.angleSin = sinDeg
              |        } else ()
              |      } else {
              |        val radius2: scala.Float = radiusX * radiusX
              |        scala.util.boundary { while (true) {
              |          val px: scala.Float = sge.math.MathUtils.random(width) - radiusX
              |          val py: scala.Float = sge.math.MathUtils.random(width) - radiusX
              |          if (((px * px) + (py * py)) <= radius2) { x = x + px; y = y + (py / scaleY); scala.util.boundary.break(()) } else ()
              |        } }
              |      }
              |    } }
              |    case sge.graphics.g2d.ParticleEmitter.SpawnShape.line => {
              |      val width: scala.Float = this.spawnWidth$field + (this.spawnWidthDiff * this.spawnWidthValue.getScale(percent))
              |      val height: scala.Float = this.spawnHeight$field + (this.spawnHeightDiff * this.spawnHeightValue.getScale(percent))
              |      if (width != 0) {
              |        val lineX: scala.Float = width * sge.math.MathUtils.random()
              |        x = x + lineX; y = y + (lineX * (height / width.asInstanceOf[scala.Float]))
              |      } else { y = y + (height * sge.math.MathUtils.random()) }
              |    }
              |    case _ => ()
              |  }
              |  particle.setBounds(x - (spriteWidth * 0.5f), y - (spriteHeight * 0.5f), spriteWidth, spriteHeight)
              |  var offsetTime: scala.Int = (this.lifeOffset$field + (this.lifeOffsetDiff * this.lifeOffsetValue.getScale(percent))).asInstanceOf[scala.Int]
              |  if (offsetTime > 0) {
              |    if (offsetTime >= particle.currentLife) { offsetTime = particle.currentLife - 1 } else ()
              |    this.updateParticle(particle, offsetTime / 1000.0f, offsetTime)
              |  } else ()
              |}""".stripMargin,
          // --- Screen default empty bodies (sge convention: only `render` is abstract) ---
          "com.badlogic.gdx.Screen#show" -> "{}",
          "com.badlogic.gdx.Screen#resize" -> "{}",
          "com.badlogic.gdx.Screen#pause" -> "{}",
          "com.badlogic.gdx.Screen#resume" -> "{}",
          "com.badlogic.gdx.Screen#hide" -> "{}",
          // --- getDependencies: return empty DynamicArray instead of null (sge convention) ---
          "com.badlogic.gdx.assets.loaders.ShaderProgramLoader#getDependencies" -> "lowlevel.util.DynamicArray.apply[sge.assets.AssetDescriptor[?]]()",
          "com.badlogic.gdx.assets.loaders.I18NBundleLoader#getDependencies" -> "lowlevel.util.DynamicArray.apply[sge.assets.AssetDescriptor[?]]()",
          "com.badlogic.gdx.assets.loaders.CubemapLoader#getDependencies" -> "lowlevel.util.DynamicArray.apply[sge.assets.AssetDescriptor[?]]()",
          "com.badlogic.gdx.assets.loaders.PixmapLoader#getDependencies" -> "lowlevel.util.DynamicArray.apply[sge.assets.AssetDescriptor[?]]()",
          "com.badlogic.gdx.assets.loaders.SoundLoader#getDependencies" -> "lowlevel.util.DynamicArray.apply[sge.assets.AssetDescriptor[?]]()",
          "com.badlogic.gdx.assets.loaders.MusicLoader#getDependencies" -> "lowlevel.util.DynamicArray.apply[sge.assets.AssetDescriptor[?]]()",
          "com.badlogic.gdx.assets.loaders.TextureLoader#getDependencies" -> "lowlevel.util.DynamicArray.apply[sge.assets.AssetDescriptor[?]]()",
          "com.badlogic.gdx.assets.loaders.ParticleEffectLoader#getDependencies" -> "{ val deps = lowlevel.util.DynamicArray.apply[sge.assets.AssetDescriptor[?]](); if ((param != null) && (param.atlasFile != null) && (!param.atlasFile.isEmpty)) { deps.add(new sge.assets.AssetDescriptor[sge.graphics.g2d.TextureAtlas](param.atlasFile, classOf[sge.graphics.g2d.TextureAtlas]).asInstanceOf[sge.assets.AssetDescriptor[?]]) }; deps }",
          // --- ParticleEffectLoader#save: use no-arg ResourceData ctor (ctor-funnel bug in 1-arg) ---
          "com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader#save" ->
            """{
              |  val data = new sge.graphics.g3d.particles.ResourceData[sge.graphics.g3d.particles.ParticleEffect]()
              |  data.resource = effect
              |  effect.save(parameter.manager, data.asInstanceOf[sge.graphics.g3d.particles.ResourceData[java.lang.Object]])
              |  if (parameter.batches != null) {
              |    for (batch <- parameter.batches) {
              |      var save: scala.Boolean = false
              |      scala.util.boundary { for (controller <- effect.controllers) {
              |        if (controller.renderer.isCompatible(batch)) { save = true; scala.util.boundary.break(()) } else ()
              |      } }
              |      if (save) {
              |        batch.asInstanceOf[sge.graphics.g3d.particles.batches.ParticleBatch[sge.graphics.g3d.particles.renderers.ParticleControllerRenderData]].save(parameter.manager, data.asInstanceOf[sge.graphics.g3d.particles.ResourceData[sge.graphics.g3d.particles.renderers.ParticleControllerRenderData]].asInstanceOf[sge.graphics.g3d.particles.ResourceData[java.lang.Object]])
              |      } else ()
              |    }
              |  } else ()
              |  val json = new sge.utils.Json(parameter.jsonOutputType)
              |  if (parameter.prettyPrint) { val prettyJson = json.prettyPrint(data); parameter.file.writeString(prettyJson, false) }
              |  else { json.toJson(data, parameter.file) }
              |}""".stripMargin
        )
      ),
      // --- 3.2g: Pool class-to-trait (ecs drop-in parity) ---
      // sge hand-ported Pool as a TRAIT with abstract vals (justified, kind=api; AD-003).
      // Pool is now DROPPED and INJECTED as sge's trait (libgdx-overrides/sge/utils/Pool.scala).
      // This phase rewrites every subclass: super(args) -> override vals, and for nilary
      // constructors the defaults from Pool()'s delegation chain (16, Integer.MAX_VALUE).
      new balticporter.transform.ClassToTraitTransform(
        specs = Map(
          "com.badlogic.gdx.utils.Pool" -> List(
            balticporter.transform.ClassToTraitTransform.ParamMapping(0, "initialCapacity"),
            balticporter.transform.ClassToTraitTransform.ParamMapping(1, "max")
          )
        )
      )
      // SuppressionPhase is now derived unconditionally by PortRun (universal, no-op
      // when no `.orNull` symbols exist) — removed from surface, no port needs to declare it.
    )

  /** Drop `()` from nullary getter-like methods — sge's empirical convention, no written rule in `conversion-rules.md`. Enabled with `Everywhere()` since the convention is whole-library: the sge hand
    * port strips parens from EVERY getter-like method, not a named list. AFTER `bean-properties`, which has already claimed its own getters. The `runsAfter` edge is on the phase; the list position is
    * the pipeline's contract.
    */
  def nullaryArity: balticporter.transform.NullaryArityTransform =
    // Drop `()` from every getter-like nullary method in scope. Dependents follow the base's
    // published shape (form=parenless in the port map) through PortMapTransform.followMemberRenames.
    // Measured with the bean switch: same six-port table (wave 1.2h).
    new balticporter.transform.NullaryArityTransform(scope = balticporter.tir.RuleScope.Everywhere())

  /** EMPTY, and the POSITION is the policy -- libGDX renames none of its own members. `disposableRedirect` renames `dispose -> close` on `Disposable`'s whole component; a dependent implementor with
    * its own `close()` must move IT out of the way BEFORE the redirect, which only a MERGE can arrange early in an unowned pipeline (`SurfaceFold` places a merged phase at the BASE's position). No
    * entries here = structural no-op.
    */
  def memberRenames: balticporter.transform.MemberRenameTransform =
    new balticporter.transform.MemberRenameTransform(
      // ---- wave 1.3: member renames from the `Migration notes: Renames:` census ----
      // Each entry is traceable to sge's documented Renames: line on the type's source file.
      // The key is the upstream FQN#member in the upstream namespace.
      renames = Map(
        // `type` is a Scala reserved word; sge renamed the field to `eventType` (1 sge file).
        // The `getType`/`setType` bean pair is handled separately by `beanProperties`.
        "com.badlogic.gdx.scenes.scene2d.InputEvent#type" -> "eventType",
        // toString(T) clashes with Any.toString(); REFUSED (policy 3->4) since the override
        // component reaches java.lang.Object#toString, which the program cannot move.
        "com.badlogic.gdx.scenes.scene2d.ui.List#toString(T)" -> "itemToString"
      )
    )

  /** `com.badlogic.gdx.Gdx`'s `public static` fields retired into `sge.Sge`, threaded as a `using` parameter. `attach = "class"`: measured against method attachment, which freezes declarations
    * anchored on an external parent the program doesn't declare. `sites` marks the two construct-at-init sites (no caller for a clause). Shared surface, in [[core]]; last (after
    * [[disposableRedirect]] and [[beanProperties]]).
    */
  def globalsToContext: balticporter.transform.GlobalsToImplicitsTransform =
    new balticporter.transform.GlobalsToImplicitsTransform(
      holders = List(
        balticporter.transform.ContextHolder(
          holder = "com.badlogic.gdx.Gdx",
          context = balticporter.transform.ContextType.Injected("sge.Sge"),
          members = Map(
            "app" -> "application",
            "graphics" -> "graphics",
            "audio" -> "audio",
            "input" -> "input",
            "files" -> "files",
            "net" -> "net",
            // the five GL statics, two-hop through the service that really owns them
            "gl" -> "graphics.gl20",
            "gl20" -> "graphics.gl20",
            "gl30" -> "graphics.gl30",
            "gl31" -> "graphics.gl31",
            "gl32" -> "graphics.gl32"
          ),
          attach = balticporter.transform.ContextAttach.Class,
          reader = balticporter.transform.ContextReader.Summon,
          boundary = balticporter.transform.ContextBoundary.Refuse,
          forceThread = Set("com.badlogic.gdx.graphics.g3d.shaders.BaseShader"),
          sites = Map(
            // `static final OnscreenKeyboard DEFAULT_ONSCREEN_KEYBOARD = new DefaultOnscreenKeyboard()`
            "com.badlogic.gdx.scenes.scene2d.ui.TextField#DEFAULT_ONSCREEN_KEYBOARD" ->
              balticporter.transform.ContextSite.LazyInit,
            // `static Pool<Cell> cellPool = new Pool<Cell>(){ protected Cell newObject(){ … } }`
            "com.badlogic.gdx.scenes.scene2d.ui.Table#cellPool" ->
              balticporter.transform.ContextSite.LazyInit
          )
        )
      ),
      // wave 3.1v: classes whose lls retarget constructions need MkArray[T] in scope;
      // BufferedParticleBatch is abstract, so the clause propagates to subclass constructors.
      requiredGivens = Map(
        "com.badlogic.gdx.math.Octree" -> "lowlevel.MkArray",
        "com.badlogic.gdx.math.BSpline" -> "lowlevel.MkArray",
        "com.badlogic.gdx.math.Bezier" -> "lowlevel.MkArray",
        "com.badlogic.gdx.utils.FlushablePool" -> "lowlevel.MkArray",
        "com.badlogic.gdx.graphics.g3d.particles.batches.BufferedParticleBatch" -> "lowlevel.MkArray",
        "com.badlogic.gdx.graphics.glutils.GLFrameBuffer" -> "lowlevel.MkArray"
      )
    )

  /** libGDX's GL texture handle — the `int` that is really a texture name — as an opaque type, matching the reference hand port's `GLHandle.scala`. Only `TextureHandle` is configured (a
    * library-specific rule); the four GL interfaces are fenced out of propagation, or a nullary `glGenTexture()` flow edge would retype the whole GL interface. Shared surface, lives in [[core]];
    * phase runs in dependents too, coercing against the object this module minted.
    */
  def textureHandle: balticporter.transform.PrimitiveToOpaqueTransform =
    new balticporter.transform.PrimitiveToOpaqueTransform(
      balticporter.tir.OpaqueSpec(
        fqn = "com.badlogic.gdx.graphics.TextureHandle",
        hints = Set("com.badlogic.gdx.graphics.GLTexture#glHandle"),
        underlying = balticporter.tir.OpaqueSpec.Primitive.Int,
        scope = balticporter.tir.RuleScope.Everywhere(
          except = Set(
            "com.badlogic.gdx.graphics.GL20",
            "com.badlogic.gdx.graphics.GL30",
            "com.badlogic.gdx.graphics.GL31",
            "com.badlogic.gdx.graphics.GL32"
          )
        )
      )
    )

  /** libGDX's `Align` — a class of `static final int` constants — as an opaque type against an existing/injected type: `Substitutions.dropTypes` drops the java class, `inject` supplies sge's own
    * `Align.scala`; the transform seeds from align-typed fields and propagates to getters/setters/parameters, coercing via `Align(rawInt)`/`Align.toInt(value)`. Shared surface, composed via
    * `MergeablePolicy`: a dependent unions its own `hints`.
    */
  def align: balticporter.transform.PrimitiveToOpaqueTransform =
    new balticporter.transform.PrimitiveToOpaqueTransform(LibgdxPolicy.AlignSpec)

  /** the one `Align` spec the full port and the ladder share (the ladder derives its seeds from the reference and drops the field hints). */
  val AlignSpec: balticporter.tir.OpaqueSpec =
    balticporter.tir.OpaqueSpec(
      fqn = "com.badlogic.gdx.utils.Align",
      target = balticporter.tir.OpaqueSpec.Target.Existing(
        typeFqn = "sge.utils.Align",
        wrapName = "apply",
        unwrapName = "toInt"
      ),
      hints = Set(
        // 13 fields typed `int align` / `int alignment` / `int columnAlign` / `int rowAlign` /
        // `int labelAlign` / `int lineAlign` across the scene2d UI types. Each is a seed; the
        // propagation discovers every getter, setter, and parameter reachable from them.
        "com.badlogic.gdx.scenes.scene2d.ui.Image#align",
        "com.badlogic.gdx.scenes.scene2d.ui.Label#labelAlign",
        "com.badlogic.gdx.scenes.scene2d.ui.Label#lineAlign",
        "com.badlogic.gdx.scenes.scene2d.ui.List#alignment",
        "com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup#align",
        "com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup#columnAlign",
        "com.badlogic.gdx.scenes.scene2d.ui.Table#align",
        "com.badlogic.gdx.scenes.scene2d.ui.SelectBox#alignment",
        "com.badlogic.gdx.scenes.scene2d.ui.Container#align",
        "com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup#align",
        "com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup#rowAlign",
        "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable#align",
        "com.badlogic.gdx.scenes.scene2d.actions.MoveToAction#alignment"
      ),
      underlying = balticporter.tir.OpaqueSpec.Primitive.Int,
      // also every slot the reference spells `Align` that no field seed reaches — `GlyphLayout.setText`'s
      // `halign`, `BitmapFont.draw`'s, which java types as plain `int` parameters (sge ISS-770)
      derive = true,
      // a `@Null Integer` slot the nullability step wrapped (`Cell#align`), which the reference spells `Nullable[Align]`
      carriers = Set("lowlevel.Nullable")
    )

  /** GL uniform locations — the `int` that is really a distinct domain value — as an opaque type following the Align pattern: no java class to drop, injected as `sge.graphics.UniformLocation` with
    * sge's own comparison extensions. Seeds: `fetchUniformLocation`'s return and `BaseShader#locations` (an `int[]` array seed). Same GL-interface fence as [[textureHandle]].
    */
  def uniformLocation: balticporter.transform.PrimitiveToOpaqueTransform =
    new balticporter.transform.PrimitiveToOpaqueTransform(
      balticporter.tir.OpaqueSpec(
        fqn = "com.badlogic.gdx.graphics.UniformLocation",
        target = balticporter.tir.OpaqueSpec.Target.Existing(
          typeFqn = "sge.graphics.UniformLocation",
          wrapName = "apply",
          unwrapName = "toInt"
        ),
        hints = Set(
          "com.badlogic.gdx.graphics.glutils.ShaderProgram#fetchUniformLocation(String)",
          "com.badlogic.gdx.graphics.g3d.shaders.BaseShader#locations"
        ),
        underlying = balticporter.tir.OpaqueSpec.Primitive.Int,
        scope = balticporter.tir.RuleScope.Everywhere(
          except = Set(
            "com.badlogic.gdx.graphics.GL20",
            "com.badlogic.gdx.graphics.GL30",
            "com.badlogic.gdx.graphics.GL31",
            "com.badlogic.gdx.graphics.GL32"
          )
        )
      )
    )

  /** libGDX's own `@Null` moved out of the annotation and into the type — `lowlevel.Nullable[T]`, the hand port's own wrapper. Shared surface, lives here once, inherited via `extendedBy`. `Named`
    * closes the abstract-type-parameter mismatch: the union floor `T | Null` is not transparent at an abstract `T`; `Nullable[T]` composes at every `T`, so that scope exit is gone entirely.
    */
  def nullability: balticporter.transform.NullabilityTransform =
    new balticporter.transform.NullabilityTransform(
      annotations = Set("com.badlogic.gdx.utils.Null"),
      target = balticporter.transform.NullabilityTransform.Target.Named("lowlevel.Nullable"),
      scope = balticporter.tir.RuleScope.Everywhere(nullabilityErasureExempt)
      // after retarget to lls ObjectMap, get(K) returns Nullable[V] natively;
      // 3.1al's .orNull Template forces the 1-arg overload where Scala would otherwise pick
      // get(K,V):V (SuppressionPhase places @nowarn on the resulting deprecated-orNull call).
    )

  /** Types whose `@Null`-annotated overload sets create ERASURE CONFLICTS under Named mode: `Nullable[A]` erases to `Object`, so distinct `@Null`-annotated overloads of the same arity collide (Union
    * mode kept them distinct). Scoped OUT, kept upstream-typed; each a COUNTED `ScopedOut` decision with a porter note. (CharArray: 10 `append` overloads; Image: 3 constructors -- all erase to one
    * descriptor.)
    */
  def nullabilityErasureExempt: Set[String] = Set(
    "com.badlogic.gdx.utils.CharArray",
    "com.badlogic.gdx.scenes.scene2d.ui.Image",
    "com.badlogic.gdx.utils.Json",
    "com.badlogic.gdx.scenes.scene2d.actions.TemporalAction",
    "com.badlogic.gdx.utils.Pools"
  )
}
