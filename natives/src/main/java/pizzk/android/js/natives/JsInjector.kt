package pizzk.android.js.natives

import android.content.Context
import android.view.View
import pizzk.android.js.natives.annotate.JsInject
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.ArrayList

class JsInjector {
    interface Feature {
        fun consume(obj: Any, method: Method, view: View?): Boolean
    }

    private val injects: MutableList<Pair<Any, Method>> = LinkedList()
    private val features: MutableList<Feature> = ArrayList()

    private fun each(obj: Any, method: Method, view: View?): Boolean {
        try {
            method.getAnnotation(JsInject::class.java) ?: return false
            features.find { feature: Feature -> feature.consume(obj, method, view) } ?: return false
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun inject(obj: Any, view: View) {
        val clazz: Class<Any> = obj.javaClass
        val methods: Array<Method> = clazz.methods ?: return
        methods.forEach { method: Method ->
            val injected: Boolean = each(obj, method, view)
            if (injected) injects.add(Pair(obj, method))
        }
    }

    fun reject() {
        injects.forEach { e: Pair<Any, Method> -> each(e.first, e.second, null) }
        injects.clear()
        features.clear()
    }

    fun addFeature(feature: Feature): JsInjector {
        features.add(feature)
        return this
    }
}

class ContextInjectFeature : JsInjector.Feature {

    override fun consume(obj: Any, method: Method, view: View?): Boolean {
        val paramsTypes: Array<Class<*>> = method.parameterTypes
        if (paramsTypes.size != 1) return false
        if (paramsTypes[0] != Context::class.java) return false
        try {
            method.invoke(obj, view?.context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }
}