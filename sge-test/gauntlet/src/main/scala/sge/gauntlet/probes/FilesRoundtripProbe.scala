/*
 * SGE Gauntlet — files: create/write/read/append/list/delete round-trip.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import scala.collection.mutable.ListBuffer

/** Exercises sge.files FileHandles end-to-end: string and binary round-trips, append, directory create/list/delete, and a local-storage handle. */
object FilesRoundtripProbe extends FeatureProbe {

  override def id: String = "files/roundtrip"

  override def area: String = "files"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    val files = ctx.sgeCtx.files

    // string round-trip (with non-ASCII content)
    val text = "gauntlet-Ω-łąka-42"
    val txt  = ctx.tempDir.child("roundtrip.txt")
    txt.writeString(text, false)
    checks += Check.eq("string-roundtrip", text, txt.readString())

    // append semantics
    txt.writeString("a", false)
    txt.writeString("b", true)
    checks += Check.eq("append", "ab", txt.readString())

    // binary round-trip
    val bytes = Array.tabulate[Byte](256)(i => i.toByte)
    val bin   = ctx.tempDir.child("roundtrip.bin")
    bin.writeBytes(bytes, false)
    val read = bin.readBytes()
    checks += Check.eq("binary-roundtrip-length", bytes.length, read.length)
    checks += Check.cond(
      "binary-roundtrip-content",
      java.util.Arrays.equals(bytes, read),
      "identical 256 bytes",
      if (java.util.Arrays.equals(bytes, read)) "identical" else "content differs"
    )

    // directory create + list + delete
    val sub = ctx.tempDir.child("subdir")
    sub.mkdirs()
    checks += Check.eq("mkdirs-exists", true, sub.exists())
    sub.child("one.txt").writeString("1", false)
    sub.child("two.txt").writeString("2", false)
    checks += Check.eq("list-count", 2, sub.list().length)
    checks += Check.eq("delete-directory", true, sub.deleteDirectory())
    checks += Check.eq("deleted-dir-gone", false, sub.exists())

    // file delete
    checks += Check.eq("delete-file", true, bin.delete())
    checks += Check.eq("deleted-file-gone", false, bin.exists())

    // local-storage handle (cwd-relative on desktop)
    val local = files.local("target/gauntlet-local-probe.txt")
    local.parent().mkdirs()
    local.writeString("local", false)
    checks += Check.eq("local-write-read", "local", local.readString())
    checks += Check.eq("local-delete", true, local.delete())
    ctx.log(s"local storage path: ${files.localStoragePath}")
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
