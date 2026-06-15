# World and Morph Selection Features

- A world-wide play form is selected during world creation and persisted in saved data.
- New worlds using a mob form move the initial overworld spawn to a biome
  whose natural spawn list contains that mob. A natural-sized group of the
  selected mob is ensured within 32 blocks of the spawn point.
- Initial spawn relocation runs once per newly created world. Existing worlds
  created before this feature keep their current world spawn.
- The world creation game-settings tab opens a separate three-column form
  selection screen, avoiding repeated cycling through every form. An optional
  SNBT field remains below the selector; empty input uses entity defaults,
  while invalid input is shown in red and prevents world creation.
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
