/*
 * New file for SGE — no LibGDX equivalent (replaces GWT's Preloader concept).
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GWT Preloader -> BrowserAssetLoader (complete rewrite using fetch API)
 *   Convention: Scala.js only; uses scalajs-dom fetch API instead of GWT XMLHttpRequest/JSNI
 *   Convention: Assets preloaded before game start, served synchronously from in-memory cache
 *   Idiom: scala.scalajs.js.Promise for async preloading
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package files

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.typedarray.Int8Array
import org.scalajs.dom

/** Browser asset loader that fetches assets via the fetch API and caches them in-memory.
  *
  * Assets are described by a manifest file (`assets.txt`) with the format:
  * {{{
  * <type>:<path>:<size>:<mime>
  * }}}
  * where type is one of: `t` (text), `b` (binary), `i` (image), `a` (audio), `d` (directory).
  *
  * All assets are preloaded before the game loop starts. After preloading, file reads are served synchronously from the in-memory cache.
  */
class BrowserAssetLoader(val baseUrl: String) {

  import BrowserAssetLoader.*

  private val textCache:    js.Dictionary[String]                = js.Dictionary()
  private val binaryCache:  js.Dictionary[Array[Byte]]           = js.Dictionary()
  private val directorySet: scala.collection.mutable.Set[String] = scala.collection.mutable.HashSet()

  /** Preload all assets listed in the manifest file.
    *
    * @param manifestUrl
    *   URL of the assets.txt manifest (relative to baseUrl)
    * @param callback
    *   progress callback
    * @return
    *   a Promise that resolves when all assets are loaded
    */
  def preload(manifestUrl: String, callback: PreloadCallback): js.Promise[Unit] = {
    val url = if (baseUrl.endsWith("/")) baseUrl + manifestUrl else baseUrl + "/" + manifestUrl
    dom
      .fetch(url)
      .toFuture
      .flatMap { response =>
        if (!response.ok)
          throw utils.SgeError.FileReadError(null, s"Failed to load asset manifest: ${response.status} ${response.statusText}")
        response.text().toFuture
      }
      .flatMap { manifestText =>
        val entries = parseManifest(manifestText)
        val total   = entries.length
        if (total == 0) {
          callback.update(0, 0, 0)
          scala.concurrent.Future.successful(())
        } else {
          var loaded  = 0
          val futures = entries.map { entry =>
            val future = entry.assetType match {
              case AssetType.Text      => loadText(entry.path)
              case AssetType.Binary    => loadBinary(entry.path)
              case AssetType.Image     => loadBinary(entry.path) // images stored as binary
              case AssetType.Audio     => loadBinary(entry.path) // audio stored as binary
              case AssetType.Directory =>
                directorySet.add(entry.path)
                scala.concurrent.Future.successful(())
            }
            future
              .map { _ =>
                loaded += 1
                callback.update(loaded, total, entry.size)
              }
              .recover { case ex: Throwable =>
                callback.error(entry.path, ex)
              }
          }
          scala.concurrent.Future.sequence(futures).map(_ => ())
        }
      }
      .map { _ =>
        callback.finished()
      }
      .toJSPromise
  }

  /** Synchronously check if an asset exists in cache. */
  def contains(path: String): Boolean = {
    val p = fixSlashes(path)
    textCache.contains(p) || binaryCache.contains(p) || directorySet.contains(p)
  }

  /** Check if the asset is a directory. */
  def isDirectory(path: String): Boolean =
    directorySet.contains(fixSlashes(path))

  /** Read a cached text asset. */
  def readText(path: String): String = {
    val p = fixSlashes(path)
    textCache.getOrElse(p, throw utils.SgeError.FileReadError(null, s"Text asset not preloaded: $p"))
  }

  /** Read a cached binary asset. */
  def readBytes(path: String): Array[Byte] = {
    val p = fixSlashes(path)
    binaryCache.getOrElse(p, throw utils.SgeError.FileReadError(null, s"Binary asset not preloaded: $p"))
  }

  /** Check if an asset is text. */
  def isText(path: String): Boolean = textCache.contains(fixSlashes(path))

  /** Check if an asset is binary (includes images and audio). */
  def isBinary(path: String): Boolean = binaryCache.contains(fixSlashes(path))

  /** Get the full URL for an asset. */
  def assetUrl(path: String): String =
    if (baseUrl.endsWith("/")) baseUrl + path else baseUrl + "/" + path

  /** Get the size of a cached asset in bytes. */
  def length(path: String): Long = {
    val p = fixSlashes(path)
    if (textCache.contains(p)) textCache(p).length.toLong
    else if (binaryCache.contains(p)) binaryCache(p).length.toLong
    else 0L
  }

  /** List entries in a directory. */
  def list(directory: String): Array[String] = {
    val dir    = fixSlashes(directory)
    val prefix = if (dir.isEmpty) "" else if (dir.endsWith("/")) dir else dir + "/"
    val result = scala.collection.mutable.ArrayBuffer[String]()

    // Collect from all caches
    def addIfChild(key: String): Unit =
      if (key.startsWith(prefix)) {
        val rest = key.substring(prefix.length)
        // Only direct children (no further slashes)
        if (rest.nonEmpty && !rest.contains("/")) result += key
      }

    textCache.keys.foreach(addIfChild)
    binaryCache.keys.foreach(addIfChild)
    directorySet.foreach(addIfChild)
    result.distinct.toArray
  }

  private def loadText(path: String): scala.concurrent.Future[Unit] =
    dom
      .fetch(assetUrl(path))
      .toFuture
      .flatMap { response =>
        if (!response.ok)
          throw utils.SgeError.FileReadError(null, s"Failed to fetch text asset: $path (${response.status})")
        response.text().toFuture
      }
      .map { text =>
        textCache(fixSlashes(path)) = text
      }

  private def loadBinary(path: String): scala.concurrent.Future[Unit] =
    dom
      .fetch(assetUrl(path))
      .toFuture
      .flatMap { response =>
        if (!response.ok)
          throw utils.SgeError.FileReadError(null, s"Failed to fetch binary asset: $path (${response.status})")
        response.arrayBuffer().toFuture
      }
      .map { arrayBuf =>
        val int8  = new Int8Array(arrayBuf)
        val bytes = new Array[Byte](int8.length)
        var i     = 0
        while (i < bytes.length) {
          bytes(i) = int8(i)
          i += 1
        }
        binaryCache(fixSlashes(path)) = bytes
      }

  private def fixSlashes(path: String): String =
    path.replace('\\', '/')
}

object BrowserAssetLoader {

  /** Asset type codes from the manifest file. */
  private enum AssetType {
    case Text, Binary, Image, Audio, Directory
  }

  final private case class AssetEntry(assetType: AssetType, path: String, size: Long)

  /** Parse the assets.txt manifest format: `type:path:size:mime` */
  private def parseManifest(text: String): Array[AssetEntry] = {
    val lines = text.split('\n').filter(_.nonEmpty)
    lines.flatMap { line =>
      val parts = line.split(':')
      if (parts.length < 3) None
      else {
        val assetType = parts(0) match {
          case "t" => AssetType.Text
          case "b" => AssetType.Binary
          case "i" => AssetType.Image
          case "a" => AssetType.Audio
          case "d" => AssetType.Directory
          case _   => AssetType.Binary // fallback
        }
        val path = parts(1)
        val size =
          try parts(2).toLong
          catch { case _: NumberFormatException => 0L }
        Some(AssetEntry(assetType, path, size))
      }
    }
  }

  /** Callback interface for preload progress. */
  trait PreloadCallback {

    /** Called as each asset finishes loading.
      * @param loaded
      *   number of assets loaded so far
      * @param total
      *   total number of assets
      * @param lastAssetSize
      *   size of the last loaded asset in bytes
      */
    def update(loaded: Int, total: Int, lastAssetSize: Long): Unit

    /** Called when an asset fails to load.
      * @param path
      *   the asset path that failed
      * @param cause
      *   the exception
      */
    def error(path: String, cause: Throwable): Unit

    /** Called when all assets have been loaded. */
    def finished(): Unit
  }
}
