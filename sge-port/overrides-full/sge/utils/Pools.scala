package sge.utils

/** INJECTED SCALA (Substitutions.inject) — the portable replacement for libGDX's `Pools`. */
object Pools {

  private final val typePools: lowlevel.util.ObjectMap[java.lang.Class[?], Pool[?]] =
    lowlevel.util.ObjectMap[java.lang.Class[?], Pool[?]]()

  /** The upstream `static { … }` block, ported by hand: every type libGDX itself pools is
    * pre-registered with its constructor as the factory. This is what makes the `Class`-keyed
    * lookups above resolve WITHOUT ever needing to construct from a `Class` — the reflective
    * fallback existed only because these registrations were not exhaustive for user types. */
  def registerDefaults()(using sge.Sge): Unit = {
    Pools.set(() => lowlevel.util.DynamicArray[java.lang.Object]())
    Pools.set(() => new sge.scenes.scene2d.utils.ChangeListener.ChangeEvent())
    Pools.set(() => new sge.scenes.scene2d.ui.Table.DebugRect())
    Pools.set(() => new sge.scenes.scene2d.utils.FocusListener.FocusEvent())
    Pools.set(() => new sge.graphics.g2d.GlyphLayout.GlyphRun())
    Pools.set(() => new sge.graphics.g2d.GlyphLayout())
    Pools.set(() => new sge.Net.HttpRequest())
    Pools.set(() => new sge.scenes.scene2d.InputEvent())
    Pools.set(() => new sge.math.Rectangle())
    Pools.set(() => new sge.scenes.scene2d.Stage.TouchFocus())
    // Actions
    Pools.set(() => new sge.scenes.scene2d.actions.AddAction())
    Pools.set(() => new sge.scenes.scene2d.actions.AddListenerAction())
    Pools.set(() => new sge.scenes.scene2d.actions.AfterAction())
    Pools.set(() => new sge.scenes.scene2d.actions.AlphaAction())
    Pools.set(() => new sge.scenes.scene2d.actions.ColorAction())
    Pools.set(() => new sge.scenes.scene2d.actions.DelayAction())
    Pools.set(() => new sge.scenes.scene2d.actions.FloatAction())
    Pools.set(() => new sge.scenes.scene2d.actions.IntAction())
    Pools.set(() => new sge.scenes.scene2d.actions.LayoutAction())
    Pools.set(() => new sge.scenes.scene2d.actions.MoveByAction())
    Pools.set(() => new sge.scenes.scene2d.actions.MoveToAction())
    Pools.set(() => new sge.scenes.scene2d.actions.ParallelAction())
    Pools.set(() => new sge.scenes.scene2d.actions.RemoveAction())
    Pools.set(() => new sge.scenes.scene2d.actions.RemoveActorAction())
    Pools.set(() => new sge.scenes.scene2d.actions.RemoveListenerAction())
    Pools.set(() => new sge.scenes.scene2d.actions.RepeatAction())
    Pools.set(() => new sge.scenes.scene2d.actions.RotateByAction())
    Pools.set(() => new sge.scenes.scene2d.actions.RotateToAction())
    Pools.set(() => new sge.scenes.scene2d.actions.RunnableAction())
    Pools.set(() => new sge.scenes.scene2d.actions.ScaleByAction())
    Pools.set(() => new sge.scenes.scene2d.actions.ScaleToAction())
    Pools.set(() => new sge.scenes.scene2d.actions.SequenceAction())
    Pools.set(() => new sge.scenes.scene2d.actions.SizeByAction())
    Pools.set(() => new sge.scenes.scene2d.actions.SizeToAction())
    Pools.set(() => new sge.scenes.scene2d.actions.TimeScaleAction())
    Pools.set(() => new sge.scenes.scene2d.actions.TouchableAction())
    Pools.set(() => new sge.scenes.scene2d.actions.VisibleAction())
  }

  /** Registers an existing pool for the specified type. */
  def set[T <: java.lang.Object](`type`: java.lang.Class[T], pool: Pool[T]): Unit =
    Pools.typePools.put(`type`, pool)

  /** Registers a factory-backed pool, keyed by the class of an instance the factory produces.
    * `Pools.set(() => new MyClass, 50)`. */
  def set[T <: java.lang.Object](factory: () => T, max: Int): Unit = {
    val probe = factory()
    Pools.set(
      probe.asInstanceOf[java.lang.Object].getClass().asInstanceOf[java.lang.Class[T]],
      new DefaultPool[T](() => factory(), 4, max),
    )
  }

  /** Registers a factory-backed pool with the default maximum of 100. */
  def set[T <: java.lang.Object](factory: () => T): Unit = Pools.set(factory, 100)

  /** The registered pool for `type`, or `null` if there is none. */
  def getOrNull[T <: java.lang.Object](`type`: java.lang.Class[T]): Pool[T] = {
    val p = Pools.typePools.get(`type`)
    if (p.isEmpty) null else p.get.asInstanceOf[Pool[T]]
  }

  /** The registered pool for `type`. Unlike the Java original this does NOT create one on a miss —
    * a pool cannot construct `T` from a `Class` without reflection. Use the `factory` overload (or
    * `set`) to register one. */
  def get[T <: java.lang.Object](`type`: java.lang.Class[T]): Pool[T] = {
    val pool = Pools.getOrNull(`type`)
    if (pool == null) {
      throw new GdxRuntimeException(
        "No Pool registered for " + `type` + " — register one with Pools.set(" +
          `type`.getSimpleName() + "::new), use Pools.get(type, factory), or call " +
          "Pools.registerDefaults() at start-up for the types libGDX itself pools"
      )
    }
    pool
  }

  /** The registered pool for `type`, creating a factory-backed one on a miss. This is the direct
    * replacement for the Java `get(type, max)` reflective fallback: same shape, but the caller
    * supplies the construction. */
  def get[T <: java.lang.Object](`type`: java.lang.Class[T], factory: () => T, max: Int): Pool[T] = {
    val existing: Pool[T] = Pools.getOrNull(`type`)
    if (existing != null) { existing }
    else {
      val pool = new DefaultPool[T](() => factory(), 4, max)
      Pools.typePools.put(`type`, pool)
      pool
    }
  }

  /** As `get(type, factory, max)` with the default maximum of 100. */
  def get[T <: java.lang.Object](`type`: java.lang.Class[T], factory: () => T): Pool[T] = Pools.get(`type`, factory, 100)

  /** Obtains an object from the registered pool for `type`. */
  def obtain[T <: java.lang.Object](`type`: java.lang.Class[T]): T = Pools.get(`type`).obtain()

  /** Obtains an object from the pool for `type`, registering a factory-backed pool on a miss. */
  def obtain[T <: java.lang.Object](`type`: java.lang.Class[T], factory: () => T): T = Pools.get(`type`, factory).obtain()

  /** Frees an object to the pool registered for its runtime class. `getClass` is supported on every
    * target — only INSTANTIATION from a `Class` is not — so the lookup side is unchanged. */
  def free(`object`: java.lang.Object): Unit = {
    if (`object` == null) { throw new java.lang.IllegalArgumentException("object cannot be null.") }
    val pool = Pools.typePools.get(`object`.getClass())
    if (pool.isEmpty) { () }
    else { pool.get.asInstanceOf[Pool[java.lang.Object]].free(`object`) }
  }

  /** Frees the specified objects. Null objects within the array are silently ignored. */
  def freeAll(objects: lowlevel.util.DynamicArray[?]): Unit = Pools.freeAll(objects, false)

  /** Frees the specified objects.
    * @param samePool if true the pool is looked up once and reused for every object. */
  def freeAll(objects: lowlevel.util.DynamicArray[?], samePool: Boolean): Unit = {
    if (objects == null) { throw new java.lang.IllegalArgumentException("objects cannot be null.") }
    var pool: lowlevel.Nullable[Pool[?]] = lowlevel.Nullable.empty
    var i             = 0
    val n             = objects.size
    while (i < n) {
      val obj = objects(i).asInstanceOf[java.lang.Object]
      if (obj != null) {
        if (pool.isEmpty) { pool = Pools.typePools.get(obj.getClass()) }
        if (!pool.isEmpty) {
          pool.get.asInstanceOf[Pool[java.lang.Object]].free(obj)
          if (!samePool) { pool = lowlevel.Nullable.empty }
        }
      }
      i += 1
    }
  }
}
