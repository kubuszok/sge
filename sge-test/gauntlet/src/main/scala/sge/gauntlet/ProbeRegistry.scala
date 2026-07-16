/*
 * SGE Gauntlet — probe registry.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

import sge.gauntlet.probes._

/** The platform-agnostic probe set. Platform launchers append platform-only probes (e.g. the JVM loopback networking probes) before handing the list to the runner. */
object ProbeRegistry {

  /** Shared probes, non-GPU first so a headless run reports its executable probes before the skips. */
  def shared: List[FeatureProbe] = List(
    FilesRoundtripProbe,
    AssetsManagerProbe,
    JsonXmlProbe,
    JbumpCollisionProbe,
    AiStateMachineProbe,
    AudioLifecycleProbe,
    SpriteBatchBlendProbe,
    ShapeRendererProbe,
    FontDrawProbe,
    FboRoundtripProbe,
    Scene2dButtonProbe,
    ModelBatchCubeProbe,
    SoundPlaybackProbe,
    MusicOnCompleteProbe
  )
}
