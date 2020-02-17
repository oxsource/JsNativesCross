package pizzk.android.process.jscross

import android.content.Context
import android.util.Log
import android.widget.Toast

class RunTimeLogger(private val name: String) {
    companion object {
        private const val TAG: String = "RunTimeCounter"
    }

    private var stamp: Long = 0

    fun start() {
        stamp = System.currentTimeMillis()
    }

    fun stop(context: Context) {
        val ms: Long = System.currentTimeMillis()
        val second: Float = (ms - stamp) / 1000.0f
        val value: String = String.format("%.2f", second)
        stamp = 0
        val msg = "$name run: $value sec. "
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        Log.d(TAG, msg)
    }
}