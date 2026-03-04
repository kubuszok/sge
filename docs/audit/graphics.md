# Audit: sge.graphics

Audited: 26/26 files | Pass: 25 | Minor: 0 | Major: 1
Last updated: 2026-03-04

---

### Color.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/Color.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/Color.java` |

All 31 color constants and all static utility methods present. No issues.

### Colors.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/Colors.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/Colors.java` |

`MutableMap` replaces `ObjectMap`. `Nullable[Color]` for nullable returns. All methods match.

### Cursor.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/Cursor.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/Cursor.java` |

`SystemCursor` enum with all 11 values matches. `Disposable` -> `AutoCloseable`.

### FPSLogger.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/FPSLogger.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/FPSLogger.java` |

Fixed: placeholder FPS replaced with `Sge().graphics.getFramesPerSecond()`, converted to anonymous using.

### GLHandle.scala — pass (SGE-original)

| SGE path | `core/src/main/scala/sge/graphics/GLHandle.scala` |

SGE-original. Defines opaque handle types preventing accidental handle mixing.

### GL20.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/GL20.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/GL20.java` |

309 constants + 162 methods match. Constants moved to companion object.

### GL30.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/GL30.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/GL30.java` |

305 constants + 95 methods. Extends GL20. All GL ES 3.0 methods present.

### GL31.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/GL31.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/GL31.java` |

All constants and methods match. Extends GL30.

### GL32.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/GL32.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/GL32.java` |

All constants and methods match. `DebugProc` inner trait. Extends GL31.

### Camera.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/Camera.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/Camera.java` |

Previously fixed: `rotate(Matrix4)` bug (mul→rot), converted to anonymous using.

### OrthographicCamera.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/OrthographicCamera.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/OrthographicCamera.java` |

Previously fixed: removed dead Vector3, converted to anonymous using.

### PerspectiveCamera.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/PerspectiveCamera.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/PerspectiveCamera.java` |

Previously fixed: converted to anonymous using.

### GLTexture.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/GLTexture.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/GLTexture.java` |

Fixed: `close()` now calls `delete()` matching Java `dispose()`. Split package fixed.

### Texture.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/Texture.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/Texture.java` |

`TextureFilter`/`TextureWrap` enums match. Managed texture support. All methods present.

### Texture3D.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/Texture3D.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/Texture3D.java` |

Fixed: converted to anonymous using.

### TextureArray.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/TextureArray.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/TextureArray.java` |

All constructors and managed lifecycle match. `@nowarn` for orNull at GL30 interop boundary.

### Cubemap.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/Cubemap.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/Cubemap.java` |

`CubemapSide` enum with 6 faces matches. All constructors present.

### TextureData.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/TextureData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/TextureData.java` |

Complete trait with `TextureDataType` enum and `Factory` object. All methods match.

### Texture3DData.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/Texture3DData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/Texture3DData.java` |

All methods match.

### TextureArrayData.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/TextureArrayData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/TextureArrayData.java` |

Complete trait with `Factory` object.

### CubemapData.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/CubemapData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/CubemapData.java` |

All methods match.

### Pixmap.scala — major_issues

| SGE path | `core/src/main/scala/sge/graphics/Pixmap.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/Pixmap.java` |

**Issues**:
- `major`: `drawPixmap(7-arg)` is a STUB — empty body
- `major`: `getPixel()` is a STUB — always returns 0
- `major`: `getPixels()` is a STUB — returns empty ByteBuffer
- `major`: Missing many Java methods: `setColor(Color)`, `setColor(float,float,float,float)`, `drawLine`, `drawRect`, `drawCircle`, `fillRect`, `fillCircle`, `drawPixmap(9-arg scaled)`, `fill`, `getBlending`, `setFilter`
- Blending/Filter/Format enums are COMPLETE

### PixmapIO.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/PixmapIO.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/PixmapIO.java` |

CIM + PNG inner class. All methods present.

### Mesh.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/Mesh.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/Mesh.java` |

Fixed: ported all missing methods — `calculateRadiusSquared`, 6 `calculateRadius` overloads, `scale`, `transform`, `transformUV`, `copy`, plus companion object `transform`/`transformUV` statics.

### VertexAttribute.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/VertexAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/VertexAttribute.java` |

All factory methods, constructor overloads, copy/equals/hashCode match.

### VertexAttributes.scala — pass

| SGE path | `core/src/main/scala/sge/graphics/VertexAttributes.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/VertexAttributes.java` |

Usage constants (Position=1, Normal=8, etc.) match. All methods present.
