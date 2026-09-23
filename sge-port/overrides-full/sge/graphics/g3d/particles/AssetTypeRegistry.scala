package sge.graphics.g3d.particles

import sge.utils.reflect.ReflectionException

import scala.collection.mutable

/** INJECTED SCALA (Substitutions.inject) — the portable replacement for the ONE runtime class
  * lookup libGDX core performed: `ResourceData.AssetData` persists an asset's type as its class
  * NAME and, on read-back, turned that string into a `Class` again. */
object AssetTypeRegistry {

  private val byName: mutable.HashMap[String, Class[?]] = mutable.HashMap.empty

  /** Make these types nameable in persisted `ResourceData`. Idempotent. */
  def register(types: Class[?]*): Unit = types.foreach(c => byName.update(c.getName, c))

  /** Every type currently nameable — the exact set `classFor` can resolve. */
  def registered: Iterable[Class[?]] = byName.values

  /** The registered type with this name.
    * @throws ReflectionException if nothing was registered under it — same failure the reflective
    *         lookup raised for an unresolvable name, so the caller's existing handler still applies.
    */
  def classFor(name: String): Class[?] =
    byName.getOrElse(name, throw new ReflectionException("Asset type not registered: " + name))

  register(
    classOf[sge.graphics.Texture],
    classOf[sge.graphics.g2d.TextureAtlas],
    classOf[sge.graphics.g3d.Model],
    classOf[sge.graphics.g3d.particles.ParticleEffect],
  )
}
