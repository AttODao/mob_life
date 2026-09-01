# Inventory, Equipment, and Crafting Features

- The `player` form bypasses morph inventory searches, slot limits, equipment
  restrictions, and morph diet handling. Its animal body, saddle, and chest
  slots remain inactive, while its ordinary inventory behavior is delegated to
  vanilla.
- Inventory capacity scales with the form's height:
  - Player: 9 hotbar + 27 inventory slots
  - Cow: 7 hotbar + 21 inventory slots
  - Sheep: 6 hotbar + 19 inventory slots
  - Chicken: 3 hotbar + 9 inventory slots
- Items in slots disabled by a smaller form move into active slots; overflow is dropped.
- Disabled inventory, hotbar, armor, and form-specific equipment slots are
  fully hidden. The vanilla slot areas are cleared and only active slots are
  redrawn, preventing leftover borders from disabled slots.
- The player inventory screen height shrinks with the number of active
  main-inventory rows. Active slots use up to nine columns, and each partial
  row and the active hotbar are centered independently.

- Mob forms cannot wear humanoid head, chest, leg, or foot armor. Unsupported
  equipped items are returned to active inventory slots or dropped if full.
- The player inventory screen adds form-specific body and saddle slots:
  pigs and equines can equip saddles, horses can equip horse armor, wolves can
  equip wolf armor, and donkeys or mules can equip a chest.
- Horse, donkey, and mule inventory screens follow the mounted-horse layout:
  saddle and body equipment are arranged vertically on the left, and the
  offhand slot moves below them.
- All mob forms reduce player-inventory crafting from 2x2 to a single input
  slot. The other three input slots are hidden and reject interaction; items
  already in those slots are returned to active inventory when transforming.
- The morph inventory directly converts one item in `#minecraft:logs` into one
  crafting table in the one-slot player crafting grid. This conversion is not a
  normal datapack recipe, so it is not available in the 3x3 crafting table or
  shown in the crafting-table recipe book.
- The recipe-book button is hidden for mob-form player inventories because its
  normal 2x2 placement logic can target disabled crafting slots. Horse, donkey,
  and mule forms retain the same one-slot manual crafting area as other forms.
- A chest-equipped donkey or mule uses a mounted-inventory-style layout. Its
  size-based personal storage remains unchanged, and a separate persisted
  15-slot container is shown as a 5-by-3 chest grid in the upper right.
- The one-slot crafting area is placed above the chest grid so it does not overlap
  the chest or personal storage. Removing the chest returns its contents to
  active personal slots and drops only overflow; chest contents also follow
  normal player save, death-drop, and keep-inventory behavior.
- The entity preview in the player inventory is fixed facing forward. Morph
  rendering preserves the preview render state's body, head, and pitch values.
