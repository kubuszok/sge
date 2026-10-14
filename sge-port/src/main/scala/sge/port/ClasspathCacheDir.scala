package sge.port

import java.nio.file.Path

/** Where every port's resolved-classpath cache file lives: under the build's `target/`, so a generation never leaves an untracked file in the checkout (which would also turn the git-describe
  * version into a dirty timestamp).
  */
object ClasspathCacheDir {

  def apply(repoRoot: Path): Path = repoRoot.resolve("target/balticporter-classpath")

  def file(repoRoot: Path, name: String): Path = apply(repoRoot).resolve(name)
}
