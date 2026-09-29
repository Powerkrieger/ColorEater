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

    /**
     * Slots on screen are separate from the slots of the rules. The rules always use their first
     * free slot (the generator plays the same way), but on screen a new volume goes to a slot
     * whose ants are all home, so it never shares a slot with a volume that is still finishing.
     */
    private val shownInSlot = arrayOfNulls<Volume>(state.slots.size)

    /** A volume waiting to take over a slot on screen once the ants of the one before are home. */
    private val incoming = arrayOfNulls<Volume>(state.slots.size)

    /** The slot on screen of every volume that is still in a slot of the rules or has ants out. */
    private val screenSlot = HashMap<Volume, Int>()

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
        promote()
        val volume = state.queues[queue].first()
        val slot = screenSlotFor()
        if (shownInSlot[slot] == null) shownInSlot[slot] = volume else incoming[slot] = volume
        screenSlot[volume] = slot
        // Always the first free slot in the rules: the level generator plays the same way, and
        // the slot order decides which volume eats first.
        val removals = state.pick(queue, state.freeSlot())
        for (removal in removals) notPickedUp.merge(removal.volume, 1, Int::plus)
        swarm.add(removals.map { Removal(screenSlot.getValue(it.volume), it.pixel, it.volume) })
    }

    /** Hands every slot on screen whose volume is done to the volume waiting for it. */
    private fun promote() {
        for (slot in shownInSlot.indices) {
            val shown = shownInSlot[slot] ?: continue
            if (!isDone(shown)) continue
            screenSlot.remove(shown)
            shownInSlot[slot] = incoming[slot]
            incoming[slot] = null
        }
    }

    private fun isLive(volume: Volume) = state.slots.any { it?.volume === volume }

    /** No longer in the rules and all its pixels picked up. */
    private fun isDone(volume: Volume) = !isLive(volume) && pending(volume) == null

    /**
     * The first empty slot on screen; if there is none, the finishing slot with the fewest pixels
     * left to pick up. There always is one: the rules have a free slot, so there are fewer live
     * volumes than slots.
     */
    private fun screenSlotFor(): Int {
        val slots = shownInSlot.indices
        slots.firstOrNull { shownInSlot[it] == null }?.let { return it }
        return slots.filter { incoming[it] == null && !isLive(shownInSlot[it]!!) }
            .minBy { pending(shownInSlot[it]!!) ?: 0 }
    }

    fun update(now: Double, geometry: AntSwarm.Geometry) {
        swarm.update(now, geometry) { removal ->
            val picture = level.picture
            bitmap.setPixel(picture.column(removal.pixel), picture.row(removal.pixel), Color.TRANSPARENT)
            notPickedUp.computeIfPresent(removal.volume) { _, n -> if (n > 1) n - 1 else null }
        }
        promote()
        if (settledAt < 0 && state.status != Status.PLAYING && swarm.isIdle) settledAt = now
    }

    private fun pending(volume: Volume): Int? = notPickedUp[volume]

    /**
     * What a slot on screen shows: the volume, the count left to carry away, whether it is only
     * finishing up, and the volume waiting to take over.
     */
    class SlotView(val volume: Volume, val count: Int, val finishing: Boolean, val incoming: Volume?)

    fun slotView(slot: Int): SlotView? {
        val shown = shownInSlot[slot] ?: return null
        val occupant = state.slots.firstOrNull { it?.volume === shown }
        val count = (occupant?.remaining ?: 0) + (pending(shown) ?: 0)
        if (count == 0) return null
        return SlotView(shown, count, occupant == null, incoming[slot])
    }
}
