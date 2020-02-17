package pizzk.android.js.natives

import org.json.JSONObject

/**
 * Javascript and native call each other via JSRequest:
 * 1、the path of callee include module and method
 * 2、specially, the payload must be a pure text or a formatted json text
 */
class JsRequest(var module: String = "", var method: String = "", var payload: String = "") {
    companion object {
        private const val KEY_MODULE: String = "module"
        private const val KEY_METHOD: String = "method"
        private const val KEY_PAYLOAD: String = "payload"

        fun parse(json: String): JsRequest {
            val jbt = JSONObject(json)
            val fallbackText = ""
            val module: String = jbt.optString(KEY_MODULE, fallbackText)
            val method: String = jbt.optString(KEY_METHOD, fallbackText)
            val payload: String = jbt.optString(KEY_PAYLOAD, fallbackText)
            return JsRequest(module, method, payload)
        }
    }

    fun json(): String {
        val json = JSONObject()
        json.put(KEY_MODULE, module)
        json.put(KEY_METHOD, method)
        json.put(KEY_PAYLOAD, payload)
        return json.toString(2)
    }
}