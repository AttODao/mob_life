# Mob Life

ワールド作成時に牛・ヒツジ・ニワトリから姿を選び、そのMobとして生活するFabric Modです。

- 選択はワールドごとに保存され、作成後は変更できません。
- 見た目、モーション、当たり判定、視点の高さ、移動速度が選択したMobに変わります。
- ニワトリは空中でゆっくり落下します。

対象: Minecraft 26.1.2 / Fabric Loader 0.19.3 / Java 25

## License

Mob Life is released under the [MIT License](LICENSE).

Third-party projects used as dependencies or implementation references are
listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Data pack morph configuration

Each supported mob has a built-in configuration at:

```text
data/mob_life/mob_life/morphs/<mob>.json
```

A data pack can override the same path. `/reload` reapplies the active
configuration and synchronizes client-side movement and vision settings.
Omitted fields retain that mob's built-in defaults.

The JSON sections configure:

- `movement`: walking, sprinting, sideways/backward and water movement,
  charged jumps, slow falling, wing animation, and rabbit-style hopping.
- `diet`: edible item IDs or item tags (`#namespace:tag`), hunted meat,
  nutrition, and saturation.
- `vision`: post-effect profile, distance thresholds, blur, peripheral
  brightness, haze, RGB color-response vectors, field-of-view multiplier,
  peripheral vision, and low-light sensitivity.
- `combat`: attack mode and damage, leap attacks, predators, mobs that avoid
  the form, and hostile detection range.
- `attributes`: mining speed, maximum food, and block/entity reach.
- `inventory`: hotbar, inventory, and chest bonus slots.
- `sleep`: schedule (`normal`, `day`, or `never`), duration, food cost, and
  maximum awkwardness.
- `traits`: fall-damage immunity and form-specific equipment support.

`attack_damage: -1` copies the transformed entity's attack-damage attribute.
`attack_mode` accepts `none`, `always`, or `evil_rabbit`.
`combat.leap_attack` controls the horizontal speed, vertical speed, and
maximum target distance of the attack-time pounce used by forms such as cats
and wolves. It does not control normal jumping.
`combat.avoided_by` lists entity type IDs whose existing avoidance behavior
should treat the form as a threat, such as `minecraft:creeper`.
`sleep.food_cost` is the exact number of food points consumed by sleeping
without a bed. `traits` is an array containing only enabled trait names.
Animal armor uses separate `can_equip_horse_armor` and
`can_equip_wolf_armor` traits.
`vision.field_of_view_multiplier` scales the configured player FOV to
approximate the form's horizontal visual field. A value of `1` retains the
player FOV, and the rendered FOV is capped at 150 degrees.

Each built-in mob uses its own vision profile. Mammal profiles approximate
species-specific dichromatic sensitivity and visual acuity. Chicken vision
approximates tetrachromatic discrimination by increasing visible RGB color
separation; ultraviolet wavelengths cannot be reproduced by an RGB display.
