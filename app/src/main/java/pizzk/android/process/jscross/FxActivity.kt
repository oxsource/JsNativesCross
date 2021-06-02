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
        sensorHelper.setCallback { azimuth, _, _ ->
            Log.d("FxActivity", "azimuth=$azimuth")
            iv?.rotation = azimuth
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