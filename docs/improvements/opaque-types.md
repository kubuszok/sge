# Opaque Types Roadmap

Tracking document for proposed opaque type introductions across the SGE codebase.
Each entry catalogs the opaque type, its underlying representation, affected files,
and implementation priority.

See also: [type-safety.md](type-safety.md) for already-implemented type safety improvements.

## Already Implemented (18 types)

| Opaque Type | Underlying | Package | Notes |
|-------------|-----------|---------|-------|
| `TextureHandle` | `Int` | `sge.graphics` | GL texture name |
| `BufferHandle` | `Int` | `sge.graphics` | GL buffer name |
| `ShaderHandle` | `Int` | `sge.graphics` | GL shader name |
| `ProgramHandle` | `Int` | `sge.graphics` | GL program name |
| `FramebufferHandle` | `Int` | `sge.graphics` | GL framebuffer name |
| `RenderbufferHandle` | `Int` | `sge.graphics` | GL renderbuffer name |
| `HttpStatus` | `Int` | `sge.net` | HTTP response code |
| `Volume` | `Float` | `sge.audio` | Sound volume 0..1 |
| `Pan` | `Float` | `sge.audio` | Stereo panning -1..1 |
| `Pitch` | `Float` | `sge.audio` | Playback pitch |
| `Position` | `Float` | `sge.audio` | Playback position in seconds |
| `SoundId` | `Long` | `sge.audio` | Sound instance ID |
| `Seconds` | `Float` | `sge.utils` | Duration in seconds |
| `Degrees` | `Float` | `sge.math` | Angle in degrees |
| `Radians` | `Float` | `sge.math` | Angle in radians |
| `Epsilon` | `Float` | `sge.math` | Floating-point tolerance |
| `Align` | `Int` | `sge.utils` | Bitfield alignment flags |
| `Nullable[A]` | `A \| Null` | `sge.utils` | Nullable wrapper |

## Proposed: Pixel Dimensions

**Type:** `opaque type Pixels = Int`
**Package:** `sge.graphics` (or `sge.utils`)
**Priority:** Medium — large surface area (~28 files), but low confusion risk in practice

Single type for all pixel coordinates and dimensions. Don't split into
ScreenX/ScreenY/PixelWidth — too granular, same underlying unit.

### Affected Files (28)

#### Core API (4 files)
| File | Methods/Params |
|------|---------------|
| `Graphics.scala` | `getWidth`, `getHeight`, `getBackBufferWidth/Height`, `getSafeInset*`, `setWindowedMode(width, height)`, `newCursor(xHotspot, yHotspot)` |
| `ApplicationListener.scala` | `resize(width, height)` |
| `Screen.scala` | `resize(width, height)` |
| `InputProcessor.scala` | `touchDown/Up/Cancelled/Dragged(screenX, screenY)`, `mouseMoved(screenX, screenY)` |

#### Input (3 files)
| File | Methods/Params |
|------|---------------|
| `Input.scala` | `getX`, `getY`, `getDeltaX`, `getDeltaY`, `setCursorPosition(x, y)` |
| `input/RemoteInput.scala` | `TouchEvent.x`, `TouchEvent.y` |
| `input/GestureDetector.scala` | `touchDown/Dragged/Up(x, y)` (Int params) |

#### GL Interface (1 file)
| File | Methods/Params |
|------|---------------|
| `graphics/GL20.scala` | `glViewport`, `glScissor`, `glReadPixels`, `glTexImage2D`, `glTexSubImage2D`, `glCopyTexImage2D`, `glCompressedTexImage2D`, `glRenderbufferStorage` (width/height params) |

#### Graphics — Images & Textures (5 files)
| File | Methods/Params |
|------|---------------|
| `graphics/Pixmap.scala` | `getWidth`, `getHeight`, `drawPixmap(x, y)`, `getPixel(x, y)` |
| `graphics/Texture.scala` | `getWidth`, `getHeight`, `draw(pixmap, x, y)` |
| `graphics/g2d/TextureRegion.scala` | `setRegion(x, y, width, height)`, `regionWidth/Height`, `getRegionX/Y` |
| `graphics/g2d/PixmapPacker.scala` | `pageWidth`, `pageHeight` |
| `graphics/glutils/HdpiUtils.scala` | `glScissor`, `glViewport`, `toLogical*`, `toBackBuffer*` |

#### Framebuffer (2 files)
| File | Methods/Params |
|------|---------------|
| `graphics/glutils/FrameBuffer.scala` | `getWidth`, `getHeight`, `end(x, y, width, height)` |
| `graphics/glutils/GLFrameBuffer.scala` | `getWidth`, `getHeight`, builder `width`/`height` |

#### Camera & Viewport (4 files)
| File | Methods/Params |
|------|---------------|
| `graphics/Camera.scala` | `viewportWidth`, `viewportHeight` (Float — may need separate `ViewportSize = Float`) |
| `graphics/OrthographicCamera.scala` | constructor `viewportWidth/Height`, `setToOrtho` |
| `graphics/PerspectiveCamera.scala` | constructor `viewportWidth/Height` |
| `utils/viewport/Viewport.scala` | `update(screenWidth, screenHeight)`, `getScreenX/Y/Width/Height` |

#### Rendering (2 files)
| File | Methods/Params |
|------|---------------|
| `graphics/g2d/SpriteBatch.scala` | viewport setup (internal) |
| `graphics/glutils/ShapeRenderer.scala` | viewport setup (internal) |

#### Text (2 files)
| File | Methods/Params |
|------|---------------|
| `graphics/g2d/BitmapFont.scala` | `draw(x, y)`, `draw(targetWidth)` (Float positions) |
| `graphics/g2d/GlyphLayout.scala` | `setText(targetWidth)` (Float) |

#### Scene2D (2 files)
| File | Methods/Params |
|------|---------------|
| `scenes/scene2d/Stage.scala` | viewport setup |
| `scenes/scene2d/ui/Table.scala` | layout calculations |

#### Input Event Queue (1 file)
| File | Methods/Params |
|------|---------------|
| `InputEventQueue.scala` | `touchDown/Up/Dragged(screenX, screenY)`, `mouseMoved(screenX, screenY)` |

#### Utilities (1 file)
| File | Methods/Params |
|------|---------------|
| `utils/ScreenUtils.scala` | `getFrameBufferPixels` params |

### Implementation Notes

- Camera uses `Float` viewport dimensions (world units, not pixels) — may warrant
  separate `ViewportSize = Float` or just skip Camera floats
- BitmapFont/GlyphLayout positions are Float (world coordinates) — likely excluded
- GL20 pixel params are the hardest to change (affects backend implementations)

---

## Proposed: Time Types

**Existing:** `opaque type Seconds = Float` in `sge.utils`
**New:** `opaque type Millis = Long`, `opaque type Nanos = Long`
**Package:** `sge.utils`
**Priority:** High — prevents unit confusion (seconds vs millis vs nanos)

### Affected Files — Seconds (15)

#### Actions System (5 files)
| File | Params |
|------|--------|
| `scenes/scene2d/actions/TemporalAction.scala` | `duration: Float`, `time: Float`, `act(delta)` |
| `scenes/scene2d/actions/FloatAction.scala` | constructor `duration: Float` |
| `scenes/scene2d/actions/IntAction.scala` | constructor `duration: Float` |
| `scenes/scene2d/actions/DelayAction.scala` | `duration: Float`, `time: Float` |
| `scenes/scene2d/actions/Actions.scala` | ~13 factory methods with `duration: Float` |

#### Particles (4 files)
| File | Params |
|------|--------|
| `graphics/g3d/particles/ParticleController.scala` | `deltaTime`, `deltaTimeSqr`, `update(deltaTime)`, `setTimeStep` |
| `graphics/g3d/particles/ParticleEffect.scala` | `update(deltaTime)` |
| `graphics/g3d/particles/ParticleSystem.scala` | `update(deltaTime)`, `updateAndDraw(deltaTime)` |
| `graphics/g3d/particles/emitters/RegularEmitter.scala` | `duration`, `delay` vars |

#### Animation (2 files)
| File | Params |
|------|--------|
| `graphics/g3d/utils/AnimationController.scala` | `update(delta)`, `AnimationDesc.time/offset/duration` |
| `graphics/g3d/utils/BaseAnimationController.scala` | `apply(animation, time, weight)` |

#### Input & UI (4 files)
| File | Params |
|------|--------|
| `input/GestureDetector.scala` | `tapCountInterval`, `longPressDuration`, `maxFlingDelay` |
| `scenes/scene2d/ui/ProgressBar.scala` | `animateDuration`, `animateTime` |
| `scenes/scene2d/ui/TooltipManager.scala` | `initialTime`, `subsequentTime`, `resetTime` |
| `utils/PerformanceCounter.scala` | `tick(delta)` |

### Affected Files — Millis / Nanos (4)

| File | Params | Type |
|------|--------|------|
| `utils/TimeUtils.scala` | `nanoTime()`, `millis()`, conversion methods | Both |
| `utils/Timer.scala` | `executeTimeMillis`, `intervalMillis`, `delay(delayMillis)` | Millis |
| `input/GestureDetector.scala` | `touchDownTime`, `tapCountIntervalNanos` (internal) | Nanos |
| `InputEventQueue.scala` | `time: Long` params on all event methods | Nanos |

### Also affected (lower priority)
| File | Params |
|------|--------|
| `utils/PerformanceCounters.scala` | `tick(deltaTime)` |
| `graphics/g2d/ParticleEmitter.scala` | timing values in numeric value objects |
| `maps/tiled/tiles/AnimatedTiledMapTile.scala` | `interval`, `loopDuration`, `lastTiledMapRenderTime` |
| `graphics/g3d/utils/FirstPersonCameraController.scala` | `update(deltaTime)` (if present) |

---

## Proposed: GL Enum Opaque Types

**Priority:** Low-Medium — very large surface area (4 GL traits + 47 consumers),
high impact but complex cross-cutting change affecting backend implementations.
Should be done incrementally, one opaque type at a time.

### Proposed Types (14)

| Opaque Type | Underlying | Constants | Consumer Count |
|-------------|-----------|-----------|---------------|
| `TextureTarget` | `Int` | GL_TEXTURE_2D, GL_TEXTURE_3D, GL_TEXTURE_CUBE_MAP, etc. | 12 |
| `BlendFactor` | `Int` | GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO, etc. | 8 |
| `BlendEquation` | `Int` | GL_FUNC_ADD, GL_FUNC_SUBTRACT, etc. | 4 |
| `CompareFunc` | `Int` | GL_NEVER, GL_LESS, GL_EQUAL, GL_LEQUAL, etc. | 6 |
| `StencilOp` | `Int` | GL_KEEP, GL_REPLACE, GL_INCR, etc. | 2 |
| `PrimitiveMode` | `Int` | GL_POINTS, GL_LINES, GL_TRIANGLES, etc. | 5 |
| `BufferTarget` | `Int` | GL_ARRAY_BUFFER, GL_ELEMENT_ARRAY_BUFFER, etc. | 9 |
| `BufferUsage` | `Int` | GL_STATIC_DRAW, GL_DYNAMIC_DRAW, etc. | 6 |
| `ShaderType` | `Int` | GL_VERTEX_SHADER, GL_FRAGMENT_SHADER, etc. | 1 |
| `PixelFormat` | `Int` | GL_RGBA, GL_RGB, GL_ALPHA, etc. | 10 |
| `DataType` | `Int` | GL_UNSIGNED_BYTE, GL_FLOAT, GL_SHORT, etc. | 10 |
| `ClearMask` | `Int` | GL_COLOR_BUFFER_BIT, GL_DEPTH_BUFFER_BIT (bitfield, needs `\|`) | 5 |
| `CullFace` | `Int` | GL_FRONT, GL_BACK, GL_FRONT_AND_BACK | 4 |
| `EnableCap` | `Int` | GL_BLEND, GL_DEPTH_TEST, GL_CULL_FACE, etc. | 12 |

### GL Trait Files (4)

| File | Impact |
|------|--------|
| `graphics/GL20.scala` | ~150 methods, 309 constants |
| `graphics/GL30.scala` | ~50 methods, 305+ constants |
| `graphics/GL31.scala` | ~30 methods, 173 constants |
| `graphics/GL32.scala` | ~20 methods, 206+ constants |

### GL Consumer Files (47)

**Textures (7):** Texture, GLTexture, Cubemap, Texture3D, TextureArray, Pixmap, DefaultTextureBinder

**Buffers (8):** VertexBufferObject, VertexBufferObjectSubData, VertexBufferObjectWithVAO,
IndexBufferObject, IndexBufferObjectSubData, InstanceBufferObject, InstanceBufferObjectSubData,
VertexData

**Framebuffers (3):** GLFrameBuffer, FrameBufferCubemap, GLOnlyTextureData

**Texture Data (4):** ETC1TextureData, FloatTextureData, FacedCubemapData, KTXTextureData

**Shaders (2):** ShaderProgram, DepthShader

**Rendering (4):** SpriteBatch, SpriteCache, PolygonSpriteBatch, Mesh

**G3D (5):** RenderContext, Decal, DecalMaterial, CameraGroupStrategy, SimpleOrthoGroupStrategy

**Particles (1):** PointSpriteParticleBatch

**Utilities (5):** HdpiUtils, ScreenUtils, Camera, Viewport, InstanceData

**Scene2D (2):** Stage, ScissorStack

**Maps (1):** OrthoCachedTiledMapRenderer

**Profiling (4):** GL20Interceptor, GL30Interceptor, GL31Interceptor, GL32Interceptor

### Implementation Strategy

1. Start with least-disruptive types: `ShaderType` (1 consumer), `StencilOp` (2 consumers)
2. Then medium-impact: `CullFace`, `BlendEquation`, `PrimitiveMode`
3. Then high-impact: `BlendFactor`, `EnableCap`, `CompareFunc`
4. Finally most-pervasive: `TextureTarget`, `PixelFormat`, `DataType`, `BufferTarget`
5. `ClearMask` needs special handling (bitfield OR operations)

---

## Proposed: Already Tracked Elsewhere

These opaque types are tracked in `memory/audit-progress.md` under Planned Improvements:

| Opaque Type | Tracking |
|-------------|---------|
| `Key` (Input.Keys) | audit-progress.md — 11 files |
| `Button` (Input.Buttons) | audit-progress.md — 11 files |
| `Red`/`Green`/`Blue`/`Alpha` (Color) | audit-progress.md — 8 files |
