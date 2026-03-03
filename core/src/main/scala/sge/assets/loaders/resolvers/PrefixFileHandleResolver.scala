/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/resolvers/PrefixFileHandleResolver.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getBaseResolver, getPrefix
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders
package resolvers

import sge.files.FileHandle

/** {@link FileHandleResolver} that adds a prefix to the filename before passing it to the base resolver. Can be used e.g. to use a given subfolder from the base resolver. The prefix is added as is,
  * you have to include any trailing '/' character if needed.
  * @author
  *   Xoppa (original implementation)
  */
class PrefixFileHandleResolver(private var baseResolver: FileHandleResolver, private var prefix: String) extends FileHandleResolver {

  def setBaseResolver(baseResolver: FileHandleResolver): Unit =
    this.baseResolver = baseResolver

  def getBaseResolver: FileHandleResolver =
    baseResolver

  def setPrefix(prefix: String): Unit =
    this.prefix = prefix

  def getPrefix: String =
    prefix

  override def resolve(fileName: String): FileHandle =
    baseResolver.resolve(prefix + fileName)
}
