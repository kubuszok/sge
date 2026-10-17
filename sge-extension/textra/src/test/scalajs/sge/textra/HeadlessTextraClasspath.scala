/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * user ruling 2026-10-17: the headless fixture's classpath reads (libGDX's default font) on Scala.js, from the port's resources
 * embedded through multiarch (the browser backend's own route).
 */
package sge
package textra

object HeadlessTextraClasspath {
  private lazy val browserFiles: files.BrowserFiles = {
    val _: AnyRef = _root_.sge.platform.GeneratedPortResources
    new files.BrowserFiles(new files.BrowserAssetLoader())
  }

  def classpath(path: String): files.FileHandle = browserFiles.classpath(path)
}
