/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/resolvers/ClasspathFileHandleResolver.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders
package resolvers

import sge.files.FileHandle

class ClasspathFileHandleResolver(using sge: Sge) extends FileHandleResolver {
  override def resolve(fileName: String): FileHandle =
    sge.files.classpath(fileName)
}
