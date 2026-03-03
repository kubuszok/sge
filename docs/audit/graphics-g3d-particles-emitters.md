# Audit: sge.graphics.g3d.particles.emitters

Audited: 2/2 files | Pass: 2 | Minor: 0 | Major: 0
Last updated: 2026-03-03

---

### Emitter.scala -- pass
All 10 public methods ported: `init`, `end`, `isComplete`, `getMinParticleCount`,
`setMinParticleCount`, `getMaxParticleCount`, `setMaxParticleCount`, `setParticleCount`,
`set`, `copy` (abstract from parent). Constructor with copy parameter ported.
`Json.Serializable` (`write`/`read`) not implemented (JSON serialization deferred) -- this is
a header-only omission since `Emitter` is abstract and subclass `RegularEmitter` would need
its own serialization. Extends `ParticleControllerComponent` which maps `Disposable` to
`AutoCloseable`.

### RegularEmitter.scala -- pass
All 19 public methods ported: `allocateChannels`, `start`, `init`, `activateParticles`,
`update`, `getLife`, `getEmission`, `getDuration`, `getDelay`, `getLifeOffset`,
`isContinuous`, `setContinuous`, `getEmissionMode`, `setEmissionMode`, `isComplete`,
`getPercentComplete`, `set(RegularEmitter)`, `copy`. Private: `addParticles`.
`EmissionMode` inner enum correctly mapped to Scala 3 enum in companion object with all 3
values: `Enabled`, `EnabledUntilCycleEnd`, `Disabled`.
`lifeChannel.data` references correctly use `lifeChannel.floatData` (Scala `FloatChannel` rename).
Java `continue` in particle update loop restructured as if/else without `continue`.
Java `return` in `addParticles` restructured with if/else (no `return`).
Java compound assignment-in-condition (`(lifeChannel.data[k] -= delta) <= 0`) separated into
assignment and check. `Json.Serializable` (`write`/`read`) not implemented (JSON serialization
deferred).
