package pizzk.android.js.natives

import android.webkit.WebView

/**
 * JsNativesCross:
 * 1、receive javascript invoke
 * 2、native invoke javascript
 */
class JsNativeCross(
    private val view: WebView,
    mapper: (requires: List<String>) -> List<JsInvoker.Hook>
) {
    private val intel = JsInvoker(name = "_js2native", provider = JsProvider(mapper))
    private var isLocked: Boolean = false

    /**
     * open duplex channel
     */
    fun open(): JsNativeCross {
        if (isLocked) return this
        val provider: JsProvider = intel.provider
        provider.inject(provider)
        view.addJavascriptInterface(intel, intel.name)
        isLocked = true
        return this
    }

    /**
     * native invoke javascript by request protocol and result callback
     */
    fun invoke(request: JsRequest, block: (JsResult) -> Unit) {
        if (!isLocked) return
        try {
            val json: String = request.json()
            val script = "javascript:_native2js($json)"
            view.evaluateJavascript(script) { result -> block(JsResult.parse(result)) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * close duplex channel
     */
    fun close() {
        if (!isLocked) return
        view.removeJavascriptInterface(intel.name)
        val provider: JsProvider = intel.provider
        provider.hooks().keys.forEach(provider::reject)
        isLocked = false
    }
}