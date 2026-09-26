package sge.port.ext.noise4j

import balticporter.core.PortManifest

import java.nio.file.Path

/** noise4j's policy (`src`, 12 types, dependency-free procedural map generation). A BASE port: noise4j imports nothing outside `java.lang`/`java.util`, so it resolves against no other module and
  * inherits no policy. `java.util` stays as java wrote it (no `collections` phase): the generators mutate through `Iterator.remove()` and read `List.set`'s previous element, which no Scala collection
  * has.
  */
object Noise4jPolicy {

  def core(sgeRoot: Path): PortManifest =
    PortManifest(
      name = "sge-noise",
      governs = Set("com.github.czyzby.noise4j"),
      // sge collapses noise4j's `array` and `map` packages onto `sge.noise` (`Grid` beside `Int2dArray`);
      // longest prefix wins, so `…map.generator` lands in `sge.noise.generator`.
      packageRenames = Map(
        "com.github.czyzby.noise4j.map" -> "sge.noise",
        "com.github.czyzby.noise4j.array" -> "sge.noise"
      ),
      surface = List(
        // `DungeonGenerator.normalizeSize` reassigns its own parameter
        new balticporter.transform.MutableParamsTransform,
        // sge's API reads the generators' settings as properties (`gen.iterationsAmount = 4`) and
        // `grid.getArray` without parentheses
        new balticporter.transform.BeanPropertyTransform(
          // the one pair whose getter and setter names differ (`isInitiating`/`setInitiate`)
          pairs = Map("com.github.czyzby.noise4j.map.generator.cellular.CellularAutomataGenerator#initiate" -> "isInitiating/setInitiate"),
          scope = balticporter.tir.RuleScope.Only(Set("com.github.czyzby.noise4j")),
          derive = true
        ),
        new balticporter.transform.NullaryArityTransform(
          scope = balticporter.tir.RuleScope.Only(Set("com.github.czyzby.noise4j")),
          derive = true
        )
      ),
      // derived from the hand port this generation replaced, while it existed
      frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/noise-derived-policy.tsv"))
    )
}
