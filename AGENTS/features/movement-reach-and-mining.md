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
  friction, or gravity. Normal player travel input is discarded while this AI
  movement is active, so it cannot outrun the shadow's route or add speed on
  top of it. WANDER uses the source mob's original `RandomStrollGoal` speed
  modifier and live movement-speed attribute without scaling or capping it to
  the normal player-controlled morph speed. Position synchronization retains
  the shadow mob's own horizontal momentum between AI ticks instead of feeding
  client/server player corrections back into an active path. Player horizontal
  movement is imported only on controller initialization and damage response,
  while vertical and grounded state remain synchronized every tick. Rabbit AI
  hops consequently retain their native low, normal, and step-up jump heights.
  Grounded horizontal movement is zeroed after native AI arbitration in REST
  and LOOK so momentum from a completed route cannot leak into a resting state.
  In REST and LOOK, holding forward retries a chance-based, cooldown-limited
  request to begin a one- or two-leg local wander toward the current camera
  yaw. Releasing forward does not stop an accepted wander. While WANDER is
  active, forward steers a short-lived direction intent toward the camera yaw
  without directly changing velocity or turning the body. Left and right retry
  a chance-based intervention using the same probability as forward wander
  start: during WANDER a success turns the direction intent 15 through 45
  degrees to that side, and during REST it
  turns the body by the same range without beginning movement. Successful and
  failed lateral attempts use 20- and 10-tick base cooldowns respectively.
  A completed natural wander also starts the same forward-wander cooldown
  before another player-requested wander can begin.
- Direction intent is held for 20 ticks and fades over the following 40 ticks.
  It biases both prompted and natural wander candidates 70% toward the intent,
  preferring a 15-degree cone and falling back to 45 degrees or ordinary AI
  candidates where terrain requires it. Twelve candidates are evaluated so a
  narrow intent cone remains usable. An existing route is reconsidered only
  after at least 20 ticks and a 15-degree intent change; a missing replacement
  path leaves the current route active. Hunting, fleeing, feeding, and social
  movement clear direction intent and take priority.
- Instinct camera targets interpolate every rendered frame. Body turns use a
  dead zone and bounded angular speed, so rapidly changing AI headings cannot
  snap the camera. During unlocked REST, LOOK, and WANDER states, mouse or
  controller look input changes only the head and camera at one-quarter sensitivity. The
  camera-to-body yaw offset is retained while the body turns, then smoothly
  recenters while mouse yaw input is idle; horizontal and vertical camera
  offsets are each clamped to `-30` through `30` degrees.
- Cow, sheep, chicken, rabbit, wolf, pig, horse, donkey, and mule forms give
  nearby groups of at least two natural mobs of the same type priority over
  random and player-requested wandering. The group target is its local center;
  hunting, fleeing, and feeding remain higher priorities. The behavior is
  enabled per morph with `instinct.social`.
- Most mob forms move at quarter speed when strafing or moving backward. The
  forward component of diagonal movement remains unchanged.
- Outside instinct control, quadruped forms turn their camera and body while
  converting sideways input into forward movement. It is configured as an
  independent feature with `movement.quadruped_turning` and
  `movement.quadruped_turn_speed`; instinct input takes precedence while active.
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
