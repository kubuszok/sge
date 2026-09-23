package sge.port.ext.gdxai

import balticporter.core.{ FrontendConfig, ParityRef, PortManifest, Provenance, RuntimeMode }
import sge.port.full.LlsClasspath
import sge.port.full.LibgdxPolicy
import balticporter.runner.{ Determinism, PortRun, SourceSet, VendoredCommit }

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** Migrate **gdx-ai** (`gdx-ai/src` — libGDX's AI extension: behaviour trees, state machines, message dispatch, pathfinding, steering, formation motion, scheduling) through the TIR. A dependent port:
  * `gdx/src` a resolution root only, [[LibgdxPolicy.core]] extended. Scope excludes `com/badlogic/gdx/emu/` (GWT super-source collision). Reflective behaviour-tree parser kept via five
  * `MethodBodyTransform` bodies.
  */
object GdxAiMigrate:

  def main(args: Array[String]): Unit =
    val repoRoot = Path.of(sys.props.getOrElse("balticporter.root", ".")).toAbsolutePath.normalize
    val upstream = repoRoot.resolve("original-src/gdx-ai").normalize
    val base     = upstream.resolve("gdx-ai/src").normalize
    val gdxSrc   = repoRoot.resolve("original-src/libgdx/gdx/src").normalize

    val files = Files
      .walk(base)
      .iterator()
      .asScala
      .filter(p => p.toString.endsWith(".java"))
      .map(p => base.relativize(p).toString)
      .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
      // GWT SUPER-SOURCE — upstream's own `compileJava` exclusion, and a second declaration of
      // `com.badlogic.gdx.ai.StandaloneFileSystem` if it is not honoured. See the scope note above.
      // Matched on the path SEGMENT and not as a substring: `com/badlogic/gdx/emu/` is a directory,
      // and a bare `contains("emu")` would also name a package called `emulation`.
      .filterNot(f => f.startsWith("com/badlogic/gdx/emu/"))
      .toList
      .sorted

    PortRun(
      label = "sge-ai",
      portRoot = repoRoot.resolve("ported/sge-ai"),
      sourceSet = SourceSet.Main,
      frontend = FrontendConfig(base, files, LlsClasspath.entries(repoRoot), resolutionRoots = List(gdxSrc)),
      phases = Nil, // supplied by the manifest — the two sources are mutually exclusive
      manifest = Some(GdxAiPolicy.core(repoRoot)),
      provenance = Some(
        Provenance(
          upstreamName = "gdx-ai",
          upstreamCommit = VendoredCommit.of(base),
          originalLicense = "Apache-2.0",
          sourcePathPrefix = "gdx-ai/src",
          sourceRoot = base.toString,
          // ONE FILE IN 167 CARRIES NO PER-FILE NOTICE (PooledBehaviorTreeLibrary.java) --
          // ship the upstream LICENSE beside it.
          notices = List(upstream.resolve("LICENSE"))
        )
      ),
      // NOT `Vendored`: `LibgdxCoreMigrate` already vendors the collection shims into the module
      // this output is compiled beside. Vendoring again would define every support type twice —
      // which the JVM tolerates only while the copies agree, and the Scala.js/Native linkers reject.
      runtimeMode = RuntimeMode.Dependency,
      determinism = Determinism.fromArgs(args.toSeq),
      nextStep = "just ai-measure"
    ).execute()

/** gdx-ai's per-library policy -- a dependent of libGDX core's, deliberately almost empty. `dropTypes`/`dropMethods`/`packageRenames`/every signature-affecting phase are inherited, not restated.
  * `inject` is NOT inherited: a drop is shared-API policy, but exactly one module ships each replacement file. Milestone 1 adds a namespace CLAIM and the base-surface residue check, nothing else.
  */
object GdxAiPolicy:

  def core(repoRoot: Path): PortManifest =
    LibgdxPolicy
      .core(repoRoot)
      .extendedBy(
        PortManifest(
          name = "sge-ai",
          governs = Set("com.badlogic.gdx.ai"),
          // NO packageRenames, deliberately: gdx-ai declares com.badlogic.gdx.ai, INSIDE the
          // base's own com.badlogic.gdx, so the inherited rename already carries every unit to
          // sge.ai.*. Restating it computes the same string and is still WRONG —
          // ManifestAgreement reports a fatal RenameOverride, since a dependent's own rule for a
          // prefix inside the base's namespace is free to drift. `governs` STAYS.
          surface = List(
            // THE BEHAVIOUR-TREE PARSER'S REFLECTIVE HALF: DefaultBehaviorTreeReader names the
            // base's dropped reflect.Field in three signatures, inside an ENUM CONSTANT WITH A
            // BODY, whose members MemberKey.parse cannot name; TypeRedirectTransform fixes
            // all three at once. SCOPED to com.badlogic.gdx.ai: a dependent's Program CONTAINS
            // its base, so unscoped would re-point `Field` in libGDX itself (0->1 FATAL).
            new balticporter.transform.TypeRedirectTransform(
              redirects = Map(
                "com.badlogic.gdx.utils.reflect.Field" -> "com.badlogic.gdx.ai.btree.utils.TaskField"
              ),
              scopes = Map(
                "com.badlogic.gdx.utils.reflect.Field" ->
                  balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx.ai"))
              )
            ),
            // THE ONE SINGLE-SITE REFLECTIVE MEMBER LEFT — ordinary algorithmic code with one
            // member reaching the base's dropped reflect package; dropping the TYPE would fork it
            // from upstream, dropping the METHOD strands its callers. Signature untouched. KEY is
            // upstream namespace; BODY is spliced verbatim in the port's FINAL namespace.
            new balticporter.transform.MethodBodyTransform(
              Map(
                // ArrayReflection.newInstance(items.getClass().getComponentType(), n): NOT an
                // approximation -- this class's own ctor writes (T[]) new Object[capacity], so
                // the component IS java.lang.Object at every call.
                "com.badlogic.gdx.ai.utils.CircularBuffer#resize(int)" ->
                  """{
                    |    val newItems: scala.Array[T] =
                    |      new scala.Array[java.lang.Object](newCapacity).asInstanceOf[scala.Array[T]]
                    |    if (this.tail > this.head) {
                    |      java.lang.System.arraycopy(this.items, this.head, newItems, 0, this.size)
                    |    } else if (this.size > 0) {
                    |      // NOTE: when head == tail the buffer can be empty or full
                    |      java.lang.System.arraycopy(this.items, this.head, newItems, 0, this.items.length - this.head)
                    |      java.lang.System.arraycopy(this.items, 0, newItems, this.items.length - this.head, this.tail)
                    |    }
                    |    this.head = 0
                    |    this.tail = this.size
                    |    this.items = newItems
                    |  }""".stripMargin,

                // `Task#cloneTask()` is retired by `RegistryTransform` below, which keys its
                // `ClassReflection.newInstance(this.getClass())` fallback on the `Class` value;
                // TASK_CLONER and java's `TaskCloneException` handler stay as java wrote them.

                // THE BEHAVIOUR-TREE PARSER — FIVE BODIES, nothing else in the file moves.
                // DefaultBehaviorTreeReader asked the JVM three run-time questions (build this
                // class, read its annotations, coerce a value); each becomes a lookup in the
                // injected `sge.ai.btree.utils.TaskRegistry`, adapted from the hand port's own
                // trio. WHAT THE PORT LOSES: java's mechanism is OPEN, a table is CLOSED.

                // `newInstance(forName(className))` STAYS a body: `RegistryTransform` keys on a
                // `Class` VALUE, and the name half would need a SCOPED, mergeable
                // `ClassTableTransform` — the phase takes neither, and the base already binds
                // `forName` globally. The catch is kept so the table's
                // refusal arrives at the caller in java's own words.
                "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#openTask(String,boolean)" ->
                  """{
                    |    try {
                    |      var task: sge.ai.btree.Task[E] = null.asInstanceOf[sge.ai.btree.Task[E]]
                    |      if (this.isSubtreeRef) {
                    |        task = this.subtreeRootTaskInstance(name)
                    |      } else {
                    |        var className: java.lang.String = this.getImport(name)
                    |        if (className == null) {
                    |          className = name
                    |        } else ()
                    |        task = sge.ai.btree.utils.TaskRegistry.newTask(className).asInstanceOf[sge.ai.btree.Task[E]]
                    |      }
                    |      if (!this.currentTree.inited()) {
                    |        this.initCurrentTree(task, this.indent)
                    |        this.indent = 0
                    |      } else {
                    |        if (!isGuard) {
                    |          val stackedTask: sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.StackedTask[E] = this.prevTask
                    |          this.indent = this.indent - this.currentTreeStartIndent
                    |          if (stackedTask.task eq this.currentTree.rootTask) {
                    |            this.step = this.indent
                    |          } else ()
                    |          if (this.indent > this.currentDepth) {
                    |            // push
                    |            this.stack.add(stackedTask)
                    |          } else {
                    |            if (this.indent <= this.currentDepth) {
                    |              // Pop tasks from the stack based on indentation
                    |              // and check their minimum number of children
                    |              val i: scala.Int = (this.currentDepth - this.indent) / this.step
                    |              this.popAndCheckMinChildren(this.stack.size - i)
                    |            } else ()
                    |          }
                    |          // Check the max number of children of the parent
                    |          val stackedParent: sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.StackedTask[E] = this.stack.peek
                    |          val maxChildren: scala.Int = stackedParent.metadata.maxChildren
                    |          if (stackedParent.task.childCount >= maxChildren) {
                    |            throw this.stackedTaskException(stackedParent, ((("max number of children exceeded (" + (stackedParent.task.childCount + 1)) + " > ") + maxChildren) + ")")
                    |          } else ()
                    |          // Add child task to the parent
                    |          stackedParent.task.addChild(task)
                    |        } else ()
                    |      }
                    |      this.updateCurrentTask(this.createStackedTask(name, task), this.indent, isGuard)
                    |    } catch {
                    |      case e: sge.utils.reflect.ReflectionException => {
                    |        throw new sge.utils.GdxRuntimeException("Cannot parse behavior tree!!!", e)
                    |      }
                    |    }
                    |  }""".stripMargin,

                // getAnnotation(...) + getFields + each field's getDeclaredAnnotation. The CACHE
                // and the null protocol are java's own: null means "no @TaskConstraint in this
                // hierarchy". The base's @Null surface is read off the generated caller: this
                // hand-written body is outside the threading closure, so it must NOT use the
                // engine's checked `.get` unwrap — an empty cache is the NORMAL case here.
                "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#findMetadata(Class)" ->
                  """{
                    |    val cached: lowlevel.Nullable[sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.Metadata] =
                    |      this.metadataCache.get(clazz)
                    |    var metadata: sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.Metadata =
                    |      if (cached.isEmpty) null else cached.get
                    |    if (metadata == null) {
                    |      val meta: sge.ai.btree.utils.TaskRegistry.Meta = sge.ai.btree.utils.TaskRegistry.metaOf(clazz)
                    |      if (meta != null) {
                    |        val taskAttributes: lowlevel.util.ObjectMap[java.lang.String, sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.AttrInfo] =
                    |          lowlevel.util.ObjectMap[java.lang.String, sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.AttrInfo]()
                    |        val attrs: scala.Array[sge.ai.btree.utils.TaskRegistry.Attr] = meta.attributes
                    |        var i: scala.Int = 0
                    |        while (i < attrs.length) {
                    |          val a: sge.ai.btree.utils.TaskRegistry.Attr = attrs(i)
                    |          val ai: sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.AttrInfo =
                    |            new sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.AttrInfo(a.name, a.fieldName, a.required)
                    |          taskAttributes.put(ai.name, ai)
                    |          i = i + 1
                    |        }
                    |        metadata = new sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.Metadata(meta.minChildren, meta.maxChildren, taskAttributes)
                    |        this.metadataCache.put(clazz, metadata)
                    |      } else ()
                    |    } else ()
                    |    metadata
                    |  }""".stripMargin,

                // `ClassReflection.getField(clazz, name)`. Java raised a `GdxRuntimeException` wrapping the
                // `ReflectionException` for a field that is not there; so does this, with the one sentence
                // that says where the answer now comes from.
                "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#getField(Class,String)" ->
                  """{
                    |    val f: sge.ai.btree.utils.TaskField = sge.ai.btree.utils.TaskRegistry.fieldOf(clazz, name)
                    |    if (f == null) {
                    |      throw new sge.utils.GdxRuntimeException(new sge.utils.reflect.ReflectionException(
                    |        ("no @TaskAttribute field '" + name + "' is registered for " + clazz.getName()) +
                    |        "; this port resolves task attributes through sge.ai.btree.utils.TaskRegistry " +
                    |        "rather than through runtime reflection, which the libGDX base drops"))
                    |    } else ()
                    |    f
                    |  }""".stripMargin,

                // field.setAccessible(true) has no counterpart and needs none; everything else
                // is java's own control flow and castValue's null protocol, verbatim.
                "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#setField(Field,Task,Object)" ->
                  """{
                    |    val valueObject: java.lang.Object = this.castValue(field, value)
                    |    if (valueObject == null) {
                    |      this.throwAttributeTypeException(this.currentTask.name, field.getName(), field.getTypeName())
                    |    } else ()
                    |    field.set(task, valueObject)
                    |  }""".stripMargin,

                // THE OVERRIDE POINT UPSTREAM DOCUMENTS, kept: the redirected TaskField carries
                // the coercion for its own declared type, so a subclass overriding this decides.
                "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#castValue(Field,Object)" ->
                  """{
                    |    field.cast(value, this.btParser.distributionAdapters)
                    |  }""".stripMargin
              )
            ),
            // gdx-ai's ONE keyable reflective instantiation: `Task#cloneTask`'s fallback
            // `ClassReflection.newInstance(this.getClass())`. `miss = JvmReflect` is DECLARED,
            // its non-JVM cost COUNTED, and it is what admits a SELF-CLONE. `onFailure` is java's OWN answer where reflection fails -- Task.java:270
            // `catch (ReflectionException e) { throw new TaskCloneException(e) }` -- so `handles`
            // names that exception and java's now-dead handler is elided EXACTLY.
            new balticporter.transform.RegistryTransform(
              List(
                balticporter.transform.RegistryTransform.Registry(
                  callee = "com.badlogic.gdx.utils.reflect.ClassReflection#newInstance",
                  placement = balticporter.transform.RegistryTransform.Placement.Object(
                    "com.badlogic.gdx.ai.btree.TaskFactories",
                    balticporter.transform.RegistryTransform.Spelling("factories", "register", "create")
                  ),
                  scope = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx.ai")),
                  handles = Set("com.badlogic.gdx.utils.reflect.ReflectionException"),
                  miss = balticporter.transform.RegistryTransform.Miss.JvmReflect(
                    balticporter.transform.RegistryTransform.Miss.OnFailure.Throw("sge.ai.btree.TaskCloneException", "cannot clone a task with no no-argument constructor: ")
                  )
                )
              )
            ),
            // LAST, deliberately (as AshleyPolicy): reads what the BASE actually emitted, must
            // run after any seam re-pointing such a reference. Milestone 1 has no such seam yet —
            // the 14 DroppedType rows it files ARE the milestone's finding. 3.1aq: MkArray[T]
            // given threading for generic classes constructing retarget targets — only where
            // the FIRST type parameter is used DIRECTLY as an element type.
            new balticporter.transform.GlobalsToImplicitsTransform(
              // Only classes whose FIRST type parameter is used DIRECTLY as an element type in a
              // retarget construction (new Array<T>(), new ObjectSet<E>()). Classes constructing
              // wrapped types (new Array<Task<E>>()) derive MkArray from ClassTag of the known class.
              requiredGivens = Map(
                "com.badlogic.gdx.ai.msg.PriorityQueue" -> "lowlevel.MkArray", // new ObjectSet<E>()
                "com.badlogic.gdx.ai.pfa.DefaultGraphPath" -> "lowlevel.MkArray", // new Array<N>()
                "com.badlogic.gdx.ai.sched.SchedulerBase" -> "lowlevel.MkArray" // new Array<T>()
              )
            ),
            balticporter.transform.PortMapTransform.forBases("sge")
          ),
          // THE SERVICE-LOCATOR FACADE, replaced whole — this port's ONE drop, ONE injection.
          // GdxAI chooses two of its three services by SNIFFING THE AMBIENT ENVIRONMENT at class
          // init, and the base retired Gdx.* into a threaded context — a static field
          // initialiser runs before anything could pass one, a QUESTION THE PORT CANNOT ASK. The
          // replacement installs JAVA'S OWN NEGATIVE BRANCH; both alternatives measured worse.
          dropTypes = Set("com.badlogic.gdx.ai.GdxAI"),
          // THREE injected files, only ONE replacing a drop: sge/ai/GdxAI.scala stands at the
          // dropped FQN; TaskField.scala/TaskRegistry.scala stand at names nothing drops. NOT
          // inherited by test below -- exactly one module ships each file.
          inject = List(repoRoot.resolve("sge-port/overrides-ext/gdxai")),
          // THE REFERENCE HAND PORT for sge-ai. NOT inherited.
          parity = Some(ParityRef(roots = List(repoRoot.resolve("sge-extension/ai/src/main/scala").normalize)))
        )
      )

  /** ...and the TEST source set's, which is `core` EXTENDED and adds exactly one phase (`TestFrameworkTransform`). `inject` is NOT restated: the main source set already ships `sge/ai/GdxAI.scala`,
    * and restating it would emit a second definition of that FQN.
    */
  def test(repoRoot: Path): PortManifest = core(repoRoot).extendedBy(
    PortManifest(
      name = "sge-ai-test",
      surface = List(new balticporter.transform.TestFrameworkTransform())
    )
  )
