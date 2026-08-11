# Survival, Combat, Awkwardness, and Sleep Features

- Maximum food scales with form height: player 20, cow 15, sheep 14, and chicken 10.
- NBT-based size scaling cannot reduce a mob form's maximum food below 8.
- Mob forms cannot eat normal food. They eat their breeding items for 4 food points instead.
- Breeding-item diets are defined through `mob_life` item tags and keep a
  realistic split between meat, fish, grain, pasture feed, root vegetables,
  and fruit. Forms that already used wheat or seeds keep those foods in their
  diets.
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
  with no gameplay input. The idle delay decreases to zero as awkwardness
  reaches 100, which forcibly enters instinct mode at the maximum. Holding the
  mining button exits only from a state that accepts view input; it takes 3
  seconds at zero awkwardness, grows without bound as awkwardness reaches 100,
  and pursuit, fleeing, feeding, and other locked states cannot be exited. A detached copy of the selected mob runs its complete native AI step
  for rest, wandering, fleeing, hunting, melee attacks, grass eating, and
  rabbit garden raids, then transfers its native movement vector to the player.
  Only forward, jump, and view input can intervene in the states that allow
  them: forward alters the shadow mob's navigation; jump uses its jump control
  and the regular landing cooldown; locked target views allow a bounded manual
  offset that keeps the target in view. The mode does not display a text
  overlay or crosshair. Its visual effect is active for the whole mode;
  per-morph data packs can disable it or scale its intensity with
  `instinct.visual_effect.enabled` and `instinct.visual_effect.strength`.
  The active-mode preference is saved with the player and restored after
  reconnecting to the world; it is cleared by an explicit exit or respawn.
- Feline hunting stores `instinct.hunting.feline_sprint_start_distance` per
  morph, allowing each feline form to accelerate before its prey's close-range
  escape behavior can create an unrecoverable gap.
- In instinct mode, the sheep's hungry grass-eating goal starts immediately
  when it stands on an edible block or grass block, rather than waiting for
  the adult vanilla goal's `1/1000` random trigger. Its eating animation and
  block conversion remain vanilla behavior. The locked instinct view also
  pitches down toward the grass while this goal is active. It likewise lowers
  while a rabbit consumes a carrot, but not while it is navigating to a crop.
  After a successful meal, the sheep configuration applies a 400-tick
  (20-second) cooldown.
- Instinct kills consume configured prey directly, suppress non-player mob
  loot and experience, and restore the final nutrition value stored on each
  prey entry. Built-in doubled values are stored directly without a nutrition
  multiplier. Hunger thresholds gate new hunts. The built-in attack interval is
  10 ticks, while the post-kill hunting cooldown is 400 ticks (20 seconds);
  both values are configurable per morph through the hunting data-pack fields.
- Instinct mode is the sole ordinary way to reduce awkwardness. Its passive
  decay continues while forward, jump, or view input intervenes, and no
  intervention adds awkwardness.
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

- Each player has a persisted and client-synchronized awkwardness value from
  0 to 100.
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
- Passive decay and the nearby-same-mob bonus apply only during instinct mode.
  Passive decay depends on the total item count in active inventory slots. An
  empty inventory decays at `5x` the base rate, with the multiplier approaching
  `1x` as more items are carried.
- Successful block breaking and block placement each add 4 awkwardness outside
  instinct mode.
- Awkwardness multiplies mob-form exhaustion up to `3x` at 100.
- Hostile detection range scales with awkwardness: at 30 or below monsters
  cannot detect the player, from 30 to 100 their normal follow range is
  restored linearly, and at 100 it matches vanilla. Vision interference still
  strengthens above 70. At 90 and above, breaking, placing, and block
  interaction are disabled.
- An experience-orb-like awkwardness indicator is always shown above the
  centered experience-level number while transformed. Its tint changes
  continuously from green at 0 through yellow to red at 100.
- Skeleton ranged attacks aim at a transformed player's morph eye height
  instead of the vanilla player-sized target position.
- At 30 awkwardness or below, the configurable `V` key sleeps without a bed
  on grass, wool, carpet, hay, moss, or beds. This sleep consumes the exact
  `sleep.food_cost` configured for the form, requires more current food than
  that cost, still checks for nearby monsters, and requires 200 ticks instead
  of 100. Its sleep timer cap is extended only for this soft-surface sleep.
