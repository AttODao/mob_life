# Codex Instructions

## Project

- Mod ID: `mob_life`
- Maven group: `cc.attodao`
- Version: `0.0.1`
- Minecraft: `26.1.2`
- Fabric Loader: `0.19.3`
- Fabric API: `0.150.0+26.1.2`
- Java: `25`
- Reference implementation for morph rendering: https://github.com/Naw7k/Morphling
- Reference implementation for dichromatic vision: https://github.com/Ladysnake/Requiem
- Reference implementation for variable inventory sizes: https://github.com/TheRedBrain/inventory-size-attributes

## Verification Policy

- After implementation, only verify that the project builds.
- Do not launch Minecraft or perform interactive/runtime tests unless the user explicitly requests them.
- Use:

```sh
env LD_LIBRARY_PATH=/nix/store/zl6x30j3w9byijlj1x2nx1zavxvxaxv6-mob-life-native-libraries/lib ./gradlew build
```

- The Gradle client run also includes `/run/opengl-driver/lib` and the Nix linker path through `build.gradle`.

## Current Features

- A world-wide play form is selected during world creation and persisted in saved data.
- The world creation game-settings tab includes an optional SNBT field below
  the form selector. Empty input uses the entity defaults; invalid input is
  shown in red and prevents world creation.
- Available forms are normal player, cow, sheep, and chicken.
- The form can be changed later with:

```text
/moblife morph minecraft:player
/moblife morph minecraft:cow
/moblife morph minecraft:sheep
/moblife morph minecraft:chicken
```

- An optional entity NBT compound can follow the entity ID:

```text
/moblife morph minecraft:cow {Age:-24000}
/moblife morph minecraft:sheep {Color:14b}
/moblife morph minecraft:chicken {Age:-24000,Health:6.0f}
/moblife morph minecraft:cow {variant:"minecraft:warm"}
/moblife morph minecraft:chicken {variant:"minecraft:cold"}
```

- Morph NBT is persisted with the world and synchronized to clients.
- The current `variant` key and legacy-style `Variant` key are both accepted;
  values are loaded through the target entity's variant registry.
- Entity identity, position, motion, rotation, and passenger tags are removed
  before the NBT is applied to the morph template.
- `Health` overrides the transformed player's maximum health. Other visual and
  entity attributes, including age and color, are loaded by the target entity.
- Dedicated servers require gamemaster permission for the command.
- Mob forms replace player rendering, animation, dimensions, collision box, eye height, movement speed, and maximum health.
- Chicken form has slower falling.
- Forms whose entity type is in `minecraft:fall_damage_immune` do not take
  fall damage; among the currently available forms this applies to chicken.
- Mob forms use a horse-style charged jump: hold jump to charge and release to jump. Maximum charge has the same height as a normal player jump.
- Maximum jump charge is reached after 8 ticks and remains full while jump is held.
- While charging, the vanilla horse jump gauge replaces the experience bar; its cooldown texture is shown briefly after jumping.
- Mob forms move at quarter speed when strafing or moving backward. The forward component of diagonal movement remains unchanged.
- Inventory capacity scales with the form's height:
  - Player: 9 hotbar + 27 inventory slots
  - Cow: 7 hotbar + 21 inventory slots
  - Sheep: 6 hotbar + 19 inventory slots
  - Chicken: 3 hotbar + 9 inventory slots
- Items in slots disabled by a smaller form move into active slots; overflow is dropped.
- NBT size changes such as `Age:-24000` further scale inventory, food capacity,
  mining speed, dimensions, collision, and eye height relative to the adult form.
- Maximum food scales with form height: player 20, cow 15, sheep 14, and chicken 10.
- NBT-based size scaling cannot reduce a mob form's maximum food below 8.
- Mob forms cannot eat normal food. They eat their breeding items for 4 food points instead.
- Breaking and placing blocks, charged jumping, sprinting, and sneaking add extra exhaustion in mob form.
- Mob forms cannot break or place blocks while airborne or sprinting. Other block interactions remain available.
- Mob forms cannot interact with `Mob` entities, preventing riding animals,
  villager trading, taming, feeding, and similar mob interactions. Boats and
  minecarts remain interactable so they can still be boarded.
- While a mob-form player rides a boat or minecart, forward, backward,
  strafing, jumping, and sprinting inputs are discarded. Sneaking remains
  available for dismounting.
- Friendly mob forms deal no attack damage.
- Sheep forms are hunted by wild wolves; chicken forms are hunted by foxes
  and ocelots.
- Each player has a persisted and client-synchronized awkwardness value from
  0 to 100.
- Awkwardness can be set for the command executor with
  `/moblife awkwardness <0-100>`.
- The awkwardness HUD is a disabled-by-default debug display. It can be
  toggled from Mob Life's configuration button in Mod Menu and is saved in
  `config/mob_life-client.json`.
- Breaking, placing, block interaction, non-forward movement, prolonged
  sprinting, and attacking increase awkwardness. Eating, staying near the same
  mob, and passive decay reduce it.
- Passive awkwardness decay depends on the total item count in active inventory
  slots. An empty inventory decays at `5x` the base rate, with the multiplier
  approaching `1x` as more items are carried.
- Successful block breaking and block placement each add 2 awkwardness.
- Awkwardness multiplies mob-form exhaustion up to `3x` at 100.
- Monsters ignore mob-form players at 70 or below. At 70 and above, vision
  interference progressively strengthens. At 90 and above, breaking, placing,
  and block interaction are disabled.
- Skeleton ranged attacks aim at a transformed player's morph eye height
  instead of the vanilla player-sized target position.
- At 30 awkwardness or below, the configurable `V` key sleeps without a bed
  on grass, wool, carpet, hay, moss, or beds. This sleep costs 6 food points,
  requires more than 6 current food points, still checks for nearby monsters,
  and requires 200 ticks instead of 100. Its sleep timer cap is extended only
  for this soft-surface sleep.
- First-person empty hands use the mob front leg or wing.
- When holding an item or map, the mob hand is not rendered so it does not overlap the item.
- Normal player form removes all Mob Life attribute modifiers and restores vanilla rendering, dimensions, health, movement, mining speed, and vision.
- Mob forms apply a form-specific low-saturation dichromatic post-processing effect.
- Mining speed uses the `minecraft:block_break_speed` attribute:
  - Cow: approximately `0.78x`
  - Sheep: approximately `0.72x`
  - Chicken: `0.5x`

## Architecture

- `morph`: form IDs and entity-type mapping.
- `world`: pending world selection and persisted world-wide form.
- `gameplay/jump`: shared charged-jump calculation and player bridge.
- `gameplay/food`: diet rules and form-specific food capacity.
- `gameplay/inventory`: form-specific hotbar and inventory capacity.
- `server/ServerMorphManager`: world morph lifecycle, charged jumps, and recurring form effects.
- `server/ServerPlayerMorphApplier`: attributes, dimensions, capacity, and client synchronization.
- `client/state`: active client form, render-entity cache, and charged-jump input state.
- `client/render`: mob hand, sized hotbar, and jump-bar renderers.
- Common mixins are grouped by `food`, `gameplay`, `inventory`, and `player`.
- Client mixins are grouped by `gameplay`, `inventory`, `player`, `render`, and `world`.

## Shader Resources

- Shared shader: `assets/mob_life/shaders/post/mob_vision.fsh`
- The shared shader applies form-specific dichromatic color and mild
  desaturation across the full image. The main depth buffer progressively adds
  blur, lower contrast, darkening, and haze to distant scenery.
- The base pass preserves the source luminance, and distant darkening and haze
  use the current sky brightness so daytime and nighttime remain distinguishable.
- Vision rendering uses two passes: distant effects run immediately after
  world rendering while the world depth buffer still exists; base dichromatic
  vision runs after first-person hand rendering without consulting depth.
- Post chains:
  - `assets/mob_life/post_effect/cow_vision.json`
  - `assets/mob_life/post_effect/sheep_vision.json`
  - `assets/mob_life/post_effect/chicken_vision.json`
- `VisionConfig.Settings.x` controls retained saturation. Current values are intentionally muted but not near grayscale.
- `DistanceBlur.Parameters` contains start distance, full-blur distance,
  full-darkening distance, and full-fog distance.
- `DistanceBlur.Effects` controls maximum blur radius, distant contrast,
  brightness, and gray haze strength. Effects are explicitly disabled before
  the start distance.
- `DistanceBlur.DepthRange` is updated every frame with Iris-compatible near
  and far planes (`0.05` and effective render distance in blocks), allowing the
  shader to linearize the depth buffer before applying distance thresholds.
- `DistanceBlur.DepthRange.w` stays at the base `1.0` through 70
  awkwardness, then increases to `2.0` at 100. The base therefore uses the
  former 100-awkwardness vision strength without additional awkwardness
  effects, while values above 70 further strengthen desaturation, darkening,
  fog, and blur.

## Repository State

- The repository was created from a Fabric template.
- Deleted `com.example` and `modid` template files are intentional.
- Do not restore template files or revert unrelated user changes.
- Third-party attribution is recorded in `THIRD_PARTY_NOTICES.md`.
