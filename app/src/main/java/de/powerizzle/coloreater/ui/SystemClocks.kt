package de.powerizzle.coloreater.ui

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import java.time.DateTimeException

/** The real device clocks behind [PlayTime.Clock], shared by the daily budget and the [Cooldown]. */
internal class SystemClocks(context: Context) : PlayTime.Clock {
    override val wall get() = System.currentTimeMillis()
    override val elapsed get() = SystemClock.elapsedRealtime()
    override val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    // Asking the system for network time is a service call, so it is only asked once in a while.
    private var networkAt = Long.MIN_VALUE
    private var networkOffset: Long? = null

    override val network: Long?
        get() {
            if (Build.VERSION.SDK_INT < 33) return null
            val now = elapsed
            if (networkOffset == null && now - networkAt > NETWORK_RETRY) {
                networkAt = now
                networkOffset = try {
                    SystemClock.currentNetworkTimeClock().millis() - now
                } catch (_: DateTimeException) {
                    null
                }
            }
            return networkOffset?.plus(now)
        }

    private companion object {
        const val NETWORK_RETRY = 60_000L
    }
}
