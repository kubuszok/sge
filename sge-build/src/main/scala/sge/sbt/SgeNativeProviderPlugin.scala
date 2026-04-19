package sge.sbt

import multiarch.sbt.NativeProviderPlugin

import sbt._
import sbt.Keys._

import scala.scalanative.sbtplugin.ScalaNativePlugin
import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport._

/** AutoPlugin that adds SGE-specific resource embedding on top of NativeProviderPlugin.
  *
  * Auto-triggers when both `ScalaNativePlugin` and `NativeProviderPlugin` are present.
  * `NativeProviderPlugin` handles manifest discovery, extraction, linker flags, and rpath.
  * This plugin layers SGE resource patterns for Scala Native embedding.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: SgeNativeProviderPlugin,projectSettings,requires,trigger
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
  */
object SgeNativeProviderPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = NativeProviderPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    nativeConfig := {
      nativeConfig.value
        .withEmbedResources(true)
        .withResourceIncludePatterns(Seq(
          "**.png", "**.jpg", "**.wav", "**.ogg", "**.mp3",
          "**.txt", "**.json", "**.xml",
          "**.g3dj", "**.g3db", "**.atlas", "**.fnt", "**.tmx"
        ))
    }
  )
}
