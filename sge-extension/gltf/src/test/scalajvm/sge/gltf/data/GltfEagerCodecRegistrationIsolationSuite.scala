/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-846 (wave 2026-07-18-G, territory G5) — forked-isolation pin for the
 * ISS-782(c) eager codec registration contract.
 *
 * `class GLTFExtensions`'s constructor calls `GLTFCodecs.ensureCodecsRegistered()`
 * so that constructing any instance eagerly forces `object GLTFCodecs` to
 * initialise and register the lazy-parse decoders — BEFORE `GLTFExtensions.get`
 * can be reached. Without it, `get` on a stored-but-unparsed extension silently
 * returns empty in any process where nothing has yet touched `object GLTFCodecs`
 * (ISS-782c).
 *
 * WHY A SPAWNED JVM (not just Test/fork): every test in this module shares ONE
 * forked JVM, and most gltf tests parse a glTF document (which imports
 * `GLTFCodecs.given` and thus initialises `object GLTFCodecs`) long before this
 * suite runs. In that shared, already-initialised JVM the decoders are present
 * regardless of the constructor, so an in-process assertion could never catch a
 * regression of the eager-init call. To pin the eager init HONESTLY, the
 * decode-before-registration scenario must run in a pristine JVM where nothing
 * has forced `object GLTFCodecs` yet. This mirrors the established
 * fresh-subprocess isolation pattern used by
 * sge-test/it-desktop's DesktopIntegrationTest (ProcessBuilder + a dedicated
 * *HarnessMain).
 *
 * CLASSPATH: sbt 2.0 launches forked tests via sbt.ForkMain with only sbt's
 * agent jars on `java.class.path`; the app/test classpath is served through a
 * separate classloader (see DesktopIntegrationTest's harnessClasspath note).
 * it-desktop solves this with a build-injected `-Dsge.it.classpath`; this suite
 * needs NO build change — it reconstructs the real classpath from the test's own
 * classloader hierarchy (URLClassLoader.getURLs), falling back to
 * `java.class.path`. The reconstructed classpath includes the test-classes dir,
 * so the spawned JVM finds GltfEagerCodecRegistrationHarnessMain.
 *
 * JVM-only (src/test/scalajvm): spawning a JVM subprocess is a JVM concern.
 */
package sge
package gltf
package data

import java.io.File
import java.net.{ URL, URLClassLoader }
import java.nio.file.Files

class GltfEagerCodecRegistrationIsolationSuite extends munit.FunSuite {

  /** Reconstruct the full runtime classpath for the spawned JVM. Walks this test's classloader hierarchy collecting `URLClassLoader.getURLs()` (sbt 2.0 forks hide the real classpath from
    * `java.class.path`), then unions `java.class.path` as a fallback so a non-forked/non-sbt invocation still works.
    */
  private def runtimeClasspath: String = {
    val sep  = File.pathSeparator
    val seen = scala.collection.mutable.LinkedHashSet.empty[String]

    def addUrls(urls: Array[URL]): Unit =
      urls.foreach { url =>
        val path =
          try new File(url.toURI).getAbsolutePath
          catch { case _: Throwable => url.getPath }
        if (path.nonEmpty) seen += path
      }

    var cl: ClassLoader = getClass.getClassLoader
    while (cl != null) {
      cl match {
        case u: URLClassLoader => addUrls(u.getURLs)
        case other =>
          // sbt's LayeredClassLoader / other loaders may still expose getURLs.
          try {
            val m = other.getClass.getMethod("getURLs")
            m.invoke(other) match {
              case urls: Array[URL @unchecked] => addUrls(urls)
              case _ => ()
            }
          } catch { case _: Throwable => () }
      }
      cl = cl.getParent
    }

    System.getProperty("java.class.path", "").split(sep).filter(_.nonEmpty).foreach(seen += _)
    seen.mkString(sep)
  }

  test("ISS-846/ISS-782c: GLTFExtensions eagerly registers GLTF codecs in a fresh JVM") {
    val resultsFile = File.createTempFile("sge-gltf-eager-codec-", ".txt")
    resultsFile.deleteOnExit()

    val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
    val cp      = runtimeClasspath

    val cmd = new java.util.ArrayList[String]()
    cmd.add(javaBin)
    cmd.add("-cp")
    cmd.add(cp)
    cmd.add("sge.gltf.data.GltfEagerCodecRegistrationHarnessMain")
    cmd.add(resultsFile.getAbsolutePath)

    val pb = new ProcessBuilder(cmd)
    pb.inheritIO() // surface the harness stdout/stderr in the test log
    val process  = pb.start()
    val exitCode = process.waitFor()

    val result = new String(Files.readAllBytes(resultsFile.toPath)).trim
    System.err.println(s"=== ISS-846 eager codec registration (fresh JVM) ===\n$result")

    // An empty result means the subprocess never reached the harness (e.g. a
    // broken reconstructed classpath) — fail loudly, never silently green.
    assert(
      result.nonEmpty,
      s"harness wrote no result — spawned JVM likely could not load the harness main (classpath=$cp)"
    )
    assert(
      result.startsWith("PASS:"),
      s"ISS-846/ISS-782c: eager codec registration failed in a pristine JVM: $result"
    )
    assertEquals(exitCode, 0, s"harness process exited with code $exitCode: $result")
  }
}
