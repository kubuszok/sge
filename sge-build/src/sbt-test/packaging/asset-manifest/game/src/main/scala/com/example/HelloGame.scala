package com.example

// Minimal standalone entry point. Deliberately does NOT reference the SGE engine
// so the scripted test project compiles without the sge library on the classpath.
// The packaging tasks under test only need a compilable project with a mainClass.
object HelloGame {
  def main(args: Array[String]): Unit =
    println("hello sge")
}
