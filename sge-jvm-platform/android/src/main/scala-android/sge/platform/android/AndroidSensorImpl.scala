// SGE — Android sensor implementation
//
// Uses android.hardware.SensorManager for accelerometer, gyroscope,
// compass, and rotation vector sensor access.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.DefaultAndroidInput (sensor part)
//   Renames: DefaultAndroidInput sensors → AndroidSensorImpl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.content.Context
import _root_.android.hardware.{ Sensor, SensorEvent, SensorEventListener, SensorManager }
import _root_.android.view.{ Surface, WindowManager }

class AndroidSensorImpl(context: Context, windowManager: WindowManager) extends SensorOps with SensorEventListener {

  private val sensorManager: SensorManager =
    context.getSystemService(Context.SENSOR_SERVICE).asInstanceOf[SensorManager]

  // Sensor state
  private var _accelX: Float = 0f
  private var _accelY: Float = 0f
  private var _accelZ: Float = 0f

  private var _gyroX: Float = 0f
  private var _gyroY: Float = 0f
  private var _gyroZ: Float = 0f

  private var _azimuth: Float = 0f
  private var _pitch:   Float = 0f
  private var _roll:    Float = 0f

  private val _rotationMatrix: Array[Float] = new Array[Float](16)
  private val _orientation:    Array[Float] = new Array[Float](3)
  private val _magneticField:  Array[Float] = new Array[Float](3)
  private val _gravity:        Array[Float] = new Array[Float](3)
  private val _R:              Array[Float] = new Array[Float](9)
  private val _I:              Array[Float] = new Array[Float](9)

  private var _hasAccel:    Boolean = false
  private var _hasGyro:     Boolean = false
  private var _hasCompass:  Boolean = false
  private var _hasRotation: Boolean = false

  private var compassRegistered:       Boolean = false
  private var accelerometerRegistered: Boolean = false

  // Detect native orientation
  val nativeOrientation: Int = {
    val rotation = windowManager.getDefaultDisplay.getRotation
    val metrics  = new _root_.android.util.DisplayMetrics()
    windowManager.getDefaultDisplay.getMetrics(metrics)
    if (
      (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
      metrics.widthPixels >= metrics.heightPixels
    ) {
      0 // landscape
    } else if (
      (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
      metrics.widthPixels <= metrics.heightPixels
    ) {
      0 // landscape
    } else {
      1 // portrait
    }
  }

  override def accelerometerX: Float = _accelX
  override def accelerometerY: Float = _accelY
  override def accelerometerZ: Float = _accelZ

  override def gyroscopeX: Float = _gyroX
  override def gyroscopeY: Float = _gyroY
  override def gyroscopeZ: Float = _gyroZ

  override def azimuth: Float = _azimuth
  override def pitch:   Float = _pitch
  override def roll:    Float = _roll

  override def rotationMatrix: Array[Float] = _rotationMatrix

  override def hasAccelerometer:  Boolean = _hasAccel
  override def hasGyroscope:      Boolean = _hasGyro
  override def hasCompass:        Boolean = _hasCompass
  override def hasRotationVector: Boolean = _hasRotation

  override def registerListeners(config: AndroidConfigOps): Unit = {
    if (config.useAccelerometer) {
      val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      if (accelSensor != null) {
        _hasAccel = true
        accelerometerRegistered = true
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
      }
    }
    if (config.useGyroscope) {
      val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
      if (gyroSensor != null) {
        _hasGyro = true
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
      }
    }
    if (config.useCompass) {
      val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
      if (magSensor != null) {
        _hasCompass = true
        compassRegistered = true
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_GAME)
        // Compass needs accelerometer too
        if (!accelerometerRegistered) {
          val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
          if (accelSensor != null) {
            _hasAccel = true
            accelerometerRegistered = true
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
          }
        }
      }
    }
    // Rotation vector sensor
    val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    if (rotSensor != null) {
      _hasRotation = true
      sensorManager.registerListener(this, rotSensor, SensorManager.SENSOR_DELAY_GAME)
    }
  }

  override def unregisterListeners(): Unit = {
    sensorManager.unregisterListener(this)
    accelerometerRegistered = false
    compassRegistered = false
  }

  override def onSensorChanged(event: SensorEvent): Unit =
    event.sensor.getType match {
      case Sensor.TYPE_ACCELEROMETER =>
        if (nativeOrientation == 0) {
          // Landscape native: swap x/y
          _accelX = event.values(1)
          _accelY = -event.values(0)
          _accelZ = event.values(2)
        } else {
          _accelX = event.values(0)
          _accelY = event.values(1)
          _accelZ = event.values(2)
        }
        if (compassRegistered) {
          System.arraycopy(event.values, 0, _gravity, 0, 3)
        }
      case Sensor.TYPE_MAGNETIC_FIELD =>
        System.arraycopy(event.values, 0, _magneticField, 0, 3)
        SensorManager.getRotationMatrix(_R, _I, _gravity, _magneticField)
        SensorManager.getOrientation(_R, _orientation)
        _azimuth = Math.toDegrees(_orientation(0).toDouble).toFloat
        _pitch = Math.toDegrees(_orientation(1).toDouble).toFloat
        _roll = Math.toDegrees(_orientation(2).toDouble).toFloat
      case Sensor.TYPE_GYROSCOPE =>
        if (nativeOrientation == 0) {
          _gyroX = event.values(1)
          _gyroY = -event.values(0)
          _gyroZ = event.values(2)
        } else {
          _gyroX = event.values(0)
          _gyroY = event.values(1)
          _gyroZ = event.values(2)
        }
      case Sensor.TYPE_ROTATION_VECTOR =>
        SensorManager.getRotationMatrixFromVector(_rotationMatrix, event.values)
      case _ => ()
    }

  override def onAccuracyChanged(sensor: Sensor, accuracy: Int): Unit = ()
}
