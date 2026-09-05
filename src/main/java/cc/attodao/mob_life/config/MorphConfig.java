package cc.attodao.mob_life.config;

import cc.attodao.mob_life.morph.MorphType;
import com.google.gson.JsonObject;
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
    Outline outline,
    Instinct instinct,
    Abilities abilities,
    Traits traits) {

  public record Movement(Map<MovementState, MovementValue> states) {
    public Movement {
      states = Map.copyOf(states);
      if (!states.containsKey(MovementState.WALK)) {
        throw new IllegalArgumentException("movement.walk is required");
      }
    }

    public MovementValue value(MovementState state) {
      MovementValue value = states.get(state);
      return value != null ? value : states.get(MovementState.WALK);
    }
  }

  public enum MovementState {
    SNEAK("sneak"),
    WALK("walk"),
    SPRINT("sprint");

    private final String id;

    MovementState(String id) {
      this.id = id;
    }

    public String id() {
      return id;
    }
  }

  public record MovementValue(double goalSpeedModifier, double movementSpeedAttributeMultiplier) {
    public MovementValue {
      if (!Double.isFinite(goalSpeedModifier)
          || goalSpeedModifier < 0.0
          || goalSpeedModifier > 4.0) {
        throw new IllegalArgumentException(
            "goal_speed_modifier must be finite and between 0 and 4");
      }
      if (!Double.isFinite(movementSpeedAttributeMultiplier)
          || movementSpeedAttributeMultiplier < 0.0
          || movementSpeedAttributeMultiplier > 4.0) {
        throw new IllegalArgumentException(
            "movement_speed_attribute_multiplier must be finite and between 0 and 4");
      }
    }

    public double controllerSpeed(double effectiveMovementSpeed) {
      return goalSpeedModifier * effectiveMovementSpeed * movementSpeedAttributeMultiplier;
    }
  }

  public record Diet(List<String> foods, int nutrition, float saturationModifier) {
    public Diet {
      foods = List.copyOf(foods);
      nutrition = Math.clamp(nutrition, 0, 100);
      saturationModifier = finite(saturationModifier, 0.0F, 10.0F);
    }
  }

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
      float lowLightBrightness) {
    public Vision {
      profile = Objects.requireNonNull(profile);
      fieldOfViewMultiplier = finite(fieldOfViewMultiplier, 0.1F, 4.0F);
      redResponse = Objects.requireNonNull(redResponse);
      greenResponse = Objects.requireNonNull(greenResponse);
      blueResponse = Objects.requireNonNull(blueResponse);
      effectStartDistance = finite(effectStartDistance, 0.0F, 512.0F);
      fullBlurDistance = finite(fullBlurDistance, 0.0F, 512.0F);
      fullDarkeningDistance = finite(fullDarkeningDistance, 0.0F, 512.0F);
      fullFogDistance = finite(fullFogDistance, 0.0F, 512.0F);
      maximumBlurRadius = finite(maximumBlurRadius, 0.0F, 64.0F);
      peripheralEdgeBrightness = finite(peripheralEdgeBrightness, 0.0F, 4.0F);
      hazeStrength = finite(hazeStrength, 0.0F, 4.0F);
      retainedSaturation = finite(retainedSaturation, 0.0F, 4.0F);
      contrast = finite(contrast, 0.0F, 4.0F);
      brightness = finite(brightness, 0.0F, 4.0F);
      peripheralBlurRadius = finite(peripheralBlurRadius, 0.0F, 64.0F);
      peripheralStart = finite(peripheralStart, 0.0F, 512.0F);
      lowLightBrightness = finite(lowLightBrightness, 0.0F, 4.0F);
    }
  }

  public record ColorResponse(float red, float green, float blue) {
    public ColorResponse {
      red = finite(red, -4.0F, 4.0F);
      green = finite(green, -4.0F, 4.0F);
      blue = finite(blue, -4.0F, 4.0F);
    }
  }

  public record Combat(
      AttackMode attackMode,
      double attackDamage,
      LeapAttack leapAttack,
      List<String> predators,
      List<String> avoidedBy,
      float hostileDetectionMultiplier) {
    public Combat {
      attackMode = Objects.requireNonNull(attackMode);
      attackDamage = finite(attackDamage, -1.0, 2048.0);
      leapAttack = Objects.requireNonNull(leapAttack);
      predators = List.copyOf(predators);
      avoidedBy = List.copyOf(avoidedBy);
      hostileDetectionMultiplier = finite(hostileDetectionMultiplier, 0.0F, 8.0F);
    }
  }

  public record LeapAttack(double horizontalSpeed, double verticalSpeed, double maximumDistance) {
    public LeapAttack {
      horizontalSpeed = finite(horizontalSpeed, 0.0, 8.0);
      verticalSpeed = finite(verticalSpeed, 0.0, 8.0);
      maximumDistance = finite(maximumDistance, 0.0, 128.0);
    }
  }

  public record Attributes(
      double miningSpeed, int maximumFood, double blockReachScale, double entityReachScale) {
    public Attributes {
      miningSpeed = finite(miningSpeed, 0.0, 64.0);
      maximumFood = Math.clamp(maximumFood, 1, 100);
      blockReachScale = finite(blockReachScale, 0.0, 8.0);
      entityReachScale = finite(entityReachScale, 0.0, 8.0);
    }
  }

  public record Inventory(int hotbarSlots, int inventorySlots, int chestBonusSlots) {
    public Inventory {
      hotbarSlots = Math.clamp(hotbarSlots, 0, 9);
      inventorySlots = Math.clamp(inventorySlots, 0, 27);
      chestBonusSlots = Math.clamp(chestBonusSlots, 0, 27);
    }
  }

  public record Sleep(SleepSchedule schedule, boolean withoutBed, int requiredTicks) {
    public Sleep {
      schedule = Objects.requireNonNull(schedule);
      requiredTicks = Math.clamp(requiredTicks, 0, 24_000);
    }
  }

  public record Outline(boolean enabled, double range) {
    public Outline {
      range = finite(range, 0.0, 128.0);
    }
  }

  public record Instinct(String profile, Forage forage) {
    public Instinct {
      profile = profile != null ? profile : "";
      forage = Objects.requireNonNull(forage);
    }

    public boolean supported() {
      return !profile.isBlank();
    }

    public static Instinct unsupported() {
      return new Instinct("", new Forage(0, 0.0F));
    }
  }

  public record Forage(int nutrition, float saturationModifier) {
    public Forage {
      if (nutrition < 0) {
        throw new IllegalArgumentException("Instinct forage nutrition must be non-negative");
      }
      if (!Float.isFinite(saturationModifier) || saturationModifier < 0.0F) {
        throw new IllegalArgumentException(
            "Instinct forage saturation_modifier must be finite and non-negative");
      }
    }
  }

  public enum AttackMode {
    NONE("none"),
    ALWAYS("always"),
    EVIL_RABBIT("evil_rabbit");

    private final String id;

    AttackMode(String id) {
      this.id = id;
    }

    public String id() {
      return id;
    }

    static AttackMode fromId(String id, AttackMode fallback) {
      for (AttackMode mode : values()) {
        if (mode.id.equals(id)) {
          return mode;
        }
      }
      return fallback;
    }
  }

  public enum SleepSchedule {
    NORMAL("normal"),
    DAY("day"),
    NEVER("never");

    private final String id;

    SleepSchedule(String id) {
      this.id = id;
    }

    public String id() {
      return id;
    }

    static SleepSchedule fromId(String id, SleepSchedule fallback) {
      for (SleepSchedule schedule : values()) {
        if (schedule.id.equals(id)) {
          return schedule;
        }
      }
      return fallback;
    }
  }

  public enum Ability {
    NONE("none"),
    EGG_LAYING("egg_laying");

    private final String id;

    Ability(String id) {
      this.id = id;
    }

    public String id() {
      return id;
    }

    static Ability fromId(String id, Ability fallback) {
      for (Ability ability : values()) {
        if (ability.id.equals(id)) {
          return ability;
        }
      }
      return fallback;
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

    static Trait fromIdOrNull(String id) {
      for (Trait trait : values()) {
        if (trait.id.equals(id)) {
          return trait;
        }
      }
      return null;
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

  private static final Map<MorphType, MorphConfig> BUILTIN_CONFIGS =
      MorphConfigCodec.loadBuiltinConfigs();

  public static MorphConfig defaults(MorphType morph) {
    if (morph.isPlayer()) {
      throw new IllegalArgumentException("The player form does not have a morph config");
    }
    return Objects.requireNonNull(
        BUILTIN_CONFIGS.get(morph), "Missing builtin config for " + morph.id());
  }

  public static MorphConfig fromJson(MorphType morph, JsonObject root) {
    if (morph.isPlayer()) {
      throw new IllegalArgumentException("player.json is not supported");
    }
    MorphConfigCodec.validateLayer(morph, root);
    MorphConfig parsed = MorphConfigCodec.fromJson(morph, root, defaults(morph));
    String expectedProfile = "mob_life:" + morph.id();
    if (!root.has("instinct") || !expectedProfile.equals(parsed.instinct().profile())) {
      if (root.has("instinct")) {
        throw new IllegalArgumentException(
            "$.instinct.profile must be " + expectedProfile + " for " + morph.id());
      }
      parsed = parsed.withInstinct(Instinct.unsupported());
    }
    return parsed;
  }

  public MorphConfig withInstinct(Instinct replacement) {
    return new MorphConfig(
        movement,
        diet,
        vision,
        combat,
        attributes,
        inventory,
        sleep,
        outline,
        replacement,
        abilities,
        traits);
  }

  public JsonObject toJson() {
    return MorphConfigCodec.toJson(this);
  }

  private static double finite(double value, double minimum, double maximum) {
    return Double.isFinite(value) ? Math.clamp(value, minimum, maximum) : minimum;
  }

  private static float finite(float value, float minimum, float maximum) {
    return Float.isFinite(value) ? Math.clamp(value, minimum, maximum) : minimum;
  }
}
