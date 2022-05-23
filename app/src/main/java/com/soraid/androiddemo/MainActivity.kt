package com.soraid.androiddemo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private lateinit var button: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var webViewDialog: Dialog
    private lateinit var verificationID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setViews()
    }

    private fun setViews() {
        button = findViewById(R.id.button)
        progressBar = findViewById(R.id.progressBar)
        button.setOnClickListener {
            // The first step in the flow is to create a session.
            createSession()
        }
    }

    // Calls the /v1/verification_sessions endpoint using the API_KEY stored in the plist.
    // A successful call returns a token which can then be used to show the verification webview
    private fun createSession() {
        showProgress(true)
        API.shared.createSession { response, exception ->
            if (exception == null) {
                val token = response?.getString("token")
                token.let {
                    val id = response?.getString("id")
                    id?.let {
                        verificationID = id

                        // If a session is created successfully, we can then use the returned token to embed the Sora ID verification page into the WebView
                        showWebView(BuildConfig.BASE_URL + "verify/?token=" + token)
                    } ?: run {
                        showAlert()
                    }
                }
            }
            else {
                showAlert()
            }
            showProgress(false)
        }
    }

    // If a session is created successfully, we can then use the returned token to embed the Sora ID verification page into the WebView
    @SuppressLint("SetJavaScriptEnabled")
    private fun showWebView(url: String) {
        runOnUiThread {
            webViewDialog = Dialog(this, com.google.android.material.R.style.Widget_MaterialComponents_MaterialCalendar_Fullscreen)
            webViewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            webViewDialog.setContentView(R.layout.web_view)

            val webView: WebView = webViewDialog.findViewById(R.id.webview)

            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true

            webView.webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(wv: WebView, url: String): Boolean {
                    if (Uri.parse(url).host == BuildConfig.BASE_URL) {
                        return false
                    } else if (Uri.parse(url).scheme == "soraid") {
                        // If the WebView detects a soraid:// redirect it triggers the code to fetch the users verification data
                        this@MainActivity.processRedirect(Uri.parse(url))
                        return true
                    }

                    return true
                }
            }

            webViewDialog.setOnKeyListener { dialog, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss()
                }
                false
            }

            webView.loadUrl(url)
            webViewDialog.show()
        }
    }

    // Parses the soraid:// redirect uri and checks for success
    private fun processRedirect(uri: Uri) {
        if (uri.host == "success") {
            webViewDialog.dismiss()
            retrieveUser(verificationID)
        }
        else if (uri.host == "exit") {
            webViewDialog.dismiss()
            showAlert()
        }
    }

    // Calls the /v1/verification_sessions endpoint to fetch the users verification data
    private fun retrieveUser(verificationID: String) {
        showProgress(true)
        API.shared.retrieveUser(verificationID) { response, exception ->
            if (exception == null && response != null) {
                parseVerification(response)
            }
            else {
                showAlert()
            }
            showProgress(false)
        }
    }

    // Helper function to parse the /v1/verification_sessions response and populates the content view with that data
    private fun parseVerification(response: JSONObject) {
        runOnUiThread {
            val status = response.getString("status")
            val responseString = response.toString(3)

            val responseTextView: TextView = findViewById(R.id.responseTextView)
            responseTextView.visibility = View.VISIBLE

            val verifiedTextView: TextView = findViewById(R.id.verifiedTextView)
            verifiedTextView.text = responseString

            val statusTextView: TextView = findViewById(R.id.statusTextView)

            val statusEmoji = when (status) {
                "success" -> "✅"
                "failed" -> "❌"
                else -> "⌛"
            }

            (statusEmoji + " " + getString(R.string.verified_text) + ": " + status).also { statusString ->
                statusTextView.text = statusString
            }

            button.visibility = View.GONE
        }
    }

    // Handles showing and hiding the progress bar and button
    private fun showProgress(progress: Boolean) {
        runOnUiThread {
            if (progress) {
                progressBar.visibility = View.VISIBLE
                button.visibility = View.INVISIBLE
            }
            else {
                progressBar.visibility = View.INVISIBLE
                button.visibility = View.VISIBLE
            }
        }
    }

    // Alert dialog if something goes wrong
    private fun showAlert() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Could not fetch data")
            builder.setNegativeButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }
    }
}