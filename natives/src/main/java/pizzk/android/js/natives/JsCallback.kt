package pizzk.android.js.natives

class JsCallback(private val jsNatives: JsNatives, private val path: String) {

    fun call(value: Any) {
        jsNatives.js(path, value)
    }
}