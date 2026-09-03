# Movement, Reach, and Mining Features

- The `player` form uses vanilla player dimensions, collision, eye height,
  movement, jump, mining, and reach behavior. Morph dimension hooks require an
  actual mob form, and form refresh relies on vanilla `refreshDimensions`
  without a second manual bounding-box placement. Switching back to this form
  removes every Mob Life-owned player attribute modifier on both server and
  client; the client also maintains this invariant each tick so a delayed old
  form attribute packet cannot leave local step-height or movement prediction
  active.
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
  10-tick, 10-tick, and 3-tick landing cooldowns respectively. The cooldown
  begins after landing, matching native Rabbit's normal and fast hop delays.
  Hop launch also supplies explicit horizontal velocity of `0.15`, `0.2`, and
  `0.35` for sneak, walking, and sprinting hops.
- Rabbit form retains horizontal momentum while grounded between hops. Holding
  space together with movement input changes the next hop to the player's
  normal jump height while retaining the rabbit hop cooldown. Standing still
  with the hop cooldown ready still allows jump-key hops.
- Most mob forms move at quarter speed when strafing or moving backward. The
  forward component of diagonal movement remains unchanged.
- Quadruped forms turn their camera and body while converting sideways input
  into forward movement. It is configured with `movement.quadruped_turning`
  and `movement.quadruped_turn_speed`.
  The conversion reads the polled common `ClientInput`, so remapped keyboard
  controls and Controlify analog movement use the same path. Analog sideways
  input proportionally scales both the turn rate and converted forward input.
- Horse, donkey, and mule forms use the mounted-horse direction factors:
  forward `1.0x`, sideways `0.5x`, and backward `0.25x`.
- Movement speeds are tracked as `generic.movement_speed` values in sneak,
  walk, and sprint order. Sneaking uses the configured sneak-to-walk ratio
  through `generic.sneaking_speed`:
  - Player: `0.03` sneak, `0.1` walk, `0.13` sprint effective
  - Cow: `0.15` sneak, `0.2` walk, `0.4` sprint
  - Sheep: `0.1725` sneak, `0.23` walk, `0.2875` sprint
  - Chicken: `0.1875` sneak, `0.25` walk, `0.35` sprint
  - Cat: `0.18` sneak, `0.18` walk, `0.65` sprint
  - Ocelot: `0.18` sneak, `0.18` walk, `0.65` sprint
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

- Instinct Mode discards normal player locomotion and advances a hidden,
  NBT-matched native Mob as an AI proxy. The proxy's goals, navigation,
  movement attributes, step handling, velocity, body/head rotations, and native
  panic or flee reactions drive the transformed player. Detached proxies reset
  `noActionTime` before each AI tick to match a registered nearby Mob and keep
  idle stroll goals eligible.
- The server sends the proxy-derived absolute position and grounded state in
  each Instinct state update. The local player applies it immediately before
  its normal position-send point and suppresses that vanilla position packet,
  preventing client prediction from reverting server-authoritative AI motion.
- Rest and wander can transition autonomously. Holding forward for about one
  second while resting requests wandering; backward stops the active wander
  goal and movement control even when pressed while airborne. Airborne forms
  retain only their current physical momentum until landing. Grounded rest
  clears residual horizontal velocity and displacement so stopped forms do not
  slide. A wander-to-rest transition also clears a stale move-control target,
  preventing rabbits from repeatedly hopping in place after their path ends.
  Resting sideways input turns the body by up to 10 degrees per tick, while
  wandering samples its next direction inside 15 degrees around
  `camera yaw + normalized sideways input * 15 degrees`, where right is positive
  and left is negative. A newly engaged, reversed, or substantially changed
  analog input replans immediately; failed paths retry quickly. Analog values
  come from the common client input path, including Controlify. Proxy rotation
  is initialized from the player once and then remains AI-authoritative so the
  vanilla player body alignment pass cannot undo accumulated rest turning or
  native AI turns. Rest turning accumulates from the retained pre-AI-tick body
  yaw, preventing the native stationary head-alignment pass from resetting each
  input step. Once rest turning is used, that facing remains locked after
  release until the AI leaves rest; untouched autonomous rest still permits
  native facing changes. Diagonal forward-and-sideways input does not invoke
  in-place rest turning; its sideways component biases the requested wander.
- While an Instinct-controlled rabbit is moving, its body approaches the
  measured hop direction by at most 15 degrees per tick instead of adopting a
  stale navigation-node yaw. During rest or wander, a native body-yaw change
  with no horizontal displacement is discarded unless it came from explicit
  rest-turn input. This prevents sideways-looking travel and camera reversals
  at path boundaries without replacing Rabbit navigation or hop physics.
- In water, an Instinct-controlled rabbit caps carried horizontal velocity to
  the native per-tick water acceleration and does not accumulate upward FloatGoal
  impulses between ticks. Entering water also ends the land jump state. Native
  path direction and fresh liquid-jump impulses remain active, while
  damage-derived external motion gets a five-tick exemption from these caps.
- Camera yaw is limited to 75 degrees from the body and pitch to 40 degrees.
  Native head motion approaches at 10 degrees per tick; without a look target,
  the camera uses proportional restoration capped at 0.5 degrees per tick
  toward the body. Body rotation carries the independently stored relative
  camera offset without treating server rotation synchronization as user input.
  Instinct mode overrides `LocalPlayer#getViewYRot` and `getViewXRot` so render
  frames interpolate the previous and current tick angles. Raw look input adds
  the same delta to both angles, keeping direct camera input immediate while
  smoothing only the latest 20 Hz body-yaw target.

- Mining speed uses the `minecraft:block_break_speed` attribute:
  - Cow: approximately `0.78x`
  - Sheep: approximately `0.72x`
  - Chicken: `0.5x`
