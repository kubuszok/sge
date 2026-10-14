package sge.port.ext.tools

import balticporter.core.{ ParityRef, PortManifest }

import java.nio.file.Path

/** gdx-tools' policy (the texture packer and the `FileProcessor` it extends, from libGDX's `extensions/gdx-tools/src`) — a DEPENDENT of sge-core: the packer reads libGDX's `Array`, `Json` and
  * `TextureAtlas` data, so libGDX's policy is inherited from the core manifest and never restated. A desktop tool over `java.awt` and `javax.imageio`: JVM only.
  */
object ToolsPolicy {

  private val Tools = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx.tools"))

  private val FileHandleOfFile = "com.badlogic.gdx.files.FileHandle#<init>(File)"
  private val FileHandleOfName = "com.badlogic.gdx.files.FileHandle#<init>(String)"
  private val MaxRectsPack     = "com.badlogic.gdx.tools.texturepacker.MaxRectsPacker#pack(Array)"
  private val GridPack         = "com.badlogic.gdx.tools.texturepacker.GridPacker#pack(Array)"

  private val packWithoutProgress =
    "{ val silent = new sge.tools.texturepacker.TexturePacker.ProgressListener { def progress(progress: scala.Float): scala.Unit = () }; silent.start(1); this.pack(silent, inputRects) }"

  private val JsonFromReader = "com.badlogic.gdx.utils.Json#fromJson(Class,Reader)"
  private val JsonReadFields = "com.badlogic.gdx.utils.Json#readFields(Object,JsonValue)"

  /** java's `fromJson(Settings.class, reader)`: parse, `null` for an empty document, else a fresh Settings read field by field */
  private val settingsFromReader: String =
    """def settingsFrom(reader: java.io.Reader): sge.tools.texturepacker.TexturePacker.Settings = {
      |  val root: sge.utils.JsonValue = new sge.utils.JsonReader().parse(reader)
      |  if (root == null) null else readSettings(new sge.tools.texturepacker.TexturePacker.Settings(), root)
      |}""".stripMargin

  /** the packer's settings, read the way java's reflective `Json.readFields` reads them (sge's Json does not reflect, ISS-871): every Settings field by its name, an unknown name refused as java's
    * `SerializationException` refuses it. The hand port's reader, at java's refusals.
    */
  private val settingsReader: String =
    """@scala.annotation.nowarn("msg=deprecated") // `orNull`: a JSON string or null is java's String slot
      |def readSettings(settings: sge.tools.texturepacker.TexturePacker.Settings, root: sge.utils.JsonValue): sge.tools.texturepacker.TexturePacker.Settings = {
      |  var v: sge.utils.JsonValue = root.child.orNull
      |  while (v != null) {
      |    v.name.orNull match {
      |      case "pot"                   => settings.pot = v.asBoolean
      |      case "multipleOfFour"        => settings.multipleOfFour = v.asBoolean
      |      case "paddingX"              => settings.paddingX = v.asInt
      |      case "paddingY"              => settings.paddingY = v.asInt
      |      case "edgePadding"           => settings.edgePadding = v.asBoolean
      |      case "duplicatePadding"      => settings.duplicatePadding = v.asBoolean
      |      case "rotation"              => settings.rotation = v.asBoolean
      |      case "minWidth"              => settings.minWidth = v.asInt
      |      case "minHeight"             => settings.minHeight = v.asInt
      |      case "maxWidth"              => settings.maxWidth = v.asInt
      |      case "maxHeight"             => settings.maxHeight = v.asInt
      |      case "square"                => settings.square = v.asBoolean
      |      case "stripWhitespaceX"      => settings.stripWhitespaceX = v.asBoolean
      |      case "stripWhitespaceY"      => settings.stripWhitespaceY = v.asBoolean
      |      case "alphaThreshold"        => settings.alphaThreshold = v.asInt
      |      case "filterMin"             => settings.filterMin = sge.graphics.Texture.TextureFilter.valueOf(v.asString.orNull)
      |      case "filterMag"             => settings.filterMag = sge.graphics.Texture.TextureFilter.valueOf(v.asString.orNull)
      |      case "wrapX"                 => settings.wrapX = sge.graphics.Texture.TextureWrap.valueOf(v.asString.orNull)
      |      case "wrapY"                 => settings.wrapY = sge.graphics.Texture.TextureWrap.valueOf(v.asString.orNull)
      |      case "format"                => settings.format = sge.graphics.Pixmap.Format.valueOf(v.asString.orNull)
      |      case "alias"                 => settings.alias = v.asBoolean
      |      case "outputFormat"          => settings.outputFormat = v.asString.orNull
      |      case "jpegQuality"           => settings.jpegQuality = v.asFloat
      |      case "ignoreBlankImages"     => settings.ignoreBlankImages = v.asBoolean
      |      case "fast"                  => settings.fast = v.asBoolean
      |      case "debug"                 => settings.debug = v.asBoolean
      |      case "silent"                => settings.silent = v.asBoolean
      |      case "combineSubdirectories" => settings.combineSubdirectories = v.asBoolean
      |      case "ignore"                => settings.ignore = v.asBoolean
      |      case "flattenPaths"          => settings.flattenPaths = v.asBoolean
      |      case "premultiplyAlpha"      => settings.premultiplyAlpha = v.asBoolean
      |      case "useIndexes"            => settings.useIndexes = v.asBoolean
      |      case "bleed"                 => settings.bleed = v.asBoolean
      |      case "bleedIterations"       => settings.bleedIterations = v.asInt
      |      case "limitMemory"           => settings.limitMemory = v.asBoolean
      |      case "grid"                  => settings.grid = v.asBoolean
      |      case "scale"                 => settings.scale = v.asFloatArray()
      |      case "scaleSuffix"           => settings.scaleSuffix = v.asStringArray()
      |      case "scaleResampling"       => settings.scaleResampling = v.asStringArray().map(sge.tools.texturepacker.TexturePacker.Resampling.valueOf)
      |      case "atlasExtension"        => settings.atlasExtension = v.asString.orNull
      |      case "prettyPrint"           => settings.prettyPrint = v.asBoolean
      |      case "legacyOutput"          => settings.legacyOutput = v.asBoolean
      |      case other                   => throw sge.utils.SgeError.SerializationError("Field not found: " + other + " (" + classOf[sge.tools.texturepacker.TexturePacker.Settings].getName + ")")
      |    }
      |    v = v.next.orNull
      |  }
      |  settings
      |}""".stripMargin

  /** the texture packer and its file processor; the packer's two upstream demo mains (`TexturePackerTest`, `TexturePackerUpscaleTest`) open an lwjgl window and are not part of the tool */
  def include(f: String): Boolean =
    f == "com/badlogic/gdx/tools/FileProcessor.java" ||
      (f.startsWith("com/badlogic/gdx/tools/texturepacker/") && !f.endsWith("Test.java"))

  def core(coreManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path], reference: Option[Path] = None): PortManifest =
    coreManifest.extendedBy(
      PortManifest(
        name = "sge-tools",
        governs = Set("com.badlogic.gdx.tools"),
        // a desktop tool over `java.awt` and `javax.imageio`, which neither Scala.js nor Scala Native ships
        targets = Set(balticporter.catalog.Platform.Jvm),
        surface = List(
          // the packer builds FileHandles over local paths (java's default `FileType.Absolute`, whose external
          // storage path is never read): the context-free factory, so the desktop tool takes no `Sge` context,
          // like the hand port's. A method body cannot do this: the context clause is threaded from the
          // java body before a body is substituted
          new balticporter.transform.CallSiteSubstitutionTransform(
            List(
              balticporter.transform.CallSiteSubstitutionTransform.Entry(FileHandleOfFile, "sge.files.FileHandle.apply({arg0})", Tools),
              balticporter.transform.CallSiteSubstitutionTransform.Entry(FileHandleOfName, "sge.files.FileHandle.apply({arg0})", Tools),
              // the CLI's settings file and every pack.json: java reads them by reflection, sge through the
              // typed reader below (by user ruling, 2026-10-15)
              balticporter.transform.CallSiteSubstitutionTransform.Entry(JsonFromReader, "sge.tools.texturepacker.TexturePacker.settingsFrom({arg1})", Tools),
              balticporter.transform.CallSiteSubstitutionTransform.Entry(JsonReadFields, "sge.tools.texturepacker.TexturePacker.readSettings({arg0}, {arg1})", Tools)
            )
          ),
          new balticporter.transform.AddMembersTransform(
            Map(
              "com.badlogic.gdx.tools.texturepacker.TexturePacker" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "readSettings",
                  2,
                  settingsReader,
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.tools.texturepacker.TexturePacker#readSettings"),
                  Some("reads the packer's settings without reflection, as the hand port did"),
                  static = true
                ),
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "settingsFrom",
                  1,
                  settingsFromReader,
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.tools.texturepacker.TexturePacker#settingsFrom"),
                  Some("java's fromJson(Settings.class, reader) over the typed reader"),
                  static = true
                )
              )
            )
          ),
          // java's one-argument `pack` hands `null` to the two-argument one, which reads `progress.count`
          // unguarded: an NPE on every call (libGDX's own bug, which the hand port fixed). A started listener that
          // reports nowhere and never cancels is what the call means — a deliberate divergence by user ruling
          // (2026-10-15), ISS-914
          new balticporter.transform.MethodBodyTransform(Map(MaxRectsPack -> packWithoutProgress, GridPack -> packWithoutProgress)),
          new balticporter.transform.BeanPropertyTransform(scope = Tools, derive = true),
          new balticporter.transform.NullaryArityTransform(scope = Tools, derive = true),
          new balticporter.transform.NullabilityTransform(scope = Tools, deriveMembers = true),
          balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0")
        ),
        parity = reference.map(r => ParityRef(roots = List(r))),
        frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/tools-derived-policy.tsv")).filter(p => java.nio.file.Files.exists(p))
      )
    )
}
