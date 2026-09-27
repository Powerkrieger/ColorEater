# Color Eater

An Android puzzle game: a pixel-art picture gets eaten by ants. Pick volumes (a color and a
pixel count) from four queues and move them into a few slots. A volume can only eat pixels of
its color that are exposed, so volumes of buried colors have to wait and take up slots. Clear the
picture before every slot is blocked.

- Kotlin, a single custom `View` drawn with Canvas, no game engine.
- 9 pixel-art pictures; Easy / Normal / Hard; levels are winnable by construction and tuned to
  be tight (a solver decides how many slots a level gets).

## Download

Get `coloreater.apk` from the [latest release](https://github.com/Powerkrieger/ColorEater/releases/latest)
and open it on an Android phone (you may have to allow installing unknown apps). Requires Android 16.

## Build and run

```sh
./gradlew installDebug          # build and install on a connected device
./gradlew testDebugUnitTest     # rules, generator and level tests
```

After changing the level generator or level settings, regenerate the shipped levels:

```sh
WRITE_LEVELS=1 ./gradlew testDebugUnitTest --tests '*LevelAssetsTest*'
```

## Releases

GitHub Actions (`.github/workflows/build.yml`) runs the unit tests and builds a signed release
APK on every push to `main`. Pushing a tag `v*` also publishes a GitHub release with the APK:

```sh
git tag v0.1.0 && git push origin v0.1.0
```

Signing uses the repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
`KEY_PASSWORD`. Locally the keystore is `coloreater-release.jks` with its passwords in
`coloreater-release.env`; both are gitignored. Keep a backup: without the keystore, installed
copies can't be updated.

## Layout

- `app/src/main/java/.../game/`: rules, solver, level generator, pictures (pure Kotlin)
- `app/src/main/java/.../ui/`: game view, play session, ant animation
- `app/src/main/assets/levels/`: pre-generated levels
- [`docs/DESIGN.md`](docs/DESIGN.md): algorithms and the reasoning behind them
