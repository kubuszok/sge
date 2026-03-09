package sge
package demos
package shaders

import sge.demos.shared.AndroidLauncherActivity

class AndroidMain extends AndroidLauncherActivity {
  override def scene = new ShaderLabGame()
}
