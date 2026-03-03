# Audit: sge.utils.compression

Audited: 13/13 files | Pass: 8 | Minor: 3 | Major: 2
Last updated: 2026-03-03

---

### ICodeProgress.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/ICodeProgress.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/ICodeProgress.java` |
| Status | pass |
| Tested | No |

**Completeness**: Single method `SetProgress(Long, Long)` faithfully ported.
**Renames**: None (method name preserved for LZMA SDK compatibility)
**Convention changes**: Java interface -> Scala trait
**TODOs**: None
**Issues**: None

---

### CRC.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/CRC.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/CRC.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 4 public methods + static Table array faithfully ported.
**Renames**: `Init`->`init`, `Update`->`update`, `UpdateByte`->`updateByte`, `GetDigest`->`getDigest`
**Convention changes**: Java static -> companion object; static initializer -> object initializer block
**TODOs**: None
**Issues**: None

---

### Lzma.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/Lzma.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/Lzma.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: Both `compress` and `decompress` methods faithfully ported. Inner `CommandLine` class ported.
**Renames**: None
**Convention changes**: Java static class -> Scala object
**TODOs**: None
**Issues**:
- `CommandLine.kEncode`/`kDecode`/`kBenchmak` are `val` (instance fields) but were `static final` in Java; should be in a companion object or `inline val`. Low impact since `CommandLine` is internal.
- `null` passed to `encoder.Code()` as `ICodeProgress` parameter (line 72) -- raw null at API boundary, acceptable for this LZMA SDK port.

---

### lzma/Base.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/lzma/Base.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/lzma/Base.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 23 constants and 6 static methods faithfully ported.
**Renames**: `StateInit`->`stateInit`, `StateUpdateChar`->`stateUpdateChar`, `StateUpdateMatch`->`stateUpdateMatch`, `StateUpdateRep`->`stateUpdateRep`, `StateUpdateShortRep`->`stateUpdateShortRep`, `StateIsCharState`->`stateIsCharState`, `GetLenToPosState`->`getLenToPosState`
**Convention changes**: Java class with all-static members -> Scala object; `GetLenToPosState` avoids mutating parameter by using local `val l`
**TODOs**: None
**Issues**: Uses flat package `package sge.utils.compression.lzma` rather than split packages; functionally equivalent but inconsistent with lzma/Decoder.scala and lzma/Encoder.scala which use split.

---

### lzma/Decoder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/lzma/Decoder.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/lzma/Decoder.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All fields, inner classes (LenDecoder, LiteralDecoder, LiteralDecoder.Decoder2), and methods faithfully ported. `Code`, `SetDictionarySize`, `SetLcLpPb`, `Init`, `SetDecoderProperties` all present.
**Renames**: Method names lowercased in rangecoder/lz calls; inner class method names preserved (Create, Init, Decode, etc.) for LZMA SDK compatibility.
**Convention changes**: Java `return` in `Code()` -> `boundary`/`break`; Java `break` in `DecodeWithMatchByte` -> boolean `done` flag; constructor initialization -> `Array.tabulate`
**TODOs**: None
**Issues**:
- `m_Coders` field initialized to `null` (line 91) -- raw null; should be `Nullable[Array[Decoder2]]` with `scala.compiletime.uninitialized` or explicit Nullable wrapping. Currently checked via `Nullable(m_Coders).isDefined` so functionally correct.

---

### lzma/Encoder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/lzma/Encoder.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/lzma/Encoder.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: All fields, inner classes (LiteralEncoder, Encoder2, LenEncoder, LenPriceTableEncoder, Optimal), companion object constants, and methods faithfully ported. Largest file in the compression package (~1240 lines Scala vs ~900 lines Java).
**Renames**: All methods lowercased; Java constructor body -> `encoder()` method; static members -> companion object.
**Convention changes**: Java `return` -> `boundary`/`break` throughout (`getOptimum`, `setPrices`, `codeOneBlock`, `Code`); Java inner classes -> companion object classes.
**TODOs**: None
**Issues**:
1. **Operator precedence bug in `getSubCoder` (line 1122)**: `(pos & m_PosMask) << m_NumPrevBits + ((prevByte & 0xff) >>> (8 - m_NumPrevBits))` -- In Scala, `+` has higher precedence than `<<`, so this parses as `(pos & m_PosMask) << (m_NumPrevBits + ((prevByte & 0xff) >>> (8 - m_NumPrevBits))))`. The Java original is `((pos & m_PosMask) << m_NumPrevBits) + ((prevByte & 0xFF) >>> (8 - m_NumPrevBits))`. Missing parentheses around `<< m_NumPrevBits`. **This is a correctness bug.**
2. **Raw null usage**: `_matchFinder = null` (lines 39, 980), `_inStream = null` (lines 90, 715), `m_Coders = null` (line 1099). All are checked via `Nullable()` wrapper where needed, but should be declared as `Nullable[T]` types.
3. **`WriteCoderProperties` uses `|=` instead of `=`**: `properties(0) = (properties(0) | ...)` and `properties(1 + i) = (properties(1 + i) | ...)`. Java uses plain assignment. Since `Array.ofDim` zeroes the array, `|=` on zero is equivalent to `=`, so this works but is misleading.
4. **`null` passed to `Code` via `Lzma.compress`**: `encoder.Code(in, out, -1, -1, null)` -- the `ICodeProgress` parameter is nullable in Java; Scala passes raw null.

---

### lz/InWindow.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/lz/InWindow.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/lz/InWindow.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All 12 fields and 10 methods faithfully ported.
**Renames**: `MoveBlock`->`moveBlock`, `ReadBlock`->`readBlock`, `Free`->`free`, `Create`->`create`, `SetStream`->`setStream`, `ReleaseStream`->`releaseStream`, `Init`->`init`, `MovePos`->`movePos`, `GetIndexByte`->`getIndexByte`, `GetMatchLen`->`getMatchLen`, `GetNumAvailableBytes`->`getNumAvailableBytes`, `ReduceOffsets`->`reduceOffsets`
**Convention changes**: Java `return` in `ReadBlock` -> `boundary`/`break`; `GetMatchLen` uses separate local vars instead of mutating params
**TODOs**: None
**Issues**:
- `free()` sets `_bufferBase = null` and `releaseStream()` sets `_stream = null` -- raw null at Java interop boundary. Fields use `scala.compiletime.uninitialized`.
- Uses flat package `package sge.utils.compression.lz` instead of split; functionally equivalent.

---

### lz/OutWindow.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/lz/OutWindow.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/lz/OutWindow.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 fields and 7 methods faithfully ported.
**Renames**: `Create`->`create`, `SetStream`->`setStream`, `ReleaseStream`->`releaseStream`, `Init`->`init`, `Flush`->`flush`, `CopyBlock`->`copyBlock`, `PutByte`->`putByte`, `GetByte`->`getByte`
**Convention changes**: Java `_buffer[_pos++]` split into separate statements; Java null check -> `Nullable(_buffer).isEmpty`
**TODOs**: None
**Issues**: `releaseStream()` sets `_stream = null` -- raw null at Java interop boundary. Acceptable for this low-level compression code.

---

### lz/BinTree.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/lz/BinTree.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/lz/BinTree.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: All 10 instance fields, 8 methods, companion object with 7 constants + CrcTable faithfully ported. Extends InWindow correctly.
**Renames**: `HASH_ARRAY`->`hashArray`, `SetType`->`setType`, `Init`->`init`, `MovePos`->`movePos`, `Create`->`create`, `GetMatches`->`getMatches`, `Skip`->`skip`, `NormalizeLinks`->`normalizeLinks`, `Normalize`->`normalize`, `SetCutValue`->`setCutValue`
**Convention changes**: Java static -> companion object; Java `break`/`continue` -> boolean flags + restructured loops; `HASH_ARRAY` (screaming case) -> `hashArray` (camelCase)
**TODOs**: None
**Issues**:
1. **`normalizeLinks` bug (line 272)**: Java compares `value <= subValue` but Scala compares `value <= BinTree.kEmptyHashValue` (which is 0). Since `subValue` is `_pos - _cyclicBufferSize` and may be much larger than 0, the Scala version will set far fewer entries to `kEmptyHashValue` than the Java version. **This is a correctness bug.**
2. Java `init()` unconditionally iterates hash array; Scala adds `Nullable(_hash).isDefined` guard (line 49). This is actually an improvement -- prevents NPE if `init()` is called before `create()`.

---

### rangecoder/BitTreeDecoder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/rangecoder/BitTreeDecoder.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/rangecoder/BitTreeDecoder.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 instance methods + 1 companion object method faithfully ported.
**Renames**: `Init`->`init`, `Decode`->`decode`, `ReverseDecode`->`reverseDecode`
**Convention changes**: Constructor parameter -> val; Java for-loop -> while-loop in `decode`
**TODOs**: None
**Issues**: None

---

### rangecoder/BitTreeEncoder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/rangecoder/BitTreeEncoder.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/rangecoder/BitTreeEncoder.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 instance methods + 2 companion object methods faithfully ported.
**Renames**: `Init`->`init`, `Encode`->`encode`, `ReverseEncode`->`reverseEncode`, `GetPrice`->`getPrice`, `ReverseGetPrice`->`reverseGetPrice`
**Convention changes**: Constructor parameter -> val; Java for-loops preserved with appropriate Scala syntax
**TODOs**: None
**Issues**: None

---

### rangecoder/Decoder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/rangecoder/Decoder.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/rangecoder/Decoder.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 fields, 5 methods, 4 constants, and 1 companion method faithfully ported.
**Renames**: `SetStream`->`setStream`, `ReleaseStream`->`releaseStream`, `Init`->`init`, `DecodeDirectBits`->`decodeDirectBits`, `DecodeBit`->`decodeBit`, `InitBitModels`->`initBitModels`
**Convention changes**: Java static -> companion object; `final` methods -> regular `def`
**TODOs**: None
**Issues**: `releaseStream()` sets `Stream = null` -- raw null at Java interop boundary. Acceptable.

---

### rangecoder/Encoder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/compression/rangecoder/Encoder.scala` |
| Java source(s) | `com/badlogic/gdx/utils/compression/rangecoder/Encoder.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 instance fields, 9 instance methods, 6 companion constants, static ProbPrices + initializer, and 4 companion methods faithfully ported.
**Renames**: `SetStream`->`setStream`, `ReleaseStream`->`releaseStream`, `Init`->`init`, `FlushData`->`flushData`, `FlushStream`->`flushStream`, `ShiftLow`->`shiftLow`, `EncodeDirectBits`->`encodeDirectBits`, `GetProcessedSizeAdd`->`getProcessedSizeAdd`, `Encode`->`encode`, `InitBitModels`->`initBitModels`, `GetPrice`->`getPrice`, `GetPrice0`->`getPrice0`, `GetPrice1`->`getPrice1`
**Convention changes**: Java do-while in `ShiftLow` -> Scala while-with-body-returns-condition; Java static -> companion object
**TODOs**: None
**Issues**: `releaseStream()` sets `Stream = null` -- raw null at Java interop boundary. Acceptable.

---

## Summary of Issues

### Correctness Bugs (2)

1. **`lzma/Encoder.scala` line 1122 -- `getSubCoder` operator precedence**: Expression `(pos & m_PosMask) << m_NumPrevBits + (...)` misses parentheses. Scala parses `+` before `<<`, producing wrong index calculation. Java original: `((pos & m_PosMask) << m_NumPrevBits) + (...)`.

2. **`lz/BinTree.scala` line 272 -- `normalizeLinks` comparison**: Compares `value <= BinTree.kEmptyHashValue` (always 0) instead of Java's `value <= subValue`. This changes which hash entries get zeroed during normalization, potentially causing incorrect match results in large files.

### Raw Null Usage (acceptable, 11 sites across 6 files)

All raw null assignments are at Java interop boundaries (stream release, field reset) and are guarded by `Nullable()` checks where dereferenced. Could be improved with `Nullable[T]` type annotations for clarity.

### Minor Style Issues

- 6 files use flat package declarations (`package sge.utils.compression.lz`) instead of split packages; the other 7 use split. Functionally equivalent but inconsistent.
- `Lzma.CommandLine` constants are instance `val` instead of companion/inline val.
