package sge.port.ext.visui

import balticporter.core.{ ParityRef, PortManifest }

import java.nio.file.Path

/** VisUI's policy (`ui/src/main/java`, 162 types: a scene2d widget toolkit with its own skins, i18n bundles and file chooser) — a DEPENDENT of sge-core: every widget extends scene2d, so libGDX's
  * policy is inherited from the core manifest and never restated. The claim is the `ui` module only; `com.kotcrab.vis.usl` is a separate port.
  */
object VisUiPolicy {

  private val VisUi = balticporter.tir.RuleScope.Only(Set("com.kotcrab.vis.ui"))

  private val Pools  = "com.badlogic.gdx.utils.Pools"
  private val Shared = "sge.scenes.scene2d.Actor.POOLS"

  /** Bodies java wrote against libGDX facts sge answers differently. Keys are upstream names, bodies the port's final spelling. */
  private val TextGetters = Set("com.kotcrab.vis.ui.widget.VisImageTextButton#getText", "com.kotcrab.vis.ui.widget.MenuItem#getShortcut")

  private val Bodies: Map[String, String] = Map(
    // a copy of the label's text, read when asked, as the hand port's `String` getters answer
    "com.kotcrab.vis.ui.widget.VisImageTextButton#getText()" -> "{\n    this.label$field.text.toString\n  }",
    "com.kotcrab.vis.ui.widget.MenuItem#getShortcut()" -> "{\n    this.shortcutLabel.text.toString\n  }",
    // libGDX's Skin resolves VisUI's FQCN-keyed style sections by reflection; sge's Skin reads a closed registry, which
    // the hand-written `VisUISkinReaders` fills before any skin is parsed (as the hand port's `checkBeforeLoad` did)
    "com.kotcrab.vis.ui.VisUI#checkBeforeLoad()" ->
      """{
        |    sge.visui.VisUISkinReaders.register()
        |    if (VisUI.skin != null) {
        |      throw sge.utils.SgeError.InvalidInput("VisUI cannot be loaded twice")
        |    } else ()
        |    if ((!VisUI.skipGdxVersionCheck) && (!sge.Version.VERSION.equals(VisUI.TARGET_GDX_VERSION))) {
        |      sge.utils.Log.info("VisUI" + ": " + (((((("Warning, using invalid libGDX version.\n" + "You are using libGDX ") + sge.Version.VERSION) + " but you need ") + VisUI.TARGET_GDX_VERSION) + ". This may cause ") + "unexpected problems and runtime exceptions."))
        |    } else ()
        |  }""".stripMargin,
    // `Button`'s constructor reaches the override through `setStyle` before this class's field initialisers ran: java's
    // field is still `null` there and its `containerMenu == null` answers it; the wrapped field is a JVM null then, not
    // the empty value, so the guard asks both
    "com.kotcrab.vis.ui.widget.MenuItem#isOver()" ->
      """{
        |    if ((this.containerMenu.asInstanceOf[AnyRef] eq null) || this.containerMenu.isEmpty || this.containerMenu.get.getActiveItem.isEmpty) {
        |      super.isOver
        |    } else {
        |      this.containerMenu.get.getActiveItem.get eq this
        |    }
        |  }""".stripMargin
  )

  def core(coreManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path], reference: Option[Path] = None): PortManifest =
    coreManifest.extendedBy(
      PortManifest(
        name = "sge-visui",
        governs = Set("com.kotcrab.vis.ui"),
        packageRenames = Map("com.kotcrab.vis.ui" -> "sge.visui"),
        // the resources VisUI loads by classpath string (a rename moves symbols, never a literal): the i18n bundles `Locales` names,
        // the two skins `VisUI.SkinScale` names (each `.json` names its atlas and fonts, the atlas its png), the colour picker's shaders
        resources = List(
          balticporter.core.ResourceTree(
            sgeRoot.resolve("original-src/vis-ui/ui/src/main/resources"),
            List("ButtonBar", "ColorPicker", "Common", "Dialogs", "FileChooser", "TabbedPane").map(b => s"com/kotcrab/vis/ui/i18n/$b.properties") ++
              List("x1", "x2").flatMap(s => List("default.fnt", "font-small.fnt", "uiskin.atlas", "uiskin.json", "uiskin.png").map(f => s"com/kotcrab/vis/ui/skin/$s/$f")) ++
              List("checkerboard.frag", "default.vert", "hsv.frag", "palette.frag", "rgb.frag", "verticalBar.frag").map(f => s"com/kotcrab/vis/ui/widget/color/internal/$f")
          )
        ),
        surface = List(
          // VisUI's `java.util` lists and maps are sge's `scala.collection.mutable` ones, as in the hand port
          new balticporter.transform.CollectionsTransform(scope = VisUi),
          new balticporter.transform.MutableParamsTransform,
          // the context the i18n enums, `SkinScale` and `TableLayout` read: `VisUI.load` caches it (the hand port's `sgeInstance`), and an
          // enum, whose constructor no caller can pass a context to, reads it from there
          new balticporter.transform.GlobalsToImplicitsTransform(
            extensions = List(
              balticporter.transform.ContextHolderExtension(
                holder = "com.badlogic.gdx.Gdx",
                cache = Map("com.kotcrab.vis.ui.VisUI" -> "sgeInstance"),
                selfSupplied = List(
                  "com.kotcrab.vis.ui.VisUI$SkinScale",
                  "com.kotcrab.vis.ui.widget.ButtonBar$ButtonType",
                  "com.kotcrab.vis.ui.building.utilities.layouts.TableLayout",
                  "com.kotcrab.vis.ui.Locales$CommonText",
                  "com.kotcrab.vis.ui.util.dialog.Dialogs$Text",
                  "com.kotcrab.vis.ui.widget.color.internal.ColorPickerText",
                  "com.kotcrab.vis.ui.widget.file.internal.FileChooserText",
                  "com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane$Text"
                ).map(_ -> "sge.visui.VisUI.sgeInstance").toMap
              )
            )
          ),
          // VisUI predates libGDX's `Actor.POOLS`: its `Pools` calls go to that shared manager, which registers `ChangeEvent` and
          // `GlyphLayout` as java's reflective `Pools` would have created them on demand
          new balticporter.transform.CallSiteSubstitutionTransform(
            List(
              balticporter.transform.CallSiteSubstitutionTransform.Entry(s"$Pools#obtain(Class)", s"$Shared.obtain(using scala.reflect.ClassTag({arg0}))", VisUi),
              balticporter.transform.CallSiteSubstitutionTransform.Entry(s"$Pools#free(Object)", s"$Shared.free({arg0})", VisUi),
              balticporter.transform.CallSiteSubstitutionTransform.Entry(s"$Pools#get(Class)", s"$Shared.getPool({arg0})", VisUi),
              // VisUI predates libGDX's `OnscreenKeyboard.show(TextField)`/`close()`: `show(visible)` is one or the other, as in the hand
              // port. A `VisTextField` is no libGDX `TextField`, so `show` is handed none; the default keyboard never reads it
              balticporter.transform.CallSiteSubstitutionTransform.Entry(
                "com.badlogic.gdx.scenes.scene2d.ui.TextField$OnscreenKeyboard#show(TextField)",
                "(if ({arg0}.asInstanceOf[scala.Boolean]) {recv}.show(null) else {recv}.close())",
                balticporter.tir.RuleScope.Only(Set("com.kotcrab.vis.ui.widget.VisTextField"))
              )
            )
          ),
          // the list adapters construct their parent's element array, so they take its witness as the parent does
          new balticporter.transform.GlobalsToImplicitsTransform(
            requiredGivens = balticporter.transform.ElementWitnessTransform.constructorGivens(
              List("ArrayAdapter", "ArrayListAdapter", "SimpleListAdapter").map(c => s"com.kotcrab.vis.ui.util.adapter.$c" -> List(0)).toMap,
              lowlevel.port.LlsPolicy.Witness
            )
          ),
          // `FileHistoryCallback#getStage` is answered in java by `Actor#getStage`, which sge types `Nullable`: as in the hand port, the
          // callback asks `getHistoryStage` and the file chooser answers it with its stage
          new balticporter.transform.MemberRenameTransform(
            renames = Map(
              "com.kotcrab.vis.ui.widget.file.internal.FileHistoryManager$FileHistoryCallback#getStage" -> "getHistoryStage",
              // libGDX's `Disposable#dispose` is sge's `close`, and `ColorPicker` is both a `VisWindow` (whose `close()` fades the
              // window out) and a `Disposable`: the window's member moves, so disposal keeps java's meaning. A decided divergence
              // from the hand port, which dropped `Disposable` from `ColorPicker` instead
              "com.kotcrab.vis.ui.widget.VisWindow#close" -> "closeWindow",
              // the field `setUseEquals` writes, under its setter's name as in `GreaterThanValidator` and the hand port (`equals`
              // is `Any#equals` in scala): the constructor's `useEquals` then fills it as the property itself
              "com.kotcrab.vis.ui.util.Validators$LesserThanValidator#equals" -> "useEquals"
            ),
            // the reference's constructor parameter names and underscore fields
            derive = true
          ),
          new balticporter.transform.AddMembersTransform(
            Map(
              "com.kotcrab.vis.ui.widget.file.FileChooser" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "historyStage",
                  0,
                  "def historyStage: sge.scenes.scene2d.Stage = this.stage.orNull",
                  balticporter.tir.Reason.Configured("add-members", "com.kotcrab.vis.ui.widget.file.FileChooser#historyStage"),
                  Some("java's `getStage()`, the callback's answer, under the renamed callback member")
                )
              )
            )
          ),
          // the two label-text getters answer a `String`, as in the hand port: java's `CharSequence` is the label's live buffer
          new balticporter.transform.TypeRedirectTransform(
            redirects = Map("java.lang.CharSequence" -> "java.lang.String"),
            scopes = Map("java.lang.CharSequence" -> balticporter.tir.RuleScope.Only(TextGetters))
          ),
          new balticporter.transform.MethodBodyTransform(Bodies),
          new balticporter.transform.BeanPropertyTransform(scope = VisUi, derive = true),
          new balticporter.transform.NullaryArityTransform(scope = VisUi, derive = true),
          new balticporter.transform.NullabilityTransform(scope = VisUi, deriveMembers = true),
          // LAST, deliberately: reads what the BASE actually emitted
          balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0")
        ),
        parity = reference.map(r => ParityRef(roots = List(r))),
        frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/visui-derived-policy.tsv")).filter(p => java.nio.file.Files.exists(p))
      )
    )
}
