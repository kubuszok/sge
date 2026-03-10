/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/AssetManager.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Disposable -> AutoCloseable; dispose() -> close(); AsyncExecutor -> ExecutionContext;
 *     injectDependencies -> addDependencies; ClassReflection.getSimpleName -> Class.getSimpleName;
 *     GdxRuntimeException -> SgeError.InvalidInput
 *   Convention: ObjectIntMap (Java) -> scala.collection.mutable.HashMap (clear() method);
 *     keys().toArray() -> foreachKey + DynamicArray; RefCountedContainer in companion object;
 *     synchronized methods preserved; Thread.yield() via backtick escaping
 *   Idiom: boundary/break (multiple return), Nullable (listener, assetFileName, loader),
 *     split packages
 *   Fixes: Java-style getters/setters → Scala property accessors
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package assets

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.util.boundary
import scala.util.control.NonFatal

import sge.assets.loaders.{
  AssetLoader,
  BitmapFontLoader,
  CubemapLoader,
  FileHandleResolver,
  I18NBundleLoader,
  MusicLoader,
  ParticleEffectLoader,
  PixmapLoader,
  ShaderProgramLoader,
  SkinLoader,
  SoundLoader,
  TextureAtlasLoader,
  TextureLoader
}
import sge.graphics.g2d.PolygonRegionLoader
import sge.graphics.g3d.loader.{ G3dBinaryModelLoader, G3dModelLoader, ObjLoader }
import sge.utils.{ DynamicArray, Nullable, ObjectMap, ObjectSet, SgeError, TimeUtils }

/** Loads and stores assets like textures, bitmapfonts, tile maps, sounds, music and so on.
  * @author
  *   mzechner (original implementation)
  */
class AssetManager(val resolver: FileHandleResolver, defaultLoaders: Boolean = true)(using Sge) extends AutoCloseable {

  private val assets:            ObjectMap[Class[?], ObjectMap[String, AssetManager.RefCountedContainer]] = ObjectMap()
  private val assetTypes:        ObjectMap[String, Class[?]]                                              = ObjectMap()
  private val assetDependencies: ObjectMap[String, DynamicArray[String]]                                  = ObjectMap()
  private val injected:          ObjectSet[String]                                                        = ObjectSet()

  private val loaders:   ObjectMap[Class[?], ObjectMap[String, AssetLoader[?, ?]]] = ObjectMap()
  private val loadQueue: DynamicArray[AssetDescriptor[?]]                          = DynamicArray()

  private val executorService: java.util.concurrent.ExecutorService =
    java.util.concurrent.Executors.newSingleThreadExecutor { (r: Runnable) =>
      val t = new Thread(r, "AssetManager")
      t.setDaemon(true)
      t
    }
  private val executor: ExecutionContext = ExecutionContext.fromExecutorService(executorService)

  private val tasks:     DynamicArray[AssetLoadingTask] = DynamicArray()
  private var listener:  Nullable[AssetErrorListener]   = Nullable.empty
  private var loaded:    Int                            = 0
  private var toLoad:    Int                            = 0
  private var peakTasks: Int                            = 0

  private val log: scribe.Logger = scribe.Logger("AssetManager")

  if (defaultLoaders) {
    setLoader[sge.graphics.g2d.BitmapFont](BitmapFontLoader(resolver))
    setLoader[sge.audio.Music](MusicLoader(resolver))
    setLoader[sge.graphics.Pixmap](PixmapLoader(resolver))
    setLoader[sge.audio.Sound](SoundLoader(resolver))
    setLoader[sge.graphics.g2d.TextureAtlas](TextureAtlasLoader(resolver))
    setLoader[sge.graphics.Texture](TextureLoader(resolver))
    setLoader[sge.scenes.scene2d.ui.Skin](SkinLoader(resolver))
    setLoader[sge.graphics.g2d.ParticleEffect](ParticleEffectLoader(resolver))
    setLoader[sge.graphics.g3d.particles.ParticleEffect](sge.graphics.g3d.particles.ParticleEffectLoader(resolver))
    setLoader[sge.graphics.g2d.PolygonRegion](PolygonRegionLoader(resolver))
    setLoader[sge.utils.I18NBundle](I18NBundleLoader(resolver))
    setLoader[sge.graphics.g3d.Model](".g3dj", G3dModelLoader(resolver))
    setLoader[sge.graphics.g3d.Model](".g3db", G3dBinaryModelLoader(resolver))
    setLoader[sge.graphics.g3d.Model](".obj", ObjLoader(resolver))
    setLoader[sge.graphics.glutils.ShaderProgram](ShaderProgramLoader(resolver))
    setLoader[sge.graphics.Cubemap](CubemapLoader(resolver))
  }

  /** Single code path for all asset lookups. Callers acquire locks at the public method level. */
  private def lookup[T](fileName: String, tpe: Nullable[Class[?]]): Nullable[T] = {
    val resolvedType = if (tpe.isDefined) tpe else assetTypes.get(fileName)
    for {
      t <- resolvedType
      byType <- assets.get(t)
      container <- byType.get(fileName)
    } yield container.obj.asInstanceOf[T]
  }

  /** @return
    *   the asset
    * @throws SgeError.InvalidInput
    *   if the asset is not loaded
    */
  def apply[T: ClassTag](fileName: String): T = synchronized {
    lookup[T](fileName, Nullable(summon[ClassTag[T]].runtimeClass)).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName))
  }

  def apply[T](fileName: String, tpe: Class[T]): T = synchronized {
    lookup[T](fileName, Nullable(tpe)).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName))
  }

  /** @return
    *   the asset
    * @throws SgeError.InvalidInput
    *   if the asset is not loaded
    */
  def apply[T](assetDescriptor: AssetDescriptor[T]): T = synchronized {
    lookup[T](assetDescriptor.fileName, Nullable(assetDescriptor.`type`)).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + assetDescriptor.fileName))
  }

  /** @return the asset or empty Nullable if not loaded */
  def get[T: ClassTag](fileName: String): Nullable[T] = synchronized {
    lookup[T](fileName, Nullable(summon[ClassTag[T]].runtimeClass))
  }

  /** @return the asset or empty Nullable if not loaded */
  def get[T](fileName: String, tpe: Class[T]): Nullable[T] = synchronized {
    lookup[T](fileName, Nullable(tpe))
  }

  /** @return the asset or empty Nullable if not loaded */
  def get[T](assetDescriptor: AssetDescriptor[T]): Nullable[T] = synchronized {
    lookup[T](assetDescriptor.fileName, Nullable(assetDescriptor.`type`))
  }

  /** @return all the assets matching the specified type */
  def getAll[T: ClassTag](out: DynamicArray[T]): DynamicArray[T] = synchronized {
    assets.get(summon[ClassTag[T]].runtimeClass).foreach { assetsByType =>
      assetsByType.foreachValue { assetRef =>
        out.add(assetRef.obj.asInstanceOf[T])
      }
    }
    out
  }

  /** Returns true if an asset with the specified name is loading, queued to be loaded, or has been loaded. */
  def contains(fileName: String): Boolean = synchronized {
    boundary {
      var i = 0
      while (i < tasks.size) {
        if (tasks(i).assetDesc.fileName == fileName) boundary.break(true)
        i += 1
      }
      i = 0
      while (i < loadQueue.size) {
        if (loadQueue(i).fileName == fileName) boundary.break(true)
        i += 1
      }
      isLoaded(fileName)
    }
  }

  /** Returns true if an asset with the specified name and type is loading, queued to be loaded, or has been loaded. */
  def contains(fileName: String, `type`: Class[?]): Boolean = synchronized {
    boundary {
      var i = 0
      while (i < tasks.size) {
        val assetDesc = tasks(i).assetDesc
        if (assetDesc.`type` == `type` && assetDesc.fileName == fileName) boundary.break(true)
        i += 1
      }
      i = 0
      while (i < loadQueue.size) {
        val assetDesc = loadQueue(i)
        if (assetDesc.`type` == `type` && assetDesc.fileName == fileName) boundary.break(true)
        i += 1
      }
      isLoaded(fileName, `type`)
    }
  }

  /** Removes the asset and all its dependencies, if they are not used by other assets. */
  def unload(fileName: String): Unit = synchronized {
    boundary {
      // check if it's currently processed (and the first element in the stack, thus not a dependency) and cancel if necessary
      if (tasks.size > 0) {
        val currentTask = tasks.first
        if (currentTask.assetDesc.fileName == fileName) {
          log.info("Unload (from tasks): " + fileName)
          currentTask.cancel = true
          currentTask.unload()
          boundary.break(())
        }
      }

      val tpe = assetTypes.get(fileName)

      // check if it's in the queue
      var foundIndex = -1
      var i          = 0
      while (i < loadQueue.size) {
        if (loadQueue(i).fileName == fileName) {
          foundIndex = i
          i = loadQueue.size // break
        }
        i += 1
      }
      if (foundIndex != -1) {
        toLoad -= 1
        val desc = loadQueue.removeIndex(foundIndex)
        log.info("Unload (from queue): " + fileName)

        // if the queued asset was already loaded, let the callback know it is available.
        tpe.foreach { _ =>
          desc.params.foreach { p =>
            p.loadedCallback.foreach { cb =>
              cb.finishedLoading(this, desc.fileName, desc.`type`)
            }
          }
        }
        boundary.break(())
      }

      val loadedType = tpe.getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName))

      val assetRef =
        assets.get(loadedType).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName)).get(fileName).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName))

      // if it is reference counted, decrement ref count and check if we can really get rid of it.
      assetRef.refCount -= 1
      if (assetRef.refCount <= 0) {
        log.info("Unload (dispose): " + fileName)

        // if it is disposable dispose it
        assetRef.obj match {
          case c: AutoCloseable => c.close()
          case _ =>
        }

        // remove the asset from the manager.
        assetTypes.remove(fileName)
        assets.get(loadedType).foreach(_.remove(fileName))
      } else {
        log.info("Unload (decrement): " + fileName)
      }

      // remove any dependencies (or just decrement their ref count).
      assetDependencies.get(fileName).foreach { dependencies =>
        dependencies.foreach { dependency =>
          if (isLoaded(dependency)) unload(dependency)
        }
      }
      // remove dependencies if ref count < 0
      if (assetRef.refCount <= 0) assetDependencies.remove(fileName)
    }
  }

  /** @return whether the asset is contained in this manager */
  def containsAsset[T](asset: T): Boolean = synchronized {
    boundary {
      val assetsByType = assets.get(asset.getClass)
      if (assetsByType.isEmpty) boundary.break(false)
      assetsByType.foreach { byType =>
        byType.foreachValue { assetRef =>
          if ((assetRef.obj.asInstanceOf[AnyRef] eq asset.asInstanceOf[AnyRef]) || asset == assetRef.obj)
            boundary.break(true)
        }
      }
      false
    }
  }

  /** @return the filename of the asset or Nullable.empty */
  def assetFileName[T](asset: T): Nullable[String] = synchronized {
    boundary {
      assets.foreachEntry { (_, assetsByType) =>
        assetsByType.foreachEntry { (fileName, assetRef) =>
          if ((assetRef.obj.asInstanceOf[AnyRef] eq asset.asInstanceOf[AnyRef]) || asset == assetRef.obj)
            boundary.break(Nullable(fileName))
        }
      }
      Nullable.empty[String]
    }
  }

  /** @return whether the asset is loaded */
  def isLoaded(assetDesc: AssetDescriptor[?]): Boolean = synchronized {
    isLoaded(assetDesc.fileName)
  }

  /** @return whether the asset is loaded */
  def isLoaded(fileName: String): Boolean = synchronized {
    assetTypes.containsKey(fileName)
  }

  /** @return whether the asset is loaded */
  def isLoaded(fileName: String, `type`: Class[?]): Boolean = synchronized {
    assets.get(`type`).exists { assetsByType =>
      assetsByType.get(fileName).isDefined
    }
  }

  /** Returns the default loader for the given type. */
  def loader[T](`type`: Class[T]): Nullable[AssetLoader[?, ?]] = synchronized {
    loader(`type`, Nullable.empty)
  }

  /** Returns the loader for the given type and the specified filename. If no loader exists for the specific filename, the default loader for that type is returned.
    */
  def loader[T](`type`: Class[T], fileName: Nullable[String]): Nullable[AssetLoader[?, ?]] = synchronized {
    boundary {
      val typeLoaders = this.loaders.get(`type`)
      if (typeLoaders.isEmpty) boundary.break(Nullable.empty)
      val loaderMap = typeLoaders.getOrElse(boundary.break(Nullable.empty))
      if (loaderMap.size < 1) boundary.break(Nullable.empty)
      if (fileName.isEmpty) boundary.break(loaderMap.get(""))
      var result: Nullable[AssetLoader[?, ?]] = Nullable.empty
      var length = -1
      loaderMap.foreachEntry { (suffix, loader) =>
        if (suffix.length > length && fileName.getOrElse("").endsWith(suffix)) {
          result = Nullable(loader)
          length = suffix.length
        }
      }
      result
    }
  }

  /** Adds the given asset to the loading queue of the AssetManager. */
  def load[T: ClassTag](fileName: String): Unit = synchronized {
    load(fileName, summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], Nullable.empty)
  }

  /** Adds the given asset to the loading queue of the AssetManager. */
  def load[T: ClassTag](fileName: String, parameter: AssetLoaderParameters[T]): Unit = synchronized {
    load(fileName, summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], Nullable(parameter))
  }

  /** Adds the given asset to the loading queue of the AssetManager. */
  def load[T](fileName: String, `type`: Class[T]): Unit = synchronized {
    load(fileName, `type`, Nullable.empty)
  }

  /** Adds the given asset to the loading queue of the AssetManager. */
  def load[T](fileName: String, `type`: Class[T], parameter: Nullable[AssetLoaderParameters[T]]): Unit = synchronized {
    val ldr = this.loader(`type`, Nullable(fileName))
    if (ldr.isEmpty) throw SgeError.InvalidInput("No loader for type: " + `type`.getSimpleName)

    // reset stats
    if (loadQueue.size == 0) {
      loaded = 0
      toLoad = 0
      peakTasks = 0
    }

    // check if an asset with the same name but a different type has already been added.

    // check preload queue
    var i = 0
    while (i < loadQueue.size) {
      val desc = loadQueue(i)
      if (desc.fileName == fileName && desc.`type` != `type`)
        throw SgeError.InvalidInput(
          s"Asset with name '$fileName' already in preload queue, but has different type (expected: ${`type`.getSimpleName}, found: ${desc.`type`.getSimpleName})"
        )
      i += 1
    }

    // check task list
    i = 0
    while (i < tasks.size) {
      val desc = tasks(i).assetDesc
      if (desc.fileName == fileName && desc.`type` != `type`)
        throw SgeError.InvalidInput(
          s"Asset with name '$fileName' already in task list, but has different type (expected: ${`type`.getSimpleName}, found: ${desc.`type`.getSimpleName})"
        )
      i += 1
    }

    // check loaded assets
    assetTypes.get(fileName).foreach { otherType =>
      if (otherType != `type`)
        throw SgeError.InvalidInput(
          s"Asset with name '$fileName' already loaded, but has different type (expected: ${`type`.getSimpleName}, found: ${otherType.getSimpleName})"
        )
    }

    toLoad += 1
    val assetDesc = AssetDescriptor(fileName, `type`, parameter)
    loadQueue.add(assetDesc)
    log.debug("Queued: " + assetDesc)
  }

  /** Adds the given asset to the loading queue of the AssetManager. */
  def load(desc: AssetDescriptor[?]): Unit = synchronized {
    // Cast is safe: the descriptor's type and params were created together
    load(
      desc.fileName,
      desc.`type`.asInstanceOf[Class[Any]],
      desc.params.asInstanceOf[Nullable[AssetLoaderParameters[Any]]]
    )
  }

  /** Updates the AssetManager for a single task. Returns if the current task is still being processed or there are no tasks, otherwise it finishes the current task and starts the next task.
    * @return
    *   true if all loading is finished.
    */
  def update(): Boolean = synchronized {
    try
      boundary {
        if (tasks.size == 0) {
          // loop until we have a new task ready to be processed
          while (loadQueue.size != 0 && tasks.size == 0)
            nextTask()
          // have we not found a task? We are done!
          if (tasks.size == 0) boundary.break(true)
        }
        updateTask() && loadQueue.size == 0 && tasks.size == 0
      }
    catch {
      case NonFatal(t) =>
        handleTaskError(t)
        loadQueue.size == 0
    }
  }

  /** Updates the AssetManager continuously for the specified number of milliseconds, yielding the CPU to the loading thread between updates. This may block for less time if all loading tasks are
    * complete. This may block for more time if the portion of a single task that happens in the GL thread takes a long time. On WebGL, updates for a single task instead (see update()).
    */
  def update(millis: Int): Boolean = boundary {
    if (Sge().application.getType() == Application.ApplicationType.WebGL) boundary.break(update())
    val endTime = TimeUtils.millis() + sge.utils.Millis(millis.toLong)
    while (true) {
      val done = update()
      if (done || TimeUtils.millis() > endTime) boundary.break(done)
      Thread.`yield`()
    }
    true // unreachable, satisfies compiler
  }

  /** Returns true when all assets are loaded. Can be called from any thread but note update() or related methods must be called to process tasks.
    */
  def isFinished: Boolean = synchronized {
    loadQueue.size == 0 && tasks.size == 0
  }

  /** Blocks until all assets are loaded. */
  def finishLoading(): Unit = {
    log.debug("Waiting for loading to complete...")
    while (!update())
      Thread.`yield`()
    log.debug("Loading complete.")
  }

  /** Blocks until the specified asset is loaded. */
  def finishLoadingAsset[T](assetDesc: AssetDescriptor[T]): T =
    finishLoadingAsset(assetDesc.fileName)

  /** Blocks until the specified asset is loaded. */
  def finishLoadingAsset[T](fileName: String): T = {
    log.debug("Waiting for asset to be loaded: " + fileName)
    boundary {
      while (true) {
        synchronized {
          val result = lookup[T](fileName, Nullable.empty)
          if (result.isDefined) {
            log.debug("Asset loaded: " + fileName)
            boundary.break(result.getOrElse(throw SgeError.InvalidInput("unreachable")))
          }
          // If update() returns true, all queues are empty. The asset either loaded (caught above)
          // or failed with an error listener swallowing the exception. Break to avoid infinite loop.
          if (update()) {
            boundary.break(
              lookup[T](fileName, Nullable.empty).getOrElse(
                throw SgeError.InvalidInput("Asset not loaded: " + fileName)
              )
            )
          }
        }
        Thread.`yield`()
      }
      throw SgeError.InvalidInput("unreachable") // satisfies compiler
    }
  }

  /** Injects dependencies for a parent asset. Called from AssetLoadingTask. */
  def addDependencies(parentAssetFilename: String, dependendAssetDescs: DynamicArray[AssetDescriptor[?]]): Unit =
    synchronized {
      val inj = this.injected
      dependendAssetDescs.foreach { desc =>
        if (!inj.contains(desc.fileName)) { // Ignore subsequent dependencies if there are duplicates.
          inj.add(desc.fileName)
          injectDependency(parentAssetFilename, desc)
        }
      }
      inj.clear(32)
    }

  private def injectDependency(parentAssetFilename: String, dependendAssetDesc: AssetDescriptor[?]): Unit =
    synchronized {
      // add the asset as a dependency of the parent asset
      var dependencies = assetDependencies.get(parentAssetFilename)
      if (dependencies.isEmpty) {
        val newDeps = DynamicArray[String]()
        assetDependencies.put(parentAssetFilename, newDeps)
        dependencies = Nullable(newDeps)
      }
      dependencies.foreach(_.add(dependendAssetDesc.fileName))

      // if the asset is already loaded, increase its reference count.
      if (isLoaded(dependendAssetDesc.fileName)) {
        log.debug("Dependency already loaded: " + dependendAssetDesc)
        assetTypes.get(dependendAssetDesc.fileName).foreach { tpe =>
          assets.get(tpe).foreach { byType =>
            byType.get(dependendAssetDesc.fileName).foreach { assetRef =>
              assetRef.refCount += 1
            }
          }
        }
        incrementRefCountedDependencies(dependendAssetDesc.fileName)
      } else {
        // else add a new task for the asset.
        log.info("Loading dependency: " + dependendAssetDesc)
        addTask(dependendAssetDesc)
      }
    }

  /** Removes a task from the loadQueue and adds it to the task stack. If the asset is already loaded (which can happen if it was a dependency of a previously loaded asset) its reference count will be
    * increased.
    */
  private def nextTask(): Unit = {
    val assetDesc = loadQueue.removeIndex(0)

    // if the asset not meant to be reloaded and is already loaded, increase its reference count
    if (isLoaded(assetDesc.fileName)) {
      log.debug("Already loaded: " + assetDesc)
      assetTypes.get(assetDesc.fileName).foreach { tpe =>
        assets.get(tpe).foreach { byType =>
          byType.get(assetDesc.fileName).foreach { assetRef =>
            assetRef.refCount += 1
          }
        }
      }
      incrementRefCountedDependencies(assetDesc.fileName)
      assetDesc.params.foreach { p =>
        p.loadedCallback.foreach { cb =>
          cb.finishedLoading(this, assetDesc.fileName, assetDesc.`type`)
        }
      }
      loaded += 1
    } else {
      // else add a new task for the asset.
      log.info("Loading: " + assetDesc)
      addTask(assetDesc)
    }
  }

  /** Adds a AssetLoadingTask to the task stack for the given asset. */
  private def addTask(assetDesc: AssetDescriptor[?]): Unit = {
    val ldr = this.loader(assetDesc.`type`, Nullable(assetDesc.fileName))
    if (ldr.isEmpty) throw SgeError.InvalidInput("No loader for type: " + assetDesc.`type`.getSimpleName)
    tasks.add(AssetLoadingTask(this, assetDesc, ldr.getOrElse(throw SgeError.InvalidInput("unreachable")), executor))
    peakTasks += 1
  }

  /** Adds an asset to this AssetManager */
  protected def addAsset[T](fileName: String, `type`: Class[T], asset: T): Unit = {
    // add the asset to the filename lookup
    assetTypes.put(fileName, `type`)

    // add the asset to the type lookup
    var typeToAssets = assets.get(`type`)
    if (typeToAssets.isEmpty) {
      val newMap = ObjectMap[String, AssetManager.RefCountedContainer]()
      assets.put(`type`, newMap)
      typeToAssets = Nullable(newMap)
    }
    val assetRef = AssetManager.RefCountedContainer()
    assetRef.obj = asset.asInstanceOf[Any]
    typeToAssets.foreach(_.put(fileName, assetRef))
  }

  /** Updates the current task on the top of the task stack.
    * @return
    *   true if the asset is loaded or the task was cancelled.
    */
  private def updateTask(): Boolean = boundary {
    val task = tasks.peek

    var complete = true
    try
      complete = task.cancel || task.update()
    catch {
      case ex: RuntimeException =>
        task.cancel = true
        taskFailed(task.assetDesc, ex)
    }

    // if the task has been cancelled or has finished loading
    if (complete) {
      // increase the number of loaded assets and pop the task from the stack
      if (tasks.size == 1) {
        loaded += 1
        peakTasks = 0
      }
      tasks.pop()

      if (task.cancel) boundary.break(true)

      addAsset(
        task.assetDesc.fileName,
        task.assetDesc.`type`.asInstanceOf[Class[Any]],
        task.asset.getOrElse(throw SgeError.InvalidInput("unreachable"))
      )

      // otherwise, if a listener was found in the parameter invoke it
      task.assetDesc.params.foreach { p =>
        p.loadedCallback.foreach { cb =>
          cb.finishedLoading(this, task.assetDesc.fileName, task.assetDesc.`type`)
        }
      }

      val endTime = TimeUtils.nanoTime()
      log.debug("Loaded: " + (endTime - task.startTime).toFloat / 1000000f + "ms " + task.assetDesc)

      true
    } else {
      false
    }
  }

  /** Called when a task throws an exception during loading. The default implementation rethrows the exception. A subclass may supress the default implementation when loading assets where loading
    * failure is recoverable.
    */
  protected def taskFailed(assetDesc: AssetDescriptor[?], ex: RuntimeException): Unit =
    throw ex

  private def incrementRefCountedDependencies(parent: String): Unit =
    assetDependencies.get(parent).foreach { dependencies =>
      dependencies.foreach { dependency =>
        assetTypes.get(dependency).foreach { tpe =>
          assets.get(tpe).foreach { byType =>
            byType.get(dependency).foreach { assetRef =>
              assetRef.refCount += 1
              incrementRefCountedDependencies(dependency)
            }
          }
        }
      }
    }

  /** Handles a runtime/loading error in update() by optionally invoking the AssetErrorListener. */
  private def handleTaskError(t: Throwable): Unit = {
    log.error("Error loading asset.", t)

    if (tasks.isEmpty) throw SgeError.InvalidInput("Error loading asset", Some(t))

    // pop the faulty task from the stack
    val task      = tasks.pop()
    val assetDesc = task.assetDesc

    // remove all dependencies
    if (task.dependenciesLoaded && task.dependencies.isDefined) {
      task.dependencies.foreach { deps =>
        deps.foreach { desc =>
          unload(desc.fileName)
        }
      }
    }

    // clear the rest of the stack
    tasks.clear()

    // inform the listener that something bad happened
    listener.fold {
      throw SgeError.InvalidInput("Error loading asset: " + assetDesc, Some(t))
    } { l =>
      l.error(assetDesc, t)
    }
  }

  /** Sets a new AssetLoader for the given type. */
  def setLoader[T: ClassTag](loader: AssetLoader[?, ?]): Unit = synchronized {
    setLoader(summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], Nullable.empty, loader)
  }

  /** Sets a new AssetLoader for the given type. */
  def setLoader[T: ClassTag](suffix: String, loader: AssetLoader[?, ?]): Unit = synchronized {
    setLoader(summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], Nullable(suffix), loader)
  }

  /** Sets a new AssetLoader for the given type. */
  def setLoader[T](`type`: Class[T], loader: AssetLoader[?, ?]): Unit = synchronized {
    setLoader(`type`, Nullable.empty, loader)
  }

  /** Sets a new AssetLoader for the given type. */
  def setLoader[T](`type`: Class[T], suffix: Nullable[String], loader: AssetLoader[?, ?]): Unit = synchronized {
    log.debug("Loader set: " + `type`.getSimpleName + " -> " + loader.getClass.getSimpleName)
    var typeLoaders = this.loaders.get(`type`)
    if (typeLoaders.isEmpty) {
      val newMap = ObjectMap[String, AssetLoader[?, ?]]()
      this.loaders.put(`type`, newMap)
      typeLoaders = Nullable(newMap)
    }
    typeLoaders.foreach(_.put(suffix.getOrElse(""), loader))
  }

  /** @return the number of loaded assets */
  def loadedAssets: Int = synchronized(assetTypes.size)

  /** @return the number of currently queued assets */
  def queuedAssets: Int = synchronized(loadQueue.size + tasks.size)

  /** @return the progress in percent of completion. */
  def progress: Float = synchronized {
    boundary {
      if (toLoad == 0) boundary.break(1f)
      var fractionalLoaded: Float = loaded.toFloat
      if (peakTasks > 0) {
        fractionalLoaded += (peakTasks - tasks.size).toFloat / peakTasks.toFloat
      }
      Math.min(1f, fractionalLoaded / toLoad.toFloat)
    }
  }

  /** Returns the AssetErrorListener to be invoked in case loading an asset failed. */
  def errorListener: Nullable[AssetErrorListener] = synchronized {
    this.listener
  }

  /** Sets an AssetErrorListener to be invoked in case loading an asset failed. */
  def errorListener_=(listener: Nullable[AssetErrorListener]): Unit = synchronized {
    this.listener = listener
  }

  /** Disposes all assets in the manager and stops all asynchronous loading. */
  override def close(): Unit = {
    log.debug("Disposing.")
    clear()
    executorService.shutdown()
  }

  /** Clears and disposes all assets and the preloading queue. */
  def clear(): Unit = {
    synchronized {
      loadQueue.clear()
    }

    // Lock is temporarily released to yield to blocked executor threads
    // A pending async task can cause a deadlock if we do not release

    finishLoading()

    synchronized {
      val dependencyCount = scala.collection.mutable.HashMap[String, Int]()
      while (assetTypes.size > 0) {
        // for each asset, figure out how often it was referenced
        dependencyCount.clear()
        val assetNames = DynamicArray[String]()
        assetTypes.foreachKey(assetNames.add)
        assetNames.foreach { asset =>
          assetDependencies.get(asset).foreach { dependencies =>
            dependencies.foreach { dependency =>
              dependencyCount(dependency) = dependencyCount.getOrElse(dependency, 0) + 1
            }
          }
        }

        // only dispose of assets that are root assets (not referenced)
        assetNames.foreach { asset =>
          if (dependencyCount.getOrElse(asset, 0) == 0) unload(asset)
        }
      }

      this.assets.clear(51)
      this.assetTypes.clear(51)
      this.assetDependencies.clear(51)
      this.loaded = 0
      this.toLoad = 0
      this.peakTasks = 0
      this.loadQueue.clear()
      this.tasks.clear()
    }
  }

  /** Returns the reference count of an asset. */
  def referenceCount(fileName: String): Int = synchronized {
    val tpe = assetTypes.get(fileName).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName))
    assets.get(tpe).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName)).get(fileName).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName)).refCount
  }

  /** Sets the reference count of an asset. */
  def setReferenceCount(fileName: String, refCount: Int): Unit = synchronized {
    val tpe = assetTypes.get(fileName).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName))
    assets.get(tpe).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName)).get(fileName).getOrElse(throw SgeError.InvalidInput("Asset not loaded: " + fileName)).refCount = refCount
  }

  /** @return a string containing ref count and dependency information for all assets. */
  def diagnostics: String = synchronized {
    val buffer = new StringBuilder(256)
    assetTypes.foreachEntry { (fileName, tpe) =>
      if (buffer.nonEmpty) buffer.append('\n')
      buffer.append(fileName)
      buffer.append(", ")
      buffer.append(tpe.getSimpleName)
      buffer.append(", refs: ")
      assets.get(tpe).foreach { byType =>
        byType.get(fileName).foreach { assetRef =>
          buffer.append(assetRef.refCount)
        }
      }

      assetDependencies.get(fileName).foreach { dependencies =>
        buffer.append(", deps: [")
        dependencies.foreach { dep =>
          buffer.append(dep)
          buffer.append(',')
        }
        buffer.append(']')
      }
    }
    buffer.toString()
  }

  /** @return the file names of all loaded assets. */
  def assetNames: DynamicArray[String] = synchronized {
    val result = DynamicArray[String]()
    assetTypes.foreachKey(result.add)
    result
  }

  /** @return the dependencies of an asset or Nullable.empty if the asset has no dependencies. */
  def dependencies(fileName: String): Nullable[DynamicArray[String]] = synchronized {
    assetDependencies.get(fileName)
  }

  /** @return the type of a loaded asset. */
  def assetType(fileName: String): Nullable[Class[?]] = synchronized {
    assetTypes.get(fileName)
  }
}

object AssetManager {
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  final private[assets] class RefCountedContainer(var obj: Any = null, var refCount: Int = 1) // null: internal mutable container
}
