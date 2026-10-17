/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * user ruling 2026-10-17: the headless fixture's classpath reads (libGDX's default font) on the JVM, off the class loader.
 */
package sge
package textra

object HeadlessTextraClasspath {
  def classpath(path: String): files.FileHandle = new files.FileHandle(path, files.FileType.Classpath)
}
