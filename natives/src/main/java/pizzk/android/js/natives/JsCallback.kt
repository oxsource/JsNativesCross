package pizzk.android.js.natives

/**
 * JS2Native Callback
 */
class JsCallback(private val jsNatives: JsNatives, private val path: String) {

    fun call(value: Any) {
        jsNatives.js(path, value)
    }

    fun failure(msg: String = "failed") {
        val maps: MutableMap<String, Any> = mutableMapOf()
        maps["success"] = false
        maps["msg"] = msg
        call(maps)
    }

    fun success(value: Any) {
        val maps: MutableMap<String, Any> = mutableMapOf()
        maps["success"] = true
        maps["data"] = value
        call(maps)
    }
}