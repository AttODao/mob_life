# Movement, Reach, and Mining Features

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

- NBT size changes such as `Age:-24000` further scale inventory, food capacity,
  mining speed, dimensions, collision, eye height, block reach, and entity
  interaction reach relative to the adult form.
- Block and entity interaction ranges are proportional to the NBT-adjusted
  morph height relative to the vanilla player height.

- Mining speed uses the `minecraft:block_break_speed` attribute:
  - Cow: approximately `0.78x`
  - Sheep: approximately `0.72x`
  - Chicken: `0.5x`
