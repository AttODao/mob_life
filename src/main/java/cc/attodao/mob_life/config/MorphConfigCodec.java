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
  private MorphConfigCodec() {}

  static Map<MorphType, MorphConfig> loadBuiltinConfigs() {
    MorphConfig defaults = fallbackDefaults();
    EnumMap<MorphType, MorphConfig> configs = new EnumMap<>(MorphType.class);
    for (MorphType morph : MorphType.values()) {
      configs.put(morph, parseConfig(builtinMorphJson(morph), defaults));
    }
    return Map.copyOf(configs);
  }

  static MorphConfig fromJson(JsonObject root, MorphConfig defaults) {
    return parseConfig(root, defaults);
  }

  static JsonObject toJson(MorphConfig config) {
    JsonObject root = new JsonObject();
    root.add("movement", movementJson(config.movement()));
    root.add("diet", dietJson(config.diet()));
    root.add("vision", visionJson(config.vision()));
    root.add("combat", combatJson(config.combat()));
    root.add("attributes", attributesJson(config.attributes()));
    root.add("inventory", inventoryJson(config.inventory()));
    root.add("sleep", sleepJson(config.sleep()));
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
            0.1,
            0.1,
            0.13,
            0.13,
            0.25F,
            0.25F,
            1.0F,
            false,
            1.0F,
            false,
            new MorphConfig.RabbitHop(false, 10, 3, 0.2F, 0.35F, 0.2, 0.3));
    return new MorphConfig(
        movement,
        new MorphConfig.Diet(List.of(), List.of(), 4, 0.3F),
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
            "none", -1.0, new MorphConfig.LeapAttack(0.4, 0.0, 4.0), List.of(), List.of(), 1.0F),
        new MorphConfig.Attributes(1.0, 20, 1.0, 1.0),
        new MorphConfig.Inventory(9, 27, 0),
        new MorphConfig.Sleep("normal", false, 200, 8, 30.0F),
        new MorphConfig.Abilities(MorphConfig.Ability.NONE),
        new MorphConfig.Traits(Set.of()));
  }

  private static MorphConfig parseConfig(JsonObject root, MorphConfig defaults) {
    JsonObject movement = object(root, "movement");
    JsonObject rabbit = object(movement, "rabbit_hop");
    JsonObject diet = object(root, "diet");
    JsonObject vision = object(root, "vision");
    JsonObject combat = object(root, "combat");
    JsonObject leapAttack = object(combat, "leap_attack");
    JsonObject attributes = object(root, "attributes");
    JsonObject inventory = object(root, "inventory");
    JsonObject sleep = object(root, "sleep");
    MorphConfig.Movement defaultMovement = defaults.movement();
    MorphConfig.RabbitHop defaultHop = defaultMovement.rabbitHop();
    MorphConfig.Diet defaultDiet = defaults.diet();
    MorphConfig.Vision defaultVision = defaults.vision();
    MorphConfig.Combat defaultCombat = defaults.combat();
    MorphConfig.Attributes defaultAttributes = defaults.attributes();
    MorphConfig.Inventory defaultInventory = defaults.inventory();
    MorphConfig.Sleep defaultSleep = defaults.sleep();
    return new MorphConfig(
        new MorphConfig.Movement(
            number(movement, "reference_mob_speed", defaultMovement.referenceMobSpeed()),
            number(movement, "walk_speed", defaultMovement.walkSpeed()),
            number(movement, "sprint_speed", defaultMovement.sprintSpeed()),
            number(movement, "fast_sprint_speed", defaultMovement.fastSprintSpeed()),
            decimal(movement, "sideways_multiplier", defaultMovement.sidewaysMultiplier()),
            decimal(movement, "backward_multiplier", defaultMovement.backwardMultiplier()),
            decimal(movement, "water_input_multiplier", defaultMovement.waterInputMultiplier()),
            bool(movement, "charged_jump", defaultMovement.chargedJump()),
            decimal(movement, "slow_fall_multiplier", defaultMovement.slowFallMultiplier()),
            bool(movement, "wing_animation", defaultMovement.wingAnimation()),
            new MorphConfig.RabbitHop(
                bool(rabbit, "enabled", defaultHop.enabled()),
                integer(rabbit, "walk_cooldown", defaultHop.walkCooldown()),
                integer(rabbit, "sprint_cooldown", defaultHop.sprintCooldown()),
                decimal(rabbit, "walk_horizontal_speed", defaultHop.walkHorizontalSpeed()),
                decimal(rabbit, "sprint_horizontal_speed", defaultHop.sprintHorizontalSpeed()),
                number(rabbit, "walk_jump_velocity", defaultHop.walkJumpVelocity()),
                number(rabbit, "sprint_jump_velocity", defaultHop.sprintJumpVelocity()))),
        new MorphConfig.Diet(
            strings(diet, "foods", defaultDiet.foods()),
            strings(diet, "hunted_foods", defaultDiet.huntedFoods()),
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
            string(combat, "attack_mode", defaultCombat.attackMode()),
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
            string(sleep, "schedule", defaultSleep.schedule()),
            bool(sleep, "without_bed", defaultSleep.withoutBed()),
            integer(sleep, "required_ticks", defaultSleep.requiredTicks()),
            integer(sleep, "food_cost", defaultSleep.foodCost()),
            decimal(sleep, "maximum_awkwardness", defaultSleep.maximumAwkwardness())),
        new MorphConfig.Abilities(ability(root, "abilities")),
        new MorphConfig.Traits(traits(root, "traits", defaults.traits().values())));
  }

  private static JsonObject movementJson(MorphConfig.Movement movement) {
    JsonObject movementJson = new JsonObject();
    movementJson.addProperty("reference_mob_speed", movement.referenceMobSpeed());
    movementJson.addProperty("walk_speed", movement.walkSpeed());
    movementJson.addProperty("sprint_speed", movement.sprintSpeed());
    movementJson.addProperty("fast_sprint_speed", movement.fastSprintSpeed());
    movementJson.addProperty("sideways_multiplier", movement.sidewaysMultiplier());
    movementJson.addProperty("backward_multiplier", movement.backwardMultiplier());
    movementJson.addProperty("water_input_multiplier", movement.waterInputMultiplier());
    movementJson.addProperty("charged_jump", movement.chargedJump());
    movementJson.addProperty("slow_fall_multiplier", movement.slowFallMultiplier());
    movementJson.addProperty("wing_animation", movement.wingAnimation());
    movementJson.add("rabbit_hop", rabbitHopJson(movement.rabbitHop()));
    return movementJson;
  }

  private static JsonObject rabbitHopJson(MorphConfig.RabbitHop rabbitHop) {
    JsonObject rabbitJson = new JsonObject();
    rabbitJson.addProperty("enabled", rabbitHop.enabled());
    rabbitJson.addProperty("walk_cooldown", rabbitHop.walkCooldown());
    rabbitJson.addProperty("sprint_cooldown", rabbitHop.sprintCooldown());
    rabbitJson.addProperty("walk_horizontal_speed", rabbitHop.walkHorizontalSpeed());
    rabbitJson.addProperty("sprint_horizontal_speed", rabbitHop.sprintHorizontalSpeed());
    rabbitJson.addProperty("walk_jump_velocity", rabbitHop.walkJumpVelocity());
    rabbitJson.addProperty("sprint_jump_velocity", rabbitHop.sprintJumpVelocity());
    return rabbitJson;
  }

  private static JsonObject dietJson(MorphConfig.Diet diet) {
    JsonObject dietJson = new JsonObject();
    dietJson.add("foods", array(diet.foods()));
    dietJson.add("hunted_foods", array(diet.huntedFoods()));
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
    combatJson.addProperty("attack_mode", combat.attackMode());
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
    sleepJson.addProperty("schedule", sleep.schedule());
    sleepJson.addProperty("without_bed", sleep.withoutBed());
    sleepJson.addProperty("required_ticks", sleep.requiredTicks());
    sleepJson.addProperty("food_cost", sleep.foodCost());
    sleepJson.addProperty("maximum_awkwardness", sleep.maximumAwkwardness());
    return sleepJson;
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
    return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
  }

  private static boolean bool(JsonObject object, String name, boolean fallback) {
    JsonElement element = object.get(name);
    return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
  }

  private static int integer(JsonObject object, String name, int fallback) {
    JsonElement element = object.get(name);
    return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
  }

  private static float decimal(JsonObject object, String name, float fallback) {
    JsonElement element = object.get(name);
    return element != null && element.isJsonPrimitive() ? element.getAsFloat() : fallback;
  }

  private static double number(JsonObject object, String name, double fallback) {
    JsonElement element = object.get(name);
    return element != null && element.isJsonPrimitive() ? element.getAsDouble() : fallback;
  }

  private static List<String> strings(JsonObject object, String name, List<String> fallback) {
    JsonElement element = object.get(name);
    if (element == null || !element.isJsonArray()) {
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
    if (element == null || !element.isJsonArray()) {
      return fallback;
    }

    EnumSet<MorphConfig.Trait> values = EnumSet.noneOf(MorphConfig.Trait.class);
    for (JsonElement value : element.getAsJsonArray()) {
      values.add(MorphConfig.Trait.fromId(value.getAsString()));
    }
    return Set.copyOf(values);
  }

  private static MorphConfig.Ability ability(JsonObject object, String name) {
    JsonElement element = object.get(name);
    if (element == null || !element.isJsonArray()) {
      throw new IllegalStateException("Missing ability list: " + name);
    }

    JsonArray values = element.getAsJsonArray();
    if (values.size() != 1) {
      throw new IllegalStateException("Ability list must contain exactly one entry: " + name);
    }
    return MorphConfig.Ability.fromId(values.get(0).getAsString());
  }

  private static MorphConfig.ColorResponse color(
      JsonObject object, String name, MorphConfig.ColorResponse fallback) {
    JsonElement element = object.get(name);
    if (element == null || !element.isJsonArray() || element.getAsJsonArray().size() != 3) {
      return fallback;
    }

    JsonArray values = element.getAsJsonArray();
    return new MorphConfig.ColorResponse(
        values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
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
