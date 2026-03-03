# Design Improvements Over LibGDX

This directory documents intentional design improvements SGE makes over the original
LibGDX API. These are not just mechanical translations but deliberate enhancements
leveraging Scala 3's type system and standard library.

## Categories

| Document | Focus |
|----------|-------|
| [type-safety.md](type-safety.md) | Opaque types, Nullable, AutoCloseable |
| [api-design.md](api-design.md) | Ordering, adapter collapse, collections |
| [bugs-and-ambiguities.md](bugs-and-ambiguities.md) | Discovered bugs and undocumented behavior |
| [opaque-types.md](opaque-types.md) | Roadmap for new opaque types (Pixels, Time, GL enums) |
| [dependencies.md](dependencies.md) | Library replacement candidates (scala-java-time, scribe, Gears) |

## Entry Format

Each improvement entry follows this template:

```
### ID: Short Title

- **LibGDX**: `com.badlogic.gdx.path.ClassName` (line X)
- **SGE**: `sge.path.ClassName` (line Y)
- **Problem**: What's wrong or suboptimal in the original
- **Improvement**: What SGE does differently
- **Status**: documented | implemented | proposed
```

## How to Add New Entries

1. Choose the appropriate category file
2. Use the next ID in sequence (TS-nnn for type safety, AD-nnn for API design, BA-nnn for bugs)
3. Fill in the template
4. Update this README if adding a new category
