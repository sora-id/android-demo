package com.soraid.androiddemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.webkit.*
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.soraid.androiddemo.response.Document
import com.soraid.androiddemo.response.Traits
import com.soraid.androiddemo.response.VerificationSession
import com.soraid.androiddemo.response.parseTraits
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var webViewDialog: Dialog
    private lateinit var verificationID: String
    //Temporarily holds the photo URI
    private var photoUri: Uri? = null

    private var fileChooserResultLauncher = createFileChooserResultLauncher()
    private var fileChooserValueCallback: ValueCallback<Array<Uri>>? = null

    /**
     * Requests runtime permission for camera
     */
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i(TAG, "Permission: Granted")
            } else {
                Log.i(TAG,"Permission: Denied")
            }
            checkCameraPermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setViews()
    }

    private fun createFileChooserResultLauncher(): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                fileChooserValueCallback?.onReceiveValue(arrayOf(Uri.parse(result?.data?.dataString)));
            } else {
                fileChooserValueCallback?.onReceiveValue(null)
            }
        }
    }

    private val takePhotoRequestLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            photoUri?.let {
                fileChooserValueCallback?.onReceiveValue(arrayOf(it));
            }
        }
    }

    private fun takePhoto() {
        getTmpFileUri().let { uri ->
            photoUri = uri
            takePhotoRequestLauncher.launch(uri)
        }
    }

    private fun setViews() {
        button = findViewById(R.id.button)
        progressBar = findViewById(R.id.progressBar)
        button.setOnClickListener {
            // The first step in the flow is to create a session.
           checkCameraPermission()
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
        Log.i(TAG, "showWebView ==> $url")
        runOnUiThread {
            webViewDialog = Dialog(this, com.google.android.material.R.style.Widget_MaterialComponents_MaterialCalendar_Fullscreen)
            webViewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            webViewDialog.setContentView(R.layout.web_view)

            val webView: WebView = webViewDialog.findViewById(R.id.webview)

            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.allowFileAccess = true
            webView.settings.allowContentAccess = true
            webView.settings.javaScriptCanOpenWindowsAutomatically = true
            webView.settings.builtInZoomControls = true

            webView.webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(wv: WebView, url: String): Boolean {
                    Log.i(TAG, "shouldOverrideUrlLoading ===> $url")
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

            //Optional Webview Log to see what is happening inside the Webview
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d(
                        TAG, consoleMessage?.message() + " -- From line "
                                + consoleMessage?.lineNumber() + " of "
                                + consoleMessage?.sourceId());
                    return super.onConsoleMessage(consoleMessage)
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    webView?.evaluateJavascript(
                        "document.getElementById('cameraFileInput').hasAttribute('capture')"
                    ) { value ->
                        Log.i(TAG, "Capture Enabled ==> $value")
                        try {
                            fileChooserValueCallback = filePathCallback
                            if (value.toBoolean()) {
                                takePhoto()
                            } else {
                                fileChooserParams?.let { params ->
                                    fileChooserResultLauncher.launch(params.createIntent())
                                }
                            }
                        } catch (exception: ActivityNotFoundException) {
                            Log.e(TAG, "Error capturing image")
                        }
                    }
                    return true
                }

                override fun onPermissionRequest(request: PermissionRequest) {
                    runOnUiThread {
                        val PERMISSIONS = arrayOf(
                            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                            PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        )
                        request.grant(PERMISSIONS)
                    }
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
        Log.i(TAG, "============================ JSON Printing =====================================")

        runOnUiThread {

            val status = response.getString("status")
            val responseString = response.toString(3)

            val responseTextView: TextView = findViewById(R.id.responseTextView)
            responseTextView.visibility = View.VISIBLE

            val traitsObject = parseTraits(response)
            Log.i(TAG, "Traits ===> \n $traitsObject")

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


    private fun showSnackbar(
        view: View,
        msg: String,
        length: Int,
        actionMessage: CharSequence?,
        action: (View) -> Unit
    ) {
        val snackbar = Snackbar.make(view, msg, length)
        if (actionMessage != null) {
            snackbar.setAction(actionMessage) {
                action(view)
            }.show()
        } else {
            snackbar.show()
        }
    }

    /**
     * Check for Camera permission and if exists, then start session
     * Else, request or show rationale
     */
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Camera Permission exists")
                createSession()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA) -> {
                showSnackbar(
                    button.rootView,
                    getString(R.string.permission_required),
                    Snackbar.LENGTH_INDEFINITE,
                    getString(R.string.ok)
                ) {
                    requestPermissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                }
            }
            else -> {
                Log.i(TAG, "Requesting Camera permission")
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    /**
     * Get temp file URI using the current time stamp
     */
    private fun getTmpFileUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(
            Date()
        )
        val tmpFile = File.createTempFile(timeStamp, ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }
}