// SGE — Robolectric harness test for the Android impl layer
//
// Exercises the real Android backend impl classes (the ones in
// sge-jvm-platform/android/src/main/scala-android) on a plain JVM via
// Robolectric — no emulator, no Android SDK. This proves the harness can
// drive the *impl layer*, not merely bare android.* framework classes.
//
// The impl sources are compiled into this module (in Scala) against the
// `android-all-instrumented` framework jar (see build.sbt: the
// `sge-android-robolectric` module). Impl classes that subclass an
// instrumented android View/Window class (GLSurfaceView, PopupWindow,
// AutoCompleteTextView, WallpaperService) are excluded from this module's
// source set because Robolectric's static framework jar leaves the synthetic
// ShadowedObject.$$robo$getData() method abstract, which the Scala compiler
// (unlike javac) refuses to leave unimplemented in a subclass. The impl
// classes exercised here (AndroidPreferencesImpl, AndroidClipboardImpl) only
// *call* android APIs, so they compile and run cleanly.
//
// Why Java for the test (not Scala): the SGE impl package is
// `sge.platform.android`, whose leaf segment `android` collides with the
// android framework's root `android` package that android-all-instrumented
// ships. When the Scala 3 compiler reads the impl classfile while compiling a
// Scala test that also references android.* it mis-resolves the impl's parent
// (PreferencesOps) owner and trips an internal assertion
// (`has non-class parent ... object android`). javac resolves the two `android`
// names without that bug, so the harness test is written in Java. The recipe
// runs JUnit4 via junit-interface, which drives Java and Scala JUnit tests
// identically.

package sge.test.robolectric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import sge.platform.android.AndroidPreferencesImpl;
import sge.platform.android.AndroidClipboardImpl;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public final class AndroidImplRobolectricTest {

  private Context application() {
    return RuntimeEnvironment.getApplication();
  }

  // AndroidPreferencesImpl wraps a real SharedPreferences. Drive a full
  // put/flush/get round-trip through the impl's own PreferencesOps methods.
  @Test
  public void preferencesImplRoundTrip() {
    Context ctx = application();
    SharedPreferences prefs = ctx.getSharedPreferences("sge-robolectric-test", Context.MODE_PRIVATE);
    AndroidPreferencesImpl impl = new AndroidPreferencesImpl(prefs);

    impl.putString("greeting", "hello");
    impl.putInteger("answer", 42);
    impl.putBoolean("enabled", true);
    impl.putLong("big", 9000000000L);
    impl.putFloat("ratio", 1.5f);
    impl.flush();

    assertEquals("hello", impl.getString("greeting", "missing"));
    assertEquals(42, impl.getInteger("answer", 0));
    assertTrue(impl.getBoolean("enabled", false));
    assertEquals(9000000000L, impl.getLong("big", 0L));
    assertEquals(1.5f, impl.getFloat("ratio", 0.0f), 0.0001f);

    assertTrue(impl.contains("greeting"));
    assertFalse(impl.contains("absent"));

    impl.remove("greeting");
    impl.flush();
    assertFalse(impl.contains("greeting"));

    impl.clear();
    impl.flush();
    assertFalse(impl.contains("answer"));
  }

  // A fresh impl over the same Context must observe persisted values:
  // proves AndroidPreferencesImpl reads through to the backing store.
  @Test
  public void preferencesImplPersistsAcrossInstances() {
    Context ctx = application();
    SharedPreferences prefs = ctx.getSharedPreferences("sge-robolectric-persist", Context.MODE_PRIVATE);

    AndroidPreferencesImpl writer = new AndroidPreferencesImpl(prefs);
    writer.putString("token", "abc123");
    writer.flush();

    AndroidPreferencesImpl reader = new AndroidPreferencesImpl(prefs);
    assertEquals("abc123", reader.getString("token", "missing"));
  }

  // AndroidClipboardImpl wraps the ClipboardManager system service. Drive a
  // set/get round-trip through the impl's own ClipboardOps methods.
  @Test
  public void clipboardImplRoundTrip() {
    AndroidClipboardImpl impl = new AndroidClipboardImpl(application());

    impl.setContents("clipboard payload");
    assertTrue(impl.hasContents());

    String contents = impl.getContents();
    assertEquals("clipboard payload", contents);
  }
}
