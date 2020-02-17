package pizzk.android.js.natives

import org.json.JSONObject

/**
 * Javascript and native feedback each other via JSResult:
 * 1、invoke success or failure indicate by success and msg
 * 2、specially, the data must be a pure text or a formatted json text.
 * eg. "{"success":true,"msg":"","data":"{\"name\":\"kitty\",\"age\":10}"}"
 */
class JsResult(var success: Boolean = false, var msg: String = "", var data: String = "") {
    companion object {
        private const val KEY_SUCCESS: String = "success"
        private const val KEY_MSG: String = "msg"
        private const val KEY_DATA: String = "data"

        fun parse(json: String): JsResult {
            return try {
                val jbt = JSONObject(json)
                val fallbackBool = false
                val success: Boolean = jbt.optBoolean(KEY_SUCCESS, fallbackBool)
                val fallbackText = ""
                val msg: String = jbt.optString(KEY_MSG, fallbackText)
                val data: String = jbt.optString(KEY_DATA, fallbackText)
                JsResult(success, msg, data)
            } catch (e: Exception) {
                JsResult(success = false, msg = e.message ?: "")
            }
        }
    }

    fun failure(msg: String, data: String = "") {
        success = false
        this.msg = msg
        this.data = data
    }

    fun success(data: String = "") {
        success = true
        this.msg = ""
        this.data = data
    }

    fun json(): String {
        val json = JSONObject()
        json.put(KEY_SUCCESS, success)
        json.put(KEY_MSG, msg)
        json.put(KEY_DATA, data)
        return json.toString(2)
    }
}