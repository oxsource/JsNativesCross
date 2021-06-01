package pizzk.android.process.jscross.impl

import android.Manifest
import android.app.Activity
import android.provider.ContactsContract
import android.webkit.WebView
import pizzk.android.js.natives.JsCallback
import pizzk.android.js.natives.JsFunction
import pizzk.android.js.natives.JsModule
import pizzk.android.js.natives.JsNatives
import pizzk.android.process.jscross.permission.PermissionHelper


@JsModule(name = "Contract")
class JSContract {

    @JsFunction(name = "simples")
    fun simples(view: WebView?, callback: JsCallback?) {
        val call = callback ?: return
        val activity: Activity = JsNatives.activity(view) ?: return call.failure()
        checkReadContract(activity) { granted: Boolean ->
            if (!granted) return@checkReadContract call.failure(msg = "")
            val projection: Array<String> = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val values: MutableList<SimpleRecord> = mutableListOf()
            activity.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use {
                val idxName: Int =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val idxPhone: Int =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(idxName)
                    val phone = it.getString(idxPhone)
                    if (name.isEmpty() || phone.isEmpty()) continue
                    values.add(SimpleRecord(name, phone))
                }
            }
            call.success(values)
        }
    }

    private fun checkReadContract(activity: Activity, callback: (Boolean) -> Unit) {
        if (PermissionHelper.checkReadContract(activity)) {
            return callback(true)
        }
        val key = "checkReadContract@JSContract"
        PermissionHelper.setCallback(key, object : PermissionHelper.ResultCallback {

            override fun invoke(permission: String?, granted: Boolean) {
                if (Manifest.permission.READ_CONTACTS != permission) return
                PermissionHelper.setCallback(key, null)
                callback(granted)
            }
        })
    }

    data class SimpleRecord(val name: String, val phone: String)
}