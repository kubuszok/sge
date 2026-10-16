package sge.port.ext.gdxai

import balticporter.core.{ ParityRef, PortManifest }

import java.nio.file.Path

/** gdx-ai's policy (`gdx-ai/src` minus `com/badlogic/gdx/emu`, GWT's emulation of JDK types; 166 types: behaviour trees, state machines, messaging, path finding, steering, scheduling) — a DEPENDENT
  * of sge-core. gdx-ai lives inside libGDX's own `com.badlogic.gdx`, so the inherited package rename already carries it to `sge.ai` and is never restated. What this policy adds answers the base's
  * dropped runtime reflection, the way sge decided it: the behaviour-tree parser reads a closed table (`TaskRegistry`, injected) instead of class files.
  */
object GdxAiPolicy {

  def include(f: String): Boolean = !f.startsWith("com/badlogic/gdx/emu/")

  private val Ai = balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx.ai"))

  /** The hand port's registration API (`TaskRegistry`, `TaskMeta`, `AttrInfo`) over the reflection-free table the injected `TaskRegistry` object keeps: a task is registered under its alias, its
    * constraint and attributes under the class its factory builds, which is what the reader asks the table for.
    */
  private val ParserRegistryShim: String =
    """final case class TaskMeta(minChildren: scala.Int = 0, maxChildren: scala.Int = scala.Int.MaxValue, attributes: scala.collection.immutable.Map[java.lang.String, AttrInfo] = scala.collection.immutable.Map.empty)
      |  object TaskMeta {
      |    val empty: TaskMeta = TaskMeta()
      |  }
      |  final case class AttrInfo(fieldName: java.lang.String, setter: (sge.ai.btree.Task[?], scala.Any, sge.ai.btree.utils.DistributionAdapters) => scala.Unit, required: scala.Boolean = false)
      |  class TaskRegistry {
      |    def registerTask(alias: java.lang.String, factory: () => sge.ai.btree.Task[?], meta: TaskMeta = TaskMeta.empty): scala.Unit = {
      |      sge.ai.btree.utils.TaskRegistry.register(alias, () => factory().asInstanceOf[sge.ai.btree.Task[java.lang.Object]])
      |      if (meta != TaskMeta.empty) {
      |        val className: java.lang.String = factory().getClass.getName
      |        sge.ai.btree.utils.TaskRegistry.constrain(className, meta.minChildren, meta.maxChildren)
      |        meta.attributes.foreach { (name, a) =>
      |          var adapters: sge.ai.btree.utils.DistributionAdapters = new sge.ai.btree.utils.DistributionAdapters()
      |          sge.ai.btree.utils.TaskRegistry.attribute(
      |            className,
      |            name,
      |            a.fieldName,
      |            a.required,
      |            "java.lang.Object",
      |            (v: java.lang.Object, ad: sge.ai.btree.utils.DistributionAdapters) => { adapters = ad; v },
      |            (t: java.lang.Object, v: java.lang.Object) => a.setter(t.asInstanceOf[sge.ai.btree.Task[?]], v, adapters)
      |          )
      |        }
      |      }
      |    }
      |  }""".stripMargin

  /** the behaviour-tree tasks (every one a user may extend at their own blackboard type) and the path-finding queue, which the hand port declares over any type */
  private val UnboundTypes: Set[String] =
    Set(
      "BehaviorTree",
      "BranchTask",
      "Decorator",
      "LeafTask",
      "LoopDecorator",
      "SingleRunningChildBranch",
      "Task",
      "branch.DynamicGuardSelector",
      "branch.Parallel",
      "branch.RandomSelector",
      "branch.RandomSequence",
      "branch.Selector",
      "branch.Sequence",
      "decorator.AlwaysFail",
      "decorator.AlwaysSucceed",
      "decorator.Include",
      "decorator.Invert",
      "decorator.Random",
      "decorator.Repeat",
      "decorator.SemaphoreGuard",
      "decorator.UntilFail",
      "decorator.UntilSuccess",
      "leaf.Failure",
      "leaf.Success",
      "leaf.Wait"
    ).map(c => s"com.badlogic.gdx.ai.btree.$c") + "com.badlogic.gdx.ai.pfa.PathFinderQueue"

  /** the library's own constructible tasks, each answering `newInstance()` itself as in the hand port */
  private def ConcreteTasks: List[String] = (UnboundTypes -- AbstractTasks).toList.sorted

  /** the tasks no table entry can construct, and the queue, which is no task */
  private val AbstractTasks: Set[String] =
    Set("BranchTask", "Decorator", "LeafTask", "LoopDecorator", "SingleRunningChildBranch", "Task").map(c => s"com.badlogic.gdx.ai.btree.$c") +
      "com.badlogic.gdx.ai.pfa.PathFinderQueue"

  private val ClockReadersOutsideContext = Set(
    "com.badlogic.gdx.ai.steer.behaviors.Jump",
    // a `Schedulable`'s `run`: the queue reads the clock it was given at construction (its body below)
    "com.badlogic.gdx.ai.pfa.PathFinderQueue#run",
    // an interruptible search keeps java's signature, as in the hand port: a user's path finder implements it
    "com.badlogic.gdx.ai.pfa.PathFinder#search",
    "com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder#search",
    "com.badlogic.gdx.ai.pfa.HierarchicalPathFinder#search"
  )

  /** the wait task, the steering behaviour and the two proximities take the clock on their constructor, as in the hand port (`Wander(owner)(using Timepiece)`) */
  private val ClockOnConstructor = Set(
    "com.badlogic.gdx.ai.btree.leaf.Wait",
    "com.badlogic.gdx.ai.steer.behaviors.Wander",
    "com.badlogic.gdx.ai.steer.proximities.RadiusProximity",
    "com.badlogic.gdx.ai.steer.proximities.FieldOfViewProximity"
  )

  def core(coreManifest: PortManifest, sgeRoot: Path, reportRoots: List[Path], reference: Option[Path] = None): PortManifest =
    coreManifest.extendedBy(
      PortManifest(
        name = "sge-ai",
        governs = Set("com.badlogic.gdx.ai"),
        // `GdxAI` picks its logger and file system by reading `Gdx.app`/`Gdx.files` in its class initialiser, which has no `Sge` context
        // to read; the injected replacement installs java's own branch for the absent application and refuses a file system until one
        // is set (its header says so)
        dropTypes = Set("com.badlogic.gdx.ai.GdxAI"),
        // `GdxAI`'s replacement, and the parser's reflection-free field and task table
        inject = List(sgeRoot.resolve("sge-port/overrides-ext/gdxai")),
        surface = List(
          // a behaviour tree's blackboard and a path-finding queue's node are any type, as in the hand port (`Task[Int]`,
          // `PathFinderQueue[Int]`): java's implicit `<: Object` bound goes, and with it every bound these reach
          new balticporter.transform.ElementWitnessTransform(dropBound = UnboundTypes),
          // the parser's three `reflect.Field` signatures name the injected `TaskField`, which carries a field's coercion and store
          new balticporter.transform.TypeRedirectTransform(
            redirects = Map("com.badlogic.gdx.utils.reflect.Field" -> "com.badlogic.gdx.ai.btree.utils.TaskField"),
            scopes = Map("com.badlogic.gdx.utils.reflect.Field" -> Ai)
          ),
          new balticporter.transform.MethodBodyTransform(ReflectionBodies),
          // a library's default resolver asks the installed file system when a tree is RESOLVED, as the hand port's does: java
          // asked at construction, which this port's `GdxAI` cannot answer until a file system is installed
          new balticporter.transform.CallSiteSubstitutionTransform(
            List(
              balticporter.transform.CallSiteSubstitutionTransform.Entry(
                "com.badlogic.gdx.ai.FileSystem#newResolver(FileType)",
                "(new sge.assets.loaders.FileHandleResolver {{ override def resolve(fileName: java.lang.String): sge.files.FileHandle = {recv}.newResolver({arg0}).resolve(fileName) }})",
                balticporter.tir.RuleScope.Only(Set("com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibrary"))
              )
            )
          ),
          // a telegram's extra info is any value, as in the hand port: a scala value need not be an `Object`
          new balticporter.transform.TypeRedirectTransform(
            redirects = Map("java.lang.Object" -> "scala.Any"),
            scopes = Map(
              "java.lang.Object" -> balticporter.tir.RuleScope.Only(
                Set(
                  "com.badlogic.gdx.ai.msg.Telegram#extraInfo",
                  "com.badlogic.gdx.ai.msg.MessageDispatcher#dispatchMessage"
                )
              )
            )
          ),
          // `Task#cloneTask`'s fallback `ClassReflection.newInstance(getClass())`: a registered factory, else the JVM's reflection, else
          // java's own `TaskCloneException`
          new balticporter.transform.RegistryTransform(
            List(
              balticporter.transform.RegistryTransform.Registry(
                callee = "com.badlogic.gdx.utils.reflect.ClassReflection#newInstance",
                placement = balticporter.transform.RegistryTransform.Placement.Object(
                  "com.badlogic.gdx.ai.btree.TaskFactories",
                  balticporter.transform.RegistryTransform.Spelling("factories", "register", "create")
                ),
                scope = Ai,
                handles = Set("com.badlogic.gdx.utils.reflect.ReflectionException"),
                miss = balticporter.transform.RegistryTransform.Miss.JvmReflect(
                  balticporter.transform.RegistryTransform.Miss.OnFailure.Throw("sge.ai.btree.TaskCloneException", "cannot clone a task with no no-argument constructor: ")
                )
              )
            )
          ),
          // `cloneTask` asks the hand port's `newInstance()` for the fresh task, AFTER the registry minted the table it defaults to
          new balticporter.transform.MethodBodyTransform(CloneBodies, group = "clone"),
          // `GdxAI.getTimepiece()` is the AI clock every caller reads; the hand port threads it as `(using Timepiece)`
          new balticporter.transform.GlobalsToImplicitsTransform(
            holders = List(
              balticporter.transform.ContextHolder(
                holder = "com.badlogic.gdx.ai.GdxAI",
                context = balticporter.transform.ContextType.Injected("sge.ai.Timepiece"),
                members = Map("timepiece" -> "self"),
                attach = balticporter.transform.ContextAttach.Method,
                // a behaviour-tree task's `run`, a steering behaviour's `calculateSteering` and a schedulable's `run` keep
                // java's signatures: these classes read the clock `getTimepiece` answers when no context is given
                scope = balticporter.tir.RuleScope.Everywhere(ClockReadersOutsideContext),
                boundary = balticporter.transform.ContextBoundary.ResidualGlobal,
                attachClass = ClockOnConstructor
              )
            )
          ),
          // the reference's constructor parameter names and underscore fields
          new balticporter.transform.MemberRenameTransform(
            // a formation pattern takes its slot count through the hand port's setter-only property
            renames = Map("com.badlogic.gdx.ai.fma.FormationPattern#setNumberOfSlots" -> "numberOfSlots_="),
            derive = true
          ),
          // the reference's access: its public members, and `copyTo` subclass-only as a user's task overrides it
          new balticporter.transform.VisibilityTransform(derive = true, narrow = Set("com.badlogic.gdx.ai.btree.Task#copyTo")),
          // the hand port's `Parallel(policy, orchestrator)` with no children, which java spells with an empty `Array`
          new balticporter.transform.AddMembersTransform(
            (List(
              "com.badlogic.gdx.ai.btree.branch.Parallel" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "this",
                  2,
                  "def this(policy: sge.ai.btree.branch.Parallel.Policy, orchestrator: sge.ai.btree.branch.Parallel.Orchestrator) = this(policy, orchestrator, new lowlevel.util.DynamicArray[sge.ai.btree.Task[E]]())",
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.ai.btree.branch.Parallel#<init>(Policy,Orchestrator)"),
                  Some("the hand port's policy-and-orchestrator constructor, children empty as java's three-argument one is given")
                )
              ),
              "com.badlogic.gdx.ai.pfa.HierarchicalPathFinder" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "bpInstalledClock",
                  0,
                  "private inline given bpInstalledClock: sge.ai.Timepiece = sge.ai.GdxAI.getTimepiece()",
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.ai.pfa.HierarchicalPathFinder#bpInstalledClock"),
                  Some(
                    "the interruptible search keeps java's signature, so the level searches it drives read the installed clock where java read it"
                  )
                )
              ),
              "com.badlogic.gdx.ai.btree.Task" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "newInstance",
                  0,
                  "def newInstance(): sge.ai.btree.Task[E] = throw new sge.ai.btree.TaskCloneException(\"a task cloned without a cloner overrides newInstance(): \" + this.getClass().getName())",
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.ai.btree.Task#newInstance"),
                  Some(
                    "the hand port's fresh-instance hook every task overrides: no platform but the JVM can construct a runtime class reflectively"
                  )
                )
              )
            ) ++ ConcreteTasks.map { c =>
              val emitted = c.replace("com.badlogic.gdx", "sge")
              c -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "newInstance",
                  0,
                  s"override def newInstance(): sge.ai.btree.Task[E] = new $emitted[E]()",
                  balticporter.tir.Reason.Configured("add-members", s"$c#newInstance"),
                  Some("the hand port's own fresh instance of this task, which cloneTask fills")
                )
              )
            } ++ List(
              "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "this",
                  1,
                  "def this(btReaderOpt: lowlevel.Nullable[sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader[E]]) = this(new sge.ai.btree.utils.DistributionAdapters(), sge.ai.btree.utils.BehaviorTreeParser.DEBUG_NONE, btReaderOpt)",
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser#<init>(btReaderOpt)"),
                  Some("the hand port's parser over a given reader, the adapters and debug level java's defaults")
                ),
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "TaskMeta",
                  0,
                  ParserRegistryShim,
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser#TaskRegistry"),
                  Some("the hand port's per-parser task registration, filed into the table the reader resolves task names through"),
                  true
                )
              ),
              "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "this",
                  1,
                  "def this(taskRegistry: sge.ai.btree.utils.BehaviorTreeParser.TaskRegistry) = this()",
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#<init>(taskRegistry)"),
                  Some("the hand port's reader over a registry, whose registrations the shared table already holds")
                )
              ),
              "com.badlogic.gdx.ai.msg.MessageDispatcher" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "dispatchMessage",
                  6,
                  "def dispatchMessage(msg: scala.Int, delay: scala.Float = 0f, sender: lowlevel.Nullable[sge.ai.msg.Telegraph] = lowlevel.Nullable.empty, receiver: lowlevel.Nullable[sge.ai.msg.Telegraph] = lowlevel.Nullable.empty, extraInfo: lowlevel.Nullable[scala.Any] = lowlevel.Nullable.empty, needsReturnReceipt: scala.Boolean = false)(using sge.ai.Timepiece): scala.Unit =\n    this.dispatchMessage(delay, (if (sender.isEmpty) null else sender.get), receiver, msg, extraInfo, needsReturnReceipt)",
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.ai.msg.MessageDispatcher#dispatchMessage(msg,...)"),
                  Some("the hand port's one dispatch with named, defaulted arguments over java's sixteen overloads")
                )
              ),
              "com.badlogic.gdx.ai.pfa.PathFinderQueue" -> List(
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "this",
                  2,
                  "def this(pathFinder: sge.ai.pfa.PathFinder[N], timepiece: sge.ai.Timepiece) = {\n    this(pathFinder)\n    this.timepieceOfQueue = timepiece\n  }",
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.ai.pfa.PathFinderQueue#<init>(PathFinder,Timepiece)"),
                  Some("the hand port's constructor taking the AI clock as an argument")
                ),
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "timepieceOfQueue",
                  0,
                  "private var timepieceOfQueue: sge.ai.Timepiece = sge.ai.GdxAI.getTimepiece()",
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.ai.pfa.PathFinderQueue#timepieceOfQueue"),
                  Some("the clock the queue answers its requests on: given at construction, else the installed one")
                ),
                balticporter.transform.AddMembersTransform.MemberSpec(
                  "timepiece",
                  0,
                  "def timepiece: sge.ai.Timepiece = this.timepieceOfQueue",
                  balticporter.tir.Reason.Configured("add-members", "com.badlogic.gdx.ai.pfa.PathFinderQueue#timepiece"),
                  Some("the hand port's timepiece accessor")
                )
              )
            )).groupMapReduce(_._1)(_._2)(_ ++ _)
          ),
          new balticporter.transform.BeanPropertyTransform(scope = Ai, derive = true),
          new balticporter.transform.NullaryArityTransform(scope = Ai, derive = true),
          new balticporter.transform.NullabilityTransform(scope = Ai, deriveMembers = true),
          // LAST, deliberately: reads what the BASE actually emitted
          balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-l0")
        ),
        parity = reference.map(r => ParityRef(roots = List(r))),
        frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/ai-derived-policy.tsv")).filter(p => java.nio.file.Files.exists(p))
      )
    )

  /** The members that asked the JVM a run-time question the base no longer answers; each body is java's own control flow with the question asked of `TaskRegistry`. Keys are upstream names, bodies the
    * port's final spelling.
    */
  /** `cloneTask`'s fresh task, java's `ClassReflection.newInstance(getClass())`, is asked of `newInstance()`, which the hand port lets a task override */
  private val CloneBodies: Map[String, String] = Map(
    "com.badlogic.gdx.ai.btree.Task#cloneTask()" ->
      """{
        |    if (Task.TASK_CLONER != null) {
        |      try {
        |        return Task.TASK_CLONER.cloneTask(this)
        |      } catch {
        |        case t: java.lang.Throwable => {
        |          throw new sge.ai.btree.TaskCloneException(t)
        |        }
        |      }
        |    } else ()
        |    val clone: Task[E] = this.copyTo(this.newInstance())
        |    clone.guard$field = lowlevel.Nullable(if (this.guard$field.isEmpty) null.asInstanceOf[Task[E]] else this.guard$field.get.cloneTask())
        |    clone
        |  }""".stripMargin
  )

  private val ReflectionBodies: Map[String, String] = Map(
    // the callback's `Object` slot takes a telegram's extra info, which this port types `Any` (a class file's slot no phase moves)
    "com.badlogic.gdx.ai.msg.MessageDispatcher#scanQueue(PendingMessageCallback)" ->
      """{
        |    val currentTime: scala.Float = sge.ai.GdxAI.getTimepiece().time
        |    val queueSize: scala.Int = this.queue.size
        |    var i: scala.Int = 0
        |    while (i < queueSize) {
        |      val telegram: sge.ai.msg.Telegram = this.queue.get(i).orNull
        |      callback.report(telegram.timestamp - currentTime, telegram.sender.orNull, telegram.receiver.orNull, telegram.message, telegram.extraInfo.orNull.asInstanceOf[java.lang.Object], telegram.returnReceiptStatus)
        |      i = i + 1
        |    }
        |  }""".stripMargin,
    // a `Schedulable` runs without a context (the schedulers call it so): the queue answers its requests on the clock it was
    // given at construction, as in the hand port; the rest is java's own
    "com.badlogic.gdx.ai.pfa.PathFinderQueue#run(long)" ->
      """{
        |    given sge.ai.Timepiece = this.timepiece
        |    // Keep track of the current time
        |    this.requestControl.lastTime = sge.utils.Nanos.toLong(sge.utils.TimeUtils.nanoTime())
        |    this.requestControl.timeToRun = timeToRun
        |    this.requestControl.timeTolerance = PathFinderQueue.TIME_TOLERANCE
        |    this.requestControl.pathFinder = this.pathFinder
        |    this.requestControl.server = lowlevel.Nullable(this)
        |    // If no search in progress, take the next from the queue
        |    if (this.currentRequest == null) {
        |      this.currentRequest = this.requestQueue.read().orNull
        |    } else ()
        |    while (this.currentRequest != null) {
        |      val finished: scala.Boolean = this.requestControl.execute(this.currentRequest)
        |      if (!finished) {
        |        return
        |      } else ()
        |      // Read next request from the queue
        |      this.currentRequest = this.requestQueue.read().orNull
        |    }
        |  }""".stripMargin,
    // `ArrayReflection.newInstance(items.getClass().getComponentType(), n)`: the constructor allocates `(T[]) new Object[capacity]`,
    // so the component is `Object` at every call
    "com.badlogic.gdx.ai.utils.CircularBuffer#resize(int)" ->
      """{
        |    val newItems: scala.Array[T] = new scala.Array[java.lang.Object](newCapacity).asInstanceOf[scala.Array[T]]
        |    if (this.tail > this.head) {
        |      java.lang.System.arraycopy(this.items, this.head, newItems, 0, this.size$field)
        |    } else {
        |      if (this.size$field > 0) {
        |        // NOTE: when head == tail the buffer can be empty or full
        |        java.lang.System.arraycopy(this.items, this.head, newItems, 0, this.items.length - this.head)
        |        java.lang.System.arraycopy(this.items, 0, newItems, this.items.length - this.head, this.tail)
        |      } else ()
        |    }
        |    this.head = 0
        |    this.tail = this.size$field
        |    this.items = newItems
        |  }""".stripMargin,
    // `ClassReflection.newInstance(ClassReflection.forName(className))`: the task table builds the named task
    "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#openTask(String,boolean)" ->
      """{
        |      try {
        |        var task: sge.ai.btree.Task[E] = null.asInstanceOf[sge.ai.btree.Task[E]]
        |        if (this.isSubtreeRef) {
        |          task = this.subtreeRootTaskInstance(name)
        |        } else {
        |          var className: java.lang.String = this.getImport(name)
        |          if (className == null) {
        |            className = name
        |          } else ()
        |          task = sge.ai.btree.utils.TaskRegistry.newTask(className).asInstanceOf[sge.ai.btree.Task[E]]
        |        }
        |        if (!this.currentTree.get.inited()) {
        |          this.initCurrentTree(task, this.indent)
        |          this.indent = 0
        |        } else {
        |          if (!isGuard) {
        |            val stackedTask: sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.StackedTask[E] = this.prevTask.get
        |            this.indent = this.indent - this.currentTreeStartIndent
        |            if (stackedTask.task eq this.currentTree.get.rootTask) {
        |              this.step = this.indent
        |            } else ()
        |            if (this.indent > this.currentDepth) {
        |              // push
        |              this.stack.add(stackedTask)
        |            } else {
        |              if (this.indent <= this.currentDepth) {
        |                // Pop tasks from the stack based on indentation
        |                // and check their minimum number of children
        |                val i: scala.Int = (this.currentDepth - this.indent) / this.step
        |                this.popAndCheckMinChildren(this.stack.size - i)
        |              } else ()
        |            }
        |            // Check the max number of children of the parent
        |            val stackedParent: sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.StackedTask[E] = this.stack.peek()
        |            val maxChildren: scala.Int = stackedParent.metadata.maxChildren
        |            if (stackedParent.task.getChildCount >= maxChildren) {
        |              throw this.stackedTaskException(stackedParent, ((("max number of children exceeded (" + (stackedParent.task.getChildCount + 1)) + " > ") + maxChildren) + ")")
        |            } else ()
        |            // Add child task to the parent
        |            stackedParent.task.addChild(task)
        |          } else ()
        |        }
        |        this.updateCurrentTask(this.createStackedTask(name, task), this.indent, isGuard)
        |      } catch {
        |        case e: sge.utils.reflect.ReflectionException => {
        |          throw sge.utils.SgeError.InvalidInput("Cannot parse behavior tree!!!", scala.Option(e))
        |        }
        |      }
        |    }""".stripMargin,
    // `getAnnotation(TaskConstraint)` + `getFields` + each field's `@TaskAttribute`: the table's entry for the class, null where
    // java found no `@TaskConstraint` in the hierarchy
    "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#findMetadata(Class)" ->
      """{
        |      var metadata: sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.Metadata = this.metadataCache.get(clazz).orNull
        |      if (metadata == null) {
        |        val meta: sge.ai.btree.utils.TaskRegistry.Meta = sge.ai.btree.utils.TaskRegistry.metaOf(clazz)
        |        if (meta != null) {
        |          val taskAttributes: lowlevel.util.ObjectMap[java.lang.String, sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.AttrInfo] =
        |            new lowlevel.util.ObjectMap[java.lang.String, sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.AttrInfo]()
        |          for (a <- meta.attributes) {
        |            val ai: sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.AttrInfo =
        |              new sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.AttrInfo(a.name, a.fieldName, a.required)
        |            taskAttributes.put(ai.name, lowlevel.Nullable(ai))
        |          }
        |          metadata = new sge.ai.btree.utils.BehaviorTreeParser.DefaultBehaviorTreeReader.Metadata(meta.minChildren, meta.maxChildren, taskAttributes)
        |          this.metadataCache.put(clazz, lowlevel.Nullable(metadata))
        |        } else ()
        |      } else ()
        |      metadata
        |    }""".stripMargin,
    // `ClassReflection.getField(clazz, name)`: a field the table does not hold fails as java's missing field did
    "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#getField(Class,String)" ->
      """{
        |      val f: sge.ai.btree.utils.TaskField = sge.ai.btree.utils.TaskRegistry.fieldOf(clazz, name)
        |      if (f == null) {
        |        val bpCause: java.lang.Throwable = new sge.utils.reflect.ReflectionException(
        |          "no @TaskAttribute field '" + name + "' is registered for " + clazz.getName() +
        |            "; task attributes are resolved through sge.ai.btree.utils.TaskRegistry, not runtime reflection")
        |        throw sge.utils.SgeError.InvalidInput(bpCause.toString, scala.Option(bpCause))
        |      } else ()
        |      f
        |    }""".stripMargin,
    // `field.setAccessible(true)` has no counterpart and needs none; the rest is java's own
    "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#setField(Field,Task,Object)" ->
      """{
        |      val valueObject: java.lang.Object = this.castValue(field, value)
        |      if (valueObject == null) {
        |        this.throwAttributeTypeException(this.getCurrentTask.name, field.getName(), field.getTypeName())
        |      } else ()
        |      field.set(task, valueObject)
        |    }""".stripMargin,
    // upstream's documented override point, kept: the field carries the coercion for its own declared type
    "com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser$DefaultBehaviorTreeReader#castValue(Field,Object)" ->
      """{
        |      field.cast(value, this.btParser.get.distributionAdapters)
        |    }""".stripMargin
  )
}
