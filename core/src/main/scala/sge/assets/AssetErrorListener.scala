/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/AssetErrorListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait; raw AssetDescriptor -> AssetDescriptor[?]
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets

trait AssetErrorListener {
  def error(asset: AssetDescriptor[?], throwable: Throwable): Unit
}
