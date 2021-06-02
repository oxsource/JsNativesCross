package pizzk.android.process.jscross

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class FxActivity : AppCompatActivity() {
    private var iv: ImageView? = null
    private val sensorHelper: OrientationSensorHelper = OrientationSensorHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fx)
        iv = findViewById(R.id.iv)
        sensorHelper.onCreate(context = baseContext)
        val azimuthAdapter = OrientationSensorHelper.ValueAdapter(threshold = 2)
        sensorHelper.setCallback { azimuth, _, _ ->
            val value = azimuthAdapter.opt(azimuth) ?: return@setCallback
            Log.d("FxActivity", "azimuth=$value")
            iv?.rotation = value
        }
    }

    override fun onResume() {
        super.onResume()
        sensorHelper.onResume()
    }

    override fun onPause() {
        sensorHelper.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        sensorHelper.onDestroy()
        super.onDestroy()
    }
}