# World and Morph Selection Features

- A world-wide play form is selected during world creation and persisted in saved data.
- New worlds using a mob form move the initial overworld spawn to a position
  that also satisfies the mob's vanilla spawn placement and spawn-rule checks.
  During world creation, spawn-time NBT for all selectable mob forms is fixed
  before seed search so the chosen world can match that selection. Cat, cow,
  chicken, pig, and wolf variants and sound variants, rabbit types, sheep
  colors, and horse/donkey/mule equine variants and attributes are prepared
  up front. Cat only prefers a village or swamp-hut cat spawn context when the
  selected variant is `all_black`.
  A natural-sized group is ensured only for forms that naturally spawn in
  groups; solitary forms such as cat, ocelot, and mule do not get an extra
  nearby copy.
- Initial spawn relocation runs once per newly created world. Existing worlds
  created before this feature keep their current world spawn.
- During world creation, pressing Create opens a separate three-column form
  selection screen before the world is generated, avoiding repeated cycling
  through every form. Confirming the selection immediately proceeds with world
  generation. An optional SNBT field remains below the selector; empty input
  uses entity defaults, while invalid input is shown in red and prevents world
  creation.
- Available forms are normal player, cow, sheep, chicken, cat, ocelot, wolf,
  pig, horse, donkey, mule, and rabbit.
- The form can be changed later with:

```text
/moblife morph minecraft:player
/moblife morph minecraft:cow
/moblife morph minecraft:sheep
/moblife morph minecraft:chicken
/moblife morph minecraft:cat
/moblife morph minecraft:ocelot
/moblife morph minecraft:wolf
/moblife morph minecraft:pig
/moblife morph minecraft:horse
/moblife morph minecraft:donkey
/moblife morph minecraft:mule
/moblife morph minecraft:rabbit
```
- An optional entity NBT compound can follow the entity ID:

```text
/moblife morph minecraft:cow {Age:-24000}
/moblife morph minecraft:sheep {Color:14b}
/moblife morph minecraft:chicken {Age:-24000,Health:6.0f}
/moblife morph minecraft:cow {variant:"minecraft:warm"}
/moblife morph minecraft:chicken {variant:"minecraft:cold"}
```
- Morph NBT is persisted with the world and synchronized to clients.
- Per-morph gameplay configuration is loaded from
  `data/mob_life/mob_life/morphs/<mob>.json`. Data packs can override the same
  paths, and successful `/reload` reapplies server attributes and synchronizes
  movement and vision settings to clients.
