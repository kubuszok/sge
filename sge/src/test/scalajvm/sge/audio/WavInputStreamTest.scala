/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package audio

import java.io.{ ByteArrayOutputStream, File, FileOutputStream }

class WavInputStreamTest extends munit.FunSuite {

  /** Builds a minimal valid WAV file in memory with the given parameters. */
  private def buildWav(
    channels:   Short,
    sampleRate: Int,
    bitDepth:   Short,
    pcmData:    Array[Byte]
  ): Array[Byte] = {
    val out           = ByteArrayOutputStream()
    val dataSize      = pcmData.length
    val fmtChunkSize  = 16
    val totalFileSize = 4 + (8 + fmtChunkSize) + (8 + dataSize)
    val byteRate      = sampleRate * channels * (bitDepth / 8)
    val blockAlign    = (channels * (bitDepth / 8)).toShort

    // RIFF header
    out.write(Array[Byte]('R', 'I', 'F', 'F'))
    writeInt32LE(out, totalFileSize)
    out.write(Array[Byte]('W', 'A', 'V', 'E'))

    // fmt chunk
    out.write(Array[Byte]('f', 'm', 't', ' '))
    writeInt32LE(out, fmtChunkSize)
    writeInt16LE(out, 1) // PCM format
    writeInt16LE(out, channels.toInt)
    writeInt32LE(out, sampleRate)
    writeInt32LE(out, byteRate)
    writeInt16LE(out, blockAlign.toInt)
    writeInt16LE(out, bitDepth.toInt)

    // data chunk
    out.write(Array[Byte]('d', 'a', 't', 'a'))
    writeInt32LE(out, dataSize)
    out.write(pcmData)

    out.toByteArray()
  }

  private def writeInt16LE(out: ByteArrayOutputStream, v: Int): Unit = {
    out.write(v & 0xff)
    out.write((v >> 8) & 0xff)
  }

  private def writeInt32LE(out: ByteArrayOutputStream, v: Int): Unit = {
    out.write(v & 0xff)
    out.write((v >> 8) & 0xff)
    out.write((v >> 16) & 0xff)
    out.write((v >> 24) & 0xff)
  }

  private def writeTempWav(data: Array[Byte]): File = {
    val f = File.createTempFile("sge-wav-test-", ".wav")
    f.deleteOnExit()
    val fos = FileOutputStream(f)
    try fos.write(data)
    finally fos.close()
    f
  }

  // ---- header parsing ----

  test("parses mono 16-bit WAV header") {
    val pcm  = new Array[Byte](100)
    val wav  = buildWav(channels = 1, sampleRate = 44100, bitDepth = 16, pcmData = pcm)
    val file = writeTempWav(wav)
    val fh   = files.FileHandle(file, files.FileType.Absolute, utils.Nullable.empty)

    val wis = WavInputStream(fh)
    try {
      assertEquals(wis.channels, 1)
      assertEquals(wis.sampleRate, 44100)
      assertEquals(wis.bitDepth, 16)
      assertEquals(wis.dataRemaining, 100)
      assertEquals(wis.codecType, 1) // PCM
    } finally wis.close()
  }

  test("parses stereo 16-bit WAV header") {
    val pcm  = new Array[Byte](200)
    val wav  = buildWav(channels = 2, sampleRate = 48000, bitDepth = 16, pcmData = pcm)
    val file = writeTempWav(wav)
    val fh   = files.FileHandle(file, files.FileType.Absolute, utils.Nullable.empty)

    val wis = WavInputStream(fh)
    try {
      assertEquals(wis.channels, 2)
      assertEquals(wis.sampleRate, 48000)
      assertEquals(wis.bitDepth, 16)
      assertEquals(wis.dataRemaining, 200)
    } finally wis.close()
  }

  test("parses mono 8-bit WAV header") {
    val pcm  = new Array[Byte](50)
    val wav  = buildWav(channels = 1, sampleRate = 22050, bitDepth = 8, pcmData = pcm)
    val file = writeTempWav(wav)
    val fh   = files.FileHandle(file, files.FileType.Absolute, utils.Nullable.empty)

    val wis = WavInputStream(fh)
    try {
      assertEquals(wis.channels, 1)
      assertEquals(wis.sampleRate, 22050)
      assertEquals(wis.bitDepth, 8)
      assertEquals(wis.dataRemaining, 50)
    } finally wis.close()
  }

  // ---- reading data ----

  test("reads PCM data correctly") {
    val pcm  = Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)
    val wav  = buildWav(channels = 1, sampleRate = 8000, bitDepth = 8, pcmData = pcm)
    val file = writeTempWav(wav)
    val fh   = files.FileHandle(file, files.FileType.Absolute, utils.Nullable.empty)

    val wis = WavInputStream(fh)
    try {
      val buffer = new Array[Byte](8)
      val read   = wis.read(buffer)
      assertEquals(read, 8)
      assertEquals(buffer.toSeq, pcm.toSeq)
    } finally wis.close()
  }

  test("read returns -1 when no data remaining") {
    val pcm  = Array[Byte](1, 2)
    val wav  = buildWav(channels = 1, sampleRate = 8000, bitDepth = 8, pcmData = pcm)
    val file = writeTempWav(wav)
    val fh   = files.FileHandle(file, files.FileType.Absolute, utils.Nullable.empty)

    val wis = WavInputStream(fh)
    try {
      val buffer = new Array[Byte](2)
      wis.read(buffer) // read all data
      val read = wis.read(buffer) // should return -1
      assertEquals(read, -1)
    } finally wis.close()
  }

  test("read fills partial buffer") {
    val pcm  = Array[Byte](10, 20, 30)
    val wav  = buildWav(channels = 1, sampleRate = 8000, bitDepth = 8, pcmData = pcm)
    val file = writeTempWav(wav)
    val fh   = files.FileHandle(file, files.FileType.Absolute, utils.Nullable.empty)

    val wis = WavInputStream(fh)
    try {
      val buffer = new Array[Byte](10) // bigger than data
      val read   = wis.read(buffer)
      assertEquals(read, 3)
      assertEquals(buffer(0), 10.toByte)
      assertEquals(buffer(1), 20.toByte)
      assertEquals(buffer(2), 30.toByte)
    } finally wis.close()
  }

  // ---- error handling ----

  test("rejects non-RIFF file") {
    val file = writeTempWav(Array[Byte](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
    val fh   = files.FileHandle(file, files.FileType.Absolute, utils.Nullable.empty)
    intercept[utils.SgeError.InvalidInput] {
      WavInputStream(fh)
    }
  }
}
