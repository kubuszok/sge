package sge.port.ext.screens

import balticporter.runner.ClasspathCache
import sge.port.full.LlsClasspath
import balticporter.core.{ FrontendConfig, ParityRef, PortManifest, Provenance, RuntimeMode }
import sge.port.full.LibgdxPolicy
import balticporter.runner.{ Determinism, PortRun, SourceSet, VendoredCommit }
import balticporter.transform.TypeRedirectTransform

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** Migrate **libgdx-screenmanager** (`src/main/java`, 22 types — a screen stack, transition queue, eleven concrete transitions) through the TIR. A DEPENDENT port (`gdx/src` a RESOLUTION root,
  * [[LibgdxPolicy.core]] EXTENDED), with a SECOND dependency, guacamole, which RESOLVES via [[ScreensClasspath]] but cannot be EMITTED — re-pointed at hand-written Scala this port ships
  * ([[TypeRedirectTransform]]). Scope: `src/main/java` only.
  */
object ScreensMigrate:

  def main(args: Array[String]): Unit =
    val repoRoot = ScreensPort.repoRoot
    val base     = ScreensPort.upstream(repoRoot).resolve("src/main/java")
    val gdxSrc   = repoRoot.resolve("original-src/libgdx/gdx/src").normalize

    PortRun(
      label = "sge-screens",
      portRoot = repoRoot.resolve("ported/sge-screens"),
      sourceSet = SourceSet.Main,
      frontend = FrontendConfig(
        base,
        ScreensPort.javaFiles(base),
        classpath = LlsClasspath.entries(repoRoot) ++ ScreensClasspath.entries(repoRoot),
        resolutionRoots = List(gdxSrc)
      ),
      phases = Nil, // supplied by the manifest — the two sources are mutually exclusive
      manifest = Some(ScreensPolicy.core(repoRoot)),
      provenance = Some(
        Provenance(
          upstreamName = "libgdx-screenmanager",
          upstreamCommit = VendoredCommit.of(base),
          originalLicense = "Apache-2.0",
          sourcePathPrefix = "src/main/java",
          sourceRoot = base.toString
        )
      ),
      // NOT Vendored: LibgdxCoreMigrate already vendors the collection shims into this
      // module.
      runtimeMode = RuntimeMode.Dependency,
      determinism = Determinism.fromArgs(args.toSeq),
      nextStep = "just screens-measure"
    ).execute()

/** Paths and file selection shared by the two source sets of this port. */
object ScreensPort:

  def repoRoot: Path =
    Path.of(sys.props.getOrElse("balticporter.root", ".")).toAbsolutePath.normalize

  def upstream(repoRoot: Path): Path =
    repoRoot.resolve("original-src/libgdx-screenmanager").normalize

  def javaFiles(base: Path): List[String] =
    Files
      .walk(base)
      .iterator()
      .asScala
      .filter(p => p.toString.endsWith(".java"))
      .map(p => base.relativize(p).toString)
      .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
      .toList
      .sorted

/** libgdx-screenmanager's per-library policy -- a DEPENDENT of libGDX core's. `dropTypes`/`dropMethods`/`packageRenames`/signature-affecting phases are INHERITED, not restated; `inject` is NOT
  * inherited (exactly one module ships each replacement file).
  */
object ScreensPolicy:

  def core(repoRoot: Path): PortManifest =
    LibgdxPolicy
      .core(repoRoot)
      .extendedBy(
        PortManifest(
          name = "sge-screens",
          governs = Set("de.eskalon.commons"),
          // sge flattens this library's three upstream packages into sge.screen and
          // sge.screen.utils; three pairs so de.eskalon.commons -> sge.screen alone would not
          // produce sge.screen.screen. libGDX's own com.badlogic.gdx -> sge is INHERITED.
          packageRenames = Map(
            "de.eskalon.commons.screen" -> "sge.screen",
            "de.eskalon.commons.core" -> "sge.screen",
            "de.eskalon.commons.utils" -> "sge.screen.utils"
          ),
          surface = List(
            guacamole,
            nullability,
            new balticporter.transform.MethodBodyTransform(
              Map(
                "de.eskalon.commons.screen.transition.impl.ShaderTransition#render" ->
                  """{
                    |  this.renderContext.begin()
                    |  this.program$field.bind()
                    |  this.program$field.setUniformMatrix(sge.graphics.UniformLocation(this.projTransLoc), this.viewport.camera.combined)
                    |  this.program$field.setUniformf(sge.graphics.UniformLocation(this.progressLoc), progress)
                    |  this.program$field.setUniformi(sge.graphics.UniformLocation(this.lastScreenLoc), this.renderContext.textureBinder.bind(lastScreen.texture))
                    |  this.program$field.setUniformi(sge.graphics.UniformLocation(this.currScreenLoc), this.renderContext.textureBinder.bind(currScreen.texture))
                    |  this.screenQuad.render(this.program$field, sge.graphics.GL20.GL_TRIANGLE_STRIP)
                    |  this.renderContext.end()
                    |}""".stripMargin
              )
            ),
            balticporter.transform.PortMapTransform.forBases("sge")
          ),
          // THE REFERENCE HAND PORT for sge-screens. NOT inherited.
          parity = Some(ParityRef(roots = List(repoRoot.resolve("sge-extension/screens/src/main/scala").normalize)))
        )
      )

  /** screenmanager's OWN nullability annotation (`org.jspecify.annotations.Nullable`), a SECOND `NullabilityTransform` instance rather than a line in the base's set — folding it in would report
    * `never-fired` on every libGDX lane forever. `MergeablePolicy` composes both halves: `Named("lowlevel.Nullable")` composes at every `T`. CONSUMED (stripped from every declaration), so `jspecify`
    * stays only on the FRONTEND classpath.
    */
  def nullability: balticporter.transform.NullabilityTransform =
    new balticporter.transform.NullabilityTransform(annotations = Set("org.jspecify.annotations.Nullable"))

  /** The guacamole seam: `com.github.crykn.guacamole:gdx`'s types RESOLVE (via [[ScreensClasspath]]) but cannot be EMITTED, since this run converts no guacamole compilation unit -- re-pointed at
    * Scala this port ships (`ported/sge-screens/src/main/scala/sge/screen/guacamole`, hand-written). This table retires the day guacamole becomes a corpus port of its own.
    */
  def guacamole: TypeRedirectTransform = new TypeRedirectTransform(
    Map(
      // NestableFrameBuffer is why upstream depends on guacamole at all, and its ABSENCE
      // from the reference hand port is a behavioural defect.
      "de.damios.guacamole.gdx.graphics.NestableFrameBuffer" -> "sge.screen.guacamole.NestableFrameBuffer",
      "de.damios.guacamole.gdx.graphics.GLUtils" -> "sge.screen.guacamole.GLUtils",
      "de.damios.guacamole.gdx.graphics.QuadMeshGenerator" -> "sge.screen.guacamole.QuadMeshGenerator",
      "de.damios.guacamole.gdx.graphics.ShaderProgramFactory" -> "sge.screen.guacamole.ShaderProgramFactory",
      "de.damios.guacamole.gdx.graphics.ShaderCompatibilityHelper" -> "sge.screen.guacamole.ShaderCompatibilityHelper",
      "de.damios.guacamole.gdx.log.Logger" -> "sge.screen.guacamole.Logger",
      "de.damios.guacamole.gdx.log.LoggerService" -> "sge.screen.guacamole.LoggerService",
      "de.damios.guacamole.Preconditions" -> "sge.screen.guacamole.Preconditions",
      "de.damios.guacamole.tuple.Pair" -> "sge.screen.guacamole.Pair",
      "de.damios.guacamole.annotations.Beta" -> "sge.screen.guacamole.Beta"
    )
  )

/** libgdx-screenmanager's COMPILE-scope dependency, for shadow-class resolution only. libGDX itself arrives as a SOURCE resolution root instead (excluded here rather than resolved twice); what's left
  * is guacamole and the jspecify annotation jar both use. guacamole is jitpack-only, so the repository is named explicitly; a resolve failure is FATAL.
  */
object ScreensClasspath:

  def cache(repoRoot: Path): Path = repoRoot.resolve("out/screens-classpath.txt")

  private val coordinates = List(
    "com.github.crykn.guacamole:gdx:v0.3.6",
    "org.jspecify:jspecify:0.3.0"
  )

  /** everything before the coordinates -- part of the cache's fingerprint, since an exclusion decides the classpath as much as a version does.
    */
  private val resolverArgs = List(
    "-r",
    "https://jitpack.io",
    // libGDX is a SOURCE resolution root; a second copy on the frontend classpath is a
    // second answer to every com.badlogic.gdx.* name, decided by scan order.
    "--exclude",
    "com.badlogicgames.gdx:gdx"
  )

  def entries(repoRoot: Path): List[Path] =
    ClasspathCache.entries(cache(repoRoot), "screens", coordinates, resolverArgs)

  /** Guarantee the cache file exists AND was resolved from these coordinates and exclusions, fetching once if not. Returns the joined line.
    */
  def ensure(repoRoot: Path): String =
    Files.readString(ClasspathCache.ensure(cache(repoRoot), "screens", coordinates, resolverArgs)).trim
