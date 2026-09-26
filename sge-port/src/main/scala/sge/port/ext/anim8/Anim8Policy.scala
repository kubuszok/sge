package sge.port.ext.anim8

import balticporter.core.PortManifest

import java.nio.file.Path

/** anim8-gdx's policy (`src/main/java`, 16 types: GIF/PNG8/APNG writers and the dithering/palette-reduction machinery) — a DEPENDENT of sge-core: anim8 reads and writes libGDX's `Pixmap`,
  * `FileHandle` and `Array`, so libGDX's policy is inherited from the core manifest and never restated.
  */
object Anim8Policy {

  private val Anim8 = balticporter.tir.RuleScope.Only(Set("com.github.tommyettinger.anim8"))

  /** @param coreManifest
    *   the sge-core manifest (lls -> sge-core chain)
    * @param reportRoots
    *   directories where the base ports' report trees live (port-map.tsv discovery)
    */
  def core(coreManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path]): PortManifest =
    coreManifest.extendedBy(
      PortManifest(
        name = "sge-anim8",
        governs = Set("com.github.tommyettinger.anim8"),
        packageRenames = Map("com.github.tommyettinger.anim8" -> "sge.anim8"),
        // sge's `DitherAlgorithm` is top level beside `Dithered`
        flattenNestedTypes = Set("com.github.tommyettinger.anim8.Dithered$DitherAlgorithm"),
        surface = List(
          new balticporter.transform.BeanPropertyTransform(scope = Anim8, derive = true),
          new balticporter.transform.NullaryArityTransform(scope = Anim8, derive = true),
          // the members the hand port typed `| Null`, as sge's `Nullable` (the target the base states)
          new balticporter.transform.NullabilityTransform(scope = Anim8, deriveMembers = true),
          // LAST, deliberately: reads what the BASE actually emitted
          balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0")
        ),
        // derived from the hand port this generation replaced, while it existed
        frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/anim8-derived-policy.tsv"))
      )
    )
}
