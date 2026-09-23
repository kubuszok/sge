/*
 * Ported from gdx-ai — https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/utils/BehaviorTreeParser.java (the reflective half)
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 */

/** Injected — the reflection-free stand-in for what `DefaultBehaviorTreeReader` asked the JVM at
  * run time (`ClassReflection.newInstance/getAnnotation/getField` become [[TaskRegistry.newTask]],
  * [[TaskRegistry.metaOf]], [[TaskRegistry.fieldOf]]); five reader method bodies substitute onto
  * this table (`MethodBodyTransform`, `GdxAiPolicy`). Closed where java's mechanism is open: an
  * unregistered task type refuses loudly rather than silently matching nothing. */
package sge.ai.btree.utils

/** The reflection-free task table `DefaultBehaviorTreeReader` resolves against.
  *
  * Mutable and process-global, because the thing it replaces is too: java's answer came from the
  * classpath, which is one per JVM. Register before parsing.
  */
object TaskRegistry {

  /** one `@TaskAttribute` slot, as `findMetadata` needs to see it. */
  final class Attr private[utils] (val name: String, val fieldName: String, val required: Boolean,
                                   val field: TaskField)

  /** one task class's `@TaskConstraint` plus every `@TaskAttribute` in its hierarchy — what java
    * built out of `getAnnotation` and `getFields`. */
  final class Meta private[utils] (val minChildren: Int, val maxChildren: Int,
                                   val attributes: Array[Attr])

  private val factories  = scala.collection.mutable.LinkedHashMap.empty[String, () => sge.ai.btree.Task[java.lang.Object]]
  private val constraints = scala.collection.mutable.LinkedHashMap.empty[String, (Int, Int)]
  private val attributes  = scala.collection.mutable.LinkedHashMap.empty[String, List[Attr]]

  // -------------------------------------------------------------------------------------------
  // what the substituted bodies call
  // -------------------------------------------------------------------------------------------

  /** java's `ClassReflection.newInstance(ClassReflection.forName(className))`. */
  def newTask(className: String): sge.ai.btree.Task[java.lang.Object] = {
    factories.get(className) match {
      case Some(f) => f()
      case None    =>
        throw new sge.utils.reflect.ReflectionException(
          s"no task registered for '$className' — this port resolves task names through " +
            "sge.ai.btree.utils.TaskRegistry rather than through runtime reflection, which the " +
            "libGDX base drops. Register it with TaskRegistry.register(\"" + className + "\", () => …)")
    }
  }

  /** java's `getAnnotation(clazz, TaskConstraint.class)` + `getFields` + `getDeclaredAnnotation(
    * TaskAttribute.class)`, over the registration table. */
  def metaOf(clazz: java.lang.Class[?]): Meta = {
    var c: java.lang.Class[?] = clazz
    var min                   = 0
    var max                   = 0
    var found                 = false
    val attrs                 = scala.collection.mutable.LinkedHashMap.empty[String, Attr]
    while (c != null) {
      val n = c.getName
      if (!found) {
        constraints.get(n).foreach { mm => min = mm._1; max = mm._2; found = true }
      }
      attributes.get(n).foreach(_.foreach(a => if (!attrs.contains(a.name)) { attrs.put(a.name, a) }))
      c = c.getSuperclass.asInstanceOf[java.lang.Class[?]]
    }
    if (found) { new Meta(min, max, attrs.values.toArray) } else { null }
  }

  /** java's `ClassReflection.getField(clazz, fieldName)`, restricted to the slots this port knows
    * how to coerce and store. `null` where there is none — `getField`'s substituted body turns that
    * into the `GdxRuntimeException` java raised for a `ReflectionException`. */
  def fieldOf(clazz: java.lang.Class[?], fieldName: String): TaskField = {
    var c: java.lang.Class[?]  = clazz
    var out: TaskField = null
    while (c != null && out == null) {
      attributes.get(c.getName).foreach(_.foreach(a => if (out == null && a.fieldName == fieldName) { out = a.field }))
      c = c.getSuperclass.asInstanceOf[java.lang.Class[?]]
    }
    out
  }

  // -------------------------------------------------------------------------------------------
  // registration — what a consumer's own task class needs, and what java needed nothing for
  // -------------------------------------------------------------------------------------------

  /** the `@TaskConstraint` of a task class, by its runtime class name. Inherited by every subclass
    * with no constraint of its own, exactly as java's `@Inherited` annotation was. */
  def constrain(className: String, minChildren: Int, maxChildren: Int): Unit =
    constraints.put(className, (minChildren, maxChildren))

  /** the factory `ClassReflection.newInstance` stood for. Registered under EVERY name a `.btree`
    * file may spell the class as — see [[registerBuiltins]] on why there are two per built-in. */
  def register(className: String, factory: () => sge.ai.btree.Task[java.lang.Object]): Unit =
    factories.put(className, factory)

  /** one `@TaskAttribute` field of a task class. */
  def attribute(className: String, name: String, fieldName: String, required: Boolean,
                typeName: String,
                cast: (java.lang.Object, DistributionAdapters) => java.lang.Object,
                set: (java.lang.Object, java.lang.Object) => Unit): Unit = {
    val a = new Attr(name, fieldName, required, new TaskField(fieldName, typeName, cast, set))
    attributes.put(className, attributes.getOrElse(className, Nil).filterNot(_.name == name) :+ a)
  }

  // -------------------------------------------------------------------------------------------
  // the coercions — java's `castValue`, one branch per field TYPE rather than one `switch`
  // -------------------------------------------------------------------------------------------

  /** `castValue`'s `String` branch (`BehaviorTreeParser.java:437`). */
  def asString(value: java.lang.Object): java.lang.Object = value match {
    case s: java.lang.String => s
    case _                   => null
  }

  /** `castValue`'s `Boolean` branch (`:433`). */
  def asBoolean(value: java.lang.Object): java.lang.Object = value match {
    case b: java.lang.Boolean => b
    case _                    => null
  }

  /** `castValue`'s two `Distribution` branches (`:428` for a number, `:442` for a string), which
    * are one function of the target distribution type. The `"constant," + n` spelling is java's
    * own. */
  def asDistribution[T <: sge.ai.utils.random.Distribution](
      value: java.lang.Object, tpe: java.lang.Class[T], adapters: DistributionAdapters,
  ): java.lang.Object = value match {
    case n: java.lang.Number => adapters.toDistribution("constant," + n, tpe)
    case s: java.lang.String => adapters.toDistribution(s, tpe)
    case _                   => null
  }

  /** `castValue`'s enum branch (`:446`) — the constant whose `name()` equals the string, ignoring
    * case, and `null` where none does. The constants are handed in rather than read off the class,
    * because `Class#getEnumConstants` is reflection and the emitted enum is a sealed hierarchy the
    * JVM does not know is one. */
  def asEnum[T <: java.lang.Object](value: java.lang.Object, constants: Array[T],
                                    nameOf: T => String): java.lang.Object = value match {
    case s: java.lang.String =>
      var i:   Int              = 0
      var out: java.lang.Object = null
      while (i < constants.length && out == null) {
        if (nameOf(constants(i)).equalsIgnoreCase(s)) { out = constants(i) }
        i += 1
      }
      out
    case _ => null
  }

  // -------------------------------------------------------------------------------------------
  // the built-ins
  // -------------------------------------------------------------------------------------------

  /** Register everything `DEFAULT_IMPORTS` names, plus the six `@TaskConstraint` sites the whole
    * `btree` hierarchy has. */
  private def registerBuiltins(): Unit = {
    // ---- the six @TaskConstraint sites, by their emitted class names ----
    // Task            @TaskConstraint                                  -> defaults (0, MAX_VALUE)
    constrain("sge.ai.btree.Task", 0, java.lang.Integer.MAX_VALUE)
    // BranchTask      @TaskConstraint(minChildren = 1)
    constrain("sge.ai.btree.BranchTask", 1, java.lang.Integer.MAX_VALUE)
    // Decorator       @TaskConstraint(minChildren = 1, maxChildren = 1)
    constrain("sge.ai.btree.Decorator", 1, 1)
    // LeafTask        @TaskConstraint(minChildren = 0, maxChildren = 0)
    constrain("sge.ai.btree.LeafTask", 0, 0)
    // Include         @TaskConstraint(minChildren = 0, maxChildren = 0)
    constrain("sge.ai.btree.decorator.Include", 0, 0)
    // Random          @TaskConstraint(minChildren = 0, maxChildren = 1)
    constrain("sge.ai.btree.decorator.Random", 0, 1)

    // ---- the eighteen DEFAULT_IMPORTS classes ----
    both("branch.Selector", () => new sge.ai.btree.branch.Selector[java.lang.Object]())
    both("branch.Sequence", () => new sge.ai.btree.branch.Sequence[java.lang.Object]())
    both("branch.RandomSelector", () => new sge.ai.btree.branch.RandomSelector[java.lang.Object]())
    both("branch.RandomSequence", () => new sge.ai.btree.branch.RandomSequence[java.lang.Object]())
    both("branch.DynamicGuardSelector", () => new sge.ai.btree.branch.DynamicGuardSelector[java.lang.Object]())
    both("branch.Parallel", () => new sge.ai.btree.branch.Parallel[java.lang.Object]())
    both("decorator.AlwaysFail", () => new sge.ai.btree.decorator.AlwaysFail[java.lang.Object]())
    both("decorator.AlwaysSucceed", () => new sge.ai.btree.decorator.AlwaysSucceed[java.lang.Object]())
    both("decorator.Invert", () => new sge.ai.btree.decorator.Invert[java.lang.Object]())
    both("decorator.UntilFail", () => new sge.ai.btree.decorator.UntilFail[java.lang.Object]())
    both("decorator.UntilSuccess", () => new sge.ai.btree.decorator.UntilSuccess[java.lang.Object]())
    both("decorator.SemaphoreGuard", () => new sge.ai.btree.decorator.SemaphoreGuard[java.lang.Object]())
    both("decorator.Repeat", () => new sge.ai.btree.decorator.Repeat[java.lang.Object]())
    both("decorator.Include", () => new sge.ai.btree.decorator.Include[java.lang.Object]())
    // the one factory that is not a bare `new`, and it is not a choice. Java's `Random()` delegates
    // — `this(ConstantFloatDistribution.ZERO_POINT_FIVE)` — and the port's emitted `Random` has no
    // nilary constructor to carry that: scala's implicit nilary primary runs nothing, and
    // `def this()` beside it is `E120`.
    both("decorator.Random", () => {
      val t = new sge.ai.btree.decorator.Random[java.lang.Object]()
      t.success$shadow = sge.ai.utils.random.ConstantFloatDistribution.ZERO_POINT_FIVE
      t
    })
    both("leaf.Success", () => new sge.ai.btree.leaf.Success[java.lang.Object]())
    both("leaf.Failure", () => new sge.ai.btree.leaf.Failure[java.lang.Object]())
    both("leaf.Wait", () => new sge.ai.btree.leaf.Wait[java.lang.Object]())

    // ---- the eight @TaskAttribute fields ----
    // Wait: @TaskAttribute(required = true) public FloatDistribution seconds;
    attribute("sge.ai.btree.leaf.Wait", "seconds", "seconds", true, "FloatDistribution",
      (v, a) => asDistribution(v, classOf[sge.ai.utils.random.FloatDistribution], a),
      (t, v) => t.asInstanceOf[sge.ai.btree.leaf.Wait[java.lang.Object]].seconds =
        v.asInstanceOf[sge.ai.utils.random.FloatDistribution])
    // Include: @TaskAttribute(required = true) public String subtree;
    attribute("sge.ai.btree.decorator.Include", "subtree", "subtree", true, "String",
      (v, _) => asString(v),
      (t, v) => t.asInstanceOf[sge.ai.btree.decorator.Include[java.lang.Object]].subtree =
        v.asInstanceOf[java.lang.String])
    // Include: @TaskAttribute public boolean lazy;  — `lazy` is a scala keyword, so the emitted
    // member is back-quoted; the ATTRIBUTE and the java field are still spelled `lazy`, which is
    // what the `.btree` text and `AttrInfo.fieldName` both carry.
    attribute("sge.ai.btree.decorator.Include", "lazy", "lazy", false, "boolean",
      (v, _) => asBoolean(v),
      (t, v) => t.asInstanceOf[sge.ai.btree.decorator.Include[java.lang.Object]].`lazy` =
        v.asInstanceOf[java.lang.Boolean].booleanValue())
    // Repeat: @TaskAttribute public IntegerDistribution times;
    attribute("sge.ai.btree.decorator.Repeat", "times", "times", false, "IntegerDistribution",
      (v, a) => asDistribution(v, classOf[sge.ai.utils.random.IntegerDistribution], a),
      (t, v) => t.asInstanceOf[sge.ai.btree.decorator.Repeat[java.lang.Object]].times =
        v.asInstanceOf[sge.ai.utils.random.IntegerDistribution])
    // Random: @TaskAttribute public FloatDistribution success;  — the field shadows an inherited
    // one, so CLAUDE.md §4.55 renamed the emitted member to `success$shadow`. The java field is still
    // `success`, which is the name the `.btree` text uses and the name `getField` is handed.
    attribute("sge.ai.btree.decorator.Random", "success", "success", false, "FloatDistribution",
      (v, a) => asDistribution(v, classOf[sge.ai.utils.random.FloatDistribution], a),
      (t, v) => t.asInstanceOf[sge.ai.btree.decorator.Random[java.lang.Object]].success$shadow =
        v.asInstanceOf[sge.ai.utils.random.FloatDistribution])
    // SemaphoreGuard: @TaskAttribute(required = true) public String name;
    attribute("sge.ai.btree.decorator.SemaphoreGuard", "name", "name", true, "String",
      (v, _) => asString(v),
      (t, v) => t.asInstanceOf[sge.ai.btree.decorator.SemaphoreGuard[java.lang.Object]].name =
        v.asInstanceOf[java.lang.String])
    // Parallel: @TaskAttribute public Policy policy;
    attribute("sge.ai.btree.branch.Parallel", "policy", "policy", false, "Policy",
      (v, _) => asEnum(v, sge.ai.btree.branch.Parallel.Policy.values(), (p: sge.ai.btree.branch.Parallel.Policy) => p.name()),
      (t, v) => t.asInstanceOf[sge.ai.btree.branch.Parallel[java.lang.Object]].policy =
        v.asInstanceOf[sge.ai.btree.branch.Parallel.Policy])
    // Parallel: @TaskAttribute public Orchestrator orchestrator;
    attribute("sge.ai.btree.branch.Parallel", "orchestrator", "orchestrator", false, "Orchestrator",
      (v, _) => asEnum(v, sge.ai.btree.branch.Parallel.Orchestrator.values(), (o: sge.ai.btree.branch.Parallel.Orchestrator) => o.name()),
      (t, v) => t.asInstanceOf[sge.ai.btree.branch.Parallel[java.lang.Object]].orchestrator =
        v.asInstanceOf[sge.ai.btree.branch.Parallel.Orchestrator])
  }

  /** register one built-in under BOTH the port's emitted FQCN and gdx-ai's upstream one — see
    * [[registerBuiltins]]. */
  private def both(suffix: String, factory: () => sge.ai.btree.Task[java.lang.Object]): Unit = {
    register("sge.ai.btree." + suffix, factory)
    register("com.badlogic.gdx.ai.btree." + suffix, factory)
  }

  registerBuiltins()
}
