package de.powerizzle.coloreater.game

enum class Outcome { SOLVED, UNSOLVABLE, UNKNOWN }

class Solution(val outcome: Outcome, val moves: List<Int>)

/**
 * Depth-first search over queue picks. States are remembered by queue progress only, which can
 * miss solutions that depend on pick order; a SOLVED result is always a real solution, though.
 */
object Solver {
    fun solve(start: GameState, maxNodes: Int = 50_000): Solution {
        val visited = HashSet<Long>()
        val moves = ArrayList<Int>()
        var nodes = 0

        fun search(state: GameState): Outcome {
            when (state.status) {
                Status.WON -> return Outcome.SOLVED
                Status.LOST -> return Outcome.UNSOLVABLE
                Status.PLAYING -> Unit
            }
            if (!visited.add(state.queueKey())) return Outcome.UNSOLVABLE
            if (++nodes > maxNodes) return Outcome.UNKNOWN

            val children = state.queues.indices
                .filter { state.canPick(it) }
                .map { queue -> val child = state.copy(); Triple(queue, child, child.pick(queue).size) }
                .sortedByDescending { it.third }
            var result = Outcome.UNSOLVABLE
            for ((queue, child, _) in children) {
                moves += queue
                when (search(child)) {
                    Outcome.SOLVED -> return Outcome.SOLVED
                    Outcome.UNKNOWN -> result = Outcome.UNKNOWN
                    Outcome.UNSOLVABLE -> Unit
                }
                moves.removeAt(moves.lastIndex)
                if (nodes > maxNodes) return Outcome.UNKNOWN
            }
            return result
        }

        val outcome = search(start)
        return Solution(outcome, if (outcome == Outcome.SOLVED) moves else emptyList())
    }
}
