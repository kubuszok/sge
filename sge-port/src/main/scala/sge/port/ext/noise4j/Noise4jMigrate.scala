package sge.port.ext.noise4j

import balticporter.runner.PortConfig

import java.nio.file.Path

/** Migrate **noise4j** (`src`, 12 types — dependency-free procedural map-generation). First corpus library with **Java enum constant bodies** — the reference hand port REWROTE each into an ordinary
  * `enum` plus `this match`, a redesign no mechanical engine may copy. Also: an unqualified interface CONSTANT read, `continue` in a doubly-nested `for`, and `java.util` mutation through the iterator
  * (why this port runs no `collections` phase).
  */
object Noise4jMigrate {

  def main(args: Array[String]): Unit =
    PortConfig.load(Noise4jPort.conf("main.conf"), args.toSeq).execute()
}

/** Where this port's configuration lives, for the `main` that names it. */
object Noise4jPort {

  def repoRoot: Path =
    Path.of(sys.props.getOrElse("balticporter.root", ".")).toAbsolutePath.normalize

  def conf(name: String): Path = repoRoot.resolve("balticporter/corpus/ports/noise4j").resolve(name)
}
