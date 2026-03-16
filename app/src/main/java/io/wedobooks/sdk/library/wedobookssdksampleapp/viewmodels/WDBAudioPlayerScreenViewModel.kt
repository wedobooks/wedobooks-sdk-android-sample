package io.wedobooks.sdk.library.wedobookssdksampleapp.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.library.wedobookssdksampleapp.services.WDBAudioPlayerSessionService
import io.wedobooks.sdk.models.CheckoutBook
import io.wedobooks.sdk.models.WdbAudioDownloadStatus
import io.wedobooks.sdk.models.enums.BookType
import io.wedobooks.sdk.models.enums.WdbDownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

private const val TAG = "WDBAudioPlayerVM"

data class WDBAudioPlayerState(
    val checkout: CheckoutBook? = null,
    val didLoad: Boolean = false,
    val isLoading: Boolean = false,
    val isPlayerReady: Boolean = false,
    val statusMessage: String = "Not loaded",
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val downloadStatus: WdbAudioDownloadStatus? = null,
) {
    val canDownload: Boolean
        get() = checkout?.type == BookType.Audiobook &&
            (downloadStatus == null ||
                downloadStatus.state == WdbDownloadState.NotStarted ||
                downloadStatus.state == WdbDownloadState.Cancelled ||
                downloadStatus.state == WdbDownloadState.Error)

    val canDelete: Boolean
        get() = checkout?.type == BookType.Audiobook &&
            downloadStatus != null &&
            downloadStatus.state != WdbDownloadState.NotStarted
}

class WDBAudioPlayerScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val checkout = MutableStateFlow<CheckoutBook?>(null)
    private val _state = MutableStateFlow(WDBAudioPlayerState())
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionJob: Job? = null
    private var previousDownloadState: WdbDownloadState? = null
    private val coverUrl: String? = null

    val state: StateFlow<WDBAudioPlayerState> = _state.asStateFlow()

    private val controllerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _state.value = _state.value.copy(
                    isPlayerReady = true,
                    statusMessage = "Player ready",
                )
            }
        }
    }

    init {
        combine(
            checkout,
            WeDoBooksSdk.storage.audioDownloadsFlow
        ) { selectedCheckout, downloads ->
            val status = selectedCheckout?.materialId?.let { downloads[it] }
            selectedCheckout to status
        }.onEach { (selectedCheckout, status) ->
            _state.value = _state.value.copy(
                checkout = selectedCheckout,
                downloadStatus = status,
            )
            onDownloadStateChanged(status?.state)
        }.launchIn(viewModelScope)
    }

    fun setCheckout(value: CheckoutBook?) {
        checkout.value = value
    }

    fun loadPlayer() {
        viewModelScope.launch {
            val selectedCheckout = checkout.value
            if (selectedCheckout?.type != BookType.Audiobook) {
                _state.value = _state.value.copy(
                    statusMessage = "Select an audiobook first",
                    didLoad = false,
                    isPlayerReady = false,
                )
                return@launch
            }
            _state.value = _state.value.copy(
                isLoading = true,
                isPlayerReady = false,
                didLoad = false,
                statusMessage = "Loading WdbAudioPlayer...",
            )
            try {
                val loaded = loadBookWithCommand(
                    checkout = selectedCheckout,
                    coverUrl = coverUrl,
                    initialProgressMs = 32000
                )
                if (loaded) {
                    _state.value = _state.value.copy(
                        didLoad = true,
                        statusMessage = "Controller connected, waiting for STATE_READY...",
                    )
                    startPositionPolling()
                } else {
                    _state.value = _state.value.copy(statusMessage = "Failed to load player")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    statusMessage = "Failed to load player: ${e.message ?: "unknown error"}"
                )
                Log.d(TAG, "loadPlayer failed: ${e.message}", e)
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            withControllerOnMain { mediaController ->
                if (mediaController.isPlaying) {
                    mediaController.pause()
                } else {
                    mediaController.play()
                }
                _state.value = _state.value.copy(
                    currentPositionMs = mediaController.currentPosition.coerceAtLeast(0L),
                    totalDurationMs = mediaController.duration.coerceAtLeast(0L),
                )
            }
        }
    }

    fun seekForward15() {
        viewModelScope.launch {
            withControllerOnMain { mediaController ->
                val target = max(0L, mediaController.currentPosition + 15_000L)
                mediaController.seekTo(target)
                _state.value = _state.value.copy(
                    currentPositionMs = mediaController.currentPosition.coerceAtLeast(0L),
                    totalDurationMs = mediaController.duration.coerceAtLeast(0L),
                )
            }
        }
    }

    fun download() {
        viewModelScope.launch {
            val selectedCheckout = checkout.value ?: return@launch
            val started = runCatching {
                WeDoBooksSdk.storage.downloadAudioBook(selectedCheckout)
            }.onFailure {
                Log.d(TAG, "downloadAudioBook failed: ${it.message}", it)
            }.getOrDefault(false)
            _state.value = _state.value.copy(
                statusMessage = if (started) "Download started" else "Download failed"
            )
        }
    }

    fun deleteDownload() {
        viewModelScope.launch {
            val selectedCheckout = checkout.value ?: return@launch
            val removed = runCatching {
                WeDoBooksSdk.storage.removeAudioBookDownload(selectedCheckout)
            }.onFailure {
                Log.d(TAG, "removeAudioBookDownload failed: ${it.message}", it)
            }.getOrDefault(false)
            _state.value = _state.value.copy(
                statusMessage = if (removed) "Download removed" else "Remove failed"
            )
        }
    }

    fun killPlayer() {
        viewModelScope.launch {
            withControllerOnMain { it.stop() }
        }
        releasePlayer()
        getApplication<Application>().applicationContext.stopService(
            Intent(getApplication(), WDBAudioPlayerSessionService::class.java)
        )
        _state.value = _state.value.copy(
            statusMessage = "Player stopped",
            didLoad = false,
            isPlayerReady = false,
        )
    }

    private suspend fun getOrCreateController(): MediaController {
        return withContext(Dispatchers.Main.immediate) {
            val current = controller
            if (current != null) return@withContext current
            val appContext = getApplication<Application>().applicationContext
            appContext.startService(Intent(appContext, WDBAudioPlayerSessionService::class.java))
            val token = SessionToken(
                appContext,
                ComponentName(appContext, WDBAudioPlayerSessionService::class.java)
            )
            val future = MediaController.Builder(appContext, token)
                .setListener(object : MediaController.Listener {})
                .buildAsync()
            controllerFuture = future
            future.await().also { created ->
                created.addListener(controllerListener)
                controller = created
            }
        }
    }

    private suspend fun loadBookWithCommand(
        checkout: CheckoutBook,
        coverUrl: String?,
        initialProgressMs: Long?,
    ): Boolean {
        val mediaController = getOrCreateController()
        val args = Bundle().apply {
            putString(WDBAudioPlayerSessionService.ARG_CHECKOUT_ID, checkout.id)
            putString(WDBAudioPlayerSessionService.ARG_MATERIAL_ID, checkout.materialId)
            putString(WDBAudioPlayerSessionService.ARG_TITLE, checkout.title)
            putStringArrayList(
                WDBAudioPlayerSessionService.ARG_AUTHORS,
                ArrayList(checkout.author)
            )
            putString(WDBAudioPlayerSessionService.ARG_BOOK_TYPE, checkout.type.name)
            putString(WDBAudioPlayerSessionService.ARG_COVER_URL, coverUrl)
            initialProgressMs?.let {
                putLong(WDBAudioPlayerSessionService.ARG_INITIAL_PROGRESS_MS, it)
            }
        }
        val command = SessionCommand(WDBAudioPlayerSessionService.LOAD_BOOK_COMMAND, Bundle.EMPTY)
        val result = mediaController.sendCustomCommand(command, args).await()
        if (result.resultCode != SessionResult.RESULT_SUCCESS) return false
        return result.extras.getBoolean(WDBAudioPlayerSessionService.RESULT_DID_LOAD, false)
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                if (!_state.value.didLoad) {
                    delay(500)
                    continue
                }
                withControllerOnMain { mediaController ->
                    _state.value = _state.value.copy(
                        currentPositionMs = mediaController.currentPosition.coerceAtLeast(0L),
                        totalDurationMs = mediaController.duration.coerceAtLeast(0L),
                        isPlaying = mediaController.isPlaying,
                    )
                }
                delay(500)
            }
        }
    }

    private fun onDownloadStateChanged(currentState: WdbDownloadState?) {
        val selectedCheckout = checkout.value ?: return
        if (selectedCheckout.type != BookType.Audiobook) return
        if (previousDownloadState != WdbDownloadState.Finished && currentState == WdbDownloadState.Finished) {
            viewModelScope.launch {
                runCatching {
                    val mediaController = withControllerOnMain { it } ?: return@launch
                    val currentPosition = mediaController.currentPosition.coerceAtLeast(0L)
                    val wasPlaying = mediaController.isPlaying
                    val reloaded = loadBookWithCommand(
                        checkout = selectedCheckout,
                        coverUrl = coverUrl,
                        initialProgressMs = currentPosition
                    )
                    if (reloaded) {
                        if (wasPlaying) {
                            withControllerOnMain { it.play() }
                        }
                        _state.value = _state.value.copy(
                            statusMessage = "Download finished, switched to local source"
                        )
                    }
                }.onFailure {
                    Log.d(TAG, "reload after download failed: ${it.message}", it)
                }
            }
        }
        previousDownloadState = currentState
    }

    private fun releasePlayer() {
        positionJob?.cancel()
        positionJob = null
        viewModelScope.launch {
            withControllerOnMain {
                it.removeListener(controllerListener)
                it.release()
            }
        }
        controller = null
        controllerFuture?.cancel(true)
        controllerFuture = null
    }

    private suspend fun <T> withControllerOnMain(block: (MediaController) -> T): T? {
        return withContext(Dispatchers.Main.immediate) {
            controller?.let(block)
        }
    }

    private suspend fun <T> ListenableFuture<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addListener(
                {
                    try {
                        continuation.resume(get())
                    } catch (throwable: Throwable) {
                        continuation.resumeWithException(throwable)
                    }
                },
                MoreExecutors.directExecutor()
            )
        }

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }
}
