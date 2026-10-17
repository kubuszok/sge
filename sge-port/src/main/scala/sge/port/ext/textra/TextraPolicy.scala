package sge.port.ext.textra

import balticporter.core.{ ParityRef, PortManifest }

import java.nio.file.Path

/** TextraTypist's policy (`src/main/java`, 92 types: the `Font`/`Layout` text engine, the typing labels and their effects, the `FWSkin` font-aware skin) — a DEPENDENT of sge-core and of the RegExodus
  * port: it extends scene2d and libGDX's font types and matches with RegExodus, so both bases' policy is inherited and never restated.
  */
object TextraPolicy {

  private val Textra = balticporter.tir.RuleScope.Only(Set("com.github.tommyettinger.textra"))

  private val T = "com.github.tommyettinger.textra"

  private def added(owner: String, name: String, arity: Int, source: String, why: String, static: Boolean = false) =
    owner -> balticporter.transform.AddMembersTransform.MemberSpec(
      name,
      arity,
      source,
      balticporter.tir.Reason.Configured("add-members", s"$owner#$name/$arity"),
      Some(why),
      static
    )

  /** The hand port's API beside java's: the accessors it kept under java's names next to the property they read, the constructors and overloads it added, and its spelling of a replacer. */
  private val HandPortMembers: Map[String, List[balticporter.transform.AddMembersTransform.MemberSpec]] = List(
    added(
      s"$T.Font$$GlyphRegion",
      "this",
      3,
      "def this(offsetX: scala.Float, offsetY: scala.Float, xAdvance: scala.Float) = {\n    this()\n    this.offsetX = offsetX\n    this.offsetY = offsetY\n    this.xAdvance = xAdvance\n  }",
      "the hand port's glyph with no texture region, given its offsets and advance"
    ),
    added(
      s"$T.Font",
      "getDistanceField",
      0,
      "def getDistanceField: sge.textra.Font.DistanceFieldType = this.distanceField",
      "the hand port keeps java's accessor beside the property"
    ),
    added(
      s"$T.TextraLabel",
      "getFont",
      0,
      "def getFont: sge.textra.Font = this.font",
      "the hand port keeps java's font accessor beside the property"
    ),
    added(
      s"$T.TextraLabel",
      "getWidth",
      0,
      "def getWidth: scala.Float = this.width",
      "the hand port keeps java's width accessor beside the property"
    ),
    added(
      s"$T.TextraLabel",
      "getHeight",
      0,
      "def getHeight: scala.Float = this.height",
      "the hand port keeps java's height accessor beside the property"
    ),
    added(
      s"$T.TextraTooltip",
      "getContainer",
      0,
      "def getContainer: sge.scenes.scene2d.ui.Container[sge.textra.TextraLabel] = this.container",
      "the hand port keeps java's container accessor"
    ),
    added(
      s"$T.TextraTooltip",
      "getActor",
      0,
      "def getActor: sge.textra.TextraLabel = this.container.actor.get",
      "the hand port keeps java's actor accessor; the label is installed by every constructor"
    ),
    added(
      s"$T.TypingTooltip",
      "getContainer",
      0,
      "def getContainer: sge.scenes.scene2d.ui.Container[sge.textra.TypingLabel] = this.container",
      "the hand port keeps java's container accessor"
    ),
    added(
      s"$T.TypingTooltip",
      "getActor",
      0,
      "def getActor: sge.textra.TypingLabel = this.container.actor.get",
      "the hand port keeps java's actor accessor; the label is installed by every constructor"
    ),
    added(
      s"$T.TextraDialog",
      "getContentTable",
      0,
      "def getContentTable: sge.scenes.scene2d.ui.Table = this.contentTable",
      "the hand port keeps java's accessor beside the property"
    ),
    added(
      s"$T.TextraDialog",
      "getButtonTable",
      0,
      "def getButtonTable: sge.scenes.scene2d.ui.Table = this.buttonTable",
      "the hand port keeps java's accessor beside the property"
    ),
    added(
      s"$T.TypingDialog",
      "getContentTable",
      0,
      "def getContentTable: sge.scenes.scene2d.ui.Table = this.contentTable",
      "the hand port keeps java's accessor beside the property"
    ),
    added(
      s"$T.TypingDialog",
      "getButtonTable",
      0,
      "def getButtonTable: sge.scenes.scene2d.ui.Table = this.buttonTable",
      "the hand port keeps java's accessor beside the property"
    ),
    added(
      s"$T.TextraField",
      "getText",
      0,
      "def getText: java.lang.String = this.text",
      "the hand port keeps java's accessor beside the property"
    ),
    added(
      s"$T.TextraField",
      "getMessageText",
      0,
      "def getMessageText: lowlevel.Nullable[java.lang.String] = this.messageText",
      "the hand port keeps java's accessor beside the property"
    ),
    added(
      s"$T.TextraField",
      "getClipboard",
      0,
      "def getClipboard: lowlevel.Nullable[sge.utils.Clipboard] = this.clipboard",
      "the hand port reads the field's clipboard through java's accessor name"
    ),
    added(
      s"$T.utils.CaseInsensitiveIntMap",
      "size",
      0,
      "def size: scala.Int = this._size",
      "the hand port's entry count beside the public field"
    ),
    added(
      s"$T.Line",
      "appendTo",
      1,
      "def appendTo(sb: scala.collection.mutable.StringBuilder): scala.collection.mutable.StringBuilder = {\n    this.appendTo(sb.underlying)\n    sb\n  }",
      "the hand port appends to a scala builder; java's own appending, onto the builder it wraps"
    ),
    added(
      s"$T.utils.StringUtils",
      "appendUnsignedHex",
      2,
      "def appendUnsignedHex(sb: scala.collection.mutable.StringBuilder, number: scala.Int): scala.collection.mutable.StringBuilder = {\n    appendUnsignedHex(sb.underlying, number)\n    sb\n  }",
      "the hand port appends to a scala builder; java's own appending, onto the builder it wraps",
      static = true
    ),
    added(
      s"$T.utils.StringUtils",
      "appendUnsignedHex",
      2,
      "def appendUnsignedHex(sb: scala.collection.mutable.StringBuilder, number: scala.Long): scala.collection.mutable.StringBuilder = {\n    appendUnsignedHex(sb.underlying, number)\n    sb\n  }",
      "the hand port appends to a scala builder; java's own appending, onto the builder it wraps",
      static = true
    ),
    added(
      s"$T.EmojiProcessor",
      "replaceEmoji",
      2,
      "def replaceEmoji(text: java.lang.String, font: sge.textra.Font): java.lang.String = getReplacer(font).replace(text)",
      "the hand port's one call over java's replacer: every emoji the font can show becomes its private-use char",
      static = true
    )
  ).groupMap(_._1)(_._2)

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
          // the hand port keeps the LZMA codec beside the fonts that read it
          typeRenames = Map(s"$T.utils.LzmaUtils" -> s"$T.LzmaUtils"),
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
                  selfSupplied = Map("com.github.tommyettinger.textra.effects.LinkEffect" -> "this.label.sgeContext"),
                  // the hand port builds a `Font` (and a family of them) with no context: only the constructors that load
                  // files and the members that read the screen take it
                  attachMethod = Set(s"$T.Font")
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
            // java's `Layout#lines()` is the hand port's `lineCount`, which leaves `lines` to the line array; java's `TextraLabel#layout`
            // field is the hand port's `baseLayout`, apart from the widget's `layout()`
            new balticporter.transform.MemberRenameTransform(
              renames = Map(s"$T.Layout#lines()" -> "lineCount", s"$T.TextraLabel#layout:field" -> "baseLayout")
            ),
            new balticporter.transform.AddMembersTransform(HandPortMembers),
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
