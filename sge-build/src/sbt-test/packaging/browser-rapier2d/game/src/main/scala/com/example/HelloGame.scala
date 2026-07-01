package com.example

// Self-contained entry point. Deliberately does NOT reference the SGE engine
// so the scripted test project compiles without the sge library on the classpath.
// The browser packaging task under test only needs a compilable project with a
// mainClass; the JS linking step (fullLinkJS) is bypassed by pointing
// sgeJsOutputDir at a fixture directory prepared in build.sbt.
object HelloGame {
  def main(args: Array[String]): Unit =
    println("hello sge browser rapier2d")
}
