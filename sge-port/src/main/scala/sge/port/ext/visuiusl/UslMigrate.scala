package sge.port.ext.visuiusl

import balticporter.runner.PortConfig

import java.nio.file.Path

/** Migrate **USL** — VisUI's skin-definition language compiler (18 files, 1,604 LOC: lexer, recursive parser, style merger, JSON writer). The port is `.../ports/visui-usl/main.conf`, STANDALONE (not
  * a scope edit to `sge-visui` — independent maven coordinates). Reference hand port never ported USL at all: a hand-written CHARACTER SCANNER and a ZERO-AUTHORING ORACLE (upstream ships both `.usl`
  * fixtures and expected `.json`).
  */
object UslMigrate {

  def main(args: Array[String]): Unit =
    PortConfig.load(UslPort.conf("main.conf"), args.toSeq).execute()
}

/** Where this port's configuration lives, for the `main` that names it. */
object UslPort {

  def repoRoot: Path =
    Path.of(sys.props.getOrElse("balticporter.root", ".")).toAbsolutePath.normalize

  def conf(name: String): Path = repoRoot.resolve("balticporter/corpus/ports/visui-usl").resolve(name)
}
