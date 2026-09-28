# Color Eater – design notes

How the game works, which algorithms it uses, and why they look the way they do.

## 1. The game

The screen has three areas:

1. **Picture**: a pixel-art image. Every pixel has one color.
2. **Immediate queue (slots)**: 2 to 5 slots, depending on the level.
3. **Volume queues**: 4 queues of *volumes*. A volume is a color plus a count (1 to 100). The
   player taps a queue to move its front volume into the first free slot.

Volumes in slots eat pixels of their color. Ants carry the pixels away.

- **Win**: the picture is empty.
- **Lose**: every slot is taken and none of the volumes can eat. Running out of volumes while
  pixels remain also loses, but the generator never lets that happen, because the counts per
  color always add up to exactly the pixels of that color.

### The eating rule: only exposed pixels

A pixel is **exposed** if it touches the border of the image or an already-empty cell (4-neighbourhood).
Only exposed pixels can be eaten, so the picture gets eaten from the outside in.

This rule creates the whole challenge. If volumes could eat any pixel of their color, a volume
would never have to wait and the slot limit would not matter. With the rule, a volume whose color
is still buried has to wait in its slot, and slots fill up.

## 2. Game rules engine (`game/`)

The rules are pure Kotlin without Android dependencies, so they can be unit-tested on the JVM
and reused by the generator and the solver.

### Picture (`Picture.kt`)

- Cells are stored row by row **starting at the bottom row**, so a lower index is closer to the
  nests below the picture.
- For every color a `BitSet` holds the exposed pixels. Removing a pixel clears its bit and
  marks its non-empty neighbours as exposed. Everything is O(1) per pixel, and a copy is cheap
  (an `IntArray` plus a few `BitSet`s), which the solver needs.
- **Determinism**: a volume always takes the exposed pixel of its color with the **lowest index**
  (`BitSet.nextSetBit(0)`). So the same moves always give the same result. The generator, the
  solver and the player all see exactly the same game.

### Game state (`GameState.kt`)

`pick(queue)` puts the front volume into the first free slot and then **eats instantly until
nothing changes**: the slots take turns (round-robin) removing one pixel each, until no slot can
eat. A slot empties when its volume's count reaches 0. Then the status is checked.

There is no time in the rules. `pick` returns the list of removals (`Removal(slot, pixel,
volume)`), and the UI replays them with ants. Two consequences:

- The rules stay simple and deterministic. Solver and generator run them thousands of times.
- The player can keep tapping while ants are still walking. The result screen appears only once
  all ants are home.

The first free slot is always used (not "a slot whose ants are done") because the slot order
decides which volume eats first. The generator plays the same way, so the solution it found
really works in the game.

## 3. Ant animation (`ui/AntSwarm.kt`, `ui/Session.kt`)

Every `Removal` becomes one ant: it walks from its slot (the nest) to the pixel, picks it up and
carries it back.

- **Order**: pick-ups happen in exactly the order the rules removed the pixels. The pick-up time
  of each ant is `max(now + walking time, previous pick-up + 1/rate)`, and the ant leaves early
  enough to be there on time. So no ant grabs a pixel that is still covered by another one.
- **Rate**: `8 + 2.5 × waiting` pick-ups per second, capped at 160. Small volumes are calm, a
  volume of 100 turns into a swarm and is done in about 1.5 s.
- **Display**: a pixel stays visible until its ant grabs it. A slot counter shows *rules count
  + pixels not yet picked up*, so it counts down together with the ants. A slot whose volume is
  finished in the rules but still has ants on the way is shown dimmed.
- **Drawing**: the ant is top-down, with three body parts, six legs in an alternating tripod gait
  and antennae. Paths bend slightly (out on one side, back on the other). No image files are needed.

### Title screen (`ui/FlowerIntro.kt`)

The app opens on a title screen with a large pixel flower and one button per difficulty (showing
the level reached); a button opens that difficulty's level list, Back returns to the title.

The flower grows out of a soil mound: pixels appear outwards from the soil (breadth-first,
shuffled within each step). Once it has bloomed for a moment, an `AntSwarm` whose nest sits just
off the screen edge, in a random direction for every flower, carries away everything but the
soil, in reverse growth order. Then the next of 5 flowers grows, in random order and never the same one twice in a row.

## 4. Level generation (`game/LevelGenerator.kt`)

Goals: every level must be **winnable**, and it should **work close**, with no easy way through.

### What didn't work: shuffling

The first version ate the picture volume by volume in a "perfect" order, shuffled that order
locally and dealt it onto the queues. Measured with the solver: **every** level could be won
with **a single slot**, no matter how strong the shuffle. Four queue fronts give so much choice,
and most colors are exposed so early, that there is almost always a volume that can eat
completely right away. Randomness alone does not create pressure.

### Intended solution with blockers

The generator plays an **intended solution** itself and creates the volumes as it goes:

- **Eatable volume**: color chosen weighted by the number of exposed pixels, size log-uniform in
  `[minVolume, maxVolume]`, capped at what that volume can reach right now (simulated on a copy).
  It is eaten completely at once.
- **Blocker** (with probability `blockers`): a volume that cannot be finished yet, because its
  color is still buried or because it is bigger than what can be reached. It waits in its slot.
  Only colors that still have unpromised pixels are allowed:
  `available = remaining pixels − counts already promised to slots`.

**Invariants**

- A blocker may only be placed if **at least one slot stays free** afterwards. So the intended
  play never loses and always fits into `maxSlots`.
- Every color that is exposed has no volume promised to it (otherwise that volume would
  already be eating), so an eatable volume can always be created. The process always ends.
- Counts add up exactly to the pixels of each color.

The order is then **dealt** onto the queues (random queue, lengths kept roughly equal). Relative
order is kept, so each volume is at the front of its queue when its turn comes: taking them in the
intended order wins.

### Fewest slots and slack

After dealing, the solver searches for the **fewest slots** the level can be won with (1, 2, ...
up to `maxSlots`, which the intended solution guarantees). The player gets **exactly that many
plus `slack`**. This is what makes levels "work close": with slack 0 there is no spare slot at
all. With slack 1 there is room for exactly one mistake.

### Trap score

Of 10 candidates the one with the **most traps** is kept:

- +1 if a **greedy** player loses (always takes the volume that eats the most right now, which
  is what a hasty player does);
- plus the share of **16 random** plays that lose;
- slots needed as a small tiebreaker.

With this, greedy play loses almost every Normal and Hard level from level 3 on: the obvious move is often the
wrong one.

### Solver (`game/Solver.kt`)

Depth-first search over "which queue next":

- Children are sorted by the number of pixels eaten (good moves first).
- States are remembered **by queue progress only** (8 bits per queue, one `Long`). That is not
  exactly the full state, because the order of moves affects which pixels were eaten, so the
  search can miss solutions. It is **sound in one direction**: a found solution is a real one.
  "Not found" can be wrong, which at worst gives the player a slot more than needed.
- **Budget** of 5,000 states per question. When giving up, the answer counts as "not solvable
  with this many slots", and again the level only gets easier, never unwinnable. With 50,000
  states the levels were almost identical (2 of 27 had one slot less), but the worst case was 10
  times slower (2.1 s instead of 0.2 s on a PC).

## 5. Curveballs

- **Pictures with repeated colors at different depths**:
  - *Dice*: the outline and the pips are both black. A big black volume eats the outline and then
    blocks a slot until the white around the pips is gone.
  - *Target*: red and white rings take turns. Red eats one ring and gets stuck.
  - *Chess*: a checkerboard. Black and white have to take turns all the time.
- **Pictures per difficulty** (`Levels.arts`): Easy keeps the simple pictures above. Normal
  (`NORMAL_ARTS`) and Hard (`HARD_ARTS`) have their own 9 pictures each, with 4 to 9 colors, where
  colors sit inside other colors, often several layers deep (the gems in *Crown*, the fish in
  *Aquarium*, the pupil and highlight of *Eye*, the snowman in *Snow globe*). More buried colors
  give the generator more blockers and the player more volumes that have to wait. All three
  lists follow the same pacing.
- **Mystery volumes** (`Volume.hidden`): shown as `?` without color or count until they reach
  the front of their queue. This only affects the display; rules and solver know them. Because
  planning ahead becomes partly impossible, mystery only appears on levels with at least one
  spare slot. Otherwise a level could need luck.

## 6. Difficulty and level sequence (`game/Levels.kt`)

| | Easy | Normal | Hard |
|---|---|---|---|
| Spare slots (slack) | 2 | 1 | 0; every 3rd level (from 5 on) 1 |
| Blocker probability | 0.25 → 0.5 | 0.45 → 0.8 | 0.6 → 0.85 |
| Mystery volumes | none | 25 % from level 5 | 40 % on the every-3rd levels |

Blockers increase every 9 levels. Picture size depends on the level only: level 1 is 16×16,
levels 2 to 9 are 2× (32×32), from level 10 on 3× (48×48, up to 2,304 pixels, volumes up to 100).
Each difficulty has its own progress and its own seed.

### Pictures are rewards

- A **new picture** appears on levels 1, 2, 4, 7, 10, 13, ... (every 3 levels from level 4 on).
  The 9 pictures last until level 22.
- The levels in between reuse the known picture that was **seen longest ago** (never the same as
  the level before), bigger or with harder settings.
- The level menu shows pictures **only for completed levels**. The current level is a `?` (with
  `NEW` if it brings a new picture), and only the next two locked levels are shown.

## 7. Pre-generated levels

Generating on the phone was far too slow. In a **debug build** a level took up to **7 s** (the
first one after the app starts; the solver allocates a lot of short-lived copies). On a PC the
same level takes 7 ms. The earlier version with 50,000 solver states therefore looked like the
game hung at "Preparing level 7".

Because the generator is deterministic (seed from level number and difficulty), levels are
**generated ahead of time**: `app/src/main/assets/levels/<difficulty>/NNN.txt`, 45 levels per
difficulty, in the text format from `LevelCodec`. The picture is not stored; it follows from the
level config. Levels after 45 are generated on the phone in the background while the previous
level is being played.

After changing the generator or `Levels.config`, regenerate them:

```sh
WRITE_LEVELS=1 ./gradlew testDebugUnitTest --tests '*LevelAssetsTest*'
```

`LevelAssetsTest` fails if the files no longer match the generator.

## 8. Tests

- `GameStateTest`: exposure, waiting volumes, losing, solver.
- `LevelGeneratorTest`: levels 1 to 27 of every difficulty are winnable and use every pixel
  exactly. It prints slots, whether one slot less would be enough, and whether greedy play wins.
- `LevelsTest`: picture pacing (1, 2, 4, 7, ... and never twice in a row).
- `LevelAssetsTest`: shipped levels match the generator, and the codec round-trips.

## 9. Open ideas

- Hand-drawn ant sprites instead of the drawn ants.
- More pictures, or larger ones.
- Undo, or a single "hint" per level (the solver knows a solution).
- Release build (minified, not debuggable), which would also make generating on the phone much faster.
