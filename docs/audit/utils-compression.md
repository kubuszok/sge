# Audit: sge.utils.compression

**Status: REMOVED** — 13 files deleted, 2026-03-03

---

## Removal Rationale

The entire `sge.utils.compression` package (13 Scala files, ~2500 lines) has been removed
because it is dead code with no consumers:

1. **Never used by LibGDX itself.** Zero calls to `Lzma.compress()` or `Lzma.decompress()`
   exist anywhere in the LibGDX codebase. All actual compression in LibGDX uses
   `java.util.zip.GZIPInputStream`/`DeflaterOutputStream` (KTX textures, PixmapIO, Tiled maps).

2. **Never used by SGE.** Zero imports of `sge.utils.compression` outside the package itself.

3. **No built-in LZMA on any target platform.** JDK, Android, browser (Scala.js), and
   Scala Native all only provide DEFLATE/GZIP/ZIP. No cross-platform LZMA library exists
   for JVM+JS+Native, so the bundled implementation was the only option — but since
   nobody calls it, that's moot.

4. **Multiple porting bugs.** The Scala port had correctness issues that were never caught
   because the code was never exercised:
   - **Encoder constructor not called**: Java constructor body was ported as a method
     `encoder()` that was never invoked, leaving `_posSlotEncoder` and `_optimum` arrays
     uninitialized (NPE on first use).
   - **Operator precedence bug** in `Encoder.getSubCoder`: missing parentheses caused
     `<<` and `+` to bind incorrectly in Scala vs Java.
   - **`BinTree.normalizeLinks`** compared against constant 0 instead of `subValue`.
   - **Compression hangs** on even trivial inputs after fixing the constructor bug,
     indicating deeper issues in the ported control flow.

5. **Originally included for `.g3db` support.** The LZMA package was bundled in LibGDX
   as a user-facing utility. The only hypothetical SGE use would be `.g3db` binary model
   files via `UBJsonReader`, which was already deferred.

## Removed Files

| File | Original Java Source |
|------|---------------------|
| `compression/CRC.scala` | `utils/compression/CRC.java` |
| `compression/ICodeProgress.scala` | `utils/compression/ICodeProgress.java` |
| `compression/Lzma.scala` | `utils/compression/Lzma.java` |
| `compression/lz/BinTree.scala` | `utils/compression/lz/BinTree.java` |
| `compression/lz/InWindow.scala` | `utils/compression/lz/InWindow.java` |
| `compression/lz/OutWindow.scala` | `utils/compression/lz/OutWindow.java` |
| `compression/lzma/Base.scala` | `utils/compression/lzma/Base.java` |
| `compression/lzma/Decoder.scala` | `utils/compression/lzma/Decoder.java` |
| `compression/lzma/Encoder.scala` | `utils/compression/lzma/Encoder.java` |
| `compression/rangecoder/BitTreeDecoder.scala` | `utils/compression/rangecoder/BitTreeDecoder.java` |
| `compression/rangecoder/BitTreeEncoder.scala` | `utils/compression/rangecoder/BitTreeEncoder.java` |
| `compression/rangecoder/Decoder.scala` | `utils/compression/rangecoder/Decoder.java` |
| `compression/rangecoder/Encoder.scala` | `utils/compression/rangecoder/Encoder.java` |

Migration status changed from `ai_converted` to `skipped` for all 13 files.
