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
  `0.2` sneak and walking jump velocity and `0.3` sprinting jump velocity, with
  10-tick, 10-tick, and 3-tick cooldowns respectively. Hop launch also supplies
  explicit horizontal velocity of `0.15`, `0.2`, and `0.35` for sneak, walking,
  and sprinting hops.
- Rabbit form retains horizontal momentum while grounded between hops. Holding
  space together with movement input changes the next hop to the player's
  normal jump height while retaining the rabbit hop cooldown. Standing still
  with the hop cooldown ready still allows jump-key hops.
- During instinct mode, every form runs a detached native mob's complete AI
  step, including path navigation, move control, jump control, gravity, and
  collision. The native movement vector passed into the shadow mob's own
  `Entity.move` call is captured and injected as the player's velocity
  immediately before player travel, avoiding a second copy of AI acceleration,
  friction, or gravity. Rabbit AI hops consequently retain their native low,
  normal, and step-up jump heights. In REST and WANDER only,
  forward input temporarily adds a native navigation goal in the current view
  direction. Jump input is queued briefly until grounded, obeys the regular
  landing cooldown, and requests the shadow mob's jump control with a minimum
  vertical velocity of `0.42`, sufficient for a one-block step; these inputs
  never act directly on the player.
- Most mob forms move at quarter speed when strafing or moving backward. The
  forward component of diagonal movement remains unchanged.
- Outside instinct control, quadruped forms turn their camera and body while
  converting sideways input into forward movement. It is configured as an
  independent feature with `movement.quadruped_turning` and
  `movement.quadruped_turn_speed`; instinct input takes precedence while active.
- Horse, donkey, and mule forms use the mounted-horse direction factors:
  forward `1.0x`, sideways `0.5x`, and backward `0.25x`.
- Movement speeds are tracked as `generic.movement_speed` values in sneak,
  walk, and sprint order. Sneaking uses the configured sneak-to-walk ratio
  through `generic.sneaking_speed`:
  - Player: `0.03` sneak, `0.1` walk, `0.13` sprint effective
  - Cow: `0.15` sneak, `0.2` walk, `0.4` sprint
  - Sheep: `0.1725` sneak, `0.23` walk, `0.2875` sprint
  - Chicken: `0.1875` sneak, `0.25` walk, `0.35` sprint
  - Cat: `0.18` sneak, `0.24` walk, `0.65` sprint
  - Ocelot: `0.18` sneak, `0.24` walk, `0.65` sprint
  - Wolf: `0.225` sneak, `0.3` walk, `0.45` sprint
  - Pig: `0.1875` sneak, `0.25` walk, `0.3125` sprint
  - Rabbit: `0.135` sneak, `0.18` walk, `0.66` sprint
  - Horse, donkey, and mule: `0.0421875` sneak, `0.05625` walk, `0.225` sprint
    Walking converts the NBT-adjusted native unmounted-mob speed into the
    player's movement scale. Sprinting retains the NBT-adjusted ridden forward
    speed, while sneaking remains 75% of walking speed.
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
