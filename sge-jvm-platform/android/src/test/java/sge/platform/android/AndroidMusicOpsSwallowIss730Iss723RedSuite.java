// SGE — RED suite (wave 2026-07-17-F, territory AA) for ISS-730 clause c5 + ISS-723 clause c11.
//
// AndroidMusicOpsImpl swallows every MediaPlayer failure at SIX sites where the
// original com.badlogic.gdx.backends.android.AndroidMusic logs via Gdx.app.error
// / Gdx.app.log. These reds drive each failure path with a MediaPlayer whose
// relevant method throws, and assert that SOMETHING is logged (not swallowed).
//
// Site map (SGE AndroidMusicOpsImpl.scala  ->  orig AndroidMusic.java):
//   ISS-730 c5 (5 error sites, orig Gdx.app.error):
//     play()        :42-43   -> :99-101  "Error trying to play music"
//     pause()       :52-53   -> :84-86   "Error trying to pause music"
//     playing       :69      -> :71-72   "Error while checking isPlaying"
//     looping       :76      -> :60-61   "Error while checking isLooping"
//     position_=    :114-115 -> :153-154 "Error setting music position"
//   ISS-723 c11 (1 log site, orig Gdx.app.log):
//     dispose()     :138     -> :46-47   "Error while disposing AndroidMusic instance, non-fatal"
//
// LOG TARGET: on Android, Gdx.app.error/log maps to android.util.Log. SGE's
// utils.Log facade itself reflects into android.util.Log on the Android host
// (sge/utils/LogPlatform.scala scalajvm: androidLogClass = "android.util.Log").
// Robolectric's ShadowLog captures android.util.Log, so this ShadowLog assertion
// is robust to EITHER faithful fix mechanism (a direct android.util.Log.e call,
// or the sanctioned utils.Log facade). These reds FAIL today because the catch
// blocks drop the exception and never log — ShadowLog stays empty across the
// driving call.
//
// INJECTION = Mockito (reproducer-verified rationale, 2026-07-17):
//   * A released real MediaPlayer under Robolectric's ShadowMediaPlayer does NOT
//     produce the IllegalStateException these sites catch — empirically start()
//     -> NullPointerException; isPlaying/isLooping/seekTo/release -> no throw
//     (ShadowMediaPlayer's default invalid-state behavior is silent). So state
//     injection cannot exercise the swallow.
//   * Subclassing MediaPlayer to override-and-throw compiles, but breaks sbt/zinc
//     incremental Java analysis (ExceptionInInitializerError in
//     sbt.internal.inc.ClassToAPI while reading inheritance of the instrumented
//     android class) — the SAME instrumented-subclass limitation the harness
//     documents for GLSurfaceView/PopupWindow impls (see AndroidImplRobolectricTest).
//   * Mockito mocks are generated at test RUNTIME (ByteBuddy) and never appear in
//     source, so zinc's incremental analysis never sees an instrumented-class
//     subclass — and doThrow/thenThrow injects the EXACT exception type each site
//     catches. This is the only faithful, headless, zinc-safe injection.
//
// Written in Java/JUnit4 to match the sge-android-robolectric harness precedent
// (AndroidImplRobolectricTest.java): a Scala test in package sge.platform.android
// that also references android.* trips a Scala 3 compiler assertion
// ("has non-class parent ... object android"). See that file's header.
//
// BUILD-WIRING GRANT REQUIRED (build.sbt is orchestrator-locked; ISS-723 c14 =
// sge-jvm-platform-android has ZERO test dirs). To compile+run this suite:
//   (1) Add a Robolectric Test config to sge-jvm-platform-android mirroring
//       sge-android-robolectric (robolectric 4.16.1 + android-all-instrumented
//       Provided + junit + junit-interface + api products on Test classpath +
//       JDK21 fork), OR relocate this suite into sge-test/android-robolectric.
//   (2) Add "org.mockito" % "mockito-core" % <ver> % Test (NOT currently on the
//       harness classpath — reproducer-checked).
// Robolectric itself is verified to RUN in this environment (JDK 21.0.6-graal
// resolved; a probe test executed under RobolectricTestRunner).

package sge.platform.android;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.media.MediaPlayer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import scala.Function1;
import scala.runtime.BoxedUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public final class AndroidMusicOpsSwallowIss730Iss723RedSuite {

  private static final Function1<AndroidMusicOpsImpl, BoxedUnit> NO_DISPOSE = m -> BoxedUnit.UNIT;
  private static final Function1<Runnable, BoxedUnit>            NO_POST    = r -> BoxedUnit.UNIT;

  private AndroidMusicOpsImpl wrap(MediaPlayer player) {
    return new AndroidMusicOpsImpl(NO_DISPOSE, NO_POST, player);
  }

  @Before
  public void resetLog() {
    ShadowLog.clear();
  }

  private void assertLogged(String site) {
    assertFalse(
        site + ": failure path must log (orig AndroidMusic.java logs via Gdx.app.error/log) "
            + "but AndroidMusicOpsImpl swallowed it — ShadowLog is empty",
        ShadowLog.getLogs().isEmpty());
  }

  // ISS-730 c5 — play() :42-43 swallows IllegalStateException from start()
  @Test
  public void playSwallowsStartFailure() {
    MediaPlayer player = mock(MediaPlayer.class);
    doThrow(new IllegalStateException("boom-start")).when(player).start();
    AndroidMusicOpsImpl impl = wrap(player);
    ShadowLog.clear();
    impl.play();
    assertLogged("play/start");
  }

  // ISS-730 c5 — pause() :52-53 swallows IllegalStateException from isPlaying()
  @Test
  public void pauseSwallowsIsPlayingFailure() {
    MediaPlayer player = mock(MediaPlayer.class);
    when(player.isPlaying()).thenThrow(new IllegalStateException("boom-isPlaying"));
    AndroidMusicOpsImpl impl = wrap(player);
    ShadowLog.clear();
    impl.pause();
    assertLogged("pause/isPlaying");
  }

  // ISS-730 c5 — playing :69 swallows IllegalStateException from isPlaying()
  @Test
  public void playingSwallowsIsPlayingFailure() {
    MediaPlayer player = mock(MediaPlayer.class);
    when(player.isPlaying()).thenThrow(new IllegalStateException("boom-isPlaying"));
    AndroidMusicOpsImpl impl = wrap(player);
    ShadowLog.clear();
    impl.playing();
    assertLogged("playing/isPlaying");
  }

  // ISS-730 c5 — looping :76 swallows IllegalStateException from isLooping()
  @Test
  public void loopingSwallowsIsLoopingFailure() {
    MediaPlayer player = mock(MediaPlayer.class);
    when(player.isLooping()).thenThrow(new IllegalStateException("boom-isLooping"));
    AndroidMusicOpsImpl impl = wrap(player);
    ShadowLog.clear();
    impl.looping();
    assertLogged("looping/isLooping");
  }

  // ISS-730 c5 — position_= :114-115 swallows IllegalStateException from seekTo()
  @Test
  public void setPositionSwallowsSeekFailure() {
    MediaPlayer player = mock(MediaPlayer.class);
    doThrow(new IllegalStateException("boom-seek")).when(player).seekTo(anyInt());
    AndroidMusicOpsImpl impl = wrap(player);
    ShadowLog.clear();
    impl.position_$eq(1.0f);
    assertLogged("position_=/seekTo");
  }

  // ISS-723 c11 — dispose() :138 swallows Throwable from release()
  @Test
  public void disposeSwallowsReleaseFailure() {
    MediaPlayer player = mock(MediaPlayer.class);
    doThrow(new RuntimeException("boom-release")).when(player).release();
    AndroidMusicOpsImpl impl = wrap(player);
    ShadowLog.clear();
    impl.dispose();
    assertLogged("dispose/release");
  }
}
