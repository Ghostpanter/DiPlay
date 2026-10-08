package com.shilapi.xcertplay.compat

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.Build

/**
 * AudioTrack/AudioRecord helpers for API 22. The builders and blocking-mode
 * write/read overloads are API 23; the underrun counter and timestamps are API 24.
 */
object AudioTrackCompat {
    /** Streaming [AudioTrack] from attributes + format (Builder on 23+, API 21 constructor below). */
    @JvmStatic
    fun buildStreamTrack(attributes: AudioAttributes, format: AudioFormat, bufferBytes: Int): AudioTrack =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferBytes)
                .build()
        } else {
            AudioTrack(attributes, format, bufferBytes, AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE)
        }

    /** Actual buffer size on 23+; below it the requested size is the best available estimate. */
    @JvmStatic
    fun bufferSizeInFrames(track: AudioTrack, requestedBytes: Int, frameBytes: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) track.bufferSizeInFrames
        else if (frameBytes > 0) requestedBytes / frameBytes else -1

    /** Underrun counter is API 24; older releases report 0 (no underrun-driven rebuffering). */
    @JvmStatic
    fun underrunCount(track: AudioTrack): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) track.underrunCount else 0

    @JvmStatic
    fun routedDeviceType(track: AudioTrack): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) track.routedDevice?.type else null

    @JvmStatic
    fun routedDeviceType(record: AudioRecord): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) record.routedDevice?.type else null

    /** Blocking write; MODE_STREAM writes are blocking by default before API 23. */
    @JvmStatic
    fun writeBlocking(track: AudioTrack, data: ByteArray, offset: Int, size: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            track.write(data, offset, size, AudioTrack.WRITE_BLOCKING)
        } else {
            track.write(data, offset, size)
        }

    @JvmStatic
    fun writeBlocking(track: AudioTrack, data: ShortArray, offset: Int, size: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            track.write(data, offset, size, AudioTrack.WRITE_BLOCKING)
        } else {
            track.write(data, offset, size)
        }

    /** Blocking read; reads are blocking by default before API 23. */
    @JvmStatic
    fun readBlocking(record: AudioRecord, data: ByteArray, offset: Int, size: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            record.read(data, offset, size, AudioRecord.READ_BLOCKING)
        } else {
            record.read(data, offset, size)
        }

    @JvmStatic
    fun readBlocking(record: AudioRecord, data: ShortArray, offset: Int, size: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            record.read(data, offset, size, AudioRecord.READ_BLOCKING)
        } else {
            record.read(data, offset, size)
        }

    /** AudioRecord.Builder is API 23; the classic constructor takes the same parameters. */
    @JvmStatic
    fun buildRecord(source: Int, sampleRate: Int, channelMask: Int, encoding: Int, bufferBytes: Int): AudioRecord =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .setEncoding(encoding)
                        .build(),
                )
                .setBufferSizeInBytes(bufferBytes)
                .build()
        } else {
            AudioRecord(source, sampleRate, channelMask, encoding, bufferBytes)
        }
}
