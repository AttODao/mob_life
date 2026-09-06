## Architecture

- `morph`: form IDs and entity-type mapping.
- `world`: pending world selection and persisted world-wide form.
- `gameplay/jump`: gait events, equine charge curve, and the player jump bridge.
- `gameplay/movement/MorphGaitControl`: deterministic multi-tick rabbit and
  equine gait progression used by the normal locomotion boundary.
- `gameplay/food`: diet rules and form-specific food capacity.
- `gameplay/inventory`: form-specific hotbar and inventory capacity.
- `gameplay/inventory/MorphEquipment`: armor restrictions and form-specific
  saddle, body-armor, and chest validation.
- `gameplay/view/MorphViewControl`: deterministic, immutable body, head, and
  camera transitions for normal-client, Instinct-client, and Instinct-server
  adapters. The Instinct AI adapter reads its per-tick source pose from the Mob proxy.
- `gameplay/instinct/InstinctController`: deep native-Mob proxy boundary that
  mirrors authoritative inputs, advances AI, applies the sanitized result to
  the player, and emits one transport-independent `InstinctSyncState`.
- `gameplay/instinct`: persisted Instinct Mode state, form-profile registry,
  input resistance, prey data, feeding, and entry/exit lifecycle. `MorphInstinct`
  adapts synchronization state to networking without unpacking proxy results.
- `gameplay/vision/MobVisionPolicy`: deterministic per-frame Mob vision values
  and the named std140 uniform layout shared with post-effect resources.
- `gameplay/targeting/MorphRelations`: per-player biological identity used by
  native targeting, avoidance, predation, leads, riding, and Instinct Mode.
- `config/MorphConfig`: morph configuration data model.
- `config/MorphConfigCodec`: built-in, data-pack, and synced morph config JSON
  parsing/serialization.
- `server/ServerMorphManager`: world morph lifecycle, gait side effects, and recurring form effects.
- `gameplay/targeting/MorphPredatorOutlineManager`: predator outline scans
  sourced from `combat.predators`, with differential synchronization.
- `server/ServerPlayerMorphApplier`: attributes, dimensions, capacity, and client synchronization.
- `client/state/ClientLocomotionController`: owns the selected morph, mode and
  lifecycle transitions, input policy, view state, gait progression, movement
  application, and body-yaw emission for normal locomotion. Mixins invoke this
  boundary directly; `ClientMorphState` does not forward its operations.
- `client/state`: active client form, render-entity cache, synchronized Instinct
  camera/input state, and remote body-yaw sync.
- `client/render/MobVisionRendering`: derives the current vision frame through
  `MobVisionPolicy`, owns dynamic GPU-buffer repair and std140 writes, and exposes
  post-effect identities. Render ordering and `PostPass` access remain mixin adapters.
- `client/render`: mob hand, sized hotbar, jump bar, Instinct Mode lock overlay,
  and viewer-specific predator/prey outlines.
- `client/screen`: standalone client screens such as world-form selection.
- `client/screen/MorphPreviewFactory`: preview entities and temporary preview
  level creation for form-selection screens.
- `client/screen/MorphSelectionDetails`: form-selection detail text assembly and
  registry display names.
- Common mixins are grouped by `food`, `gameplay`, `instinct`, `inventory`, and `player`.
- Client mixins are grouped by `gameplay`, `inventory`, `player`, `render`, and `world`.
