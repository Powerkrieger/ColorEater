package de.powerizzle.coloreater.ui

import android.graphics.Bitmap
import android.graphics.Color
import de.powerizzle.coloreater.game.GameState
import de.powerizzle.coloreater.game.Level
import de.powerizzle.coloreater.game.Picture
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

    /** The volume most recently placed in each slot, kept while its ants are still working. */
    private val shownInSlot = arrayOfNulls<Volume>(state.slots.size)

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

    fun pick(queue: Int, now: Double) {
        if (!state.canPick(queue)) {
            rejectedAt[queue] = now
            return
        }
        // Always the first free slot: the level generator plays the same way, and the slot
        // order decides which volume eats first.
        val slot = state.freeSlot()
        shownInSlot[slot] = state.queues[queue].first()
        val removals = state.pick(queue, slot)
        for (removal in removals) notPickedUp.merge(removal.volume, 1, Int::plus)
        swarm.add(removals)
    }

    fun update(now: Double, geometry: AntSwarm.Geometry) {
        swarm.update(now, geometry) { removal ->
            val picture = level.picture
            bitmap.setPixel(picture.column(removal.pixel), picture.row(removal.pixel), Color.TRANSPARENT)
            notPickedUp.computeIfPresent(removal.volume) { _, n -> if (n > 1) n - 1 else null }
        }
        if (settledAt < 0 && state.status != Status.PLAYING && swarm.isIdle) settledAt = now
    }

    private fun pending(volume: Volume): Int? = notPickedUp[volume]

    /** What a slot shows: the volume, the count left to carry away and whether it is only finishing up. */
    class SlotView(val volume: Volume, val count: Int, val finishing: Boolean)

    fun slotView(slot: Int): SlotView? {
        val occupant = state.slots[slot]
        if (occupant != null) {
            return SlotView(occupant.volume, occupant.remaining + (pending(occupant.volume) ?: 0), false)
        }
        val shown = shownInSlot[slot] ?: return null
        val left = pending(shown) ?: return null
        return SlotView(shown, left, true)
    }
}
