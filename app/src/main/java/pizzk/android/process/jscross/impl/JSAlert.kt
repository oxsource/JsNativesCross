package pizzk.android.process.jscross.impl

import android.content.Context
import android.widget.Toast
import pizzk.android.js.natives.annotate.JsFunction
import pizzk.android.js.natives.annotate.JsInject
import pizzk.android.js.natives.annotate.JsModule
import java.lang.ref.WeakReference

/**
 * JS原生Alert
 */
@JsModule(name = "Alert")
class JSAlert {
    private var refContext: WeakReference<Context>? = null

    @JsInject
    fun setContext(context: Context?) {
        refContext?.clear()
        if (null == context) return
        refContext = WeakReference(context)
    }

    @JsFunction(name = "showToast")
    fun showToast(msg: String) {
        val context: Context = refContext?.get() ?: return
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}