package sge.port.ext.screens

import balticporter.core.{ ParityRef, PortManifest }

import java.nio.file.Path

/** libgdx-screenmanager's policy (`src/main/java`, 22 types: the screen stack, the transition queue and its transitions) — a DEPENDENT of guacamole (generated into the same module), itself a
  * dependent of sge-core: libGDX's and guacamole's policies are inherited from guacamole's manifest and never restated.
  */
object ScreensPolicy {

  private val Screens = balticporter.tir.RuleScope.Only(Set("de.eskalon.commons"))

  private def hook(name: String, arity: Int, source: String): balticporter.transform.AddMembersTransform.MemberSpec =
    balticporter.transform.AddMembersTransform.MemberSpec(
      name,
      arity,
      source,
      balticporter.tir.Reason.Configured("add-members", s"de.eskalon.commons.screen#$name"),
      Some("sge's screen-manager API, as the hand port spelled it")
    )

  private val ScreenManagerRender = "de.eskalon.commons.screen.ScreenManager#render"

  /** java's `render`, with its two clears and two screen-to-texture renders going through the hooks */
  private val renderBody: String =
    """{
      |  sge.guacamole.Preconditions.checkState(this.initialized, lowlevel.Nullable("The screen manager has to be initalized first!"))
      |  if (this.transition.isEmpty) {
      |    if (!this.transitionQueue.isEmpty()) {
      |      val nextTransition: sge.guacamole.tuple.Pair[sge.screen.utils.Supplier[T], sge.screen.utils.Supplier[S]] = this.transitionQueue.poll()
      |      val tmp: sge.screen.ManagedScreen = nextTransition.y.get.get()
      |      if (tmp eq this.currScreen.orNull) {
      |        if (sge.guacamole.gdx.log.LoggerService.isDebugEnabled()) {
      |          this.LOG.debug("Screens cannot be pushed twice; the second call to push '%s' was ignored", tmp.getClass().getSimpleName())
      |        }
      |        this.render(delta)
      |      } else {
      |        this.lastScreen$field = this.currScreen
      |        this.currScreen = lowlevel.Nullable(tmp)
      |        this.transition = lowlevel.Nullable(if (nextTransition.x.isEmpty) null.asInstanceOf[T] else nextTransition.x.get.get())
      |        this.gameInputMultiplexer.removeProcessors(this.currentProcessors)
      |        this.initializeScreen(this.currScreen.orNull)
      |        if (!this.transition.isEmpty) {
      |          this.initializeTransition(this.transition.orNull)
      |        } else {
      |          this.finalizeScreen(this.lastScreen$field.orNull)
      |          this.lastScreen$field = lowlevel.Nullable.empty
      |          this.currentProcessors = new lowlevel.util.DynamicArray[sge.InputProcessor](this.currScreen.get.inputProcessors)
      |          this.gameInputMultiplexer.addProcessors(this.currentProcessors)
      |        }
      |        this.render(delta)
      |      }
      |    } else {
      |      this.clearScreen(this.currScreen.get)
      |      this.currScreen.get.render(delta)
      |    }
      |  } else {
      |    if (!this.transition.get.isDone) {
      |      this.clearTransition(this.transition.get)
      |      this.transition.get.render(delta, this.renderLastScreenToTexture(delta), this.renderCurrScreenToTexture(delta))
      |    } else {
      |      this.finalizeTransition(this.transition.orNull)
      |      this.transition = lowlevel.Nullable.empty[T]
      |      this.finalizeScreen(this.lastScreen$field.orNull)
      |      this.lastScreen$field = lowlevel.Nullable.empty
      |      this.currentProcessors = new lowlevel.util.DynamicArray[sge.InputProcessor](this.currScreen.get.inputProcessors)
      |      this.gameInputMultiplexer.addProcessors(this.currentProcessors)
      |      this.render(delta)
      |    }
      |  }
      |}""".stripMargin

  /** @param guacamoleManifest
    *   guacamole's manifest (lls -> sge-core -> guacamole chain)
    */
  def core(guacamoleManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path], reference: Option[Path] = None): PortManifest =
    guacamoleManifest.extendedBy(
      PortManifest(
        name = "sge-screens",
        governs = Set("de.eskalon.commons"),
        // sge keeps the library's game class beside its screens, and its utilities below them
        packageRenames = Map(
          "de.eskalon.commons.screen" -> "sge.screen",
          "de.eskalon.commons.core" -> "sge.screen",
          "de.eskalon.commons.utils" -> "sge.screen.utils"
        ),
        surface = List(
          // sge's four test seams on `ScreenManager` (the java test mocks `ScreenUtils.clear` and
          // `ScreenFboUtils.screenToTexture` instead, which sge's suites cannot): `render` calls them where java
          // calls the two utilities, each hook doing exactly that call. By user ruling, with `initBuffers` plain
          // `protected` below so a subclass can replace it.
          new balticporter.transform.MethodBodyTransform(Map(ScreenManagerRender -> renderBody)),
          new balticporter.transform.AddMembersTransform(
            Map(
              "de.eskalon.commons.screen.ScreenManager" -> List(
                hook("clearScreen", 1, "protected def clearScreen(screen: sge.screen.ManagedScreen): scala.Unit = screen.clearColor.foreach(c => sge.utils.ScreenUtils.clear(c, true))"),
                hook("clearTransition", 1, "protected def clearTransition(trans: T): scala.Unit = trans.clearColor.foreach(c => sge.utils.ScreenUtils.clear(c, true))"),
                hook(
                  "renderLastScreenToTexture",
                  1,
                  "@scala.annotation.nowarn(\"msg=deprecated\") // java hands the field's value through, null included\n  protected def renderLastScreenToTexture(delta: sge.utils.Seconds): sge.graphics.g2d.TextureRegion = sge.screen.utils.ScreenFboUtils.screenToTexture(this.lastScreen" + "$field.orNull, this.lastFBO, delta)"
                ),
                hook(
                  "renderCurrScreenToTexture",
                  1,
                  "@scala.annotation.nowarn(\"msg=deprecated\") // java hands the field's value through, null included\n  protected def renderCurrScreenToTexture(delta: sge.utils.Seconds): sge.graphics.g2d.TextureRegion = sge.screen.utils.ScreenFboUtils.screenToTexture(this.currScreen.orNull, this.currFBO, delta)"
                ),
                // sge's name for java's `getLastScreen()` (the hand port's API)
                hook("lastScreenOption", 0, "def lastScreenOption: lowlevel.Nullable[S] = this.lastScreen")
              ),
              // java's `getProgram()` under its java name beside the `program` property (the hand port's API)
              "de.eskalon.commons.screen.transition.impl.ShaderTransition" -> List(
                hook("getProgram", 0, "def getProgram: sge.graphics.glutils.ShaderProgram = this.program")
              )
            )
          ),
          new balticporter.transform.VisibilityTransform(narrow = Set("de.eskalon.commons.screen.ScreenManager#initBuffers")),
          new balticporter.transform.BeanPropertyTransform(scope = Screens, derive = true),
          new balticporter.transform.NullaryArityTransform(scope = Screens, derive = true),
          // screenmanager's own nullability annotation, consumed as guacamole's is
          new balticporter.transform.NullabilityTransform(
            target = balticporter.transform.NullabilityTransform.Target.Named("lowlevel.Nullable"),
            annotations = Set("org.jspecify.annotations.Nullable"),
            scope = Screens,
            deriveMembers = true
          ),
          balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0", "sge-guacamole")
        ),
        parity = reference.map(r => ParityRef(roots = List(r))),
        frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/screens-derived-policy.tsv")).filter(p => java.nio.file.Files.exists(p))
      )
    )
}
