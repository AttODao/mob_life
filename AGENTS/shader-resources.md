## Shader Resources

- Shared shader: `assets/mob_life/shaders/post/mob_vision.fsh`
- The shared shader applies form-specific dichromatic color and mild
  desaturation across the full image. The distant pass linearizes the world
  depth buffer, then uses the current camera FOV to approximate player distance
  across the screen. Sky/depth-clear pixels remain full-distance scenery.
- The base pass progressively blurs the peripheral view. The client multiplies
  peripheral blur by 16 and leaves peripheral darkening disabled, so low-FOV
  forms get a much stronger edge treatment without changing the distance pass.
- The base pass preserves the source luminance, and distant darkening and haze
  use the current sky brightness so daytime and nighttime remain distinguishable.
- Vision rendering uses two passes: distant effects run immediately after
  `LevelRenderer.renderLevel`, before HUD projection setup and depth clear, so
  the world projection matrix still matches the world depth buffer. Base
  dichromatic vision runs after first-person hand rendering without consulting
  depth.
- Post chains:
  - `assets/mob_life/post_effect/cow_vision.json`
  - `assets/mob_life/post_effect/sheep_vision.json`
  - `assets/mob_life/post_effect/chicken_vision.json`
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
  pass selector and interference scalar used by the shader. `VisionBehavior.z`
  carries `tan(cameraFov / 2)` for distance compensation toward the screen
  edges.
- `DistanceBlur.DepthRange.w` stays at the base `1.0` through 70
  awkwardness, then increases to `2.0` at 100. The base therefore uses the
  former 100-awkwardness vision strength without additional awkwardness
  effects, while values above 70 further strengthen desaturation, darkening,
  fog, and blur.
- Every supported mob has an individual post-effect profile. Color response,
  retained saturation, contrast, low-light brightness, peripheral degradation,
  and distance-effect parameters are loaded from its morph data-pack JSON and
  synchronized to the client.
