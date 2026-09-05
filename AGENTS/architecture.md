## Architecture

- `morph`: form IDs and entity-type mapping.
- `world`: pending world selection and persisted world-wide form.
- `gameplay/jump`: gait events, equine charge curve, and the player jump bridge.
- `gameplay/food`: diet rules and form-specific food capacity.
- `gameplay/inventory`: form-specific hotbar and inventory capacity.
- `gameplay/inventory/MorphEquipment`: armor restrictions and form-specific
  saddle, body-armor, and chest validation.
- `gameplay/instinct`: persisted Instinct Mode state, form-profile registry,
  native-Mob AI proxy, input resistance, prey data, feeding, and entry/exit lifecycle.
- `gameplay/targeting/MorphRelations`: per-player biological identity used by
  native targeting, avoidance, predation, leads, riding, and Instinct Mode.
- `config/MorphConfig`: morph configuration data model.
- `config/MorphConfigCodec`: built-in, data-pack, and synced morph config JSON
  parsing/serialization.
- `server/ServerMorphManager`: world morph lifecycle, gait side effects, and recurring form effects.
- `gameplay/targeting/MorphPredatorOutlineManager`: predator outline scans
  sourced from `combat.predators`, with differential synchronization.
- `server/ServerPlayerMorphApplier`: attributes, dimensions, capacity, and client synchronization.
- `client/state`: active client form, render-entity cache, normal locomotion,
  body-yaw sync, and synchronized Instinct Mode camera/input state.
- `client/render`: mob hand, sized hotbar, jump bar, Instinct Mode lock overlay,
  and viewer-specific predator/prey outlines.
- `client/screen`: standalone client screens such as world-form selection.
- `client/screen/MorphPreviewFactory`: preview entities and temporary preview
  level creation for form-selection screens.
- `client/screen/MorphSelectionDetails`: form-selection detail text assembly and
  registry display names.
- Common mixins are grouped by `food`, `gameplay`, `instinct`, `inventory`, and `player`.
- Client mixins are grouped by `gameplay`, `inventory`, `player`, `render`, and `world`.
