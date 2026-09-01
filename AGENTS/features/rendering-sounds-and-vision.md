# Rendering, Sounds, and Vision Features

- First-person empty hands use the mob front leg or wing.
- When holding an item or map, the mob hand is not rendered so it does not overlap the item.
- Mob forms emit their NBT-adjusted entity's normal ambient sounds using
  vanilla mob timing and probability. Hurt sounds also use the transformed
  entity's sound event, volume, and voice pitch on both server and client.
- In the inventory screen, transformed player previews keep the vanilla
  mouse-following rotation instead of facing fixed forward.
- Normal player form removes all Mob Life attribute modifiers and restores vanilla rendering, dimensions, health, movement, mining speed, and vision.
- Mob forms apply a form-specific low-saturation dichromatic post-processing effect.
- Cat and ocelot forms, plus any morph tagged with `night_vision`, receive a
  persistent night-vision light boost while transformed. Use that trait for
  forms that spawn in dark places.
- Mob vision widens the camera using each form's
  `vision.field_of_view_multiplier`, capped at 150 degrees. Movement-speed
  changes contribute only 10% of vanilla's dynamic FOV effect while
  transformed, so walking and sprinting do not substantially alter the field
  of view.
- Configured predators receive red outlines. The server scans up to the
  per-form `outline.range` every second without loading chunks. Predator types
  come from `combat.predators`, so targeting and outlines share one data source.
