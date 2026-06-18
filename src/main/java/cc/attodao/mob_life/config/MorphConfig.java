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
import java.util.Objects;
import java.util.Set;

public record MorphConfig(
    Movement movement,
    Diet diet,
    Vision vision,
    Combat combat,
    Attributes attributes,
    Inventory inventory,
    Sleep sleep,
    Abilities abilities,
    Traits traits) {

  public record Movement(
      double referenceMobSpeed,
      double walkSpeed,
      double sprintSpeed,
      double fastSprintSpeed,
      float sidewaysMultiplier,
      float backwardMultiplier,
      float waterInputMultiplier,
      boolean chargedJump,
      float slowFallMultiplier,
      boolean wingAnimation,
      RabbitHop rabbitHop) {}

  public record RabbitHop(
      boolean enabled,
      int walkCooldown,
      int sprintCooldown,
      float walkHorizontalSpeed,
      float sprintHorizontalSpeed,
      double walkJumpVelocity,
      double sprintJumpVelocity) {}

  public record Diet(
      List<String> foods, List<String> huntedFoods, int nutrition, float saturationModifier) {}

  public record Vision(
      String profile,
      float fieldOfViewMultiplier,
      ColorResponse redResponse,
      ColorResponse greenResponse,
      ColorResponse blueResponse,
      float effectStartDistance,
      float fullBlurDistance,
      float fullDarkeningDistance,
      float fullFogDistance,
      float maximumBlurRadius,
      float peripheralEdgeBrightness,
      float hazeStrength,
      float retainedSaturation,
      float contrast,
      float brightness,
      float peripheralBlurRadius,
      float peripheralStart,
      float lowLightBrightness) {}

  public record ColorResponse(float red, float green, float blue) {}

  public record Combat(
      String attackMode,
      double attackDamage,
      LeapAttack leapAttack,
      List<String> predators,
      List<String> avoidedBy,
      float hostileDetectionMultiplier) {}

  public record LeapAttack(double horizontalSpeed, double verticalSpeed, double maximumDistance) {}

  public record Attributes(
      double miningSpeed, int maximumFood, double blockReachScale, double entityReachScale) {}

  public record Inventory(int hotbarSlots, int inventorySlots, int chestBonusSlots) {}

  public record Sleep(
      String schedule,
      boolean withoutBed,
      int requiredTicks,
      int foodCost,
      float maximumAwkwardness) {}

  public enum Ability {
    NONE("none"),
    FAST_SPRINT("fast_sprint"),
    EGG_LAYING("egg_laying");

    private final String id;

    Ability(String id) {
      this.id = id;
    }

    public String id() {
      return id;
    }

    static Ability fromId(String id) {
      for (Ability ability : values()) {
        if (ability.id.equals(id)) {
          return ability;
        }
      }
      throw new IllegalArgumentException("Unknown morph ability: " + id);
    }
  }

  public record Abilities(Ability value) {
    public Abilities {
      value = Objects.requireNonNull(value);
    }
  }

  public enum Trait {
    FALL_DAMAGE_IMMUNE("fall_damage_immune"),
    NIGHT_VISION("night_vision"),
    EATS_GRASS("eats_grass"),
    CAN_EQUIP_SADDLE("can_equip_saddle"),
    CAN_EQUIP_HORSE_ARMOR("can_equip_horse_armor"),
    CAN_EQUIP_WOLF_ARMOR("can_equip_wolf_armor"),
    CAN_EQUIP_CHEST("can_equip_chest");

    private final String id;

    Trait(String id) {
      this.id = id;
    }

    public String id() {
      return id;
    }

    static Trait fromId(String id) {
      for (Trait trait : values()) {
        if (trait.id.equals(id)) {
          return trait;
        }
      }
      throw new IllegalArgumentException("Unknown morph trait: " + id);
    }
  }

  public record Traits(Set<Trait> values) {
    public Traits {
      values = Set.copyOf(values);
    }

    public boolean fallDamageImmune() {
      return values.contains(Trait.FALL_DAMAGE_IMMUNE);
    }

    public boolean nightVision() {
      return values.contains(Trait.NIGHT_VISION);
    }

    public boolean eatsGrass() {
      return values.contains(Trait.EATS_GRASS);
    }

    public boolean canEquipSaddle() {
      return values.contains(Trait.CAN_EQUIP_SADDLE);
    }

    public boolean canEquipHorseArmor() {
      return values.contains(Trait.CAN_EQUIP_HORSE_ARMOR);
    }

    public boolean canEquipWolfArmor() {
      return values.contains(Trait.CAN_EQUIP_WOLF_ARMOR);
    }

    public boolean canEquipChest() {
      return values.contains(Trait.CAN_EQUIP_CHEST);
    }
  }

  private static final MorphConfig FALLBACK_DEFAULTS = fallbackDefaults();
  private static final Map<MorphType, MorphConfig> BUILTIN_CONFIGS = loadBuiltinConfigs();

  private static Map<MorphType, MorphConfig> loadBuiltinConfigs() {
    EnumMap<MorphType, MorphConfig> configs = new EnumMap<>(MorphType.class);
    for (MorphType morph : MorphType.values()) {
      configs.put(morph, loadBuiltinConfig(morph));
    }
    return Map.copyOf(configs);
  }

  private static MorphConfig loadBuiltinConfig(MorphType morph) {
    return parseConfig(builtinMorphJson(morph), FALLBACK_DEFAULTS);
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
    Movement movement =
        new Movement(
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
            new RabbitHop(false, 10, 3, 0.2F, 0.35F, 0.2, 0.3));
    return new MorphConfig(
        movement,
        new Diet(List.of(), List.of(), 4, 0.3F),
        new Vision(
            "cow",
            1.0F,
            new ColorResponse(0.57F, 0.43F, 0.0F),
            new ColorResponse(0.56F, 0.44F, 0.0F),
            new ColorResponse(0.0F, 0.24F, 0.76F),
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
        new Combat("none", -1.0, new LeapAttack(0.4, 0.0, 4.0), List.of(), List.of(), 1.0F),
        new Attributes(1.0, 20, 1.0, 1.0),
        new Inventory(9, 27, 0),
        new Sleep("normal", false, 200, 8, 30.0F),
        new Abilities(Ability.NONE),
        new Traits(Set.of()));
  }

  public static MorphConfig defaults(MorphType morph) {
    return Objects.requireNonNull(
        BUILTIN_CONFIGS.get(morph), "Missing builtin config for " + morph.id());
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
    RabbitHop defaultHop = defaults.movement.rabbitHop;
    return new MorphConfig(
        new Movement(
            number(movement, "reference_mob_speed", defaults.movement.referenceMobSpeed),
            number(movement, "walk_speed", defaults.movement.walkSpeed),
            number(movement, "sprint_speed", defaults.movement.sprintSpeed),
            number(movement, "fast_sprint_speed", defaults.movement.fastSprintSpeed),
            decimal(movement, "sideways_multiplier", defaults.movement.sidewaysMultiplier),
            decimal(movement, "backward_multiplier", defaults.movement.backwardMultiplier),
            decimal(movement, "water_input_multiplier", defaults.movement.waterInputMultiplier),
            bool(movement, "charged_jump", defaults.movement.chargedJump),
            decimal(movement, "slow_fall_multiplier", defaults.movement.slowFallMultiplier),
            bool(movement, "wing_animation", defaults.movement.wingAnimation),
            new RabbitHop(
                bool(rabbit, "enabled", defaultHop.enabled),
                integer(rabbit, "walk_cooldown", defaultHop.walkCooldown),
                integer(rabbit, "sprint_cooldown", defaultHop.sprintCooldown),
                decimal(rabbit, "walk_horizontal_speed", defaultHop.walkHorizontalSpeed),
                decimal(rabbit, "sprint_horizontal_speed", defaultHop.sprintHorizontalSpeed),
                number(rabbit, "walk_jump_velocity", defaultHop.walkJumpVelocity),
                number(rabbit, "sprint_jump_velocity", defaultHop.sprintJumpVelocity))),
        new Diet(
            strings(diet, "foods", defaults.diet.foods),
            strings(diet, "hunted_foods", defaults.diet.huntedFoods),
            integer(diet, "nutrition", defaults.diet.nutrition),
            decimal(diet, "saturation_modifier", defaults.diet.saturationModifier)),
        new Vision(
            string(vision, "profile", defaults.vision.profile),
            decimal(vision, "field_of_view_multiplier", defaults.vision.fieldOfViewMultiplier),
            color(vision, "red_response", defaults.vision.redResponse),
            color(vision, "green_response", defaults.vision.greenResponse),
            color(vision, "blue_response", defaults.vision.blueResponse),
            decimal(vision, "effect_start_distance", defaults.vision.effectStartDistance),
            decimal(vision, "full_blur_distance", defaults.vision.fullBlurDistance),
            decimal(vision, "full_darkening_distance", defaults.vision.fullDarkeningDistance),
            decimal(vision, "full_fog_distance", defaults.vision.fullFogDistance),
            decimal(vision, "maximum_blur_radius", defaults.vision.maximumBlurRadius),
            decimal(vision, "peripheral_edge_brightness", defaults.vision.peripheralEdgeBrightness),
            decimal(vision, "haze_strength", defaults.vision.hazeStrength),
            decimal(vision, "retained_saturation", defaults.vision.retainedSaturation),
            decimal(vision, "contrast", defaults.vision.contrast),
            decimal(vision, "brightness", defaults.vision.brightness),
            decimal(vision, "peripheral_blur_radius", defaults.vision.peripheralBlurRadius),
            decimal(vision, "peripheral_start", defaults.vision.peripheralStart),
            decimal(vision, "low_light_brightness", defaults.vision.lowLightBrightness)),
        new Combat(
            string(combat, "attack_mode", defaults.combat.attackMode),
            number(combat, "attack_damage", defaults.combat.attackDamage),
            new LeapAttack(
                number(leapAttack, "horizontal_speed", defaults.combat.leapAttack.horizontalSpeed),
                number(leapAttack, "vertical_speed", defaults.combat.leapAttack.verticalSpeed),
                number(leapAttack, "maximum_distance", defaults.combat.leapAttack.maximumDistance)),
            strings(combat, "predators", defaults.combat.predators),
            strings(combat, "avoided_by", defaults.combat.avoidedBy),
            decimal(
                combat,
                "hostile_detection_multiplier",
                defaults.combat.hostileDetectionMultiplier)),
        new Attributes(
            number(attributes, "mining_speed", defaults.attributes.miningSpeed),
            integer(attributes, "maximum_food", defaults.attributes.maximumFood),
            number(attributes, "block_reach_scale", defaults.attributes.blockReachScale),
            number(attributes, "entity_reach_scale", defaults.attributes.entityReachScale)),
        new Inventory(
            integer(inventory, "hotbar_slots", defaults.inventory.hotbarSlots),
            integer(inventory, "inventory_slots", defaults.inventory.inventorySlots),
            integer(inventory, "chest_bonus_slots", defaults.inventory.chestBonusSlots)),
        new Sleep(
            string(sleep, "schedule", defaults.sleep.schedule),
            bool(sleep, "without_bed", defaults.sleep.withoutBed),
            integer(sleep, "required_ticks", defaults.sleep.requiredTicks),
            integer(sleep, "food_cost", defaults.sleep.foodCost),
            decimal(sleep, "maximum_awkwardness", defaults.sleep.maximumAwkwardness)),
        new Abilities(ability(root, "abilities")),
        new Traits(traits(root, "traits", defaults.traits.values)));
  }

  public static MorphConfig fromJson(MorphType morph, JsonObject root) {
    return parseConfig(root, defaults(morph));
  }

  public JsonObject toJson() {
    JsonObject root = new JsonObject();
    JsonObject movementJson = new JsonObject();
    movementJson.addProperty("reference_mob_speed", movement.referenceMobSpeed);
    movementJson.addProperty("walk_speed", movement.walkSpeed);
    movementJson.addProperty("sprint_speed", movement.sprintSpeed);
    movementJson.addProperty("fast_sprint_speed", movement.fastSprintSpeed);
    movementJson.addProperty("sideways_multiplier", movement.sidewaysMultiplier);
    movementJson.addProperty("backward_multiplier", movement.backwardMultiplier);
    movementJson.addProperty("water_input_multiplier", movement.waterInputMultiplier);
    movementJson.addProperty("charged_jump", movement.chargedJump);
    movementJson.addProperty("slow_fall_multiplier", movement.slowFallMultiplier);
    movementJson.addProperty("wing_animation", movement.wingAnimation);
    JsonObject rabbitJson = new JsonObject();
    rabbitJson.addProperty("enabled", movement.rabbitHop.enabled);
    rabbitJson.addProperty("walk_cooldown", movement.rabbitHop.walkCooldown);
    rabbitJson.addProperty("sprint_cooldown", movement.rabbitHop.sprintCooldown);
    rabbitJson.addProperty("walk_horizontal_speed", movement.rabbitHop.walkHorizontalSpeed);
    rabbitJson.addProperty("sprint_horizontal_speed", movement.rabbitHop.sprintHorizontalSpeed);
    rabbitJson.addProperty("walk_jump_velocity", movement.rabbitHop.walkJumpVelocity);
    rabbitJson.addProperty("sprint_jump_velocity", movement.rabbitHop.sprintJumpVelocity);
    movementJson.add("rabbit_hop", rabbitJson);
    root.add("movement", movementJson);

    JsonObject dietJson = new JsonObject();
    dietJson.add("foods", array(diet.foods));
    dietJson.add("hunted_foods", array(diet.huntedFoods));
    dietJson.addProperty("nutrition", diet.nutrition);
    dietJson.addProperty("saturation_modifier", diet.saturationModifier);
    root.add("diet", dietJson);

    JsonObject visionJson = new JsonObject();
    visionJson.addProperty("profile", vision.profile);
    visionJson.addProperty("field_of_view_multiplier", vision.fieldOfViewMultiplier);
    visionJson.add("red_response", color(vision.redResponse));
    visionJson.add("green_response", color(vision.greenResponse));
    visionJson.add("blue_response", color(vision.blueResponse));
    visionJson.addProperty("effect_start_distance", vision.effectStartDistance);
    visionJson.addProperty("full_blur_distance", vision.fullBlurDistance);
    visionJson.addProperty("full_darkening_distance", vision.fullDarkeningDistance);
    visionJson.addProperty("full_fog_distance", vision.fullFogDistance);
    visionJson.addProperty("maximum_blur_radius", vision.maximumBlurRadius);
    visionJson.addProperty("peripheral_edge_brightness", vision.peripheralEdgeBrightness);
    visionJson.addProperty("haze_strength", vision.hazeStrength);
    visionJson.addProperty("retained_saturation", vision.retainedSaturation);
    visionJson.addProperty("contrast", vision.contrast);
    visionJson.addProperty("brightness", vision.brightness);
    visionJson.addProperty("peripheral_blur_radius", vision.peripheralBlurRadius);
    visionJson.addProperty("peripheral_start", vision.peripheralStart);
    visionJson.addProperty("low_light_brightness", vision.lowLightBrightness);
    root.add("vision", visionJson);

    JsonObject combatJson = new JsonObject();
    combatJson.addProperty("attack_mode", combat.attackMode);
    combatJson.addProperty("attack_damage", combat.attackDamage);
    JsonObject leapAttackJson = new JsonObject();
    leapAttackJson.addProperty("horizontal_speed", combat.leapAttack.horizontalSpeed);
    leapAttackJson.addProperty("vertical_speed", combat.leapAttack.verticalSpeed);
    leapAttackJson.addProperty("maximum_distance", combat.leapAttack.maximumDistance);
    combatJson.add("leap_attack", leapAttackJson);
    combatJson.add("predators", array(combat.predators));
    combatJson.add("avoided_by", array(combat.avoidedBy));
    combatJson.addProperty("hostile_detection_multiplier", combat.hostileDetectionMultiplier);
    root.add("combat", combatJson);

    JsonObject attributesJson = new JsonObject();
    attributesJson.addProperty("mining_speed", attributes.miningSpeed);
    attributesJson.addProperty("maximum_food", attributes.maximumFood);
    attributesJson.addProperty("block_reach_scale", attributes.blockReachScale);
    attributesJson.addProperty("entity_reach_scale", attributes.entityReachScale);
    root.add("attributes", attributesJson);

    JsonObject inventoryJson = new JsonObject();
    inventoryJson.addProperty("hotbar_slots", inventory.hotbarSlots);
    inventoryJson.addProperty("inventory_slots", inventory.inventorySlots);
    inventoryJson.addProperty("chest_bonus_slots", inventory.chestBonusSlots);
    root.add("inventory", inventoryJson);

    JsonObject sleepJson = new JsonObject();
    sleepJson.addProperty("schedule", sleep.schedule);
    sleepJson.addProperty("without_bed", sleep.withoutBed);
    sleepJson.addProperty("required_ticks", sleep.requiredTicks);
    sleepJson.addProperty("food_cost", sleep.foodCost);
    sleepJson.addProperty("maximum_awkwardness", sleep.maximumAwkwardness);
    root.add("sleep", sleepJson);

    JsonArray abilitiesJson = new JsonArray();
    abilitiesJson.add(abilities.value.id());
    root.add("abilities", abilitiesJson);

    JsonArray traitsJson = new JsonArray();
    traits.values.stream().map(Trait::id).sorted().forEach(traitsJson::add);
    root.add("traits", traitsJson);
    return root;
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

  private static Set<Trait> traits(JsonObject object, String name, Set<Trait> fallback) {
    JsonElement element = object.get(name);
    if (element == null || !element.isJsonArray()) {
      return fallback;
    }
    EnumSet<Trait> values = EnumSet.noneOf(Trait.class);
    for (JsonElement value : element.getAsJsonArray()) {
      values.add(Trait.fromId(value.getAsString()));
    }
    return Set.copyOf(values);
  }

  private static Ability ability(JsonObject object, String name) {
    JsonElement element = object.get(name);
    if (element == null || !element.isJsonArray()) {
      throw new IllegalStateException("Missing ability list: " + name);
    }
    JsonArray values = element.getAsJsonArray();
    if (values.size() != 1) {
      throw new IllegalStateException("Ability list must contain exactly one entry: " + name);
    }
    return Ability.fromId(values.get(0).getAsString());
  }

  private static Set<Trait> traits(
      boolean fallDamageImmune,
      boolean nightVision,
      boolean eatsGrass,
      boolean canEquipSaddle,
      boolean canEquipHorseArmor,
      boolean canEquipWolfArmor,
      boolean canEquipChest) {
    EnumSet<Trait> values = EnumSet.noneOf(Trait.class);
    if (fallDamageImmune) {
      values.add(Trait.FALL_DAMAGE_IMMUNE);
    }
    if (nightVision) {
      values.add(Trait.NIGHT_VISION);
    }
    if (eatsGrass) {
      values.add(Trait.EATS_GRASS);
    }
    if (canEquipSaddle) {
      values.add(Trait.CAN_EQUIP_SADDLE);
    }
    if (canEquipHorseArmor) {
      values.add(Trait.CAN_EQUIP_HORSE_ARMOR);
    }
    if (canEquipWolfArmor) {
      values.add(Trait.CAN_EQUIP_WOLF_ARMOR);
    }
    if (canEquipChest) {
      values.add(Trait.CAN_EQUIP_CHEST);
    }
    return Set.copyOf(values);
  }

  private static ColorResponse color(JsonObject object, String name, ColorResponse fallback) {
    JsonElement element = object.get(name);
    if (element == null || !element.isJsonArray() || element.getAsJsonArray().size() != 3) {
      return fallback;
    }
    JsonArray values = element.getAsJsonArray();
    return new ColorResponse(
        values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
  }

  private static JsonArray color(ColorResponse response) {
    JsonArray result = new JsonArray();
    result.add(response.red);
    result.add(response.green);
    result.add(response.blue);
    return result;
  }

  private static JsonArray array(List<String> values) {
    JsonArray result = new JsonArray();
    values.forEach(result::add);
    return result;
  }
}
