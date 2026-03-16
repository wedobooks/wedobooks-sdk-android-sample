package io.wedobooks.sdk.library.wedobookssdksampleapp.service

import android.util.Log
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.BuildConfig
import io.wedobooks.sdk.library.wedobookssdksampleapp.util.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val TAG = "AuthService"

class AuthService private constructor() {

    companion object {
        val instance = AuthService()
    }
    private val client = OkHttpClient()
    val currentUser = WeDoBooksSdk.userOperations.currentUserIdFlow

    suspend fun getToken(uid: String): String? {
        return withContext(Dispatchers.IO) {
            val jsonBody = JSONObject().put(
                "uid", uid
            )
            val mediaType = "application/json".toMediaType()
            val body = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(BuildConfig.CUSTOM_TOKEN_URL)
                .post(body)
                .addHeader("Content-Length", "${body.contentLength()}")
                .addHeader("Connection", "close")
                .build()
            client.newCall(request).await().body?.string()?.let {
                Log.d(TAG, it)
                val jsonResponse = JSONObject(it)
                jsonResponse.getString("token")
            }
        }
    }

    suspend fun tokenLogin(token: String) {
        withContext(Dispatchers.IO) {
            WeDoBooksSdk.userOperations.signInWithToken(token)
        }
    }

    fun logout() {
        WeDoBooksSdk.userOperations.signOut()
    }
}