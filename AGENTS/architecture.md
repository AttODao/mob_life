## Architecture

- `morph`: form IDs and entity-type mapping.
- `world`: pending world selection and persisted world-wide form.
- `gameplay/jump`: shared charged-jump calculation and player bridge.
- `gameplay/food`: diet rules and form-specific food capacity.
- `gameplay/inventory`: form-specific hotbar and inventory capacity.
- `gameplay/inventory/MorphEquipment`: armor restrictions and form-specific
  saddle, body-armor, and chest validation.
- `config/MorphConfig`: morph configuration data model.
- `config/MorphConfigCodec`: built-in, data-pack, and synced morph config JSON
  parsing/serialization.
- `server/ServerMorphManager`: world morph lifecycle, charged jumps, and recurring form effects.
- `gameplay/targeting/MorphPredatorOutlineManager`: predator outline scans
  sourced from `combat.predators`, with differential synchronization.
- `server/ServerPlayerMorphApplier`: attributes, dimensions, capacity, and client synchronization.
- `client/state`: active client form, render-entity cache, and charged-jump input state.
- `client/render`: mob hand, sized hotbar, and jump-bar renderers.
- `client/screen`: standalone client screens such as world-form selection.
- `client/screen/MorphPreviewFactory`: preview entities and temporary preview
  level creation for form-selection screens.
- `client/screen/MorphSelectionDetails`: form-selection detail text assembly and
  registry display names.
- Common mixins are grouped by `food`, `gameplay`, `inventory`, and `player`.
- Client mixins are grouped by `gameplay`, `inventory`, `player`, `render`, and `world`.
