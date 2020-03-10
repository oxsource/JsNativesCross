package pizzk.android.js.natives

import android.content.Context
import android.util.ArrayMap
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import pizzk.android.js.natives.annotate.JsAsync
import pizzk.android.js.natives.annotate.JsFunction
import pizzk.android.js.natives.annotate.JsInject
import java.lang.reflect.Method
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class JsInvoker(private val view: WebView, private val parcel: JsonParcel) {
    companion object {
        private const val TAG: String = "JsInvoker"
        //
        private const val NATIVE_API: String = "_js2native"
        private const val JS_API: String = "_native2js"
        private const val JS_CALLBACK: String = "CallbackQueue"
        private const val PATH_SPLIT_STR = "/"
        //
        private const val ERR_PATH_MISMATCH = "path mismatch."
        private const val ERR_PARAM_TYPE = "param type error."
        private const val ERR_DISCONNECTED = "invoker disconnected."
        //thread pool
        private val THREADS: ExecutorService by lazy {
            val min = 1
            val max = 5
            val keepSec = 60L
            val queue = SynchronousQueue<Runnable>()
            return@lazy ThreadPoolExecutor(min, max, keepSec, TimeUnit.SECONDS, queue)
        }
    }

    private var connected: Boolean = false
    private var hooks: MutableMap<String, Any> = ArrayMap()
    private var provider: ((String) -> Any?)? = null
    private var debug: Boolean = BuildConfig.DEBUG

    /**
     * open duplex channel
     */
    fun open(provider: (String) -> Any?): JsInvoker {
        if (connected) return this
        hooks.clear()
        this.provider = provider
        view.addJavascriptInterface(this, NATIVE_API)
        connected = true
        return this
    }

    /**
     * close duplex channel
     */
    fun close() {
        if (!connected) return
        view.removeJavascriptInterface(NATIVE_API)
        this.provider = null
        connected = false
        hooks.forEach { scanInject(it) }
        hooks.clear()
    }

    /**
     * native invoke javascript
     */
    fun js(path: String, payload: Any?, block: (String) -> Unit = {}) {
        try {
            if (!connected) throw Exception(ERR_DISCONNECTED)
            if (Thread.currentThread() != view.handler.looper.thread) {
                view.post { js(path, payload, block) }
                return
            }
            val params: String = payload?.let(parcel::string) ?: ""
            if (debug) Log.d(TAG, "js(path=$path, params=$params)")
            val script = "javascript:$JS_API('$path', '$params')"
            view.evaluateJavascript(script) { s: String? -> block(s ?: "") }
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
            if (!connected) throw Exception(ERR_DISCONNECTED)
            val size = 2
            val paths: List<String> = path.split(PATH_SPLIT_STR, limit = size)
            if (paths.size != size) throw Exception(ERR_PATH_MISMATCH)
            val moduleKey: String = paths[0]
            val methodKey: String = paths[1]
            val module: Any = findModule(moduleKey) ?: throw Exception(ERR_PATH_MISMATCH)
            //
            val clazz: Class<*> = module.javaClass
            val method: Method = findMethod(clazz, methodKey) ?: throw Exception(ERR_PATH_MISMATCH)
            val paramsTypes: Array<Class<*>> = method.parameterTypes
            if (paramsTypes.size != 1) throw Exception(ERR_PARAM_TYPE)
            //
            val params: Any = parcel.parse(payload, paramsTypes[0]) ?: return
            val isAsync: Boolean = method.getAnnotation(JsAsync::class.java) != null
            val runnable: () -> Unit = {
                val value: String = try {
                    val value: Any = method.invoke(module, params)
                    parcel.string(value)
                } catch (e: Exception) {
                    Log.e(TAG, "invoke runnable exception(${e.message})")
                    e.printStackTrace()
                    ""
                }
                js(callbackPath, value)
            }
            if (!connected) throw Exception(ERR_DISCONNECTED)
            if (isAsync) THREADS.execute(runnable) else view.post(runnable)
        } catch (e: Exception) {
            js(callbackPath, payload = "")
            Log.e(TAG, "invoke exception(${e.message})")
            e.printStackTrace()
        }
    }

    private fun findModule(name: String): Any? {
        val cache: Any? = hooks[name]
        if (null != cache) return cache
        val provide: (String) -> Any? = this.provider ?: return null
        val obj: Any = provide(name) ?: return null
        hooks[name] = obj
        scanInject(obj)
        return obj
    }

    private fun findMethod(clazz: Class<*>, name: String): Method? {
        val methods: Array<Method> = clazz.methods ?: return null
        val annClazz: Class<JsFunction> = JsFunction::class.java
        return methods.find { m: Method ->
            val annotate: JsFunction = m.getAnnotation(annClazz) ?: return@find false
            return@find annotate.name == name
        }
    }

    private fun scanInject(obj: Any) {
        val clazz: Class<Any> = obj.javaClass
        val methods: Array<Method> = clazz.methods ?: return
        methods.forEach { method: Method ->
            method.getAnnotation(JsInject::class.java) ?: return@forEach
            val paramsTypes: Array<Class<*>> = method.parameterTypes
            if (paramsTypes.size != 1) return@forEach
            try {
                when (paramsTypes[0]) {
                    Context::class.java -> method.invoke(obj, if (connected) view.context else null)
                    else -> return@forEach
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}