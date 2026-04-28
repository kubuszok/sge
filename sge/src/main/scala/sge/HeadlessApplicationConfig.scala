/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-headless/.../HeadlessApplicationConfiguration.java
 * Original authors: Jon Renner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: HeadlessApplicationConfiguration -> HeadlessApplicationConfig
 *   Convention: Java mutable POJO -> Scala final case class with defaults
 *   Audited: 2026-03-05
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 29
 * Covenant-baseline-methods: HeadlessApplicationConfig
 * Covenant-source-reference: backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/HeadlessApplicationConfiguration.java
 *   Renames: HeadlessApplicationConfiguration -> HeadlessApplicationConfig
 *   Convention: Java mutable POJO -> Scala final case class with defaults
 *   Audited: 2026-03-05
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 29
 * Covenant-baseline-methods: HeadlessApplicationConfig
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: caf7e6fe10f923ca15bee8ec7ff4911c4fc691cd
 */
package sge

/** Configuration for [[HeadlessApplication]].
  *
  * @param updatesPerSecond
  *   Target frame rate. Use 0 to never sleep; negative to not call render at all. Default is 60.
  * @param preferencesDirectory
  *   Directory for preferences files. Default is ".prefs/".
  * @param maxNetThreads
  *   Maximum threads for network requests. Default is [[Int.MaxValue]].
  */
final case class HeadlessApplicationConfig(
  updatesPerSecond:     Int = 60,
  preferencesDirectory: String = ".prefs/",
  maxNetThreads:        Int = Int.MaxValue
)
