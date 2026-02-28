# Native JNI Bridge Convention

SGE calls native JNI methods for performance-critical operations (buffer copies,
ETC1 texture encoding/decoding, etc.). Since Scala cannot declare `native`
methods directly, we use a **two-layer pattern**: a stripped Java file holds the
native declarations, and the Scala wrapper imports and delegates to them.

## Directory Layout

| Layer | Location | Contents |
|-------|----------|---------|
| Java bridge | `core/src/main/java/gdx/src/com/badlogic/gdx/<path>.java` | Only `public static native` declarations with JNI C code in comments |
| Scala wrapper | `core/src/main/scala/sge/<path>.scala` | All logic; imports native methods from Java class |

The `gdx/src/` prefix in the Java path is organisational only -- the Java
compiler uses the `package` declaration, not the filesystem path.

## Existing Bridges

| Java Bridge | Scala Wrapper |
|-------------|--------------|
| `com.badlogic.gdx.utils.BufferUtils` | `sge.utils.BufferUtils` |
| `com.badlogic.gdx.graphics.glutils.ETC1` | `sge.graphics.glutils.ETC1` |

## How to Create a New Bridge

1. **Copy the original LibGDX `.java` file** from `./libgdx/`.
2. **Strip everything except `native` method declarations.** Comment out or
   remove all non-native methods, static fields, imports, and inner classes.
3. **Make all native methods `public static`.** The Scala side needs to call them
   via `ClassName.methodName(...)`.
4. **Preserve JNI C code in comments** (the `/* ... */` blocks after each native
   declaration). These are used by the gdx-jnigen tool to generate C stubs.
5. **Place the file** at
   `core/src/main/java/gdx/src/com/badlogic/gdx/<matching-package-path>.java`.

## How to Import in Scala

Use a renamed import to avoid name clashes between the Java bridge class and the
Scala object:

```scala
import com.badlogic.gdx.graphics.glutils.{ETC1 => ETC1Jni}
```

Then delegate each native call:

```scala
def getCompressedDataSize(width: Int, height: Int): Int =
  ETC1Jni.getCompressedDataSize(width, height)
```

## Notes

- The Java bridge files compile alongside Scala via sbt's mixed Java/Scala
  compilation. No special sbt configuration is needed.
- At runtime the JNI native library (e.g. `libgdx64.so`) must be loaded for
  these methods to work. This is handled by the backend (LWJGL, Android, etc.).
- For Scala.js and Scala Native targets, these bridges will need platform-specific
  replacements (e.g. WebGL calls, or linking to C libraries directly).
