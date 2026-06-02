package sge.sbt

import sbt._

sealed abstract class SgeExtension(val artifactName: String, val jvmOnly: Boolean)

object SgeExtension {
  case object Ai extends SgeExtension("sge-extension-ai", jvmOnly = false)
  case object Anim8 extends SgeExtension("sge-extension-anim8", jvmOnly = false)
  case object Colorful extends SgeExtension("sge-extension-colorful", jvmOnly = false)
  case object Controllers extends SgeExtension("sge-extension-controllers", jvmOnly = false)
  case object Ecs extends SgeExtension("sge-extension-ecs", jvmOnly = false)
  case object FreeType extends SgeExtension("sge-extension-freetype", jvmOnly = false)
  case object Gltf extends SgeExtension("sge-extension-gltf", jvmOnly = false)
  case object Graphs extends SgeExtension("sge-extension-graphs", jvmOnly = false)
  case object JBump extends SgeExtension("sge-extension-jbump", jvmOnly = false)
  case object Noise extends SgeExtension("sge-extension-noise", jvmOnly = false)
  case object Physics extends SgeExtension("sge-extension-physics", jvmOnly = false)
  case object Physics3d extends SgeExtension("sge-extension-physics3d", jvmOnly = false)
  case object Screens extends SgeExtension("sge-extension-screens", jvmOnly = false)
  case object Textra extends SgeExtension("sge-extension-textra", jvmOnly = false)
  case object Tools extends SgeExtension("sge-extension-tools", jvmOnly = true)
  case object Vfx extends SgeExtension("sge-extension-vfx", jvmOnly = false)
  case object VisUi extends SgeExtension("sge-extension-visui", jvmOnly = false)

  val values: Set[SgeExtension] = Set(
    Ai,
    Anim8,
    Colorful,
    Controllers,
    Ecs,
    FreeType,
    Gltf,
    Graphs,
    JBump,
    Noise,
    Physics,
    Physics3d,
    Screens,
    Textra,
    Tools,
    Vfx,
    VisUi
  )

  def jvmDeps(extensions: Set[SgeExtension], scalaBinVer: String, version: String): Seq[ModuleID] =
    extensions.toSeq.map { ext =>
      "com.kubuszok" % s"${ext.artifactName}_$scalaBinVer" % version
    }

  def jsDeps(extensions: Set[SgeExtension], scalaBinVer: String, version: String): Seq[ModuleID] =
    extensions.toSeq.filterNot(_.jvmOnly).map { ext =>
      "com.kubuszok" % s"${ext.artifactName}_sjs1_$scalaBinVer" % version
    }

  def nativeDeps(extensions: Set[SgeExtension], scalaBinVer: String, version: String): Seq[ModuleID] =
    extensions.toSeq.filterNot(_.jvmOnly).map { ext =>
      "com.kubuszok" % s"${ext.artifactName}_native0.5_$scalaBinVer" % version
    }
}
