package demos.tileworld

import demos.shared.AndroidLauncherActivity

class AndroidMain extends AndroidLauncherActivity {
  override def scene = new TileWorldGame()
}
