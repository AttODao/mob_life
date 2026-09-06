## Shader Resources

- Shared shader: `assets/mob_life/shaders/post/mob_vision.fsh`
- The shared shader applies form-specific dichromatic color and mild
  desaturation across the full image. The distant pass linearizes the world
  depth buffer, then uses the existing minimum screen-edge compensation to
  approximate player distance. Sky/depth-clear pixels remain full-distance scenery.
- The base pass progressively blurs the peripheral view. The client multiplies
  peripheral blur by 16 and leaves peripheral darkening disabled, so low-FOV
  forms get a much stronger edge treatment without changing the distance pass.
- The base pass also blends the Instinct `#3F2818` vignette through a continuous
  rounded-edge mask. `InstinctVignette.Color.a` is zero outside Instinct and
  rises to 0.55 at level 100. There is no non-shader fallback.
- The base pass generally preserves the source luminance, and distant darkening
  and haze use the current sky brightness so daytime and nighttime remain
  distinguishable. High `low_light_brightness` profiles also use local darkness
  to boost dark-adapted luminance.
- High `low_light_brightness` profiles add a sharp isolated-bright-pixel
  overlay in the distant pass, so far light sources remain visible through
  distance darkening, haze, and blur without reducing the distance blur amount.
  The overlay is gated by local darkness and point contrast so broad bright
  blocks such as sand do not become light sources.
- Vision rendering uses two shared post chains: `vision_distance` runs
  immediately after `LevelRenderer.render`, before HUD projection setup and
  depth clear, so the world projection matrix still matches the world depth
  buffer. `vision_base` runs after first-person hand rendering without
  consulting depth.
- `gameplay/vision/MobVisionPolicy` owns deterministic per-frame value
  derivation and the named std140 layout. `client/render/MobVisionRendering`
  owns dynamic uniform-buffer repair and writes. `GameRendererMixin` keeps only
  render ordering and post-chain lookup, while `PostPassDistanceUniformMixin`
  keeps only the `PostPass` adapter call.
- Post chains:
  - `assets/mob_life/post_effect/vision_distance.json`
  - `assets/mob_life/post_effect/vision_base.json`
- `DistanceBlur.ConfigOverrides.x` controls retained saturation. Current values
  are intentionally muted but not near grayscale. `DistanceBlur.ConfigOverrides.w`
  controls the peripheral blur radius in pixels after FOV and the global 16x
  peripheral blur multiplier are applied.
- `DistanceBlur.Parameters` contains start distance, full-blur distance,
  full-darkening distance, and full-fog distance. The client enforces an
  8-block minimum start distance and shifts the rest of the curve by the same
  offset so the distant effect does not cover the whole view at close range.
- `DistanceBlur.Effects` controls maximum distant blur radius, current sky
  brightness, peripheral edge brightness, and distant haze strength. Peripheral
  edge brightness is pinned to full brightness, so the edge treatment is blur
  only.
- `DistanceBlur.DepthRange` is updated every frame with Iris-compatible near
  and far planes (`0.05` and effective render distance in blocks), plus the
  interference scalar used by the shader. `VisionPass.Mode.x` selects the
  distance or base pass. `VisionBehavior.z` remains the reserved zero value;
  the shader clamps it to its minimum while reconstructing edge distance.
- `DistanceBlur.DepthRange.w` stays at the base `1.0` through 70
  awkwardness, then increases to `2.0` at 100. The base therefore uses the
  former 100-awkwardness vision strength without additional awkwardness
  effects, while values above 70 further strengthen desaturation, darkening,
  fog, and blur.
- All supported mobs share the same post-effect resources. Color response,
  retained saturation, contrast, low-light brightness, peripheral degradation,
  and distance-effect parameters are loaded from each morph's data-pack JSON
  and synchronized to the client.
- `DistanceBlur` is eight declaration-ordered `vec4` values (128 bytes), and
  `InstinctVignette` is one `vec4` (16 bytes). Tests compare the Java-owned names
  and order with both post-chain JSON files and `mob_vision.fsh` so layout drift
  fails the build.
