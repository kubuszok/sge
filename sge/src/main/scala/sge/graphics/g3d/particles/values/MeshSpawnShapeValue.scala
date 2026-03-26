/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/MeshSpawnShapeValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - All public methods ported: load, setMesh (x2), save, load(AssetManager), Triangle inner class
 * - null -> Nullable[Mesh]/Nullable[Model] (correct pattern)
 * - Java `mesh`/`model` are protected bare nulls; Scala uses Nullable[T] wrappers
 * - Java load() passes null mesh freely; Scala uses getOrElse + SgeError
 * - Triangle inner class: fields are `var` in Scala (Java package-private fields) -- minor visibility widening
 * - Triangle.pick static method moved to companion object (correct pattern)
 * - Java save uses `model.meshes.indexOf(mesh, true)` (Array identity); Scala uses DynamicArray.indexOf
 * - Java load uses `manager.get(descriptor)` (AssetDescriptor overload); Scala uses fileName+type overload
 * - Status: pass
 */
package sge
package graphics
package g3d
package particles
package values

import sge.assets.AssetManager
import sge.graphics.{ Mesh, VertexAttributes }
import sge.graphics.g3d.Model
import sge.math.{ MathUtils, Vector3 }
import sge.utils.{ Nullable, SgeError }

/** The base class of all the {@link ParticleValue} values which spawn a particle on a mesh shape.
  * @author
  *   Inferno
  */
abstract class MeshSpawnShapeValue extends SpawnShapeValue {

  protected var mesh: Nullable[Mesh] = Nullable.empty

  /** the model this mesh belongs to. It can be null, but this means the mesh will not be able to be serialized correctly. */
  protected var model: Nullable[Model] = Nullable.empty

  def this(value: MeshSpawnShapeValue) =
    this()

  override def load(value: ParticleValue): Unit = {
    super.load(value)
    val spawnShapeValue = value.asInstanceOf[MeshSpawnShapeValue]
    setMesh(
      spawnShapeValue.mesh.getOrElse(throw SgeError.InvalidInput("mesh is null")),
      spawnShapeValue.model
    )
  }

  def setMesh(mesh: Mesh, model: Nullable[Model]): Unit = {
    if (mesh.getVertexAttribute(VertexAttributes.Usage.Position).isEmpty)
      throw SgeError.InvalidInput("Mesh vertices must have Usage.Position")
    this.model = model
    this.mesh = Nullable(mesh)
  }

  def setMesh(mesh: Mesh): Unit =
    this.setMesh(mesh, Nullable.empty)

  override def save(manager: AssetManager, data: ResourceData[?]): Unit =
    model.foreach { m =>
      val saveData = data.createSaveData()
      saveData.saveAsset[Model](manager.assetFileName(m).getOrElse(""))
      saveData.save("index", Integer.valueOf(m.meshes.indexOf(mesh.getOrElse(throw SgeError.InvalidInput("mesh is null")))))
    }

  override def load(manager: AssetManager, data: ResourceData[?]): Unit = {
    val saveData   = data.saveData
    val descriptor = saveData.loadAsset()
    descriptor.foreach { desc =>
      val m     = manager(desc.fileName, desc.`type`).asInstanceOf[Model]
      val index = saveData.load[Int]("index").getOrElse(0)
      setMesh(m.meshes(index), Nullable(m))
    }
  }

}

object MeshSpawnShapeValue {

  class Triangle(
    var x1: Float,
    var y1: Float,
    var z1: Float,
    var x2: Float,
    var y2: Float,
    var z2: Float,
    var x3: Float,
    var y3: Float,
    var z3: Float
  ) {

    def pick(vector: Vector3): Vector3 = {
      val a = MathUtils.random()
      val b = MathUtils.random()
      vector.set(
        x1 + a * (x2 - x1) + b * (x3 - x1),
        y1 + a * (y2 - y1) + b * (y3 - y1),
        z1 + a * (z2 - z1) + b * (z3 - z1)
      )
    }
  }

  object Triangle {
    def pick(
      x1:     Float,
      y1:     Float,
      z1:     Float,
      x2:     Float,
      y2:     Float,
      z2:     Float,
      x3:     Float,
      y3:     Float,
      z3:     Float,
      vector: Vector3
    ): Vector3 = {
      val a = MathUtils.random()
      val b = MathUtils.random()
      vector.set(
        x1 + a * (x2 - x1) + b * (x3 - x1),
        y1 + a * (y2 - y1) + b * (y3 - y1),
        z1 + a * (z2 - z1) + b * (z3 - z1)
      )
    }
  }

}
