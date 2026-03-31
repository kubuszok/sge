package sge.sbt

import sbt._
import sbt.Keys._

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.jar.JarFile

/** Native library bundle manifest format.
  *
  * Each library or extension that requires native (`.a` / `.so` / `.dylib` / `.dll`)
  * libraries for Scala Native linking can ship a `native-bundle.json` resource in its
  * JAR (or project resources). The sbt plugin discovers all such manifests on the
  * classpath and auto-configures `nativeConfig` with the correct `-L`, `-l`, and
  * platform-specific linker flags.
  *
  * === Manifest format (`native-bundle.json`) ===
  * {{{
  * {
  *   "name": "sge-native-ops",
  *   "libraries": [
  *     {
  *       "name": "sge_native_ops",
  *       "platforms": {
  *         "linux-x86_64":    { "flags": ["-lpthread", "-ldl"] },
  *         "linux-aarch64":   { "flags": ["-lpthread", "-ldl"] },
  *         "macos-x86_64":    { "flags": ["-framework", "Cocoa", "-framework", "IOKit"] },
  *         "macos-aarch64":   { "flags": ["-framework", "Cocoa", "-framework", "IOKit"] },
  *         "windows-x86_64":  { "flags": ["-lntdll", "-lmsvcrt"] },
  *         "windows-aarch64": { "flags": ["-lntdll", "-lmsvcrt"] }
  *       }
  *     }
  *   ]
  * }
  * }}}
  *
  * Libraries listed under `"libraries"` will be linked via `-l<name>`. Additional
  * platform-specific flags (frameworks, system libraries, rpath entries) are merged
  * from all discovered manifests.
  *
  * === Usage ===
  * For consumers:
  * {{{
  * // In build.sbt — no manual linker flags needed
  * .nativePlatform(settings = Seq(
  *   nativeConfig := {
  *     val c = nativeConfig.value
  *     val merged = NativeLibBundle.mergedLinkerFlags.value
  *     c.withLinkingOptions(c.linkingOptions ++ merged)
  *   }
  * ))
  * }}}
  *
  * Or even simpler, if the plugin auto-applies:
  * {{{
  * .settings(NativeLibBundle.settings *)
  * }}}
  */
object NativeLibBundle {

  // ── Manifest data model ───────────────────────────────────────────

  /** A single native library entry in a bundle manifest. */
  final case class NativeLibrary(
    /** Library name, used as `-l<name>` in linker flags. */
    name: String,
    /** Per-platform configuration. Key is the platform classifier (e.g. "linux-x86_64"). */
    platforms: Map[String, PlatformConfig]
  )

  /** Platform-specific configuration for a native library. */
  final case class PlatformConfig(
    /** Additional linker flags for this platform (e.g. `["-framework", "Cocoa"]`). */
    flags: Seq[String] = Seq.empty
  )

  /** A complete native bundle manifest. */
  final case class BundleManifest(
    /** Human-readable name of this bundle (e.g. "sge-native-ops", "sge-controllers"). */
    name: String,
    /** Native libraries declared by this bundle. */
    libraries: Seq[NativeLibrary] = Seq.empty,
    /** Global linker flags that apply to all platforms (e.g. rpath entries). */
    globalFlags: Seq[String] = Seq.empty
  )

  // ── JSON parsing (minimal, no external deps) ──────────────────────

  /** Parses a `native-bundle.json` string into a [[BundleManifest]].
    * Uses a minimal JSON parser to avoid adding dependencies to the sbt plugin.
    */
  @SuppressWarnings(Array("deprecation"))
  def parseManifest(json: String): BundleManifest = {
    import scala.util.parsing.json.JSON
    val parsed = JSON.parseFull(json).getOrElse(
      throw new RuntimeException("Failed to parse native-bundle.json: invalid JSON")
    )
    val root = parsed.asInstanceOf[Map[String, Any]]
    val name = root.getOrElse("name", "unnamed").toString
    val globalFlags = root.get("globalFlags") match {
      case Some(arr: List[_]) => arr.map(_.toString)
      case _ => Seq.empty
    }
    val libraries = root.get("libraries") match {
      case Some(arr: List[_]) => arr.map { item =>
        val lib = item.asInstanceOf[Map[String, Any]]
        val libName = lib.getOrElse("name", "").toString
        val platforms = lib.get("platforms") match {
          case Some(platMap: Map[_, _]) => platMap.map { case (k, v) =>
            val platConfig = v.asInstanceOf[Map[String, Any]]
            val flags = platConfig.get("flags") match {
              case Some(fl: List[_]) => fl.map(_.toString)
              case _ => Seq.empty
            }
            k.toString -> PlatformConfig(flags)
          }.toMap
          case _ => Map.empty[String, PlatformConfig]
        }
        NativeLibrary(libName, platforms)
      }
      case _ => Seq.empty
    }
    BundleManifest(name, libraries, globalFlags)
  }

  // ── Manifest discovery ────────────────────────────────────────────

  /** Discovers all `native-bundle.json` resources on the compile classpath. */
  val discoverManifests = taskKey[Seq[BundleManifest]](
    "Discovers all native-bundle.json manifests from classpath dependencies and project resources"
  )

  /** Merged linker flags from all discovered manifests for the current platform. */
  val mergedLinkerFlags = taskKey[Seq[String]](
    "Linker flags merged from all native-bundle.json manifests for the target platform"
  )

  /** The target platform for native library resolution. Defaults to the host platform. */
  val nativeBundlePlatform = settingKey[Platform](
    "Target platform for native bundle resolution. Defaults to host platform."
  )

  /** Optional: the directory containing native libraries. When set, `-L<dir>` is prepended to flags. */
  val nativeBundleLibDir = taskKey[Option[File]](
    "Optional directory containing native libraries. Adds -L<dir> to linker flags."
  )

  // ── Settings ──────────────────────────────────────────────────────

  /** Settings to add to projects that want automatic native bundle resolution. */
  lazy val settings: Seq[Setting[_]] = Seq(
    nativeBundlePlatform := Platform.host,
    nativeBundleLibDir := None,
    discoverManifests := {
      val log = streams.value.log
      val cp  = (Compile / dependencyClasspathAsJars).value

      // Scan JARs for native-bundle.json
      val fromJars: Seq[BundleManifest] = cp.flatMap { entry =>
        val file = entry.data
        if (file.isFile && file.getName.endsWith(".jar")) {
          try {
            val jar = new JarFile(file)
            try {
              val manifestEntry = jar.getEntry("native-bundle.json")
              if (manifestEntry != null) {
                val reader = new InputStreamReader(jar.getInputStream(manifestEntry), StandardCharsets.UTF_8)
                try {
                  val sb = new StringBuilder
                  val buf = new Array[Char](4096)
                  var read = reader.read(buf)
                  while (read > 0) { sb.appendAll(buf, 0, read); read = reader.read(buf) }
                  val manifest = parseManifest(sb.toString)
                  log.info(s"[native-bundle] Found manifest '${manifest.name}' in ${file.getName}")
                  Some(manifest)
                } finally reader.close()
              } else None
            } finally jar.close()
          } catch {
            case e: Exception =>
              log.warn(s"[native-bundle] Error reading ${file.getName}: ${e.getMessage}")
              None
          }
        } else None
      }

      // Scan project resources for native-bundle.json
      val resourceDirs = (Compile / resourceDirectories).value
      val fromResources: Seq[BundleManifest] = resourceDirs.flatMap { dir =>
        val f = dir / "native-bundle.json"
        if (f.exists()) {
          try {
            val json = IO.read(f, StandardCharsets.UTF_8)
            val manifest = parseManifest(json)
            log.info(s"[native-bundle] Found manifest '${manifest.name}' in ${f.getAbsolutePath}")
            Some(manifest)
          } catch {
            case e: Exception =>
              log.warn(s"[native-bundle] Error reading ${f.getAbsolutePath}: ${e.getMessage}")
              None
          }
        } else None
      }

      fromJars ++ fromResources
    },

    mergedLinkerFlags := {
      val manifests = discoverManifests.value
      val platform  = nativeBundlePlatform.value
      val libDirOpt = nativeBundleLibDir.value
      val log       = streams.value.log

      mergeFlags(manifests, platform, libDirOpt, log)
    }
  )

  /** Merge linker flags from all manifests for a given platform. */
  def mergeFlags(
    manifests: Seq[BundleManifest],
    platform: Platform,
    libDirOpt: Option[File],
    log: sbt.util.Logger
  ): Seq[String] = {
    val classifier = platform.classifier

    // Collect all library names and per-platform flags
    val libraryFlags = manifests.flatMap { manifest =>
      manifest.libraries.flatMap { lib =>
        val libLink = Seq(s"-l${lib.name}")
        val platFlags = lib.platforms.get(classifier).map(_.flags).getOrElse(Seq.empty)
        libLink ++ platFlags
      }
    }

    // Collect global flags
    val globalFlags = manifests.flatMap(_.globalFlags)

    // Library directory flag
    val libDirFlags = libDirOpt.toSeq.map(dir => s"-L${dir.getAbsolutePath}")

    val merged = libDirFlags ++ libraryFlags ++ globalFlags
    if (merged.nonEmpty) {
      log.info(s"[native-bundle] Merged ${merged.size} linker flags for $classifier from ${manifests.size} manifest(s)")
    }
    merged
  }
}
