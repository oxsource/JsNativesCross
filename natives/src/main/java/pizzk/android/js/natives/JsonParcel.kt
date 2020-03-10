package pizzk.android.js.natives

interface JsonParcel {
    fun string(value: Any): String

    fun <T> parse(value: String, clazz: Class<T>): T?
}