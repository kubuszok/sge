package sge.port.ext.vfx

import balticporter.core.{ FrontendConfig, ParityRef, PortManifest, Provenance, RuntimeMode }
import sge.port.full.LlsClasspath
import sge.port.full.LibgdxPolicy
import balticporter.runner.{ Determinism, PortRun, SourceSet, VendoredCommit }

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** Migrate **gdx-vfx** (44 types — post-processing shader effects for libGDX) through the TIR. First GL-facing library: nearly every signature mentions a BASE-emitted type
  * (`FrameBuffer`/`Mesh`/`ShaderProgram`/`GL20`), testing the base-inheritance agreement. A DEPENDENT port: `gdx/src` a RESOLUTION root, [[LibgdxPolicy.core]] EXTENDED. Scope:
  * `core/src`+`effects/src` into ONE sbt module. No upstream suite; evidence is the hand-written suite under `ported/sge-vfx`.
  */
object VfxMigrate:

  def main(args: Array[String]): Unit =
    val repoRoot = Path.of(sys.props.getOrElse("balticporter.root", ".")).toAbsolutePath.normalize
    // The COMMON root of the two library modules -- Spoon takes each file as its own
    // input resource, so an ancestor root (not a package root) is fine.
    val base   = repoRoot.resolve("original-src/gdx-vfx/gdx-vfx").normalize
    val gdxSrc = repoRoot.resolve("original-src/libgdx/gdx/src").normalize

    // `gwt/` is the third directory under `base` and is NOT converted — see the scope note.
    val files = Files
      .walk(base)
      .iterator()
      .asScala
      .filter(p => p.toString.endsWith(".java"))
      .map(p => base.relativize(p).toString)
      .filter(f => f.startsWith("core/src/") || f.startsWith("effects/src/"))
      .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
      .toList
      .sorted

    PortRun(
      label = "sge-vfx",
      portRoot = repoRoot.resolve("ported/sge-vfx"),
      sourceSet = SourceSet.Main,
      frontend = FrontendConfig(base, files, LlsClasspath.entries(repoRoot), resolutionRoots = List(gdxSrc)),
      phases = Nil, // supplied by the manifest — the two sources are mutually exclusive
      manifest = Some(VfxPolicy.core(repoRoot)),
      provenance = Some(
        Provenance(
          upstreamName = "gdx-vfx",
          upstreamCommit = VendoredCommit.of(base),
          originalLicense = "Apache-2.0",
          // `sourceRoot` above is `<checkout>/gdx-vfx`, so a relativised origin reads
          // `core/src/com/crashinvaders/…` and this prefix restores the upstream repo-relative form.
          sourcePathPrefix = "gdx-vfx",
          sourceRoot = base.toString
        )
      ),
      // NOT `Vendored`: `LibgdxCoreMigrate` already vendors the collection shims into the module
      // this output is compiled beside. Vendoring again would define every support type twice —
      // which the JVM tolerates only while the copies agree, and the Scala.js/Native linkers reject.
      runtimeMode = RuntimeMode.Dependency,
      determinism = Determinism.fromArgs(args.toSeq),
      nextStep = "just vfx-measure"
    ).execute()

/** gdx-vfx's per-library policy -- a DEPENDENT of libGDX core's. `dropTypes`/`dropMethods`/ `packageRenames`/signature-affecting phases are INHERITED, not restated; `inject` is NOT inherited (exactly
  * one module ships each replacement file).
  */
object VfxPolicy:

  def core(repoRoot: Path): PortManifest =
    LibgdxPolicy
      .core(repoRoot)
      .extendedBy(
        PortManifest(
          name = "sge-vfx",
          governs = Set("com.crashinvaders.vfx"),
          // sge puts gdx-vfx at sge.vfx with upstream subpackages carried straight through;
          // libGDX's own com.badlogic.gdx -> sge is INHERITED, not restated.
          packageRenames = Map("com.crashinvaders.vfx" -> "sge.vfx"),
          // base renamed setTransform -> transform_= (BeanPropertyTransform); this override must go
          dropMethods = Set("com.crashinvaders.vfx.scene2d.VfxWidgetGroup#setTransform"),
          // TWO PER-LOCATION SELECTIONS: `@SuppressWarnings` on
          // ShaderVfxEffect suppresses nothing even in java (no cast, no type variable, no raw
          // type anywhere in the 193-line class) -- the port drops a marker that was already
          // vestigial upstream.
          resolutions = Map(
            "com.crashinvaders.vfx.effects.ShaderVfxEffect" -> "accept-dropped-type-annotation",

            // java.util.Map#clear() at ValueArrayMap#clear(): COVERAGE BY COINCIDENCE --
            // CollectionsTransform retyped the field to mutable.Map[K,V] with no table entry for
            // clear, and mutable.Map inherits clear() from Clearable with java's own semantics.
            "java.util.Map#clear()" -> "accept-jdk-member"
          ),
          surface = List(
            // VfxGLUtils' STATIC INITIALISER reflectively instantiates a GWT-only class out of
            // scope, so the branch is UNREACHABLE (hand port reaches the same conclusion).
            // `DefaultVfxGlExtension` now takes the threaded `sge.Sge` context, so `<clinit>`
            // becomes EMPTY and construction moves to the first call with one; a call made without
            // a context fails loudly, residue the hand port avoids by hand-writing the member.
            new balticporter.transform.MethodBodyTransform(
              Map(
                "com.crashinvaders.vfx.gl.VfxGLUtils#<clinit>" -> "{ }",
                // Align opaque boundary: Align.left/right/top/bottom are Align (opaque Int);
                // bitwise & needs both sides as Int
                "com.crashinvaders.vfx.effects.RadialBlurEffect#setOrigin(int)" ->
                  """{
                    |  var originX: scala.Float = 0.0f; var originY: scala.Float = 0.0f
                    |  if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.left)) != 0) { originX = 0.0f }
                    |  else { if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.right)) != 0) { originX = 1.0f } else { originX = 0.5f } }
                    |  if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.bottom)) != 0) { originY = 0.0f }
                    |  else { if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.top)) != 0) { originY = 1.0f } else { originY = 0.5f } }
                    |  this.setOrigin(originX, originY)
                    |}""".stripMargin,
                "com.crashinvaders.vfx.effects.ZoomEffect#setOrigin(int)" ->
                  """{
                    |  var originX: scala.Float = 0.0f; var originY: scala.Float = 0.0f
                    |  if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.left)) != 0) { originX = 0.0f }
                    |  else { if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.right)) != 0) { originX = 1.0f } else { originX = 0.5f } }
                    |  if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.bottom)) != 0) { originY = 0.0f }
                    |  else { if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.top)) != 0) { originY = 1.0f } else { originY = 0.5f } }
                    |  this.setOrigin(originX, originY)
                    |}""".stripMargin,
                "com.crashinvaders.vfx.utils.CommonUtils#getAlignFactorX" ->
                  """{
                    |  if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.left)) != 0) { return 0.0f } else ()
                    |  if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.right)) != 0) { return 1.0f } else ()
                    |  return 0.5f
                    |}""".stripMargin,
                "com.crashinvaders.vfx.utils.CommonUtils#getAlignFactorY" ->
                  """{
                    |  if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.bottom)) != 0) { return 0.0f } else ()
                    |  if ((sge.utils.Align.toInt(align) & sge.utils.Align.toInt(sge.utils.Align.top)) != 0) { return 1.0f } else ()
                    |  return 0.5f
                    |}""".stripMargin,
                "com.crashinvaders.vfx.framebuffer.VfxFrameBuffer#getBoundFboHandle" ->
                  """{
                    |  if (sge.vfx.gl.VfxGLUtils.glExtension == null)
                    |    sge.vfx.gl.VfxGLUtils.glExtension = new sge.vfx.gl.DefaultVfxGlExtension()
                    |  sge.vfx.gl.VfxGLUtils.getBoundFboHandle()
                    |}""".stripMargin,
                "com.crashinvaders.vfx.gl.VfxGLUtils#getBoundFboHandle" ->
                  """{
                    |  if (sge.vfx.gl.VfxGLUtils.glExtension == null)
                    |    throw new java.lang.IllegalStateException(
                    |      "sge.vfx.gl.VfxGLUtils.glExtension is not initialised. Upstream assigned it in a " +
                    |        "static initialiser; this port cannot, because constructing a DefaultVfxGlExtension " +
                    |        "needs the sge.Sge context and a class initialiser has no clause to take it from. " +
                    |        "It is initialised on the first VfxFrameBuffer bind (VfxFrameBuffer.getBoundFboHandle); " +
                    |        "bind one first, or assign VfxGLUtils.glExtension yourself.")
                    |  sge.vfx.gl.VfxGLUtils.glExtension.boundFboHandle
                    |}""".stripMargin
              )
            ),
            // WHAT A DEPENDENT ADDS TO THE BASE'S CONTEXT HOLDER: the holder is SHARED
            // SURFACE, inherited from `LibgdxPolicy.core`; this carries only the
            // PER-DECLARATION half. `VfxFrameBuffer#tmpCam` (a base-threaded class) moved to first
            // READ via `LazyInit`. NOT carried: `VfxGLUtils#<clinit>` (READS the holder, wrong site
            // kind); a `selfSupplied` entry (the suite is HAND-WRITTEN, declares its own `given`).
            new balticporter.transform.GlobalsToImplicitsTransform(
              requiredGivens = Map(
                // 3.1as: `|` separator names BOTH type parameters -- ValueArrayMap[K, V]
                // constructs DynamicArray[K] and DynamicArray[V], both need MkArray threaded.
                "com.crashinvaders.vfx.utils.ValueArrayMap" -> "lowlevel.MkArray:0|lowlevel.MkArray:1"
              ),
              extensions = List(
                balticporter.transform.ContextHolderExtension(
                  holder = "com.badlogic.gdx.Gdx",
                  sites = Map(
                    "com.crashinvaders.vfx.framebuffer.VfxFrameBuffer#tmpCam" ->
                      balticporter.transform.ContextSite.LazyInit
                  )
                )
              )
            ),
            // DEPENDENT SEEDS for the base's Align opaque family, folded via `MergeablePolicy` at
            // the base's pipeline position. Propagation follows pure-move flows and does NOT follow
            // a bitwise test, so these four PARAMETERS (only combined with Align.* via bitwise ops)
            // are unreachable from the base's field hints alone. Hints are parameter FQNs;
            // identity fields match the base's so `mergedWith` composes.
            new balticporter.transform.PrimitiveToOpaqueTransform(
              balticporter.tir.OpaqueSpec(
                fqn = "com.badlogic.gdx.utils.Align",
                target = balticporter.tir.OpaqueSpec.Target.Existing(
                  typeFqn = "sge.utils.Align",
                  wrapName = "apply",
                  unwrapName = "toInt"
                ),
                hints = Set(
                  // pure-arithmetic helpers whose align parameter is semantically an Align value
                  "com.crashinvaders.vfx.utils.CommonUtils#getAlignFactorX#align",
                  "com.crashinvaders.vfx.utils.CommonUtils#getAlignFactorY#align",
                  // overloaded beside setOrigin(float, float), existing so callers pass Align constants
                  "com.crashinvaders.vfx.effects.ZoomEffect#setOrigin#align",
                  "com.crashinvaders.vfx.effects.RadialBlurEffect#setOrigin#align"
                ),
                underlying = balticporter.tir.OpaqueSpec.Primitive.Int
              )
            ),
            // LAST, deliberately (as AshleyPolicy): reads what the BASE actually emitted;
            // must run after any seam re-pointing such a reference.
            balticporter.transform.PortMapTransform.forBases("sge")
          ),
          // THE REFERENCE HAND PORT for sge-vfx. NOT inherited.
          parity = Some(ParityRef(roots = List(repoRoot.resolve("sge-extension/vfx/src/main/scala").normalize)))
        )
      )
