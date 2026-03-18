package io.wedobooks.sdk.library.wedobookssdksampleapp.services

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.models.Checkout
import io.wedobooks.sdk.models.WdbAudioPlayer
import io.wedobooks.sdk.models.enums.MaterialType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@UnstableApi
class WDBAudioPlayerSessionService : MediaLibraryService() {
    private var player: WdbAudioPlayer? = null
    private var session: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        private const val SESSION_ID = "custom_player_session"
        const val LOAD_BOOK_COMMAND = "custom_player.load_book"
        const val RESULT_DID_LOAD = "result_did_load"
        const val ARG_CHECKOUT_ID = "arg_checkout_id"
        const val ARG_MATERIAL_ID = "arg_material_id"
        const val ARG_TITLE = "arg_title"
        const val ARG_AUTHORS = "arg_authors"
        const val ARG_BOOK_TYPE = "arg_book_type"
        const val ARG_COVER_URL = "arg_cover_url"
        const val ARG_INITIAL_PROGRESS_MS = "arg_initial_progress_ms"
    }

    override fun onCreate() {
        super.onCreate()
        attachSession()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        session?.release()
        session = null
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
    }

    private fun attachSession() {
        val builtPlayer = WeDoBooksSdk.wdbAudioPlayerBuilder()
            .setCoroutineContext(Dispatchers.Main.immediate)
            .build()
        player = builtPlayer
        session = MediaLibrarySession.Builder(this, builtPlayer, callback)
            .setId(SESSION_ID)
            .build()
    }

    private suspend fun loadBookInternal(
        checkout: Checkout,
        coverUrl: String?,
        initialProgressMs: Long?,
    ): Boolean = withContext(Dispatchers.Main.immediate) {
        val currentPlayer = player ?: return@withContext false
        val loaded = currentPlayer.loadBook(
            checkout = checkout,
            coverUrl = coverUrl,
            initialProgressMs = initialProgressMs
        )
        if (loaded) {
            currentPlayer.prepare()
        }
        loaded
    }

    private fun parseCheckout(args: Bundle): Checkout? {
        val checkoutId = args.getString(ARG_CHECKOUT_ID) ?: return null
        val materialId = args.getString(ARG_MATERIAL_ID) ?: return null
        val title = args.getString(ARG_TITLE) ?: return null
        val authors = args.getStringArrayList(ARG_AUTHORS)?.toList() ?: emptyList()
        val typeName = args.getString(ARG_BOOK_TYPE) ?: return null
        val type = runCatching { MaterialType.valueOf(typeName) }.getOrNull() ?: return null
        return object : Checkout {
            override val id: String = checkoutId
            override val userId: String = ""
            override val active: Boolean = true
            override val author: List<String> = authors
            override val materialId: String = materialId
            override val title: String = title
            override val type: MaterialType = type
            override val publisher: String = ""
            override val start: Instant = Instant.EPOCH
            override val end: Instant = Instant.EPOCH
        }
    }

    private val callback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(SessionCommand(LOAD_BOOK_COMMAND, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != LOAD_BOOK_COMMAND) {
                return super.onCustomCommand(session, controller, customCommand, args)
            }
            val resultFuture = SettableFuture.create<SessionResult>()
            val checkout = parseCheckout(args)
            if (checkout == null) {
                resultFuture.set(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                return resultFuture
            }
            val coverUrl = args.getString(ARG_COVER_URL)
            val initialProgressMs = args.getLong(ARG_INITIAL_PROGRESS_MS, -1L)
                .takeIf { it >= 0L }

            serviceScope.launch {
                val loaded = runCatching {
                    loadBookInternal(
                        checkout = checkout,
                        coverUrl = coverUrl,
                        initialProgressMs = initialProgressMs
                    )
                }.getOrDefault(false)
                val extras = Bundle().apply {
                    putBoolean(RESULT_DID_LOAD, loaded)
                }
                resultFuture.set(
                    SessionResult(
                        if (loaded) SessionResult.RESULT_SUCCESS else SessionResult.RESULT_ERROR_UNKNOWN,
                        extras
                    )
                )
            }
            return resultFuture
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId("custom_root")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("WdbAudioPlayer")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }
    }
}
