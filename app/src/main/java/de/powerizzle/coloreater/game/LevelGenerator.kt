package de.powerizzle.coloreater.game

import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/**
 * Cuts a picture into volumes and deals them onto the queues.
 *
 * The generator plays an intended solution itself: most volumes can be eaten right away, but now
 * and then it adds a "blocker" (its color is still buried, or it is bigger than what can be
 * reached) that has to wait in a slot. The intended play never fills the last slot, so taking the
 * volumes in that order always wins. Dealing the order onto the queues keeps it playable, since
 * each volume is at the front of its queue when its turn comes.
 *
 * The solver then finds the fewest slots the level can be won with, and the player gets exactly
 * that many plus the configured slack. Of several attempts the one with the most traps is kept:
 * ideally the obvious greedy play loses and random play mostly loses too.
 */
object LevelGenerator {
    private const val ATTEMPTS = 10
    private const val RANDOM_PLAYS = 16

    /**
     * Search limit when looking for the fewest slots. If the solver gives up, the level gets
     * more slots, which is always safe. Higher values barely change levels but cost up to 10x
     * the time, which a debug build on a phone feels badly.
     */
    private const val SOLVER_BUDGET = 5_000

    fun generate(config: LevelConfig): Level {
        val (picture, palette) = config.art.toPicture(config.scale)
        val empty = Level(config, picture, palette, emptyList(), config.maxSlots)
        val random = Random(config.seed)
        var best: Level? = null
        var bestScore = -1.0
        for (attempt in 0 until ATTEMPTS) {
            val order = intendedOrder(empty, config.blockers, random) ?: continue
            val queues = deal(order, config.queues, config.mystery, random)
            val need = fewestSlots(Level(config, picture, palette, queues, config.maxSlots))
            val level = Level(config, picture, palette, queues, minOf(config.maxSlots, need + config.slack))
            // Slots needed only breaks ties; tightness itself comes from handing out that many.
            val score = trapScore(level, random) + need / 100.0
            if (score > bestScore) {
                best = level
                bestScore = score
            }
        }
        return best ?: run {
            val queues = deal(requireNotNull(intendedOrder(empty, 0.0, random)), config.queues, config.mystery, random)
            val need = fewestSlots(Level(config, picture, palette, queues, config.maxSlots))
            Level(config, picture, palette, queues, minOf(config.maxSlots, need + config.slack))
        }
    }

    /** 1 if greedy play loses, plus the share of random plays that lose. */
    private fun trapScore(level: Level, random: Random): Double {
        val greedyLoses = if (play(level) { state -> greedyPick(state) } == Status.LOST) 1.0 else 0.0
        val randomLosses = (0 until RANDOM_PLAYS).count { _ ->
            play(level) { state -> state.queues.indices.filter { state.canPick(it) }.random(random) } == Status.LOST
        }
        return greedyLoses + randomLosses.toDouble() / RANDOM_PLAYS
    }

    private inline fun play(level: Level, choose: (GameState) -> Int): Status {
        val state = GameState(level)
        while (state.status == Status.PLAYING) state.pick(choose(state))
        return state.status
    }

    /** What a hasty player does: take whatever eats the most pixels right now. */
    private fun greedyPick(state: GameState): Int =
        state.queues.indices.filter { state.canPick(it) }.maxBy { state.copy().pick(it).size }

    /** The intended solution always fits into maxSlots, so that is the upper bound. */
    private fun fewestSlots(level: Level): Int =
        (1 until level.config.maxSlots).firstOrNull { slots ->
            Solver.solve(GameState(level, slots), SOLVER_BUDGET).outcome == Outcome.SOLVED
        } ?: level.config.maxSlots

    /** Plays the level volume by volume; returns the volumes in the order they were used. */
    private fun intendedOrder(empty: Level, blockers: Double, random: Random): List<Volume>? {
        val config = empty.config
        val state = GameState(empty)
        val order = ArrayList<Volume>()
        while (!state.picture.isEmpty()) {
            // A blocker may only take a slot if one stays free for the volumes that unblock it.
            val canBlock = state.occupiedSlots() <= config.maxSlots - 2
            val volume = (if (canBlock && random.nextDouble() < blockers) blocker(state, config, random) else null)
                ?: eatable(state, config, random)
            state.place(volume)
            order += volume
            if (state.occupiedSlots() >= config.maxSlots) return null
        }
        return order
    }

    /** A volume that is completely eaten as soon as it is placed. */
    private fun eatable(state: GameState, config: LevelConfig, random: Random): Volume {
        val picture = state.picture
        val weights = IntArray(picture.colorCount) { picture.exposedCount(it) }
        var roll = random.nextInt(weights.sum())
        val color = weights.indices.first { roll -= weights[it]; roll < 0 }
        val size = volumeSize(config.minVolume, config.maxVolume, random)
        return Volume(color, reach(picture, color, size))
    }

    /** A volume that cannot be finished yet and will wait in its slot, or null if there is none. */
    private fun blocker(state: GameState, config: LevelConfig, random: Random): Volume? {
        val picture = state.picture
        val options = (0 until picture.colorCount).mapNotNull { color ->
            val available = minOf(picture.remainingOf(color) - state.promised(color), config.maxVolume)
            if (available <= 0) return@mapNotNull null
            // Pixels of this color that could be eaten right now; the blocker must want more.
            val least = reach(picture, color, available) + 1
            if (least > available) null else color to least..available
        }
        if (options.isEmpty()) return null
        val (color, sizes) = options.random(random)
        val size = volumeSize(maxOf(config.minVolume, sizes.first), sizes.last, random)
        return Volume(color, size.coerceIn(sizes))
    }

    /** How many pixels of [color] a single volume could eat right now, up to [limit]. */
    private fun reach(original: Picture, color: Int, limit: Int): Int {
        if (!original.hasExposed(color)) return 0
        val picture = original.copy()
        var eaten = 0
        while (eaten < limit) {
            val pixel = picture.nextExposed(color)
            if (pixel < 0) break
            picture.remove(pixel)
            eaten++
        }
        return eaten
    }

    /** Log-uniform, so small and large volumes both show up regularly. */
    private fun volumeSize(min: Int, max: Int, random: Random): Int {
        if (min >= max) return max
        return exp(ln(min.toDouble()) + random.nextDouble() * (ln(max + 1.0) - ln(min.toDouble())))
            .toInt().coerceIn(min, max)
    }

    private fun deal(order: List<Volume>, queueCount: Int, mystery: Double, random: Random): List<List<Volume>> {
        val queues = List(queueCount) { ArrayList<Volume>() }
        for (volume in order) {
            val shortest = queues.minOf { it.size }
            val queue = queues.filter { it.size <= shortest + 1 }.random(random)
            // The first volume of each queue is visible from the start anyway.
            val hidden = queue.isNotEmpty() && random.nextDouble() < mystery
            queue += if (hidden) Volume(volume.color, volume.count, hidden = true) else volume
        }
        return queues
    }
}
