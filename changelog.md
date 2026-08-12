# Changelog

Future changes should be recorded here with the date and version.

## 2026-08-12 - v1.2.5

- Fixed instinct hunting so a living prey target remains locked while being
  pursued, even when multiple valid prey are nearby.

## 2026-08-12 - v1.2.4

- Rebalanced instinct nutrition: cats restore 4 from rabbits and 3 from baby
  turtles; ocelots restore 4 from chickens and 3 from baby turtles; wolves
  restore 4 from sheep, rabbits, and foxes and 3 from baby turtles; evil
  rabbits restore 5 from players and 4 from wolves; sheep grass meals restore
  2; and rabbit garden raids restore 3.
- Made low-food awkwardness decay half as fast while keeping low-food gains at
  2x, and added a 6-second cooldown to damage-based awkwardness increases.
- Persisted hunting and feeding cooldowns across reconnects and instinct-mode
  transitions.
- Added configurable pursuit timeouts and long cooldowns after abandoned hunts,
  while preserving wall-through prey sensing.
- Delayed hunting nutrition recovery until the eating animation completes.

## 2026-08-12 - v1.2.3

- Fixed instinct-mode controller input by using the common player-input and
  view-turn paths, so Controllify movement and camera controls both prevent
  idle entry and support permitted interventions. Instinct mode now also
  ignores block targets and keeps the first-person mob limb still.
- Changed bedless sleep to require awkwardness below 30, consume no food, and
  add 70 awkwardness when it starts. The former per-morph food-cost and
  awkwardness-limit sleep settings were removed.

## 2026-08-12 - v1.2.2

- Reworked instinct-mode intervention: forward input can start or steer a
  short native wander, while lateral input can turn the resting body or a
  current wander's direction intent. Hunting, fleeing, feeding, and social
  movement retain priority over those interventions.
- Smoothed instinct camera control with bounded, reduced-sensitivity manual
  look offsets that recenter naturally, preventing abrupt turns as native AI
  headings change.
- Kept the shadow mob in a stationary rest state while holding the attack key
  to leave instinct mode, so wandering and rabbit hops cannot interrupt the
  exit hold. Eligible movement-intervention attempts now temporarily pause
  awkwardness decay.
- Added configurable social instinct behavior for cow, sheep, chicken, rabbit,
  wolf, pig, horse, donkey, and mule forms. Nearby natural groups take
  precedence over wandering, but not over hunting, fleeing, or feeding.

## 2026-08-11 - v1.2.1

- Tied instinct mode timing to awkwardness: higher awkwardness enters instinct
  mode sooner, reaches forced entry at the maximum, and takes longer to exit.
- Prevented instinct mode from being manually exited while awkwardness is at
  its maximum.

## 2026-08-11 - v1.2.0

- Updated the mod to Minecraft 26.2, Fabric API 0.157.0, and the current
  Fabric build tooling.
- Updated vision rendering for Minecraft's reversed depth buffer so distance
  blur, haze, and low-light highlights remain correctly ordered.
- Consolidated morph vision resources into shared distance and base passes,
  reducing post-effect duplication while keeping per-morph vision data-driven.
- Avoided unnecessary bright-light sampling when the distant low-light effect
  is inactive.
- Assigned IDs to unregistered morph render entities so 26.2 render components
  can render selection previews and first-person mob hands without registering
  them in a world, while keeping previews out of predator and prey outlines.

## 2026-08-11 - v1.1.0

- Added instinct mode, where each morph can follow its native mob AI for
  resting, wandering, fleeing, hunting, attacking, and feeding. Instinct mode
  enters automatically after inactivity, supports limited movement, view, and
  jump intervention, and persists across reconnects.
- Added instinct hunting and feeding behaviors, including configured prey
  relations, feline pursuit, sheep grass eating, rabbit crop raids, hunger
  thresholds, and awkwardness reduction through instinct activity.
- Added predator and prey outlines, plus an amber instinct vision effect with
  smooth transitions and per-morph configuration.
- Reworked quadruped, horse, donkey, and mule locomotion, removed the former
  fast-sprint behavior, and recalibrated morph movement speeds and rabbit hops.
- Improved feline low-light vision so distant light sources remain visible
  through the vision effects.

## 2026-07-05 - v1.0.4

- Fixed mob selection mouse hit testing so clicks on rows, description
  buttons, and footer controls land on the correct widget instead of opening
  the mob details overlay.
- Kept the row frame unchanged when the `?` button is focused, so only the
  `?` button itself highlights.

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
