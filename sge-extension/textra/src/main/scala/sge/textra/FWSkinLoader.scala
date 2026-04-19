/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/FWSkinLoader.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: SkinLoader → sge.assets.loaders.SkinLoader,
 *     FileHandleResolver → sge.assets.loaders.FileHandleResolver,
 *     TextureAtlas → sge.graphics.g2d.TextureAtlas
 *   Convention: Asset loader for FWSkin; extends SkinLoader and overrides
 *     newSkin() to return FWSkin instead of Skin.
 *   Idiom: split packages; (using Sge) propagation.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 37
 * Covenant-baseline-methods: FWSkinLoader,newSkin
 * Covenant-source-reference: com/github/tommyettinger/textra/FWSkinLoader.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import sge.assets.loaders.FileHandleResolver
import sge.assets.loaders.SkinLoader
import sge.graphics.g2d.TextureAtlas
import sge.scenes.scene2d.ui.Skin

/** An AssetLoader to load an FWSkin. This enables you to deserialize .json, .dat, .ubj, .json.lzma, and .ubj.lzma fonts from a Skin JSON, then load it through an AssetManager. It also allows
  * scene2d.ui styles in a skin JSON file to load as both their expected scene2d.ui form and a TextraTypist widget style.
  */
class FWSkinLoader(resolver: FileHandleResolver)(using Sge) extends SkinLoader(resolver) {

  /** Override to allow subclasses of Skin to be loaded or the skin instance to be configured.
    * @param atlas
    *   The TextureAtlas that the skin will use.
    * @return
    *   A new FWSkin instance based on the provided TextureAtlas.
    */
  override protected def newSkin(atlas: TextureAtlas): Skin =
    new FWSkin(atlas)
}
