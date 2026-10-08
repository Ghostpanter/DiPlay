# DiPlay on Android 5.1 (API 22) — fork notes

Branch `minsdk-22` of `Ghostpanter/DiPlay` lowers `minSdk` of `:mobile`, `:common` and `:shared`
from 28 to 22 (Android 5.1). `compileSdk`/`targetSdk` stay at 37. Not intended for upstream.

## Build changes

| Item | Upstream | This fork | Why |
| --- | --- | --- | --- |
| `minSdk` (`mobile`, `common`, `shared`) | 28 | 22 | Android 5.1 head units |
| NDK `APP_PLATFORM` | android-28 | android-22 | JNI libs load on 5.1 |
| Compose BOM | 2026.02.01 (Compose 1.10, minSdk 23) | 2025.11.01 (Compose 1.9.5, material3 1.4.0) | last Compose line that supports API 21/22 |
| `androidx.core` | 1.19.0 (minSdk 23) | 1.17.0 | minSdk 21 |
| Media3 | 1.11.1 (minSdk 23) | 1.8.0 | minSdk 21 |
| Core library desugaring | off | on (`desugar_jdk_libs` 2.1.5) | `java.time`, `java.util.Base64`, Java 8 collection defaults |
| `versionName` | 0.2.14 | 0.2.14-api22 | |

No `tools:overrideLibrary` is used; every dependency declares minSdk ≤ 22.
`./gradlew :mobile:lintDebug` with `checkOnly NewApi` + `checkDependencies` reports 0 issues.

## Runtime fallbacks (API level where the modern path starts)

Helpers live in `shared/src/main/java/com/shilapi/xcertplay/compat/`.

| Area | Modern API | Below it |
| --- | --- | --- |
| `Context.getSystemService(Class)` | 23 | name-based lookup (`ApiCompat.systemService`) |
| Runtime permission checks | 23 | `checkCallingOrSelfPermission` (install-time grants) |
| `Settings.canDrawOverlays` / `Settings.System.canWrite` | 23 | treated as granted (install-time) |
| Audio focus (`AudioFocusRequest`) | 26 | legacy `requestAudioFocus(listener, stream, gain)` |
| `AudioTrack.Builder` / `AudioRecord.Builder` | 23 | API 21 `AudioTrack(AudioAttributes, AudioFormat, …)` / classic `AudioRecord` constructor |
| Blocking `write`/`read` overloads | 23 | default blocking overloads |
| `AudioTrack.getUnderrunCount` | 24 | 0 → no underrun-triggered rebuffering |
| `AudioRecord.getTimestamp` | 24 | fixed latency estimate for echo cancellation |
| `AudioTrack.getBufferSizeInFrames`, routed device | 23 | requested size / "unknown" in diagnostics |
| `MediaCodec.setOutputSurface` | 23 | decoder is released and reconfigured on surface change; no off-screen parking |
| USB read (`UsbRequest.queue(ByteBuffer)` + `requestWait(timeout)`) | 26 | synchronous `bulkTransfer` with timeout, ≤16 KiB per read (`LegacyUsbRead`) |
| `WifiP2pManager.Channel.close` | 27 | not called (channel released with the manager) |
| LocalOnlyHotspot | 26 | mode refused with a clear message (it is not user-selectable anyway) |
| `LocationManager.isLocationEnabled` | 28 | `Settings.Secure.LOCATION_MODE` |
| `ConnectivityManager.getActiveNetwork` | 23 | match `activeNetworkInfo` against `allNetworks` |
| `NetworkRequest.Builder.clearCapabilities` | 30 | default capability set |
| `SSLParameters.setEndpointIdentificationAlgorithm` | 24 | skipped (old SSLEngines never verify hostnames) |
| Notification channel / `Notification.Builder(ctx, channel)` | 26 | legacy builder with low priority |
| `startForegroundService` | 26 | `startService` + `startForeground` |
| `Configuration.getLocales/setLocales` | 24 | single `Configuration.locale` |
| `View.setForeground` | 23 | only `FrameLayout`s get the focus ring/ripple overlay |
| `Switch` thumb/track tint | 23 | tint the drawables directly |
| `ProgressBar.setMinHeight` | 29 | `View.setMinimumHeight` |
| `PackageInfo.signingInfo` / `longVersionCode` | 28 | `signatures` / `versionCode` |
| `CompletableFuture`, `ThreadLocal.withInitial`, `ConcurrentHashMap.newKeySet`, `LinkedBlockingQueue.removeIf`, `HashMap.putIfAbsent`, `MacAddress` | 24–28 | plain JDK 7 equivalents |

## Known limitations on Android 5.1

* Not tested on a real Android 5.1 head unit or emulator.
* Wireless CarPlay depends on Wi-Fi Direct (legacy group API) or the car's own hotspot / an existing
  Wi-Fi network; Android's LocalOnlyHotspot does not exist before 8.0.
* Launcher map embedding (`MapEmbedService`) and DiPlay Home need Android 11+, as upstream.
* Vector icons that use `fillType="evenOdd"` render with the non-zero rule before API 24.
* The source APK, like upstream, contains no MFi authentication identity.
