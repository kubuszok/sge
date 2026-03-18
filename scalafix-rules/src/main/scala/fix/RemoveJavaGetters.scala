package fix

import scalafix.v1._
import scala.meta._

/** Scalafix rule that renames Java-style `getXxx` no-arg methods to Scala property style.
  *
  * Definitions: `def getXxx: T` → `def xxx: T`, `def getXxx(): T` → `def xxx: T`
  * Call sites: `obj.getXxx` → `obj.xxx`, `obj.getXxx()` → `obj.xxx`, `getXxx` → `xxx`
  *
  * Only processes no-arg methods. Methods with parameters are left untouched.
  *
  * When renaming `getXxx` to `xxx` would conflict with a private/protected field named `xxx`
  * in the same class, the rule automatically renames the field to `_xxx` and updates all
  * internal references.
  *
  * Usage: `sbt "sge / scalafix RemoveJavaGetters"`
  */
class RemoveJavaGetters extends SyntacticRule("RemoveJavaGetters") {

  /** Method names that must never be renamed (JDK, Scala conflicts, public field conflicts, builder clashes). */
  private val permanentExclusions: Set[String] = Set(
    // --- JDK / Scala language conflicts ---
    "getClass", "getType",
    // JDK java.lang.Class methods
    "getName", "getSimpleName", "getCanonicalName", "getTypeName",
    "getSuperclass", "getInterfaces", "getClassLoader",
    "getConstructor", "getConstructors", "getDeclaredField", "getDeclaredFields",
    "getDeclaredMethod", "getDeclaredMethods", "getMethod", "getMethods",
    "getField", "getFields", "getAnnotation", "getAnnotations",
    "getPackage", "getComponentType", "getModifiers", "getResource",
    "getResourceAsStream", "getEnclosingClass",
    // JDK java.io.File methods
    "getPath", "getAbsolutePath", "getCanonicalPath", "getParent", "getParentFile",
    "getAbsoluteFile", "getCanonicalFile", "getFreeSpace", "getTotalSpace",
    "getUsableSpace",
    // JDK java.util.Locale
    "getDefault", "getLanguage", "getCountry", "getDisplayName", "getDisplayLanguage",
    "getVariant",
    // JDK java.lang.Thread / Throwable
    "getStackTrace", "getMessage", "getCause", "getLocalizedMessage",
    // JDK java.nio.Buffer / ByteBuffer
    "getBuffer", "getInt", "getShort", "getChar", "getDouble",
    // JDK java.lang.System
    "getProperty", "getenv",
    // JDK java.io.RandomAccessFile
    "getChannel",
    // JDK java.util.zip.CRC32 / Checksum
    "getValue",
    // JDK java.net.InetAddress / Socket
    "getHostName", "getLocalHost", "getHostAddress",
    "getInputStream", "getOutputStream", "getAllByName",
    "getRemoteSocketAddress",
    // JDK java.lang.StackTraceElement
    "getMethodName",
    // JDK java.util.Base64
    "getDecoder",
    // JDK misc / Scala stdlib
    "getOrElse", "getOrDefault", "getIndex",
    // FileHandle methods that are self-referencing
    "getFile",
    // Preferences: parameterized (take key) so auto-excluded, but listed for safety
    "getBoolean", "getInteger", "getFloat", "getLong", "getString",

    // --- Public field conflicts (field is public, can't safely rename across files) ---
    // Cell.scala: public Nullable[Value] fields vs Float getters
    "getPrefWidth", "getPrefHeight", "getMinWidth", "getMinHeight",
    "getMaxWidth", "getMaxHeight",
    "getSpaceTop", "getSpaceLeft", "getSpaceBottom", "getSpaceRight",
    "getPadTop", "getPadLeft", "getPadBottom", "getPadRight",
    "getPadX", "getPadY",
    "getActor", // Cell.getActor does type cast; ScrollPane.getActor returns typed widget
    // ScrollPane — public var scrollX/scrollY are Boolean, getScrollX/Y returns Float
    "getScrollX", "getScrollY",
    "getMaxX", "getMaxY", "getVelocityX", "getVelocityY",
    // Tree — public field conflicts
    "getSelection", "getRootNodes", "getIndentSpacing", "getYSpacing",
    // Tree.Node — public field conflicts
    "getChildren", "getIcon", "getTree",
    // Table — public field conflicts
    "getBackground", "getCells", "getTableDebug", "getColumns", "getSkin",
    // Container — builder method conflicts (def fillX(): Container vs def getFillX: Float)
    "getFillX", "getFillY", "getClip",
    // Dialog — public field conflicts
    "getContentTable", "getButtonTable",
    // HorizontalGroup / VerticalGroup — builder method conflicts
    "getReverse", "getWrapReverse", "getFill", "getExpand", "getWrap",
    // TextField — public field conflicts
    "getFocusTraversal", "getMessageText", "getProgrammaticChangeEvents", "getSelectionStart",
    // ArraySelection
    "getRangeSelect",
    // Drawable field conflicts
    "getPatch", "getSprite", "getRegion", "getColor",
    // SelectBox / TextButton / ImageTextButton — field conflict + bare name calls
    "getFontColor",
    // HttpStatus (opaque type conflict)
    "getStatusCode",
    // AssetManager (has type params)
    "getAll",
    // Pool/PoolManager
    "getFree", "getPool",

    // --- Additional JDK methods (called via reflection or on JDK types) ---
    "getRuntime", // java.lang.Runtime.getRuntime()
    "getKey", // java.util.Map.Entry.getKey()
    "getColorModel", "getRaster", // java.awt.image.BufferedImage
    "getContext", // Android context APIs
    "getContents", // clipboard contents (JDK interop boundary)
    "getAndroidVersion", "getNativeHeapAllocatedSize" // Android platform ops
  )

  /** Custom name mappings that override the default lowercasing. */
  private val customNames: Map[String, String] = Map(
    "getGLType" -> "glType",
    "getGlInternalFormat" -> "glInternalFormat"
  )

  /** Compute the new name from a getXxx method name. Returns None if excluded. */
  private def newName(name: String): Option[String] = {
    if (permanentExclusions.contains(name)) None
    else customNames.get(name).orElse {
      if (name.startsWith("get") && name.length > 3 && name.charAt(3).isUpper) {
        Some(name.charAt(3).toLower + name.substring(4))
      } else {
        None
      }
    }
  }

  /** Check if a Defn.Def has no value parameters (only empty or using/implicit). */
  private def isNoArgDef(paramClauses: List[Member.ParamClauseGroup]): Boolean =
    paramClauses.isEmpty ||
      paramClauses.forall { group =>
        group.paramClauses.isEmpty ||
          group.paramClauses.forall {
            case Term.ParamClause(Nil, _) => true
            case pc if pc.values.forall(isUsingOrImplicit) => true
            case _ => false
          }
      }

  private def isUsingOrImplicit(param: Term.Param): Boolean =
    param.mods.exists {
      case Mod.Using() => true
      case Mod.Implicit() => true
      case _ => false
    }

  /** Find and remove the first empty `()` param clause from param clause groups.
    * This converts `def xxx(): T` to `def xxx: T` (property style).
    * Only removes empty `()`, not `(using Sge)` or `(implicit x: T)`.
    */
  private def removeLeadingEmptyParens(groups: List[Member.ParamClauseGroup]): Patch = {
    val emptyClause = groups.flatMap(_.paramClauses).find(_.values.isEmpty)
    emptyClause.fold(Patch.empty)(pc => Patch.removeTokens(pc.tokens))
  }

  /** Check if the given Term.Select is the function part of ANY Apply or ApplyType. */
  private def isFunctionOfApplyOrTypeApply(sel: Term.Select): Boolean =
    sel.parent match {
      case Some(Term.Apply.After_4_6_0(fn, _)) if fn.eq(sel) => true
      case Some(Term.ApplyType.After_4_6_0(fn, _)) if fn.eq(sel) => true
      case _ => false
    }

  /** Check if the given Term.Select is used in a Term.Apply with non-empty args. */
  private def isApplyWithArgs(sel: Term.Select): Boolean =
    sel.parent match {
      case Some(Term.Apply.After_4_6_0(fn, Term.ArgClause(args, _))) if fn.eq(sel) =>
        args.nonEmpty
      case Some(ta @ Term.ApplyType.After_4_6_0(fn, _)) if fn.eq(sel) =>
        ta.parent match {
          case Some(Term.Apply.After_4_6_0(fn2, Term.ArgClause(args2, _))) if fn2.eq(ta) =>
            args2.nonEmpty
          case _ => false
        }
      case _ => false
    }

  /** Check if a bare Term.Name is already handled by another pattern. */
  private def isHandledByOtherPattern(n: Term.Name): Boolean =
    n.parent match {
      case Some(_: Term.Select) => true
      case Some(Term.Apply.After_4_6_0(fn, _)) if fn.eq(n) => true
      case Some(_: Defn.Def) => true
      case Some(_: Decl.Def) => true
      case Some(_: Pat.Var) => true // local val/var pattern names like `val getWidth = ...`
      case _ => false
    }

  // --- Field conflict resolution ---

  /** Information about a template (class/trait/object body) needed for field conflict detection. */
  private final case class TemplateInfo(
    templ:          Template,
    ctorParamNames: Set[String],
    privateFields:  Set[String], // names of private/protected fields
    publicFields:   Set[String], // names of public fields
    getterNames:    Set[String]  // names of getXxx no-arg methods defined in this template
  )

  /** Check if a modifier list indicates private or protected access. */
  private def isNonPublic(mods: List[Mod]): Boolean =
    mods.exists {
      case _: Mod.Private   => true
      case _: Mod.Protected => true
      case _ => false
    }

  /** Collect field names from a template's stats. */
  private def collectFields(stats: List[Stat]): (Set[String], Set[String]) = {
    var privateFields = Set.empty[String]
    var publicFields = Set.empty[String]
    stats.foreach {
      case Defn.Var.After_4_7_2(mods, List(Pat.Var(Term.Name(n))), _, _) =>
        if (isNonPublic(mods)) privateFields += n else publicFields += n
      case v: Defn.Val =>
        v.pats match {
          case List(Pat.Var(Term.Name(n))) =>
            if (isNonPublic(v.mods)) privateFields += n else publicFields += n
          case _ => ()
        }
      case Decl.Var(mods, List(Pat.Var(Term.Name(n))), _) =>
        if (isNonPublic(mods)) privateFields += n else publicFields += n
      case Decl.Val(mods, List(Pat.Var(Term.Name(n))), _) =>
        if (isNonPublic(mods)) privateFields += n else publicFields += n
      case _ => ()
    }
    (privateFields, publicFields)
  }

  /** Collect constructor parameter names from a class definition. */
  private def collectCtorParams(tree: Tree): Set[String] =
    tree match {
      case Defn.Class.After_4_6_0(_, _, _, ctor, _) =>
        ctor.paramClauses.flatMap(_.values).map(_.name.value).toSet
      case _ => Set.empty
    }

  /** Collect no-arg getXxx method names defined in the given template stats. */
  private def collectGetterDefs(stats: List[Stat]): Set[String] =
    stats.collect {
      case Defn.Def.After_4_7_3(_, Term.Name(name), paramClauseGroup, _, _)
          if newName(name).isDefined && isNoArgDef(paramClauseGroup) =>
        name
      case Decl.Def.After_4_7_3(_, Term.Name(name), paramClauseGroup, _)
          if newName(name).isDefined && isNoArgDef(paramClauseGroup) =>
        name
    }.toSet

  /** Build a map of fieldName -> "_fieldName" for private fields that conflict with getter renames. */
  private def computeFieldRenames(info: TemplateInfo): Map[String, String] = {
    val result = scala.collection.mutable.Map.empty[String, String]
    info.getterNames.foreach { getterName =>
      newName(getterName).foreach { propName =>
        if (info.privateFields.contains(propName) && !info.ctorParamNames.contains(propName)) {
          result(propName) = "_" + propName
        }
        // If propName conflicts with a constructor param, the getter stays excluded
        // (handled by checking ctorParamNames in shouldSkipGetter)
      }
    }
    result.toMap
  }

  /** Check if a getXxx method should NOT be renamed because the new name conflicts with
    * a constructor param or public field.
    */
  private def shouldSkipGetter(getterName: String, info: TemplateInfo): Boolean = {
    newName(getterName) match {
      case Some(propName) =>
        // Skip if propName is a constructor param (can't rename ctor params safely)
        info.ctorParamNames.contains(propName) ||
        // Skip if propName is a public field (can't rename across files)
        info.publicFields.contains(propName)
      case None => true // already excluded
    }
  }

  /** Check if a Term.Name referring to a field being renamed is shadowed by a local parameter. */
  private def isShadowedByParam(node: Tree, fieldName: String): Boolean = {
    var current: Option[Tree] = node.parent
    while (current.isDefined) {
      current.get match {
        case d: Defn.Def =>
          val paramNames = d.paramClauseGroups.flatMap(_.paramClauses).flatMap(_.values).map(_.name.value)
          if (paramNames.contains(fieldName)) return true
          // Don't stop — could be nested, continue to check enclosing
        case _: Ctor.Secondary =>
          val ctor = current.get.asInstanceOf[Ctor.Secondary]
          val paramNames = ctor.paramClauses.flatMap(_.values).map(_.name.value)
          if (paramNames.contains(fieldName)) return true
        case _: Template =>
          // Reached template level — check class constructor params
          current.get.parent match {
            case Some(Defn.Class.After_4_6_0(_, _, _, ctor, _)) =>
              val ctorParamNames = ctor.paramClauses.flatMap(_.values).map(_.name.value)
              return ctorParamNames.contains(fieldName)
            case _ => return false
          }
        case _ => ()
      }
      current = current.get.parent
    }
    false
  }

  /** Check if a node is inside a given template. */
  private def isInsideTemplate(node: Tree, templ: Template): Boolean = {
    var current: Option[Tree] = node.parent
    while (current.isDefined) {
      if (current.get.eq(templ)) return true
      // Stop at the first template boundary (don't cross into inner classes)
      current.get match {
        case _: Template if !current.get.eq(templ) => return false
        case _ => ()
      }
      current = current.get.parent
    }
    false
  }

  /** Generate patches for renaming field declarations and their references within a template. */
  private def generateFieldRenamePatches(templ: Template, fieldRenames: Map[String, String]): List[Patch] = {
    if (fieldRenames.isEmpty) return Nil

    val patches = scala.collection.mutable.ListBuffer.empty[Patch]

    templ.collect {
      // Field declarations
      case Defn.Var.After_4_7_2(_, List(Pat.Var(name @ Term.Name(n))), _, _) if fieldRenames.contains(n) =>
        patches += Patch.replaceTree(name, fieldRenames(n))

      case v: Defn.Val if v.pats.headOption.exists {
            case Pat.Var(Term.Name(n)) => fieldRenames.contains(n)
            case _ => false
          } =>
        v.pats.head match {
          case Pat.Var(name @ Term.Name(n)) => patches += Patch.replaceTree(name, fieldRenames(n))
          case _ => ()
        }

      case Decl.Var(_, List(Pat.Var(name @ Term.Name(n))), _) if fieldRenames.contains(n) =>
        patches += Patch.replaceTree(name, fieldRenames(n))

      // this.fieldName -> this._fieldName
      case Term.Select(Term.This(_), name @ Term.Name(n)) if fieldRenames.contains(n) =>
        patches += Patch.replaceTree(name, fieldRenames(n))

      // Bare fieldName reference (not in a select, not shadowed by param)
      case name @ Term.Name(n) if fieldRenames.contains(n) =>
        name.parent match {
          // Skip if this is inside Term.Select(_, name) — already handled above
          case Some(Term.Select(_, child)) if child.eq(name) => ()
          // Skip if this is a Term.Select(name, _) — 'name' is the qualifier, not a field ref
          // (e.g., fieldName.someMethod — 'fieldName' here IS a field ref, keep it)
          // Skip if this is a parameter declaration
          case Some(_: Term.Param) => ()
          // Skip if this is a Pat.Var in a val/var (could be the declaration itself or a local)
          case Some(Pat.Var(_)) => ()
          // Skip if this is a method/class/trait name
          case Some(_: Defn.Def) | Some(_: Defn.Class) | Some(_: Defn.Trait) | Some(_: Defn.Object) => ()
          case Some(_: Decl.Def) => ()
          // Skip if this is inside a Term.Select where 'name' is the qualifier and it's THIS.name
          case _ =>
            // Check if shadowed by a local parameter
            if (!isShadowedByParam(name, n)) {
              patches += Patch.replaceTree(name, fieldRenames(n))
            }
        }
    }

    patches.toList
  }

  /** Collect TemplateInfo for all classes/traits/objects in the file. */
  private def collectTemplateInfos(tree: Tree): List[TemplateInfo] = {
    tree.collect {
      case cls @ Defn.Class.After_4_6_0(_, _, _, _, templ) =>
        val (privFields, pubFields) = collectFields(templ.stats)
        val ctorParams = collectCtorParams(cls)
        val getterDefs = collectGetterDefs(templ.stats)
        TemplateInfo(templ, ctorParams, privFields, pubFields, getterDefs)

      case Defn.Trait.After_4_6_0(_, _, _, _, templ) =>
        val (privFields, pubFields) = collectFields(templ.stats)
        val getterDefs = collectGetterDefs(templ.stats)
        TemplateInfo(templ, Set.empty, privFields, pubFields, getterDefs)

      case Defn.Object(_, _, templ) =>
        val (privFields, pubFields) = collectFields(templ.stats)
        val getterDefs = collectGetterDefs(templ.stats)
        TemplateInfo(templ, Set.empty, privFields, pubFields, getterDefs)
    }
  }

  /** Find the TemplateInfo for the template containing a given tree node. */
  private def findContainingTemplateInfo(node: Tree, infos: List[TemplateInfo]): Option[TemplateInfo] = {
    var current: Option[Tree] = node.parent
    while (current.isDefined) {
      current.get match {
        case templ: Template =>
          return infos.find(_.templ.eq(templ))
        case _ => ()
      }
      current = current.get.parent
    }
    None
  }

  /** Check if a getter definition should be renamed, considering per-class conflicts. */
  private def shouldRenameGetter(name: String, node: Tree, infos: List[TemplateInfo]): Boolean = {
    if (newName(name).isEmpty) return false
    findContainingTemplateInfo(node, infos) match {
      case Some(info) => !shouldSkipGetter(name, info)
      case None => true // not in a class — rename freely
    }
  }

  override def fix(implicit doc: SyntacticDocument): Patch = {
    // Phase 1: Collect template info
    val infos = collectTemplateInfos(doc.tree)

    // Phase 2: Generate field rename patches
    val fieldPatches = infos.flatMap { info =>
      val fieldRenames = computeFieldRenames(info)
      generateFieldRenamePatches(info.templ, fieldRenames)
    }

    // Phase 3: Generate getter rename patches (definitions + call sites)
    val getterPatches = doc.tree.collect {
      // --- Definition sites ---

      // def getXxx(): T = ... or def getXxx: T = ... (concrete, no params or only using/implicit)
      case d @ Defn.Def.After_4_7_3(_, Term.Name(name), paramClauseGroup, _, _)
          if shouldRenameGetter(name, d, infos) && isNoArgDef(paramClauseGroup) =>
        Patch.replaceTree(d.name, newName(name).get) + removeLeadingEmptyParens(paramClauseGroup)

      // def getXxx(): T or def getXxx: T (abstract declaration, no params or only using/implicit)
      case d @ Decl.Def.After_4_7_3(_, Term.Name(name), paramClauseGroup, _)
          if shouldRenameGetter(name, d, infos) && isNoArgDef(paramClauseGroup) =>
        Patch.replaceTree(d.name, newName(name).get) + removeLeadingEmptyParens(paramClauseGroup)

      // --- Call sites ---

      // obj.getXxx() → obj.xxx (qualified call with empty arg list)
      case Term.Apply.After_4_6_0(
            sel @ Term.Select(_, Term.Name(name)),
            argClause @ Term.ArgClause(Nil, _)
          ) if newName(name).isDefined =>
        Patch.replaceTree(sel.name, newName(name).get) + Patch.removeTokens(argClause.tokens)

      // getXxx() → xxx (unqualified call with empty arg list)
      case Term.Apply.After_4_6_0(
            n @ Term.Name(name),
            argClause @ Term.ArgClause(Nil, _)
          ) if newName(name).isDefined =>
        Patch.replaceTree(n, newName(name).get) + Patch.removeTokens(argClause.tokens)

      // obj.getXxx (no parens, bare select — only if NOT part of Apply/ApplyType)
      case t @ Term.Select(_, Term.Name(name))
          if newName(name).isDefined && !isFunctionOfApplyOrTypeApply(t) && !isApplyWithArgs(t) =>
        Patch.replaceTree(t.name, newName(name).get)

      // getXxx (bare unqualified reference — no parens, no qualifier)
      // Only if not already handled by other patterns (Apply, Select, Def)
      case n @ Term.Name(name)
          if newName(name).isDefined && !isHandledByOtherPattern(n) =>
        Patch.replaceTree(n, newName(name).get)

    }

    fieldPatches.asPatch + getterPatches.asPatch
  }
}
