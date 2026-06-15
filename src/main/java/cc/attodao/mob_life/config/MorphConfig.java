package cc.attodao.mob_life.config;

import cc.attodao.mob_life.morph.MorphType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
      double attributeScale,
      double walkMultiplier,
      double sprintMultiplier,
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

  public record Abilities(Set<Ability> values) {
    public Abilities {
      values = Set.copyOf(values);
    }

    public boolean fastSprint() {
      return values.contains(Ability.FAST_SPRINT);
    }

    public boolean eggLaying() {
      return values.contains(Ability.EGG_LAYING);
    }
  }

  public enum Trait {
    FALL_DAMAGE_IMMUNE("fall_damage_immune"),
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

  public static MorphConfig defaults(MorphType morph) {
    double movementScale = morph.isEquine() ? 1.0 : morph.isPlayer() ? 1.0 : 0.25;
    double walk = 1.0;
    double sprint = morph.isEquine() ? 1.0 : 1.3;
    float sideways = morph.isEquine() ? 0.5F : 0.25F;
    float backward = 0.25F;
    boolean chargedJump = !morph.isPlayer() && morph != MorphType.RABBIT;
    RabbitHop rabbitHop = new RabbitHop(false, 20, 3, 0.2F, 0.35F, 0.2, 0.3);
    String visionProfile = "cow";
    float fieldOfViewMultiplier = 1.65F;
    ColorResponse redResponse = new ColorResponse(0.57F, 0.43F, 0.0F);
    ColorResponse greenResponse = new ColorResponse(0.56F, 0.44F, 0.0F);
    ColorResponse blueResponse = new ColorResponse(0.0F, 0.24F, 0.76F);
    float retainedSaturation = 0.5F;
    float contrast = 0.98F;
    float brightness = 1.0F;
    float effectStartDistance = 4.0F;
    float fullBlurDistance = 20.0F;
    float fullDarkeningDistance = 36.0F;
    float fullFogDistance = 56.0F;
    float maximumBlurRadius = 8.0F;
    float peripheralEdgeBrightness = 0.82F;
    float hazeStrength = 0.8F;
    float peripheralBlurRadius = 1.8F;
    float peripheralStart = 0.58F;
    float lowLightBrightness = 1.0F;
    String food = "";
    List<String> huntedFoods = List.of();
    String attackMode = "none";
    double leapVertical = 0.0;
    List<String> predators = List.of();
    List<String> avoidedBy = List.of();
    double miningSpeed = 1.0;
    int maximumFood = 20;
    int hotbarSlots = 9;
    int inventorySlots = 27;
    String sleepSchedule = "normal";
    boolean canEquipSaddle = false;
    boolean canEquipHorseArmor = false;
    boolean canEquipWolfArmor = false;
    boolean canEquipChest = false;
    boolean fallDamageImmune = false;
    boolean eatsGrass = false;
    boolean fastSprint = false;
    boolean eggLaying = false;

    switch (morph) {
      case PLAYER -> {}
      case COW -> {
        fastSprint = true;
        sprint = 2.0;
        food = "#minecraft:cow_food";
        miningSpeed = 0.78;
        maximumFood = 15;
        hotbarSlots = 7;
        inventorySlots = 21;
      }
      case SHEEP -> {
        eatsGrass = true;
        fastSprint = true;
        sprint = 1.25;
        visionProfile = "sheep";
        fieldOfViewMultiplier = 1.60F;
        redResponse = new ColorResponse(0.63F, 0.37F, 0.0F);
        greenResponse = new ColorResponse(0.68F, 0.32F, 0.0F);
        blueResponse = new ColorResponse(0.0F, 0.30F, 0.70F);
        retainedSaturation = 0.46F;
        contrast = 0.97F;
        maximumBlurRadius = 9.0F;
        peripheralEdgeBrightness = 0.84F;
        peripheralBlurRadius = 1.6F;
        peripheralStart = 0.60F;
        food = "#minecraft:sheep_food";
        predators = List.of("minecraft:wolf");
        miningSpeed = 0.72;
        maximumFood = 14;
        hotbarSlots = 6;
        inventorySlots = 19;
      }
      case CHICKEN -> {
        eggLaying = true;
        fallDamageImmune = true;
        sprint = 1.4;
        visionProfile = "chicken";
        fieldOfViewMultiplier = 1.50F;
        redResponse = new ColorResponse(1.08F, -0.06F, -0.02F);
        greenResponse = new ColorResponse(-0.04F, 1.08F, -0.04F);
        blueResponse = new ColorResponse(-0.03F, 0.08F, 1.10F);
        retainedSaturation = 1.0F;
        contrast = 1.04F;
        brightness = 1.0F;
        fullBlurDistance = 32.0F;
        fullDarkeningDistance = 48.0F;
        fullFogDistance = 64.0F;
        maximumBlurRadius = 4.0F;
        peripheralEdgeBrightness = 0.90F;
        hazeStrength = 0.55F;
        peripheralBlurRadius = 0.7F;
        peripheralStart = 0.65F;
        lowLightBrightness = 0.78F;
        food = "#minecraft:chicken_food";
        predators = List.of("minecraft:fox", "minecraft:ocelot");
        miningSpeed = 0.5;
        maximumFood = 10;
        hotbarSlots = 3;
        inventorySlots = 9;
      }
      case CAT -> {
        fastSprint = true;
        fallDamageImmune = true;
        walk = 0.8;
        sprint = 1.33;
        visionProfile = "cat";
        fieldOfViewMultiplier = 1.0F;
        redResponse = new ColorResponse(0.91F, 0.09F, 0.0F);
        greenResponse = new ColorResponse(0.0F, 0.46F, 0.54F);
        blueResponse = new ColorResponse(0.0F, 0.40F, 0.60F);
        retainedSaturation = 0.50F;
        contrast = 1.05F;
        brightness = 1.02F;
        maximumBlurRadius = 8.5F;
        peripheralEdgeBrightness = 0.68F;
        hazeStrength = 0.65F;
        peripheralBlurRadius = 3.0F;
        peripheralStart = 0.42F;
        lowLightBrightness = 1.42F;
        food = "#minecraft:cat_food";
        huntedFoods = List.of("minecraft:rabbit", "minecraft:cooked_rabbit");
        attackMode = "always";
        leapVertical = 0.3;
        avoidedBy = List.of("minecraft:creeper");
        miningSpeed = 0.5;
        maximumFood = 10;
        hotbarSlots = 3;
        inventorySlots = 9;
        sleepSchedule = "day";
      }
      case OCELOT -> {
        fastSprint = true;
        fallDamageImmune = true;
        walk = 0.8;
        sprint = 1.33;
        visionProfile = "ocelot";
        fieldOfViewMultiplier = 1.0F;
        redResponse = new ColorResponse(0.91F, 0.09F, 0.0F);
        greenResponse = new ColorResponse(0.0F, 0.46F, 0.54F);
        blueResponse = new ColorResponse(0.0F, 0.40F, 0.60F);
        retainedSaturation = 0.52F;
        contrast = 1.07F;
        brightness = 1.02F;
        maximumBlurRadius = 8.0F;
        peripheralEdgeBrightness = 0.70F;
        hazeStrength = 0.62F;
        peripheralBlurRadius = 2.7F;
        peripheralStart = 0.44F;
        lowLightBrightness = 1.38F;
        food = "#minecraft:ocelot_food";
        huntedFoods = List.of("minecraft:chicken", "minecraft:cooked_chicken");
        attackMode = "always";
        leapVertical = 0.3;
        avoidedBy = List.of("minecraft:creeper");
        miningSpeed = 0.5;
        maximumFood = 10;
        hotbarSlots = 3;
        inventorySlots = 9;
        sleepSchedule = "day";
      }
      case WOLF -> {
        fastSprint = true;
        sprint = 1.5;
        visionProfile = "wolf";
        fieldOfViewMultiplier = 1.25F;
        redResponse = new ColorResponse(0.94F, 0.06F, 0.0F);
        greenResponse = new ColorResponse(0.0F, 0.43F, 0.57F);
        blueResponse = new ColorResponse(0.0F, 0.48F, 0.52F);
        retainedSaturation = 0.48F;
        contrast = 1.03F;
        brightness = 1.0F;
        maximumBlurRadius = 7.5F;
        peripheralEdgeBrightness = 0.74F;
        hazeStrength = 0.68F;
        peripheralBlurRadius = 2.3F;
        peripheralStart = 0.47F;
        lowLightBrightness = 1.25F;
        food = "#minecraft:wolf_food";
        huntedFoods =
            List.of(
                "minecraft:mutton",
                "minecraft:cooked_mutton",
                "minecraft:rabbit",
                "minecraft:cooked_rabbit");
        attackMode = "always";
        leapVertical = 0.4;
        miningSpeed = 0.47;
        maximumFood = 10;
        hotbarSlots = 4;
        inventorySlots = 12;
        sleepSchedule = "day";
        canEquipWolfArmor = true;
      }
      case PIG -> {
        fastSprint = true;
        sprint = 1.25;
        visionProfile = "pig";
        fieldOfViewMultiplier = 1.55F;
        redResponse = new ColorResponse(0.60F, 0.40F, 0.0F);
        greenResponse = new ColorResponse(0.53F, 0.47F, 0.0F);
        blueResponse = new ColorResponse(0.0F, 0.27F, 0.73F);
        retainedSaturation = 0.52F;
        maximumBlurRadius = 9.5F;
        peripheralEdgeBrightness = 0.80F;
        peripheralBlurRadius = 2.0F;
        peripheralStart = 0.56F;
        lowLightBrightness = 1.08F;
        food = "#minecraft:pig_food";
        miningSpeed = 0.5;
        maximumFood = 10;
        hotbarSlots = 4;
        inventorySlots = 13;
        canEquipSaddle = true;
      }
      case HORSE -> {
        visionProfile = "horse";
        fieldOfViewMultiplier = 1.65F;
        redResponse = new ColorResponse(0.61F, 0.39F, 0.0F);
        greenResponse = new ColorResponse(0.66F, 0.34F, 0.0F);
        blueResponse = new ColorResponse(0.0F, 0.22F, 0.78F);
        retainedSaturation = 0.50F;
        maximumBlurRadius = 6.5F;
        peripheralEdgeBrightness = 0.91F;
        hazeStrength = 0.70F;
        peripheralBlurRadius = 0.9F;
        peripheralStart = 0.68F;
        lowLightBrightness = 1.13F;
        food = "#minecraft:horse_food";
        miningSpeed = 0.89;
        maximumFood = 17;
        hotbarSlots = 8;
        inventorySlots = 24;
        canEquipSaddle = true;
        canEquipHorseArmor = true;
      }
      case DONKEY -> {
        visionProfile = "donkey";
        fieldOfViewMultiplier = 1.65F;
        redResponse = new ColorResponse(0.61F, 0.39F, 0.0F);
        greenResponse = new ColorResponse(0.66F, 0.34F, 0.0F);
        blueResponse = new ColorResponse(0.0F, 0.22F, 0.78F);
        retainedSaturation = 0.49F;
        maximumBlurRadius = 7.0F;
        peripheralEdgeBrightness = 0.91F;
        hazeStrength = 0.72F;
        peripheralBlurRadius = 0.9F;
        peripheralStart = 0.68F;
        lowLightBrightness = 1.12F;
        food = "#minecraft:horse_food";
        miningSpeed = 0.83;
        maximumFood = 16;
        hotbarSlots = 7;
        inventorySlots = 22;
        canEquipSaddle = true;
        canEquipChest = true;
      }
      case MULE -> {
        visionProfile = "mule";
        fieldOfViewMultiplier = 1.65F;
        redResponse = new ColorResponse(0.61F, 0.39F, 0.0F);
        greenResponse = new ColorResponse(0.66F, 0.34F, 0.0F);
        blueResponse = new ColorResponse(0.0F, 0.22F, 0.78F);
        retainedSaturation = 0.50F;
        maximumBlurRadius = 6.8F;
        peripheralEdgeBrightness = 0.91F;
        hazeStrength = 0.71F;
        peripheralBlurRadius = 0.9F;
        peripheralStart = 0.68F;
        lowLightBrightness = 1.13F;
        food = "#minecraft:horse_food";
        miningSpeed = 0.89;
        maximumFood = 17;
        hotbarSlots = 8;
        inventorySlots = 24;
        canEquipSaddle = true;
        canEquipChest = true;
      }
      case RABBIT -> {
        fastSprint = true;
        walk = 0.6;
        sprint = 2.2;
        visionProfile = "rabbit";
        fieldOfViewMultiplier = 1.80F;
        redResponse = new ColorResponse(0.67F, 0.33F, 0.0F);
        greenResponse = new ColorResponse(0.57F, 0.43F, 0.0F);
        blueResponse = new ColorResponse(0.0F, 0.24F, 0.76F);
        retainedSaturation = 0.48F;
        contrast = 0.96F;
        maximumBlurRadius = 11.0F;
        peripheralEdgeBrightness = 0.93F;
        hazeStrength = 0.82F;
        peripheralBlurRadius = 0.7F;
        peripheralStart = 0.70F;
        lowLightBrightness = 1.08F;
        food = "#minecraft:rabbit_food";
        attackMode = "evil_rabbit";
        predators = List.of("minecraft:fox", "minecraft:wolf");
        rabbitHop = new RabbitHop(true, 20, 3, 0.2F, 0.35F, 0.2, 0.3);
        miningSpeed = 0.36;
        maximumFood = 8;
        hotbarSlots = 2;
        inventorySlots = 6;
      }
    }

    List<String> foods = food.isEmpty() ? List.of() : List.of(food);
    return new MorphConfig(
        new Movement(
            movementScale,
            walk,
            sprint,
            sideways,
            backward,
            1.0F,
            chargedJump,
            morph == MorphType.CHICKEN ? 0.6F : 1.0F,
            morph == MorphType.CHICKEN,
            rabbitHop),
        new Diet(foods, huntedFoods, 4, 0.3F),
        new Vision(
            visionProfile,
            morph.isPlayer() ? 1.0F : fieldOfViewMultiplier,
            redResponse,
            greenResponse,
            blueResponse,
            effectStartDistance,
            fullBlurDistance,
            fullDarkeningDistance,
            fullFogDistance,
            maximumBlurRadius,
            peripheralEdgeBrightness,
            hazeStrength,
            retainedSaturation,
            contrast,
            brightness,
            peripheralBlurRadius,
            peripheralStart,
            lowLightBrightness),
        new Combat(
            attackMode, -1.0, new LeapAttack(0.4, leapVertical, 4.0), predators, avoidedBy, 1.0F),
        new Attributes(miningSpeed, maximumFood, 1.0, 1.0),
        new Inventory(hotbarSlots, inventorySlots, canEquipChest ? 15 : 0),
        new Sleep(sleepSchedule, !morph.isPlayer(), 200, (int) Math.ceil(maximumFood * 0.4), 30.0F),
        new Abilities(abilities(fastSprint, eggLaying)),
        new Traits(
            traits(
                fallDamageImmune,
                eatsGrass,
                canEquipSaddle,
                canEquipHorseArmor,
                canEquipWolfArmor,
                canEquipChest)));
  }

  public static MorphConfig fromJson(MorphType morph, JsonObject root) {
    MorphConfig defaults = defaults(morph);
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
            number(movement, "attribute_scale", defaults.movement.attributeScale),
            number(movement, "walk_multiplier", defaults.movement.walkMultiplier),
            number(movement, "sprint_multiplier", defaults.movement.sprintMultiplier),
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
        new Abilities(abilities(root, "abilities", defaults.abilities.values)),
        new Traits(traits(root, "traits", defaults.traits.values)));
  }

  public JsonObject toJson() {
    JsonObject root = new JsonObject();
    JsonObject movementJson = new JsonObject();
    movementJson.addProperty("attribute_scale", movement.attributeScale);
    movementJson.addProperty("walk_multiplier", movement.walkMultiplier);
    movementJson.addProperty("sprint_multiplier", movement.sprintMultiplier);
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
    abilities.values.stream().map(Ability::id).sorted().forEach(abilitiesJson::add);
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

  private static Set<Ability> abilities(JsonObject object, String name, Set<Ability> fallback) {
    JsonElement element = object.get(name);
    if (element == null || !element.isJsonArray()) {
      return fallback;
    }
    EnumSet<Ability> values = EnumSet.noneOf(Ability.class);
    for (JsonElement value : element.getAsJsonArray()) {
      values.add(Ability.fromId(value.getAsString()));
    }
    return Set.copyOf(values);
  }

  private static Set<Ability> abilities(boolean fastSprint, boolean eggLaying) {
    EnumSet<Ability> values = EnumSet.noneOf(Ability.class);
    if (fastSprint) {
      values.add(Ability.FAST_SPRINT);
    }
    if (eggLaying) {
      values.add(Ability.EGG_LAYING);
    }
    return Set.copyOf(values);
  }

  private static Set<Trait> traits(
      boolean fallDamageImmune,
      boolean eatsGrass,
      boolean canEquipSaddle,
      boolean canEquipHorseArmor,
      boolean canEquipWolfArmor,
      boolean canEquipChest) {
    EnumSet<Trait> values = EnumSet.noneOf(Trait.class);
    if (fallDamageImmune) {
      values.add(Trait.FALL_DAMAGE_IMMUNE);
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
