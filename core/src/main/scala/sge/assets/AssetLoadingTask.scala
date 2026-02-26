/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/AssetLoadingTask.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, boundary }
import sge.assets.loaders.AssetLoader
import sge.files.FileHandle
import sge.utils.{ Logger, Nullable, SgeError, TimeUtils }

/** Responsible for loading an asset through an AssetLoader based on an AssetDescriptor. Please don't forget to update the overriding emu file on GWT backend when changing this file!
  *
  * @author
  *   mzechner (original implementation)
  */
class AssetLoadingTask(
  var manager:   AssetManager,
  val assetDesc: AssetDescriptor[?],
  val loader:    AssetLoader[?, ?],
  val executor:  ExecutionContext,
  val startTime: Long
) extends (() => Unit) {

  @volatile var asyncDone:          Boolean                                   = false
  @volatile var dependenciesLoaded: Boolean                                   = false
  @volatile var dependencies:       Nullable[ArrayBuffer[AssetDescriptor[?]]] = Nullable.empty
  @volatile var depsFuture:         Nullable[Future[Unit]]                    = Nullable.empty
  @volatile var loadFuture:         Nullable[Future[Unit]]                    = Nullable.empty
  @volatile var asset:              Nullable[Any]                             = Nullable.empty

  @volatile var cancel: Boolean = false

  def this(manager: AssetManager, assetDesc: AssetDescriptor[?], loader: AssetLoader[?, ?], threadPool: ExecutionContext) =
    this(manager, assetDesc, loader, threadPool, if (manager.getLogLevel == Logger.DEBUG) TimeUtils.nanoTime() else 0)

  /** Loads parts of the asset asynchronously if the loader is an AsynchronousAssetLoader. */
  def apply(): Unit = boundary {
    if (cancel) boundary.break()
    val asyncLoader = loader.asInstanceOf[AsynchronousAssetLoader[Any, AssetLoaderParameters[Any]]]
    if (!dependenciesLoaded) {
      dependencies = Nullable(
        asyncLoader.getDependencies(assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params.orNull.asInstanceOf[AssetLoaderParameters[Any]])
      )
      dependencies.foreach { deps =>
        removeDuplicates(deps)
        manager.addDependencies(assetDesc.fileName, deps)
      }
      if (dependencies.isEmpty) {
        // if we have no dependencies, we load the async part of the task immediately.
        asyncLoader.loadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params.orNull.asInstanceOf[AssetLoaderParameters[Any]])
        asyncDone = true
      }
    } else {
      asyncLoader.loadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params.orNull.asInstanceOf[AssetLoaderParameters[Any]])
      asyncDone = true
    }
  }

  /** Updates the loading of the asset. In case the asset is loaded with an AsynchronousAssetLoader, the loaders loadAsync method is first called on a worker thread. Once this method returns, the rest
    * of the asset is loaded on the rendering thread via loadSync.
    * @return
    *   true in case the asset was fully loaded, false otherwise
    * @throws SgeError
    */
  def update(): Boolean = {
    loader match {
      case _: SynchronousAssetLoader[?, ?] => handleSyncLoader()
      case _ => handleAsyncLoader()
    }
    asset.isDefined
  }

  private def handleSyncLoader(): Unit = boundary {
    val syncLoader = loader.asInstanceOf[SynchronousAssetLoader[Any, AssetLoaderParameters[Any]]]
    if (!dependenciesLoaded) {
      dependenciesLoaded = true
      dependencies = Nullable(
        syncLoader.getDependencies(assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params.orNull.asInstanceOf[AssetLoaderParameters[Any]])
      )
      dependencies match {
        case dep if dep.isEmpty =>
          asset = Nullable(
            syncLoader.load(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params.orNull.asInstanceOf[AssetLoaderParameters[Any]])
          )
          boundary.break()
        case deps =>
          deps.foreach(removeDuplicates)
          manager.addDependencies(assetDesc.fileName, deps.getOrElse(throw SgeError.SerializationError("dependencies not loaded")))
      }
    } else {
      asset = Nullable(
        syncLoader.load(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params.orNull.asInstanceOf[AssetLoaderParameters[Any]])
      )
    }
  }

  private def handleAsyncLoader(): Unit = {
    val asyncLoader = loader.asInstanceOf[AsynchronousAssetLoader[Any, AssetLoaderParameters[Any]]]
    if (!dependenciesLoaded) {
      if (depsFuture.isEmpty) {
        implicit val ec: ExecutionContext = executor
        depsFuture = Nullable(Future(apply()))
      } else {
        depsFuture.foreach { future =>
          if (future.isCompleted) {
            future.value match {
              case Some(Success(_)) =>
                dependenciesLoaded = true
                if (asyncDone)
                  asset = Nullable(
                    asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params.orNull.asInstanceOf[AssetLoaderParameters[Any]])
                  )
              case Some(Failure(e)) =>
                throw SgeError.SerializationError(s"Couldn't load dependencies of asset: ${assetDesc.fileName}", Some(e))
              case None => // still running
            }
          }
        }
      }
    } else if (loadFuture.isEmpty && !asyncDone) {
      implicit val ec: ExecutionContext = executor
      loadFuture = Nullable(Future(apply()))
    } else if (asyncDone) {
      asset = Nullable(
        asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params.orNull.asInstanceOf[AssetLoaderParameters[Any]])
      )
    } else {
      loadFuture.foreach { future =>
        if (future.isCompleted) {
          future.value match {
            case Some(Success(_)) =>
              asset = Nullable(
                asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params.orNull.asInstanceOf[AssetLoaderParameters[Any]])
              )
            case Some(Failure(e)) =>
              throw SgeError.SerializationError(s"Couldn't load asset: ${assetDesc.fileName}", Some(e))
            case None => // still running
          }
        }
      }
    }
  }

  /** Called when this task is the task that is currently being processed and it is unloaded. */
  def unload(): Unit =
    // Use type checking that works at runtime
    try {
      val asyncLoader = loader.asInstanceOf[AsynchronousAssetLoader[Any, AssetLoaderParameters[Any]]]
      asyncLoader.unloadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params.orNull.asInstanceOf[AssetLoaderParameters[Any]])
    } catch {
      case _: ClassCastException => // Not an async loader, nothing to do
    }

  private def resolve(loader: AssetLoader[?, ?], assetDesc: AssetDescriptor[?]): FileHandle = {
    if (assetDesc.file.isEmpty) {
      assetDesc.file = Nullable(loader.resolve(assetDesc.fileName))
    }
    assetDesc.file.getOrElse(throw SgeError.SerializationError(s"Could not resolve file for asset: ${assetDesc.fileName}"))
  }

  private def removeDuplicates(array: ArrayBuffer[AssetDescriptor[?]]): Unit = {
    // Use a Set to track seen combinations of filename and type for O(n) deduplication
    val seen = scala.collection.mutable.Set[(String, Class[?])]()
    var i    = 0
    while (i < array.size) {
      val desc = array(i)
      val key  = (desc.fileName, desc.`type`)
      if (seen.contains(key)) {
        array.remove(i)
      } else {
        seen.add(key)
        i += 1
      }
    }
  }
}

// These would be defined elsewhere, but adding stubs for compilation
trait AsynchronousAssetLoader[T, P <: AssetLoaderParameters[T]] extends AssetLoader[T, P] {
  def loadAsync(manager:   AssetManager, fileName: String, file: FileHandle, parameter: P): Unit
  def loadSync(manager:    AssetManager, fileName: String, file: FileHandle, parameter: P): T
  def unloadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: P): Unit
}

trait SynchronousAssetLoader[T, P <: AssetLoaderParameters[T]] extends AssetLoader[T, P] {
  def load(manager: AssetManager, fileName: String, file: FileHandle, parameter: P): T
}
