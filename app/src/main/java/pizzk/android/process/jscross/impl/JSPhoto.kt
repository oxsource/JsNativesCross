package pizzk.android.process.jscross.impl

import android.app.Activity
import android.net.Uri
import android.webkit.WebView
import pizzk.android.js.natives.JsCallback
import pizzk.android.js.natives.JsFunction
import pizzk.android.js.natives.JsModule
import pizzk.android.js.natives.JsNatives
import pizzk.android.process.jscross.R
import pizzk.media.picker.arch.PickControl
import pizzk.media.picker.utils.PickUtils

@JsModule(name = "Photo")
class JSPhoto {

    @JsFunction(name = "take")
    fun take(view: WebView?, params: Map<String, Any>?, callback: JsCallback?) {
        val call = callback ?: return
        val activity: Activity = JsNatives.activity(view) ?: return call.failure()
        val authorities = activity.getString(R.string.file_provider)
        PickControl.authority(authorities)
        val mode: String = params?.get("mode")?.toString() ?: "album"
        val limit: Int = params?.get("limit") as? Int ?: 1
        val action = when (mode) {
            "camera" -> PickControl.ACTION_CAMERA
            else -> PickControl.ACTION_ALBUM
        }
        val takeCallback: (Int, List<Uri>) -> Unit = { _, uris ->
            val values = uris.map { PickUtils.filePath(activity, it.toString()) }
            callback.success(values)
        }
        PickControl.obtain(true)
            .action(action)
            .limit(limit)
            .callback(takeCallback)
            .done(activity)
    }
}