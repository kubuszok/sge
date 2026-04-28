/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/Influencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Faithful port. No public API differences.
 * - write/read(Json) omitted (Json serialization not ported).
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 24
 * Covenant-baseline-methods: Influencer
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/influencers/Influencer.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: f087ba76cac7ff2cc4ded91efc009f52bf0987c2
 */
package sge
package graphics
package g3d
package particles
package influencers

/** It's a {@link ParticleControllerComponent} which usually modifies one or more properties of the particles(i.e color, scale, graphical representation, velocity, etc...).
  * @author
  *   Inferno
  */
abstract class Influencer extends ParticleControllerComponent {}
