package com.example

// Minimal standalone entry point. Deliberately does NOT reference the SGE engine
// so the scripted test project compiles without the sge library on the classpath.
// The browser packaging task under test only needs a compilable project with a
// mainClass + the asset resources; the JS linking step (fullLinkJS) is bypassed
// by overriding sgeJsOutputDir to a stub directory (see build.sbt).
object HelloGame {
  def main(args: Array[String]): Unit =
    println("hello sge browser")
}
