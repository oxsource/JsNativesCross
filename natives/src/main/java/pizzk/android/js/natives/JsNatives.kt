package pizzk.android.js.natives

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.lang.reflect.Method
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class JsNatives {
    companion object {
        private const val TAG: String = "JsInvoker"

        //
        private const val NATIVE_API: String = "_js2native"
        private const val JS_API: String = "_native2js"
        private const val JS_CALLBACK: String = "CallbackQueue"
        private const val PATH_SPLIT_STR = "/"

        //
        private const val ERR_PATH_MISMATCH = "path mismatch."
        private const val ERR_DISCONNECTED = "invoker disconnected."

        //
        const val ERR_PREFIX = "ERROR@"

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
            val params: String = payload?.let(parcel::string) ?: ""
            if (debug) Log.d(TAG, "js(path=$path, params=$params)")
            val script = "javascript:$JS_API('$path', '$params')"
            web.evaluateJavascript(script) { s: String? -> block(s ?: "") }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "js exception(${e.message})")
        }
    }

    fun joinPath(module: String, method: String): String = "$module$PATH_SPLIT_STR$method"

    @JavascriptInterface
    fun invoke(path: String, payload: String, callback: String) {
        val callbackPath: String = joinPath(JS_CALLBACK, callback)
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
            val params: List<Any> = paramsTypes.map { c: Class<*> ->
                if (c == WebView::class.java) return@map web
                if (c == JsCallback::class.java) {
                    return@map JsCallback(this, callbackPath)
                }
                return@map parcel.parse(payload, c)
            }.filterNotNull()
            val jsCallbackUsed = params.find { it.javaClass == JsCallback::class.java } != null
            val isAsync: Boolean = method.getAnnotation(JsAsync::class.java) != null
            val runnable: () -> Unit = runnable@{
                val value: String = try {
                    val value: Any? = method.invoke(module, *params.toTypedArray())
                    if (null == value) "" else parcel.string(value)
                } catch (e: Exception) {
                    Log.e(TAG, "invoke runnable exception(${e.message})")
                    e.printStackTrace()
                    "$ERR_PREFIX${e.message}"
                }
                if (jsCallbackUsed && !value.startsWith(ERR_PREFIX)) return@runnable
                js(callbackPath, value)
            }
            if (isAsync) THREADS.execute(runnable) else web.post(runnable)
        } catch (e: Exception) {
            js(callbackPath, payload = "$ERR_PREFIX${e.message}")
            Log.e(TAG, "invoke exception(${e.message})")
            e.printStackTrace()
        }
    }
}