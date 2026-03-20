package sge.sbt

import sbt._
import sbt.Keys._

import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport._

import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermissions

/** Zig-based cross-compilation support for Scala Native.
  *
  * Uses `zig cc` / `zig c++` as drop-in replacements for clang/clang++,
  * enabling cross-compilation to non-host platforms from a single machine.
  * This is similar to the approach used by scala-native-zig.
  *
  * Usage in build.sbt:
  * {{{
  * .nativePlatform(scalaVersions = Seq(sv),
  *   settings = ZigCross.crossSettings(Platform.LinuxX86_64, crossLibDir))
  * }}}
  */
object ZigCross {

  /** Check if zig is available on PATH. */
  def isAvailable: Boolean = {
    try {
      val pb = new ProcessBuilder("zig", "version")
      pb.redirectErrorStream(true)
      val p = pb.start()
      p.waitFor() == 0
    } catch {
      case _: Exception => false
    }
  }

  /** Create a zig cc wrapper script for the target platform.
    * Returns the path to the wrapper script.
    */
  def clangWrapper(platform: Platform, wrapperDir: File): Path = {
    val dir = wrapperDir.toPath
    Files.createDirectories(dir)
    val wrapper = dir.resolve(s"zig-cc-${platform.classifier}")
    if (!Files.exists(wrapper)) {
      val q = '"'
      val content = s"#!/bin/sh\nexec zig cc -target ${platform.zigTarget} ${q}$$@${q}\n"
      Files.writeString(wrapper, content)
      try Files.setPosixFilePermissions(wrapper, PosixFilePermissions.fromString("rwxr-xr-x"))
      catch { case _: UnsupportedOperationException => () }
    }
    wrapper
  }

  /** Create a zig c++ wrapper script for the target platform.
    * Returns the path to the wrapper script.
    */
  def clangPPWrapper(platform: Platform, wrapperDir: File): Path = {
    val dir = wrapperDir.toPath
    Files.createDirectories(dir)
    val wrapper = dir.resolve(s"zig-cxx-${platform.classifier}")
    if (!Files.exists(wrapper)) {
      val q = '"'
      val content = s"#!/bin/sh\nexec zig c++ -target ${platform.zigTarget} ${q}$$@${q}\n"
      Files.writeString(wrapper, content)
      try Files.setPosixFilePermissions(wrapper, PosixFilePermissions.fromString("rwxr-xr-x"))
      catch { case _: UnsupportedOperationException => () }
    }
    wrapper
  }

  /** sbt settings for cross-compilation to a target platform using zig.
    *
    * Configures `nativeConfig` with zig cc/c++ wrappers, the target triple,
    * and linker flags pointing at the cross-compiled native libraries.
    *
    * @param platform target platform to cross-compile for
    * @param crossLibDir directory containing the cross-compiled native libraries for the target
    */
  def crossSettings(platform: Platform, crossLibDir: File): Seq[Setting[_]] = Seq(
    nativeConfig := {
      val c = nativeConfig.value
      val wrapperDir = target.value / "zig-wrappers"
      c.withClang(clangWrapper(platform, wrapperDir))
        .withClangPP(clangPPWrapper(platform, wrapperDir))
        .withTargetTriple(Some(platform.scalaNativeTarget))
        .withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(crossLibDir, platform))
    }
  )
}
