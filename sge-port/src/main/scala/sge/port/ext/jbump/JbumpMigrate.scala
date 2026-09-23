package sge.port.ext.jbump

import balticporter.runner.PortConfig

import java.nio.file.Path

/** Migrate **jbump** (`jbump/src`, 19 types — dependency-free 2D AABB collision library): `.../ports/jbump/main.conf`. NO test suite (zero `@Test` upstream): the hand port's 32 Scala tests were
  * WRITTEN there, so evidence stops at the compiler. Forces: a class both `Iterable<T>` and `Iterator<T>`, interface constants as anonymous classes, a field/method sharing a name, `size++` as a
  * value, a `Collisions` copy ctor.
  */
object JbumpMigrate {

  def main(args: Array[String]): Unit =
    PortConfig.load(JbumpPort.conf("main.conf"), args.toSeq).execute()
}

/** Where this port's configuration lives, for the `main` that names it. */
object JbumpPort {

  def repoRoot: Path =
    Path.of(sys.props.getOrElse("balticporter.root", ".")).toAbsolutePath.normalize

  def conf(name: String): Path = repoRoot.resolve("balticporter/corpus/ports/jbump").resolve(name)
}
