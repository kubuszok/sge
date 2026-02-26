/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/loader/ObjLoader.java
 * Original authors: mzechner, espitz, xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package loader

import java.io.{ BufferedReader, InputStreamReader }

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

import sge.assets.loaders.{ FileHandleResolver, ModelLoader }
import sge.files.FileHandle
import sge.graphics.{ Color, GL20, VertexAttribute, VertexAttributes }
import sge.graphics.g3d.Material
import sge.graphics.g3d.model.data._
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Quaternion, Vector3 }
import sge.utils.{ DynamicArray, Nullable }

/** {@link ModelLoader} to load Wavefront OBJ files. Only intended for testing basic models/meshes and educational usage. The Wavefront specification is NOT fully implemented, only a subset of the
  * specification is supported. Especially the {@link Material} ({@link Attributes}), e.g. the color or texture applied, might not or not correctly be loaded. </p>
  *
  * This {@link ModelLoader} can be used to load very basic models without having to convert them to a more suitable format. Therefore it can be used for educational purposes and to quickly test a
  * basic model, but should not be used in production. Instead use {@link G3dModelLoader}. </p>
  *
  * Because of above reasons, when an OBJ file is loaded using this loader, it will log and error. To prevent this error from being logged, set the {@link #logWarning} flag to false. However, it is
  * advised not to do so. </p>
  *
  * An OBJ file only contains the mesh (shape). It may link to a separate MTL file, which is used to describe one or more materials. In that case the MTL filename (might be case-sensitive) is expected
  * to be located relative to the OBJ file. The MTL file might reference one or more texture files, in which case those filename(s) are expected to be located relative to the MTL file. </p>
  * @author
  *   mzechner, espitz, xoppa
  */
class ObjLoader(resolver: FileHandleResolver)(using sge: Sge) extends ModelLoader[ObjLoader.ObjLoaderParameters](resolver) {

  def this()(using sge: Sge) =
    this(null)

  private val verts:  DynamicArray[Float]          = DynamicArray[Float](300)
  private val norms:  DynamicArray[Float]          = DynamicArray[Float](300)
  private val uvs:    DynamicArray[Float]          = DynamicArray[Float](200)
  private val groups: ArrayBuffer[ObjLoader.Group] = ArrayBuffer[ObjLoader.Group]()

  /** Directly load the model on the calling thread. The model with not be managed by an {@link AssetManager}. */
  def loadModel(fileHandle: FileHandle, flipV: Boolean): Nullable[Model] =
    loadModel(fileHandle, new ObjLoader.ObjLoaderParameters(flipV))

  override def loadModelData(file: FileHandle, parameters: ObjLoader.ObjLoaderParameters): Nullable[ModelData] =
    loadModelData(file, Nullable(parameters).fold(false)(_.flipV))

  protected def loadModelData(file: FileHandle, flipV: Boolean): Nullable[ModelData] = boundary {
    if (ObjLoader.logWarning) {
      sge.application.error("ObjLoader", "Wavefront (OBJ) is not fully supported, consult the documentation for more information")
    }
    var line:      String        = null.asInstanceOf[String]
    var tokens:    Array[String] = null.asInstanceOf[Array[String]]
    var firstChar: Char          = '\u0000'
    val mtl = new ObjLoader.MtlLoader()

    // Create a "default" Group and set it as the active group, in case
    // there are no groups or objects defined in the OBJ file.
    var activeGroup = new ObjLoader.Group("default")
    groups += activeGroup

    val reader = new BufferedReader(new InputStreamReader(file.read()), 4096)
    var id     = 0
    try {
      line = reader.readLine()
      while (line != null) {

        tokens = line.split("\\s+")
        if (tokens.length < 1) {
          line = null // break equivalent
        } else {
          if (tokens(0).isEmpty) {
            // continue - skip empty token
          } else {
            firstChar = tokens(0).toLowerCase().charAt(0)
            if (firstChar == '#') {
              // continue - skip comment
            } else if (firstChar == 'v') {
              if (tokens(0).length == 1) {
                verts.add(java.lang.Float.parseFloat(tokens(1)))
                verts.add(java.lang.Float.parseFloat(tokens(2)))
                verts.add(java.lang.Float.parseFloat(tokens(3)))
              } else if (tokens(0).charAt(1) == 'n') {
                norms.add(java.lang.Float.parseFloat(tokens(1)))
                norms.add(java.lang.Float.parseFloat(tokens(2)))
                norms.add(java.lang.Float.parseFloat(tokens(3)))
              } else if (tokens(0).charAt(1) == 't') {
                uvs.add(java.lang.Float.parseFloat(tokens(1)))
                uvs.add(if (flipV) 1 - java.lang.Float.parseFloat(tokens(2)) else java.lang.Float.parseFloat(tokens(2)))
              }
            } else if (firstChar == 'f') {
              var parts: Array[String] = null
              val faces = activeGroup.faces
              var i     = 1
              while (i < tokens.length - 2) {
                parts = tokens(1).split("/")
                faces += getIndex(parts(0), verts.size)
                if (parts.length > 2) {
                  if (i == 1) activeGroup.hasNorms = true
                  faces += getIndex(parts(2), norms.size)
                }
                if (parts.length > 1 && parts(1).nonEmpty) {
                  if (i == 1) activeGroup.hasUVs = true
                  faces += getIndex(parts(1), uvs.size)
                }
                i += 1
                parts = tokens(i).split("/")
                faces += getIndex(parts(0), verts.size)
                if (parts.length > 2) faces += getIndex(parts(2), norms.size)
                if (parts.length > 1 && parts(1).nonEmpty) faces += getIndex(parts(1), uvs.size)
                i += 1
                parts = tokens(i).split("/")
                faces += getIndex(parts(0), verts.size)
                if (parts.length > 2) faces += getIndex(parts(2), norms.size)
                if (parts.length > 1 && parts(1).nonEmpty) faces += getIndex(parts(1), uvs.size)
                activeGroup.numFaces += 1
                i += 1
              }
            } else if (firstChar == 'o' || firstChar == 'g') {
              // This implementation only supports single object or group
              // definitions. i.e. "o group_a group_b" will set group_a
              // as the active group, while group_b will simply be
              // ignored.
              if (tokens.length > 1)
                activeGroup = setActiveGroup(tokens(1))
              else
                activeGroup = setActiveGroup("default")
            } else if (tokens(0).equals("mtllib")) {
              mtl.load(file.parent().child(tokens(1)))
            } else if (tokens(0).equals("usemtl")) {
              if (tokens.length == 1)
                activeGroup.materialName = "default"
              else
                activeGroup.materialName = tokens(1).replace('.', '_')
            }
          }
          if (line != null) line = reader.readLine()
        }
      }
      reader.close()
    } catch {
      case _: java.io.IOException =>
        break(Nullable.empty[ModelData])
    }

    // If the "default" group or any others were not used, get rid of them
    var i = 0
    while (i < groups.size)
      if (groups(i).numFaces < 1) {
        groups.remove(i)
      } else {
        i += 1
      }

    // If there are no groups left, there is no valid Model to return
    if (groups.isEmpty) break(Nullable.empty[ModelData])

    // Get number of objects/groups remaining after removing empty ones
    val numGroups = groups.size

    val data = new ModelData()

    var g = 0
    while (g < numGroups) {
      val group       = groups(g)
      val faces       = group.faces
      val numElements = faces.size
      val numFaces    = group.numFaces
      val hasNorms    = group.hasNorms
      val hasUVs      = group.hasUVs

      val finalVerts = new Array[Float]((numFaces * 3) * (3 + (if (hasNorms) 3 else 0) + (if (hasUVs) 2 else 0)))

      var fi = 0
      var vi = 0
      while (fi < numElements) {
        var vertIndex = faces(fi) * 3
        fi += 1
        finalVerts(vi) = verts(vertIndex)
        vi += 1
        vertIndex += 1
        finalVerts(vi) = verts(vertIndex)
        vi += 1
        vertIndex += 1
        finalVerts(vi) = verts(vertIndex)
        vi += 1
        if (hasNorms) {
          var normIndex = faces(fi) * 3
          fi += 1
          finalVerts(vi) = norms(normIndex)
          vi += 1
          normIndex += 1
          finalVerts(vi) = norms(normIndex)
          vi += 1
          normIndex += 1
          finalVerts(vi) = norms(normIndex)
          vi += 1
        }
        if (hasUVs) {
          var uvIndex = faces(fi) * 2
          fi += 1
          finalVerts(vi) = uvs(uvIndex)
          vi += 1
          uvIndex += 1
          finalVerts(vi) = uvs(uvIndex)
          vi += 1
        }
      }

      val numIndices   = if (numFaces * 3 >= Short.MaxValue) 0 else numFaces * 3
      val finalIndices = new Array[Short](numIndices)
      // if there are too many vertices in a mesh, we can't use indices
      if (numIndices > 0) {
        var ii = 0
        while (ii < numIndices) {
          finalIndices(ii) = ii.toShort
          ii += 1
        }
      }

      val attributes = ArrayBuffer[VertexAttribute]()
      attributes += new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE)
      if (hasNorms) attributes += new VertexAttribute(VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE)
      if (hasUVs) attributes += new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0")

      id += 1
      val stringId = id.toString
      val nodeId   = if ("default" == group.name) "node" + stringId else group.name
      val meshId   = if ("default" == group.name) "mesh" + stringId else group.name
      val partId   = if ("default" == group.name) "part" + stringId else group.name
      val node     = new ModelNode()
      node.id = nodeId
      node.meshId = meshId
      node.scale = new Vector3(1, 1, 1)
      node.translation = new Vector3()
      node.rotation = new Quaternion()
      val pm = new ModelNodePart()
      pm.meshPartId = partId
      pm.materialId = group.materialName
      node.parts = Array(pm)
      val part = new ModelMeshPart()
      part.id = partId
      part.indices = finalIndices
      part.primitiveType = GL20.GL_TRIANGLES
      val mesh = new ModelMesh()
      mesh.id = meshId
      mesh.attributes = attributes.toArray
      mesh.vertices = finalVerts
      mesh.parts = Array(part)
      data.nodes += node
      data.meshes += mesh
      val mm = mtl.getMaterial(group.materialName)
      data.materials += mm
      g += 1
    }

    // for (ModelMaterial m : mtl.materials)
    // data.materials.add(m);

    // An instance of ObjLoader can be used to load more than one OBJ.
    // Clearing the Array cache instead of instantiating new
    // Arrays should result in slightly faster load times for
    // subsequent calls to loadObj
    if (verts.size > 0) verts.clear()
    if (norms.size > 0) norms.clear()
    if (uvs.size > 0) uvs.clear()
    if (groups.nonEmpty) groups.clear()

    Nullable(data)
  }

  private def setActiveGroup(name: String): ObjLoader.Group = boundary {
    // TODO: Check if a HashMap.get calls are faster than iterating
    // through an Array
    for (group <- groups)
      if (group.name.equals(name)) break(group)
    val group = new ObjLoader.Group(name)
    groups += group
    group
  }

  private def getIndex(index: String, size: Int): Int =
    if (index == null || index.isEmpty) 0
    else {
      val idx = Integer.parseInt(index)
      if (idx < 0) size + idx
      else idx - 1
    }
}

object ObjLoader {

  /** Set to false to prevent a warning from being logged when this class is used. Do not change this value, unless you are absolutely sure what you are doing. Consult the documentation for more
    * information.
    */
  var logWarning: Boolean = false

  class ObjLoaderParameters(val flipV: Boolean = false) extends ModelLoader.ModelParameters {
    def this() = this(false)
  }

  private class Group(val name: String) {
    var materialName: String           = "default"
    var faces:        ArrayBuffer[Int] = ArrayBuffer[Int]()
    var numFaces:     Int              = 0
    var hasNorms:     Boolean          = false
    var hasUVs:       Boolean          = false
    var mat:          Material         = new Material("")
  }

  /** Loads .mtl files */
  private class MtlLoader {
    val materials: ArrayBuffer[ModelMaterial] = ArrayBuffer[ModelMaterial]()

    /** loads .mtl file */
    def load(file: FileHandle): Unit = {
      var line:   String        = null.asInstanceOf[String]
      var tokens: Array[String] = null.asInstanceOf[Array[String]]

      val currentMaterial = new ObjMaterial()

      if (file == null || !file.exists()) {
        // early exit - no file to load
      } else {
        val reader = new BufferedReader(new InputStreamReader(file.read()), 4096)
        try {
          line = reader.readLine()
          while (line != null) {

            if (line.nonEmpty && line.charAt(0) == '\t') line = line.substring(1).trim

            tokens = line.split("\\s+")

            if (tokens(0).isEmpty) {
              // continue - skip empty
            } else if (tokens(0).charAt(0) == '#') {
              // continue - skip comment
            } else {
              val key = tokens(0).toLowerCase
              if (key.equals("newmtl")) {
                val mat = currentMaterial.build()
                materials += mat

                if (tokens.length > 1) {
                  currentMaterial.materialName = tokens(1)
                  currentMaterial.materialName = currentMaterial.materialName.replace('.', '_')
                } else {
                  currentMaterial.materialName = "default"
                }

                currentMaterial.reset()
              } else if (key.equals("ka")) {
                currentMaterial.ambientColor = Nullable(parseColor(tokens))
              } else if (key.equals("kd")) {
                currentMaterial.diffuseColor = parseColor(tokens)
              } else if (key.equals("ks")) {
                currentMaterial.specularColor = parseColor(tokens)
              } else if (key.equals("tr") || key.equals("d")) {
                currentMaterial.opacity = java.lang.Float.parseFloat(tokens(1))
              } else if (key.equals("ns")) {
                currentMaterial.shininess = java.lang.Float.parseFloat(tokens(1))
              } else if (key.equals("map_d")) {
                currentMaterial.alphaTexFilename = Nullable(file.parent().child(tokens(1)).path())
              } else if (key.equals("map_ka")) {
                currentMaterial.ambientTexFilename = Nullable(file.parent().child(tokens(1)).path())
              } else if (key.equals("map_kd")) {
                currentMaterial.diffuseTexFilename = Nullable(file.parent().child(tokens(1)).path())
              } else if (key.equals("map_ks")) {
                currentMaterial.specularTexFilename = Nullable(file.parent().child(tokens(1)).path())
              } else if (key.equals("map_ns")) {
                currentMaterial.shininessTexFilename = Nullable(file.parent().child(tokens(1)).path())
              }
            }
            line = reader.readLine()
          }
          reader.close()
        } catch {
          case _: java.io.IOException => // silently return
        }

        // last material
        val mat = currentMaterial.build()
        materials += mat
      }
    }

    private def parseColor(tokens: Array[String]): Color = {
      val r = java.lang.Float.parseFloat(tokens(1))
      val g = java.lang.Float.parseFloat(tokens(2))
      val b = java.lang.Float.parseFloat(tokens(3))
      val a = if (tokens.length > 4) java.lang.Float.parseFloat(tokens(4)) else 1f
      new Color(r, g, b, a)
    }

    def getMaterial(name: String): ModelMaterial = boundary {
      for (m <- materials)
        if (m.id.equals(name)) break(m)
      val mat = new ModelMaterial()
      mat.id = name
      mat.diffuse = new Color(Color.WHITE)
      materials += mat
      mat
    }

    private class ObjMaterial {
      var materialName:         String           = "default"
      var ambientColor:         Nullable[Color]  = Nullable.empty
      var diffuseColor:         Color            = Color.WHITE
      var specularColor:        Color            = Color.WHITE
      var opacity:              Float            = 1f
      var shininess:            Float            = 0f
      var alphaTexFilename:     Nullable[String] = Nullable.empty
      var ambientTexFilename:   Nullable[String] = Nullable.empty
      var diffuseTexFilename:   Nullable[String] = Nullable.empty
      var shininessTexFilename: Nullable[String] = Nullable.empty
      var specularTexFilename:  Nullable[String] = Nullable.empty

      def build(): ModelMaterial = {
        val mat = new ModelMaterial()
        mat.id = materialName
        mat.ambient = ambientColor.fold(null.asInstanceOf[Color])(c => new Color(c))
        mat.diffuse = new Color(diffuseColor)
        mat.specular = new Color(specularColor)
        mat.opacity = opacity
        mat.shininess = shininess
        alphaTexFilename.foreach(fn => addTexture(mat, fn, ModelTexture.USAGE_TRANSPARENCY))
        ambientTexFilename.foreach(fn => addTexture(mat, fn, ModelTexture.USAGE_AMBIENT))
        diffuseTexFilename.foreach(fn => addTexture(mat, fn, ModelTexture.USAGE_DIFFUSE))
        specularTexFilename.foreach(fn => addTexture(mat, fn, ModelTexture.USAGE_SPECULAR))
        shininessTexFilename.foreach(fn => addTexture(mat, fn, ModelTexture.USAGE_SHININESS))
        mat
      }

      private def addTexture(mat: ModelMaterial, texFilename: String, usage: Int): Unit = {
        val tex = new ModelTexture()
        tex.usage = usage
        tex.fileName = texFilename
        if (mat.textures == null) mat.textures = new ArrayBuffer[ModelTexture](1)
        mat.textures += tex
      }

      def reset(): Unit = {
        ambientColor = Nullable.empty
        diffuseColor = Color.WHITE
        specularColor = Color.WHITE
        opacity = 1f
        shininess = 0f
        alphaTexFilename = Nullable.empty
        ambientTexFilename = Nullable.empty
        diffuseTexFilename = Nullable.empty
        shininessTexFilename = Nullable.empty
        specularTexFilename = Nullable.empty
      }
    }
  }
}
