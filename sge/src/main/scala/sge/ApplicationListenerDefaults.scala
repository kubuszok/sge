package sge

trait ApplicationListenerDefaults extends ApplicationListener {
  def create(): Unit  = ()
  def resize(width: sge.Pixels, height: sge.Pixels): Unit = ()
  def render(): Unit  = ()
  def pause(): Unit   = ()
  def resume(): Unit  = ()
  def dispose(): Unit = ()
}
