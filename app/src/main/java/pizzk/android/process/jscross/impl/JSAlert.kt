package pizzk.android.process.jscross.impl

import android.content.Context
import android.widget.Toast
import pizzk.android.js.natives.JsInvoker
import pizzk.android.js.natives.JsResult
import java.lang.ref.WeakReference

/**
 * JS原生Alert
 */
class JSAlert(context: Context) : JsInvoker.Hook {
    companion object {
        const val NAME = "Alert"
        private const val TOAST = "toast"
    }

    private val refContext: WeakReference<Context> = WeakReference(context)

    override fun name(): String = NAME

    override fun dispatch(method: String, payload: String, result: JsResult): Boolean {
        val context: Context = refContext.get() ?: return true
        when (method) {
            TOAST -> {
                Toast.makeText(context, payload, Toast.LENGTH_SHORT).show()
            }
            else -> return false
        }
        return true
    }
}