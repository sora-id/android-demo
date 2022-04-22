package com.soraid.androiddemo

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class Method {
    GET, POST
}

class API {
    companion object {
        val shared: API = API()
    }

    fun retrieveUser(verificationID: String, callback: (JSONObject?, Exception?) -> Unit) {
        request(verificationID, Method.GET, null, callback)
    }

    fun createSession(callback: (JSONObject?, Exception?) -> Unit) {
        val payload = JSONObject()
        payload.put("is_webview", "true")
        request(null, Method.POST, payload, callback)
    }

    private fun request(queryParams: String? = null, method: Method, payload: JSONObject? = null, callback: (JSONObject?, Exception?) -> Unit) {

        val apiURL = BuildConfig.BASE_URL
        if (apiURL.isEmpty()) {
            callback(null, IllegalArgumentException("Missing BASE_URL. Please make sure you've added the BASE_URL in local.properties"))
        }

        var urlString = apiURL + "v1/verification_sessions"

        val thread = Thread {
            val apiKey = BuildConfig.API_KEY
            if (apiKey.isEmpty()) {
                callback(null, IllegalArgumentException("Missing API_KEY. Please make sure you've added the API_KEY in local.properties"))
            }

            if (queryParams != null) {
                urlString = "$urlString/$queryParams"
            }

            val url = URL(urlString)
            val httpURLConnection = url.openConnection() as HttpURLConnection
            httpURLConnection.requestMethod = method.name
            httpURLConnection.setRequestProperty("Authorization", "Bearer $apiKey")
            httpURLConnection.setRequestProperty("Content-Type", "application/json")

            try {
                httpURLConnection.doInput = true

                if (method == Method.POST) {
                    httpURLConnection.doOutput = true

                    val outputStreamWriter = OutputStreamWriter(httpURLConnection.outputStream)
                    if (payload != null) {
                        val jsonObjectString = payload.toString()
                        outputStreamWriter.write(jsonObjectString)
                    }
                    outputStreamWriter.flush()
                }

                // Check if the connection is successful
                val responseCode = httpURLConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = httpURLConnection.inputStream.bufferedReader()
                        .use {
                            it.readText()
                        }
                    callback(JSONObject(response), null)
                } else {
                    callback(null, Exception(httpURLConnection.responseMessage))
                }
            } catch (e: Exception) {
                callback(null, e)
            }
            finally {
                httpURLConnection.disconnect()
            }
        }
        thread.start()
    }
}