package pizzk.android.process.jscross.impl

import android.util.Log
import pizzk.android.js.natives.JsAsync
import pizzk.android.js.natives.JsFunction
import pizzk.android.js.natives.JsModule

@JsModule(name = "Files")
class JSFiles {

    @JsAsync
    @JsFunction(name = "saveFile")
    fun save(map: Map<String, String>): Map<String, String> {
        Thread.sleep(2000)
        val path: String = map["path"] ?: ""
        val content: String = map["content"] ?: ""
        Log.d("JSFiles", "path: $path, content: $content")
        val values: MutableMap<String, String> = HashMap(2)
        values["seconds"] = "2"
        values["name"] = "saveFile success"
        return values
    }
}