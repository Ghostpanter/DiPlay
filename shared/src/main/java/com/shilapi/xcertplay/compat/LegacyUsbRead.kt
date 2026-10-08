package com.shilapi.xcertplay.compat

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.os.Build
import android.os.SystemClock
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Synchronous bulk-IN reader for API 22-25, where `UsbRequest.queue(ByteBuffer)` and
 * `UsbDeviceConnection.requestWait(timeout)` (both API 26) are unavailable and the untimed
 * `requestWait()` could block forever. Before API 28 one bulk transfer is capped at 16 KiB,
 * so reads are limited to that; callers already reassemble a byte stream from chunks.
 *
 * `bulkTransfer` reports both a timeout and an I/O error as -1. A -1 that returns well before
 * the timeout is treated as an error; [QUICK_FAILURE_LIMIT] consecutive ones fail the session.
 */
class LegacyUsbRead(
    private val connection: UsbDeviceConnection,
    private val endpoint: UsbEndpoint,
) {
    private var quickFailures = 0

    sealed class Result {
        data class Data(val length: Int) : Result()
        object Timeout : Result()
        data class Failed(val code: Int) : Result()
    }

    fun read(buffer: ByteArray, timeoutMillis: Long): Result {
        val length = minOf(buffer.size, MAX_LEGACY_BULK_BYTES)
        val timeout = timeoutMillis.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        val started = SystemClock.elapsedRealtime()
        val count = connection.bulkTransfer(endpoint, buffer, length, timeout)
        if (count >= 0) {
            quickFailures = 0
            return if (count == 0) Result.Timeout else Result.Data(count)
        }
        val elapsed = SystemClock.elapsedRealtime() - started
        if (elapsed * 2 >= timeout) {
            quickFailures = 0
            return Result.Timeout
        }
        quickFailures += 1
        return if (quickFailures >= QUICK_FAILURE_LIMIT) Result.Failed(count) else Result.Timeout
    }

    companion object {
        const val MAX_LEGACY_BULK_BYTES = 16_384
        const val QUICK_FAILURE_LIMIT = 5

        /** True when queued requests with a timed `requestWait` (API 26) are available. */
        @JvmStatic
        @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
        val queuedRequestsAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}
