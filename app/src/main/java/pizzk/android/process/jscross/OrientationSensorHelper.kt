package pizzk.android.process.jscross

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.WindowManager


//https://developer.android.google.cn/guide/topics/sensors/sensors_position?hl=zh_cn#kotlin
class OrientationSensorHelper : SensorEventListener {
    private var manager: SensorManager? = null
    private var winManager: WindowManager? = null
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)

    //azimuth/pitch/roll
    private val orientationAngles = FloatArray(3)

    private var callback: (Float, Float, Float) -> Unit = { _, _, _ -> }

    fun onCreate(context: Context) {
        manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        winManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    fun onResume() {
        val manager = this.manager ?: return
        manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            manager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            manager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun onPause() {
        manager?.unregisterListener(this)
    }

    fun onDestroy() {
        manager = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        val windowManager = winManager ?: return
        if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(e.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (e.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(e.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
        //updateOrientationAngles
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val values = orientationAngles.map { Math.toDegrees(it.toDouble()).toFloat() }
        callback.invoke(values[0], values[1], values[2])
    }

    override fun onAccuracyChanged(sensor: Sensor?, value: Int) = Unit

    fun setCallback(block: (Float, Float, Float) -> Unit) {
        this.callback = block
    }
}