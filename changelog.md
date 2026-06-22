# Changelog

Future changes should be recorded here with the date and version.

## 2026-06-22 - v1.0.3

- Fixed controller focus traversal in the mob selection screen so the mob
  rows, description buttons, and footer controls can all be reached normally.
- Made the mob selection scrollbar draggable with touch or mouse input.
- Fixed charged jump input so controller jump state is read from the live
  player input instead of only the keyboard binding.

## 2026-06-20 - v1.0.2

- Reworked initial spawn selection so structure-dependent forms choose biomes
  where their required structure can generate, and the world generator now
  places that structure at the spawn point.
- Disabled `charged_jump` for cat, ocelot, and wolf forms, and standardized
  jump cooldowns across forms to 0.5 seconds starting on landing. Rabbits keep
  their own jump cooldown.
- Made transformed players and inventory-screen players face the pointer
  direction like they do in the normal player view.
- Moved morph diets to item tags under `resources`, expanded the available
  foods with a more realistic meat-and-herbivore split, and kept wheat and seed
  foods for the forms that already used them.
- Expanded rabbit food options further.
- Changed the log-to-crafting-table recipe so it only works in inventory
  crafting and no longer appears in the crafting table or crafting book.
- Fixed the resource layout for item tags and restored the missing `cat.json`
  morph definition.
- Updated the awkwardness indicator to look like an experience orb.

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
