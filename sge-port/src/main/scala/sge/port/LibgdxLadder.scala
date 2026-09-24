package sge.port

import balticporter.core.{ PortManifest, ResourceTree }
import lowlevel.port.LlsPolicy

import java.nio.file.Path

/** How sge ports libGDX core: a dependent of the lls port, built from named steps. Each step contributes phases, drops and the hand-written files it injects from `sge-port/overrides/<name>`. */
object LibgdxLadder {

  /** Core's declarations that allocate an array at their own type parameter, or construct a `DynamicArray` at it, so they take the `MkArray` clause (the "witness" step); the null-as-empty tables
    * (`IntMap`, `ObjectIntMap`, …) stay refused and counted.
    */
  val CoreWitnessSubjects: Map[String, List[Int]] = Map(
    "com.badlogic.gdx.utils.SnapshotArray" -> List(0),
    "com.badlogic.gdx.utils.DelayedRemovalArray" -> List(0),
    "com.badlogic.gdx.utils.Queue" -> List(0),
    "com.badlogic.gdx.graphics.g2d.Animation" -> List(0),
    "com.badlogic.gdx.graphics.g3d.particles.ParallelArray$ObjectChannel" -> List(0),
    "com.badlogic.gdx.math.Octree" -> List(0),
    "com.badlogic.gdx.graphics.g3d.particles.batches.BufferedParticleBatch" -> List(0)
  )

  /** The step fragments, cumulative; each merges with the base's instance of the same phase at the base's position.
    */
  /** the GL statics' two-hop path: the property step renames the getter (`gl20`); before it, the call. */
  private def glPath(sel: Set[String], n: String): String =
    if sel("properties") then s"graphics.gl$n" else s"graphics.getGL$n()"

  /** sge's remaining GL enums (all in the injected `GLEnum.scala`): one opaque per family, seeded at every GL parameter sge types with it, GL20 through GL32 (sge's ANGLE bindings implement that
    * surface). A `def`: a phase instance carries binding state.
    */
  /** the derive step: an opaque spec's seeds and fence are the reference's alone — the hand-listed hints and the propagation fences were the pre-derivation device.
    */
  private def opaque(spec: balticporter.tir.OpaqueSpec)(using derive: Boolean): balticporter.tir.Phase =
    new balticporter.transform.PrimitiveToOpaqueTransform(
      if derive then spec.copy(hints = Set.empty, extraHints = Set.empty, scope = balticporter.tir.RuleScope.Everywhere(Set.empty), derive = true)
      else spec
    )
  private def glEnum(name: String, hints: String*)(using derive: Boolean): balticporter.tir.Phase = glEnumExcept(name, Set.empty, hints*)

  /** `except`: declarations the flow would reach that sge keeps `Int` (member or parameter names under `com.badlogic.gdx.graphics.`). */
  private def glEnumExcept(name: String, except: Set[String], hints: String*)(using derive: Boolean): balticporter.tir.Phase =
    opaque(
      balticporter.tir.OpaqueSpec(
        fqn = "com.badlogic.gdx.graphics." + name,
        target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.graphics." + name, wrapName = "apply", unwrapName = "toInt"),
        hints = hints.map("com.badlogic.gdx.graphics." + _).toSet,
        underlying = balticporter.tir.OpaqueSpec.Primitive.Int,
        scope = balticporter.tir.RuleScope.Everywhere(except.map("com.badlogic.gdx.graphics." + _)),
        derive = derive
      )
    )
  private def moreGlEnums(using derive: Boolean): List[balticporter.tir.Phase] = List(
    glEnum(
      "BufferTarget",
      "GL20#glBindBuffer#target",
      "GL20#glBufferData#target",
      "GL20#glBufferSubData#target",
      "GL20#glGetBufferParameteriv#target",
      "GL30#glUnmapBuffer#target",
      "GL30#glGetBufferPointerv#target",
      "GL30#glMapBufferRange#target",
      "GL30#glFlushMappedBufferRange#target",
      "GL30#glBindBufferRange#target",
      "GL30#glBindBufferBase#target",
      "GL30#glCopyBufferSubData#readTarget",
      "GL30#glCopyBufferSubData#writeTarget",
      "GL30#glGetBufferParameteri64v#target"
    ),
    glEnum("BufferUsage", "GL20#glBufferData#usage"),
    glEnum(
      "TextureTarget",
      "GL20#glBindTexture#target",
      "GL20#glCompressedTexImage2D#target",
      "GL20#glCompressedTexSubImage2D#target",
      "GL20#glCopyTexImage2D#target",
      "GL20#glCopyTexSubImage2D#target",
      "GL20#glTexImage2D#target",
      "GL20#glTexParameterf#target",
      "GL20#glTexSubImage2D#target",
      "GL20#glFramebufferTexture2D#textarget",
      "GL20#glGenerateMipmap#target",
      "GL20#glGetTexParameterfv#target",
      "GL20#glGetTexParameteriv#target",
      "GL20#glTexParameterfv#target",
      "GL20#glTexParameteri#target",
      "GL20#glTexParameteriv#target",
      "GL30#glTexImage2D#target",
      "GL30#glTexImage3D#target",
      "GL30#glTexSubImage2D#target",
      "GL30#glTexSubImage3D#target",
      "GL30#glCopyTexSubImage3D#target",
      "GL31#glTexStorage2DMultisample#target",
      "GL31#glGetTexLevelParameteriv#target",
      "GL31#glGetTexLevelParameterfv#target",
      "GL32#glTexParameterIiv#target",
      "GL32#glTexParameterIuiv#target",
      "GL32#glGetTexParameterIiv#target",
      "GL32#glGetTexParameterIuiv#target",
      "GL32#glTexBuffer#target",
      "GL32#glTexBufferRange#target",
      "GL32#glTexStorage3DMultisample#target"
    ),
    glEnum(
      "BlendFactor",
      "GL20#glBlendFunc#sfactor",
      "GL20#glBlendFunc#dfactor",
      "GL20#glBlendFuncSeparate#srcRGB",
      "GL20#glBlendFuncSeparate#dstRGB",
      "GL20#glBlendFuncSeparate#srcAlpha",
      "GL20#glBlendFuncSeparate#dstAlpha",
      "GL32#glBlendFunci#src",
      "GL32#glBlendFunci#dst",
      "GL32#glBlendFuncSeparatei#srcRGB",
      "GL32#glBlendFuncSeparatei#dstRGB",
      "GL32#glBlendFuncSeparatei#srcAlpha",
      "GL32#glBlendFuncSeparatei#dstAlpha"
    ),
    glEnum(
      "BlendEquation",
      "GL20#glBlendEquation#mode",
      "GL20#glBlendEquationSeparate#modeRGB",
      "GL20#glBlendEquationSeparate#modeAlpha",
      "GL32#glBlendEquationi#mode",
      "GL32#glBlendEquationSeparatei#modeRGB",
      "GL32#glBlendEquationSeparatei#modeAlpha"
    ),
    glEnum(
      "CullFace",
      "GL20#glCullFace#mode",
      "GL20#glStencilFuncSeparate#face",
      "GL20#glStencilMaskSeparate#face",
      "GL20#glStencilOpSeparate#face"
    ),
    glEnum(
      "ShaderType",
      "GL20#glCreateShader#type",
      "GL20#glGetShaderPrecisionFormat#shadertype",
      "GL31#glCreateShaderProgramv#type"
    ),
    glEnum(
      "StencilOp",
      "GL20#glStencilOp#fail",
      "GL20#glStencilOp#zfail",
      "GL20#glStencilOp#zpass",
      "GL20#glStencilOpSeparate#fail",
      "GL20#glStencilOpSeparate#zfail",
      "GL20#glStencilOpSeparate#zpass"
    ),
    // sge keeps `internalformat` a plain Int (only `format` and `type` are typed): fence the flow at the GL sinks
    glEnumExcept(
      "PixelFormat",
      Set("GL20#glTexImage2D#internalformat", "GL30#glTexImage2D#internalformat", "GL30#glTexImage3D#internalformat"),
      "GL20#glCompressedTexSubImage2D#format",
      "GL20#glReadPixels#format",
      "GL20#glTexImage2D#format",
      "GL20#glTexSubImage2D#format",
      "GL30#glTexImage2D#format",
      "GL30#glTexImage3D#format",
      "GL30#glTexSubImage2D#format",
      "GL30#glTexSubImage3D#format",
      "GL32#glReadnPixels#format"
    ),
    glEnum(
      "DataType",
      "GL20#glDrawElements#type",
      "GL20#glReadPixels#type",
      "GL20#glTexImage2D#type",
      "GL20#glTexSubImage2D#type",
      "GL20#glVertexAttribPointer#type",
      "GL30#glDrawRangeElements#type",
      "GL30#glTexImage2D#type",
      "GL30#glTexImage3D#type",
      "GL30#glTexSubImage2D#type",
      "GL30#glTexSubImage3D#type",
      "GL30#glVertexAttribIPointer#type",
      "GL30#glDrawElementsInstanced#type",
      "GL30#glVertexAttribPointer#type",
      "GL31#glDrawElementsIndirect#type",
      "GL31#glVertexAttribFormat#type",
      "GL31#glVertexAttribIFormat#type",
      "GL32#glDrawElementsBaseVertex#type",
      "GL32#glDrawRangeElementsBaseVertex#type",
      "GL32#glDrawElementsInstancedBaseVertex#type",
      "GL32#glReadnPixels#type"
    )
  )

  def Steps:                      Map[String, List[balticporter.tir.Phase]] = stepsFor(Set.empty)
  def stepsFor(sel: Set[String]): Map[String, List[balticporter.tir.Phase]] = {
    given derive: Boolean = sel("derive")
    Map(
      // the reference-derived spelling step: no phase of its own — it switches `derive` on in the
      // opaque, nullability and arity phases and declares sge's tree as the manifest's reference.
      "derive" -> Nil,
      // sge's `[T: ClassTag]` bounds where java takes a `Class<T>` (`PoolManager.addPool`, `Skin.get`,
      // `Actions.action`, `AssetManager.get`): read off sge's tree
      "classtags" -> List(new balticporter.transform.ClassTagParamsTransform(derive = derive)),
      // members sge ships public where java declared them protected (`FileHandle(File, FileType)`), off sge's tree
      "visibility" -> List(
        new balticporter.transform.VisibilityTransform(
          widen = Set(
            "com.badlogic.gdx.scenes.scene2d.InputEvent#type",
            "com.badlogic.gdx.graphics.g2d.GlyphLayout#glyphRunPool",
            "com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue#edges",
            "com.badlogic.gdx.graphics.g3d.particles.ResourceData#data",
            "com.badlogic.gdx.graphics.g3d.particles.ResourceData#uniqueData",
            // java package-private, emitted `private[ui]`; sge's VisUI overrides it from `sge.visui.widget`
            // (the hand port spelled it `private[sge]`) — a cross-package override is public or it is
            // reflection, and reflection does not link on Scala.js/Native
            "com.badlogic.gdx.scenes.scene2d.ui.TextField#changeText"
          ),
          derive = derive
        )
      ),
      // sge's own members the suite reaches for; inline MemberSpecs come first, then the members
      // committed as inline text in AddedMembers (formerly fromReference, which read sge's hand port)
      "extras" -> List(
        // sge's ResourceData.encodeResourceJson: java writes the polymorphic resource with a `class` tag
        // (ResourceData.java:216); the injected ParticleEffectCodecs (step `json`) emit the controller graph
        new balticporter.transform.AddMembersTransform(
          Map(
            "com.badlogic.gdx.graphics.g3d.particles.ResourceData" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "encodeResourceJson",
                1,
                "private[particles] def encodeResourceJson(resource: Any): sge.utils.Json = {\n    resource match {\n      case effect: sge.graphics.g3d.particles.ParticleEffect => sge.graphics.g3d.particles.ParticleEffectCodecs.encodeResource(effect)\n      case other => throw sge.utils.SgeError.InvalidInput(\"Cannot serialize particle resource of type: \" + other.getClass.getName)\n    }\n  }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.particles.ResourceData#encodeResourceJson"),
                Some("sge's ResourceData.encodeResourceJson over the injected ParticleEffectCodecs"),
                true
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "fromJson",
                1,
                "private[particles] def fromJson[T <: java.lang.Object](json: sge.utils.Json): sge.graphics.g3d.particles.ResourceData[T] = {\n    val rd = new sge.graphics.g3d.particles.ResourceData[T]()\n    json match {\n      case sge.utils.Json.Obj(fields) =>\n        fields.fields.foreach { case (k, v) =>\n          if (k == \"assets\") v match {\n            case sge.utils.Json.Arr(elems) => for (elem <- elems) rd.sharedAssets.add(assetDataFromJson(elem).asInstanceOf[sge.graphics.g3d.particles.ResourceData.AssetData[T]])\n            case _ => ()\n          }\n        }\n        fields.fields.foreach { case (k, v) => k match {\n          case \"data\" => v match {\n            case sge.utils.Json.Arr(elems) => for (elem <- elems) rd.data.add(saveDataFromJson(elem, rd))\n            case _ => ()\n          }\n          case \"unique\" => v match {\n            case sge.utils.Json.Obj(uf) => uf.fields.foreach { case (uk, uv) => rd.uniqueData.put(uk, saveDataFromJson(uv, rd)) }\n            case _ => ()\n          }\n          case \"resource\" => v match {\n            case sge.utils.Json.Null => ()\n            case other => rd.resourceJson = lowlevel.Nullable(other)\n          }\n          case _ => ()\n        }}\n      case _ => throw new java.lang.IllegalArgumentException(\"Expected JSON object for ResourceData\")\n    }\n    rd\n  }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.particles.ResourceData#fromJson"),
                Some("sge's ResourceData.fromJson with T <: Object bound"),
                true
              )
            ),
            "com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "AlignMode",
                0,
                "type AlignMode = sge.graphics.g3d.particles.ParticleShader.AlignMode\nval AlignMode = sge.graphics.g3d.particles.ParticleShader.AlignMode",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch#AlignMode"),
                Some("sge moved AlignMode from ParticleShader to BillboardParticleBatch"),
                true
              )
            ),
            "com.badlogic.gdx.assets.AssetManager" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "apply",
                1,
                "def apply(resolver: sge.assets.loaders.FileHandleResolver, defaultLoaders: scala.Boolean = true)(using sge.Sge): sge.assets.AssetManager = new sge.assets.AssetManager(resolver, defaultLoaders)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.AssetManager#apply"),
                Some("factory with original param names — the funnel renamed them to $p"),
                true
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "apply",
                1,
                "def apply[T <: java.lang.Object](fileName: java.lang.String, tpe: java.lang.Class[T]): T = get[T](fileName, tpe).getOrElse(throw new java.lang.IllegalArgumentException(\"Asset not loaded: \" + fileName))",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.AssetManager#apply(String,Class)"),
                Some("sge's throwing apply — delegates to the port's Nullable-returning get"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "apply",
                1,
                "def apply[T <: java.lang.Object](assetDescriptor: sge.assets.AssetDescriptor[T]): T = get[T](assetDescriptor).getOrElse(throw new java.lang.IllegalArgumentException(\"Asset not loaded: \" + assetDescriptor.fileName))",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.AssetManager#apply(AssetDescriptor)"),
                Some("sge's throwing apply — delegates to the port's Nullable-returning get"),
                false
              ),
              // sge spells AssetManager.errorListener as a property; java only has setErrorListener
              balticporter.transform.AddMembersTransform.MemberSpec(
                "errorListener",
                0,
                "def errorListener: lowlevel.Nullable[sge.assets.AssetErrorListener] = lowlevel.Nullable(this.listener)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.AssetManager#errorListener"),
                Some("sge's errorListener getter — java only had setErrorListener"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "errorListener_=",
                0,
                "def errorListener_=(listener: lowlevel.Nullable[sge.assets.AssetErrorListener]): scala.Unit = setErrorListener(listener)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.AssetManager#errorListener_="),
                Some("sge's errorListener setter — property pair"),
                false
              )
            ),
            "com.badlogic.gdx.assets.loaders.SkinLoader$SkinParameter" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "apply",
                1,
                "def apply(textureAtlasPath: lowlevel.Nullable[java.lang.String] = lowlevel.Nullable.empty, resources: lowlevel.Nullable[lowlevel.util.ObjectMap[java.lang.String, java.lang.Object]] = lowlevel.Nullable.empty): sge.assets.loaders.SkinLoader.SkinParameter = new sge.assets.loaders.SkinLoader.SkinParameter(textureAtlasPath, resources)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.loaders.SkinLoader$SkinParameter#apply"),
                Some("factory with named params (the funnel renamed them to $p)"),
                true
              )
            ),
            "com.badlogic.gdx.utils.PerformanceCounter" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "apply",
                1,
                "def apply(name: java.lang.String, windowSize: scala.Int = 5): sge.utils.PerformanceCounter = new sge.utils.PerformanceCounter(name, windowSize)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.utils.PerformanceCounter#apply"),
                Some("factory with original param names — the funnel renamed them to $p"),
                true
              ),
              // sge's toString(sb) takes scala.StringBuilder; the port takes java.lang.StringBuilder
              balticporter.transform.AddMembersTransform.MemberSpec(
                "toString",
                1,
                "def toString(sb: scala.collection.mutable.StringBuilder): scala.collection.mutable.StringBuilder = { toString(sb.underlying); sb }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.utils.PerformanceCounter#toString(StringBuilder)"),
                Some("sge uses scala.StringBuilder; port uses java.lang.StringBuilder"),
                false
              )
            ),
            "com.badlogic.gdx.scenes.scene2d.Actor" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "isDebug",
                0,
                "def isDebug: scala.Boolean = this.debug" + "$field",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.Actor#isDebug"),
                Some("sge's boolean reader beside the fluent debug()"),
                false
              )
            ),
            "com.badlogic.gdx.utils.Timer" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "disposeThread",
                0,
                "private[sge] def disposeThread(): scala.Unit = { this.threadLock.synchronized { if (this.thread" + "$field != null) { this.thread" + "$field.dispose(); this.thread" + "$field = null } } }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.utils.Timer#disposeThread"),
                Some("sge's timer thread cleanup for test teardown"),
                true
              ),
              // sge's scheduleTask has default delaySeconds=Seconds.zero; add 1-arg overload
              balticporter.transform.AddMembersTransform.MemberSpec(
                "scheduleTask",
                1,
                "def scheduleTask(task: sge.utils.Timer.Task): sge.utils.Timer.Task = scheduleTask(task, sge.utils.Seconds(0f))",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.utils.Timer#scheduleTask(Task)"),
                Some("sge has default delaySeconds=Seconds.zero"),
                false
              )
            ),
            "com.badlogic.gdx.math.Vector3" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "cross",
                0,
                "infix def cross(vector: sge.math.Vector3): sge.math.Vector3 = set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Vector3#cross(Vector3)"),
                Some("sge's cross product (returns Vector3 not this.type)"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "+",
                0,
                "def +(v: sge.math.Vector3): sge.math.Vector3 = add(v)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Vector3#+"),
                None,
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "-",
                0,
                "def -(v: sge.math.Vector3): sge.math.Vector3 = sub(v)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Vector3#-"),
                None,
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "cross",
                0,
                "infix def cross(x: scala.Float, y: scala.Float, z: scala.Float): sge.math.Vector3 = set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Vector3#cross(float,float,float)"),
                Some("sge's cross product 3-arg (returns Vector3 not this.type)"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "rotateAroundDeg",
                0,
                "def rotateAroundDeg(axis: sge.math.Vector3, degrees: scala.Float): sge.math.Vector3 = rotateRad(axis, degrees * lowlevel.math.MathUtils.degreesToRadians)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Vector3#rotateAroundDeg"),
                Some("sge's rotate around axis in degrees"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "rotateAroundRad",
                0,
                "def rotateAroundRad(axis: sge.math.Vector3, radians: scala.Float): sge.math.Vector3 = { val tmpMat = new sge.math.Matrix4(); tmpMat.setToRotation(axis, radians * lowlevel.math.MathUtils.radiansToDegrees); this.mul(tmpMat) }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Vector3#rotateAroundRad"),
                Some("sge's rotate around axis in radians"),
                false
              )
            ),
            "com.badlogic.gdx.scenes.scene2d.ui.Table" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "isClip",
                0,
                "def isClip: scala.Boolean = this.clip" + "$field",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.ui.Table#isClip"),
                Some("sge's boolean reader for the private clip field"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "tableAlign",
                0,
                // the field keeps java's `int` (no derived slot reaches it: master renamed the FIELD to `tableAlign`); the accessor wraps at the boundary
                "def tableAlign: sge.utils.Align = sge.utils.Align(this.align" + "$field)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.ui.Table#tableAlign"),
                Some("sge's public tableAlign (renamed from align to avoid Widget collision)"),
                false
              ),
              // sge has `var padTop/padLeft/padRight/padBottom: Value`; the port has fluent overloads
              // that create setter ambiguity — add explicit property _= setters
              balticporter.transform.AddMembersTransform.MemberSpec(
                "padTop_=",
                0,
                "def padTop_=(v: sge.scenes.scene2d.ui.Value): scala.Unit = { this.padTop" + "$field = v }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.ui.Table#padTop_="),
                Some("explicit property setter — resolves fluent overload ambiguity"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "padLeft_=",
                0,
                "def padLeft_=(v: sge.scenes.scene2d.ui.Value): scala.Unit = { this.padLeft" + "$field = v }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.ui.Table#padLeft_="),
                Some("explicit property setter — resolves fluent overload ambiguity"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "padRight_=",
                0,
                "def padRight_=(v: sge.scenes.scene2d.ui.Value): scala.Unit = { this.padRight" + "$field = v }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.ui.Table#padRight_="),
                Some("explicit property setter — resolves fluent overload ambiguity"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "padBottom_=",
                0,
                "def padBottom_=(v: sge.scenes.scene2d.ui.Value): scala.Unit = { this.padBottom" + "$field = v }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.ui.Table#padBottom_="),
                Some("explicit property setter — resolves fluent overload ambiguity"),
                false
              )
            ),
            // sge's Preferences.put takes immutable Map; port takes mutable.Map (from retarget)
            "com.badlogic.gdx.Preferences" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "put",
                1,
                "def put(vals: scala.collection.immutable.Map[java.lang.String, ?]): sge.Preferences = { val m = scala.collection.mutable.Map.from(vals); put(m) }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.Preferences#put(immutable.Map)"),
                Some("sge uses immutable Map; port retargeted to mutable.Map — bridge"),
                false
              )
            ),
            // sge's NativeInputConfiguration.setMaxLength takes Option[Int]; port takes Int (-1 sentinel)
            "com.badlogic.gdx.input.NativeInputConfiguration" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "setMaxLength",
                1,
                "def setMaxLength(maxLength: scala.Option[scala.Int]): sge.input.NativeInputConfiguration = setMaxLength(maxLength.getOrElse(-1))",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.input.NativeInputConfiguration#setMaxLength(Option)"),
                Some("sge uses Option[Int]; port uses Int (-1 sentinel) — bridge"),
                false
              )
            ),
            // sge's I18NBundleParameter takes Nullable[Locale]; port takes bare Locale
            "com.badlogic.gdx.assets.loaders.I18NBundleLoader$I18NBundleParameter" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(locale: lowlevel.Nullable[java.util.Locale]) = this(locale, lowlevel.Nullable.empty)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.loaders.I18NBundleLoader$I18NBundleParameter#this(Nullable)"),
                Some("sge wraps locale in Nullable; tests pass Nullable(Locale)"),
                false
              )
            ),
            "com.badlogic.gdx.math.Vector2" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "+",
                0,
                "def +(v: sge.math.Vector2): sge.math.Vector2 = add(v)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Vector2#+"),
                None,
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "-",
                0,
                "def -(v: sge.math.Vector2): sge.math.Vector2 = sub(v)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Vector2#-"),
                None,
                false
              )
            ),
            "com.badlogic.gdx.math.Vector4" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "+",
                0,
                "def +(v: sge.math.Vector4): sge.math.Vector4 = add(v)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Vector4#+"),
                None,
                false
              )
            ),
            // sge's BitmapFontData.imagePaths is a var; the port has getter only (field renamed to imagePaths$field)
            "com.badlogic.gdx.graphics.g2d.BitmapFont$BitmapFontData" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "imagePaths_=",
                0,
                "def imagePaths_=(v: lowlevel.Nullable[scala.Array[java.lang.String]]): scala.Unit = { this.imagePaths" + "$field = v }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g2d.BitmapFont$BitmapFontData#imagePaths_="),
                Some("sge's imagePaths setter — field was renamed to imagePaths$field"),
                false
              )
            ),
            // sge spells BaseDrawable.name as a var; the port has getter (from getName) but no setter
            "com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "name_=",
                0,
                "def name_=(v: lowlevel.Nullable[java.lang.String]): scala.Unit = setName(v)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable#name_="),
                Some("sge's name setter — delegates to the port's setName"),
                false
              )
            ),
            // sge's Selection extends Scala Iterable; the port's extends JavaIterable — add toList
            "com.badlogic.gdx.scenes.scene2d.utils.Selection" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "toList",
                0,
                "def toList: scala.List[T] = { val b = scala.List.newBuilder[T]; val it = iterator(); while (it.hasNext()) b += it.next(); b.result() }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.utils.Selection#toList"),
                Some("sge's Selection extends Scala Iterable — bridge"),
                false
              )
            ),
            // sge added poolOrNull with ClassTag
            "com.badlogic.gdx.utils.PoolManager" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "poolOrNull",
                0,
                "def poolOrNull[T <: java.lang.Object](using ct: scala.reflect.ClassTag[T]): lowlevel.Nullable[sge.utils.Pool[T]] = { val p = this.typePools.get(ct.runtimeClass.asInstanceOf[java.lang.Class[T]]); if (p.isEmpty) lowlevel.Nullable.empty else lowlevel.Nullable(p.get.asInstanceOf[sge.utils.Pool[T]]) }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.utils.PoolManager#poolOrNull"),
                Some("sge's ClassTag-based Nullable pool lookup"),
                false
              )
            ),
            // sge renamed FileHandle.file to internalFile
            "com.badlogic.gdx.files.FileHandle" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "internalFile",
                0,
                "def internalFile: java.io.File = this.file",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.files.FileHandle#internalFile"),
                Some("sge renamed file -> internalFile"),
                false
              )
            ),
            // sge wraps AssetDescriptor.params in Nullable; add constructor overload that takes Nullable
            "com.badlogic.gdx.assets.AssetDescriptor" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(file: sge.files.FileHandle, assetType: java.lang.Class[T], params: lowlevel.Nullable[sge.assets.AssetLoaderParameters[T]]) = { this(); this.fileName = file.path; this.file = lowlevel.Nullable(file); this.`type` = assetType; this.params = params.asInstanceOf[lowlevel.Nullable[sge.assets.AssetLoaderParameters[?]]] }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.AssetDescriptor#this(FileHandle,Class,Nullable)"),
                Some("sge wraps params in Nullable; tests pass Nullable.empty"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(fileName: java.lang.String, assetType: java.lang.Class[T], params: lowlevel.Nullable[sge.assets.AssetLoaderParameters[T]]) = { this(); this.fileName = fileName; this.`type` = assetType; this.params = params.asInstanceOf[lowlevel.Nullable[sge.assets.AssetLoaderParameters[?]]] }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.AssetDescriptor#this(String,Class,Nullable)"),
                Some("sge wraps params in Nullable; tests pass Nullable.empty"),
                false
              )
            ),
            // sge's ParticleEffectSaveParameter has default batches=Nullable.empty; port requires 3+ args
            "com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader$ParticleEffectSaveParameter" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(file: sge.files.FileHandle, manager: sge.assets.AssetManager) = this(file, manager, lowlevel.Nullable.empty)",
                balticporter.tir.Reason.Configured(
                  "add-members",
                  "com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader$ParticleEffectSaveParameter#this(FileHandle,AssetManager)"
                ),
                Some("sge has batches=Nullable.empty default; add 2-arg ctor"),
                false
              )
            ),
            // sge's ParticleEffectLoadParameter has a no-arg ctor; port requires batches
            "com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader$ParticleEffectLoadParameter" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(batches: lowlevel.Nullable[lowlevel.util.DynamicArray[sge.graphics.g3d.particles.batches.ParticleBatch[?]]]) = { this(new lowlevel.util.DynamicArray[sge.graphics.g3d.particles.batches.ParticleBatch[?]]()); this.batches = batches }",
                balticporter.tir.Reason.Configured(
                  "add-members",
                  "com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader$ParticleEffectLoadParameter#this(Nullable)"
                ),
                Some("sge wraps batches in Nullable"),
                false
              )
            ),
            // sge's ParticleController takes varargs; port takes Array
            "com.badlogic.gdx.graphics.g3d.particles.ParticleController" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(name: java.lang.String, emitter: sge.graphics.g3d.particles.emitters.Emitter, renderer: sge.graphics.g3d.particles.renderers.ParticleControllerRenderer[?, ?], influencers: sge.graphics.g3d.particles.influencers.Influencer*)(using sge.Sge) = this(name, emitter, renderer, influencers.toArray)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.particles.ParticleController#this(varargs)"),
                Some("sge's varargs ctor — delegates to Array ctor"),
                false
              )
            ),
            // sge's ParticleEffect takes varargs; port takes Array
            "com.badlogic.gdx.graphics.g3d.particles.ParticleEffect" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(emitters: sge.graphics.g3d.particles.ParticleController*)(using sge.Sge) = { this(); for (e <- emitters) this.controllers.add(e) }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.particles.ParticleEffect#this(varargs)"),
                Some("sge's varargs ctor — delegates to no-arg then adds"),
                false
              )
            ),
            // sge's ModelInstance takes Nullable[Seq[String]]; port takes Array[String]. An ABSENT id
            // list is java's `null` (ModelInstance.java:153: copy EVERY root node); an empty array is
            // java's `new String[0]` (copy the nodes named by zero ids: none) — the two are not the
            // same value, so the bridge hands `null` to the (Model, Matrix4, String...) ctor
            "com.badlogic.gdx.graphics.g3d.ModelInstance" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(model: sge.graphics.g3d.Model, rootNodeIds: lowlevel.Nullable[scala.collection.immutable.Seq[java.lang.String]]) = this(model, lowlevel.Nullable.empty, rootNodeIds.map(s => { val a = new scala.Array[java.lang.String](s.size); s.copyToArray(a); a }).getOrElse(null))",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.ModelInstance#this(Model,Nullable[Seq])"),
                Some("sge takes Nullable[Seq[String]]; port takes String*"),
                false
              )
            )
          )
        ),
        // Table.add varargs
        new balticporter.transform.AddMembersTransform(
          Map(
            "com.badlogic.gdx.scenes.scene2d.ui.Table" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "add",
                1,
                "def add(actors: sge.scenes.scene2d.Actor*): sge.scenes.scene2d.ui.Table = { for (a <- actors) add(lowlevel.Nullable(a)); this }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.ui.Table#add(Actor*)"),
                Some("sge's varargs add: `table.add(a1, a2, a3)`"),
                false
              ),
              // sge's Table.stack takes Actor*; port takes Array[Actor]
              balticporter.transform.AddMembersTransform.MemberSpec(
                "stack",
                1,
                "def stack(actors: sge.scenes.scene2d.Actor*): sge.scenes.scene2d.ui.Cell[sge.scenes.scene2d.ui.Stack] = stack(actors.toArray)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.ui.Table#stack(Actor*)"),
                Some("sge's varargs stack"),
                false
              )
              // sge's Table.padTop takes Value only (not Float overload too); Preferences.put takes immutable Map
              // — deep API shape differences, not fixable with an extra member
            )
          )
        ),
        // VertexAttributes and Mesh varargs: sge takes T*; port takes Array[T]
        new balticporter.transform.AddMembersTransform(
          Map(
            "com.badlogic.gdx.graphics.VertexAttributes" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "apply",
                1,
                "def apply(attributes: sge.graphics.VertexAttribute*): sge.graphics.VertexAttributes = new sge.graphics.VertexAttributes(attributes.toArray)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.VertexAttributes#apply"),
                Some("sge's varargs ctor — port takes Array"),
                true
              )
            ),
            // sge's TextureAtlasData has Region/Page as inner classes of the CLASS; the port puts them
            // in the companion object — add factory methods on the class so `data.Region()` works
            "com.badlogic.gdx.graphics.g2d.TextureAtlas$TextureAtlasData" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "Region",
                0,
                "def Region(): sge.graphics.g2d.TextureAtlas.TextureAtlasData.Region = new sge.graphics.g2d.TextureAtlas.TextureAtlasData.Region()",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g2d.TextureAtlas$TextureAtlasData#Region"),
                Some("sge's Region is an inner class; port puts it in companion — factory bridge"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "Page",
                0,
                "def Page(): sge.graphics.g2d.TextureAtlas.TextureAtlasData.Page = new sge.graphics.g2d.TextureAtlas.TextureAtlasData.Page()",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g2d.TextureAtlas$TextureAtlasData#Page"),
                Some("sge's Page is an inner class; port puts it in companion — factory bridge"),
                false
              )
            ),
            // sge has DebugProc as an inner trait of GL32; the port puts it in the companion object
            "com.badlogic.gdx.graphics.GL32" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "DebugProc",
                0,
                "type DebugProc = sge.graphics.GL32.DebugProc",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.GL32#DebugProc"),
                Some("type alias — inner trait moved to companion by the port"),
                false
              )
            ),
            // sge's Intersector.isPointInPolygon takes Array[Vector2]; port retargeted to DynamicArray
            "com.badlogic.gdx.math.Intersector" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "isPointInPolygon",
                1,
                "def isPointInPolygon(polygon: scala.Array[sge.math.Vector2], point: sge.math.Vector2): scala.Boolean = { val da = new lowlevel.util.DynamicArray[sge.math.Vector2](true, polygon, 0, polygon.length); isPointInPolygon(da, point) }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Intersector#isPointInPolygon(Array)"),
                Some("sge uses Array[Vector2]; port retargeted to DynamicArray — bridge"),
                true
              )
            ),
            // sge's Octree.getAll takes mutable.Set; port takes ObjectSet
            "com.badlogic.gdx.math.Octree" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "getAll",
                1,
                "def getAll(resultSet: scala.collection.mutable.Set[T]): scala.collection.mutable.Set[T] = { val os = new lowlevel.util.ObjectSet[T](); getAll(os); val it = os.iterator(); while (it.hasNext()) resultSet.add(it.next()); resultSet }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Octree#getAll(Set)"),
                Some("sge uses mutable.Set; port uses ObjectSet — bridge"),
                false
              )
            )
          )
        ),
        // varargs constructors: java's `T...` emits `Array[T]`; sge writes `T*`
        new balticporter.transform.AddMembersTransform(
          Map(
            "com.badlogic.gdx.graphics.g2d.Animation" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(frameDuration: scala.Float, keyFrames: T*)(using mk: lowlevel.MkArray[T]) = {\n    this(frameDuration, mk.create(0))\n    val da = lowlevel.util.DynamicArray[T](true, keyFrames.size)\n    for (k <- keyFrames) da.add(k)\n    this.setKeyFrames(mk.copyOf(da.items.asInstanceOf[scala.Array[T]], da.size))\n  }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g2d.Animation#<init>(T*)"),
                Some("sge's varargs ctor: `Animation[String](0.1f, \"a\", \"b\", \"c\")`"),
                false
              )
            ),
            "com.badlogic.gdx.InputMultiplexer" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(processors: sge.InputProcessor*) = {\n    this()\n    for (p <- processors) this.processors.add(p)\n  }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.InputMultiplexer#<init>(InputProcessor*)"),
                Some("sge's varargs ctor: `InputMultiplexer(p1, p2)`"),
                false
              )
            ),
            "com.badlogic.gdx.graphics.glutils.VertexBufferObject" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(isStatic: scala.Boolean, numVertices: scala.Int, attributes: sge.graphics.VertexAttribute*)(using sge.Sge) =\n    this(isStatic, numVertices, new sge.graphics.VertexAttributes(attributes.toArray))",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.glutils.VertexBufferObject#<init>(VertexAttribute*)"),
                Some("sge's varargs ctor: `VertexBufferObject(true, 4, positionAttr())`"),
                false
              )
            ),
            "com.badlogic.gdx.scenes.scene2d.actions.Actions" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "sequence",
                1,
                "def sequence(actions: sge.scenes.scene2d.Action*): sge.scenes.scene2d.actions.SequenceAction = {\n    val action = new sge.scenes.scene2d.actions.SequenceAction()\n    for (a <- actions) action.addAction(a)\n    action\n  }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.actions.Actions#sequence(Action*)"),
                Some("sge's varargs sequence: `Actions.sequence(moves*)`"),
                true
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "parallel",
                1,
                "def parallel(actions: sge.scenes.scene2d.Action*): sge.scenes.scene2d.actions.ParallelAction = {\n    val action = new sge.scenes.scene2d.actions.ParallelAction()\n    for (a <- actions) action.addAction(a)\n    action\n  }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.scenes.scene2d.actions.Actions#parallel(Action*)"),
                Some("sge's varargs parallel: `Actions.parallel(actions*)`"),
                true
              )
            ),
            "com.badlogic.gdx.graphics.glutils.VertexArray" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(numVertices: scala.Int, attributes: sge.graphics.VertexAttribute*) =\n    this(numVertices, new sge.graphics.VertexAttributes(attributes.toArray))",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.glutils.VertexArray#<init>(VertexAttribute*)"),
                Some("sge's varargs ctor: `VertexArray(4, positionAttr())`"),
                false
              )
            )
          )
        ),
        // the reference port's own members, committed as inline text (formerly fromReference)
        new balticporter.transform.AddMembersTransform(members = AddedMembers.all)
      ),
      // (`Actor.top`/`right` collide with the fluent `top()`/`right()` of `Table`/`Container`/`HorizontalGroup`:
      // 6 errors — sge respelled those; 2 suite sites stay)
      // sge's float opaques for tolerances and angles (`Epsilon`, `Degrees`, `Radians`), seeded off sge's tree
      "mathunits" -> (List("Epsilon", "Degrees", "Radians").map(n =>
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.math." + n,
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.math." + n, wrapName = "apply", unwrapName = "toFloat"),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Float
          )
        )
      ) :+ new balticporter.transform.ThreadConfinedStaticsTransform(LibgdxPolicy.ThreadConfinedScratch)), // Matrix4's scratch statics, per thread (the full port's own value)
      // sge's `Input.Key`/`Input.Button` opaques (its Input companion): spliced as the companion's members,
      // then seeded from sge's tree (the derive step) — `isKeyPressed(key: Key)`, `Keys.A: Key`, …
      "keys" -> List(
        new balticporter.transform.AddMembersTransform(
          Map(
            "com.badlogic.gdx.Input" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "Key",
                0,
                "opaque type Key = Int\n  object Key {\n    inline def apply(value: Int): Key = value\n    given lowlevel.MkArray.OfInts[Key] = lowlevel.MkArray.ofIntAs[Key]\n    extension (k: Key) { inline def toInt: Int = k }\n  }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.Input#Key"),
                Some("sge's opaque key code (`Input.Key`) over java's int constants"),
                true
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "Button",
                0,
                "opaque type Button = Int\n  object Button {\n    inline def apply(value: Int): Button = value\n    given lowlevel.MkArray.OfInts[Button] = lowlevel.MkArray.ofIntAs[Button]\n    extension (b: Button) { inline def toInt: Int = b }\n  }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.Input#Button"),
                Some("sge's opaque button code (`Input.Button`) over java's int constants"),
                true
              )
            )
          )
        ),
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.Input.Key",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.Input.Key", wrapName = "apply", unwrapName = "toInt"),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Int
          )
        ),
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.Input.Button",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.Input.Button", wrapName = "apply", unwrapName = "toInt"),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Int
          )
        )
      ),
      // sge's `Align` opaque over java's `int` alignment bitmasks — the full port's spec, seeded here from
      // the reference (`GlyphLayout.setText`'s `halign`, `BitmapFont.draw`'s, every scene2d field; ISS-770)
      "align" -> List(opaque(LibgdxPolicy.AlignSpec)),
      // java's `Gdx.app.log/error/debug(tag, msg[, t])` become sge's context-free `Log`: a class that only
      // logs then takes no context (sge commented the particle
      // values' call out — a skip; the port keeps java's call). Placed before the context step.
      "logging" -> List(
        new balticporter.transform.CallSiteSubstitutionTransform(
          Map(
            "com.badlogic.gdx.Application#log(String,String)" -> "sge.utils.Log.info({arg0} + \": \" + {arg1})",
            "com.badlogic.gdx.Application#log(String,String,Throwable)" -> "sge.utils.Log.info({arg0} + \": \" + {arg1} + \"\\n\" + {arg2})",
            "com.badlogic.gdx.Application#error(String,String)" -> "sge.utils.Log.error({arg0} + \": \" + {arg1})",
            "com.badlogic.gdx.Application#error(String,String,Throwable)" -> "sge.utils.Log.error({arg0} + \": \" + {arg1}, {arg2})",
            "com.badlogic.gdx.Application#debug(String,String)" -> "sge.utils.Log.debug({arg0} + \": \" + {arg1})",
            "com.badlogic.gdx.Application#debug(String,String,Throwable)" -> "sge.utils.Log.debug({arg0} + \": \" + {arg1} + \"\\n\" + {arg2})"
          )
        )
      ),
      // the async executor per platform row (java's own on JVM/Native, libGDX's GWT emulation on JS): no phase, a drop and platform injections only.
      "async" -> Nil,
      // WebGL refuses vertex arrays from client memory, and java's `DecalBatch.initialize` falls back to
      // `VertexDataType.VertexArray` without GL30 (sge browser IT, Viewer3D: "Vertex arrays from client memory
      // not supported in WebGL"). sge's hand port falls back to `VertexBufferObject` on every platform — the one
      // site where it diverged (java's SpriteBatch already defaults to buffer objects, PolygonSpriteBatch is kept);
      // the body is java's, that constant apart. Shadowing `VertexArray` per row was tried and refused: shared
      // suites pin its java semantics on every row.
      "webgl" -> List(
        new balticporter.transform.MethodBodyTransform(
          Map(
            "com.badlogic.gdx.graphics.g3d.decals.DecalBatch#initialize" ->
              """{
                |  this.vertices = new scala.Array[scala.Float](size * sge.graphics.g3d.decals.Decal.SIZE)
                |  val vertexDataType: sge.graphics.Mesh.VertexDataType =
                |    if (!scala.Predef.summon[sge.Sge].graphics.gl30.isEmpty) sge.graphics.Mesh.VertexDataType.VertexBufferObjectWithVAO
                |    else sge.graphics.Mesh.VertexDataType.VertexBufferObject
                |  this.mesh = new sge.graphics.Mesh(vertexDataType, false, size * 4, size * 6, scala.Array[sge.graphics.VertexAttribute](new sge.graphics.VertexAttribute(sge.graphics.VertexAttributes.Usage.Position, 3, sge.graphics.glutils.ShaderProgram.POSITION_ATTRIBUTE), new sge.graphics.VertexAttribute(sge.graphics.VertexAttributes.Usage.ColorPacked, 4, sge.graphics.glutils.ShaderProgram.COLOR_ATTRIBUTE), new sge.graphics.VertexAttribute(sge.graphics.VertexAttributes.Usage.TextureCoordinates, 2, sge.graphics.glutils.ShaderProgram.TEXCOORD_ATTRIBUTE + "0")))
                |  val indices: scala.Array[scala.Short] = new scala.Array[scala.Short](size * 6)
                |  var v: scala.Int = 0
                |  var i: scala.Int = 0
                |  while (i < indices.length) {
                |    indices(i) = v.asInstanceOf[scala.Short]
                |    indices(i + 1) = (v + 2).asInstanceOf[scala.Short]
                |    indices(i + 2) = (v + 1).asInstanceOf[scala.Short]
                |    indices(i + 3) = (v + 1).asInstanceOf[scala.Short]
                |    indices(i + 4) = (v + 2).asInstanceOf[scala.Short]
                |    indices(i + 5) = (v + 3).asInstanceOf[scala.Short]
                |    i = i + 6
                |    v = v + 4
                |  }
                |  this.mesh.setIndices(indices)
                |}""".stripMargin
          )
        )
      ),
      // sge's platform contract and its JVM implementations, copied: no phase, injections only.
      "backend-jvm" -> Nil,
      // the 59 java `native` members answered on the JVM: bodies from
      // `LibgdxNativeBodies`, the objects they call injected (`Gdx2DNative`, `BufferUtilsNative`, `ETC1Native`).
      // sge's shared `BufferUtils`/`Gdx2DPixmap`/`ETC1`/`UIUtils` over the platform ops traits (one
      // implementation per row, already injected) replace java's JNI classes on every row; only
      // `Matrix4`'s three native loops keep a substituted body.
      "natives" -> List(new balticporter.transform.MethodBodyTransform(LibgdxNativeBodies.matrix4)),
      // sge's desktop backend (GLFW window, input, files, preferences, net, miniaudio), copied: injections only.
      "backend-desktop" -> Nil,
      // sge's typed JSON/UBJSON documents with Kindlings-derived codecs replace java's reflective JSON stack, one
      // consumer family at a time: first the g3d model loader.
      "json" -> List(
        // the g3d particle loader reads sge's typed document (`ResourceData.fromJson` over the jsoniter AST)
        // and rebuilds the effect through the injected `ParticleEffectCodecs` in `loadSync`, where the
        // `Sge` context is in scope; java did both reflectively in `ResourceData.read` (ISS-507).
        new balticporter.transform.MethodBodyTransform(
          Map(
            "com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader#getDependencies" ->
              """{
                |  val jsonAst = sge.utils.readFromString[sge.utils.Json](file.readString())(using hearth.kindlings.jsoniterjson.codec.JsonCodec.jsonValueCodec)
                |  val data = sge.graphics.g3d.particles.ResourceData.fromJson[sge.graphics.g3d.particles.ParticleEffect](jsonAst)
                |  val assets = this.items.synchronized {
                |    val entry = new lowlevel.util.ObjectMap.Entry[java.lang.String, sge.graphics.g3d.particles.ResourceData[sge.graphics.g3d.particles.ParticleEffect]]()
                |    entry.key = fileName
                |    entry.value = lowlevel.Nullable(data)
                |    this.items.add(entry)
                |    data.assets
                |  }
                |  val descriptors = new lowlevel.util.DynamicArray[sge.assets.AssetDescriptor[?]]()
                |  for (assetData <- assets) {
                |    // If the asset doesn't exist try to load it from loading effect directory
                |    if (!this.resolve(assetData.filename).exists()) {
                |      assetData.filename = file.parent().child(scala.Predef.summon[sge.Sge].files.internal(assetData.filename).name).path
                |    } else ()
                |    if (assetData.asInstanceOf[sge.graphics.g3d.particles.ResourceData.AssetData[java.lang.Object]].`type` eq classOf[sge.graphics.g3d.particles.ParticleEffect]) {
                |      descriptors.add(new sge.assets.AssetDescriptor(assetData.filename, assetData.asInstanceOf[sge.graphics.g3d.particles.ResourceData.AssetData[java.lang.Object]].`type`.asInstanceOf[java.lang.Class[sge.graphics.g3d.particles.ParticleEffect]], parameter))
                |    } else {
                |      descriptors.add(new sge.assets.AssetDescriptor(assetData.filename, assetData.asInstanceOf[sge.graphics.g3d.particles.ResourceData.AssetData[java.lang.Object]].`type`))
                |    }
                |  }
                |  descriptors
                |}""".stripMargin,
            "com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader#loadSync" ->
              """{
                |  var effectData: lowlevel.Nullable[sge.graphics.g3d.particles.ResourceData[sge.graphics.g3d.particles.ParticleEffect]] = lowlevel.Nullable.empty
                |  this.items.synchronized {
                |    scala.util.boundary {
                |      var i: scala.Int = 0
                |      while (i < this.items.size) {
                |        val entry = this.items.get(i)
                |        if (entry.key.equals(fileName)) {
                |          effectData = entry.value
                |          this.items.removeIndex(i)
                |          scala.util.boundary.break(())
                |        } else ()
                |        i = i + 1
                |      }
                |    }
                |  }
                |  val data = effectData.getOrElse(throw new java.lang.RuntimeException("No ResourceData found for " + fileName))
                |  // the effect graph `ResourceData.fromJson` kept as an AST is rebuilt here, where the `Sge` context is in scope
                |  if (data.resource.isEmpty) {
                |    data.resourceJson.foreach { resourceJson =>
                |      data.resource = lowlevel.Nullable(sge.graphics.g3d.particles.ParticleEffectCodecs.decodeResource(resourceJson))
                |    }
                |  } else ()
                |  data.resource.foreach { res => res.load(manager, data.asInstanceOf[sge.graphics.g3d.particles.ResourceData[java.lang.Object]]) }
                |  lowlevel.Nullable(parameter).foreach { p =>
                |    p.batches.foreach { batchList =>
                |      for (batch <- batchList) {
                |        batch.asInstanceOf[sge.graphics.g3d.particles.batches.ParticleBatch[sge.graphics.g3d.particles.renderers.ParticleControllerRenderData]].load(manager, data.asInstanceOf[sge.graphics.g3d.particles.ResourceData[sge.graphics.g3d.particles.renderers.ParticleControllerRenderData]].asInstanceOf[sge.graphics.g3d.particles.ResourceData[java.lang.Object]])
                |      }
                |      data.resource.foreach(_.setBatch(batchList))
                |    }
                |  }
                |  data.resource.getOrElse(throw new java.lang.RuntimeException("ResourceData has no resource for " + fileName))
                |}""".stripMargin
          )
        )
      ),
      "witness" -> List(
        new balticporter.transform.GlobalsToImplicitsTransform(
          requiredGivens = balticporter.transform.ElementWitnessTransform.constructorGivens(CoreWitnessSubjects, LlsPolicy.Witness)
        ),
        new balticporter.transform.ElementWitnessTransform(
          witness = LlsPolicy.Witness,
          subjectTypes = CoreWitnessSubjects,
          // the clause is threaded; java's implicit `<: Object` bound stays on core's subjects — their
          // collaborators (`ObjectSet[T]`) keep theirs.
          dropBound = Set.empty,
          boxedWitness = Some("lowlevel.MkArray.anyRef[scala.AnyRef].asInstanceOf[lowlevel.MkArray[{elem}]]")
        )
      ),
      // core's collections onto lls's and the JDK table, `Comparator -> Ordering`: the base's instance
      // widened to core's entry (merged `Only` scopes).
      "collections" -> List(
        new balticporter.transform.CollectionsTransform(
          scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx")),
          retarget = Map(
            "java.util.Comparator" -> "scala.math.Ordering",
            "com.badlogic.gdx.utils.FloatArray" -> "lowlevel.util.DynamicArray",
            "com.badlogic.gdx.utils.ShortArray" -> "lowlevel.util.DynamicArray",
            "com.badlogic.gdx.utils.IntArray" -> "lowlevel.util.DynamicArray",
            "com.badlogic.gdx.utils.BooleanArray" -> "lowlevel.util.DynamicArray",
            "com.badlogic.gdx.utils.LongArray" -> "lowlevel.util.DynamicArray"
          ),
          retargetTypeArgs = Map(
            "com.badlogic.gdx.utils.FloatArray" -> List(balticporter.transform.CollectionsTransform.RetargetArg.FixedType("scala.Float")),
            "com.badlogic.gdx.utils.ShortArray" -> List(balticporter.transform.CollectionsTransform.RetargetArg.FixedType("scala.Short")),
            "com.badlogic.gdx.utils.IntArray" -> List(balticporter.transform.CollectionsTransform.RetargetArg.FixedType("scala.Int")),
            "com.badlogic.gdx.utils.BooleanArray" -> List(balticporter.transform.CollectionsTransform.RetargetArg.FixedType("scala.Boolean")),
            "com.badlogic.gdx.utils.LongArray" -> List(balticporter.transform.CollectionsTransform.RetargetArg.FixedType("scala.Long"))
          ),
          retargetRewrites = {
            val RT = balticporter.transform.CollectionsTransform.RetargetRewrite
            val common: Map[(String, Int), balticporter.transform.CollectionsTransform.RetargetRewrite] = Map(
              ("empty", 0) -> RT.Rename("isEmpty"),
              ("isEmpty", 0) -> RT.Template("$recv.isEmpty"),
              ("first", 0) -> RT.Template("$recv.head"),
              ("incr", 2) -> RT.Template("{ val $i = $0; $recv($i) = ($recv($i) + $1).asInstanceOf[$T0] }"),
              ("incr", 1) -> RT.Template(
                "{ var $i = 0; while ($i < $recv.size) { $recv($i) = ($recv($i).asInstanceOf[Int] + $0).asInstanceOf[$T0]; $i += 1 } }"
              )
            )
            Map(
              "com.badlogic.gdx.utils.FloatArray" -> common,
              "com.badlogic.gdx.utils.ShortArray" -> (common ++ Map[(String, Int), balticporter.transform.CollectionsTransform.RetargetRewrite](("add", 1) -> RT.Template("$recv.add($0.toShort)"))),
              "com.badlogic.gdx.utils.IntArray" -> common,
              "com.badlogic.gdx.utils.BooleanArray" -> common,
              "com.badlogic.gdx.utils.LongArray" -> common
            )
          }
        )
      ),
      // `@Null -> lowlevel.Nullable` on core's entry: merges with lls's instance (`Only` union), so an
      // override of a base member the base retyped (`SnapshotArray.replaceFirst`, 2 `E120` name
      // clashes after erasure) moves with its component; ahead of `enrich`, whose value-map templates
      // are written against the nullable API.
      "nullability" -> List(
        new balticporter.transform.NullabilityTransform(
          annotations = Set("com.badlogic.gdx.utils.Null"),
          target = balticporter.transform.NullabilityTransform.Target.Named("lowlevel.Nullable"),
          scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx")),
          deriveMembers = sel("derive")
        )
      ),
      // lls's added API on core's own collections, and the factories core's subclasses of lls's
      // types must declare themselves (`LibgdxEnrich`).
      "enrich" -> List(LibgdxEnrich.transform(w = true, n = sel("nullability"))),
      // no runtime reflection: the reflective `Json` and the `reflect` package go (types below), the
      // one class lookup by name becomes a table (`AssetTypeRegistry`, injected), and `ClassReflection`'s
      // statics are `java.lang.Class`'s own — the full port's policy, lifted (`LibgdxPolicy`).
      // the classpath probe respelled for Scala Native — the full port's own value (`LibgdxPolicy.ClasspathProbeCalls`)
      "net" -> List(new balticporter.transform.CallSiteSubstitutionTransform(LibgdxPolicy.ClasspathProbeCalls)),
      // sge's renames that need no injection: `Disposable -> java.lang.AutoCloseable` (`dispose` ->
      // `close`, whole override component), and the two member renames the full port carries
      // (`InputEvent.type` -> `eventType`, `List.toString(T)` -> `itemToString`: java overloads
      // `Object.toString`, scala reads a clash).
      "renames" -> List(
        new balticporter.transform.TypeRedirectTransform(
          redirects = Map(
            "com.badlogic.gdx.utils.Disposable" -> "java.lang.AutoCloseable",
            // sge's `Comparable -> Ordered[T]` (`compareTo -> compare`), the two core classes it reshaped (their migration notes);
            // scoped so `Sort`'s `T extends Comparable` bounds keep java's type
            "java.lang.Comparable" -> "scala.math.Ordered"
          ),
          memberRenames = Map(
            "com.badlogic.gdx.utils.Disposable" -> Map("dispose" -> "close"),
            // `Comparable` is EXTERNAL: the hits are the owned overrides anchored on `Comparable#compareTo`, which the redirect detaches
            "java.lang.Comparable" -> Map("compareTo" -> "compare")
          ),
          // `Attributes` is also a `java.util.Comparator`, whose surface the engine keeps deliberately UNKNOWN (a closed platform
          // row would be a silent under-refusal if a release grew it); this port states the JDK 8..25 instance surface it relies on,
          // so the graph can rule `compareTo` out there instead of anchoring the rename on it
          external = balticporter.tir.ExternalSurface.default ++ balticporter.tir.ExternalSurface(
            Map(
              "java.util.Comparator" -> Set(
                balticporter.tir.ExternalSurface.Member("compare", 2),
                balticporter.tir.ExternalSurface.Member("equals", 1),
                balticporter.tir.ExternalSurface.Member("reversed", 0),
                balticporter.tir.ExternalSurface.Member("thenComparing", 1),
                balticporter.tir.ExternalSurface.Member("thenComparing", 2),
                balticporter.tir.ExternalSurface.Member("thenComparingInt", 1),
                balticporter.tir.ExternalSurface.Member("thenComparingLong", 1),
                balticporter.tir.ExternalSurface.Member("thenComparingDouble", 1)
              )
            )
          ),
          scopes = Map(
            // the OVERRIDE COMPONENT: `Attribute` and every `attributes.*` subclass, `Attributes`, `TextureDescriptor` — the classes master spells
            // `Ordered`; `Shader`/`DefaultShader`/`VertexAttributes` keep java's `compareTo` there and are not named
            "java.lang.Comparable" -> balticporter.tir.RuleScope.Only(
              Set(
                "com.badlogic.gdx.graphics.g3d.Attribute",
                "com.badlogic.gdx.graphics.g3d.Attributes",
                "com.badlogic.gdx.graphics.g3d.attributes",
                "com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor"
              )
            )
          )
        ),
        new balticporter.transform.MemberRenameTransform(
          derive = derive,
          renames = Map(
            "com.badlogic.gdx.scenes.scene2d.InputEvent#type" -> "eventType",
            "com.badlogic.gdx.scenes.scene2d.ui.List#toString(T)" -> "itemToString",
            // sge's vector spellings (`Vectors.scala`): the whole override component moves with `Vector`.
            // sge's names for java's `type` accessors: `fileType`, `graphicsType`, `applicationType`, `inputType`
            "com.badlogic.gdx.files.FileHandle#type" -> "fileType",
            "com.badlogic.gdx.Graphics#getType" -> "getGraphicsType",
            "com.badlogic.gdx.Application#getType" -> "getApplicationType",
            "com.badlogic.gdx.input.NativeInputConfiguration#getType" -> "getInputType",
            "com.badlogic.gdx.input.NativeInputConfiguration#setType" -> "setInputType",
            "com.badlogic.gdx.math.Vector#len" -> "length",
            "com.badlogic.gdx.math.Vector#len2" -> "lengthSq",
            "com.badlogic.gdx.math.Vector#dst" -> "distance",
            "com.badlogic.gdx.math.Vector#dst2" -> "distanceSq",
            "com.badlogic.gdx.math.Vector#scl" -> "scale",
            "com.badlogic.gdx.math.Vector#nor" -> "normalize"
          )
        ),
        // (the classes' own overloads — `dst(x, y)`, static `len(x, y)` — keep java's names: a second
        // key on the same component REFUSES the whole rename, 3 -> 23 policy rows; counted residue)
        // screenWidth body substituted BEFORE globals->implicits so the phase sees no Gdx.graphics read
        new balticporter.transform.MethodBodyTransform(
          Map(
            "com.badlogic.gdx.graphics.g3d.particles.ParticleShader$Setters#screenWidth" ->
              "new sge.graphics.g3d.shaders.BaseShader.GlobalSetter() { override def set(shader: sge.graphics.g3d.shaders.BaseShader, inputID: scala.Int, renderable: sge.graphics.g3d.Renderable, combinedAttributes: sge.graphics.g3d.Attributes): scala.Unit = { shader.set(inputID, shader.sgeContext.graphics.width.asInstanceOf[scala.Float]) } }"
          )
        )
      ),

      // the implicit `Sge` context instead of the `Gdx` globals: the full port's holder policy lifted
      // verbatim (attach on the CLASS, read by `summon`, refuse at the boundary, two lazy statics);
      // the context type is the injected `sge.Sge`. Late by design: every constructor moves.
      "context" -> List(
        new balticporter.transform.GlobalsToImplicitsTransform(
          holders = List(
            balticporter.transform.ContextHolder(
              holder = "com.badlogic.gdx.Gdx",
              context = balticporter.transform.ContextType.Injected("sge.Sge"),
              members = Map(
                "app" -> "application",
                "graphics" -> "graphics",
                "audio" -> "audio",
                "input" -> "input",
                "files" -> "files",
                "net" -> "net",
                // the GL statics, two hops through the service that owns them — as GETTER CALLS until the
                // property step renames them (`graphics.gl20` in the full port).
                "gl" -> glPath(sel, "20"),
                "gl20" -> glPath(sel, "20"),
                "gl30" -> glPath(sel, "30"),
                "gl31" -> glPath(sel, "31"),
                "gl32" -> glPath(sel, "32")
              ),
              attach = balticporter.transform.ContextAttach.Class,
              reader = balticporter.transform.ContextReader.Summon,
              boundary = balticporter.transform.ContextBoundary.Refuse,
              // `Pixmap.dispose` is left OUT of the closure so the class takes no clause (sge's `Pixmap` is
              // context-free, `Pixmap(w, h, format)` in the demos; its statics still take one); the
              // `Gdx.app.error` inside stays a counted residual global read.
              scope = balticporter.tir.RuleScope.Everywhere(Set("com.badlogic.gdx.graphics.Pixmap#dispose")),
              // sge's `GLProfiler(graphics)` takes no context: the GL statics it swaps are read off the
              // `graphics` it was handed (the `through` mapping).
              through = Map("com.badlogic.gdx.graphics.profiling.GLProfiler" -> "graphics"),
              // sge's `FileHandle(file, type, externalStoragePath)` is context-free: java's one read of
              // `Gdx.files.getExternalStoragePath()` (in `file()`) is a value given at construction.
              capture = Map(
                "com.badlogic.gdx.files.FileHandle" ->
                  "files.getExternalStoragePath() as externalStoragePath: lowlevel.Nullable = lowlevel.Nullable.empty"
              ),
              forceThread = Set("com.badlogic.gdx.graphics.g3d.shaders.BaseShader"),
              sites = Map(
                "com.badlogic.gdx.scenes.scene2d.ui.TextField#DEFAULT_ONSCREEN_KEYBOARD" -> balticporter.transform.ContextSite.LazyInit,
                "com.badlogic.gdx.scenes.scene2d.ui.Table#cellPool" -> balticporter.transform.ContextSite.LazyInit
              )
            )
          )
        )
      ),
      // `Seconds`: a frame delta is not a bare `Float` (sge's opaque type, injected from sge's own
      // file). Seeded at the two producers on `Graphics`; the phase propagates along pure moves
      // (`render(delta)`, `act(delta)`) and coerces at the boundary.
      "seconds" -> List(
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.utils.Seconds",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.utils.Seconds", wrapName = "apply", unwrapName = "toFloat"),
            derive = sel("derive"),
            hints = Set("com.badlogic.gdx.Graphics#getDeltaTime", "com.badlogic.gdx.Graphics#getRawDeltaTime"),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Float,
            // core's own declarations, never the base's: a shared int utility (`MathUtils.isPowerOfTwo`)
            // is a HUB the symmetric propagation would otherwise ride into unrelated ints (a touch bitmask).
            scope = balticporter.tir.RuleScope.Everywhere(Set.empty)
          )
        )
      ),
      // `Pool` as sge's TRAIT (injected from sge's own files, with `Pool.Default`, `Pool.Flushable`
      // and the `Poolable` type class the demos use): java's `Pool`/`DefaultPool`/`FlushablePool` go,
      // their references re-point, and a subclass's constructor arguments become the trait's
      // abstract vals (the full port's `ClassToTraitTransform` specs).
      "pool" -> List(
        new balticporter.transform.TypeRedirectTransform(
          // `FlushablePool` stays java's CLASS over the injected trait (as in the full port): a subclass
          // with several constructors passing different `super(...)` arguments cannot map onto one val.
          redirects = Map(
            "com.badlogic.gdx.utils.DefaultPool" -> "sge.utils.Pool.Default",
            "com.badlogic.gdx.utils.DefaultPool$PoolSupplier" -> "scala.Function0"
          ), // `T get()` is `() => A`
          memberRenames = Map("com.badlogic.gdx.utils.DefaultPool$PoolSupplier" -> Map("get" -> "apply"))
        ),
        new balticporter.transform.ClassToTraitTransform(specs = Map("com.badlogic.gdx.utils.Pool" -> PoolMappings))
      ),
      // `Pixels`: a screen coordinate or size is not a bare `Int` (sge's opaque type, injected).
      // Seeded at the producers on `Graphics` and `Input` and at the two resize callbacks; the
      // phase propagates along pure moves and coerces at the boundary.
      "pixels" -> List(
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.Pixels",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.Pixels", wrapName = "apply", unwrapName = "toInt"),
            derive = sel("derive"),
            hints = Set(
              "com.badlogic.gdx.Graphics#getWidth",
              "com.badlogic.gdx.Graphics#getHeight",
              "com.badlogic.gdx.Graphics#getBackBufferWidth",
              "com.badlogic.gdx.Graphics#getBackBufferHeight",
              "com.badlogic.gdx.Graphics#getSafeInsetLeft",
              "com.badlogic.gdx.Graphics#getSafeInsetTop",
              "com.badlogic.gdx.Graphics#getSafeInsetBottom",
              "com.badlogic.gdx.Graphics#getSafeInsetRight",
              "com.badlogic.gdx.Input#getX",
              "com.badlogic.gdx.Input#getY",
              "com.badlogic.gdx.Input#getDeltaX",
              "com.badlogic.gdx.Input#getDeltaY",
              // a PARAMETER seed is `owner#method#param`
              "com.badlogic.gdx.ApplicationListener#resize#width",
              "com.badlogic.gdx.ApplicationListener#resize#height",
              "com.badlogic.gdx.Screen#resize#width",
              "com.badlogic.gdx.Screen#resize#height"
            )
              // sge types these GL20 parameters in `Pixels` (its ANGLE bindings implement that surface)
              ++ Set(
                "glCompressedTexImage2D#width",
                "glCompressedTexImage2D#height",
                "glCompressedTexSubImage2D#xoffset",
                "glCompressedTexSubImage2D#yoffset",
                "glCompressedTexSubImage2D#width",
                "glCompressedTexSubImage2D#height",
                "glCopyTexImage2D#x",
                "glCopyTexImage2D#y",
                "glCopyTexImage2D#width",
                "glCopyTexImage2D#height",
                "glCopyTexSubImage2D#xoffset",
                "glCopyTexSubImage2D#yoffset",
                "glCopyTexSubImage2D#x",
                "glCopyTexSubImage2D#y",
                "glCopyTexSubImage2D#width",
                "glCopyTexSubImage2D#height",
                "glReadPixels#x",
                "glReadPixels#y",
                "glReadPixels#width",
                "glReadPixels#height",
                "glScissor#x",
                "glScissor#y",
                "glScissor#width",
                "glScissor#height",
                "glTexImage2D#width",
                "glTexImage2D#height",
                "glTexSubImage2D#xoffset",
                "glTexSubImage2D#yoffset",
                "glTexSubImage2D#width",
                "glTexSubImage2D#height",
                "glViewport#x",
                "glViewport#y",
                "glViewport#width",
                "glViewport#height",
                "glRenderbufferStorage#width",
                "glRenderbufferStorage#height"
              ).map("com.badlogic.gdx.graphics.GL20#" + _),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Int,
            // sge sizes a `Pixmap` in plain `Int` (image dimensions are not screen pixels; the demos write
            // `Pixmap(w, h, format)`): the flow stops at its declarations, the call sites coerce.
            scope = balticporter.tir.RuleScope.Everywhere(
              Set(
                "com.badlogic.gdx.graphics.Pixmap",
                // sge types GL20's sizes in `Pixels` and keeps GL30's plain `Int`: the flow stops at these members, the calls coerce
                "com.badlogic.gdx.graphics.GL30#glTexImage2D",
                "com.badlogic.gdx.graphics.GL30#glTexImage3D",
                "com.badlogic.gdx.graphics.GL30#glTexSubImage2D",
                "com.badlogic.gdx.graphics.GL30#glTexSubImage3D",
                "com.badlogic.gdx.graphics.GL30#glCopyTexSubImage3D",
                "com.badlogic.gdx.graphics.GL30#glBlitFramebuffer",
                "com.badlogic.gdx.graphics.GL30#glRenderbufferStorageMultisample",
                // sge sizes ETC1 in plain Int throughout (its JNI-shaped statics answer through the contract)
                "com.badlogic.gdx.graphics.glutils.ETC1",
                // sge sizes a NinePatch in plain Int (the flow had reached `left`/`right` and not `top`/`bottom`)
                "com.badlogic.gdx.graphics.g2d.NinePatch"
              )
            )
          )
        )
      ),
      // sge's helper API the demos use: the `gl` alias, `rendering { … }` around `begin`/`end`,
      // class-tag `load` and a `Nullable` `get` on the asset manager.
      "helpers" -> List(
        new balticporter.transform.AddMembersTransform(
          Map(
            "com.badlogic.gdx.Graphics" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "gl",
                0,
                "def gl: sge.graphics.GL20 = gl20",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.Graphics#gl"),
                Some("sge's `graphics.gl` alias of the GL20 property"),
                false
              )
            ),
            "com.badlogic.gdx.graphics.g2d.Batch" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "rendering",
                1,
                "inline def rendering[A](inline body: => A): A = {{ begin(); try body finally end() }}",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g2d.Batch#rendering"),
                Some("sge's `rendering {{ … }}` around `begin()`/`end()`"),
                false
              )
            ),
            "com.badlogic.gdx.graphics.g3d.ModelBatch" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "rendering",
                2,
                "inline def rendering[A](cam: sge.graphics.Camera)(inline body: => A): A = {{ begin(cam); try body finally end() }}",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.ModelBatch#rendering"),
                Some("sge's `rendering(camera) {{ … }}` around `begin(cam)`/`end()`"),
                false
              )
            ),
            // sge's `OrthogonalTiledMapRenderer(map, unitScale, batch, ownsBatch)`: java's three-argument
            // constructor with the ownership flag it fixes at `false` made explicit.
            "com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                4,
                "def this(map: sge.maps.tiled.TiledMap, unitScale: scala.Float, batch: sge.graphics.g2d.Batch, ownsBatch: scala.Boolean)(using sge.Sge) = { this(map, unitScale, batch); this.ownsBatch = ownsBatch }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer#<init>"),
                Some("sge's four-argument constructor: the batch-ownership flag made explicit"),
                false
              )
            ),
            // java's `T...` is emitted `Array[T]`; sge spells these four as repeated parameters and the
            // demos call them so — one overload each, until the varargs mechanism lands.
            "com.badlogic.gdx.graphics.g3d.Material" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                1,
                "def this(attributes: sge.graphics.g3d.Attribute*) = this(attributes.toArray)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.Material#<init>"),
                Some("sge's repeated-parameter spelling of java's `T...` (the port emits `Array[T]`)"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                2,
                "def this(id: java.lang.String, attributes: sge.graphics.g3d.Attribute*) = this(id, attributes.toArray)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.Material#<init>"),
                Some("sge's repeated-parameter spelling of java's `T...` (the port emits `Array[T]`)"),
                false
              )
            ),
            "com.badlogic.gdx.graphics.Mesh" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "this",
                3,
                "def this(isStatic: scala.Boolean, maxVertices: scala.Int, maxIndices: scala.Int)(attributes: sge.graphics.VertexAttribute*)(using sge.Sge) = this(isStatic, maxVertices, maxIndices, attributes.toArray)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.Mesh#<init>"),
                Some("sge's repeated-parameter spelling of java's `T...` (the port emits `Array[T]`)"),
                false
              )
            ),
            "com.badlogic.gdx.math.Bezier" -> List(
              // a secondary constructor cannot unify the class's F-bounded `T` with its own inside a
              // self-invocation (scalac 3.8), so the repeated-points constructor is the companion's `apply`
              balticporter.transform.AddMembersTransform.MemberSpec(
                "apply",
                1,
                "def apply[T <: sge.math.Vector[T]](points: T*)(using lowlevel.MkArray[T]): sge.math.Bezier[T] = { val b = new sge.math.Bezier[T](); b.set(points*); b }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Bezier#apply"),
                Some("sge's repeated-parameter constructor, as the companion's `apply`"),
                true
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "set",
                1,
                "def set(points: T*): Bezier[?] = { val d = new lowlevel.util.DynamicArray[T](); points.foreach(p => d.add(p)); set(d, 0, points.size) }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.math.Bezier#set"),
                Some("sge's repeated-parameter spelling of java's `T...` (the port emits `Array[T]`)"),
                false
              )
            ),
            "com.badlogic.gdx.utils.TextFormatter" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "format",
                2,
                "def format(pattern: java.lang.String, args: java.lang.Object*): java.lang.String = format(pattern, args.toArray)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.utils.TextFormatter#format"),
                Some("sge's repeated-parameter spelling of java's `T...` (the port emits `Array[T]`)"),
                false
              )
            ),
            // sge nests the five argument-free resolvers in `FileHandleResolver`'s companion
            // (`AssetManager(FileHandleResolver.Internal())` in the demos); each is the java class under sge's name.
            "com.badlogic.gdx.assets.loaders.FileHandleResolver" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "Absolute",
                0,
                "class Absolute(using sge.Sge) extends sge.assets.loaders.resolvers.AbsoluteFileHandleResolver",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.loaders.FileHandleResolver#Absolute"),
                Some("sge nests the resolvers in the companion: `FileHandleResolver.Absolute()`"),
                true
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "Classpath",
                0,
                "class Classpath(using sge.Sge) extends sge.assets.loaders.resolvers.ClasspathFileHandleResolver",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.loaders.FileHandleResolver#Classpath"),
                Some("sge nests the resolvers in the companion: `FileHandleResolver.Classpath()`"),
                true
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "External",
                0,
                "class External(using sge.Sge) extends sge.assets.loaders.resolvers.ExternalFileHandleResolver",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.loaders.FileHandleResolver#External"),
                Some("sge nests the resolvers in the companion: `FileHandleResolver.External()`"),
                true
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "Internal",
                0,
                "class Internal(using sge.Sge) extends sge.assets.loaders.resolvers.InternalFileHandleResolver",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.loaders.FileHandleResolver#Internal"),
                Some("sge nests the resolvers in the companion: `FileHandleResolver.Internal()`"),
                true
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "Local",
                0,
                "class Local(using sge.Sge) extends sge.assets.loaders.resolvers.LocalFileHandleResolver",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.loaders.FileHandleResolver#Local"),
                Some("sge nests the resolvers in the companion: `FileHandleResolver.Local()`"),
                true
              )
            ),
            "com.badlogic.gdx.assets.AssetManager" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "load",
                1,
                "def load[T <: java.lang.Object](fileName: java.lang.String)(using ct: scala.reflect.ClassTag[T]): scala.Unit = load(fileName, ct.runtimeClass.asInstanceOf[java.lang.Class[T]])",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.AssetManager#load"),
                Some("sge's class-tag `load[T](fileName)`"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "load",
                2,
                "def load[T <: java.lang.Object](fileName: java.lang.String, parameter: sge.assets.AssetLoaderParameters[T])(using ct: scala.reflect.ClassTag[T]): scala.Unit = load(fileName, ct.runtimeClass.asInstanceOf[java.lang.Class[T]], parameter)",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.assets.AssetManager#load"),
                Some("sge's class-tag `load[T](fileName, parameter)`"),
                false
              )
            ),
            "com.badlogic.gdx.graphics.g3d.shaders.BaseShader" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "sgeContext",
                0,
                "val sgeContext: sge.Sge = scala.Predef.summon[sge.Sge]",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.g3d.shaders.BaseShader#sgeContext"),
                Some("sge's sgeContext: Sge exposed to setters (replaces Gdx.graphics static)"),
                false
              )
            )
          )
        ),
        // sge's `Screen` gives every lifecycle member but `render` an empty default (a screen overrides
        // what it needs — the demos implement `show`/`render`/`resize`/`hide`/`close` only).
        new balticporter.transform.MethodBodyTransform(
          Map(
            "com.badlogic.gdx.Screen#show" -> "{}",
            "com.badlogic.gdx.Screen#resize" -> "{}",
            "com.badlogic.gdx.Screen#pause" -> "{}",
            "com.badlogic.gdx.Screen#resume" -> "{}",
            "com.badlogic.gdx.Screen#hide" -> "{}"
          )
        ),
        // sge returns empty DynamicArray instead of java's null from getDependencies (the reference
        // port's convention, PixmapLoader.scala:8); callers call .size on the result without a null check.
        new balticporter.transform.MethodBodyTransform(
          Map(
            "com.badlogic.gdx.assets.loaders.ShaderProgramLoader#getDependencies" -> "new lowlevel.util.DynamicArray[sge.assets.AssetDescriptor[?]]()",
            "com.badlogic.gdx.assets.loaders.I18NBundleLoader#getDependencies" -> "new lowlevel.util.DynamicArray[sge.assets.AssetDescriptor[?]]()",
            "com.badlogic.gdx.assets.loaders.CubemapLoader#getDependencies" -> "new lowlevel.util.DynamicArray[sge.assets.AssetDescriptor[?]]()",
            "com.badlogic.gdx.assets.loaders.PixmapLoader#getDependencies" -> "new lowlevel.util.DynamicArray[sge.assets.AssetDescriptor[?]]()",
            "com.badlogic.gdx.assets.loaders.SoundLoader#getDependencies" -> "new lowlevel.util.DynamicArray[sge.assets.AssetDescriptor[?]]()",
            "com.badlogic.gdx.assets.loaders.MusicLoader#getDependencies" -> "new lowlevel.util.DynamicArray[sge.assets.AssetDescriptor[?]]()",
            "com.badlogic.gdx.assets.loaders.TextureLoader#getDependencies" -> "new lowlevel.util.DynamicArray[sge.assets.AssetDescriptor[?]]()",
            "com.badlogic.gdx.assets.loaders.ParticleEffectLoader#getDependencies" -> "{ val deps = new lowlevel.util.DynamicArray[sge.assets.AssetDescriptor[?]](); if ((param != null) && (!param.atlasFile.isEmpty)) { deps.add(new sge.assets.AssetDescriptor[sge.graphics.g2d.TextureAtlas](param.atlasFile.orNull, classOf[sge.graphics.g2d.TextureAtlas]).asInstanceOf[sge.assets.AssetDescriptor[?]]) }; deps }",
            // BinaryHeap.contains: iterate only up to size, not the full backing array (sge fix)
            "com.badlogic.gdx.utils.BinaryHeap#contains" ->
              "{ if (node == null) { throw new java.lang.IllegalArgumentException(\"node cannot be null.\") }; var i = 0; if (identity) { while (i < this.size) { if (this.nodes(i) eq node) { return true }; i += 1 } } else { while (i < this.size) { if (this.nodes(i).equals(node.asInstanceOf[java.lang.Object])) { return true }; i += 1 } }; return false }",
            // ctor-funnel bug: ResourceData(T) skips field init — use no-arg ctor + set resource. The document
            // is sge's `ResourceData.toJson` written through jsoniter (`LegacyJson`, the reflection step's
            // stand-in for the reflective `Json`, refuses at run time on every row); `ParticleEffectLoader#getDependencies`
            // (json step) reads the same document back.
            "com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader#save" ->
              """{
                |  val data = new sge.graphics.g3d.particles.ResourceData[sge.graphics.g3d.particles.ParticleEffect]()
                |  data.resource = lowlevel.Nullable(effect)
                |  effect.save(parameter.manager, data.asInstanceOf[sge.graphics.g3d.particles.ResourceData[java.lang.Object]])
                |  if (!parameter.batches.isEmpty) {
                |    for (batch <- parameter.batches.get) {
                |      var save: scala.Boolean = false
                |      scala.util.boundary { for (controller <- effect.controllers) {
                |        if (controller.renderer.isCompatible(batch)) { save = true; scala.util.boundary.break(()) } else ()
                |      } }
                |      if (save) {
                |        batch.asInstanceOf[sge.graphics.g3d.particles.batches.ParticleBatch[sge.graphics.g3d.particles.renderers.ParticleControllerRenderData]].save(parameter.manager, data.asInstanceOf[sge.graphics.g3d.particles.ResourceData[sge.graphics.g3d.particles.renderers.ParticleControllerRenderData]].asInstanceOf[sge.graphics.g3d.particles.ResourceData[java.lang.Object]])
                |      } else ()
                |    }
                |  } else ()
                |  val jsonAst = data.toJson
                |  val config = if (parameter.prettyPrint) com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig.withIndentionStep(2) else com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig
                |  parameter.file.writeString(sge.utils.writeToString[sge.utils.Json](jsonAst, config)(using hearth.kindlings.jsoniterjson.codec.JsonCodec.jsonValueCodec), false)
                |}""".stripMargin
          )
        ),
        // `AssetManager.get` answers `Nullable` in sge (the demos write `.get`); java throws on a miss.
        new balticporter.transform.NullabilityTransform(
          annotations = Set.empty,
          target = balticporter.transform.NullabilityTransform.Target.Named("lowlevel.Nullable"),
          scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx")),
          nullableMembers = Set(
            "com.badlogic.gdx.assets.AssetManager#get",
            // parameters sge accepts as `Nullable` where java wrote no annotation (the demos pass `Nullable.empty`)
            "com.badlogic.gdx.graphics.g3d.ModelBatch#<init>#context",
            "com.badlogic.gdx.graphics.g3d.ModelBatch#<init>#shaderProvider",
            "com.badlogic.gdx.graphics.g3d.ModelBatch#<init>#sorter",
            "com.badlogic.gdx.utils.Clipboard#setContents#content",
            "com.badlogic.gdx.maps.tiled.TiledMapTileLayer#setCell#cell",
            // sge wraps these fields in Nullable; the reference has them private so the deriver didn't see them
            "com.badlogic.gdx.scenes.scene2d.Action#target",
            "com.badlogic.gdx.scenes.scene2d.Action#actor",
            "com.badlogic.gdx.scenes.scene2d.Action#pool"
          )
        )
      ),
      // sge's audio opaques (`Volume`, `Pitch`, `Pan`, `SoundId`), each fenced to the files sge keeps it in.
      "audio" -> List(
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.audio.Volume",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.audio.Volume", wrapName = "unsafeMake", unwrapName = "toFloat"),
            hints = Set(
              "com.badlogic.gdx.audio.Sound#play#volume",
              "com.badlogic.gdx.audio.Sound#loop#volume",
              "com.badlogic.gdx.audio.Sound#setVolume#volume",
              "com.badlogic.gdx.audio.Sound#setPan#volume",
              "com.badlogic.gdx.audio.Music#setVolume#volume",
              "com.badlogic.gdx.audio.Music#getVolume",
              "com.badlogic.gdx.audio.Music#setPan#volume",
              "com.badlogic.gdx.audio.AudioDevice#setVolume#volume"
            ),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Float,
            scope = balticporter.tir.RuleScope.Only(
              Set(
                "com.badlogic.gdx.audio.Sound",
                "com.badlogic.gdx.audio.Music",
                "com.badlogic.gdx.audio.AudioDevice",
                "com.badlogic.gdx.Input"
              )
            )
          )
        ),
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.audio.Pitch",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.audio.Pitch", wrapName = "unsafeMake", unwrapName = "toFloat"),
            hints = Set(
              "com.badlogic.gdx.audio.Sound#play#pitch",
              "com.badlogic.gdx.audio.Sound#loop#pitch",
              "com.badlogic.gdx.audio.Sound#setPitch#pitch"
            ),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Float,
            scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx.audio.Sound"))
          )
        ),
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.audio.Pan",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.audio.Pan", wrapName = "unsafeMake", unwrapName = "toFloat"),
            hints = Set(
              "com.badlogic.gdx.audio.Sound#play#pan",
              "com.badlogic.gdx.audio.Sound#loop#pan",
              "com.badlogic.gdx.audio.Sound#setPan#pan",
              "com.badlogic.gdx.audio.Music#setPan#pan"
            ),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Float,
            scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx.audio.Sound", "com.badlogic.gdx.audio.Music"))
          )
        ),
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.audio.SoundId",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.audio.SoundId", wrapName = "apply", unwrapName = "toLong"),
            hints = Set(
              "com.badlogic.gdx.audio.Sound#play",
              "com.badlogic.gdx.audio.Sound#loop",
              "com.badlogic.gdx.audio.Sound#stop#soundId",
              "com.badlogic.gdx.audio.Sound#pause#soundId",
              "com.badlogic.gdx.audio.Sound#resume#soundId",
              "com.badlogic.gdx.audio.Sound#setLooping#soundId",
              "com.badlogic.gdx.audio.Sound#setPitch#soundId",
              "com.badlogic.gdx.audio.Sound#setVolume#soundId",
              "com.badlogic.gdx.audio.Sound#setPan#soundId"
            ),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Long,
            scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx.audio.Sound"))
          )
        ),
        // sge's opaque playback position (seconds into the track): `Music.position`/`setPosition`, read off sge
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.audio.Position",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.audio.Position", wrapName = "apply", unwrapName = "toFloat"),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Float
          )
        )
      ),
      // sge's time opaques (`Millis`, `Nanos`) over `TimeUtils`, fenced to sge's files.
      "time" -> List(
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.utils.Millis",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.utils.Millis", wrapName = "apply", unwrapName = "toLong"),
            hints = Set(
              "com.badlogic.gdx.utils.TimeUtils#millis",
              "com.badlogic.gdx.utils.TimeUtils#nanosToMillis",
              "com.badlogic.gdx.utils.TimeUtils#millisToNanos#millis",
              "com.badlogic.gdx.utils.TimeUtils#timeSinceMillis",
              "com.badlogic.gdx.utils.TimeUtils#timeSinceMillis#prevTime"
            ),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Long,
            scope = balticporter.tir.RuleScope.Only(
              Set(
                "com.badlogic.gdx.utils.TimeUtils",
                "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile",
                "com.badlogic.gdx.scenes.scene2d.utils.ClickListener",
                "com.badlogic.gdx.assets.AssetManager"
              )
            )
          )
        ),
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.utils.Nanos",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.utils.Nanos", wrapName = "apply", unwrapName = "toLong"),
            hints = Set(
              "com.badlogic.gdx.utils.TimeUtils#nanoTime",
              "com.badlogic.gdx.utils.TimeUtils#millisToNanos",
              "com.badlogic.gdx.utils.TimeUtils#nanosToMillis#nanos",
              "com.badlogic.gdx.utils.TimeUtils#timeSinceNanos",
              "com.badlogic.gdx.utils.TimeUtils#timeSinceNanos#prevTime"
            ),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Long,
            scope = balticporter.tir.RuleScope.Only(
              Set(
                "com.badlogic.gdx.utils.TimeUtils",
                "com.badlogic.gdx.scenes.scene2d.utils.ClickListener",
                "com.badlogic.gdx.input.GestureDetector",
                "com.badlogic.gdx.input.RemoteInput",
                "com.badlogic.gdx.utils.PerformanceCounters",
                "com.badlogic.gdx.utils.PerformanceCounter",
                "com.badlogic.gdx.InputEventQueue",
                "com.badlogic.gdx.Input",
                "com.badlogic.gdx.graphics.FPSLogger",
                "com.badlogic.gdx.assets.AssetLoadingTask"
              )
            )
          )
        )
      ),
      // sge's typed GL enums (`GLEnum.scala`, injected) at the GL20 parameters the demos reach; the raw
      // `GL_*` constants stay java's `inline val`s (a constant is never a seed) and wrap at the call.
      "glenum" -> (List(
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.graphics.EnableCap",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.graphics.EnableCap", wrapName = "apply", unwrapName = "toInt"),
            hints = Set(
              "com.badlogic.gdx.graphics.GL20#glEnable#cap",
              "com.badlogic.gdx.graphics.GL20#glDisable#cap",
              "com.badlogic.gdx.graphics.GL20#glIsEnabled#cap"
            ),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Int,
            scope = balticporter.tir.RuleScope.Everywhere(Set.empty),
            derive = derive
          )
        ),
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.graphics.PrimitiveMode",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.graphics.PrimitiveMode", wrapName = "apply", unwrapName = "toInt"),
            hints = Set(
              "GL20#glDrawArrays#mode",
              "GL20#glDrawElements#mode",
              "GL30#glDrawRangeElements#mode",
              "GL30#glBeginTransformFeedback#primitiveMode",
              "GL30#glDrawArraysInstanced#mode",
              "GL30#glDrawElementsInstanced#mode",
              "GL31#glDrawArraysIndirect#mode",
              "GL31#glDrawElementsIndirect#mode",
              "GL32#glDrawElementsBaseVertex#mode",
              "GL32#glDrawRangeElementsBaseVertex#mode",
              "GL32#glDrawElementsInstancedBaseVertex#mode"
            ).map("com.badlogic.gdx.graphics." + _),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Int,
            scope = balticporter.tir.RuleScope.Everywhere(Set.empty),
            derive = derive
          )
        ),
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.graphics.CompareFunc",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.graphics.CompareFunc", wrapName = "apply", unwrapName = "toInt"),
            hints = Set(
              "com.badlogic.gdx.graphics.GL20#glDepthFunc#func",
              "com.badlogic.gdx.graphics.GL20#glStencilFunc#func",
              "com.badlogic.gdx.graphics.GL20#glStencilFuncSeparate#func"
            ),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Int,
            scope = balticporter.tir.RuleScope.Everywhere(Set.empty),
            derive = derive
          )
        ),
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.graphics.ClearMask",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.graphics.ClearMask", wrapName = "apply", unwrapName = "toInt"),
            hints = Set("com.badlogic.gdx.graphics.GL20#glClear#mask", "com.badlogic.gdx.graphics.GL30#glBlitFramebuffer#mask"),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Int,
            scope = balticporter.tir.RuleScope.Everywhere(Set.empty),
            derive = derive
          )
        )
      ) ++ moreGlEnums),
      // `WorldUnits`: world-space sizes are not bare `Float`s (sge's opaque type, injected from sge's
      // own file). Seeded at the viewport's and the camera's world-size fields.
      "worldunits" -> List(
        opaque(
          balticporter.tir.OpaqueSpec(
            fqn = "com.badlogic.gdx.WorldUnits",
            target = balticporter.tir.OpaqueSpec.Target.Existing(typeFqn = "sge.WorldUnits", wrapName = "apply", unwrapName = "toFloat"),
            derive = sel("derive"),
            hints = Set(
              "com.badlogic.gdx.utils.viewport.Viewport#worldWidth",
              "com.badlogic.gdx.utils.viewport.Viewport#worldHeight",
              "com.badlogic.gdx.graphics.Camera#viewportWidth",
              "com.badlogic.gdx.graphics.Camera#viewportHeight"
            ),
            underlying = balticporter.tir.OpaqueSpec.Primitive.Float,
            // sge keeps world units INSIDE the viewports and the cameras (13 files; `Stage`, the shadow
            // light and the math library wrap at the call): the flow is fenced there, the call sites
            // coerce — unfenced it reached `Sprite.scaleX` and `Batch.draw`, 237 then 85 errors.
            scope = balticporter.tir.RuleScope.Only(
              Set(
                "com.badlogic.gdx.utils.viewport",
                "com.badlogic.gdx.graphics.Camera",
                "com.badlogic.gdx.graphics.OrthographicCamera",
                "com.badlogic.gdx.graphics.PerspectiveCamera"
              )
            )
          )
        )
      ),
      // properties and parenless getters: the full port's bean pairs and targets (`LibgdxPolicy`),
      // lifted by reference, on core's entry; the `Only` scope merges with lls's arity instance.
      "properties" -> List(
        // sge's own setter decisions the demos rely on (`game.screen = …`, `batch.projectionMatrix = …`):
        // configured pairs, which the behaviour-setter guard does not apply to (`Cell.setTile` is fluent:
        // a configured pair collapses it, the property's setter returns Unit, the chain is counted).
        new balticporter.transform.BeanPropertyTransform(
          LibgdxPolicy.beanPropertyPairs ++ Map(
            "com.badlogic.gdx.Game#screen" -> "getScreen/setScreen",
            "com.badlogic.gdx.graphics.g2d.Batch#projectionMatrix" -> "getProjectionMatrix/setProjectionMatrix",
            "com.badlogic.gdx.maps.tiled.TiledMapTileLayer$Cell#tile" -> "getTile/setTile"
          ),
          LibgdxPolicy.beanPropertyTargets,
          scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx")),
          derive = sel("derive")
        ),
        new balticporter.transform.NullaryArityTransform(
          scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx")),
          // sge's `clip.hasContents` — parenless although the body reads the platform clipboard
          force = Set("com.badlogic.gdx.utils.Clipboard#hasContents"),
          derive = sel("derive")
        )
      ),
      // sge's graphics API spellings the demos use: `ShapeRenderer.rect -> rectangle` (its four
      // overloads, one component) and the `drawing(type) { … }` helper around `begin`/`end`.
      "graphics" -> List(
        new balticporter.transform.MemberRenameTransform(renames = Map("com.badlogic.gdx.graphics.glutils.ShapeRenderer#rect" -> "rectangle")),
        new balticporter.transform.AddMembersTransform(
          Map(
            "com.badlogic.gdx.graphics.glutils.ShapeRenderer" -> List(
              balticporter.transform.AddMembersTransform.MemberSpec(
                "drawing",
                2,
                "inline def drawing[A](shapeType: ShapeRenderer.ShapeType)(inline body: => A): A = { begin(shapeType); try body finally end() }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.glutils.ShapeRenderer#drawing"),
                Some("sge's `drawing(type) { … }` around `begin`/`end`, `end` guaranteed"),
                false
              ),
              balticporter.transform.AddMembersTransform.MemberSpec(
                "drawing",
                1,
                "inline def drawing[A](inline body: => A): A = { begin(); try body finally end() }",
                balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.graphics.glutils.ShapeRenderer#drawing"),
                Some("sge's `drawing { … }` around `begin()`/`end` (auto shape type)"),
                false
              )
            )
          )
        )
      ),
      "reflection" -> List(
        new balticporter.transform.ClassTableTransform(
          Map(
            "com.badlogic.gdx.utils.reflect.ClassReflection#forName" ->
              "com.badlogic.gdx.graphics.g3d.particles.AssetTypeRegistry#classFor"
          )
        ),
        new balticporter.transform.StaticForwarderTransform(
          List(
            balticporter.transform.StaticForwarderTransform.Forwarder(
              wrapper = "com.badlogic.gdx.utils.reflect.ClassReflection",
              receiver = "java.lang.Class",
              members = Set(
                "getSimpleName",
                "isInstance",
                "isAssignableFrom",
                "isArray",
                "isEnum",
                "isInterface",
                "isPrimitive",
                "isAnnotation",
                "getComponentType"
              )
            )
          )
        )
      )
    )
  }

  /** `Pool(int initialCapacity, int max)` onto sge's trait: the two vals a subclass site overrides; a site passing no argument keeps the trait's defaults (java's, carried by the injected file).
    */
  val PoolMappings: List[balticporter.transform.ClassToTraitTransform.ParamMapping] = List(
    balticporter.transform.ClassToTraitTransform.ParamMapping(0, "initialCapacity"),
    balticporter.transform.ClassToTraitTransform.ParamMapping(1, "max")
  )

  /** per step, the TYPES it removes (each replaced by an injection or made dead by the step). */
  val stepTypeDrops: Map[String, Set[String]] = Map(
    "helpers" -> Set("com.badlogic.gdx.utils.TextFormatter", "com.badlogic.gdx.utils.Timer"),
    "json" -> Set("com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader", "com.badlogic.gdx.scenes.scene2d.ui.Skin"),
    // java's JNI-backed classes: sge's shared files (natives step) stand at the same names;
    // `GdxNativesLoader` has no reader and no sge counterpart
    "natives" -> Set(
      "com.badlogic.gdx.utils.BufferUtils",
      "com.badlogic.gdx.graphics.g2d.Gdx2DPixmap",
      "com.badlogic.gdx.graphics.glutils.ETC1",
      "com.badlogic.gdx.scenes.scene2d.utils.UIUtils",
      "com.badlogic.gdx.utils.GdxNativesLoader",
      // written against sge's Gdx2DPixmap/ETC1 (getters spelled sge's way): sge's own files
      "com.badlogic.gdx.graphics.Pixmap"
    ),
    "pool" -> Set("com.badlogic.gdx.utils.Pool", "com.badlogic.gdx.utils.DefaultPool"),
    // java's constants class; sge's own `opaque type Align` (injected) stands at the same name
    "align" -> Set("com.badlogic.gdx.utils.Align"),
    // java.util.concurrent.{ExecutorService, Future} and `Thread.yield` have no Scala.js javalib: the
    // three types are PLATFORM ROWS (`stepPlatformInjects`) — java's own on JVM/Native, libGDX's GWT
    // emulation on JS (the browser regression app was the first entry point to reach them, CI run
    // 35115474743: "Referring to non-existent class java.util.concurrent.Future", then
    // "non-existent method static java.lang.Thread.yield()" from `AssetManager.finishLoading`)
    "async" -> Set(
      "com.badlogic.gdx.utils.async.AsyncExecutor",
      "com.badlogic.gdx.utils.async.AsyncResult",
      "com.badlogic.gdx.utils.async.ThreadUtils"
    ),
    // DataBuffer reads FilterOutputStream.out which Scala.js javalib doesn't expose; nobody
    // references it; sge rewrote it entirely
    "extras" -> Set(
      "com.badlogic.gdx.utils.DataBuffer",
      "com.badlogic.gdx.utils.FloatArray",
      "com.badlogic.gdx.utils.ShortArray",
      "com.badlogic.gdx.utils.IntArray",
      "com.badlogic.gdx.utils.BooleanArray",
      "com.badlogic.gdx.utils.LongArray"
    ),
    // sge's `Music` (position/duration as `Position`, `onComplete(Music => Unit)`) replaces java's
    "audio" -> Set("com.badlogic.gdx.audio.Music"),
    // sge's `InputProcessor` (every callback defaulted to `false`, so `new InputProcessor {}` stands)
    "backend-desktop" -> Set("com.badlogic.gdx.InputProcessor"),
    // the JVM-only `HttpURLConnection` client: nothing in core references it; the backends supply
    // their own `Net` (sge's capability convention).
    "net" -> Set(
      "com.badlogic.gdx.net.NetJavaImpl",
      // sge's HTTP stack (`Net.httpClient`, an sttp client) replaces java's Net and its net helpers;
      // `HttpRequestBuilder` built java's HttpRequest and has no sge counterpart
      "com.badlogic.gdx.Net",
      "com.badlogic.gdx.net.HttpParametersUtils",
      "com.badlogic.gdx.net.HttpRequestBuilder",
      "com.badlogic.gdx.net.HttpRequestHeader",
      "com.badlogic.gdx.net.HttpResponseHeader",
      "com.badlogic.gdx.net.HttpStatus",
      "com.badlogic.gdx.net.NetJavaServerSocketImpl",
      "com.badlogic.gdx.net.NetJavaSocketImpl",
      "com.badlogic.gdx.net.ServerSocket",
      "com.badlogic.gdx.net.ServerSocketHints",
      "com.badlogic.gdx.net.Socket",
      "com.badlogic.gdx.net.SocketHints"
    ),
    "reflection" -> Set(
      "com.badlogic.gdx.utils.Json",
      // the `Class`-keyed static pool registry minted `ReflectionPool`s and registers, at class
      // initialisation, constructors that take the context; no core reader; sge has no `Pools`.
      "com.badlogic.gdx.utils.Pools",
      "com.badlogic.gdx.utils.ReflectionPool",
      "com.badlogic.gdx.utils.reflect.Annotation",
      "com.badlogic.gdx.utils.reflect.Field",
      "com.badlogic.gdx.utils.reflect.ArrayReflection",
      "com.badlogic.gdx.utils.reflect.ClassReflection",
      "com.badlogic.gdx.utils.reflect.Constructor",
      "com.badlogic.gdx.utils.reflect.Method",
      "com.badlogic.gdx.utils.reflect.ReflectionException"
    )
  ).withDefaultValue(Set.empty)

  /** Per step, the hand-written files it injects (the shared row), under `overrides` — sge's `sge-port/overrides`. */
  def stepInjects(overrides: Path): Map[String, List[Path]] = Map(
    "helpers" -> List(overrides.resolve("helpers")),
    // the reflection-free `Json`, `ReflectionException` and the asset-type registry
    "reflection" -> List(overrides.resolve("reflection")),
    "net" -> List(overrides.resolve("net/shared")),
    "mathunits" -> List(overrides.resolve("math")),
    "context" -> List(overrides.resolve("context")),
    "seconds" -> List(overrides.resolve("seconds")),
    "pool" -> List(overrides.resolve("pool")),
    "pixels" -> List(overrides.resolve("pixels")),
    "align" -> List(overrides.resolve("align")),
    "worldunits" -> List(overrides.resolve("worldunits")),
    "audio" -> List(overrides.resolve("audio")),
    "time" -> List(overrides.resolve("time")),
    "glenum" -> List(overrides.resolve("glenum")),
    // the backend steps' shared files only: sge hand-writes its platform layers (`sge/src/main/scala{jvm,js,native,desktop}`)
    "backend-jvm" -> List(overrides.resolve("backend-jvm/shared")),
    "natives" -> List(overrides.resolve("natives/shared")),
    "backend-desktop" -> List(overrides.resolve("backend-desktop/shared")),
    "json" -> List(overrides.resolve("json"))
  ).withDefaultValue(Nil)

  /** Per step, the files that differ per platform row (`PortManifest.platformDirs`): they land in `src_managed/<row>/scala`, which only that row compiles. */
  def stepPlatformInjects(overrides: Path): Map[String, Map[String, List[Path]]] = Map(
    // the async executor and its result: java's own (java.util.concurrent) on the threaded rows,
    // libGDX's GWT emulation (a task runs inside `submit`) on JS — see `stepTypeDrops("async")`
    "async" -> Map(
      "jvm" -> List(overrides.resolve("async/threaded")),
      "native" -> List(overrides.resolve("async/threaded")),
      "js" -> List(overrides.resolve("async/js"))
    )
  ).withDefaultValue(Map.empty)

  /** Per step, the members the step makes dead: the reflective `Class`-typed constructors the witness replaces (each has a portable twin).
    */
  val stepDrops: Map[String, Set[String]] = Map(
    "witness" -> Set(
      "com.badlogic.gdx.utils.SnapshotArray#<init>(boolean,int,Class)",
      "com.badlogic.gdx.utils.SnapshotArray#<init>(Class)",
      "com.badlogic.gdx.utils.DelayedRemovalArray#<init>(boolean,int,Class)",
      "com.badlogic.gdx.utils.DelayedRemovalArray#<init>(Class)",
      "com.badlogic.gdx.utils.Queue#<init>(int,Class)",
      "com.badlogic.gdx.graphics.g3d.particles.batches.BufferedParticleBatch#<init>(Class)"
    ),
    // sge's FileHandle has no temp-file statics (Scala.js's `java.io.File` has no `createTempFile`)
    "backend-jvm" -> Set("com.badlogic.gdx.files.FileHandle#tempFile", "com.badlogic.gdx.files.FileHandle#tempDirectory"),
    "reflection" -> Set(
      "com.badlogic.gdx.scenes.scene2d.ui.Skin#setEnabledReflection",
      "com.badlogic.gdx.scenes.scene2d.ui.Skin#findMethod",
      "com.badlogic.gdx.graphics.g3d.particles.ParallelArray$ChannelDescriptor#<init>(int,Class,int)"
    )
  ).withDefaultValue(Set.empty)

  val StepOrder: List[String] = List(
    "logging",
    "witness",
    "collections",
    "nullability",
    "enrich",
    "reflection",
    "net",
    "renames",
    "context",
    "seconds",
    "pool",
    "pixels",
    "keys",
    "align",
    "mathunits",
    "worldunits",
    "properties",
    "graphics",
    "helpers",
    "audio",
    "time",
    "glenum",
    "async",
    "webgl",
    "backend-jvm",
    "natives",
    "backend-desktop",
    "json",
    "derive",
    "extras",
    "classtags",
    "visibility"
  )

  /** the steps landed so far (measured, baselined). */
  val DefaultSteps: Set[String] = Set(
    "witness",
    "collections",
    "nullability",
    "enrich",
    "reflection",
    "net",
    "renames",
    "logging",
    "context",
    "seconds",
    "pool",
    "pixels",
    "keys",
    "align",
    "mathunits",
    "graphics",
    "properties",
    "worldunits",
    "helpers",
    "audio",
    "time",
    "glenum",
    "async",
    "webgl",
    "backend-jvm",
    "natives",
    "backend-desktop",
    "json",
    "derive",
    "extras",
    "classtags",
    "visibility"
  )

  /** The manifest of sge core: a dependent of the lls port. `overrides` is `sge-port/overrides`, `upstreamResources` libGDX's `gdx/res`, `frozenDerivedPolicy` a committed TSV file the derive step
    * reads its spellings from.
    */
  def universal(overrides: Path, upstreamResources: Path, frozenDerivedPolicy: Option[Path], steps: Set[String] = DefaultSteps): PortManifest = {
    val unknown = steps -- Steps.keySet
    require(unknown.isEmpty, s"unknown ladder steps: ${unknown.mkString(",")}; known: ${Steps.keySet.toList.sorted.mkString(",")}")
    // the base is lls's own policy (the published `lls-port`); this manifest adds core's
    LlsPolicy
      .core(LlsPolicy.DefaultRungs)
      .extendedBy(
        PortManifest(
          name = "sge-l0",
          governs = Set("com.badlogic.gdx"),
          dropTypes = StepOrder.filter(steps).flatMap(stepTypeDrops).toSet,
          dropMethods = StepOrder.filter(steps).flatMap(stepDrops).toSet,
          // sge ships `TextFormatter` public (java: package-private): declared, the split publishes it.
          allowPackageSplit = (if steps("helpers") then Set("com.badlogic.gdx.utils.TextFormatter") else Set.empty) ++
            // sge's top-level `BitmapFontData`: promoted out of `BitmapFont`, whose package-private
            // members it reads ship public — the split declared, as sge's own tree has it
            Set("com.badlogic.gdx.graphics.g2d.BitmapFont$BitmapFontData"),
          inject = StepOrder.filter(steps).flatMap(stepInjects(overrides)),
          platformDirs = StepOrder.filter(steps).flatMap(stepPlatformInjects(overrides)(_).toList).groupMapReduce(_._1)(_._2)(_ ++ _),
          // a dependent follows the base's published member spellings (`first()` -> `first`): the
          // port-map follow reads what lls published, never re-derives it.
          surface = StepOrder.filter(steps).flatMap(stepsFor(steps)(_)) :+
            balticporter.transform.PortMapTransform.forBases("lls"),
          packageRenames = Map("com.badlogic.gdx" -> "sge"),
          // java's reflective `Json` (dropped by the reflection step, a refusing stand-in injected) keeps the name
          // `LegacyJson`: `Json` is the Kindlings JSON AST sge's Skin and Tiled loaders read (json step).
          typeRenames = Map(
            "com.badlogic.gdx.scenes.scene2d.ui.List" -> "SgeList",
            "com.badlogic.gdx.utils.Json" -> "LegacyJson",
            // sge's `XmlElement`: java's `XmlReader.Element` promoted and renamed
            "com.badlogic.gdx.utils.XmlReader$Element" -> "XmlElement"
          ),
          // sge's `sge.files.FileType`: java's nested `Files.FileType` promoted to top level and nested under `files`
          flattenNestedTypes = Set(
            "com.badlogic.gdx.Files$FileType",
            "com.badlogic.gdx.utils.XmlReader$Element",
            // sge's top-level `BitmapFontData` and `GlyphRun` (same package)
            "com.badlogic.gdx.graphics.g2d.BitmapFont$BitmapFontData",
            "com.badlogic.gdx.graphics.g2d.GlyphLayout$GlyphRun"
          ),
          subPackages = Map("com.badlogic.gdx.Files$FileType" -> "files"),
          resources = List(
            ResourceTree(
              root = upstreamResources.normalize,
              files = List(
                "com/badlogic/gdx/utils/lsans-15.fnt",
                "com/badlogic/gdx/utils/lsans-15.png",
                "com/badlogic/gdx/graphics/g3d/shaders/default.vertex.glsl",
                "com/badlogic/gdx/graphics/g3d/shaders/default.fragment.glsl",
                "com/badlogic/gdx/graphics/g3d/shaders/depth.vertex.glsl",
                "com/badlogic/gdx/graphics/g3d/shaders/depth.fragment.glsl"
              )
            )
          ),
          dependencies = List(
            balticporter.catalog.ArtifactDep("com.badlogicgames.gdx", "gdx-jnigen-loader", "2.5.2", balticporter.catalog.CrossKind.Java)
          ),
          // the derive step reads its spellings from a committed TSV file; no reference tree needed
          frozenDerivedPolicy = if steps("derive") then frozenDerivedPolicy else None
        )
      )
  }
}
