package sge

import scala.annotation.implicitNotFound

/** INJECTED SCALA (`Substitutions.inject`) — the CONTEXT TYPE the globals policy threads. */
@implicitNotFound(
  "No given `sge.Sge` is in scope. `Sge` is this application's context — application, graphics, " +
    "audio, files, input, net — passed explicitly through `(using sge.Sge)`; it replaces libGDX's " +
    "global `Gdx.*` static fields. Add a `(using sge.Sge)` clause to the enclosing class " +
    "constructor or method and propagate the one your `ApplicationListener` was handed."
)
final case class Sge(
  application: sge.Application,
  graphics: sge.Graphics,
  audio: sge.Audio,
  files: sge.Files,
  input: sge.Input,
  net: sge.Net,
)

object Sge {

  /** Sugar for `summon[Sge]`, so a service reads `sge.Sge().graphics`. The emitted code does NOT
    * use it — `reader = "summon"` is what this port configures, because `summon` needs no import
    * and no companion — but a consumer writing against the port does, and the reference hand port
    * declares exactly this. */
  inline def apply()(using s: Sge): Sge = s
}
