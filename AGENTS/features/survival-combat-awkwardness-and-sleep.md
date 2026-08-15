# Survival, Combat, Awkwardness, and Sleep Features

- Maximum food scales with form height: player 20, cow 15, sheep 14, and chicken 10.
- NBT-based size scaling cannot reduce a mob form's maximum food below 8.
- Mob forms cannot eat normal food. They eat their breeding items for the configured
  diet nutrition; all built-in forms currently restore 4 food points.
- Breeding-item diets are defined through `mob_life` item tags and keep a
  realistic split between meat, fish, grain, pasture feed, root vegetables,
  and fruit. Forms that already used wheat or seeds keep those foods in their
  diets.
- Form-specific item meals, normal-control grass meals, and instinct feeding
  or predation play vanilla's generic eating sound only for the player who
  consumed the meal. Server-side consumption does not broadcast that sound to
  other players. Normal grass eating repeats the sound every four ticks while
  its crouched eating action remains valid. Instinct feeding repeats it at the
  same interval only while the selected action is `EAT` and the player is
  stationary for grazing or a consumed meal; higher-priority actions stop it.
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
- With zero awkwardness, instinct mode enters automatically after 10 seconds
  with no gameplay input. The server measures input and view rotation, and owns
  the instinct level used for exiting.
  The idle delay decreases to zero as awkwardness reaches 100, which forcibly
  enters instinct mode at the maximum. Instinct level starts at 100 and
  regenerates by 10 per second up to 100. Each distinct attack, use, or jump
  press reduces it by `10 * (1 - awkwardness / 100)` only during `REST` or
  `WANDER`; reaching zero then exits. `LOOK`, forced entries, pursuit, fleeing,
  feeding, and every other state are ineligible for exit. The client sends only
  input-kind flags, and the server validates the exact state and eligibility, computes the
  reduction from authoritative awkwardness, and accepts each input kind at most
  once per player tick. Attack and use use Minecraft's resolved key mappings,
  while jump uses the polled common `ClientInput`, so Controlify controller
  bindings follow the same press-edge path without treating a held action as
  repeated presses. At awkwardness 100 every press reduces zero. Spectator and sleeping
  players cannot enter instinct mode, and leave it if they become ineligible.
  A player already in instinct mode remains in it when forced into a boat,
  minecart, or another vehicle; only forced entries may start while riding.
  A detached copy of the selected mob runs its complete native AI step
  for rest, wandering, fleeing, hunting, melee attacks, grass eating, and
  rabbit garden raids, then transfers its native movement vector to the player.
  Only forward, lateral, and view input can intervene in the states that allow
  them: forward makes a chance-based local wander request from rest or continues
  an active wander, while lateral input immediately turns the resting body or
  active wander search direction. Keyboard and Controllify controller input use
  the same intervention path. The mode does not display a text overlay
  or crosshair. Its visual effect is active for the whole mode;
  per-morph data packs can disable it or scale its intensity with
  `instinct.visual_effect.enabled` and `instinct.visual_effect.strength`. It
  does not target blocks, and its first-person mob limb remains still.
  The server-authoritative instinct level is included in the control sync.
  As it falls from 100 to zero, the original instinct tint fades continuously
  back to the form's normal vision color. The locked-hotbar icon changes from
  red at 100 to gray at zero using the same synchronized level.
  The active-mode preference is saved with the player and restored after
  reconnecting to the world. Respawn removes the old controller and transition
  meter and clears the persisted preference on both player instances before
  initializing the new player, so instinct mode is never restored by respawn.
- Feline hunting stores `instinct.hunting.feline_sprint_start_distance` per
  morph. Built-in cat and ocelot forms switch to their dash at 8 blocks.
- Outside instinct mode, a hungry sheep-form player eats the grass block below
  after remaining grounded and stationary for 40 ticks while crouching and
  looking downward by at least 30 degrees. A successful meal restores 2 food
  points and starts a separate 1200-tick (60-second) normal-control cooldown;
  it neither reads nor changes the instinct feeding cooldown.
- In instinct mode, sheep grass eating uses the adult vanilla goal's `1/1000`
  random trigger (or the baby goal's `1/50` trigger) during ordinary hunger.
  At or below 30% of maximum food, the random gate is bypassed and an edible
  grass block starts the goal immediately. Its eating animation and block
  conversion remain vanilla behavior. Rabbits use the native garden-raid
  interval of 200-399 ticks during ordinary hunger and bypass that interval
  at or below 30% when a mature carrot is available. The locked instinct view
  pitches down toward grass or a carrot while the corresponding eating
  animation is active, but not while a rabbit is navigating to a crop. After
  a successful meal, the sheep configuration applies a 1200-tick (60-second)
  cooldown.
- Instinct kills consume configured prey directly and suppress non-player mob
  loot and experience. Their configured nutrition is restored when the eating
  animation completes. While in instinct mode, a hunt-capable form pursues
  configured prey whenever its `post_kill_cooldown_ticks` has elapsed. At or
  below 30% of maximum food, that cooldown is ignored until food rises above
  30%. A pursuit can detect prey through walls, but stops after
  `pursuit_timeout_ticks`; it then applies
  `abandoned_hunt_cooldown_ticks`, which cannot be bypassed by hunger. The
  selected prey remains fixed throughout a pursuit while it is alive; another
  prey is selected only after the target is lost or the pursuit is abandoned.
  The built-in attack interval is 10 ticks, the post-kill cooldown is 400 ticks
  (20 seconds), the pursuit timeout is 400 ticks, and the abandoned-hunt
  cooldown is 1200 ticks (60 seconds); all are configurable per morph.
  Panic and flee behavior take priority over hunting: danger clears the shadow
  target before its AI step and prevents prey acquisition or attacks until the
  threat has passed.
- Built-in hunting nutrition follows the adjusted prey values: cats restore 4
  from rabbits and 3 from baby turtles; ocelots restore 4 from chickens and 3
  from baby turtles; wolves restore 4 from sheep, rabbits, and foxes and 3
  from baby turtles; evil rabbits restore 5 from players and 4 from wolves.
  Sheep restore 2 from a grass meal, and rabbits restore 3 from a mature carrot
  raid. Turtle prey is limited to baby turtles by the native prey eligibility
  rule.
- Outside instinct mode, a hunt-capable form at or below 30% of maximum food
  forcibly enters instinct mode when configured prey is within its configured
  prey-sensing range. A form with configured non-hunting instinct food actions
  also forcibly enters at or below 30% when it can eat a nearby matching target:
  sheep require edible grass at their feet and rabbits require a mature carrot
  within the native garden-raiding range. The check follows the form's
  configured sensing interval. Maximum awkwardness, hunger-prey, and
  hunger-feeding forced entries are tracked separately from ordinary/restored
  instinct mode, so reconnect persistence and exit eligibility remain correct.
- A configured predator that acquires the transformed player with its existing
  target goal forcibly enters the player into instinct mode and the shadow mob
  continues to run only its own native panic and avoidance goals. Predator detection begins at
  awkwardness 30 with a
  zero-block range, grows linearly, and reaches the predator's vanilla
  `FOLLOW_RANGE` at awkwardness 70; it is capped at that native range above 70.
  The compatibility target goal retains each predator's vanilla LOS, sensing,
  `canAttack`, follow range, randomized acquisition interval, tame filtering,
  and target memory. Configured predators therefore attack a matching morph as
  native prey; the scaled range predicate is used only for forced instinct
  entry. A configured relationship does not add a common fallback flee goal:
  native goal priority, range, speeds, and continuation conditions determine
  whether the source mob actually flees.
- Foxes treat a rabbit-form player as native rabbit prey after the compatibility
  target goal selects it: an existing player-avoid goal ends, then the native
  Fox stalk, pounce, and melee goals run normally.
- Nearby living-entity scans are shared for up to 5 ticks by instinct sensing,
  forced hunting, outlines, predator acquisition, and nearby-same-mob decay.
- Instinct mode is the sole ordinary way to reduce awkwardness. A valid forward
  intervention attempt pauses both passive and nearby-same-mob decay
  for the configured `instinct.intervention.decay_pause_ticks`. While a valid
  forward or left/right direction-interference key is held, decay is paused
  whether its probability roll or path generation succeeds. No intervention adds
  awkwardness.
- While any client GUI is open, Idle Instinct's accumulated wait time is frozen
  without being reset. Forced predator, damage, hunger, and maximum-awkwardness
  entry checks continue normally. The GUI adds 2 awkwardness per second through
  the ordinary gain path, so critical hunger doubles that amount to 4.
- At or below 30% of the form's maximum food, every awkwardness gain is doubled
  and passive plus nearby-same-mob decay are halved. The threshold is shared
  with hunger-forced instinct entry and critical-hunger hunting.
- Sheep forms are hunted by wild wolves; chicken forms are hunted by foxes
  and ocelots; rabbit forms are hunted by foxes and wild wolves.
- Avoidance is configured with `combat.avoided_by`; built-in cat and ocelot
  forms list `minecraft:creeper` and use the creeper's vanilla cat avoidance
  distance and speeds. This avoidance works independently of hostile detection
  and prevents the creeper from selecting the transformed player as an attack
  target.
- Cat, ocelot, and wolf forms are nocturnal but are never forced to sleep or
  wake. Their sleep on soft surfaces or in beds is allowed during daytime and
  is rejected at night.
- Every second, a nocturnal form gains 0.2 awkwardness at local raw light level 8 or above,
  while a diurnal form gains 0.2 at local raw light level 0 through 7. The schedule's `day`
  value denotes nocturnal and `normal` denotes diurnal; `never` has no circadian gain. Critical
  hunger applies the normal 2x awkwardness gain multiplier to this environmental gain.

- Each player has a persisted and client-synchronized awkwardness value from
  0 to 100. It resets to 0 on respawn.
- Awkwardness can be set for the command executor with
  `/moblife awkwardness <0-100>`, or for a selected player with
  `/moblife awkwardness <0-100> <target>`.
- The awkwardness HUD is a disabled-by-default debug display. It can be
  toggled from the global config and is saved in `config/mob_life.json`.
  When Mod Menu is installed, that screen is available from the mod list.
- Outside instinct mode, breaking, placing, block interaction, non-forward
  movement, and attacks gain `2x` their former awkwardness. A single large
  action cannot add more than 20, keeping failed-form combat from reaching the
  cap in only a few actions.
- Damage that removes health or absorption from a mob-form player adds 20 to 50
  awkwardness based on final damage relative to the player's current maximum
  health; maximum-health damage adds 50. It applies during instinct mode, while
  damage that the player fully blocks or otherwise negates adds none. This
  increase has a 120-tick (6-second) cooldown; native damage responses still
  evaluate every valid damage event.
- Damage forces instinct mode only when the detached source mob would change AI
  behavior. Its native `PanicGoal` decides panic damage, including the
  distinction between animals that panic from attacks and wolves that panic
  only from environmental damage such as fire. A native `HurtByTargetGoal`
  decides retaliation with its normal `canAttack`, team, tame, universal-anger,
  and ignored-attacker conditions. A wolf's native
  `ResetUniversalAngerTargetGoal` also owns the universal-anger response. Damage
  that starts none of these goals does not force instinct mode. Panic and flee
  preempt retaliation; retaliation preempts feeding, ordinary hunting, herd
  movement, and wandering. For panic caused by a living attacker, the panic
  destination search uses a 16-block horizontal and 4-block vertical range and
  is biased away from that attacker; environmental panic retains vanilla
  random-position and fire water-search behavior. The source mob's target
  selector owns retaliation continuation and termination.
- Built-in hunting sensing ranges are 24 blocks for cats and ocelots, 32 for
  rabbits, and 32 for wolves. These ranges apply both to automatic hungry
  entry and to prey acquisition during instinct mode.
- Hunting and feeding cooldowns are persisted as world-time expirations, so
  reconnecting or leaving instinct mode cannot reset them.
- Passive decay and the nearby-same-mob bonus apply only during instinct mode.
  Passive decay depends on the total item count in active inventory slots. An
  empty inventory decays at `5x` the base rate, with the multiplier approaching
  `1x` as more items are carried.
- Successful block breaking and block placement each add 4 awkwardness outside
  instinct mode.
- Awkwardness multiplies mob-form exhaustion up to `3x` at 100.
- Hostile detection range scales with awkwardness: at 30 or below enemies
  cannot detect the player, from 30 to 100 their normal follow range is
  restored linearly, and at 100 it matches vanilla. Vision interference still
  strengthens above 70. While in instinct mode, enemies cannot acquire the
  transformed player as a new target regardless of awkwardness; an enemy that
  already targets that player through a goal or Brain memory keeps its existing
  target. At 90 and above,
  breaking, placing, and block interaction are disabled.
- An experience-orb-like awkwardness indicator is always shown above the
  centered experience-level number while transformed. Its tint changes
  continuously from green at 0 through yellow to red at 100.
- Skeleton ranged attacks aim at a transformed player's morph eye height
  instead of the vanilla player-sized target position.
- Below 30 awkwardness, the configurable `V` key sleeps without a bed on
  grass, wool, carpet, hay, moss, or beds. It does not consume food; completing
  the sleep adds 70 awkwardness afterward. It still checks for nearby monsters
  and requires 200 ticks instead of 100. Its sleep timer cap is extended only
  for this soft-surface sleep.
