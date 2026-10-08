package com.shilapi.xcertplay.compat

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi

/**
 * Audio focus request that uses [AudioFocusRequest] on API 26+ and the legacy
 * listener/stream API on API 22-25. The legacy stream is derived from the
 * attributes' legacy stream type, so media stays on STREAM_MUSIC.
 */
class AudioFocusCompat private constructor(
    private val modern: Any?,
    private val listener: AudioManager.OnAudioFocusChangeListener,
    private val legacyStream: Int,
    private val gain: Int,
) {
    /** The listener the platform calls back (exposed for tests). */
    val onFocusChange: AudioManager.OnAudioFocusChangeListener get() = listener

    fun request(manager: AudioManager): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Modern.request(manager, modern)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(listener, legacyStream, gain)
        }

    fun abandon(manager: AudioManager): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Modern.abandon(manager, modern)
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(listener)
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private object Modern {
        fun build(
            gain: Int,
            attributes: AudioAttributes,
            listener: AudioManager.OnAudioFocusChangeListener,
            handler: Handler,
        ): Any = AudioFocusRequest.Builder(gain)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(listener, handler)
            .build()

        fun request(manager: AudioManager, request: Any?): Int =
            manager.requestAudioFocus(request as AudioFocusRequest)

        fun abandon(manager: AudioManager, request: Any?): Int =
            manager.abandonAudioFocusRequest(request as AudioFocusRequest)
    }

    companion object {
        @JvmStatic
        fun create(
            gain: Int,
            attributes: AudioAttributes,
            listener: AudioManager.OnAudioFocusChangeListener,
            handler: Handler,
        ): AudioFocusCompat {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return AudioFocusCompat(Modern.build(gain, attributes, listener, handler), listener,
                    AudioManager.STREAM_MUSIC, gain)
            }
            // The legacy API calls back on the main looper; hop to the requested handler.
            val posting = AudioManager.OnAudioFocusChangeListener { change ->
                if (handler.looper.thread === Thread.currentThread()) listener.onAudioFocusChange(change)
                else handler.post { listener.onAudioFocusChange(change) }
            }
            @Suppress("DEPRECATION")
            val stream = legacyStreamFor(attributes)
            return AudioFocusCompat(null, posting, stream, gain)
        }

        private fun legacyStreamFor(attributes: AudioAttributes): Int = when (attributes.usage) {
            AudioAttributes.USAGE_VOICE_COMMUNICATION,
            AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING -> AudioManager.STREAM_VOICE_CALL
            AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> AudioManager.STREAM_RING
            AudioAttributes.USAGE_ALARM -> AudioManager.STREAM_ALARM
            else -> AudioManager.STREAM_MUSIC
        }
    }
}
