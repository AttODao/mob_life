# Codex Instructions

## Project

- Mod ID: `mob_life`
- Maven group: `cc.attodao`
- Version: `0.3.0`
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
- New worlds using a mob form move the initial overworld spawn to a biome
  whose natural spawn list contains that mob. A natural-sized group of the
  selected mob is ensured within 32 blocks of the spawn point.
- Initial spawn relocation runs once per newly created world. Existing worlds
  created before this feature keep their current world spawn.
- The world creation game-settings tab opens a separate three-column form
  selection screen, avoiding repeated cycling through every form. An optional
  SNBT field remains below the selector; empty input uses entity defaults,
  while invalid input is shown in red and prevents world creation.
- Available forms are normal player, cow, sheep, chicken, cat, ocelot, wolf,
  pig, horse, donkey, mule, and rabbit.
- The form can be changed later with:

```text
/moblife morph minecraft:player
/moblife morph minecraft:cow
/moblife morph minecraft:sheep
/moblife morph minecraft:chicken
/moblife morph minecraft:cat
/moblife morph minecraft:ocelot
/moblife morph minecraft:wolf
/moblife morph minecraft:pig
/moblife morph minecraft:horse
/moblife morph minecraft:donkey
/moblife morph minecraft:mule
/moblife morph minecraft:rabbit
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
- Per-morph gameplay configuration is loaded from
  `data/mob_life/mob_life/morphs/<mob>.json`. Data packs can override the same
  paths, and successful `/reload` reapplies server attributes and synchronizes
  movement and vision settings to clients.
- When morph NBT does not already contain generated values, the morph entity is
  finalized at the selected initial-spawn location. This persists a random
  vanilla variant and other spawn-time properties, while explicitly supplied
  NBT always overrides generated values.
- Initial spawn biome selection honors explicit warm/cold cow, chicken, and pig
  variants plus vanilla wolf variant biome rules. Otherwise the form is
  finalized in a biome where it can naturally spawn, so its generated variant
  is appropriate for that biome.
- Horse, donkey, and mule maximum health, movement speed, and jump strength are
  randomized once and persisted with the world morph. Horse uses its vanilla
  spawn distributions; donkey and mule use the same speed and jump
  distributions while retaining their vanilla randomized health.
- The current `variant` key and legacy-style `Variant` key are both accepted;
  values are loaded through the target entity's variant registry.
- Entity identity, position, motion, rotation, and passenger tags are removed
  before the NBT is applied to the morph template.
- `Health` overrides the transformed player's maximum health. Other visual and
  entity attributes, including age and color, are loaded by the target entity.
- The `/moblife` command tree is a cheat command and requires vanilla
  gamemaster command permission, including in integrated singleplayer worlds.
- Mob forms replace player rendering, animation, dimensions, collision box, eye height, movement speed, and maximum health.
- The maximum step height is proportional to the actual NBT-adjusted morph
  height, using the player's vanilla 0.6-block step height as the baseline.
  Horse, donkey, and mule forms instead copy their original entity's
  `minecraft:step_height` attribute.
- Chicken form has slower falling outside water. In water it uses normal
  vertical fluid movement.
- Forms whose entity type is in `minecraft:fall_damage_immune` do not take
  fall damage. Safe-fall distance and fall-damage multiplier are also copied
  from the morph entity.
- Most mob forms use a horse-style charged jump: hold jump to charge and
  release to jump. Maximum charge has the same height as a normal player jump.
- Maximum jump charge is reached after 8 ticks and remains full while jump is held.
- While charging, the vanilla horse jump gauge replaces the experience bar; its cooldown texture is shown briefly after jumping.
- Rabbit form instead hops automatically while movement input is held. It uses
  vanilla rabbit-style `0.2` walking and `0.3` sprinting jump velocity, with
  20-tick and 3-tick cooldowns respectively. Hop launch also supplies explicit
  horizontal velocity, with sprinting hops moving faster than walking hops.
- Rabbit form cannot move horizontally while grounded between hops. Holding
  space together with movement input changes the next hop to the player's
  normal jump height while retaining the rabbit hop cooldown.
- Most mob forms move at quarter speed when strafing or moving backward. The
  forward component of diagonal movement remains unchanged.
- Horse, donkey, and mule forms use the mounted-horse direction factors:
  forward `1.0x`, sideways `0.5x`, and backward `0.25x`.
- Non-equine forms first apply the established `0.25x` mob-to-player control
  conversion to their NBT-adjusted `minecraft:movement_speed`, then apply
  their vanilla AI walk and run multipliers:
  - Cow: `1.0x` walk, `2.0x` run
  - Sheep: `1.0x` walk, `1.25x` run
  - Chicken: `1.0x` walk, `1.4x` run
  - Cat and ocelot: `0.8x` walk, `1.33x` run
  - Wolf: `1.0x` walk, `1.5x` run
  - Pig: `1.0x` walk, `1.25x` run
  - Rabbit: `0.6x` walk, `2.2x` flee/run
  - Horse, donkey, and mule retain mounted-style direct movement speed.
    Sprint input does not add the player's `1.3x` speed modifier because
    vanilla ridden equines use their movement-speed attribute directly.
- In water, mob-form movement input is scaled by the transformed mob's actual
  movement-speed attribute and swimming sprint is disabled, matching ordinary
  land mobs' slow water movement. Jump ascent and sneak descent use the same
  scale as forward water input. NBT attribute overrides are reflected.
- Inventory capacity scales with the form's height:
  - Player: 9 hotbar + 27 inventory slots
  - Cow: 7 hotbar + 21 inventory slots
  - Sheep: 6 hotbar + 19 inventory slots
  - Chicken: 3 hotbar + 9 inventory slots
- Items in slots disabled by a smaller form move into active slots; overflow is dropped.
- Disabled inventory, hotbar, armor, and form-specific equipment slots are
  fully hidden. The vanilla slot areas are cleared and only active slots are
  redrawn, preventing leftover borders from disabled slots.
- The player inventory screen height shrinks with the number of active
  main-inventory rows. Active slots use up to nine columns, and each partial
  row and the active hotbar are centered independently.
- NBT size changes such as `Age:-24000` further scale inventory, food capacity,
  mining speed, dimensions, collision, eye height, block reach, and entity
  interaction reach relative to the adult form.
- Block and entity interaction ranges are proportional to the NBT-adjusted
  morph height relative to the vanilla player height.
- Maximum food scales with form height: player 20, cow 15, sheep 14, and chicken 10.
- NBT-based size scaling cannot reduce a mob form's maximum food below 8.
- Mob forms cannot eat normal food. They eat their breeding items for 4 food points instead.
- Breeding-item diets cover each supported form, including fish for cats and
  ocelots, meat for wolves, pig food, and horse food.
- Breaking and placing blocks, charged jumping, sprinting, and sneaking add extra exhaustion in mob form.
- Mob forms cannot break or place blocks while airborne or sprinting. Other block interactions remain available.
- Mob forms cannot interact with `Mob` entities, preventing riding animals,
  villager trading, taming, feeding, and similar mob interactions. Boats and
  minecarts remain interactable so they can still be boarded.
- While a mob-form player rides a boat or minecart, forward, backward,
  strafing, jumping, and sprinting inputs are discarded. Sneaking remains
  available for dismounting.
- Forms without attack AI deal no attack damage. Cat, ocelot, and wolf forms
  use their NBT-adjusted vanilla attack-damage attribute. Rabbit form does so
  only for the hostile `evil` variant.
- Attacking does not increase awkwardness for forms whose NBT-adjusted morph
  actually has attack AI. Weapon use by non-attacking forms still does.
- Attack-capable predators can eat meat from their natural prey with the
  item's vanilla food values and effects: cats eat rabbit, ocelots eat
  chicken, and wolves eat mutton or rabbit, raw or cooked.
- Cat, ocelot, and wolf forms reproduce their vanilla `LeapAtTargetGoal`
  attack movement. Attacking a living target while grounded launches the
  player toward it with the form's original horizontal and vertical velocity.
- Sheep forms are hunted by wild wolves; chicken forms are hunted by foxes
  and ocelots; rabbit forms are hunted by foxes and wild wolves.
- Avoidance is configured with `combat.avoided_by`; built-in cat and ocelot
  forms list `minecraft:creeper` and use the creeper's vanilla cat avoidance
  distance and speeds.
- Cat, ocelot, and wolf forms are nocturnal but are never forced to sleep or
  wake. Their manual soft-surface sleep action is allowed during daytime,
  bypassing the vanilla nighttime-only bed rule, and is rejected at night.
- Mob forms cannot wear humanoid head, chest, leg, or foot armor. Unsupported
  equipped items are returned to active inventory slots or dropped if full.
- The player inventory screen adds form-specific body and saddle slots:
  pigs and equines can equip saddles, horses can equip horse armor, wolves can
  equip wolf armor, and donkeys or mules can equip a chest.
- Horse, donkey, and mule inventory screens follow the mounted-horse layout:
  saddle and body equipment are arranged vertically on the left, and the
  offhand slot moves below them.
- All mob forms reduce player-inventory crafting from 2x2 to a single input
  slot. The other three input slots are hidden and reject interaction; items
  already in those slots are returned to active inventory when transforming.
- The morph inventory adds `mob_life:log_to_crafting_table`, a shapeless recipe
  that converts one item in `#minecraft:logs` into one crafting table. It is
  explicitly preferred over vanilla log-to-planks recipes in the one-slot
  player crafting grid.
- The recipe-book button is hidden for mob-form player inventories because its
  normal 2x2 placement logic can target disabled crafting slots. Horse, donkey,
  and mule forms retain the same one-slot manual crafting area as other forms.
- A chest-equipped donkey or mule uses a mounted-inventory-style layout. Its
  size-based personal storage remains unchanged, and a separate persisted
  15-slot container is shown as a 5-by-3 chest grid in the upper right.
- The one-slot crafting area is placed above the chest grid so it does not overlap
  the chest or personal storage. Removing the chest returns its contents to
  active personal slots and drops only overflow; chest contents also follow
  normal player save, death-drop, and keep-inventory behavior.
- The entity preview in the player inventory is fixed facing forward. Morph
  rendering preserves the preview render state's body, head, and pitch values.
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
- Hostile detection range scales with awkwardness: at 30 or below monsters
  cannot detect the player, from 30 to 100 their normal follow range is
  restored linearly, and at 100 it matches vanilla. Vision interference still
  strengthens above 70. At 90 and above, breaking, placing, and block
  interaction are disabled.
- A small circular awkwardness indicator is always shown above the centered
  experience-level number while transformed. Its color changes continuously
  from green at 0 through yellow to red at 100.
- Skeleton ranged attacks aim at a transformed player's morph eye height
  instead of the vanilla player-sized target position.
- At 30 awkwardness or below, the configurable `V` key sleeps without a bed
  on grass, wool, carpet, hay, moss, or beds. This sleep consumes the exact
  `sleep.food_cost` configured for the form, requires more current food than
  that cost, still checks for nearby monsters, and requires 200 ticks instead
  of 100. Its sleep timer cap is extended only for this soft-surface sleep.
- First-person empty hands use the mob front leg or wing.
- When holding an item or map, the mob hand is not rendered so it does not overlap the item.
- Mob forms emit their NBT-adjusted entity's normal ambient sounds using
  vanilla mob timing and probability. Hurt sounds also use the transformed
  entity's sound event, volume, and voice pitch on both server and client.
- Normal player form removes all Mob Life attribute modifiers and restores vanilla rendering, dimensions, health, movement, mining speed, and vision.
- Mob forms apply a form-specific low-saturation dichromatic post-processing effect.
- Mob vision widens the camera using each form's
  `vision.field_of_view_multiplier`, capped at 150 degrees. Movement-speed
  changes contribute only 10% of vanilla's dynamic FOV effect while
  transformed, so walking and sprinting do not substantially alter the field
  of view.
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
- `gameplay/inventory/MorphEquipment`: armor restrictions and form-specific
  saddle, body-armor, and chest validation.
- `server/ServerMorphManager`: world morph lifecycle, charged jumps, and recurring form effects.
- `server/ServerPlayerMorphApplier`: attributes, dimensions, capacity, and client synchronization.
- `client/state`: active client form, render-entity cache, and charged-jump input state.
- `client/render`: mob hand, sized hotbar, and jump-bar renderers.
- `client/screen`: standalone client screens such as world-form selection.
- Common mixins are grouped by `food`, `gameplay`, `inventory`, and `player`.
- Client mixins are grouped by `gameplay`, `inventory`, `player`, `render`, and `world`.

## Shader Resources

- Shared shader: `assets/mob_life/shaders/post/mob_vision.fsh`
- The shared shader applies form-specific dichromatic color and mild
  desaturation across the full image. The main depth buffer progressively adds
  blur, lower contrast, darkening, and haze to distant scenery.
- The base pass progressively blurs and darkens the peripheral view, beginning
  outside the central 30% and reaching a 5-pixel blur and 40% brightness at
  the screen edges.
- The base pass preserves the source luminance, and distant darkening and haze
  use the current sky brightness so daytime and nighttime remain distinguishable.
- Vision rendering uses two passes: distant effects run immediately after
  world rendering while the world depth buffer still exists; base dichromatic
  vision runs after first-person hand rendering without consulting depth.
- Post chains:
  - `assets/mob_life/post_effect/cow_vision.json`
  - `assets/mob_life/post_effect/sheep_vision.json`
  - `assets/mob_life/post_effect/chicken_vision.json`
- `VisionConfig.Settings.x` controls retained saturation. Current values are
  intentionally muted but not near grayscale. `Settings.w` controls the
  peripheral blur radius in pixels.
- `DistanceBlur.Parameters` contains start distance, full-blur distance,
  full-darkening distance, and full-fog distance.
- `DistanceBlur.Effects` controls maximum distant blur radius, current sky
  brightness, peripheral edge brightness, and distant haze strength.
- `DistanceBlur.DepthRange` is updated every frame with Iris-compatible near
  and far planes (`0.05` and effective render distance in blocks), allowing the
  shader to linearize the depth buffer before applying distance thresholds.
- `DistanceBlur.DepthRange.w` stays at the base `1.0` through 70
  awkwardness, then increases to `2.0` at 100. The base therefore uses the
  former 100-awkwardness vision strength without additional awkwardness
  effects, while values above 70 further strengthen desaturation, darkening,
  fog, and blur.
- Every supported mob has an individual post-effect profile. Color response,
  retained saturation, contrast, low-light brightness, peripheral degradation,
  and distance-effect parameters are loaded from its morph data-pack JSON and
  synchronized to the client.

## Repository State

- The repository was created from a Fabric template.
- Deleted `com.example` and `modid` template files are intentional.
- Do not restore template files or revert unrelated user changes.
- Third-party attribution is recorded in `THIRD_PARTY_NOTICES.md`.
- Mob Life itself is released under the MIT License; third-party dependencies and
  implementation references retain their own licenses.
