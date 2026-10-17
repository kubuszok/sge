/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * user ruling 2026-10-17: the headless fixture's classpath reads (libGDX's default font) on Scala Native, from the resources
 * embedded into the test binary (the desktop backend's own route).
 */
package sge
package textra

object HeadlessTextraClasspath {
  private lazy val desktopFiles: files.DesktopFiles = new files.DesktopFiles()

  def classpath(path: String): files.FileHandle = desktopFiles.classpath(path)
}
