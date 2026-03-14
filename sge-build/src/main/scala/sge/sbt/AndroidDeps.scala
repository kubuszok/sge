package sge.sbt

import sbt._

import java.io.File

/** Resolves Android-specific runtime dependencies for APK builds.
  *
  * PanamaPort (com.v7878.foreign — Panama FFM backport for Android API 26+) is published as
  * AAR artifacts on Maven Central. Standard sbt/Ivy dependency resolution doesn't handle AARs,
  * so this helper downloads the AARs, extracts classes.jar from each, and provides them as
  * a Seq[File] for use in unmanagedJars / fullClasspath.
  *
  * The sge library compiles against PanamaPort Core (via sge-jvm-platform-android's
  * unmanagedJars), but the full transitive closure is only needed at runtime when building
  * Android APKs. Desktop JVM uses JDK 22+ java.lang.foreign instead.
  */
object AndroidDeps {

  val panamaPortVersion = "v0.1.0"

  /** Maven Central artifact descriptor with download URL and cache file name. */
  private sealed trait Dep {
    def artifactId: String
    def url: String
    def cacheFileName: String
  }

  /** AAR artifact — needs classes.jar extraction.
    * @param classifier "release" for PanamaPort modules, empty string for standalone AARs
    */
  private case class AarDep(groupPath: String, artifactId: String, version: String, classifier: String) extends Dep {
    def url: String = {
      val base = s"https://repo1.maven.org/maven2/$groupPath/$artifactId/$version"
      val suffix = if (classifier.isEmpty) s"$artifactId-$version.aar" else s"$artifactId-$version-$classifier.aar"
      s"$base/$suffix"
    }
    def cacheFileName: String = s"$artifactId-$version-classes.jar"
  }

  /** JAR artifact — directly usable. */
  private case class JarDep(groupPath: String, artifactId: String, version: String) extends Dep {
    def url: String = {
      val base = s"https://repo1.maven.org/maven2/$groupPath/$artifactId/$version"
      s"$base/$artifactId-$version.jar"
    }
    def cacheFileName: String = s"$artifactId-$version.jar"
  }

  /** All PanamaPort dependencies (compile + runtime transitive closure).
    *
    * Resolved from Maven Central POM files. The dependency graph is:
    *   Core → VarHandles (compile), Unsafe (runtime), SunCleanerStub, R8Annotations, DexFile
    *   Unsafe → DexFile (compile), LLVM (runtime), SunUnsafeWrapper (pom-only)
    *   VarHandles → Unsafe (runtime), R8Annotations
    *   LLVM → Core (compile), Unsafe (runtime), R8Annotations
    *   DexFile → AndroidMisc (runtime)
    *
    * Artifact formats (verified on Maven Central):
    *   - PanamaPort modules (Core, Unsafe, VarHandles, LLVM): AAR with "-release" classifier
    *   - SunCleanerStub, R8Annotations: AAR without classifier
    *   - DexFile, AndroidMisc: JAR
    *   - SunUnsafeWrapper: AAR without classifier (contains libs/classes.jar with Getter.class)
    */
  private val allDeps: Seq[Dep] = Seq(
    // PanamaPort modules — AAR without classifier (v0.1.0 has no -release classifier)
    AarDep("io/github/vova7878/panama", "Core", panamaPortVersion, ""),
    AarDep("io/github/vova7878/panama", "Unsafe", panamaPortVersion, ""),
    AarDep("io/github/vova7878/panama", "VarHandles", panamaPortVersion, ""),
    AarDep("io/github/vova7878/panama", "LLVM", panamaPortVersion, ""),
    // Standalone AARs — no classifier
    AarDep("io/github/vova7878", "SunCleanerStub", "v1.0.0", ""),
    AarDep("io/github/vova7878", "R8Annotations", "v1.0.0", ""),
    // SunUnsafeWrapper — AAR without classifier; has libs/ with Getter.class (raung bytecode)
    AarDep("io/github/vova7878", "SunUnsafeWrapper", "v1.0.0", ""),
    // JARs — directly usable
    JarDep("io/github/vova7878", "DexFile", "v1.2.0"),
    JarDep("io/github/vova7878", "AndroidMisc", "v1.0.0")
  )

  /** Resolve all PanamaPort runtime dependencies, downloading and caching as needed.
    *
    * Returns a Seq[File] of JARs (extracted from AARs or downloaded directly)
    * suitable for adding to unmanagedJars.
    *
    * @param cacheDir directory to cache downloaded artifacts
    * @param log sbt logger
    */
  def resolvePanamaPort(cacheDir: File, log: sbt.util.Logger): Seq[File] = {
    IO.createDirectory(cacheDir)
    allDeps.flatMap {
      case dep: AarDep => resolveAar(cacheDir, dep, log)
      case dep: JarDep => Seq(resolveJar(cacheDir, dep, log))
    }
  }

  /** Extract all JARs from an AAR: classes.jar + any JARs in libs/. */
  private def resolveAar(cacheDir: File, dep: AarDep, log: sbt.util.Logger): Seq[File] = {
    val mainJar = cacheDir / dep.cacheFileName
    val aarFile = cacheDir / s"${dep.artifactId}-${dep.version}.aar"
    if (!mainJar.exists()) {
      if (!aarFile.exists()) {
        log.info(s"Downloading ${dep.artifactId} AAR from ${dep.url}")
        val in = java.net.URI.create(dep.url).toURL.openStream()
        try { IO.transfer(in, aarFile) }
        finally { in.close() }
      }
      log.info(s"Extracting JARs from ${dep.artifactId} AAR")
      val zip = new java.util.zip.ZipFile(aarFile)
      try {
        // Extract classes.jar
        val entry = zip.getEntry("classes.jar")
        if (entry == null) sys.error(s"${dep.artifactId} AAR does not contain classes.jar")
        val in = zip.getInputStream(entry)
        try { IO.transfer(in, mainJar) }
        finally { in.close() }
        // Extract any JARs in libs/
        import scala.jdk.CollectionConverters._
        zip.entries().asScala
          .filter(e => e.getName.startsWith("libs/") && e.getName.endsWith(".jar"))
          .foreach { libEntry =>
            val libName = libEntry.getName.substring("libs/".length)
            val libJar = cacheDir / s"${dep.artifactId}-${dep.version}-$libName"
            if (!libJar.exists()) {
              val libIn = zip.getInputStream(libEntry)
              try { IO.transfer(libIn, libJar) }
              finally { libIn.close() }
            }
          }
      } finally { zip.close() }
    }
    // Return main JAR + any extracted lib JARs
    val libJars = IO.listFiles(cacheDir).filter { f =>
      f.getName.startsWith(s"${dep.artifactId}-${dep.version}-") && f.getName.endsWith(".jar") &&
        f.getName != dep.cacheFileName
    }.toSeq
    mainJar +: libJars
  }

  private def resolveJar(cacheDir: File, dep: JarDep, log: sbt.util.Logger): File = {
    val jarFile = cacheDir / dep.cacheFileName
    if (!jarFile.exists()) {
      log.info(s"Downloading ${dep.artifactId} JAR from ${dep.url}")
      val in = java.net.URI.create(dep.url).toURL.openStream()
      try { IO.transfer(in, jarFile) }
      finally { in.close() }
    }
    jarFile
  }
}
