package de.powerizzle.coloreater.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CooldownTest {
    private val minute = 60_000L
    private val duration = 10 * minute

    private class FakeClock(override var wall: Long) : PlayTime.Clock {
        override var elapsed = 1_000L
        override var boot = 1
        override var network: Long? = null

        fun pass(millis: Long) {
            wall += millis
            elapsed += millis
            network = network?.plus(millis)
        }

        fun reboot(off: Long) {
            wall += off
            network = null
            elapsed = 500L
            boot++
        }
    }

    private class MemoryStore : Cooldown.Store {
        var record: Cooldown.Record? = null
        override fun load() = record
        override fun save(record: Cooldown.Record?) {
            this.record = record
        }
    }

    private val clock = FakeClock(1_000_000L)
    private val store = MemoryStore()

    /** A fresh instance each time, as after the app was closed. */
    private fun open() = Cooldown(clock, store, duration)

    @Test
    fun noWaitUntilStarted() {
        val cooldown = open()
        assertFalse(cooldown.active)
        assertEquals(0, cooldown.remaining)
    }

    @Test
    fun countsDownWithUptime() {
        val cooldown = open()
        cooldown.start()
        assertTrue(cooldown.active)
        assertEquals(duration, cooldown.remaining)
        clock.pass(4 * minute)
        assertEquals(duration - 4 * minute, cooldown.remaining)
        clock.pass(6 * minute)
        assertFalse(cooldown.active)
        assertEquals(0, cooldown.remaining)
    }

    @Test
    fun survivesClosingTheApp() {
        open().start()
        clock.pass(3 * minute)
        // Reopened from the saved record, the wait continues where it was.
        assertEquals(duration - 3 * minute, open().remaining)
    }

    @Test
    fun changingTheClockForwardDoesNotSkipIt() {
        val cooldown = open()
        cooldown.start()
        clock.wall += 5 * duration // only the wall clock jumps; no real time passes
        assertTrue(cooldown.active)
        assertEquals(duration, cooldown.remaining)
    }

    @Test
    fun rebootDoesNotSkipIt() {
        open().start()
        // Reboot and set the clock far forward: the off time is not credited.
        clock.reboot(off = 5 * duration)
        val cooldown = open()
        assertTrue(cooldown.active)
        assertEquals(duration, cooldown.remaining)
        // But it still counts down by real uptime after the reboot, so it never gets stuck.
        clock.pass(duration)
        assertFalse(cooldown.active)
    }

    @Test
    fun networkTimeCountsTheWaitDown() {
        clock.network = 2_000_000L
        val cooldown = open()
        cooldown.start()
        assertEquals(duration, cooldown.remaining)
        // Even across a reboot, real time known from the network ends the wait.
        clock.reboot(off = 0)
        clock.network = 2_000_000L + duration
        assertFalse(open().active)
    }

    @Test
    fun clearEndsItEarly() {
        val cooldown = open()
        cooldown.start()
        cooldown.clear()
        assertFalse(cooldown.active)
        assertFalse(open().active) // and it stays cleared after reopening
    }
}
