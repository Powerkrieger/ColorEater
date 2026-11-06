package de.powerizzle.coloreater.ui

import android.content.Context
import androidx.core.content.edit
import kotlin.math.max

/**
 * A wait that must pass after each won level before the next one may start, which changing the clock
 * or rebooting can't skip.
 *
 * The wait counts against a trusted time that only ever moves forward at the real rate: network time
 * when the system has it, otherwise the uptime clock within a single boot. A reboot without network
 * time is not credited at all, so the wait can only ever get shorter by real time actually passing.
 */
class Cooldown(
    private val clock: PlayTime.Clock,
    private val store: Store,
    private val duration: Long,
) {
    interface Store {
        fun load(): Record?

        /** Saves [record], or clears the stored wait when it is null. */
        fun save(record: Record?)
    }

    /** The wait ends at trusted time [until]; [trusted] is the trusted time as of uptime [elapsed] in [boot]. */
    data class Record(val until: Long, val trusted: Long, val elapsed: Long, val boot: Int)

    private var record = store.load()

    /** Milliseconds left on the wait; 0 when the next level may start. */
    val remaining: Long
        get() {
            val now = record?.let(::refresh)?.also { record = it } ?: return 0
            return (now.until - now.trusted).coerceAtLeast(0)
        }

    /** Whether a wait is in effect right now. */
    val active get() = remaining > 0

    /** Starts a fresh wait of [duration] from now, replacing any wait still running. */
    fun start() {
        if (duration <= 0) return
        val base = record?.let(::refresh) ?: (clock.network ?: clock.wall).let { now ->
            Record(now, now, clock.elapsed, clock.boot)
        }
        record = base.copy(until = base.trusted + duration)
        store.save(record)
    }

    /** Ends the wait early. */
    fun clear() {
        record = null
        store.save(null)
    }

    private fun refresh(r: Record): Record {
        val elapsed = clock.elapsed
        val boot = clock.boot
        val sameBoot = boot == r.boot && elapsed >= r.elapsed
        // Within a boot the uptime clock can't be tampered with. A reboot without network time could
        // hide how long the phone was off, so it is not credited: the wait simply resumes counting.
        val advanced = clock.network ?: if (sameBoot) r.trusted + (elapsed - r.elapsed) else r.trusted
        return r.copy(trusted = max(r.trusted, advanced), elapsed = elapsed, boot = boot)
    }

    companion object {
        fun of(context: Context, minutes: Int) =
            Cooldown(SystemClocks(context), PreferenceStore(context), minutes * 60_000L)
    }

    private class PreferenceStore(context: Context) : Store {
        private val prefs = context.getSharedPreferences("cooldown", Context.MODE_PRIVATE)

        override fun load(): Record? {
            if (!prefs.contains("until")) return null
            return Record(
                prefs.getLong("until", 0),
                prefs.getLong("trusted", 0),
                prefs.getLong("elapsed", 0),
                prefs.getInt("boot", -1),
            )
        }

        override fun save(record: Record?) = prefs.edit {
            if (record == null) {
                clear()
            } else {
                putLong("until", record.until)
                putLong("trusted", record.trusted)
                putLong("elapsed", record.elapsed)
                putInt("boot", record.boot)
            }
        }
    }
}
