package cc.attodao.mob_life.config;

import cc.attodao.mob_life.morph.MorphType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MorphConfigCodec {
  private static final Set<String> ROOT_KEYS =
      Set.of(
          "schema_version",
          "movement",
          "diet",
          "vision",
          "combat",
          "attributes",
          "inventory",
          "sleep",
          "outline",
          "instinct",
          "abilities",
          "traits");

  private MorphConfigCodec() {}

  static Map<MorphType, MorphConfig> loadBuiltinConfigs() {
    MorphConfig defaults = fallbackDefaults();
    EnumMap<MorphType, MorphConfig> configs = new EnumMap<>(MorphType.class);
    for (MorphType morph : MorphType.values()) {
      if (!morph.isPlayer()) {
        JsonObject root = builtinMorphJson(morph);
        validateLayer(morph, root);
        configs.put(morph, parseConfig(morph, root, defaults));
      }
    }
    return Map.copyOf(configs);
  }

  static MorphConfig fromJson(MorphType morph, JsonObject root, MorphConfig defaults) {
    return parseConfig(morph, root, defaults);
  }

  static void validateLayer(MorphType morph, JsonObject root) {
    requireSchemaVersion(root);
    keys(root, "$", ROOT_KEYS);
    validateMovement(morph, root.get("movement"));
    validateDiet(root.get("diet"));
    validateVision(root.get("vision"));
    validateCombat(root.get("combat"));
    validateAttributes(root.get("attributes"));
    validateInventory(root.get("inventory"));
    validateSleep(root.get("sleep"));
    validateOutline(root.get("outline"));
    validateInstinct(root.get("instinct"));
    validateAbilities(root.get("abilities"));
    validateTraits(root.get("traits"));
  }

  static JsonObject toJson(MorphConfig config) {
    JsonObject root = new JsonObject();
    root.addProperty("schema_version", 2);
    root.add("movement", movementJson(config.movement()));
    root.add("diet", dietJson(config.diet()));
    root.add("vision", visionJson(config.vision()));
    root.add("combat", combatJson(config.combat()));
    root.add("attributes", attributesJson(config.attributes()));
    root.add("inventory", inventoryJson(config.inventory()));
    root.add("sleep", sleepJson(config.sleep()));
    root.add("outline", outlineJson(config.outline()));
    if (config.instinct().supported()) {
      root.add("instinct", instinctJson(config.instinct()));
    }
    root.add("abilities", abilitiesJson(config.abilities()));
    root.add("traits", traitsJson(config.traits()));
    return root;
  }

  private static JsonObject builtinMorphJson(MorphType morph) {
    String path = "/data/mob_life/mob_life/morphs/" + morph.id() + ".json";
    try (InputStream stream = MorphConfig.class.getResourceAsStream(path)) {
      if (stream == null) {
        throw new IllegalStateException("Missing builtin morph config: " + path);
      }
      try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        return JsonParser.parseReader(reader).getAsJsonObject();
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Could not load builtin morph config: " + path, exception);
    }
  }

  private static MorphConfig fallbackDefaults() {
    MorphConfig.Movement movement =
        new MorphConfig.Movement(
            Map.of(MorphConfig.MovementState.WALK, new MorphConfig.MovementValue(1.0, 1.0)));
    return new MorphConfig(
        movement,
        new MorphConfig.Diet(List.of(), 4, 0.3F),
        new MorphConfig.Vision(
            "cow",
            1.0F,
            new MorphConfig.ColorResponse(0.57F, 0.43F, 0.0F),
            new MorphConfig.ColorResponse(0.56F, 0.44F, 0.0F),
            new MorphConfig.ColorResponse(0.0F, 0.24F, 0.76F),
            4.0F,
            20.0F,
            36.0F,
            56.0F,
            8.0F,
            0.82F,
            0.8F,
            0.5F,
            0.98F,
            1.0F,
            1.8F,
            0.58F,
            1.0F),
        new MorphConfig.Combat(
            MorphConfig.AttackMode.NONE,
            -1.0,
            new MorphConfig.LeapAttack(0.4, 0.0, 4.0),
            List.of(),
            List.of(),
            1.0F),
        new MorphConfig.Attributes(1.0, 20, 1.0, 1.0),
        new MorphConfig.Inventory(9, 27, 0),
        new MorphConfig.Sleep(MorphConfig.SleepSchedule.NORMAL, false, 200),
        new MorphConfig.Outline(true, 96.0),
        MorphConfig.Instinct.unsupported(),
        new MorphConfig.Abilities(MorphConfig.Ability.NONE),
        new MorphConfig.Traits(Set.of()));
  }

  private static MorphConfig parseConfig(MorphType morph, JsonObject root, MorphConfig defaults) {
    requireSchemaVersion(root);
    JsonObject movement = object(root, "movement");
    JsonObject diet = object(root, "diet");
    JsonObject vision = object(root, "vision");
    JsonObject combat = object(root, "combat");
    JsonObject leapAttack = object(combat, "leap_attack");
    JsonObject attributes = object(root, "attributes");
    JsonObject inventory = object(root, "inventory");
    JsonObject sleep = object(root, "sleep");
    JsonObject outline = object(root, "outline");
    JsonElement instinct = root.get("instinct");
    MorphConfig.Movement defaultMovement = defaults.movement();
    MorphConfig.Diet defaultDiet = defaults.diet();
    MorphConfig.Vision defaultVision = defaults.vision();
    MorphConfig.Combat defaultCombat = defaults.combat();
    MorphConfig.Attributes defaultAttributes = defaults.attributes();
    MorphConfig.Inventory defaultInventory = defaults.inventory();
    MorphConfig.Sleep defaultSleep = defaults.sleep();
    MorphConfig.Outline defaultOutline = defaults.outline();
    MorphConfig.Instinct defaultInstinct = defaults.instinct();
    return new MorphConfig(
        movement(morph, movement, defaultMovement),
        new MorphConfig.Diet(
            strings(diet, "foods", defaultDiet.foods()),
            integer(diet, "nutrition", defaultDiet.nutrition()),
            decimal(diet, "saturation_modifier", defaultDiet.saturationModifier())),
        new MorphConfig.Vision(
            string(vision, "profile", defaultVision.profile()),
            decimal(vision, "field_of_view_multiplier", defaultVision.fieldOfViewMultiplier()),
            color(vision, "red_response", defaultVision.redResponse()),
            color(vision, "green_response", defaultVision.greenResponse()),
            color(vision, "blue_response", defaultVision.blueResponse()),
            decimal(vision, "effect_start_distance", defaultVision.effectStartDistance()),
            decimal(vision, "full_blur_distance", defaultVision.fullBlurDistance()),
            decimal(vision, "full_darkening_distance", defaultVision.fullDarkeningDistance()),
            decimal(vision, "full_fog_distance", defaultVision.fullFogDistance()),
            decimal(vision, "maximum_blur_radius", defaultVision.maximumBlurRadius()),
            decimal(vision, "peripheral_edge_brightness", defaultVision.peripheralEdgeBrightness()),
            decimal(vision, "haze_strength", defaultVision.hazeStrength()),
            decimal(vision, "retained_saturation", defaultVision.retainedSaturation()),
            decimal(vision, "contrast", defaultVision.contrast()),
            decimal(vision, "brightness", defaultVision.brightness()),
            decimal(vision, "peripheral_blur_radius", defaultVision.peripheralBlurRadius()),
            decimal(vision, "peripheral_start", defaultVision.peripheralStart()),
            decimal(vision, "low_light_brightness", defaultVision.lowLightBrightness())),
        new MorphConfig.Combat(
            MorphConfig.AttackMode.fromId(
                string(combat, "attack_mode", defaultCombat.attackMode().id()),
                defaultCombat.attackMode()),
            number(combat, "attack_damage", defaultCombat.attackDamage()),
            new MorphConfig.LeapAttack(
                number(
                    leapAttack, "horizontal_speed", defaultCombat.leapAttack().horizontalSpeed()),
                number(leapAttack, "vertical_speed", defaultCombat.leapAttack().verticalSpeed()),
                number(
                    leapAttack, "maximum_distance", defaultCombat.leapAttack().maximumDistance())),
            strings(combat, "predators", defaultCombat.predators()),
            strings(combat, "avoided_by", defaultCombat.avoidedBy()),
            decimal(
                combat,
                "hostile_detection_multiplier",
                defaultCombat.hostileDetectionMultiplier())),
        new MorphConfig.Attributes(
            number(attributes, "mining_speed", defaultAttributes.miningSpeed()),
            integer(attributes, "maximum_food", defaultAttributes.maximumFood()),
            number(attributes, "block_reach_scale", defaultAttributes.blockReachScale()),
            number(attributes, "entity_reach_scale", defaultAttributes.entityReachScale())),
        new MorphConfig.Inventory(
            integer(inventory, "hotbar_slots", defaultInventory.hotbarSlots()),
            integer(inventory, "inventory_slots", defaultInventory.inventorySlots()),
            integer(inventory, "chest_bonus_slots", defaultInventory.chestBonusSlots())),
        new MorphConfig.Sleep(
            MorphConfig.SleepSchedule.fromId(
                string(sleep, "schedule", defaultSleep.schedule().id()), defaultSleep.schedule()),
            bool(sleep, "without_bed", defaultSleep.withoutBed()),
            integer(sleep, "required_ticks", defaultSleep.requiredTicks())),
        new MorphConfig.Outline(
            bool(outline, "enabled", defaultOutline.enabled()),
            number(outline, "range", defaultOutline.range())),
        instinct(instinct, defaultInstinct),
        new MorphConfig.Abilities(ability(root, "abilities", defaults.abilities().value())),
        new MorphConfig.Traits(traits(root, "traits", defaults.traits().values())));
  }

  private static JsonObject movementJson(MorphConfig.Movement movement) {
    JsonObject movementJson = new JsonObject();
    for (MorphConfig.MovementState state : MorphConfig.MovementState.values()) {
      MorphConfig.MovementValue value = movement.states().get(state);
      if (value == null) {
        continue;
      }
      JsonObject stateJson = new JsonObject();
      stateJson.addProperty("goal_speed_modifier", value.goalSpeedModifier());
      stateJson.addProperty(
          "movement_speed_attribute_multiplier", value.movementSpeedAttributeMultiplier());
      movementJson.add(state.id(), stateJson);
    }
    return movementJson;
  }

  private static MorphConfig.Movement movement(
      MorphType morph, JsonObject json, MorphConfig.Movement fallback) {
    EnumMap<MorphConfig.MovementState, MorphConfig.MovementValue> states =
        new EnumMap<>(MorphConfig.MovementState.class);
    for (MorphConfig.MovementState state : supportedStates(morph)) {
      MorphConfig.MovementValue fallbackValue = fallback.states().get(state);
      JsonObject valueJson = object(json, state.id());
      if (valueJson.isEmpty()) {
        throw new IllegalArgumentException("movement." + state.id() + " is required");
      }
      if (!valueJson.has("goal_speed_modifier")
          || !valueJson.has("movement_speed_attribute_multiplier")) {
        throw new IllegalArgumentException(
            "movement." + state.id() + " must define both speed values");
      }
      states.put(
          state,
          new MorphConfig.MovementValue(
              number(
                  valueJson,
                  "goal_speed_modifier",
                  fallbackValue != null ? fallbackValue.goalSpeedModifier() : 1.0),
              number(
                  valueJson,
                  "movement_speed_attribute_multiplier",
                  fallbackValue != null ? fallbackValue.movementSpeedAttributeMultiplier() : 1.0)));
    }
    return new MorphConfig.Movement(states);
  }

  private static Set<MorphConfig.MovementState> supportedStates(MorphType morph) {
    return morph == MorphType.CAT || morph == MorphType.OCELOT
        ? EnumSet.allOf(MorphConfig.MovementState.class)
        : EnumSet.of(MorphConfig.MovementState.WALK, MorphConfig.MovementState.SPRINT);
  }

  private static void requireSchemaVersion(JsonObject root) {
    JsonElement element = root.get("schema_version");
    if (element == null || !isExactInteger(element, 2)) {
      throw new IllegalArgumentException("$.schema_version must be the integer 2");
    }
  }

  private static boolean isExactInteger(JsonElement element, int expected) {
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
      return false;
    }
    double value = element.getAsDouble();
    return Double.isFinite(value) && value == expected;
  }

  private static void validateMovement(MorphType morph, JsonElement element) {
    if (element == null) {
      return;
    }
    JsonObject movement = requiredObject(element, "$.movement");
    Set<String> supported = new java.util.HashSet<>();
    for (MorphConfig.MovementState state : supportedStates(morph)) {
      supported.add(state.id());
    }
    keys(movement, "$.movement", supported);
    for (Map.Entry<String, JsonElement> entry : movement.entrySet()) {
      String path = "$.movement." + entry.getKey();
      JsonObject state = requiredObject(entry.getValue(), path);
      keys(state, path, Set.of("goal_speed_modifier", "movement_speed_attribute_multiplier"));
      optionalNumber(state, "goal_speed_modifier", path, 0.0, 4.0);
      optionalNumber(state, "movement_speed_attribute_multiplier", path, 0.0, 4.0);
    }
  }

  private static void validateDiet(JsonElement element) {
    JsonObject value = optionalObject(element, "$.diet");
    if (value == null) return;
    keys(value, "$.diet", Set.of("foods", "nutrition", "saturation_modifier"));
    optionalStrings(value, "foods", "$.diet");
    optionalInteger(value, "nutrition", "$.diet", 0, 100);
    optionalNumber(value, "saturation_modifier", "$.diet", 0.0, 10.0);
  }

  private static void validateVision(JsonElement element) {
    JsonObject value = optionalObject(element, "$.vision");
    if (value == null) return;
    keys(
        value,
        "$.vision",
        Set.of(
            "profile",
            "field_of_view_multiplier",
            "red_response",
            "green_response",
            "blue_response",
            "effect_start_distance",
            "full_blur_distance",
            "full_darkening_distance",
            "full_fog_distance",
            "maximum_blur_radius",
            "peripheral_edge_brightness",
            "haze_strength",
            "retained_saturation",
            "contrast",
            "brightness",
            "peripheral_blur_radius",
            "peripheral_start",
            "low_light_brightness"));
    optionalString(value, "profile", "$.vision");
    optionalNumber(value, "field_of_view_multiplier", "$.vision", 0.1, 4.0);
    optionalColor(value, "red_response", "$.vision");
    optionalColor(value, "green_response", "$.vision");
    optionalColor(value, "blue_response", "$.vision");
    optionalNumber(value, "effect_start_distance", "$.vision", 0.0, 512.0);
    optionalNumber(value, "full_blur_distance", "$.vision", 0.0, 512.0);
    optionalNumber(value, "full_darkening_distance", "$.vision", 0.0, 512.0);
    optionalNumber(value, "full_fog_distance", "$.vision", 0.0, 512.0);
    optionalNumber(value, "maximum_blur_radius", "$.vision", 0.0, 64.0);
    optionalNumber(value, "peripheral_edge_brightness", "$.vision", 0.0, 4.0);
    optionalNumber(value, "haze_strength", "$.vision", 0.0, 4.0);
    optionalNumber(value, "retained_saturation", "$.vision", 0.0, 4.0);
    optionalNumber(value, "contrast", "$.vision", 0.0, 4.0);
    optionalNumber(value, "brightness", "$.vision", 0.0, 4.0);
    optionalNumber(value, "peripheral_blur_radius", "$.vision", 0.0, 64.0);
    optionalNumber(value, "peripheral_start", "$.vision", 0.0, 512.0);
    optionalNumber(value, "low_light_brightness", "$.vision", 0.0, 4.0);
  }

  private static void validateCombat(JsonElement element) {
    JsonObject value = optionalObject(element, "$.combat");
    if (value == null) return;
    keys(
        value,
        "$.combat",
        Set.of(
            "attack_mode",
            "attack_damage",
            "leap_attack",
            "predators",
            "avoided_by",
            "hostile_detection_multiplier"));
    optionalEnum(value, "attack_mode", "$.combat", Set.of("none", "always", "evil_rabbit"));
    optionalNumber(value, "attack_damage", "$.combat", -1.0, 2048.0);
    optionalStrings(value, "predators", "$.combat");
    optionalStrings(value, "avoided_by", "$.combat");
    optionalNumber(value, "hostile_detection_multiplier", "$.combat", 0.0, 8.0);
    JsonObject leap = optionalObject(value.get("leap_attack"), "$.combat.leap_attack");
    if (leap != null) {
      keys(
          leap,
          "$.combat.leap_attack",
          Set.of("horizontal_speed", "vertical_speed", "maximum_distance"));
      optionalNumber(leap, "horizontal_speed", "$.combat.leap_attack", 0.0, 8.0);
      optionalNumber(leap, "vertical_speed", "$.combat.leap_attack", 0.0, 8.0);
      optionalNumber(leap, "maximum_distance", "$.combat.leap_attack", 0.0, 128.0);
    }
  }

  private static void validateAttributes(JsonElement element) {
    JsonObject value = optionalObject(element, "$.attributes");
    if (value == null) return;
    keys(
        value,
        "$.attributes",
        Set.of("mining_speed", "maximum_food", "block_reach_scale", "entity_reach_scale"));
    optionalNumber(value, "mining_speed", "$.attributes", 0.0, 64.0);
    optionalInteger(value, "maximum_food", "$.attributes", 1, 100);
    optionalNumber(value, "block_reach_scale", "$.attributes", 0.0, 8.0);
    optionalNumber(value, "entity_reach_scale", "$.attributes", 0.0, 8.0);
  }

  private static void validateInventory(JsonElement element) {
    JsonObject value = optionalObject(element, "$.inventory");
    if (value == null) return;
    keys(value, "$.inventory", Set.of("hotbar_slots", "inventory_slots", "chest_bonus_slots"));
    optionalInteger(value, "hotbar_slots", "$.inventory", 0, 9);
    optionalInteger(value, "inventory_slots", "$.inventory", 0, 27);
    optionalInteger(value, "chest_bonus_slots", "$.inventory", 0, 27);
  }

  private static void validateSleep(JsonElement element) {
    JsonObject value = optionalObject(element, "$.sleep");
    if (value == null) return;
    keys(value, "$.sleep", Set.of("schedule", "without_bed", "required_ticks"));
    optionalEnum(value, "schedule", "$.sleep", Set.of("normal", "day", "never"));
    optionalBoolean(value, "without_bed", "$.sleep");
    optionalInteger(value, "required_ticks", "$.sleep", 0, 24_000);
  }

  private static void validateOutline(JsonElement element) {
    JsonObject value = optionalObject(element, "$.outline");
    if (value == null) return;
    keys(value, "$.outline", Set.of("enabled", "range"));
    optionalBoolean(value, "enabled", "$.outline");
    optionalNumber(value, "range", "$.outline", 0.0, 128.0);
  }

  private static void validateInstinct(JsonElement element) {
    JsonObject value = optionalObject(element, "$.instinct");
    if (value == null) return;
    keys(value, "$.instinct", Set.of("profile", "forage"));
    optionalString(value, "profile", "$.instinct");
    JsonObject forage = optionalObject(value.get("forage"), "$.instinct.forage");
    if (forage != null) {
      keys(forage, "$.instinct.forage", Set.of("nutrition", "saturation_modifier"));
      optionalInteger(forage, "nutrition", "$.instinct.forage", 0, Integer.MAX_VALUE);
      optionalNumber(forage, "saturation_modifier", "$.instinct.forage", 0.0, Float.MAX_VALUE);
    }
  }

  private static void validateAbilities(JsonElement element) {
    if (element == null) return;
    if (!element.isJsonArray() || element.getAsJsonArray().size() != 1) {
      throw new IllegalArgumentException("$.abilities must be an array with exactly one value");
    }
    JsonElement value = element.getAsJsonArray().get(0);
    if (!isString(value) || !Set.of("none", "egg_laying").contains(value.getAsString())) {
      throw new IllegalArgumentException("$.abilities[0] is unknown");
    }
  }

  private static void validateTraits(JsonElement element) {
    if (element == null) return;
    if (!element.isJsonArray()) {
      throw new IllegalArgumentException("$.traits must be an array");
    }
    Set<String> allowed =
        Set.of(
            "fall_damage_immune",
            "night_vision",
            "eats_grass",
            "can_equip_saddle",
            "can_equip_horse_armor",
            "can_equip_wolf_armor",
            "can_equip_chest");
    Set<String> seen = new java.util.HashSet<>();
    int index = 0;
    for (JsonElement value : element.getAsJsonArray()) {
      if (!isString(value) || !allowed.contains(value.getAsString())) {
        throw new IllegalArgumentException("$.traits[" + index + "] is unknown");
      }
      if (!seen.add(value.getAsString())) {
        throw new IllegalArgumentException("$.traits contains duplicate " + value.getAsString());
      }
      index++;
    }
  }

  private static void keys(JsonObject object, String path, Set<String> allowed) {
    for (String key : object.keySet()) {
      if (!allowed.contains(key)) {
        throw new IllegalArgumentException(path + "." + key + " is not supported");
      }
    }
  }

  private static JsonObject optionalObject(JsonElement element, String path) {
    return element == null ? null : requiredObject(element, path);
  }

  private static JsonObject requiredObject(JsonElement element, String path) {
    if (!element.isJsonObject()) {
      throw new IllegalArgumentException(path + " must be an object");
    }
    return element.getAsJsonObject();
  }

  private static void optionalString(JsonObject object, String name, String path) {
    JsonElement value = object.get(name);
    if (value != null && !isString(value)) {
      throw new IllegalArgumentException(path + "." + name + " must be a string");
    }
  }

  private static void optionalEnum(
      JsonObject object, String name, String path, Set<String> allowed) {
    optionalString(object, name, path);
    JsonElement value = object.get(name);
    if (value != null && !allowed.contains(value.getAsString())) {
      throw new IllegalArgumentException(path + "." + name + " is unknown");
    }
  }

  private static void optionalBoolean(JsonObject object, String name, String path) {
    JsonElement value = object.get(name);
    if (value != null && (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())) {
      throw new IllegalArgumentException(path + "." + name + " must be a boolean");
    }
  }

  private static void optionalInteger(
      JsonObject object, String name, String path, int minimum, int maximum) {
    JsonElement element = object.get(name);
    if (element == null) return;
    double value = requiredNumber(element, path + "." + name);
    if (value != Math.rint(value) || value < minimum || value > maximum) {
      throw new IllegalArgumentException(
          path + "." + name + " must be an integer between " + minimum + " and " + maximum);
    }
  }

  private static void optionalNumber(
      JsonObject object, String name, String path, double minimum, double maximum) {
    JsonElement element = object.get(name);
    if (element == null) return;
    double value = requiredNumber(element, path + "." + name);
    if (value < minimum || value > maximum) {
      throw new IllegalArgumentException(
          path + "." + name + " must be between " + minimum + " and " + maximum);
    }
  }

  private static double requiredNumber(JsonElement element, String path) {
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
      throw new IllegalArgumentException(path + " must be a number");
    }
    double value = element.getAsDouble();
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(path + " must be finite");
    }
    return value;
  }

  private static void optionalStrings(JsonObject object, String name, String path) {
    JsonElement element = object.get(name);
    if (element == null) return;
    if (!element.isJsonArray()) {
      throw new IllegalArgumentException(path + "." + name + " must be an array");
    }
    int index = 0;
    for (JsonElement value : element.getAsJsonArray()) {
      if (!isString(value)) {
        throw new IllegalArgumentException(path + "." + name + "[" + index + "] must be a string");
      }
      index++;
    }
  }

  private static void optionalColor(JsonObject object, String name, String path) {
    JsonElement element = object.get(name);
    if (element == null) return;
    if (!element.isJsonArray() || element.getAsJsonArray().size() != 3) {
      throw new IllegalArgumentException(path + "." + name + " must contain three numbers");
    }
    for (int index = 0; index < 3; index++) {
      double value =
          requiredNumber(
              element.getAsJsonArray().get(index), path + "." + name + "[" + index + "]");
      if (value < -4.0 || value > 4.0) {
        throw new IllegalArgumentException(path + "." + name + " values must be between -4 and 4");
      }
    }
  }

  private static boolean isString(JsonElement element) {
    return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
  }

  private static JsonObject dietJson(MorphConfig.Diet diet) {
    JsonObject dietJson = new JsonObject();
    dietJson.add("foods", array(diet.foods()));
    dietJson.addProperty("nutrition", diet.nutrition());
    dietJson.addProperty("saturation_modifier", diet.saturationModifier());
    return dietJson;
  }

  private static JsonObject visionJson(MorphConfig.Vision vision) {
    JsonObject visionJson = new JsonObject();
    visionJson.addProperty("profile", vision.profile());
    visionJson.addProperty("field_of_view_multiplier", vision.fieldOfViewMultiplier());
    visionJson.add("red_response", color(vision.redResponse()));
    visionJson.add("green_response", color(vision.greenResponse()));
    visionJson.add("blue_response", color(vision.blueResponse()));
    visionJson.addProperty("effect_start_distance", vision.effectStartDistance());
    visionJson.addProperty("full_blur_distance", vision.fullBlurDistance());
    visionJson.addProperty("full_darkening_distance", vision.fullDarkeningDistance());
    visionJson.addProperty("full_fog_distance", vision.fullFogDistance());
    visionJson.addProperty("maximum_blur_radius", vision.maximumBlurRadius());
    visionJson.addProperty("peripheral_edge_brightness", vision.peripheralEdgeBrightness());
    visionJson.addProperty("haze_strength", vision.hazeStrength());
    visionJson.addProperty("retained_saturation", vision.retainedSaturation());
    visionJson.addProperty("contrast", vision.contrast());
    visionJson.addProperty("brightness", vision.brightness());
    visionJson.addProperty("peripheral_blur_radius", vision.peripheralBlurRadius());
    visionJson.addProperty("peripheral_start", vision.peripheralStart());
    visionJson.addProperty("low_light_brightness", vision.lowLightBrightness());
    return visionJson;
  }

  private static JsonObject combatJson(MorphConfig.Combat combat) {
    JsonObject combatJson = new JsonObject();
    MorphConfig.LeapAttack leapAttack = combat.leapAttack();
    JsonObject leapAttackJson = new JsonObject();
    leapAttackJson.addProperty("horizontal_speed", leapAttack.horizontalSpeed());
    leapAttackJson.addProperty("vertical_speed", leapAttack.verticalSpeed());
    leapAttackJson.addProperty("maximum_distance", leapAttack.maximumDistance());
    combatJson.addProperty("attack_mode", combat.attackMode().id());
    combatJson.addProperty("attack_damage", combat.attackDamage());
    combatJson.add("leap_attack", leapAttackJson);
    combatJson.add("predators", array(combat.predators()));
    combatJson.add("avoided_by", array(combat.avoidedBy()));
    combatJson.addProperty("hostile_detection_multiplier", combat.hostileDetectionMultiplier());
    return combatJson;
  }

  private static JsonObject attributesJson(MorphConfig.Attributes attributes) {
    JsonObject attributesJson = new JsonObject();
    attributesJson.addProperty("mining_speed", attributes.miningSpeed());
    attributesJson.addProperty("maximum_food", attributes.maximumFood());
    attributesJson.addProperty("block_reach_scale", attributes.blockReachScale());
    attributesJson.addProperty("entity_reach_scale", attributes.entityReachScale());
    return attributesJson;
  }

  private static JsonObject inventoryJson(MorphConfig.Inventory inventory) {
    JsonObject inventoryJson = new JsonObject();
    inventoryJson.addProperty("hotbar_slots", inventory.hotbarSlots());
    inventoryJson.addProperty("inventory_slots", inventory.inventorySlots());
    inventoryJson.addProperty("chest_bonus_slots", inventory.chestBonusSlots());
    return inventoryJson;
  }

  private static JsonObject sleepJson(MorphConfig.Sleep sleep) {
    JsonObject sleepJson = new JsonObject();
    sleepJson.addProperty("schedule", sleep.schedule().id());
    sleepJson.addProperty("without_bed", sleep.withoutBed());
    sleepJson.addProperty("required_ticks", sleep.requiredTicks());
    return sleepJson;
  }

  private static JsonObject outlineJson(MorphConfig.Outline outline) {
    JsonObject result = new JsonObject();
    result.addProperty("enabled", outline.enabled());
    result.addProperty("range", outline.range());
    return result;
  }

  private static JsonObject instinctJson(MorphConfig.Instinct instinct) {
    JsonObject result = new JsonObject();
    result.addProperty("profile", instinct.profile());
    JsonObject forage = new JsonObject();
    forage.addProperty("nutrition", instinct.forage().nutrition());
    forage.addProperty("saturation_modifier", instinct.forage().saturationModifier());
    result.add("forage", forage);
    return result;
  }

  private static MorphConfig.Instinct instinct(
      JsonElement instinctElement, MorphConfig.Instinct fallback) {
    if (instinctElement == null) {
      return fallback;
    }
    if (!instinctElement.isJsonObject()) {
      throw new IllegalArgumentException("instinct must be an object");
    }
    JsonObject instinct = instinctElement.getAsJsonObject();
    JsonElement profileElement = instinct.get("profile");
    if (profileElement == null
        || !profileElement.isJsonPrimitive()
        || !profileElement.getAsJsonPrimitive().isString()
        || profileElement.getAsString().isBlank()) {
      throw new IllegalArgumentException("instinct.profile must be a non-empty string");
    }
    JsonElement forageElement = instinct.get("forage");
    if (forageElement == null || !forageElement.isJsonObject()) {
      throw new IllegalArgumentException("instinct.forage must be an object");
    }
    JsonObject forage = forageElement.getAsJsonObject();
    JsonElement nutritionElement = forage.get("nutrition");
    JsonElement saturationElement = forage.get("saturation_modifier");
    if (nutritionElement == null
        || !nutritionElement.isJsonPrimitive()
        || !nutritionElement.getAsJsonPrimitive().isNumber()) {
      throw new IllegalArgumentException("instinct.forage.nutrition must be an integer");
    }
    double rawNutrition = nutritionElement.getAsDouble();
    if (!Double.isFinite(rawNutrition)
        || rawNutrition < 0.0
        || rawNutrition > Integer.MAX_VALUE
        || rawNutrition != Math.rint(rawNutrition)) {
      throw new IllegalArgumentException(
          "instinct.forage.nutrition must be a non-negative integer");
    }
    if (saturationElement == null
        || !saturationElement.isJsonPrimitive()
        || !saturationElement.getAsJsonPrimitive().isNumber()) {
      throw new IllegalArgumentException("instinct.forage.saturation_modifier must be a number");
    }
    float saturation = saturationElement.getAsFloat();
    if (!Float.isFinite(saturation) || saturation < 0.0F) {
      throw new IllegalArgumentException(
          "instinct.forage.saturation_modifier must be finite and non-negative");
    }
    return new MorphConfig.Instinct(
        profileElement.getAsString(), new MorphConfig.Forage((int) rawNutrition, saturation));
  }

  private static JsonArray abilitiesJson(MorphConfig.Abilities abilities) {
    JsonArray abilitiesJson = new JsonArray();
    abilitiesJson.add(abilities.value().id());
    return abilitiesJson;
  }

  private static JsonArray traitsJson(MorphConfig.Traits traits) {
    JsonArray traitsJson = new JsonArray();
    traits.values().stream().map(MorphConfig.Trait::id).sorted().forEach(traitsJson::add);
    return traitsJson;
  }

  private static JsonObject object(JsonObject parent, String name) {
    JsonElement element = parent.get(name);
    return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
  }

  private static String string(JsonObject object, String name, String fallback) {
    JsonElement element = object.get(name);
    return element == null ? fallback : element.getAsString();
  }

  private static boolean bool(JsonObject object, String name, boolean fallback) {
    JsonElement element = object.get(name);
    return element == null ? fallback : element.getAsBoolean();
  }

  private static int integer(JsonObject object, String name, int fallback) {
    JsonElement element = object.get(name);
    return element == null ? fallback : element.getAsInt();
  }

  private static float decimal(JsonObject object, String name, float fallback) {
    JsonElement element = object.get(name);
    return element == null ? fallback : element.getAsFloat();
  }

  private static double number(JsonObject object, String name, double fallback) {
    JsonElement element = object.get(name);
    return element == null ? fallback : element.getAsDouble();
  }

  private static List<String> strings(JsonObject object, String name, List<String> fallback) {
    JsonElement element = object.get(name);
    if (element == null) {
      return fallback;
    }

    List<String> values = new ArrayList<>();
    for (JsonElement value : element.getAsJsonArray()) {
      values.add(value.getAsString());
    }
    return List.copyOf(values);
  }

  private static Set<MorphConfig.Trait> traits(
      JsonObject object, String name, Set<MorphConfig.Trait> fallback) {
    JsonElement element = object.get(name);
    if (element == null) {
      return fallback;
    }

    EnumSet<MorphConfig.Trait> values = EnumSet.noneOf(MorphConfig.Trait.class);
    for (JsonElement value : element.getAsJsonArray()) {
      values.add(MorphConfig.Trait.fromIdOrNull(value.getAsString()));
    }
    return Set.copyOf(values);
  }

  private static MorphConfig.Ability ability(
      JsonObject object, String name, MorphConfig.Ability fallback) {
    JsonElement element = object.get(name);
    if (element == null) {
      return fallback;
    }

    JsonArray values = element.getAsJsonArray();
    return MorphConfig.Ability.fromId(values.get(0).getAsString(), fallback);
  }

  private static MorphConfig.ColorResponse color(
      JsonObject object, String name, MorphConfig.ColorResponse fallback) {
    JsonElement element = object.get(name);
    if (element == null) {
      return fallback;
    }

    JsonArray values = element.getAsJsonArray();
    return new MorphConfig.ColorResponse(
        decimalAt(values, 0), decimalAt(values, 1), decimalAt(values, 2));
  }

  private static float decimalAt(JsonArray values, int index) {
    return values.get(index).getAsFloat();
  }

  private static JsonArray color(MorphConfig.ColorResponse response) {
    JsonArray result = new JsonArray();
    result.add(response.red());
    result.add(response.green());
    result.add(response.blue());
    return result;
  }

  private static JsonArray array(List<String> values) {
    JsonArray result = new JsonArray();
    values.forEach(result::add);
    return result;
  }
}
