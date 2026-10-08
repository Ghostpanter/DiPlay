package com.shilapi.xcertplay.compat

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothManager
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.hardware.usb.UsbManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Switch
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import java.util.Locale

/**
 * Small helpers for the API 22 (Android 5.1) fork. Every entry point keeps the
 * upstream behaviour on newer releases and only takes a legacy path below the
 * API level that introduced the modern call.
 */
object ApiCompat {
    private val legacyServiceNames: Map<Class<*>, String> = mapOf(
        ActivityManager::class.java to Context.ACTIVITY_SERVICE,
        AppOpsManager::class.java to Context.APP_OPS_SERVICE,
        AudioManager::class.java to Context.AUDIO_SERVICE,
        BluetoothManager::class.java to Context.BLUETOOTH_SERVICE,
        ClipboardManager::class.java to Context.CLIPBOARD_SERVICE,
        ConnectivityManager::class.java to Context.CONNECTIVITY_SERVICE,
        DisplayManager::class.java to Context.DISPLAY_SERVICE,
        LocationManager::class.java to Context.LOCATION_SERVICE,
        NotificationManager::class.java to Context.NOTIFICATION_SERVICE,
        SensorManager::class.java to Context.SENSOR_SERVICE,
        UsageStatsManager::class.java to Context.USAGE_STATS_SERVICE,
        UsbManager::class.java to Context.USB_SERVICE,
        WifiManager::class.java to Context.WIFI_SERVICE,
        WifiP2pManager::class.java to Context.WIFI_P2P_SERVICE,
        WindowManager::class.java to Context.WINDOW_SERVICE,
    )

    /** `Context.getSystemService(Class)` is API 23; map to the string name below it. */
    @JvmStatic
    fun <T : Any> systemService(context: Context, cls: Class<T>): T? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) return context.getSystemService(cls)
        val name = legacyServiceNames[cls] ?: return null
        return cls.cast(context.getSystemService(name))
    }

    /** Runtime permissions start at API 23; below it a declared permission is install-granted. */
    @JvmStatic
    fun hasPermission(context: Context, permission: String): Boolean =
        context.checkPermission(permission, Process.myPid(), Process.myUid()) ==
            PackageManager.PERMISSION_GRANTED

    /** Overlay permission is install-time below API 23. */
    @JvmStatic
    fun canDrawOverlays(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

    /** WRITE_SETTINGS is install-time below API 23. */
    @JvmStatic
    fun canWriteSystemSettings(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.System.canWrite(context) else true

    @JvmStatic
    @Suppress("DEPRECATION")
    fun longVersionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

    /** Flags to request signing certificates for [signatures]. */
    @JvmStatic
    @Suppress("DEPRECATION")
    val signingCertificateFlags: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun signatures(info: PackageInfo): Array<Signature>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        }

    /** `LocationManager.isLocationEnabled` is API 28. */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun isLocationEnabled(context: Context): Boolean? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return systemService(context, LocationManager::class.java)?.isLocationEnabled
        }
        return runCatching {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE) !=
                Settings.Secure.LOCATION_MODE_OFF
        }.getOrNull()
    }

    /** `Configuration.getLocales()` is API 24; older releases expose a single locale. */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun primaryLocale(configuration: Configuration): Locale? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) configuration.locales[0] else configuration.locale

    /** Copies the locale list (API 24+) or the single locale (older) from [source] to [target]. */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun copyLocales(target: Configuration, source: Configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) target.setLocales(source.locales)
        else target.locale = source.locale
    }

    /**
     * `ConnectivityManager.getActiveNetwork` is API 23. Below it, match the legacy active
     * NetworkInfo against the connected [Network]s (API 21).
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun activeNetwork(manager: ConnectivityManager): Network? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) return manager.activeNetwork
        val active = manager.activeNetworkInfo ?: return null
        return manager.allNetworks.firstOrNull { network ->
            manager.getNetworkInfo(network)?.let { it.type == active.type && it.isConnected } == true
        }
    }

    /** `WifiP2pManager.Channel.close()` is API 27; older channels are released with the manager. */
    @JvmStatic
    fun closeP2pChannel(channel: WifiP2pManager.Channel?) {
        if (channel == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) channel.close()
    }
}

/**
 * `View.setForeground` is API 23 (only `FrameLayout` had a foreground before). Below it the
 * focus ring / ripple overlay is skipped for other views; the view still works.
 */
@SuppressLint("NewApi") // FrameLayout.setForeground exists since API 1.
fun View.setForegroundCompat(drawable: Drawable?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) foreground = drawable
    else if (this is FrameLayout) (this as FrameLayout).setForeground(drawable)
}

/** `Switch.setThumbTintList` is API 23; tint the thumb drawable directly below it. */
fun Switch.setThumbTintCompat(tint: ColorStateList?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) thumbTintList = tint
    else thumbDrawable = thumbDrawable?.mutate()?.apply { setTintList(tint) }
}

/** `Switch.setTrackTintList` is API 23; tint the track drawable directly below it. */
fun Switch.setTrackTintCompat(tint: ColorStateList?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) trackTintList = tint
    else trackDrawable = trackDrawable?.mutate()?.apply { setTintList(tint) }
}

/** Kotlin shorthand for [ApiCompat.systemService]. */
fun <T : Any> Context.systemServiceCompat(cls: Class<T>): T? = ApiCompat.systemService(this, cls)
