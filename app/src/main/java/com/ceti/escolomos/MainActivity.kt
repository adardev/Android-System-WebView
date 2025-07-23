package com.ceti.escolomos

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var topBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        topBar = findViewById(R.id.topBar)

        // Configuración del WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            builtInZoomControls = true
            displayZoomControls = false
        }

        // Interfaz para recibir color desde JavaScript
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun sendColor(color: String) {
                runOnUiThread {
                    try {
                        val parsedColor = Color.parseColor(color)

                        // Actualiza la topBar (como antes)
                        topBar.setBackgroundColor(parsedColor)

                        // Actualiza status bar
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            window.statusBarColor = parsedColor
                        }

                        // Cambia color de íconos según fondo
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val decorView = window.decorView
                            if (isColorLight(parsedColor)) {
                                // Fondo claro → íconos oscuros
                                decorView.systemUiVisibility =
                                    decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            } else {
                                // Fondo oscuro → íconos claros
                                decorView.systemUiVisibility =
                                    decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }, "AndroidApp")

        // WebViewClient para ejecutar JS al finalizar la carga
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                view?.evaluateJavascript(
                    """
                    (function() {
                        var header = document.querySelector('header') 
                                  || document.querySelector('.navbar') 
                                  || document.querySelector('[class*="header"]');
                        if (header) {
                            var color = window.getComputedStyle(header).backgroundColor;
                            function rgb2hex(rgb) {
                                const result = rgb.match(/\d+/g);
                                if (result) {
                                    return "#" + result.map(x => {
                                        const hex = parseInt(x).toString(16);
                                        return hex.length == 1 ? "0" + hex : hex;
                                    }).join('');
                                }
                                return "#000000";
                            }
                            AndroidApp.sendColor(rgb2hex(color));
                        }
                    })();
                    """.trimIndent(), null
                )
            }
        }

        // Cargar el sitio
        webView.loadUrl("https://escolomos.ceti.mx")

        // Soporte para botón atrás
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    // Función para detectar si el color es claro
    private fun isColorLight(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val darkness = 1 - (0.299 * r + 0.587 * g + 0.114 * b) / 255
        return darkness < 0.5 // true: claro
    }
}
