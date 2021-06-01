package pizzk.android.js.natives

import java.lang.reflect.Method

class JsProvider {

    private val modules: MutableMap<String, Any> = mutableMapOf()

    fun get(name: String): Any? = kotlin.runCatching { modules[name] }.getOrNull()

    fun get(module: Any, name: String): Method? = kotlin.runCatching {
        val clazz = module as? Class<*> ?: return@runCatching null
        val methods: Array<Method> = clazz.declaredMethods ?: return@runCatching null
        val annClazz: Class<JsFunction> = JsFunction::class.java
        return@runCatching methods.find { m: Method ->
            val annotate: JsFunction = m.getAnnotation(annClazz) ?: return@find false
            return@find annotate.name == name
        }
    }.getOrNull()

    fun append(clazz: Class<*>) {
        kotlin.runCatching {
            val annotate = clazz.getAnnotation(JsModule::class.java) ?: return@runCatching
            modules[annotate.name] = clazz.newInstance()
        }
    }
}