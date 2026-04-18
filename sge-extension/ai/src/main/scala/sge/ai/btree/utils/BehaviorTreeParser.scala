/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/utils/BehaviorTreeParser.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.utils` -> `sge.ai.btree.utils`; `ObjectMap` -> `mutable.HashMap`;
 *     `Array` -> `DynamicArray`; `ObjectSet` -> `ObjectSet`; `GdxRuntimeException` -> `SgeError.InvalidInput`
 *   Convention: split packages, Nullable instead of null
 *   Idiom: ClassReflection replaced by TaskRegistry with factory functions and TaskMeta;
 *     @TaskConstraint/@TaskAttribute annotations replaced by programmatic metadata registration;
 *     field.set replaced by setter functions in AttrInfo;
 *     castValue (reflection-based type coercion for field.set) eliminated —
 *       AttrInfo.setter closures handle typed conversion at registration time
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree
package utils

import java.io.InputStream
import java.io.Reader

import scala.collection.mutable

import sge.files.FileHandle
import sge.utils.DynamicArray
import sge.utils.Nullable
import sge.utils.ObjectSet
import sge.utils.SgeError

/** A [[BehaviorTree]] parser.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   davebaol (original implementation)
  */
class BehaviorTreeParser[E](
  val distributionAdapters: DistributionAdapters = new DistributionAdapters(),
  var debugLevel:           Int = BehaviorTreeParser.DebugNone,
  btReaderOpt:              Nullable[BehaviorTreeParser.DefaultBehaviorTreeReader[E]] = Nullable.empty
) {

  private val btReader: BehaviorTreeParser.DefaultBehaviorTreeReader[E] =
    btReaderOpt.getOrElse(new BehaviorTreeParser.DefaultBehaviorTreeReader[E]())
  btReader.setParser(this)

  def this(debugLevel: Int) = this(new DistributionAdapters(), debugLevel, Nullable.empty)

  /** Parses the given string.
    * @param string
    *   the string to parse
    * @param obj
    *   the blackboard object. It can be `null`.
    * @return
    *   the behavior tree
    * @throws SgeError.SerializationError
    *   if the string cannot be successfully parsed.
    */
  def parse(string: String, obj: Nullable[E]): BehaviorTree[E] = {
    btReader.parse(string)
    createBehaviorTree(btReader.root.get, obj)
  }

  /** Parses the given input stream.
    * @param input
    *   the input stream to parse
    * @param obj
    *   the blackboard object. It can be `null`.
    * @return
    *   the behavior tree
    * @throws SgeError.SerializationError
    *   if the input stream cannot be successfully parsed.
    */
  def parse(input: InputStream, obj: Nullable[E]): BehaviorTree[E] = {
    btReader.parse(input)
    createBehaviorTree(btReader.root.get, obj)
  }

  /** Parses the given file.
    * @param file
    *   the file to parse
    * @param obj
    *   the blackboard object. It can be `null`.
    * @return
    *   the behavior tree
    * @throws SgeError.SerializationError
    *   if the file cannot be successfully parsed.
    */
  def parse(file: FileHandle, obj: Nullable[E]): BehaviorTree[E] = {
    btReader.parse(file)
    createBehaviorTree(btReader.root.get, obj)
  }

  /** Parses the given reader.
    * @param reader
    *   the reader to parse
    * @param obj
    *   the blackboard object. It can be `null`.
    * @return
    *   the behavior tree
    * @throws SgeError.SerializationError
    *   if the reader cannot be successfully parsed.
    */
  def parse(reader: Reader, obj: Nullable[E]): BehaviorTree[E] = {
    btReader.parse(reader)
    createBehaviorTree(btReader.root.get, obj)
  }

  protected def createBehaviorTree(root: Task[E], obj: Nullable[E]): BehaviorTree[E] = {
    if (debugLevel > BehaviorTreeParser.DebugLow) BehaviorTreeParser.printTree(root, 0)
    val bt = new BehaviorTree[E](Nullable(root), Nullable.empty)
    obj.foreach(bt.setObject)
    bt
  }
}

object BehaviorTreeParser {

  val DebugNone: Int = 0
  val DebugLow:  Int = 1
  val DebugHigh: Int = 2

  /** Prints a behavior tree for debugging. */
  def printTree[E](task: Task[E], indent: Int): Unit = {
    var i = 0
    while (i < indent) { print(' '); i += 1 }
    var nextIndent = indent
    task.guard.foreach { g =>
      println("Guard")
      nextIndent += 2
      printTree(g, nextIndent)
      var j = 0
      while (j < nextIndent) { print(' '); j += 1 }
    }
    println(task.getClass.getSimpleName)
    i = 0
    while (i < task.getChildCount) {
      printTree(task.getChild(i), nextIndent + 2)
      i += 1
    }
  }

  // ── Metadata types ──────────────────────────────────────────────────

  /** Metadata about task constraints (min/max children, attributes). */
  final case class TaskMeta(
    minChildren: Int = 0,
    maxChildren: Int = Int.MaxValue,
    attributes:  Map[String, AttrInfo] = Map.empty
  )

  object TaskMeta {
    val empty: TaskMeta = TaskMeta()
  }

  /** Metadata about a single task attribute.
    * @param fieldName
    *   the internal field name
    * @param setter
    *   function to set the attribute value on a task instance
    * @param required
    *   whether this attribute is required
    */
  final case class AttrInfo(
    fieldName: String,
    setter:    (Task[?], Any) => Unit,
    required:  Boolean = false
  )

  // ── Task Registry ───────────────────────────────────────────────────

  /** A registry of task factories and metadata, replacing ClassReflection-based instantiation. */
  class TaskRegistry {
    private val taskFactories = mutable.HashMap[String, () => Task[?]]()
    private val taskMetadata  = mutable.HashMap[String, TaskMeta]()

    /** Register a task with an alias, factory function, and optional metadata. */
    def registerTask(alias: String, factory: () => Task[?], meta: TaskMeta = TaskMeta.empty): Unit = {
      taskFactories.put(alias, factory)
      taskMetadata.put(alias, meta)
    }

    /** Create a new task instance by alias. */
    def createTask(alias: String): Task[?] =
      taskFactories.getOrElse(alias, throw SgeError.InvalidInput(s"Unknown task type: '$alias'"))()

    /** Get metadata for a task alias. */
    def getMeta(alias: String): TaskMeta =
      taskMetadata.getOrElse(alias, TaskMeta.empty)

    /** Returns true if the alias is registered. */
    def contains(alias: String): Boolean = taskFactories.contains(alias)
  }

  // ── Default imports (built-in task aliases) ─────────────────────────

  private val defaultImports: Map[String, String] = {
    val classes = Seq(
      "alwaysFail" -> "sge.ai.btree.decorator.AlwaysFail",
      "alwaysSucceed" -> "sge.ai.btree.decorator.AlwaysSucceed",
      "dynamicGuardSelector" -> "sge.ai.btree.branch.DynamicGuardSelector",
      "failure" -> "sge.ai.btree.leaf.Failure",
      "include" -> "sge.ai.btree.decorator.Include",
      "invert" -> "sge.ai.btree.decorator.Invert",
      "parallel" -> "sge.ai.btree.branch.Parallel",
      "random" -> "sge.ai.btree.decorator.Random",
      "randomSelector" -> "sge.ai.btree.branch.RandomSelector",
      "randomSequence" -> "sge.ai.btree.branch.RandomSequence",
      "repeat" -> "sge.ai.btree.decorator.Repeat",
      "selector" -> "sge.ai.btree.branch.Selector",
      "semaphoreGuard" -> "sge.ai.btree.decorator.SemaphoreGuard",
      "sequence" -> "sge.ai.btree.branch.Sequence",
      "success" -> "sge.ai.btree.leaf.Success",
      "untilFail" -> "sge.ai.btree.decorator.UntilFail",
      "untilSuccess" -> "sge.ai.btree.decorator.UntilSuccess",
      "wait" -> "sge.ai.btree.leaf.Wait"
    )
    classes.toMap
  }

  // ── Stacked task ────────────────────────────────────────────────────

  final private[utils] case class StackedTask[E](
    lineNumber: Int,
    name:       String,
    task:       Task[E],
    metadata:   TaskMeta
  )

  // ── Subtree ─────────────────────────────────────────────────────────

  private[utils] class Subtree[E](val name: Nullable[String] = Nullable.empty) {
    var rootTask:       Nullable[Task[E]] = Nullable.empty
    var referenceCount: Int               = 0

    def init(root: Task[E]): Unit =
      this.rootTask = Nullable(root)

    def inited: Boolean = !rootTask.isEmpty

    def isRootTree: Boolean = name.fold(true)(n => n.isEmpty)

    def rootTaskInstance(): Task[E] = {
      val rt = rootTask.getOrElse(throw SgeError.InvalidInput("Subtree not initialized"))
      if (referenceCount == 0) {
        referenceCount += 1
        rt
      } else {
        referenceCount += 1
        rt.cloneTask()
      }
    }
  }

  // ── Statement types ─────────────────────────────────────────────────

  sealed private trait Statement {
    def statementName:                                                                      Nullable[String]
    def enter[E](reader:     DefaultBehaviorTreeReader[E], name: String, isGuard: Boolean): Unit
    def attribute[E](reader: DefaultBehaviorTreeReader[E], name: String, value:   Any):     Boolean
    def exit[E](reader:      DefaultBehaviorTreeReader[E]):                                 Unit
  }

  private object ImportStatement extends Statement {
    val statementName:                                                                  Nullable[String] = Nullable("import")
    def enter[E](reader: DefaultBehaviorTreeReader[E], name: String, isGuard: Boolean): Unit             = {}
    def attribute[E](reader: DefaultBehaviorTreeReader[E], name: String, value: Any):   Boolean          = {
      if (!value.isInstanceOf[String]) reader.throwAttributeTypeException("import", name, "String")
      reader.addImport(name, value.asInstanceOf[String])
      true
    }
    def exit[E](reader: DefaultBehaviorTreeReader[E]): Unit = {}
  }

  private object SubtreeStatement extends Statement {
    val statementName:                                                                  Nullable[String] = Nullable("subtree")
    def enter[E](reader: DefaultBehaviorTreeReader[E], name: String, isGuard: Boolean): Unit             = {}
    def attribute[E](reader: DefaultBehaviorTreeReader[E], name: String, value: Any):   Boolean          = {
      if (name != "name") reader.throwAttributeNameException("subtree", name, "name")
      if (!value.isInstanceOf[String]) reader.throwAttributeTypeException("subtree", name, "String")
      if (value == "") throw SgeError.InvalidInput("subtree: the name cannot be empty")
      if (!reader.subtreeName.isEmpty)
        throw SgeError.InvalidInput("subtree: the name has been already specified")
      reader.subtreeName = Nullable(value.asInstanceOf[String])
      true
    }
    def exit[E](reader: DefaultBehaviorTreeReader[E]): Unit = {
      if (reader.subtreeName.isEmpty)
        throw SgeError.InvalidInput("subtree: the name has not been specified")
      reader.switchToNewTree(reader.subtreeName.get)
      reader.subtreeName = Nullable.empty
    }
  }

  private object RootStatement extends Statement {
    val statementName:                                                                  Nullable[String] = Nullable("root")
    def enter[E](reader: DefaultBehaviorTreeReader[E], name: String, isGuard: Boolean): Unit             =
      reader.subtreeName = Nullable("") // the root tree has empty name
    def attribute[E](reader: DefaultBehaviorTreeReader[E], name: String, value: Any): Boolean = {
      reader.throwAttributeTypeException("root", name, "none")
      true
    }
    def exit[E](reader: DefaultBehaviorTreeReader[E]): Unit = {
      reader.switchToNewTree(reader.subtreeName.get)
      reader.subtreeName = Nullable.empty
    }
  }

  private object TreeTaskStatement extends Statement {
    val statementName:                                                                  Nullable[String] = Nullable.empty
    def enter[E](reader: DefaultBehaviorTreeReader[E], name: String, isGuard: Boolean): Unit             = {
      // Root tree is the default one
      if (reader.currentTree.isEmpty) {
        reader.switchToNewTree("")
        reader.subtreeName = Nullable.empty
      }
      reader.openTask(name, isGuard)
    }
    def attribute[E](reader: DefaultBehaviorTreeReader[E], name: String, value: Any): Boolean = {
      val stackedTask = reader.getCurrentTask
      val meta        = stackedTask.metadata
      meta.attributes.get(name) match {
        case None     => false
        case Some(ai) =>
          val isNew = reader.encounteredAttributes.add(name)
          if (!isNew) throw reader.stackedTaskException(stackedTask, s"attribute '$name' specified more than once")
          ai.setter(stackedTask.task, value)
          true
      }
    }
    def exit[E](reader: DefaultBehaviorTreeReader[E]): Unit =
      if (!reader.isSubtreeRef) {
        reader.checkRequiredAttributes(reader.getCurrentTask)
        reader.encounteredAttributes.clear()
      }
  }

  // ── DefaultBehaviorTreeReader ───────────────────────────────────────

  /** The default reader implementation that extends [[BehaviorTreeReader]] and builds a tree using a [[TaskRegistry]]. */
  class DefaultBehaviorTreeReader[E](
    reportsComments:  Boolean = false,
    val taskRegistry: TaskRegistry = new TaskRegistry()
  ) extends BehaviorTreeReader(reportsComments) {

    private[utils] var btParser: Nullable[BehaviorTreeParser[E]] = Nullable.empty

    private[utils] var root:        Nullable[Task[E]] = Nullable.empty
    private[utils] var subtreeName: Nullable[String]  = Nullable.empty
    private var statement:          Statement         = TreeTaskStatement
    private var _indent:            Int               = 0

    private[utils] val userImports = mutable.HashMap[String, String]()
    private[utils] val subtrees    = mutable.HashMap[String, Subtree[E]]()
    private[utils] var currentTree:            Nullable[Subtree[E]]     = Nullable.empty
    private[utils] var currentTreeStartIndent: Int                      = 0
    private[utils] var currentDepth:           Int                      = 0
    private[utils] var step:                   Int                      = 0
    private[utils] var isSubtreeRef:           Boolean                  = false
    private[utils] var prevTask:               Nullable[StackedTask[E]] = Nullable.empty
    private[utils] var guardChain:             Nullable[StackedTask[E]] = Nullable.empty
    private[utils] val stack                 = DynamicArray[StackedTask[E]]()
    private[utils] val encounteredAttributes = ObjectSet[String]()
    private[utils] var isGuard: Boolean = false

    def getParser: BehaviorTreeParser[E] = btParser.get

    def setParser(parser: BehaviorTreeParser[E]): Unit =
      this.btParser = Nullable(parser)

    override def parse(data: Array[Char], offset: Int, length: Int): Unit = {
      debug = btParser.get.debugLevel > BehaviorTreeParser.DebugNone
      root = Nullable.empty
      clear()
      super.parse(data, offset, length)

      // Pop all tasks from the stack and check their minimum number of children
      popAndCheckMinChildren(0)

      val rootTree = subtrees.getOrElse("", throw SgeError.InvalidInput("Missing root tree"))
      root = Nullable(
        rootTree.rootTask.getOrElse(
          throw SgeError.InvalidInput("The tree must have at least the root task")
        )
      )

      clear()
    }

    override protected def startLine(indent: Int): Unit =
      this._indent = indent

    private def checkStatement(name: String): Statement =
      if (name == "import") ImportStatement
      else if (name == "subtree") SubtreeStatement
      else if (name == "root") RootStatement
      else TreeTaskStatement

    override protected def startStatement(name: String, isSubtreeReference: Boolean, isGuard: Boolean): Unit = {
      this.isSubtreeRef = isSubtreeReference

      this.statement = if (isSubtreeReference) TreeTaskStatement else checkStatement(name)
      if (isGuard) {
        if (statement ne TreeTaskStatement)
          throw SgeError.InvalidInput(s"$name: only tree's tasks can be guarded")
      }

      statement.enter(this, name, isGuard)
    }

    override protected def attribute(name: String, value: Any): Unit = {
      val validAttribute = statement.attribute(this, name, value)
      if (!validAttribute) {
        if (statement eq TreeTaskStatement) {
          throw stackedTaskException(getCurrentTask, s"unknown attribute '$name'")
        } else {
          throw SgeError.InvalidInput(s"${statement.statementName.get}: unknown attribute '$name'")
        }
      }
    }

    override protected def endLine(): Unit = {}

    override protected def endStatement(): Unit =
      statement.exit(this)

    private[utils] def openTask(name: String, isGuard: Boolean): Unit = {
      val task: Task[E] =
        if (isSubtreeRef) {
          subtreeRootTaskInstance(name)
        } else {
          val className = getImport(name).getOrElse(name)
          // Try the task registry first; if not found, try looking up by className
          val resolved =
            if (taskRegistry.contains(className)) className
            else if (taskRegistry.contains(name)) name
            else className // will throw from registry
          taskRegistry.createTask(resolved).asInstanceOf[Task[E]]
        }

      val ct = currentTree.get
      if (!ct.inited) {
        initCurrentTree(task, _indent)
        _indent = 0
      } else if (!isGuard) {
        val stackedTask = getPrevTask

        val adjustedIndent = _indent - currentTreeStartIndent
        if (stackedTask.task eq ct.rootTask.get) {
          step = adjustedIndent
        }
        if (adjustedIndent > currentDepth) {
          stack.add(stackedTask) // push
        } else if (adjustedIndent <= currentDepth) {
          // Pop tasks from the stack based on indentation
          // and check their minimum number of children
          val i = (currentDepth - adjustedIndent) / step
          popAndCheckMinChildren(stack.size - i)
        }

        // Check the max number of children of the parent
        val stackedParent = stack.peek
        val maxChildren   = stackedParent.metadata.maxChildren
        if (stackedParent.task.getChildCount >= maxChildren) {
          throw stackedTaskException(
            stackedParent,
            s"max number of children exceeded (${stackedParent.task.getChildCount + 1} > $maxChildren)"
          )
        }

        // Add child task to the parent
        stackedParent.task.addChild(task)
      }
      updateCurrentTask(createStackedTask(name, task), _indent - currentTreeStartIndent, isGuard)
    }

    private def createStackedTask(name: String, task: Task[E]): StackedTask[E] = {
      val importName   = getImport(name).getOrElse(name)
      val resolvedName =
        if (taskRegistry.contains(importName)) importName
        else if (taskRegistry.contains(name)) name
        else importName
      val metadata = taskRegistry.getMeta(resolvedName)
      StackedTask(lineNumber, name, task, metadata)
    }

    private[utils] def getCurrentTask: StackedTask[E] =
      if (isGuard) guardChain.get else prevTask.get

    private def getPrevTask: StackedTask[E] = prevTask.get

    private def updateCurrentTask(stackedTask: StackedTask[E], indent: Int, isGuard: Boolean): Unit = {
      this.isGuard = isGuard
      stackedTask.task.guard = guardChain.map(_.task)
      if (isGuard) {
        guardChain = Nullable(stackedTask)
      } else {
        prevTask = Nullable(stackedTask)
        guardChain = Nullable.empty
        currentDepth = indent
      }
    }

    private[utils] def clear(): Unit = {
      prevTask = Nullable.empty
      guardChain = Nullable.empty
      currentTree = Nullable.empty
      userImports.clear()
      subtrees.clear()
      stack.clear()
      encounteredAttributes.clear()
    }

    // ── Subtree ─────────────────────────────────────────────────────────

    private[utils] def switchToNewTree(name: String): Unit = {
      // Pop all tasks from the stack and check their minimum number of children
      popAndCheckMinChildren(0)

      val newTree = new Subtree[E](Nullable(name))
      if (subtrees.contains(name))
        throw SgeError.InvalidInput(s"A subtree named '$name' is already defined")
      subtrees.put(name, newTree)
      this.currentTree = Nullable(newTree)
    }

    private def initCurrentTree(rootTask: Task[E], startIndent: Int): Unit = {
      currentDepth = -1
      step = 1
      currentTreeStartIndent = startIndent
      currentTree.get.init(rootTask)
      prevTask = Nullable.empty
    }

    private def subtreeRootTaskInstance(name: String): Task[E] = {
      val tree = subtrees.getOrElse(name, throw SgeError.InvalidInput(s"Undefined subtree with name '$name'"))
      tree.rootTaskInstance()
    }

    // ── Import ──────────────────────────────────────────────────────────

    private[utils] def addImport(alias: String, task: String): Unit = {
      if (task == null) throw SgeError.InvalidInput("import: missing task class name.") // @nowarn — null check for parser protocol
      val resolvedAlias =
        if (alias == null) { // @nowarn — null check for parser protocol
          // Extract simple name from fully qualified class name
          val lastDot = task.lastIndexOf('.')
          if (lastDot >= 0) task.substring(lastDot + 1) else task
        } else {
          alias
        }
      if (getImport(resolvedAlias).nonEmpty)
        throw SgeError.InvalidInput(s"import: alias '$resolvedAlias' previously defined already.")
      userImports.put(resolvedAlias, task)
    }

    private def getImport(as: String): Option[String] =
      defaultImports.get(as).orElse(userImports.get(as))

    // ── Integrity checks ────────────────────────────────────────────────

    private def popAndCheckMinChildren(upToFloor: Int): Unit = {
      // Check the minimum number of children in prevTask
      prevTask.foreach(checkMinChildren)

      // Check the minimum number of children while popping up to the specified floor
      while (stack.size > upToFloor) {
        val stackedTask = stack.pop()
        checkMinChildren(stackedTask)
      }
    }

    private def checkMinChildren(stackedTask: StackedTask[E]): Unit = {
      // Check the minimum number of children
      val minChildren = stackedTask.metadata.minChildren
      if (stackedTask.task.getChildCount < minChildren) {
        throw stackedTaskException(stackedTask, s"not enough children (${stackedTask.task.getChildCount} < $minChildren)")
      }
    }

    private[utils] def checkRequiredAttributes(stackedTask: StackedTask[E]): Unit =
      stackedTask.metadata.attributes.foreach { case (key, ai) =>
        if (ai.required && !encounteredAttributes.contains(key)) {
          throw stackedTaskException(stackedTask, s"missing required attribute '$key'")
        }
      }

    private[utils] def stackedTaskException(stackedTask: StackedTask[E], message: String): SgeError.InvalidInput =
      SgeError.InvalidInput(s"${stackedTask.name} at line ${stackedTask.lineNumber}: $message")

    private[utils] def throwAttributeNameException(statement: String, name: String, expectedName: String): Nothing = {
      val expected = if (expectedName != null) s"expected '$expectedName' instead" else " no attribute expected" // @nowarn — null check for protocol
      throw SgeError.InvalidInput(s"$statement: attribute '$name' unknown; $expected")
    }

    private[utils] def throwAttributeTypeException(statement: String, name: String, expectedType: String): Nothing =
      throw SgeError.InvalidInput(s"$statement: attribute '$name' must be of type $expectedType")
  }
}
