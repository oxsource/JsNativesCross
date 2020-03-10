package pizzk.android.process.jscross.impl

import android.util.Log
import pizzk.android.js.natives.annotate.JsFunction
import pizzk.android.js.natives.annotate.JsModule

/**
 * JS原生日志
 */
@JsModule(name = "Console")
class JSConsole {
    companion object {
        private const val TAG_NAME = "JSConsole"
    }

    @JsFunction(name = "i")
    fun info(msg: String) {
        Log.i(TAG_NAME, msg)
    }

    @JsFunction(name = "d")
    fun debug(msg: String) {
        Log.d(TAG_NAME, msg)
    }

    @JsFunction(name = "w")
    fun warn(msg: String) {
        Log.w(TAG_NAME, msg)
    }


    @JsFunction(name = "e")
    fun error(msg: String) {
        Log.e(TAG_NAME, msg)
    }
}