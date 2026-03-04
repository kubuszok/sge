/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ResourceData.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All public methods ported faithfully
 * - Json.Serializable (write/read) on ResourceData, SaveData, AssetData: not implemented
 *   (JSON serialization deferred)
 * - resource field: Nullable[T] instead of T|null
 * - SaveData.resources: Nullable[ResourceData[?]] instead of ResourceData|null
 * - SaveData.loadAsset: returns Nullable[AssetDescriptor[?]] instead of AssetDescriptor|null
 * - SaveData.load[K]: returns Nullable[K] instead of K|null
 * - getSaveData(key): returns Nullable[SaveData] instead of SaveData|null
 * - IntArray → DynamicArray[Int] for SaveData.assets
 * - Configurable interface: Java used Configurable<T> generic; Scala uses ResourceData[?]
 */

package sge
package graphics
package g3d
package particles

import sge.assets.AssetDescriptor
import sge.assets.AssetManager
import sge.utils.DynamicArray
import sge.utils.Nullable
import sge.utils.ObjectMap

/** This class handles the assets and configurations required by a given resource when de/serialized. It's handy when a given object or one of its members requires some assets to be loaded to work
  * properly after being deserialized. To save the assets, the object should implement the [[ResourceData.Configurable]] interface and obtain a [[ResourceData.SaveData]] object to store every required
  * asset or information which will be used during the loading phase. The passed in [[AssetManager]] is generally used to find the asset file name for a given resource of a given type. The class can
  * also store global configurations, this is useful when dealing with objects which should be allocated once (i.e singleton). The deserialization process must happen in the same order of
  * serialization, because the per object [[ResourceData.SaveData]] blocks are stored as a [[DynamicArray]] within the [[ResourceData]], while the global [[ResourceData.SaveData]] instances can be
  * accessed in any order because require a unique [[String]] and are stored in an [[ObjectMap]].
  * @author
  *   Inferno
  */
class ResourceData[T]() {

  import ResourceData.*

  /** Unique data, can be used to save/load generic data which is not always loaded back after saving. Must be used to store data which is uniquely addressable by a given string (i.e a system
    * configuration).
    */
  private val uniqueData: ObjectMap[String, SaveData] = ObjectMap[String, SaveData]()

  /** Objects save data, must be loaded in the same saving order */
  private val data: DynamicArray[SaveData] = DynamicArray[SaveData]()

  /** Shared assets among all the configurable objects */
  var sharedAssets:             DynamicArray[AssetData[?]] = DynamicArray[AssetData[?]]()
  private var currentLoadIndex: Int                        = 0
  var resource:                 Nullable[T]                = Nullable.empty

  def this(resource: T) = {
    this()
    this.resource = Nullable(resource)
  }

  private[particles] def getAssetData[K](filename: String, `type`: Class[K]): Int =
    scala.util.boundary {
      var i = 0
      for (assetData <- sharedAssets) {
        if (assetData.filename == filename && assetData.`type` == `type`) {
          scala.util.boundary.break(i)
        }
        i += 1
      }
      -1
    }

  def getAssetDescriptors(): DynamicArray[AssetDescriptor[?]] = {
    val descriptors = DynamicArray[AssetDescriptor[?]]()
    for (assetData <- sharedAssets)
      descriptors.add(new AssetDescriptor(assetData.filename, assetData.`type`))
    descriptors
  }

  def getAssets(): DynamicArray[AssetData[?]] =
    sharedAssets

  /** Creates and adds a new SaveData object to the save data list */
  def createSaveData(): SaveData = {
    val saveData = new SaveData(this)
    data.add(saveData)
    saveData
  }

  /** Creates and adds a new and unique SaveData object to the save data map */
  def createSaveData(key: String): SaveData = {
    val saveData = new SaveData(this)
    if (uniqueData.containsKey(key)) throw new RuntimeException("Key already used, data must be unique, use a different key")
    uniqueData.put(key, saveData)
    saveData
  }

  /** @return the next save data in the list */
  def getSaveData(): SaveData = {
    val result = data(currentLoadIndex)
    currentLoadIndex += 1
    result
  }

  /** @return the unique save data in the map */
  def getSaveData(key: String): Nullable[SaveData] =
    uniqueData.get(key)
}

object ResourceData {

  /** This interface must be implemented by any class requiring additional assets to be loaded/saved */
  trait Configurable {
    def save(manager: AssetManager, resources: ResourceData[?]): Unit

    def load(manager: AssetManager, resources: ResourceData[?]): Unit
  }

  /** Contains all the saved data. [[SaveData.data]] is a map which links an asset name to its instance. [[SaveData.assets]] is an array of indices addressing a given [[AssetData]] in the
    * [[ResourceData]]
    */
  class SaveData(
    var resources: Nullable[ResourceData[?]] = Nullable.empty
  ) {

    val data:              ObjectMap[String, AnyRef] = ObjectMap[String, AnyRef]()
    val assets:            DynamicArray[Int]         = DynamicArray[Int]()
    private var loadIndex: Int                       = 0

    def this(resources: ResourceData[?]) = {
      this(Nullable(resources))
    }

    def saveAsset[K](filename: String, `type`: Class[K]): Unit =
      resources.foreach { res =>
        var i = res.getAssetData(filename, `type`)
        if (i == -1) {
          res.sharedAssets.add(new AssetData(filename, `type`))
          i = res.sharedAssets.size - 1
        }
        assets.add(i)
      }

    def save(key: String, value: AnyRef): Unit =
      data.put(key, value)

    def loadAsset(): Nullable[AssetDescriptor[?]] =
      if (loadIndex == assets.size) Nullable.empty
      else {
        resources.fold(Nullable.empty[AssetDescriptor[?]]) { res =>
          val assetData = res.sharedAssets(assets.items(loadIndex))
          loadIndex += 1
          Nullable(new AssetDescriptor(assetData.filename, assetData.`type`))
        }
      }

    def load[K](key: String): Nullable[K] =
      data.get(key).map(_.asInstanceOf[K])
  }

  /** This class contains all the information related to a given asset */
  class AssetData[T](
    var filename: String,
    var `type`:   Class[T]
  )
}
