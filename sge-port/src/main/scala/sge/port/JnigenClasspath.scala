package sge.port

import balticporter.runner.ClasspathCache

import java.nio.file.Path

/** The `gdx-jnigen-loader` jar libGDX's own build declares: upstream moved `com.badlogic.gdx.utils.SharedLibraryLoader` out of `gdx/src` into that artifact, so a port of `gdx/src` reaches it only
  * through the frontend classpath. The resolved jar paths are cached in a file under `cacheDir`.
  */
object JnigenClasspath {

  val Coordinates: List[String] = List("com.badlogicgames.gdx:gdx-jnigen-loader:2.5.2")

  def entries(cacheDir: Path): List[Path] =
    ClasspathCache.entries(cacheDir.resolve("jnigen-classpath.txt"), "jnigen", Coordinates)
}
