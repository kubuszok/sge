/*
 * Ported from gdx-ai — https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/GdxAI.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 */

/** INJECTED (`Substitutions.dropTypes` + `inject`): java's `GdxAI` sniffs `Gdx.app`/`Gdx.files` at
  * class init to pick two of its three services; the port threads that context through
  * `(using sge.Sge)`, unavailable to a static initialiser, so it installs java's own NEGATIVE
  * branch (`NullLogger`, `StandaloneFileSystem` — upstream's own out-of-libGDX path) instead, with
  * the libGDX-backed pair still emitted and settable via `setLogger`/`setFileSystem`. */
package sge.ai

/** Environment class holding references to the [[Timepiece]], [[Logger]] and [[FileSystem]]
  * instances. The references are held in static fields which allows static access to all sub
  * systems. */
object GdxAI {
  private var timepiece: sge.ai.Timepiece = new sge.ai.DefaultTimepiece()

  /** `NullLogger` rather than java's `Gdx.app == null ? … : new GdxLogger()` — see the file header.
    * Install the libGDX-backed one with `GdxAI.setLogger(new sge.ai.GdxLogger())`. */
  private var logger: sge.ai.Logger = new sge.ai.NullLogger()

  /** THE ONE SERVICE WITH NO DEFAULT THIS PORT CAN BUILD: both `FileSystem` implementations need
    * a `(using sge.Sge)` context this `object`'s initialiser has no caller or clause to supply, so
    * [[getFileSystem]] REFUSES rather than answering — louder than java, never quieter (CLAUDE.md
    * §1) — naming `setFileSystem` as the fix; both implementations are emitted and installable. */
  private var fileSystem: sge.ai.FileSystem = null

  /** Returns the timepiece service. */
  def getTimepiece(): sge.ai.Timepiece = GdxAI.timepiece

  /** Sets the timepiece service. */
  def setTimepiece(timepiece: sge.ai.Timepiece): scala.Unit = GdxAI.timepiece = timepiece

  /** Returns the logger service. */
  def getLogger(): sge.ai.Logger = GdxAI.logger

  /** Sets the logger service. */
  def setLogger(logger: sge.ai.Logger): scala.Unit = GdxAI.logger = logger

  /** Returns the filesystem service. */
  def getFileSystem(): sge.ai.FileSystem =
    if GdxAI.fileSystem != null then GdxAI.fileSystem
    else throw new sge.utils.GdxRuntimeException(
      "GdxAI has no FileSystem: this port cannot build one at class initialisation, because every " +
      "FileHandle it would hand out takes the application context this port threads through " +
      "`(using sge.Sge)` and a static initialiser has no clause. Call " +
      "`sge.ai.GdxAI.setFileSystem(new sge.ai.StandaloneFileSystem())` — or `new sge.ai.GdxFileSystem()` " +
      "— from a scope that has one. Upstream documents the same act for every platform its own " +
      "`Gdx.files == null` sniff cannot serve.")

  /** Sets the filesystem service. */
  def setFileSystem(fileSystem: sge.ai.FileSystem): scala.Unit = GdxAI.fileSystem = fileSystem
}
