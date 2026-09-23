package sge.port.ext.simplegraphs

import balticporter.runner.PortConfig

import java.nio.file.Path

/** Migrate **simple-graphs** (`src/main/java`, 29 types — a dependency-free graph library). The whole port is `.../ports/simplegraphs/main.conf` — read that, not this file; this `main` only names it
  * and gives the run its report identity (also a config-front-door acceptance proof). Exercises a package rename, a standalone BASE port with no resolution roots, `java.util.function`/`stream`, and
  * extending `AbstractCollection`.
  */
object SimpleGraphsMigrate:

  def main(args: Array[String]): Unit =
    PortConfig.load(SimpleGraphsPort.conf("main.conf"), args.toSeq).execute()

/** Where this port's two configuration files live, for the two `main`s that name them. */
object SimpleGraphsPort:

  def repoRoot: Path =
    Path.of(sys.props.getOrElse("balticporter.root", ".")).toAbsolutePath.normalize

  def conf(name: String): Path = repoRoot.resolve("balticporter/corpus/ports/simplegraphs").resolve(name)
