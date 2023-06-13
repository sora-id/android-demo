# Sora Android Demo
This app is a sample demo of the Sora ID verification flow for Android

![Sora Verification Flow](https://files.readme.io/42b1ba6-Simulator_Screen_Shot_-_iPhone_X_15.2_-_2022-04-12_at_13.11.04_copy.png)

## Quickstart
To quickly test out the verification flow you can clone the app from:
https://github.com/sora-id/android-demo
In the `local.properties` set BASE_URL="https://verify.soraid.com/", the API_KEY, and the PROJECT_ID of the project you want to use for verification (Can be found in the "Projects" page of the Sora platform (https://verify.soraid.com/dashboard).

> ⚠️ **Security Note**: For a production system, do not embed this key in the client side application. Requests to Sora’s API should only be performed on your backend servers.!

## Deep dive
The demo app implements the following steps to validate a user:
 - Creates and fetches a verification session from Sora
 - Launches a web view for the user to submit their information
 - If successful, a deeplink redirect with the custom soraid:// scheme is made with a verification token
 - The app then uses that token to retrieve the user's information

## The demo app is broken down into these parts:

### API:
- Handles calling the APIs for the verification flow. To call these APIs you will have to pass the API Key as a header
```json
"Authorization: Bearer <API_KEY>"
```
- createSession: Fetches a token from the /v1/verification_sessions that can be used to embed a verification webView. Include the project ID for the verification in this call. To receive the deep link you will have to pass `is_webview: true` as a payload.
```json
"{\"is_webview\": \"true\", \"project_id\": \"<PROJECT_ID>\"}"
```
You can also specify the desired authorization type, (one of `email` or `phone`) in this call. 
- retrieveUser: After getting the token from createSession pass it as a query parameter to get the verified data.

### MainActivity:
- UI to trigger the WebView
- Accepts deep links
- createSession: Calls the createSession in the API class and triggers the webView with the verification page
- showWebView: Handles showing the webview and setting listeners for redirects and back button.
- processRedirect: Parses the soraid:// deep links and checks the status. If it’s a success the WebView redirects to soraid://success and the retrieveUser function is called
- parseVerification: If the retrieveUser call is successful retrieveUser  will return a JSON of the users retrieved verified data. ContentView will then display this data

### Requesting permissions:
If you wish to use Sora's government ID scan or selfie verification functions, you will need to handle camera permissions in your Android application. At minimum, you need to request `android.permission.CAMERA` and `android.permission.READ_EXTERNAL_STORAGE`. See `requestPermissionLauncher` in `MainActivity.kt` for an example of how to handle these permissions.

When a user uploads a photo for their government ID, the webView uses a basic hidden `<input>` element. You need to handle the FileChooser request in your application and create the correct intent to either upload or capture a photo. We are working to develop this into a JavaScript interface, but for the moment, to detect whether the intent should open the camera directly or give users the ability to upload a previously taken photo, you can check the `<input>` for the attribute `capture`. 
```
webView?.evaluateJavascript("document.getElementById('cameraFileInput').hasAttribute('capture')")
```
**Most commonly, we recommend only allowing users to take a photo of their ID for security purposes.** You can configure this in the Sora platform IDV builder under Traits -> Government ID -> Allow photo ID upload. When the camera file input has attribute `capture`, it is set to accept newly captured photos only. When it does not have this attribute, users should be able to either upload or capture a new photo. For full code, see `onShowFileChooser` in `MainActivity.kt`. 
