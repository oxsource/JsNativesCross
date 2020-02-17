package pizzk.android.process.jscross

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import org.json.JSONObject
import pizzk.android.js.natives.JsNativeCross
import pizzk.android.js.natives.JsInvoker
import pizzk.android.js.natives.JsRequest
import pizzk.android.process.jscross.impl.JSAlert
import pizzk.android.process.jscross.impl.JSConsole

class MainActivity : AppCompatActivity() {
    private lateinit var vWeb: WebView
    private lateinit var btCallJs: Button
    private lateinit var jsNativeCross: JsNativeCross
    //
    private var count: Int = 0
    private val logger: RunTimeLogger by lazy { RunTimeLogger(name = "WebView") }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vWeb = findViewById(R.id.vWeb)
        btCallJs = findViewById(R.id.btCallJs)
        btCallJs.visibility = View.GONE
        btCallJs.setOnClickListener { consoleInJs() }
        //web view设置
        vWeb.settings.javaScriptEnabled = true
        vWeb.webChromeClient = WebChromeClient()
        vWeb.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                btCallJs.visibility = View.VISIBLE
                logger.stop(baseContext)
            }
        }
        //注册原生sdk功能模块
        jsNativeCross = JsNativeCross(vWeb, ::applyHandles).open()
        //加载index页面
        logger.start()
        vWeb.loadUrl("file:///android_asset/web/index.html")
    }

    private fun applyHandles(requires: List<String>): List<JsInvoker.Hook> {
        return requires.mapNotNull { name ->
            return@mapNotNull when (name) {
                JSAlert.NAME -> JSAlert(baseContext)
                JSConsole.NAME -> JSConsole()
                else -> null
            }
        }
    }

    private fun consoleInJs() {
        val request = JsRequest(module = "Print", method = "consoleInJs")
        val jbt = JSONObject()
        count += 1
        jbt.put("count", count)
        jbt.put("msg", "call by android")
        request.payload = jbt.toString()
        jsNativeCross.invoke(request) { result ->
            val hint: String =
                if (result.success) "success: ${result.data}" else "failure: ${result.msg}"
            Toast.makeText(baseContext, hint, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jsNativeCross.close()
    }
}
