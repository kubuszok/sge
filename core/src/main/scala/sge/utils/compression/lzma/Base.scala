package sge.utils.compression.lzma

object Base {
  val kNumRepDistances: Int = 4
  val kNumStates: Int = 12

  def stateInit(): Int = 0

  def stateUpdateChar(index: Int): Int =
    if (index < 4) 0
    else if (index < 10) index - 3
    else index - 6

  def stateUpdateMatch(index: Int): Int =
    if (index < 7) 7 else 10

  def stateUpdateRep(index: Int): Int =
    if (index < 7) 8 else 11

  def stateUpdateShortRep(index: Int): Int =
    if (index < 7) 9 else 11

  def stateIsCharState(index: Int): Boolean = index < 7

  val kNumPosSlotBits: Int = 6
  val kDicLogSizeMin: Int = 0
  // val kDicLogSizeMax: Int = 28
  // val kDistTableSizeMax: Int = kDicLogSizeMax * 2

  val kNumLenToPosStatesBits: Int = 2 // it's for speed optimization
  val kNumLenToPosStates: Int = 1 << kNumLenToPosStatesBits

  val kMatchMinLen: Int = 2

  def getLenToPosState(len: Int): Int = {
    val l = len - kMatchMinLen
    if (l < kNumLenToPosStates) l else kNumLenToPosStates - 1
  }

  val kNumAlignBits: Int = 4
  val kAlignTableSize: Int = 1 << kNumAlignBits
  val kAlignMask: Int = kAlignTableSize - 1

  val kStartPosModelIndex: Int = 4
  val kEndPosModelIndex: Int = 14
  val kNumPosModels: Int = kEndPosModelIndex - kStartPosModelIndex

  val kNumFullDistances: Int = 1 << (kEndPosModelIndex / 2)

  val kNumLitPosStatesBitsEncodingMax: Int = 4
  val kNumLitContextBitsMax: Int = 8

  val kNumPosStatesBitsMax: Int = 4
  val kNumPosStatesMax: Int = 1 << kNumPosStatesBitsMax
  val kNumPosStatesBitsEncodingMax: Int = 4
  val kNumPosStatesEncodingMax: Int = 1 << kNumPosStatesBitsEncodingMax

  val kNumLowLenBits: Int = 3
  val kNumMidLenBits: Int = 3
  val kNumHighLenBits: Int = 8
  val kNumLowLenSymbols: Int = 1 << kNumLowLenBits
  val kNumMidLenSymbols: Int = 1 << kNumMidLenBits
  val kNumLenSymbols: Int = kNumLowLenSymbols + kNumMidLenSymbols + (1 << kNumHighLenBits)
  val kMatchMaxLen: Int = kMatchMinLen + kNumLenSymbols - 1
}
