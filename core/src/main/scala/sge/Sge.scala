package sge

final case class Sge private[sge] (
  application: Application,
  graphics:    Graphics,
  audio:       Audio,
  files:       Files,
  input:       Input,
  net:         Net
)
object Sge {

  inline def apply()(using sge: Sge): Sge = sge
}
