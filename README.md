# Color Eater

An Android puzzle game: a pixel-art picture gets eaten by ants. Pick volumes (a color and a
pixel count) from four queues and move them into a few slots. A volume can only eat pixels of
its color that are exposed, so volumes of buried colors have to wait and take up slots. Clear the
picture before every slot is blocked.

- Kotlin, a single custom `View` drawn with Canvas, no game engine.
- 9 pixel-art pictures; Easy / Normal / Hard; levels are winnable by construction and tuned to
  be tight (a solver decides how many slots a level gets).

## Build and run

```sh
./gradlew installDebug          # build and install on a connected device
./gradlew testDebugUnitTest     # rules, generator and level tests
```

After changing the level generator or level settings, regenerate the shipped levels:

```sh
WRITE_LEVELS=1 ./gradlew testDebugUnitTest --tests '*LevelAssetsTest*'
```

## Layout

- `app/src/main/java/.../game/`: rules, solver, level generator, pictures (pure Kotlin)
- `app/src/main/java/.../ui/`: game view, play session, ant animation
- `app/src/main/assets/levels/`: pre-generated levels
- [`docs/DESIGN.md`](docs/DESIGN.md): algorithms and the reasoning behind them
