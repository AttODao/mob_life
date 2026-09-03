# Survival, Combat, Awkwardness, and Sleep Features
- Maximum food scales with form height: player 20, cow 15, sheep 14, and chicken 10.
- NBT-based size scaling cannot reduce a mob form's maximum food below 8.
- Mob forms cannot eat normal food. They eat their breeding items for the configured
  diet nutrition; all built-in forms currently restore 4 food points.
- Breeding-item diets are defined through `mob_life` item tags and keep a
  realistic split between meat, fish, grain, pasture feed, root vegetables,
  and fruit. Forms that already used wheat or seeds keep those foods in their diets.
- Form-specific item meals and grass meals play vanilla's generic eating sound
  only for the player who consumed the meal. Server-side consumption does not
  broadcast that sound to other players. Grass eating repeats the sound every
  four ticks while its crouched eating action remains valid.
- Breaking and placing blocks, charged jumping, sprinting, and sneaking add extra
  exhaustion in mob form.
- Mob forms cannot break or place blocks while airborne or sprinting. Other
  block interactions remain available until the awkwardness action lock applies.
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
- A hungry sheep-form player eats the grass block below after remaining
  grounded and stationary for 40 ticks while crouching and looking downward
  by at least 30 degrees. A successful meal restores 2 food points and starts
  a 1200-tick (60-second) cooldown.
- Sheep forms are hunted by wild wolves; chicken forms are hunted by foxes
  and ocelots; rabbit forms are hunted by foxes and wild wolves. These
  relationships are configured by exact entity type IDs in `combat.predators`.
  The same list drives native predator targeting and red predator outlines.
- Configured predator goals retain vanilla LOS, sensing, `canAttack`, follow
  range, randomized acquisition interval, tame filtering, and target memory.
  Configured predators use their native range independently of the general
  hostile-detection scale.
- Foxes treat a rabbit-form player as native rabbit prey after the compatibility
  target goal selects it: an existing player-avoid goal ends, then the native
  fox stalk, pounce, and melee goals run normally.
- Avoidance is configured with `combat.avoided_by`; built-in cat and ocelot
  forms list `minecraft:creeper` and use the creeper's vanilla cat avoidance
  distance and speeds. This prevents the creeper from selecting the transformed
  player as an attack target.
- Cat, ocelot, and wolf forms are nocturnal but are never forced to sleep or
  wake. Their sleep on soft surfaces or in beds is allowed during daytime and
  is rejected at night.
- Every second, a nocturnal form gains 0.2 awkwardness at local raw light level
  8 or above, while a diurnal form gains 0.2 at local raw light level 0 through
  7. The schedule's `day` value denotes nocturnal and `normal` denotes diurnal;
  `never` has no circadian gain. Critical hunger applies the normal 2x multiplier.
- Rabbit forms gain 0.5 awkwardness per second while a configured predator is
  actively targeting them through vanilla goal logic.

- Each player has a persisted and client-synchronized awkwardness value from
  0 to 100. It resets to 0 on respawn.
- Awkwardness can be set for the command executor with
  `/moblife awkwardness <0-100>`, or for a selected player with
  `/moblife awkwardness <0-100> <target>`.
- Outside Instinct Mode, awkwardness does not decrease automatically. While
  Instinct Mode is active and no movement, camera, or button interference has
  occurred for five ticks, it decreases at `2.0 - 1.5 * inventory fill ratio`
  per second. All awkwardness increases are disabled during the mode.
- The awkwardness HUD is a disabled-by-default debug display. It can be
  toggled from the global config and is saved in `config/mob_life.json`.
  When Mod Menu is installed, that screen is available from the mod list.
- Breaking, placing, block interaction, non-forward movement, and attacks gain
  `2x` their former awkwardness. A single large action cannot add more than 20,
  keeping failed-form combat from reaching the cap in only a few actions.
- Damage that removes health or absorption from a mob-form player adds 20 to
  50 awkwardness based on final damage relative to the player's current maximum
  health; maximum-health damage adds 50. Fully blocked or otherwise negated
  damage adds none. This increase has a 120-tick (6-second) cooldown.
- At or below 30% of the form's maximum food, every awkwardness gain is doubled.
- Successful block breaking and block placement each add 4 awkwardness.
- Awkwardness multiplies mob-form exhaustion up to `3x` at 100.
- Hostile detection range scales with awkwardness: at 30 or below enemies
  cannot detect the player, from 30 to 70 their normal follow range is restored
  linearly, and at 70 or above it matches vanilla. Configured natural predators
  bypass this scaling. Vision interference strengthens above 70. At 90 and
  above, breaking, placing, and block interaction are disabled.
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

## Instinct Mode

- Each supported Mob form has an explicit code-registered native AI profile.
  Survival and Adventure enter after `10 * (1 - awkwardness / 100)` seconds of
  inactivity. Gameplay GUIs reset the timer; chat, pause, and option screens
  freeze it. Awkwardness 100 enters immediately. Creative always holds
  awkwardness at zero and disables every automatic entry cause; Spectator is
  unsupported.
- Automatic entry also occurs for a native panic-producing damage reaction,
  Love state, a reachable native prey/forage target at or below 30% food, or a
  native pre-avoid threat above awkwardness 30. Avoid distance scales linearly
  from zero at 30 to the form's native distance at 100. A vanished trigger does
  not exit the mode.
- Instinct level starts at 100 and gains 20 per second, capped at 100. A new
  jump, attack, use, or inventory press accepted during rest/wander lowers it by
  `10 * (1 - awkwardness / 100)`, at most once per tick. Held inputs must be
  released before counting again. Level zero exits.
- Native danger, feeding, Love, riding, and non-rest/wander goals reject
  resistance input. Movement uses 0.2 engage and 0.1 release thresholds, and
  camera movement counts after a cumulative one degree within five ticks.
- At critical hunger, native hunting and sheep/rabbit forage goals take priority.
  A configured natural prey killed by the active player drops neither loot nor
  experience and starts a stationary 40-tick feeding action. Completing it
  restores prey-configured food; danger, riding, or moving over two blocks
  cancels recovery. Sheep and rabbit forage use the form's `instinct.forage`
  value and require `mobGriefing`.
- Natural prey nutrition is loaded from the stacked exact resource
  `data/mob_life/mob_life/instinct/prey.json`. Higher packs replace entries;
  `enabled:false` or an invalid higher entry removes that prey. Built-ins are
  chicken 2/0.3, fox 3/0.3, rabbit 3/0.3, sheep 2/0.3, and baby turtle 1/0.1.
- Active native attacks use the Mob proxy rather than player weapons,
  enchantments, cooldown, advancements, or Looting. The proxy still attributes
  damage to the transformed player. Transformed players are treated as their
  biological form for native target and avoidance relations, never as ordinary
  Player prey.
- Adult chickens keep a persisted 6000-12000 tick egg timer that advances only
  while online in chicken form. A due egg waits for active Instinct Mode and at
  least three food points, shares the manual daily quota, and costs three food.
- Instinct active/level/idle, Love/cooldown, and egg timing persist through
  logout and dimension changes. Death clears the mode but keeps egg timing;
  form change clears mode, goal, Love, and cooldown while retaining egg timing.
