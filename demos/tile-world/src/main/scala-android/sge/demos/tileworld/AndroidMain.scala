package sge
package demos
package tileworld

import sge.demos.shared.AndroidLauncherActivity

class AndroidMain extends AndroidLauncherActivity {
  override def scene = new TileWorldGame()
}
