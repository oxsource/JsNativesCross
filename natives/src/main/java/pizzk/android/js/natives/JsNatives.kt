package pizzk.android.js.natives

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class JsNatives {
    companion object {
        private const val TAG: String = "JsNatives"

        //
        private const val NATIVE_API: String = "_js2native"
        private const val JS_API: String = "_native2js"
        private const val PATH_SPLIT_STR = "/"

        //
        private const val ERR_PATH_MISMATCH = "path mismatch."
        private const val ERR_DISCONNECTED = "invoker disconnected."

        //thread pool
        private val THREADS: ExecutorService by lazy {
            val min = 1
            val max = 5
            val keepSec = 60L
            val queue = SynchronousQueue<Runnable>()
            return@lazy ThreadPoolExecutor(min, max, keepSec, TimeUnit.SECONDS, queue)
        }

        fun activity(view: WebView?, checkFinished: Boolean = true): Activity? {
            val context: Context = view?.context ?: return null
            val activity: Activity = (context as? ContextWrapper) as? Activity ?: return null
            if (!checkFinished) return activity
            if (activity.isFinishing || activity.isDestroyed) return null
            return activity
        }
    }

    private var view: WebView? = null
    private val provider: JsProvider = JsProvider()
    private var debug: Boolean = BuildConfig.DEBUG
    private var parcel: JsonParcel = JsonParcelImpl

    fun modules(vararg clazz: Class<*>): JsNatives {
        clazz.iterator().forEach(provider::append)
        return this
    }

    /**
     * open duplex channel
     */
    fun active(web: WebView): JsNatives {
        if (null != view) return this
        web.addJavascriptInterface(this, NATIVE_API)
        view = web
        return this
    }

    /**
     * close duplex channel
     */
    fun release() {
        val web = view ?: return
        web.removeJavascriptInterface(NATIVE_API)
    }

    /**
     * native invoke javascript
     */
    fun js(path: String, payload: Any?, block: (String) -> Unit = {}) {
        try {
            val web = view ?: throw Exception(ERR_DISCONNECTED)
            if (Thread.currentThread() != web.handler.looper.thread) {
                web.post { js(path, payload, block) }
                return
            }
            val maps = mutableMapOf<String, Any?>()
            maps["path"] = path
            maps["payload"] = payload
            val params: String = maps.let(parcel::string)
            val script = "javascript:$JS_API('$params')"
            if (debug) Log.d(TAG, script)
            web.evaluateJavascript(script) { s: String? -> block(s ?: "") }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "js exception(${e.message})")
        }
    }

    fun joinPath(module: String, method: String): String = "$module$PATH_SPLIT_STR$method"

    @JavascriptInterface
    fun invoke(path: String, payload: String, callback: String) {
        val jsCallback = JsCallback(this, callback)
        try {
            if (debug) Log.d(TAG, "invoke(path=$path, payload=$payload, callback=$callback)")
            val web = view ?: throw Exception(ERR_DISCONNECTED)
            val size = 2
            val paths: List<String> = path.split(PATH_SPLIT_STR, limit = size)
            if (paths.size != size) throw Exception(ERR_PATH_MISMATCH)
            val kModule: String = paths[0]
            val kMethod: String = paths[1]
            val module: Any = provider.get(kModule) ?: throw Exception(ERR_PATH_MISMATCH)
            //
            val mClazz: Class<*> = module.javaClass
            val method: Method = provider.get(mClazz, kMethod) ?: throw Exception(ERR_PATH_MISMATCH)
            val paramsTypes: Array<Class<*>> = method.parameterTypes ?: emptyArray()
            val params: List<Any> = paramsTypes.map { clazz: Class<*> ->
                if (clazz == WebView::class.java) return@map web
                if (clazz == JsCallback::class.java) return@map jsCallback
                return@map parcel.parse(payload, clazz)
            }.filterNotNull()
            val jsCallbackUsed = params.find { it.javaClass == JsCallback::class.java } != null
            val isAsync: Boolean = method.getAnnotation(JsAsync::class.java) != null
            val runnable: () -> Unit = runnable@{
                val value: String = try {
                    method.invoke(module, *params.toTypedArray())
                    ""
                } catch (e: Exception) {
                    val exp = (e as? InvocationTargetException)?.targetException ?: e
                    Log.e(TAG, "invoke runnable exception(${exp.message})")
                    exp.printStackTrace()
                    "${exp.message}"
                }
                if (jsCallbackUsed && value.isEmpty()) return@runnable
                if (value.isEmpty()) {
                    jsCallback.success("")
                } else {
                    jsCallback.failure(msg = value)
                }
            }
            if (isAsync) THREADS.execute(runnable) else web.post(runnable)
        } catch (e: Exception) {
            jsCallback.failure(msg = "${e.message}")
            Log.e(TAG, "invoke exception(${e.message})")
            e.printStackTrace()
        }
    }
}