package de.powerizzle.coloreater.ui

import android.graphics.Bitmap
import android.graphics.Color
import de.powerizzle.coloreater.game.GameState
import de.powerizzle.coloreater.game.Level
import de.powerizzle.coloreater.game.Picture
import de.powerizzle.coloreater.game.Removal
import de.powerizzle.coloreater.game.Status
import de.powerizzle.coloreater.game.Volume

/**
 * One attempt at a level. The [state] is always ahead of what is shown: pixels stay visible until
 * an ant picks them up, and slot counters only go down once pixels are actually carried away.
 */
class Session(val level: Level) {
    val state = GameState(level)
    val swarm = AntSwarm()
    val bitmap: Bitmap

    /** Pixels already eaten by the rules but not yet picked up by an ant, per volume. */
    private val notPickedUp = HashMap<Volume, Int>()

    private val screenSlots = ScreenSlots(
        state.slots.size,
        isLive = { volume -> state.slots.any { it?.volume === volume } },
        pending = { notPickedUp[it] ?: 0 },
    )

    /** The queues picked so far, in order; replaying them restores the level. */
    val picks = ArrayList<Int>()

    /** Time the queue was tapped without a free slot, for a short shake. */
    val rejectedAt = DoubleArray(state.queues.size) { -10.0 }

    /** Time the rules were finished and all ants were home, or -1. */
    var settledAt = -1.0
        private set

    init {
        val picture = level.picture
        bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
        for (index in 0 until picture.size) {
            val color = picture.colorAt(index)
            val argb = if (color == Picture.EMPTY) Color.TRANSPARENT else level.palette[color]
            bitmap.setPixel(picture.column(index), picture.row(index), argb)
        }
    }

    /** Returns false if the queue can't be picked right now. */
    fun pick(queue: Int, now: Double): Boolean {
        if (!state.canPick(queue)) {
            rejectedAt[queue] = now
            return false
        }
        val removals = place(queue)
        for (removal in removals) notPickedUp.merge(removal.volume, 1, Int::plus)
        swarm.add(removals.map { Removal(screenSlots.slotOf(it.volume), it.pixel, it.volume) })
        return true
    }

    /** Plays [picks] again at once, without ants, to continue a saved level. */
    fun restore(picks: List<Int>) {
        for (queue in picks) {
            if (!state.canPick(queue)) break
            for (removal in place(queue)) clearPixel(removal.pixel)
        }
        screenSlots.promote()
    }

    private fun place(queue: Int): List<Removal> {
        screenSlots.place(state.queues[queue].first())
        picks += queue
        // Always the first free slot in the rules: the level generator plays the same way, and
        // the slot order decides which volume eats first.
        return state.pick(queue, state.freeSlot())
    }

    private fun clearPixel(pixel: Int) {
        bitmap.setPixel(level.picture.column(pixel), level.picture.row(pixel), Color.TRANSPARENT)
    }

    fun update(now: Double, geometry: AntSwarm.Geometry) {
        swarm.update(now, geometry) { removal ->
            clearPixel(removal.pixel)
            notPickedUp.computeIfPresent(removal.volume) { _, n -> if (n > 1) n - 1 else null }
        }
        screenSlots.promote()
        if (settledAt < 0 && state.status != Status.PLAYING && swarm.isIdle) settledAt = now
    }

    /**
     * What a slot on screen shows: the volume, the count left to carry away, whether it is only
     * finishing up, and the volume waiting to take over.
     */
    class SlotView(val volume: Volume, val count: Int, val finishing: Boolean, val incoming: Volume?)

    fun slotView(slot: Int): SlotView? {
        val shown = screenSlots.shown(slot) ?: return null
        val occupant = state.slots.firstOrNull { it?.volume === shown }
        val count = (occupant?.remaining ?: 0) + (notPickedUp[shown] ?: 0)
        if (count == 0) return null
        return SlotView(shown, count, occupant == null, screenSlots.next(slot))
    }
}
