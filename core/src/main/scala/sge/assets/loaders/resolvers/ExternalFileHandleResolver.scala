/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/resolvers/ExternalFileHandleResolver.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: constructor gains `(using Sge)` — Java uses static `Gdx.files`, Scala uses context `Sge().files`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders
package resolvers

import sge.files.FileHandle

class ExternalFileHandleResolver(using Sge) extends FileHandleResolver {
  override def resolve(fileName: String): FileHandle =
    Sge().files.external(fileName)
}
