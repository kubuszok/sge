package sge.port.ext.jbump

import balticporter.core.PortManifest

import java.nio.file.Path

/** jbump's policy (`jbump/src`, 19 types, a dependency-free 2D AABB collision library). A BASE port: jbump imports nothing of libGDX's (its `util` package vendors copies of libGDX's primitive arrays
  * and `IntIntMap`, ported as jbump's own), and resolves against no other module.
  */
object JbumpPolicy {

  private val Jbump = balticporter.tir.RuleScope.Only(Set("com.dongbat.jbump"))

  def core(sgeRoot: Path): PortManifest =
    PortManifest(
      name = "sge-jbump",
      governs = Set("com.dongbat.jbump"),
      packageRenames = Map("com.dongbat.jbump" -> "sge.jbump"),
      surface = List(
        // jbump's `java.util` collections are sge's `scala.collection.mutable` ones (`queryRect` fills an `ArrayBuffer`)
        new balticporter.transform.CollectionsTransform(scope = Jbump),
        new balticporter.transform.MutableParamsTransform,
        new balticporter.transform.BeanPropertyTransform(scope = Jbump, derive = true),
        new balticporter.transform.NullaryArityTransform(scope = Jbump, derive = true),
        new balticporter.transform.NullabilityTransform(
          target = balticporter.transform.NullabilityTransform.Target.Named("lowlevel.Nullable"),
          scope = Jbump,
          deriveMembers = true
        )
      ),
      // derived from the hand port this generation replaced, while it existed
      frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/jbump-derived-policy.tsv"))
    )
}
