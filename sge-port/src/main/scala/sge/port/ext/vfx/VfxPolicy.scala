package sge.port.ext.vfx

import balticporter.core.{ ParityRef, PortManifest }

import java.nio.file.Path

/** gdx-vfx's policy (`gdx-vfx/core/src` and `gdx-vfx/effects/src`, 44 types: the frame-buffer ping-pong machinery and the shader effects) — a DEPENDENT of sge-core: nearly every signature names a
  * libGDX GL type (`FrameBuffer`, `Mesh`, `ShaderProgram`), so libGDX's policy is inherited from the core manifest and never restated. The `gwt` module is GWT's emulation and not taken.
  */
object VfxPolicy {

  private val Vfx = balticporter.tir.RuleScope.Only(Set("com.crashinvaders.vfx"))

  def include(f: String): Boolean = f.startsWith("core/src/") || f.startsWith("effects/src/")

  private val WitnessSubjects: Map[String, List[Int]] = Map("com.crashinvaders.vfx.utils.PrioritizedArray" -> List(0))
  def core(coreManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path], reference: Option[Path] = None): PortManifest =
    coreManifest.extendedBy(
      PortManifest(
        name = "sge-vfx",
        governs = Set("com.crashinvaders.vfx"),
        packageRenames = Map("com.crashinvaders.vfx" -> "sge.vfx"),
        surface = List(
          // `VfxGLUtils`' class initialiser picks the GL extension: GWT's by reflection (a class sge does not ship), else
          // `DefaultVfxGlExtension`, which takes the `Sge` context a class initialiser cannot be given. As the hand port
          // did, the extension is built on the first frame-buffer bind, which has the context; a bind through
          // `VfxGLUtils` before that names what is missing instead of a null dereference.
          new balticporter.transform.MethodBodyTransform(
            Map(
              "com.crashinvaders.vfx.gl.VfxGLUtils#<clinit>" -> "{ }",
              "com.crashinvaders.vfx.framebuffer.VfxFrameBuffer#getBoundFboHandle" ->
                """{
                  |  if (sge.vfx.gl.VfxGLUtils.glExtension == null)
                  |    sge.vfx.gl.VfxGLUtils.glExtension = new sge.vfx.gl.DefaultVfxGlExtension()
                  |  sge.vfx.gl.VfxGLUtils.getBoundFboHandle()
                  |}""".stripMargin,
              "com.crashinvaders.vfx.gl.VfxGLUtils#getBoundFboHandle" ->
                """{
                  |  if (sge.vfx.gl.VfxGLUtils.glExtension == null)
                  |    throw new java.lang.IllegalStateException(
                  |      "sge.vfx.gl.VfxGLUtils.glExtension is not initialised: it is built on the first VfxFrameBuffer bind, " +
                  |        "which has the sge.Sge context its DefaultVfxGlExtension needs. Bind one first, or assign VfxGLUtils.glExtension.")
                  |  sge.vfx.gl.VfxGLUtils.glExtension.boundFboHandle
                  |}""".stripMargin
            )
          ),
          // the shared scratch camera is built at its first read, where the context is in scope
          new balticporter.transform.GlobalsToImplicitsTransform(
            extensions = List(
              balticporter.transform.ContextHolderExtension(
                holder = "com.badlogic.gdx.Gdx",
                sites = Map("com.crashinvaders.vfx.framebuffer.VfxFrameBuffer#tmpCam" -> balticporter.transform.ContextSite.LazyInit)
              )
            )
          ),
          // the hand port's `PrioritizedArray[T]` holds any element, primitives included: its element array goes through
          // the witness and java's implicit `Object` bound goes, as on core's own containers
          new balticporter.transform.GlobalsToImplicitsTransform(
            requiredGivens = balticporter.transform.ElementWitnessTransform.constructorGivens(WitnessSubjects, lowlevel.port.LlsPolicy.Witness)
          ),
          new balticporter.transform.ElementWitnessTransform(
            witness = lowlevel.port.LlsPolicy.Witness,
            subjectTypes = WitnessSubjects,
            dropBound = WitnessSubjects.keySet,
            boxedWitness = Some("lowlevel.MkArray.anyRef[scala.AnyRef].asInstanceOf[lowlevel.MkArray[{elem}]]")
          ),
          // sge's `close` for java's `dispose`, which core renames through `Disposable`; this class declares it without one
          new balticporter.transform.MemberRenameTransform(renames = Map("com.crashinvaders.vfx.utils.ViewportQuadMesh#dispose" -> "close")),
          // the hand port's `toList` over java's `Iterable` (a scala view of the elements in iteration order)
          new balticporter.transform.AddMembersTransform(
            Map(
              "com.crashinvaders.vfx.utils.PrioritizedArray" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "toList",
                  0,
                  "def toList: scala.List[T] = { val b = scala.List.newBuilder[T]; val it = iterator(); while (it.hasNext()) b += it.next(); b.result() }",
                  balticporter.tir.Reason.Configured("add-members", "com.crashinvaders.vfx.utils.PrioritizedArray#toList"),
                  Some("the hand port's toList over the elements in iteration order")
                )
              )
            )
          ),
          new balticporter.transform.BeanPropertyTransform(scope = Vfx, derive = true),
          new balticporter.transform.NullaryArityTransform(scope = Vfx, derive = true),
          new balticporter.transform.NullabilityTransform(scope = Vfx, deriveMembers = true),
          balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0")
        ),
        parity = reference.map(r => ParityRef(roots = List(r))),
        frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/vfx-derived-policy.tsv")).filter(p => java.nio.file.Files.exists(p))
      )
    )
}
