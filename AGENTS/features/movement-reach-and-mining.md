# Movement, Reach, and Mining Features

- All non-rabbit mob forms share a 10-tick landing cooldown before the next
  jump. Most mob forms still use a horse-style charged jump: hold jump to
  charge and release to jump. Maximum charge has the same height as a normal
  player jump.
- Non-equine charged-jump forms reach maximum jump charge after 4 ticks;
  horse, donkey, and mule keep the 8-tick charge time. Charge remains full
  while jump is held.
- While charging, the vanilla horse jump gauge replaces the experience bar; its cooldown texture is shown briefly after jumping.
- Cat, ocelot, and wolf use a plain jump instead of charged jump, but still
  obey the same 10-tick landing cooldown.
- Rabbit form instead hops automatically while movement input is held. It uses
  vanilla rabbit-style `0.2` walking and `0.3` sprinting jump velocity, with
  10-tick and 3-tick cooldowns respectively. Hop launch also supplies explicit
  horizontal velocity, with sprinting hops moving faster than walking hops.
- Rabbit form retains horizontal momentum while grounded between hops. Holding
  space together with movement input changes the next hop to the player's
  normal jump height while retaining the rabbit hop cooldown. Standing still
  with the hop cooldown ready still allows jump-key hops.
- Most mob forms move at quarter speed when strafing or moving backward. The
  forward component of diagonal movement remains unchanged.
- Horse, donkey, and mule forms use the mounted-horse direction factors:
  forward `1.0x`, sideways `0.5x`, and backward `0.25x`.
- Movement speeds are tracked as `generic.movement_speed` values:
  - Player walk: `0.1`, sprint effective: `0.13`
  - Cow: `0.2` walk, `0.25` dash, `0.4` fast sprint
  - Sheep: `0.23` walk, `0.253` dash, `0.2875` fast sprint
  - Chicken: `0.25` walk, `0.275` dash, `0.35` fast sprint
  - Cat: `0.18` walk, `0.24` dash, `0.399` fast sprint
  - Ocelot: `0.18` walk, `0.24` dash, `0.399` fast sprint
  - Wolf: `0.3` walk, `0.45` dash; fast sprint removed
  - Pig: `0.25` walk, `0.3` dash, `0.3125` fast sprint
  - Rabbit: `0.18` walk, `0.66` dash; fast sprint removed
  - Horse, donkey, and mule: `0.225` ridden speed; sprint input does not add
    the player's `1.3x` modifier, so walk and dash use the same base speed
- The built-in `data/mob_life/mob_life/morphs/*.json` files, including
  `player.json`, store each form's `reference_mob_speed`, and the code converts
  the configured `generic.movement_speed` values to `block/s` by comparing the
  live morph attribute against that reference and applying the vanilla ratio
  for the current form. NBT-adjusted `Age:-24000` or attribute overrides scale
  the result proportionally.
- In water, mob-form movement input is scaled by the transformed mob's actual
  movement-speed attribute and swimming sprint is disabled, matching ordinary
  land mobs' slow water movement. Jump ascent and sneak descent use the same
  scale as forward water input. NBT attribute overrides are reflected.

- NBT size changes such as `Age:-24000` further scale inventory, food capacity,
  mining speed, dimensions, collision, eye height, block reach, and entity
  interaction reach relative to the adult form.
- Block and entity interaction ranges are proportional to the NBT-adjusted
  morph height relative to the vanilla player height.

- Mining speed uses the `minecraft:block_break_speed` attribute:
  - Cow: approximately `0.78x`
  - Sheep: approximately `0.72x`
  - Chicken: `0.5x`
