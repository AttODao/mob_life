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
- Available forms are normal player, cow, sheep, and chicken.
- The form can be changed later with:

```text
/moblife morph minecraft:player
/moblife morph minecraft:cow
/moblife morph minecraft:sheep
/moblife morph minecraft:chicken
```

- Dedicated servers require gamemaster permission for the command.
- Mob forms replace player rendering, animation, dimensions, collision box, eye height, movement speed, and maximum health.
- Chicken form has slower falling.
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
- Maximum food scales with form height: player 20, cow 15, sheep 14, and chicken 10.
- Mob forms cannot eat normal food. They eat their breeding items for 4 food points instead.
- Breaking and placing blocks, charged jumping, sprinting, and sneaking add extra exhaustion in mob form.
- Mob forms cannot break or place blocks while airborne or sprinting. Other block interactions remain available.
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
- Post chains:
  - `assets/mob_life/post_effect/cow_vision.json`
  - `assets/mob_life/post_effect/sheep_vision.json`
  - `assets/mob_life/post_effect/chicken_vision.json`
- `VisionConfig.Settings.x` controls retained saturation. Current values are intentionally muted but not near grayscale.

## Repository State

- The repository was created from a Fabric template.
- Deleted `com.example` and `modid` template files are intentional.
- Do not restore template files or revert unrelated user changes.
- Third-party attribution is recorded in `THIRD_PARTY_NOTICES.md`.
