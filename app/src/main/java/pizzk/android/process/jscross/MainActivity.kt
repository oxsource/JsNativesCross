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
import pizzk.android.js.natives.JsInvoker
import pizzk.android.process.jscross.impl.JSAlert
import pizzk.android.process.jscross.impl.JSConsole
import pizzk.android.process.jscross.impl.JSFiles
import pizzk.android.process.jscross.impl.JsonParcelImpl

class MainActivity : AppCompatActivity() {
    private lateinit var vWeb: WebView
    private lateinit var btCallJs: Button
    private lateinit var jsInvoker: JsInvoker
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
        jsInvoker = JsInvoker(vWeb, JsonParcelImpl).open(::applyHandles)
        //加载index页面
        logger.start()
        vWeb.loadUrl("file:///android_asset/web/index.html")
    }

    private fun applyHandles(name: String): Any? {
        return when (name) {
            JSAlert.NAME -> JSAlert(baseContext)
            JSConsole.NAME -> JSConsole()
            JSFiles.NAME -> JSFiles(baseContext)
            else -> null
        }
    }

    private fun consoleInJs() {
        count += 1
        val map: MutableMap<String, Any> = HashMap()
        map["count"] = count
        map["msg"] = "call by android"
        val path: String = jsInvoker.joinPath("Print", "consoleInJs")
        jsInvoker.js(path, map) { result: String ->
            Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        jsInvoker.close()
        super.onDestroy()
    }
}
