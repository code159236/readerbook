package com.v2reading.reader.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.activityViewModels
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseFragment
import com.v2reading.reader.constant.AppConst
import com.v2reading.reader.data.entities.BaseSource
import com.v2reading.reader.databinding.FragmentWebViewLoginBinding
import com.v2reading.reader.help.http.CookieStore
import com.v2reading.reader.utils.snackbar
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding

class WebViewLoginFragment : BaseFragment(R.layout.fragment_web_view_login) {

    private val binding by viewBinding(FragmentWebViewLoginBinding::bind)
    private val viewModel by activityViewModels<SourceLoginViewModel>()

    private var checking = false

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        viewModel.source?.let {
            binding.titleBar.title = getString(R.string.login_source, it.getTag())
            initWebView(it)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.source_webview_login, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_ok -> {
                if (!checking) {
                    checking = true
                    binding.titleBar.snackbar(R.string.check_host_cookie)
                    viewModel.source?.let { source ->
                        source.loginUrl?.let {
                            binding.webView.loadUrl(it, source.getHeaderMap(true))
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(source: BaseSource) {
        binding.webView.settings.apply {
            builtInZoomControls = true
            javaScriptEnabled = true
            source.getHeaderMap()[AppConst.UA_NAME]?.let {
                userAgentString = it
            }
        }
        val cookieManager = CookieManager.getInstance()
        source.loginUrl?.let {
            cookieManager.setCookie(it, CookieStore.getCookie(it))
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val cookie = cookieManager.getCookie(url)
                CookieStore.setCookie(source.getKey(), cookie)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val cookie = cookieManager.getCookie(url)
                CookieStore.setCookie(source.getKey(), cookie)
                if (checking) {
                    activity?.finish()
                }
                super.onPageFinished(view, url)
            }
        }
        source.loginUrl?.let {
            binding.webView.loadUrl(it, source.getHeaderMap(true))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

}