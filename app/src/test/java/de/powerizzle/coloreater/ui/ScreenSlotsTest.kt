package de.powerizzle.coloreater.ui

import de.powerizzle.coloreater.game.Difficulty
import de.powerizzle.coloreater.game.GameState
import de.powerizzle.coloreater.game.LevelGenerator
import de.powerizzle.coloreater.game.Levels
import de.powerizzle.coloreater.game.Status
import de.powerizzle.coloreater.game.Volume
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class ScreenSlotsTest {
    /** Plays like Session does, with ants that pick up pixels at random speed, and taps at random times. */
    @Test
    fun fastTappingNeverRunsOutOfSlotsOnScreen() {
        val random = Random(1)
        for (difficulty in Difficulty.entries) for (number in 1..20) {
            val level = LevelGenerator.generate(Levels.config(number, difficulty))
            repeat(10) {
                val state = GameState(level)
                val pending = HashMap<Volume, Int>()
                val ants = ArrayDeque<Volume>()
                val slots = ScreenSlots(
                    state.slots.size,
                    isLive = { volume -> state.slots.any { it?.volume === volume } },
                    pending = { pending[it] ?: 0 },
                )
                while (state.status == Status.PLAYING || ants.isNotEmpty()) {
                    val pickable = state.queues.indices.filter { state.canPick(it) }
                    if (pickable.isNotEmpty() && (ants.isEmpty() || random.nextInt(3) == 0)) {
                        val queue = pickable.random(random)
                        slots.place(state.queues[queue].first())
                        for (removal in state.pick(queue)) {
                            slots.slotOf(removal.volume)
                            pending.merge(removal.volume, 1, Int::plus)
                            ants += removal.volume
                        }
                    } else {
                        repeat(random.nextInt(1, 30)) {
                            val volume = ants.removeFirstOrNull() ?: return@repeat
                            pending.computeIfPresent(volume) { _, n -> if (n > 1) n - 1 else null }
                        }
                        slots.promote()
                    }
                    // Every volume in the rules has a slot on screen.
                    for (occupant in state.slots.filterNotNull()) slots.slotOf(occupant.volume)
                }
                slots.promote()
                if (state.status == Status.WON) {
                    assertEquals(null, state.slots.indices.firstNotNullOfOrNull { slots.shown(it) })
                }
            }
        }
    }
}
