# Bugs and Ambiguities

Last updated: 2026-03-08

## Critical Bugs from Audit

All 13 critical bugs identified during the [audit process](../audit/README.md) have been resolved.

| # | Location | Description | Resolution |
|---|----------|-------------|------------|
| 1 | `math.collision.OrientedBoundingBox.update()` | axes via `mul(transform)` includes translation | Verified identical to LibGDX source — not a bug |
| 2 | `graphics.Camera.rotate(Matrix4)` | uses `mul()` instead of `rot()` | Fixed — uses `rot()` correctly |
| 3 | `math.CumulativeDistribution.ensureCapacity()` | values assignment commented out | Fixed — assignment restored |
| 4 | `utils.compression.lzma.Encoder.getSubCoder` | operator precedence differs from Java | Removed — compression package deleted (dead code) |
| 5 | `utils.compression.lz.BinTree.normalizeLinks` | compares against constant 0 | Removed — compression package deleted (dead code) |
| 6 | `graphics.g2d.DistanceFieldFont` | vertex shader missing v_texCoords | Fixed — shader string corrected |
| 7 | `maps.tiled.BaseTiledMapLoader.loadProjectFile` | never calls projectClassMembers.add() | Fixed — add() call restored |
| 8 | `graphics.g3d.loader.G3dModelLoader.parseMeshes` | reads mesh ID instead of meshPart ID | Fixed — reads correct ID |
| 9 | `graphics.Pixmap` | drawing methods are stubs | Fixed — all methods implemented |
| 10 | `graphics.Mesh` | missing calculateRadius, scale, transform | Fixed — all methods added |
| 11 | `graphics.glutils.ShapeRenderer` | ~25+ method overloads missing | False positive — all methods verified present |
| 12 | `graphics.glutils.MipMapGenerator` | public methods are stubs | Fixed — implementation completed |
| 13 | `graphics.glutils.ETC1TextureData` | consumeCustomData commented out | Fixed — implementation completed |

## Ambiguities

No open ambiguities at this time. All design decisions are documented in:
- [Conversion rules](../contributing/conversion-rules.md)
- [Architecture docs](../architecture/)
- [CLAUDE.md](../../CLAUDE.md) (project conventions)
