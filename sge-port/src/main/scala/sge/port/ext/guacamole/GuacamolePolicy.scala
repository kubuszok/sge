package sge.port.ext.guacamole

import balticporter.core.PortManifest

import java.nio.file.Path

/** guacamole's policy (the `core` and `gdx` modules' `src/main/java`): libgdx-screenmanager's second dependency, a DEPENDENT of sge-core since its `gdx` module reads libGDX (`Gdx`, `FrameBuffer`,
  * `ShaderProgram`), so libGDX's policy is inherited from the core manifest and never restated. No hand port of it exists, so nothing is derived.
  */
object GuacamolePolicy {

  private val Guacamole = balticporter.tir.RuleScope.Only(Set("de.damios.guacamole"))

  /** @param coreManifest
    *   the sge-core manifest (lls -> sge-core chain)
    * @param reportRoots
    *   directories where the base ports' report trees live (port-map.tsv discovery)
    */
  def core(coreManifest: PortManifest, reportRoots: List[Path]): PortManifest =
    coreManifest.extendedBy(
      PortManifest(
        name = "sge-guacamole",
        // and its one class inside libGDX's own package (a reflection helper sge does not ship; dropped below)
        governs = Set("de.damios.guacamole", "com.badlogic.gdx.utils.reflect.ReflectionConverter"),
        packageRenames = Map("de.damios.guacamole" -> "sge.guacamole"),
        dropTypes = Set(
          // JVM thread pools (Scala.js has no ExecutorService family) and libGDX reflection sge does not ship
          "de.damios.guacamole.concurrent.DaemonThreadFactory",
          "de.damios.guacamole.concurrent.ThreadHandler",
          "de.damios.guacamole.gdx.reflection.ReflectionUtils",
          "com.badlogic.gdx.utils.reflect.ReflectionConverter"
        ),
        surface = List(
          // guacamole's own nullability annotation, consumed: `@Nullable` members become sge's `Nullable`
          new balticporter.transform.NullabilityTransform(
            target = balticporter.transform.NullabilityTransform.Target.Named("lowlevel.Nullable"),
            annotations = Set("org.jspecify.annotations.Nullable"),
            scope = Guacamole
          ),
          // formic's `Stringf` is a GWT-portable `String.format`; every platform sge targets has the JDK's own
          new balticporter.transform.TypeRedirectTransform(Map("text.formic.Stringf" -> "java.lang.String")),
          // LAST, deliberately: reads what the BASE actually emitted
          balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0")
        )
      )
    )

  /** the frontend classpath guacamole's java resolves against beyond libGDX (a source root): its annotation jar and formic. */
  def classpath(sgeRoot: Path): List[Path] =
    balticporter.runner.ClasspathCache.entries(
      sgeRoot.resolve("out/guacamole-classpath.txt"),
      "guacamole",
      List("org.jspecify:jspecify:0.3.0", "com.github.tommyettinger:formic:0.1.4")
    )
}
