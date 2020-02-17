package pizzk.android.process.jscross.impl

import android.util.Log
import pizzk.android.js.natives.JsInvoker
import pizzk.android.js.natives.JsResult

/**
 * JS原生日志
 */
class JSConsole : JsInvoker.Hook {
    companion object {
        const val NAME = "Console"
        private const val TAG_NAME = "JSConsole"
        private const val INFO = "info"
        private const val DEBUG = "debug"
        private const val WARN = "warn"
        private const val ERROR = "error"
    }

    override fun name(): String = NAME

    override fun dispatch(method: String, payload: String, result: JsResult): Boolean {
        when (method) {
            INFO -> Log.i(TAG_NAME, payload)
            DEBUG -> Log.i(TAG_NAME, payload)
            WARN -> Log.i(TAG_NAME, payload)
            ERROR -> Log.i(TAG_NAME, payload)
            else -> return false
        }
        return true
    }
}