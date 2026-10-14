package sge.port.ext.colorful

import balticporter.core.{ ParityRef, PortManifest }

import java.nio.file.Path

/** colorful-gdx's policy (`colorful/src/main/java`, 46 types: seven colour spaces, each with its packed-float `ColorTools`, palettes, gradients, a `ColorfulBatch` and a `ColorfulSprite`) — a
  * DEPENDENT of sge-core: every batch implements libGDX's `Batch` and every sprite extends its `Sprite`, so libGDX's policy is inherited from the core manifest and never restated.
  */
object ColorfulPolicy {

  private val Colorful = balticporter.tir.RuleScope.Only(Set("com.github.tommyettinger.colorful"))

  def core(coreManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path], reference: Option[Path] = None): PortManifest =
    coreManifest.extendedBy(
      PortManifest(
        name = "sge-colorful",
        governs = Set("com.github.tommyettinger.colorful"),
        packageRenames = Map("com.github.tommyettinger.colorful" -> "sge.colorful"),
        surface = List(
          new balticporter.transform.BeanPropertyTransform(scope = Colorful, derive = true),
          new balticporter.transform.NullaryArityTransform(scope = Colorful, derive = true),
          new balticporter.transform.NullabilityTransform(scope = Colorful, deriveMembers = true),
          // LAST, deliberately: reads what the BASE actually emitted
          balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0")
        ),
        parity = reference.map(r => ParityRef(roots = List(r))),
        frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/colorful-derived-policy.tsv")).filter(p => java.nio.file.Files.exists(p))
      )
    )
}
