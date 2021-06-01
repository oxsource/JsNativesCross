package pizzk.android.process.jscross.impl

import android.content.Context
import android.webkit.WebView
import android.widget.Toast
import pizzk.android.js.natives.JsFunction
import pizzk.android.js.natives.JsModule

/**
 * JS原生Alert
 */
@JsModule(name = "Alert")
class JSAlert {

    @JsFunction(name = "showToast")
    fun showToast(web: WebView?, msg: String) {
        val context: Context = web?.context ?: return
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}