package pizzk.android.process.jscross.impl

import android.content.Context
import android.widget.Toast
import pizzk.android.js.natives.JsFunction
import pizzk.android.js.natives.JsInvoker
import java.lang.ref.WeakReference

/**
 * JS原生Alert
 */
class JSAlert(context: Context) {
    companion object {
        const val NAME = "Alert"
    }

    private val refContext: WeakReference<Context> = WeakReference(context)

    @JsFunction(name = "showToast")
    fun showToast(msg: String) {
        val context: Context = refContext.get() ?: return
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}