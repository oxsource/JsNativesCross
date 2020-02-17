package pizzk.android.js.natives

import android.util.ArrayMap
import org.json.JSONArray

/**
 * Javascript invoke Hook Provider
 * call window._natives.require([xx,xx]) in javascript as init requirements where you want use
 */
class JsProvider(private val mapper: (requires: List<String>) -> List<JsInvoker.Hook>) :
    JsInvoker.Hook {
    companion object {
        const val NAME: String = "MODULE_PROVIDER"
        private const val ACTION_INJECT = "inject"
        private const val ACTION_REJECT = "reject"
    }

    override fun name(): String = NAME

    private var hooks: MutableMap<String, JsInvoker.Hook> = ArrayMap()

    override fun dispatch(method: String, payload: String, result: JsResult): Boolean {
        val array = JSONArray(payload)
        val inject: Boolean = when (method) {
            ACTION_INJECT -> true
            ACTION_REJECT -> false
            else -> return false
        }
        val fallback = ""
        val names: MutableList<String> = ArrayList(array.length())
        for (index: Int in 0..array.length()) {
            val e: String = array.optString(index, fallback)
            names.add(e)
        }
        val requires: List<String> = names.filter { e -> e.isNotEmpty() }
        if (inject) mapper(requires).forEach(::inject) else requires.forEach(::reject)
        return true
    }

    fun inject(hook: JsInvoker.Hook) {
        hooks[hook.name()] = hook
    }

    fun reject(name: String) {
        hooks.remove(name)
    }

    fun hooks(): Map<String, JsInvoker.Hook> = hooks
}