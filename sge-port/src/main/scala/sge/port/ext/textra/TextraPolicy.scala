package sge.port.ext.textra

import balticporter.core.{ ParityRef, PortManifest }

import java.nio.file.Path

/** TextraTypist's policy (`src/main/java`, 92 types: the `Font`/`Layout` text engine, the typing labels and their effects, the `FWSkin` font-aware skin) — a DEPENDENT of sge-core and of the RegExodus
  * port: it extends scene2d and libGDX's font types and matches with RegExodus, so both bases' policy is inherited and never restated.
  */
object TextraPolicy {

  private val Textra = balticporter.tir.RuleScope.Only(Set("com.github.tommyettinger.textra"))

  /** @param regexodusManifest
    *   the RegExodus port's manifest (a base of its own)
    * @param coreManifest
    *   the sge-core manifest (lls -> sge-core chain)
    */
  def core(regexodusManifest: PortManifest, coreManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path], reference: Option[Path] = None): PortManifest =
    regexodusManifest.extendedBy(
      coreManifest.extendedBy(
        PortManifest(
          name = "sge-textra",
          governs = Set("com.github.tommyettinger.textra"),
          packageRenames = Map("com.github.tommyettinger.textra" -> "sge.textra"),
          // `FWSkin` re-runs libGDX's reflective `Json` skin loader and writes `Skin`'s atlas; sge's `Skin` reads its JSON with its own
          // reader and keeps the atlas private, so the hand port's replacement (a second pass after `Skin.load`) stands in its place
          dropTypes = Set("com.github.tommyettinger.textra.FWSkin"),
          inject = List(sgeRoot.resolve("sge-port/overrides-ext/textra")),
          surface = List(
            // TextraTypist's `java.util` lists and maps are sge's `scala.collection.mutable` ones, as in the hand port
            new balticporter.transform.CollectionsTransform(scope = Textra),
            new balticporter.transform.MutableParamsTransform,
            // libGDX's `FloatArray#mul`, which lls's `DynamicArray` does not carry: java's own loop at the call
            new balticporter.transform.CallSiteSubstitutionTransform(
              List(
                balticporter.transform.CallSiteSubstitutionTransform.Entry(
                  "com.badlogic.gdx.utils.FloatArray#mul(int,float)",
                  "{{ val bpA = {recv}; val bpI = {arg0}; if (bpI >= bpA.size) throw new java.lang.IndexOutOfBoundsException(\"index can't be >= size: \" + bpI + \" >= \" + bpA.size); bpA(bpI) = bpA(bpI) * {arg1} }}",
                  Textra
                ),
                // libGDX's `Skin#getAll` hands back its map or null; sge's answers a `Nullable` scala map. Java dereferences the
                // result at once, so an absent type fails here as java's would, and the entries arrive in an lls `ObjectMap`
                balticporter.transform.CallSiteSubstitutionTransform.Entry(
                  "com.badlogic.gdx.scenes.scene2d.ui.Skin#getAll(Class)",
                  "{{ def bpCopy[V <: AnyRef](m: scala.collection.mutable.Map[java.lang.String, V]): lowlevel.util.ObjectMap[java.lang.String, V] = {{ val o = new lowlevel.util.ObjectMap[java.lang.String, V](); m.foreach((k, v) => o.put(k, lowlevel.Nullable(v))); o }}; bpCopy({recv}.getAll({arg0}).get) }}",
                  Textra
                )
              )
            ),
            // `LinkEffect` opens its URI through the context, and is built by a lambda in `TypingConfig`'s class initialiser, which
            // has none: as in the hand port, a `TextraLabel` keeps the context it was built with and the effect reads its label's
            new balticporter.transform.GlobalsToImplicitsTransform(
              extensions = List(
                balticporter.transform.ContextHolderExtension(
                  holder = "com.badlogic.gdx.Gdx",
                  retain = Map("com.github.tommyettinger.textra.TextraLabel" -> "sgeContext"),
                  selfSupplied = Map("com.github.tommyettinger.textra.effects.LinkEffect" -> "this.label.sgeContext")
                )
              )
            ),
            // `TypingListBox` constructs its parent's element array, so it takes the parent's witness
            new balticporter.transform.GlobalsToImplicitsTransform(
              requiredGivens = balticporter.transform.ElementWitnessTransform.constructorGivens(
                Map("com.github.tommyettinger.textra.TypingListBox" -> List(0)),
                lowlevel.port.LlsPolicy.Witness
              )
            ),
            new balticporter.transform.BeanPropertyTransform(scope = Textra, derive = true),
            new balticporter.transform.NullaryArityTransform(scope = Textra, derive = true),
            new balticporter.transform.NullabilityTransform(scope = Textra, deriveMembers = true),
            // LAST, deliberately: reads what the BASES actually emitted
            balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0", "sge-regexodus")
          ),
          parity = reference.map(r => ParityRef(roots = List(r))),
          frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/textra-derived-policy.tsv")).filter(p => java.nio.file.Files.exists(p))
        )
      )
    )
}
