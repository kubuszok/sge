/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/PolygonRegionLoader.java
 * Original authors: dermetfan
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import java.io.BufferedReader
import java.io.IOException

import sge.assets.AssetDescriptor
import sge.assets.AssetLoaderParameters
import sge.assets.AssetManager
import sge.assets.loaders.FileHandleResolver
import sge.assets.loaders.SynchronousAssetLoader
import sge.assets.loaders.resolvers.InternalFileHandleResolver
import sge.files.FileHandle
import sge.graphics.Texture
import sge.math.EarClippingTriangulator
import sge.utils.{ DynamicArray, Nullable, SgeError, StreamUtils }

import scala.language.implicitConversions

/** loads {@link PolygonRegion PolygonRegions} using a {@link com.badlogic.gdx.graphics.g2d.PolygonRegionLoader}
  * @author
  *   dermetfan
  */
class PolygonRegionLoader(resolver: FileHandleResolver) extends SynchronousAssetLoader[PolygonRegion, PolygonRegionLoader.PolygonRegionParameters](resolver) {

  private val defaultParameters = new PolygonRegionLoader.PolygonRegionParameters()

  private val triangulator = new EarClippingTriangulator()

  def this()(using sge: Sge) = {
    this(new InternalFileHandleResolver())
  }

  override def load(manager: AssetManager, fileName: String, file: FileHandle, parameter: PolygonRegionLoader.PolygonRegionParameters): PolygonRegion =
    // TODO: Once AssetManager has get() and getDependencies() methods, uncomment the following:
    // val texture = manager.get(manager.getDependencies(fileName).head)
    // load(new TextureRegion(texture), file)

    // For now, throw an error indicating this functionality is not yet implemented
    throw SgeError.InvalidInput("PolygonRegionLoader.load not yet implemented - AssetManager missing required methods")

  /** If the PSH file contains a line starting with {@link PolygonRegionParameters#texturePrefix params.texturePrefix} , an {@link AssetDescriptor} for the file referenced on that line will be added
    * to the returned Array. Otherwise a sibling of the given file with the same name and the first found extension in {@link PolygonRegionParameters#textureExtensions params.textureExtensions} will
    * be used. If no suitable file is found, the returned Array will be empty.
    */
  override def getDependencies(fileName: String, file: FileHandle, params: PolygonRegionLoader.PolygonRegionParameters): DynamicArray[AssetDescriptor[?]] = {
    val actualParams = Nullable(params).getOrElse(defaultParameters)
    var image: Nullable[String] = Nullable.empty
    try {
      val reader = file.reader(actualParams.readerBuffer)
      var line   = reader.readLine()
      while (Nullable(line).isDefined) {
        if (line.startsWith(actualParams.texturePrefix)) {
          image = Nullable(line.substring(actualParams.texturePrefix.length()))
          // Break from loop - in Scala we can use return or a different pattern
        }
        line = reader.readLine()
      }
      reader.close()
    } catch {
      case e: IOException => throw SgeError.FileReadError(file, s"Error reading $fileName", Some(e))
    }

    if (image.isEmpty) {
      for (extension <- actualParams.textureExtensions) {
        val sibling = file.sibling(file.nameWithoutExtension().concat("." + extension))
        if (sibling.exists() && image.isEmpty) {
          image = Nullable(sibling.name())
        }
      }
    }

    if (image.isDefined) {
      val deps = DynamicArray[AssetDescriptor[?]]()
      deps.add(new AssetDescriptor[Texture](file.sibling(image.getOrElse("")), classOf[Texture]))
      deps
    } else {
      DynamicArray[AssetDescriptor[?]]()
    }
  }

  /** Loads a PolygonRegion from a PSH (Polygon SHape) file. The PSH file format defines the polygon vertices before triangulation: <p> s 200.0, 100.0, ... <p> Lines not prefixed with "s" are ignored.
    * PSH files can be created with external tools, eg: <br> https://code.google.com/p/libgdx-polygoneditor/ <br> http://www.codeandweb.com/physicseditor/
    * @param file
    *   file handle to the shape definition file
    */
  def load(textureRegion: TextureRegion, file: FileHandle): PolygonRegion = scala.util.boundary {
    val reader = file.reader(256)
    try {
      var line = reader.readLine()
      while (Nullable(line).isDefined) {
        if (line.startsWith("s")) {
          // Read shape.
          val polygonStrings = line.substring(1).trim().split(",")
          val vertices       = new Array[Float](polygonStrings.length)
          for (i <- vertices.indices)
            vertices(i) = java.lang.Float.parseFloat(polygonStrings(i))
          // It would probably be better if PSH stored the vertices and triangles, then we don't have to triangulate here.
          scala.util.boundary.break(new PolygonRegion(textureRegion, vertices, triangulator.computeTriangles(vertices).toArray))
        }
        line = reader.readLine()
      }
    } catch {
      case ex: IOException => throw SgeError.FileReadError(file, s"Error reading polygon shape file: $file", Some(ex))
    } finally
      StreamUtils.closeQuietly(reader)
    throw SgeError.FileReadError(file, s"Polygon shape not found: $file")
  }
}

object PolygonRegionLoader {
  class PolygonRegionParameters extends AssetLoaderParameters[PolygonRegion] {

    /** what the line starts with that contains the file name of the texture for this {@code PolygonRegion} */
    var texturePrefix = "i "

    /** what buffer size of the reader should be used to read the {@link #texturePrefix} line
      * @see
      *   FileHandle#reader(int)
      */
    var readerBuffer = 1024

    /** the possible file name extensions of the texture file */
    var textureExtensions = Array("png", "PNG", "jpeg", "JPEG", "jpg", "JPG", "cim", "CIM", "etc1", "ETC1", "ktx", "KTX", "zktx", "ZKTX")
  }
}
