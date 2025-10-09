package com.getvisitapp.google_fit.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.getvisitapp.google_fit.R
import com.getvisitapp.google_fit.databinding.FeebackWebviewBinding

class FeedBackActivity : AppCompatActivity() {
    lateinit var binding: FeebackWebviewBinding


    lateinit var webUrl: String

    companion object {
        const val WEB_URL = "webUrl"
        fun getIntent(context: Context, webUrl: String): Intent {
            val intent = Intent(context, FeedBackActivity::class.java)
            intent.putExtra(WEB_URL, webUrl)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_feed_back)

        webUrl = intent.getStringExtra(WEB_URL)!!


        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview, true)


        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {

                Log.d("mytag", "onPageStarted called")

                binding.progressBar.visibility = View.VISIBLE

                url?.let {
                    if (url.startsWith("https://www.tataaig.com/")) {
                        destroyWebViewAndGoBack()
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d("mytag", "onPageFinished called")
                binding.progressBar.visibility = View.GONE
                super.onPageFinished(view, url)

            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.d("mytag", "onReceivedError called")

                super.onReceivedError(view, request, error)
            }


        }
        binding.webview.webChromeClient = object : WebChromeClient() {


        }

        binding.webview.loadUrl(webUrl)

        binding.closeImageView.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_from_top, R.anim.slide_in_top);
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        destroyWebViewAndGoBack()
    }

    fun destroyWebViewAndGoBack() {
        binding.webview.clearHistory()
        binding.webview.onPause()
        binding.webview.removeAllViews()
        binding.webview.destroyDrawingCache()

        // NOTE: This can occasionally cause a segfault below API 17 (4.2)
        binding.webview.destroy();

        finish()
        overridePendingTransition(R.anim.slide_from_top, R.anim.slide_in_top);
    }
}