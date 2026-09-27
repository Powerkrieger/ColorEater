package de.powerizzle.coloreater.game

/**
 * A pack of [count] pixels of one [color] that can be eaten. Compared by identity.
 * A [hidden] volume only shows its color and count once it reaches the front of its queue.
 */
class Volume(val color: Int, val count: Int, val hidden: Boolean = false)

class SlotVolume(val volume: Volume, var remaining: Int = volume.count)

/** One pixel ([pixel] index in the picture) eaten by the volume in [slot]. */
class Removal(val slot: Int, val pixel: Int, val volume: Volume)

enum class Status { PLAYING, WON, LOST }

/**
 * The rules of the game, without any timing: picking a volume immediately eats everything that
 * can be eaten. The UI replays the returned [Removal]s with ants.
 */
class GameState private constructor(
    val picture: Picture,
    val queues: List<ArrayDeque<Volume>>,
    val slots: Array<SlotVolume?>,
    status: Status,
) {
    constructor(level: Level, slotCount: Int = level.slots) : this(
        level.picture.copy(),
        level.queues.map { ArrayDeque(it) },
        arrayOfNulls(slotCount),
        Status.PLAYING,
    ) {
        updateStatus()
    }

    var status: Status = status
        private set

    fun freeSlot(): Int = slots.indexOfFirst { it == null }

    fun canPick(queue: Int) = status == Status.PLAYING && queues[queue].isNotEmpty() && freeSlot() >= 0

    /** Moves the front volume of [queue] into [slot] and eats until nothing more can be eaten. */
    fun pick(queue: Int, slot: Int = freeSlot()): List<Removal> {
        check(canPick(queue)) { "Cannot pick from queue $queue" }
        val removals = place(queues[queue].first(), slot)
        queues[queue].removeFirst()
        updateStatus()
        return removals
    }

    /** Puts [volume] into [slot] without taking it from a queue and eats; used to build levels. */
    fun place(volume: Volume, slot: Int = freeSlot()): List<Removal> {
        check(slots[slot] == null) { "Slot $slot is occupied" }
        slots[slot] = SlotVolume(volume)
        return eat()
    }

    fun occupiedSlots() = slots.count { it != null }

    /** Pixels of [color] that volumes in slots will still eat. */
    fun promised(color: Int) = slots.sumOf { if (it != null && it.volume.color == color) it.remaining else 0 }

    /** All volumes in slots take turns eating one exposed pixel of their color. */
    private fun eat(): List<Removal> {
        val removals = ArrayList<Removal>()
        var progress = true
        while (progress) {
            progress = false
            for (slot in slots.indices) {
                val occupant = slots[slot] ?: continue
                val pixel = picture.nextExposed(occupant.volume.color)
                if (pixel < 0) continue
                picture.remove(pixel)
                removals += Removal(slot, pixel, occupant.volume)
                occupant.remaining--
                if (occupant.remaining == 0) slots[slot] = null
                progress = true
            }
        }
        return removals
    }

    private fun updateStatus() {
        status = when {
            picture.isEmpty() -> Status.WON
            slots.all { it != null } -> Status.LOST
            queues.all { it.isEmpty() } -> Status.LOST
            else -> Status.PLAYING
        }
    }

    fun copy() = GameState(
        picture.copy(),
        queues.map { ArrayDeque(it) },
        Array(slots.size) { i -> slots[i]?.let { SlotVolume(it.volume, it.remaining) } },
        status,
    )

    /** Identifies how far each queue has been played (8 bits per queue). */
    fun queueKey(): Long = queues.fold(0L) { key, queue -> (key shl 8) or queue.size.toLong() }
}
