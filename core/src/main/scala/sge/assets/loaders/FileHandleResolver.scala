/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/FileHandleResolver.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle

/** Interface for classes the can map a file name to a {@link FileHandle} . Used to allow the {@link AssetManager} to load resources from anywhere or implement caching strategies.
  * @author
  *   mzechner (original implementation)
  */
trait FileHandleResolver {
  def resolve(fileName: String): FileHandle
}
