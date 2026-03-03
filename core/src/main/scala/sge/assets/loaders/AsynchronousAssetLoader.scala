/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/AsynchronousAssetLoader.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle

/** Base class for asynchronous {@link AssetLoader} instances. Such loaders try to load parts of an OpenGL resource, like the Pixmap, on a separate thread to then load the actual resource on the
  * thread the OpenGL context is active on.
  * @author
  *   mzechner
  *
  * @param <T>
  * @param <P>
  */
abstract class AsynchronousAssetLoader[T, P <: AssetLoaderParameters[T]](resolver: FileHandleResolver) extends AssetLoader[T, P](resolver) {

  /** Loads the non-OpenGL part of the asset and injects any dependencies of the asset into the AssetManager.
    * @param manager
    * @param fileName
    *   the name of the asset to load
    * @param file
    *   the resolved file to load
    * @param parameter
    *   the parameters to use for loading the asset
    */
  def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: P): Unit

  /** Called if this task is unloaded before {@link #loadSync(AssetManager, String, FileHandle, AssetLoaderParameters) loadSync} is called. This method may be invoked on any thread, but will not be
    * invoked during or after {@link #loadSync(AssetManager, String, FileHandle, AssetLoaderParameters) loadSync} . This method is not invoked when a task is cancelled because it threw an exception,
    * only when the asset is unloaded before loading is complete. <p> The default implementation does nothing. Subclasses should release any resources acquired in
    * {@link #loadAsync(AssetManager, String, FileHandle, AssetLoaderParameters) loadAsync} , which may or may not have been called before this method, but never during or after this method. Note that
    * {@link #loadAsync(AssetManager, String, FileHandle, AssetLoaderParameters) loadAsync} may still be executing when this method is called and must release any resources it allocated.
    */
  def unloadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: P): Unit = {}

  /** Loads the OpenGL part of the asset.
    * @param manager
    * @param fileName
    * @param file
    *   the resolved file to load
    * @param parameter
    */
  def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: P): T
}
