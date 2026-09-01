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
- Awkwardness does not decrease automatically. Only an explicit command or
  respawn can lower it.
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
