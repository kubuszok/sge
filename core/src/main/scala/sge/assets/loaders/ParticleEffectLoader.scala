/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/ParticleEffectLoader.java
 * Original authors: See AUTHORS file
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
import sge.graphics.g2d.{ ParticleEffect, TextureAtlas }
import sge.utils.{ DynamicArray, Nullable }

/** {@link AssetLoader} to load {@link ParticleEffect} instances. Passing a {@link ParticleEffectParameter} to {@link AssetManager#load(String, Class, AssetLoaderParameters)} allows to specify an
  * atlas file or an image directory to be used for the effect's images. Per default images are loaded from the directory in which the effect file is found.
  */
class ParticleEffectLoader(resolver: FileHandleResolver)(using Sge) extends SynchronousAssetLoader[ParticleEffect, ParticleEffectLoader.ParticleEffectParameter](resolver) {

  override def load(assetManager: AssetManager, fileName: String, file: FileHandle, parameter: ParticleEffectLoader.ParticleEffectParameter): ParticleEffect = {
    val effect = new ParticleEffect()
    Nullable(parameter).fold {
      effect.load(file, file.parent())
    } { p =>
      p.atlasFile.fold {
        p.imagesDir.fold {
          effect.load(file, file.parent())
        } { imgDir =>
          effect.load(file, imgDir)
        }
      } { atlasFile =>
        effect.load(file, assetManager.get(atlasFile, classOf[TextureAtlas]), p.atlasPrefix)
      }
    }
    effect
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: ParticleEffectLoader.ParticleEffectParameter): DynamicArray[AssetDescriptor[?]] = {
    val deps = DynamicArray[AssetDescriptor[?]]()
    Nullable(parameter).foreach { p =>
      p.atlasFile.foreach { atlasFile =>
        deps.add(new AssetDescriptor[TextureAtlas](atlasFile, classOf[TextureAtlas]))
      }
    }
    deps
  }
}

object ParticleEffectLoader {

  /** Parameter to be passed to {@link AssetManager#load(String, Class, AssetLoaderParameters)} if additional configuration is necessary for the {@link ParticleEffect}.
    */
  class ParticleEffectParameter extends AssetLoaderParameters[ParticleEffect] {

    /** Atlas file name. */
    var atlasFile: Nullable[String] = Nullable.empty

    /** Optional prefix to image names * */
    var atlasPrefix: Nullable[String] = Nullable.empty

    /** Image directory. */
    var imagesDir: Nullable[FileHandle] = Nullable.empty
  }
}
