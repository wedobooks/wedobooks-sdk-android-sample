package io.wedobooks.sdk.library.wedobookssdksampleapp.util

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (cont.isCancelled) return
            cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (cont.isCancelled) return
            cont.resumeWith(Result.success(response))
        }
    })

    cont.invokeOnCancellation {
        try {
            cancel()
        } catch (e: Exception) {
            Log.d("CallExt", "[await] cancel failed err: ${e.message}")
        }
    }
}