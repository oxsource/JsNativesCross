package pizzk.android.js.natives

/**
 * JS2Native Callback
 */
class JsCallback(private val jsNatives: JsNatives, private val path: String) {

    fun call(value: Any) {
        jsNatives.js(path, value)
    }

    fun failure(msg: String = "call failed.") {
        call(value = "${JsNatives.ERR_PREFIX}$msg")
    }

    fun success(value: Any) {
        val maps: MutableMap<String, Any> = mutableMapOf()
        maps["data"] = value
        call(maps)
    }
}