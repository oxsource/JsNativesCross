package pizzk.android.js.natives

import android.webkit.JavascriptInterface

/**
 * JavaScript Native Interface:
 * 1、javascript invoke native by window.[name].invoke(json)
 * 2、json format must match [JsRequest]
 */
class JsInvoker(val name: String, val provider: JsProvider) {
    interface Hook {
        fun name(): String

        fun dispatch(method: String, payload: String, result: JsResult): Boolean
    }

    @JavascriptInterface
    fun invoke(json: String): String {
        val result = JsResult()
        try {
            val request: JsRequest = JsRequest.parse(json)
            val name: String = request.module
            val hook: Hook = provider.hooks()[name] ?: throw Exception("NoSuchNativeModule: $name")
            result.success()
            val consumed: Boolean = hook.dispatch(request.method, request.payload, result)
            if (!consumed) throw Exception("NoSuchNativeMethod: ${request.method}@$name")
        } catch (e: Exception) {
            e.printStackTrace()
            result.failure(msg = e.message ?: "")
        }
        return result.json()
    }
}