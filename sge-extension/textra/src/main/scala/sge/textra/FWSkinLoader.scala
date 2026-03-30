/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/FWSkinLoader.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: SkinLoader → standalone class (AssetManager integration deferred),
 *     FileHandleResolver → deferred, TextureAtlas → deferred
 *   Convention: Asset loader for FWSkin; deferred until AssetManager wired up.
 */
package sge
package textra

/** An AssetLoader to load an FWSkin. This enables you to deserialize .json, .dat, .ubj, .json.lzma, and .ubj.lzma fonts from a Skin JSON, then load it through an AssetManager. It also allows
  * scene2d.ui styles in a skin JSON file to load as both their expected scene2d.ui form and a TextraTypist widget style.
  */
class FWSkinLoader {

  /** Override to allow subclasses of Skin to be loaded or the skin instance to be configured. */
  protected def newSkin(): FWSkin = new FWSkin()
}
