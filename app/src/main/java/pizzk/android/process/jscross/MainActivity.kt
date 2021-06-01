package pizzk.android.process.jscross

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import com.fasterxml.jackson.core.type.TypeReference
import pizzk.android.js.natives.JsNatives
import pizzk.android.js.natives.JsonParcelImpl
import pizzk.android.process.jscross.impl.*

class MainActivity : AppCompatActivity() {
    private lateinit var vWeb: WebView
    private lateinit var btCallJs: Button
    private val jsNatives: JsNatives = JsNatives()

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
        jsNatives.modules(
            JSAlert::class.java,
            JSConsole::class.java,
            JSFiles::class.java
        ).active(vWeb)
        //加载index页面
        logger.start()
        vWeb.loadUrl("file:///android_asset/web/index.html")
    }

    private fun consoleInJs() {
        count += 1
        val map: MutableMap<String, Any> = HashMap()
        map["count"] = count
        map["msg"] = "call by android"
        val path: String = jsNatives.joinPath("Print", "consoleInJs")
        jsNatives.js(path, map) { result: String ->
            val tf: TypeReference<HashMap<String, Any>> =
                object : TypeReference<HashMap<String, Any>>() {}
            val maps: Map<String, Any> = JsonParcelImpl.parse(result, tf) ?: emptyMap()
            val value = "code = ${maps["code"]}, who = ${maps["who"]}"
            Toast.makeText(baseContext, value, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        jsNatives.release()
        super.onDestroy()
    }
}
