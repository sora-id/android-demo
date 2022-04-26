# Sora Android Demo
This app is a sample demo of the Sora ID verification flow for Android

![Sora Verification Flow](https://files.readme.io/42b1ba6-Simulator_Screen_Shot_-_iPhone_X_15.2_-_2022-04-12_at_13.11.04_copy.png)

## Quickstart
To quickly test out the verification flow you can clone the app from:
https://github.com/sora-id/android-demo
In the `local.properties` set BASE_URL="https://verify.soraid.com/" and the API_KEY.

> ⚠️ **Security Note**: For a production system, do not embed this key in the client side application. Requests to Sora’s API should only be performed on your backend servers.!

## Deep dive
The demo app implements the following steps to validate a user:
 - Fetches a verification session from Sora
 - Launches a web view for the user to put in their information
 - If successful, a redirect with the custom soraid:// scheme is made with a verification token
 - The app then uses that token to retrieve the users information

## The demo app is broken down into these parts:

### API:
- Handles calling the APIs for the verification flow. To call these APIs you will have to pass the API Key as a header
```json
"Authorization: Bearer <API_KEY>"
```
- createSession: Fetches a token from the /v1/verification_sessions that can be used to embed a verification webView. To receive the deep link you will have to pass this as a payload:
```json
"{\"is_webview\": \"true\"}"
```
- retrieveUser: After getting the token from createSession pass it as a query parameter to get the verified data.

### MainActivity:
- UI to trigger the WebView
- Accepts deep links
- createSession: Calls the createSession in the API class and triggers the webView with the verification page
- showWebView: Handles showing the webview and setting listeners for redirects and back button.
- processRedirect: Parses the soraid:// deep links and checks the status. If it’s a success the WebView redirects to soraid://success and the retrieveUser function is called
- parseVerification: If the retrieveUser call is successful retrieveUser  will return a JSON of the users retrieved verified data. ContentView will then display this data
