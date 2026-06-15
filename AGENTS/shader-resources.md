## Shader Resources

- Shared shader: `assets/mob_life/shaders/post/mob_vision.fsh`
- The shared shader applies form-specific dichromatic color and mild
  desaturation across the full image. The main depth buffer progressively adds
  blur, lower contrast, darkening, and haze to distant scenery.
- The base pass progressively blurs and darkens the peripheral view, beginning
  outside the central 30% and reaching a 5-pixel blur and 40% brightness at
  the screen edges.
- The base pass preserves the source luminance, and distant darkening and haze
  use the current sky brightness so daytime and nighttime remain distinguishable.
- Vision rendering uses two passes: distant effects run immediately after
  world rendering while the world depth buffer still exists; base dichromatic
  vision runs after first-person hand rendering without consulting depth.
- Post chains:
  - `assets/mob_life/post_effect/cow_vision.json`
  - `assets/mob_life/post_effect/sheep_vision.json`
  - `assets/mob_life/post_effect/chicken_vision.json`
- `VisionConfig.Settings.x` controls retained saturation. Current values are
  intentionally muted but not near grayscale. `Settings.w` controls the
  peripheral blur radius in pixels.
- `DistanceBlur.Parameters` contains start distance, full-blur distance,
  full-darkening distance, and full-fog distance.
- `DistanceBlur.Effects` controls maximum distant blur radius, current sky
  brightness, peripheral edge brightness, and distant haze strength.
- `DistanceBlur.DepthRange` is updated every frame with Iris-compatible near
  and far planes (`0.05` and effective render distance in blocks), allowing the
  shader to linearize the depth buffer before applying distance thresholds.
- `DistanceBlur.DepthRange.w` stays at the base `1.0` through 70
  awkwardness, then increases to `2.0` at 100. The base therefore uses the
  former 100-awkwardness vision strength without additional awkwardness
  effects, while values above 70 further strengthen desaturation, darkening,
  fog, and blur.
- Every supported mob has an individual post-effect profile. Color response,
  retained saturation, contrast, low-light brightness, peripheral degradation,
  and distance-effect parameters are loaded from its morph data-pack JSON and
  synchronized to the client.
