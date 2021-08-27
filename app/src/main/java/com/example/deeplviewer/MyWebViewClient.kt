package com.example.deeplviewer

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.Toast
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MyWebViewClient(
    private val activity: MainActivity,
    private val webView: WebView
) : WebViewClient() {
    private var isSplashFadeDone: Boolean = false
    private var param: String = "#en/en/"

    val urlParam: String get() = param

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(intent)
        return true
    }

    override fun onPageFinished(view: WebView, url: String) {
        view.loadUrl(
            "javascript:" +
                    """
                        $('button').css('-webkit-tap-highlight-color','rgba(0, 0, 0, 0)');
                        $('#dl_translator').siblings().hide();
                        $('.dl_header_menu_v2__buttons__menu').hide();
                        $('.dl_header_menu_v2__buttons__item').hide();
                        $('.dl_header_menu_v2__links').children().not('#dl_menu_translator_simplified').hide();
                        $('.dl_header_menu_v2__separator').hide();
                        $('.lmt__bottom_text--mobile').hide();
                        $('#dl_cookieBanner').hide();
                        $('.lmt__language_container_sec').hide();
                        $('.docTrans_translator_upload_button__inner_button').hide();
                        $('.lmt__target_toolbar__save').hide();
                        $('footer').hide();
                        $('a').css('pointer-events','none');
                        $('.lmt__sides_container').css('margin-bottom','32px');
                        $('.lmt__translations_as_text__copy_button, .lmt__target_toolbar__copy').on('click',function() {
                            const text = $('.lmt__translations_as_text__text_btn').eq(0).text();
                            Android.copyClipboard(text);
                        });
                        
                        if (!$('#lang_switch').length) {
                            $('html > head').append($('<style>#lang_switch.switched svg {transform:scaleX(-1)}</style>'));
                            $('\
                                <div id="lang_switch" style="display: block;z-index: 11;padding: 9px;border-radius: 3px;border: 1px solid #e3e3e3;background-color: #fff;margin-top: -10px;margin-left: -28px;margin-right: 10px;height: 44px;width: 44px;" data-testid="deepl-ui-tooltip-target">\
                                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" style="transition: .24s transform ease-out;height: 24px;width: 24px;">\
                                        <path d="M5 13.8317L6.57731 12.2972V15.3661L5 13.8317ZM5 13.8317H14.5" stroke="#0F2B46" stroke-width="2"></path>\
                                        <path d="M14.5 6.53448L12.9227 5V8.06897L14.5 6.53448ZM14.5 6.53448H5" stroke="#0F2B46" stroke-width="2"></path>\
                                    </svg>\
                                </div>\
                            ').click(function(){
                                this.classList.contains("switched") ? this.classList.remove("switched") : this.classList.add("switched");
                                window.location = window.location.href.split('#')[0] +
                                    '#' + document.getElementsByClassName('lmt__language_select lmt__language_select--target')[0].getAttribute('dl-selected-lang').split('-')[0] +
                                    '/' + document.getElementsByClassName('lmt__language_select lmt__language_select--source')[0].getAttribute('dl-selected-lang').split('-')[0] + 
                                    '/' + encodeURI(document.getElementsByClassName('lmt__textarea lmt__target_textarea lmt__textarea_base_style')[0].value);
                            }).prependTo($('.lmt__language_container')[1]);
                        }
                    """
        )

        if (!isSplashFadeDone) {
            isSplashFadeDone = true
            val animation = AlphaAnimation(0.0F, 1.0F)
            animation.duration = 100
            webView.startAnimation(animation)
        }
        webView.alpha = 1.0F

        val nightMode =
            (webView.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (nightMode) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
                webView.loadUrl(
                    "javascript:" +
                            """
                            $('.dl_header_menu_v2__logo__img').attr('src','data:image/svg+xml;base64,${
                                getAssetsString(
                                    webView.context,
                                    "DeepL_Logo_lightBlue_v2.svg"
                                ).toBase64String()
                            }');
                            $('.dl_logo_text').attr('src','data:image/svg+xml;base64,${
                                getAssetsString(
                                    webView.context,
                                    "DeepL_Text_light.svg"
                                ).toBase64String()
                            }');
                    """
                )
            } else {
                Toast.makeText(activity, "Dark mode cannot be used because FORCE_DARK is not supported", Toast.LENGTH_LONG).show()
            }
        }

        Regex("""#(.+?)/(.+?)/""").find(webView.url ?: "")?.let { param = it.value }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest,
        error: WebResourceError?
    ) {
        if (request.isForMainFrame) {
            activity.setContentView(R.layout.network_err)
            val button: ImageButton = activity.findViewById(R.id.reload)
            val listener = ReloadButtonListener()
            button.setOnClickListener(listener)

            val errorDescription =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) error?.description.toString() else ""
            Toast.makeText(activity, errorDescription, Toast.LENGTH_LONG).show()
            Log.e("onReceivedError", errorDescription)
        }
    }

    private inner class ReloadButtonListener : View.OnClickListener {
        override fun onClick(view: View) {
            val i = Intent(activity, MainActivity::class.java)
            activity.finish()
            activity.overridePendingTransition(0, 0)
            activity.startActivity(i)
        }
    }

    private fun String.toBase64String(): String {
        return Base64.encodeToString(this.toByteArray(), Base64.DEFAULT)
    }

    private fun getAssetsString(context: Context, fileName: String): String {
        return context.assets.open(fileName).reader(charset = Charsets.UTF_8).use { it.readText() }
    }
}