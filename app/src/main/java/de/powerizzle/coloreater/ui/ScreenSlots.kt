package de.powerizzle.coloreater.ui

import de.powerizzle.coloreater.game.Volume

/**
 * Which slot on screen shows which volume. Slots on screen are separate from the slots of the
 * rules: the rules always use their first free slot (the generator plays the same way), but on
 * screen a new volume goes to a slot whose ants are all home, so it never shares a slot with a
 * volume that is still finishing. If every slot still has ants out, it waits in the slot with the
 * fewest volumes ahead of it, and of those the one closest to done.
 *
 * A volume is live while it is in a slot of the rules; [pending] counts its pixels that ants
 * have not picked up yet. A volume is done when it is neither.
 */
class ScreenSlots(
    size: Int,
    private val isLive: (Volume) -> Boolean,
    private val pending: (Volume) -> Int,
) {
    /** Per slot on screen: the volume shown, then the ones waiting to take over. */
    private val slots = Array(size) { ArrayDeque<Volume>() }
    private val slotOf = HashMap<Volume, Int>()

    fun slotOf(volume: Volume): Int = slotOf.getValue(volume)

    fun shown(slot: Int): Volume? = slots[slot].firstOrNull()

    /** The volume that takes over once the one shown is done. */
    fun next(slot: Int): Volume? = slots[slot].getOrNull(1)

    /**
     * Picks the slot on screen for a volume that is about to go into the rules. There always is a
     * slot without a live volume: the rules have a free slot, so there are fewer live volumes
     * than slots.
     */
    fun place(volume: Volume): Int {
        promote()
        val free = slots.indices.filter { slot -> slots[slot].none(isLive) }.ifEmpty { slots.indices.toList() }
        val slot = free.minWith(compareBy({ slots[it].size }, { slots[it].sumOf(pending) }))
        slots[slot].addLast(volume)
        slotOf[volume] = slot
        return slot
    }

    /** Hands every slot whose volume is done to the volume waiting behind it. */
    fun promote() {
        for (queue in slots) {
            while (queue.isNotEmpty() && !isLive(queue.first()) && pending(queue.first()) == 0) {
                slotOf.remove(queue.removeFirst())
            }
        }
    }
}
