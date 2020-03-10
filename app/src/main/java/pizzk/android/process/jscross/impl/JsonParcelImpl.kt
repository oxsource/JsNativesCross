package pizzk.android.process.jscross.impl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import pizzk.android.js.natives.JsonParcel
import pizzk.android.js.natives.annotate.JsProvide
import java.text.SimpleDateFormat
import java.util.*

@JsProvide(name = "JsModuleKeys", build = true)
object JsonParcelImpl : JsonParcel {
    private val mapper: ObjectMapper by lazy { ObjectMapper() }
    private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

    init {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    }

    override fun string(value: Any): String {
        if (value is String) return value
        return try {
            mapper.writeValueAsString(value)
        } catch (e: Exception) {
            ""
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> parse(value: String, clazz: Class<T>): T? {
        try {
            if (clazz == String::class.java) return value as? T
            return mapper.readValue(value, clazz)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}