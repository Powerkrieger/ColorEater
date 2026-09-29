<p align="center">
  <img src="docs/images/title.gif" alt="Color Eater title screen: a pixel tulip grows and a swarm of ants carries it away" width="380">
  &nbsp;&nbsp;
  <img src="docs/images/gameplay.png" alt="A level in progress: a swarm of ants carries the green leaves of a strawberry to their slot" width="200">
</p>

<p align="center">
  <b>A cozy little puzzle game for Android where ants eat pixel art, one pixel at a time.</b><br>
  <a href="https://github.com/Powerkrieger/ColorEater/releases/latest"><b>⬇ Download the latest APK</b></a>
</p>

---

## How to play

A pixel-art picture sits on top of the screen, and below it wait four queues of **volumes**. A volume
is a color plus a number of pixels. Tap a queue to move its front volume into one of the few
**slots**, and ants from that slot swarm out and carry away pixels of that color.

The catch: ants can only reach pixels that are **exposed**, meaning they touch the edge of the
picture or a hole that has already been eaten. The picture gets eaten from the outside in. A
volume whose color is still buried has to wait in its slot, and slots are precious.

- 🏆 **Win** by clearing every pixel.
- 🪤 **Lose** when every slot is full and no volume can eat.

The obvious move is often the wrong one: grabbing the biggest bite right away tends to leave
buried colors waiting in your slots until you're stuck.

## 27 pictures to uncover

<p align="center">
  <img src="docs/images/pictures.png" alt="The first picture of each difficulty: a heart, a strawberry and an eye" width="512">
</p>

Every difficulty has its own set of 9 pictures. These are the first ones; the rest are only
revealed as you play, a new one every few levels. Completed levels show their picture in the
level list.

| | Easy | Normal | Hard |
|---|---|---|---|
| Pictures | Simple shapes | Many colors, details tucked inside | Colors walled in several layers deep |
| Spare slots | 2 | 1 | Usually none |
| Mystery volumes `?` | – | Sometimes | On every third level |

Every level is **guaranteed to be winnable**: the generator plays a solution itself, and a solver
then hands you only as many slots as the level really needs (plus the spare ones).

## Download

Grab `coloreater.apk` from the [latest release](https://github.com/Powerkrieger/ColorEater/releases/latest)
and open it on your Android phone (you may have to allow installing apps from unknown sources).
Requires Android 8.0 or newer.

## Under the hood

- Kotlin, a single custom `View` drawn with Canvas. No game engine, no image files: even the ants
  are drawn in code, with a six-legged tripod gait.
- The rules are deterministic and pure Kotlin, shared by the game, the level generator and the solver.
- [`docs/DESIGN.md`](docs/DESIGN.md) explains the algorithms and why they look the way they do.

### Build and run

```sh
./gradlew installDebug          # build and install on a connected device
./gradlew testDebugUnitTest     # rules, generator and level tests
```

After changing the level generator, the level settings or the pictures, regenerate the shipped levels:

```sh
WRITE_LEVELS=1 ./gradlew testDebugUnitTest --tests '*LevelAssetsTest*'
```

### Project layout

- `app/src/main/java/.../game/`: rules, solver, level generator, pictures (pure Kotlin)
- `app/src/main/java/.../ui/`: game view, title animation, play session, ants
- `app/src/main/assets/levels/`: pre-generated levels

### Releases

GitHub Actions (`.github/workflows/build.yml`) runs the unit tests and builds a signed release
APK on every push to `main`. Pushing a tag `v*` also publishes a GitHub release with the APK:

```sh
git tag v0.1.0 && git push origin v0.1.0
```

Signing uses the repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
`KEY_PASSWORD`. Locally the keystore is `coloreater-release.jks` with its passwords in
`coloreater-release.env`; both are gitignored. Keep a backup: without the keystore, installed
copies can't be updated.

---

<p align="center"><sub>Created with AI.</sub></p>
