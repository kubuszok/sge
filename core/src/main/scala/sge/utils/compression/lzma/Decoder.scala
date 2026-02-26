/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/compression/lzma/Decoder.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils
package compression
package lzma

// Converted from Java to Scala
import java.io.IOException

import sge.utils.compression.lz.OutWindow
import sge.utils.compression.rangecoder.BitTreeDecoder

class Decoder {
  class LenDecoder {
    val m_Choice:       Array[Short]          = Array.ofDim[Short](2)
    val m_LowCoder:     Array[BitTreeDecoder] = Array.ofDim[BitTreeDecoder](Base.kNumPosStatesMax)
    val m_MidCoder:     Array[BitTreeDecoder] = Array.ofDim[BitTreeDecoder](Base.kNumPosStatesMax)
    var m_HighCoder:    BitTreeDecoder        = new BitTreeDecoder(Base.kNumHighLenBits)
    var m_NumPosStates: Int                   = 0

    def Create(numPosStates: Int): Unit =
      while (m_NumPosStates < numPosStates) {
        m_LowCoder(m_NumPosStates) = new BitTreeDecoder(Base.kNumLowLenBits)
        m_MidCoder(m_NumPosStates) = new BitTreeDecoder(Base.kNumMidLenBits)
        m_NumPosStates += 1
      }

    def Init(): Unit = {
      sge.utils.compression.rangecoder.Decoder.initBitModels(m_Choice)
      for (posState <- 0 until m_NumPosStates) {
        m_LowCoder(posState).init()
        m_MidCoder(posState).init()
      }
      m_HighCoder.init()
    }

    def Decode(rangeDecoder: sge.utils.compression.rangecoder.Decoder, posState: Int): Int =
      if (rangeDecoder.decodeBit(m_Choice, 0) == 0) m_LowCoder(posState).decode(rangeDecoder)
      else {
        var symbol = Base.kNumLowLenSymbols
        if (rangeDecoder.decodeBit(m_Choice, 1) == 0)
          symbol = symbol + m_MidCoder(posState).decode(rangeDecoder)
        else
          symbol = symbol + Base.kNumMidLenSymbols + m_HighCoder.decode(rangeDecoder)
        symbol
      }
  }

  class LiteralDecoder {
    class Decoder2 {
      val m_Decoders: Array[Short] = Array.ofDim[Short](0x300)

      def Init(): Unit =
        sge.utils.compression.rangecoder.Decoder.initBitModels(m_Decoders)

      def DecodeNormal(rangeDecoder: sge.utils.compression.rangecoder.Decoder): Byte = {
        var symbol = 1
        while (symbol < 0x100)
          symbol = (symbol << 1) | rangeDecoder.decodeBit(m_Decoders, symbol)
        symbol.toByte
      }

      def DecodeWithMatchByte(rangeDecoder: sge.utils.compression.rangecoder.Decoder, matchByte: Byte): Byte = {
        var symbol = 1
        var mb     = matchByte
        var done   = false
        while (symbol < 0x100 && !done) {
          val matchBit = (mb >> 7) & 1
          mb = (mb << 1).toByte
          val bit = rangeDecoder.decodeBit(m_Decoders, ((1 + matchBit) << 8) + symbol)
          symbol = (symbol << 1) | bit
          if (matchBit != bit) {
            while (symbol < 0x100)
              symbol = (symbol << 1) | rangeDecoder.decodeBit(m_Decoders, symbol)
            done = true
          }
        }
        symbol.toByte
      }
    }

    var m_Coders:      Array[Decoder2] = null
    var m_NumPrevBits: Int             = 0
    var m_NumPosBits:  Int             = 0
    var m_PosMask:     Int             = 0

    def Create(numPosBits: Int, numPrevBits: Int): Unit =
      if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits) ()
      else {
        m_NumPosBits = numPosBits
        m_PosMask = (1 << numPosBits) - 1
        m_NumPrevBits = numPrevBits
        val numStates = 1 << (m_NumPrevBits + m_NumPosBits)
        m_Coders = Array.ofDim[Decoder2](numStates)
        for (i <- 0 until numStates)
          m_Coders(i) = new Decoder2()
      }

    def Init(): Unit = {
      val numStates = 1 << (m_NumPrevBits + m_NumPosBits)
      for (i <- 0 until numStates)
        m_Coders(i).Init()
    }

    def GetDecoder(pos: Int, prevByte: Byte): Decoder2 =
      m_Coders(((pos & m_PosMask) << m_NumPrevBits) + ((prevByte & 0xff) >>> (8 - m_NumPrevBits)))
  }

  val m_OutWindow    = new OutWindow()
  val m_RangeDecoder = new sge.utils.compression.rangecoder.Decoder()

  val m_IsMatchDecoders:    Array[Short] = Array.ofDim[Short](Base.kNumStates << Base.kNumPosStatesBitsMax)
  val m_IsRepDecoders:      Array[Short] = Array.ofDim[Short](Base.kNumStates)
  val m_IsRepG0Decoders:    Array[Short] = Array.ofDim[Short](Base.kNumStates)
  val m_IsRepG1Decoders:    Array[Short] = Array.ofDim[Short](Base.kNumStates)
  val m_IsRepG2Decoders:    Array[Short] = Array.ofDim[Short](Base.kNumStates)
  val m_IsRep0LongDecoders: Array[Short] = Array.ofDim[Short](Base.kNumStates << Base.kNumPosStatesBitsMax)

  val m_PosSlotDecoder: Array[BitTreeDecoder] = Array.tabulate(Base.kNumLenToPosStates)(_ => new BitTreeDecoder(Base.kNumPosSlotBits))
  val m_PosDecoders:    Array[Short]          = Array.ofDim[Short](Base.kNumFullDistances - Base.kEndPosModelIndex)

  val m_PosAlignDecoder = new BitTreeDecoder(Base.kNumAlignBits)

  val m_LenDecoder    = new LenDecoder()
  val m_RepLenDecoder = new LenDecoder()

  val m_LiteralDecoder = new LiteralDecoder()

  var m_DictionarySize:      Int = -1
  var m_DictionarySizeCheck: Int = -1

  var m_PosStateMask: Int = 0

  def SetDictionarySize(dictionarySize: Int): Boolean =
    if (dictionarySize < 0) false
    else {
      if (m_DictionarySize != dictionarySize) {
        m_DictionarySize = dictionarySize
        m_DictionarySizeCheck = Math.max(m_DictionarySize, 1)
        m_OutWindow.create(Math.max(m_DictionarySizeCheck, 1 << 12))
      }
      true
    }

  def SetLcLpPb(lc: Int, lp: Int, pb: Int): Boolean =
    if (lc > Base.kNumLitContextBitsMax || lp > 4 || pb > Base.kNumPosStatesBitsMax) false
    else {
      m_LiteralDecoder.Create(lp, lc)
      val numPosStates = 1 << pb
      m_LenDecoder.Create(numPosStates)
      m_RepLenDecoder.Create(numPosStates)
      m_PosStateMask = numPosStates - 1
      true
    }

  @throws[IOException]
  def Init(): Unit = {
    m_OutWindow.init(false)

    sge.utils.compression.rangecoder.Decoder.initBitModels(m_IsMatchDecoders)
    sge.utils.compression.rangecoder.Decoder.initBitModels(m_IsRep0LongDecoders)
    sge.utils.compression.rangecoder.Decoder.initBitModels(m_IsRepDecoders)
    sge.utils.compression.rangecoder.Decoder.initBitModels(m_IsRepG0Decoders)
    sge.utils.compression.rangecoder.Decoder.initBitModels(m_IsRepG1Decoders)
    sge.utils.compression.rangecoder.Decoder.initBitModels(m_IsRepG2Decoders)
    sge.utils.compression.rangecoder.Decoder.initBitModels(m_PosDecoders)

    m_LiteralDecoder.Init()
    for (i <- 0 until Base.kNumLenToPosStates)
      m_PosSlotDecoder(i).init()
    m_LenDecoder.Init()
    m_RepLenDecoder.Init()
    m_PosAlignDecoder.init()
    m_RangeDecoder.init()
  }

  @throws[IOException]
  def Code(inStream: java.io.InputStream, outStream: java.io.OutputStream, outSize: Long): Boolean = scala.util.boundary {
    m_RangeDecoder.setStream(inStream)
    m_OutWindow.setStream(outStream)
    Init()

    var state = Base.stateInit()
    var rep0  = 0
    var rep1  = 0
    var rep2  = 0
    var rep3  = 0

    var nowPos64 = 0L
    var prevByte: Byte = 0
    while (outSize < 0 || nowPos64 < outSize) {
      val posState = (nowPos64 & m_PosStateMask).toInt
      if (m_RangeDecoder.decodeBit(m_IsMatchDecoders, (state << Base.kNumPosStatesBitsMax) + posState) == 0) {
        val decoder2 = m_LiteralDecoder.GetDecoder(nowPos64.toInt, prevByte)
        if (!Base.stateIsCharState(state))
          prevByte = decoder2.DecodeWithMatchByte(m_RangeDecoder, m_OutWindow.getByte(rep0))
        else
          prevByte = decoder2.DecodeNormal(m_RangeDecoder)
        m_OutWindow.putByte(prevByte)
        state = Base.stateUpdateChar(state)
        nowPos64 += 1
      } else {
        var len = 0
        if (m_RangeDecoder.decodeBit(m_IsRepDecoders, state) == 1) {
          len = 0
          if (m_RangeDecoder.decodeBit(m_IsRepG0Decoders, state) == 0) {
            if (m_RangeDecoder.decodeBit(m_IsRep0LongDecoders, (state << Base.kNumPosStatesBitsMax) + posState) == 0) {
              state = Base.stateUpdateShortRep(state)
              len = 1
            }
          } else {
            var distance = 0
            if (m_RangeDecoder.decodeBit(m_IsRepG1Decoders, state) == 0)
              distance = rep1
            else {
              if (m_RangeDecoder.decodeBit(m_IsRepG2Decoders, state) == 0)
                distance = rep2
              else {
                distance = rep3
                rep3 = rep2
              }
              rep2 = rep1
            }
            rep1 = rep0
            rep0 = distance
          }
          if (len == 0) {
            len = m_RepLenDecoder.Decode(m_RangeDecoder, posState) + Base.kMatchMinLen
            state = Base.stateUpdateRep(state)
          }
        } else {
          rep3 = rep2
          rep2 = rep1
          rep1 = rep0
          len = Base.kMatchMinLen + m_LenDecoder.Decode(m_RangeDecoder, posState)
          state = Base.stateUpdateMatch(state)
          val posSlot = m_PosSlotDecoder(Base.getLenToPosState(len)).decode(m_RangeDecoder)
          if (posSlot >= Base.kStartPosModelIndex) {
            val numDirectBits = (posSlot >> 1) - 1
            rep0 = (2 | (posSlot & 1)) << numDirectBits
            if (posSlot < Base.kEndPosModelIndex)
              rep0 = rep0 + BitTreeDecoder.reverseDecode(m_PosDecoders, rep0 - posSlot - 1, m_RangeDecoder, numDirectBits)
            else {
              rep0 = rep0 + (m_RangeDecoder.decodeDirectBits(numDirectBits - Base.kNumAlignBits) << Base.kNumAlignBits)
              rep0 = rep0 + m_PosAlignDecoder.reverseDecode(m_RangeDecoder)
              if (rep0 < 0) {
                if (rep0 == -1) scala.util.boundary.break(true) // break
                scala.util.boundary.break(false)
              }
            }
          } else
            rep0 = posSlot
        }
        if (rep0 >= nowPos64 || rep0 >= m_DictionarySizeCheck) {
          // m_OutWindow.Flush()
          scala.util.boundary.break(false)
        }
        m_OutWindow.copyBlock(rep0, len)
        nowPos64 += len
        prevByte = m_OutWindow.getByte(0)
      }
    }
    m_OutWindow.flush()
    m_OutWindow.releaseStream()
    m_RangeDecoder.releaseStream()
    true
  }

  def SetDecoderProperties(properties: Array[Byte]): Boolean =
    if (properties.length < 5) false
    else {
      val value          = properties(0) & 0xff
      val lc             = value % 9
      val remainder      = value / 9
      val lp             = remainder % 5
      val pb             = remainder / 5
      var dictionarySize = 0
      for (i <- 0 until 4)
        dictionarySize += (properties(1 + i) & 0xff) << (i * 8)
      if (!SetLcLpPb(lc, lp, pb)) false
      else SetDictionarySize(dictionarySize)
    }
}
