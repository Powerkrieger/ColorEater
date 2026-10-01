package de.powerizzle.coloreater.ui

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.edit
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max

/**
 * A daily play time budget that changing the clock can't extend.
 *
 * Time played is measured with the uptime clock, which the user can't change. Which day it is comes
 * from a trusted time: network time when the system has it, otherwise the wall clock read once per
 * boot and from then on carried forward with the uptime clock, so clock changes while the phone is on
 * are ignored. The current day never goes backward, so setting the clock back, or traveling west,
 * never brings back a day that was already played.
 */
class PlayTime(
    private val clock: Clock,
    private val store: Store,
    private val limit: Long,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    interface Clock {
        /** Wall clock in epoch milliseconds; the user can set it to anything. */
        val wall: Long

        /** Milliseconds since boot, including sleep. */
        val elapsed: Long

        /** Changes with every boot; -1 if unknown. */
        val boot: Int

        /** Epoch milliseconds from the network, if the system has it. */
        val network: Long?
    }

    interface Store {
        fun load(): Record?
        fun save(record: Record)
    }

    /** Milliseconds [used] on [day] (epoch day), and the trusted time as of uptime [elapsed] in [boot]. */
    data class Record(val day: Long, val used: Long, val trusted: Long, val elapsed: Long, val boot: Int)

    private var record = store.load() ?: (clock.network ?: clock.wall).let { now ->
        Record(dayOf(now), 0, now, clock.elapsed, clock.boot)
    }
    private var countingSince = -1L

    /** Whether time is being played right now. */
    var counting: Boolean
        get() = countingSince >= 0
        set(value) {
            if (value == counting) return
            flush()
            countingSince = if (value) record.elapsed else -1
        }

    /** Play time left today, in milliseconds. */
    val remaining: Long
        get() {
            val now = refresh().also { record = it }
            val running = if (counting) now.elapsed - countingSince else 0
            return (limit - now.used - running).coerceAtLeast(0)
        }

    /** Whether a new game may be started today. */
    val canStart get() = remaining > 0

    /** Books the time played so far and saves. */
    fun flush() {
        var now = refresh()
        if (counting) {
            now = now.copy(used = now.used + now.elapsed - countingSince)
            countingSince = now.elapsed
        }
        record = now
        store.save(now)
    }

    private fun refresh(): Record {
        val elapsed = clock.elapsed
        val boot = clock.boot
        val sameBoot = boot == record.boot && elapsed >= record.elapsed
        val trusted = clock.network ?: if (sameBoot) {
            record.trusted + (elapsed - record.elapsed)
        } else {
            // How long the phone was off is unknown, so the wall clock has to be believed, but only forward.
            max(record.trusted, clock.wall)
        }
        val day = max(record.day, dayOf(trusted))
        return Record(day, if (day == record.day) record.used else 0, trusted, elapsed, boot)
    }

    private fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay()

    companion object {
        private const val NETWORK_RETRY = 60_000L

        fun of(context: Context, minutes: Int) =
            PlayTime(SystemClocks(context), PreferenceStore(context), minutes * 60_000L)
    }

    private class SystemClocks(context: Context) : Clock {
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
    }

    private class PreferenceStore(context: Context) : Store {
        private val prefs = context.getSharedPreferences("playtime", Context.MODE_PRIVATE)

        override fun load(): Record? {
            if (!prefs.contains("day")) return null
            return Record(
                prefs.getLong("day", 0),
                prefs.getLong("used", 0),
                prefs.getLong("trusted", 0),
                prefs.getLong("elapsed", 0),
                prefs.getInt("boot", -1),
            )
        }

        override fun save(record: Record) = prefs.edit {
            putLong("day", record.day)
            putLong("used", record.used)
            putLong("trusted", record.trusted)
            putLong("elapsed", record.elapsed)
            putInt("boot", record.boot)
        }
    }
}
