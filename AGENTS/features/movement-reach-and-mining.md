# Movement, Reach, and Mining Features

- The `player` form has no morph JSON and uses vanilla player dimensions,
  collision, eye height, movement, jump, mining, and reach behavior. Switching
  back removes all Mob Life attribute modifiers and clears `NoGravity`.
- Normal-mode mob locomotion is client-authoritative. The client applies input,
  gait, and vanilla travel physics once; the server does not duplicate the
  movement calculation. Successful gait events only trigger server-side sound,
  jump-stat, and exhaustion side effects.
- Each mob config has schema version 2 movement states. Cat and ocelot define
  `sneak`, `walk`, and `sprint`; every other form defines `walk` and `sprint`.
  Each state contains only `goal_speed_modifier` and
  `movement_speed_attribute_multiplier`.
- Controller speed is the effective NBT-adjusted movement-speed attribute times
  both state multipliers. It is assigned as both LivingEntity speed and input,
  allowing vanilla travel to produce the MoveControl-like squared ground term
  and native air/water acceleration, friction, drag, slopes, collision, and
  external-force behavior.
- Built-in goal modifiers are: Cow `1/2`, Sheep `1/1.25`, Chicken `1/1.4`,
  Pig `1/1.25`, Wolf `1/1.5`, Rabbit `.6/2.2`, equines `.7/1`, and Cat/Ocelot
  `.6 sneak/.8 walk/1.33 sprint`. Cat/Ocelot also use a `1.3` sprint-only
  movement-speed attribute multiplier.
- Vanilla Player's implicit sprint modifier is removed in mob forms. Actual
  sprint state selects the sprint profile, lateral input alone cannot sustain
  sprint, and water always disables sprint.
- Normal land input is digital. W is inverse-rotated into view-relative travel
  input so it always follows body yaw. S is ignored, A/D rotate body,
  head, and camera by at most 10 degrees per tick without lateral translation,
  A+D cancels, and W wins over W+S. W+A/D combines turning with movement.
- Equines are the only exception while sprinting: body snaps to camera yaw and
  A/D supplies physical `0.5` lateral input. Their walking controls retain the
  shared turn-in-place behavior.
- Normal-mode body orientation no longer auto-follows head yaw. Without A/D,
  head/camera yaw and pitch recover to body forward by at most 2 degrees per
  tick. Recovery pauses while attack or use is held on a targeted block/entity.
  Tick-driven A/D and recovery retain their previous angles for render
  interpolation; direct view input updates current and previous angles together.
  Direct view input is bounded to body +/-75 yaw and +/-40 pitch. Changed body
  yaw is sent at most once per tick and relayed to current and late entity
  trackers.
- Ordinary non-rabbit, non-equine mob forms jump immediately using effective
  `jump_strength`, block jump factor, and Jump Boost. Holding jump repeats on
  landing. There is no landing cooldown or sprint forward boost.
- Rabbit auto-hops from W or jump, but not A/D alone. Walk uses `.2` vertical
  power and a 10-tick landing cooldown; sprint uses `.3` and 3 ticks; manual
  jump uses `.5`. All scale against Rabbit's `.42` source jump strength, so NBT,
  block factor, and Jump Boost remain effective.
- A low-speed Rabbit hop invokes vanilla `moveRelative(.1, vector)` with adult
  `(0, 1.5, 1)` or baby `(0, .5, 1)`, without snapping yaw. Ground W input is
  suppressed during the post-landing cooldown to prevent sliding.
- Rabbit form freezes vanilla camera walk phase and clears movement-bob
  amplitude in normal and Instinct modes. Physical camera height and non-walk
  camera effects remain active.
- Equine walk jump is disabled; copied `step_height` handles obstacles. Sprint
  jump starts only on a new jump press and fires on release with the exact
  LocalPlayer horse charge curve. Losing sprint, ground, or land state discards
  charge. Vertical power is effective `jump_strength * charge`; W adds
  `.4 * charge` forward velocity. The 10-tick bar cooldown is cosmetic only.
- Water uses native player fluid travel with copied mob attributes. Sprint and
  sneak descent are disabled; jump ascent retains vanilla `+0.04`. Entering
  water clears Rabbit hop and equine charge state. Lava remains vanilla.
- Chicken multiplies downward velocity by `.6` after native travel while
  airborne, outside water, and not flying.
- Mob forms copy effective `movement_speed`, `friction_modifier`,
  `air_drag_modifier`, `movement_efficiency`, `water_movement_efficiency`,
  `gravity`, `bounciness`, `step_height`, and `jump_strength` from the
  NBT-loaded source entity. `NoGravity` is copied; `Motion` and `OnGround` are
  sanitized, and `NoAI` does not disable player control.
- NBT size changes such as `Age:-24000` further scale inventory, food capacity,
  mining speed, dimensions, collision, eye height, block reach, and entity
  interaction reach relative to the adult form.
- Block and entity interaction ranges are proportional to the NBT-adjusted
  morph height relative to vanilla player height.
- Instinct mode remains server-authoritative and advances an NBT-matched native
  Mob proxy. Its goals, navigation, movement, collision, and position do not
  share the normal-mode client controller. ServerPlayer travel is suppressed
  while Instinct is active so gravity, step traversal, and external motion are
  integrated exactly once by the proxy rather than once per entity.
- Instinct water movement retains the native Mob's fluid velocity, FloatGoal,
  MoveControl, and fluid-exit jump. Gaze-biased random strolling is disabled
  while the proxy is in water so WaterAvoidingRandomStrollGoal keeps its native
  land destination and can drive GroundPathNavigation toward a reachable bank.
- Instinct body yaw follows native AI by at most 90 degrees per tick. Head yaw
  follows active LookControl or direct camera input by at most 10; without
  either it recovers to body forward and zero pitch by at most 2 degrees per
  tick. Head yaw hard-clamps to body +/-75 and pitch to +/-40. The independent
  camera follows head at most 30 degrees per tick with render interpolation, so
  it may temporarily lag outside the body/head limit. Direct view input remains
  immediate and bounded.
- Instinct movement feeds confirmed proxy horizontal displacement and final
  speed into `ClientAvatarState`. The client buffers at most five samples,
  consumes at most two per client tick, and holds only the last amplitude target
  for up to five ticks. Teleports and lifecycle replacement clear pending data.
- Mining speed uses the `minecraft:block_break_speed` attribute:
  Cow approximately `.78x`, Sheep approximately `.72x`, Chicken `.5x`.
