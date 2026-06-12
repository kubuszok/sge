/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/utils/BehaviorTreeLibrary.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.utils` -> `sge.ai.btree.utils`; `ObjectMap` -> `scala.collection.mutable.HashMap`
 *   Convention: split packages, Nullable instead of null
 *   Idiom: AssetManager integration removed (commented out in the original too); the `FileHandleResolver` + parser seam
 *     is preserved so `retrieveArchetypeTree` lazily parses unregistered references from file via a user-supplied
 *     resolver (BehaviorTreeLibrary.java:121-134). The default resolver stands in for gdx-ai's
 *     `GdxAI.getFileSystem().newResolver(FileType.Internal)` (BehaviorTreeLibrary.java:52) but cannot read — the ai
 *     module carries no Sge/FileSystem dependency, so it resolves to a FileHandle that fails with a clear
 *     FileReadError (triggering the parse-attempt, never a lookup-only NoSuchElementException) and keeps the no-arg
 *     constructor compiling on every platform (BehaviorTreeLibraryManager:45). Inject a real FileHandleResolver to
 *     load trees from file.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 178
 * Covenant-baseline-methods: BehaviorTreeLibrary,InternalFileHandleResolver,UnresolvedFileHandle,bt,createBehaviorTree,createRootTask,disposeBehaviorTree,exists,fail,hasArchetypeTree,parser,read,readString,reader,registerArchetypeTree,repository,resolve,resolver,retrieveArchetypeTree,this
 * Covenant-source-reference: com/badlogic/gdx/ai/btree/utils/BehaviorTreeLibrary.java
 * Covenant-verified: 2026-06-12
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package btree
package utils

import java.io.{ File, InputStream, Reader }

import scala.collection.mutable

import sge.assets.loaders.FileHandleResolver
import sge.files.{ FileHandle, FileType }
import lowlevel.Nullable
import sge.utils.SgeError

/** A `BehaviorTreeLibrary` is a repository of behavior tree archetypes. Behavior tree archetypes never run. Indeed, they are only cloned to create behavior tree instances that can run.
  *
  * @param resolver
  *   the [[FileHandleResolver]] used to resolve a tree reference into a [[FileHandle]] whenever the reference is absent from the repository. Defaults to
  *   [[BehaviorTreeLibrary.InternalFileHandleResolver]], which stands in for gdx-ai's `GdxAI.getFileSystem().newResolver(FileType.Internal)` but fails to read because the ai module has no file
  *   system; supply your own resolver to load trees from file.
  * @param parseDebugLevel
  *   the debug level the underlying [[BehaviorTreeParser]] will use.
  *
  * @author
  *   davebaol (original implementation)
  */
class BehaviorTreeLibrary(
  protected val resolver: FileHandleResolver = BehaviorTreeLibrary.InternalFileHandleResolver,
  parseDebugLevel:        Int = BehaviorTreeParser.DebugNone
) {

  protected val repository: mutable.HashMap[String, BehaviorTree[?]] = mutable.HashMap.empty

  /** The parser used to lazily parse unregistered tree references resolved through [[resolver]]. */
  protected val parser: BehaviorTreeParser[Any] = new BehaviorTreeParser[Any](debugLevel = parseDebugLevel)

  /** Creates a `BehaviorTreeLibrary` with the given debug level and the default internal resolver. */
  def this(parseDebugLevel: Int) = this(BehaviorTreeLibrary.InternalFileHandleResolver, parseDebugLevel)

  /** Creates the root task of [[BehaviorTree]] for the specified reference.
    * @param treeReference
    *   the tree identifier, typically a path
    * @return
    *   the root task of the tree cloned from the archetype.
    */
  def createRootTask[T](treeReference: String): Task[T] =
    retrieveArchetypeTree(treeReference).getChild(0).cloneTask().asInstanceOf[Task[T]]

  /** Creates the [[BehaviorTree]] for the specified reference.
    * @param treeReference
    *   the tree identifier, typically a path
    * @return
    *   the tree cloned from the archetype.
    */
  def createBehaviorTree[T](treeReference: String): BehaviorTree[T] =
    createBehaviorTree(treeReference, Nullable.empty[T])

  /** Creates the [[BehaviorTree]] for the specified reference and blackboard object.
    * @param treeReference
    *   the tree identifier, typically a path
    * @param blackboard
    *   the blackboard object (it can be empty).
    * @return
    *   the tree cloned from the archetype.
    */
  def createBehaviorTree[T](treeReference: String, blackboard: Nullable[T]): BehaviorTree[T] = {
    val bt = retrieveArchetypeTree(treeReference).cloneTask().asInstanceOf[BehaviorTree[T]]
    blackboard.foreach(bt.setObject)
    bt
  }

  /** Retrieves the archetype tree from the library. If the library doesn't contain the archetype tree it is loaded and added to the library.
    * @param treeReference
    *   the tree identifier, typically a path
    * @return
    *   the archetype tree.
    * @throws sge.utils.SgeError.SerializationError
    *   if the reference cannot be successfully parsed.
    */
  protected def retrieveArchetypeTree(treeReference: String): BehaviorTree[?] =
    repository.getOrElse(
      treeReference, {
        // Lazy parse-from-file fallback (BehaviorTreeLibrary.java:121-134): resolve the reference to a file,
        // parse it, and register the resulting archetype.
        val archetypeTree = parser.parse(resolver.resolve(treeReference), Nullable.empty[Any])
        registerArchetypeTree(treeReference, archetypeTree)
        archetypeTree
      }
    )

  /** Registers the [[BehaviorTree]] archetypeTree with the specified reference. Existing archetypes in the repository with the same treeReference will be replaced.
    * @param treeReference
    *   the tree identifier, typically a path.
    * @param archetypeTree
    *   the archetype tree.
    */
  def registerArchetypeTree(treeReference: String, archetypeTree: BehaviorTree[?]): Unit =
    repository.put(treeReference, archetypeTree)

  /** Returns `true` if an archetype tree with the specified reference is registered in this library.
    * @param treeReference
    *   the tree identifier, typically a path.
    * @return
    *   `true` if the archetype is registered already; `false` otherwise.
    */
  def hasArchetypeTree(treeReference: String): Boolean =
    repository.contains(treeReference)

  /** Dispose behavior tree obtained by this library.
    * @param treeReference
    *   the tree identifier.
    * @param behaviorTree
    *   the tree to dispose.
    */
  def disposeBehaviorTree(treeReference: String, behaviorTree: BehaviorTree[?]): Unit =
    Task.taskCloner.foreach(_.freeTask(behaviorTree))
}

object BehaviorTreeLibrary {

  /** A [[FileHandle]] produced by the [[InternalFileHandleResolver]] default. It overrides every read entry point to fail with a clear [[SgeError.FileReadError]] rather than touching
    * `java.io.FileInputStream` / `java.lang.Class.getResourceAsStream`, neither of which links on Scala.js. The ai module carries no Sge/FileSystem dependency, so it cannot read assets itself;
    * applications that load behavior trees from file inject a real [[FileHandleResolver]] (e.g. an Sge-backed `FileHandleResolver.Internal` or an AssetManager-backed resolver) through the
    * [[BehaviorTreeLibrary]] constructor. The override guarantees that an unregistered reference triggers a *parse attempt* whose failure is a serialization/file error — never the lookup-only
    * `NoSuchElementException` that the pre-fix port threw (BehaviorTreeLibrary.java:121-134).
    */
  final private class UnresolvedFileHandle(file: File) extends FileHandle(file, FileType.Classpath) {
    private def fail(): Nothing =
      throw SgeError.FileReadError(
        this,
        "BehaviorTreeLibrary has no file system: supply a FileHandleResolver to load '" + file.getPath() + "'"
      )

    override def read():                                                 InputStream = fail()
    override def reader():                                               Reader      = fail()
    override def reader(charset:     String):                            Reader      = fail()
    override def readString(charset: Nullable[String] = Nullable.empty): String      = fail()
    override def exists():                                               Boolean     = false
  }

  /** Default [[FileHandleResolver]] standing in for gdx-ai's `GdxAI.getFileSystem().newResolver(FileType.Internal)` (BehaviorTreeLibrary.java:52). Because the ai module has no Sge/FileSystem
    * dependency, this default resolver produces an [[UnresolvedFileHandle]] that fails to read with a clear error, keeping the no-arg [[BehaviorTreeLibrary]] constructor compiling for
    * [[BehaviorTreeLibraryManager]] on every platform. Applications that actually load trees from file supply their own [[FileHandleResolver]] through the [[BehaviorTreeLibrary]] constructor.
    */
  object InternalFileHandleResolver extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      new UnresolvedFileHandle(new File(fileName))
  }
}
