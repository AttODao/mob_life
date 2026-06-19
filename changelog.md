# Changelog

Future changes should be recorded here with the date and version.

## 2026-06-20 - v1.0.1

- Moved night vision out of potion effects and into morph-specific rendering state.
- Changed the vision post effect to use player-relative distance and fixed the render order so the world, distance effect, and HUD layers do not interfere.
- Increased the screen-edge blur strength for low-FOV mobs and adjusted the rabbit/cat balance.
- Fixed initial world generation so the mob variant and its NBT are decided before world creation, then used to choose the spawn context.
- Simplified morph diet data by removing `hunted_foods` and keeping all food entries in `foods`.

## 2026-06-19 - v1.0.0

- Bumped the mod version to `1.0.0`.
- Updated the build workflow to publish only the current version jar.
- Added a release workflow that creates a GitHub release for each new version.
- Linked GitHub release descriptions to this changelog.
