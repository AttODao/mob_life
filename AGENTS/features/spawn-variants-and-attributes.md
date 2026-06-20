# Spawn, Variants, and Attributes Features

- During initial world creation, spawn-time NBT for all selectable mob forms is
  fixed before the seed search so the chosen world and initial spawn can match
  that selection. Cat, cow, chicken, pig, and wolf variants and sound
  variants, rabbit types, sheep colors, and horse/donkey/mule equine variants
  and attributes are prepared up front.
- When morph NBT does not already contain generated values, the morph entity is
  finalized at the selected initial-spawn location after that location has
  passed the morph's vanilla spawn placement and spawn-rule checks. This
  persists the selected vanilla variant and other spawn-time properties, while
  explicitly supplied NBT always overrides generated values.
- Initial spawn biome selection honors explicit warm/cold cow, chicken, and pig
  variants, rabbit white/gold variants, vanilla wolf variant biome rules, and
  cat structure contexts. Cat initial spawn uses a village biome and forced
  village structure placement; `all_black` cat uses a swamp-hut biome and
  forced swamp-hut placement. Otherwise the form is finalized in a biome where
  it can naturally spawn, so its generated variant is appropriate for that
  biome.
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
