package pizzk.android.js.natives.v2

import com.pizzk.android.jsn.annotation.Provider
import java.util.*

@Provider(name = "NativeProvider", live = true)
class NativeHandle {
    private val invokes: MutableList<Any> = LinkedList()

    fun call(path: String, params: String, callbackId: String) {
    }
}