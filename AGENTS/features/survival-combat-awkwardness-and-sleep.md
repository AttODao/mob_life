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
- Sheep forms are hunted by wild wolves; chicken forms are hunted by foxes
  and ocelots; rabbit forms are hunted by foxes and wild wolves.
- Avoidance is configured with `combat.avoided_by`; built-in cat and ocelot
  forms list `minecraft:creeper` and use the creeper's vanilla cat avoidance
  distance and speeds.
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
- Breaking, placing, block interaction, non-forward movement, prolonged
  sprinting, and attacking increase awkwardness. Eating, staying near the same
  mob, and passive decay reduce it.
- Passive awkwardness decay depends on the total item count in active inventory
  slots. An empty inventory decays at `5x` the base rate, with the multiplier
  approaching `1x` as more items are carried.
- Successful block breaking and block placement each add 2 awkwardness.
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
