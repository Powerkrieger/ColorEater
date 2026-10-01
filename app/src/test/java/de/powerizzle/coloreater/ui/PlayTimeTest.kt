package de.powerizzle.coloreater.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class PlayTimeTest {
    private val minute = 60_000L
    private val hour = 60 * minute
    private val limit = 30 * minute

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

    private class MemoryStore : PlayTime.Store {
        var record: PlayTime.Record? = null
        override fun load() = record
        override fun save(record: PlayTime.Record) {
            this.record = record
        }
    }

    private var zone: ZoneId = ZoneOffset.UTC
    private val noon = LocalDateTime.of(2026, 10, 1, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val clock = FakeClock(noon)
    private val store = MemoryStore()

    /** A fresh instance each time, as after the app was closed. */
    private fun open() = PlayTime(clock, store, limit, zone = { zone })

    /** Plays the whole budget of the day. */
    private fun useUp(time: PlayTime) {
        time.counting = true
        clock.pass(limit)
        time.counting = false
        assertFalse(time.canStart)
    }

    @Test
    fun countsOnlyWhilePlaying() {
        val time = open()
        assertEquals(limit, time.remaining)
        time.counting = true
        clock.pass(10 * minute)
        assertEquals(limit - 10 * minute, time.remaining)
        time.counting = false
        clock.pass(hour)
        assertEquals(limit - 10 * minute, time.remaining)
        assertEquals(limit - 10 * minute, open().remaining)
        time.counting = true
        clock.pass(limit)
        assertEquals(0, time.remaining)
        assertFalse(time.canStart)
    }

    @Test
    fun nextDayBringsANewBudget() {
        useUp(open())
        clock.pass(24 * hour)
        assertEquals(limit, open().remaining)
    }

    @Test
    fun changingTheClockWhileOnDoesNothing() {
        val time = open()
        useUp(time)
        clock.wall += 3 * 24 * hour
        assertFalse(time.canStart)
        assertFalse(open().canStart)
        clock.wall -= 5 * 24 * hour
        assertFalse(open().canStart)
    }

    @Test
    fun settingTheClockBackAfterADayNeverBringsItBack() {
        useUp(open())
        clock.pass(24 * hour)
        useUp(open())
        clock.reboot(off = 0)
        clock.wall -= 24 * hour
        assertFalse(open().canStart)
    }

    @Test
    fun rebootWithTheClockBackDoesNotReturnTime() {
        useUp(open())
        clock.reboot(off = minute)
        clock.wall -= 3 * 24 * hour
        val time = open()
        assertFalse(time.canStart)
        // And the next real day still comes.
        clock.pass(24 * hour)
        assertTrue(time.canStart)
    }

    @Test
    fun networkTimeOutranksTheClock() {
        clock.network = noon
        useUp(open())
        clock.reboot(off = minute)
        clock.wall += 24 * hour
        clock.network = noon + limit + minute
        assertFalse(open().canStart)
    }

    @Test
    fun travelingWestDoesNotRepeatTheDay() {
        clock.wall = noon - 11 * hour // 01:00 UTC
        zone = ZoneOffset.ofHours(-10) // still the day before over there
        val time = open()
        useUp(time)
        zone = ZoneOffset.UTC
        assertTrue(time.canStart)
        useUp(time)
        zone = ZoneOffset.ofHours(-10)
        assertFalse(time.canStart)
    }
}
