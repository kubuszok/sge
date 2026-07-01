/*
 * SGE — ISS-562 scripted test fixture (packaging/android).
 * Scala port copyright 2025-2026 Mateusz Kubuszok.
 * Licensed under the Apache License, Version 2.0.
 */
package com.example

// Standalone entry point for the Android packaging scripted test.
// Deliberately does NOT reference the SGE engine or any android.* API so the
// fixture compiles without the sge library on the classpath — the androidSign
// pipeline under test only needs a compilable project with an
// AndroidManifest.xml to DEX, package (aapt2), zipalign and sign.
object HelloGame {
  def main(args: Array[String]): Unit =
    println("hello sge android")
}
