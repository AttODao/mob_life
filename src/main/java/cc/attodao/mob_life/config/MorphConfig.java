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
    Instinct instinct,
    Outline outline,
    Abilities abilities,
    Traits traits) {

  public record Movement(
      double referenceMobSpeed,
      double sneakSpeed,
      double walkSpeed,
      double sprintSpeed,
      float sidewaysMultiplier,
      float backwardMultiplier,
      float waterInputMultiplier,
      boolean chargedJump,
      float slowFallMultiplier,
      boolean wingAnimation,
      boolean quadrupedTurning,
      float quadrupedTurnSpeed,
      RabbitHop rabbitHop) {
    public Movement {
      referenceMobSpeed = finite(referenceMobSpeed, 0.0, 4.0);
      sneakSpeed = finite(sneakSpeed, 0.0, 4.0);
      walkSpeed = finite(walkSpeed, 0.0, 4.0);
      sprintSpeed = finite(sprintSpeed, 0.0, 4.0);
      sidewaysMultiplier = finite(sidewaysMultiplier, 0.0F, 4.0F);
      backwardMultiplier = finite(backwardMultiplier, 0.0F, 4.0F);
      waterInputMultiplier = finite(waterInputMultiplier, 0.0F, 4.0F);
      slowFallMultiplier = finite(slowFallMultiplier, 0.0F, 4.0F);
      quadrupedTurnSpeed = finite(quadrupedTurnSpeed, 0.1F, 30.0F);
      rabbitHop = Objects.requireNonNull(rabbitHop);
    }
  }

  public record RabbitHop(
      boolean enabled,
      int sneakCooldown,
      int walkCooldown,
      int sprintCooldown,
      float sneakHorizontalSpeed,
      float walkHorizontalSpeed,
      float sprintHorizontalSpeed,
      double sneakJumpVelocity,
      double walkJumpVelocity,
      double sprintJumpVelocity) {
    public RabbitHop {
      sneakCooldown = Math.clamp(sneakCooldown, 0, 1200);
      walkCooldown = Math.clamp(walkCooldown, 0, 1200);
      sprintCooldown = Math.clamp(sprintCooldown, 0, 1200);
      sneakHorizontalSpeed = finite(sneakHorizontalSpeed, 0.0F, 4.0F);
      walkHorizontalSpeed = finite(walkHorizontalSpeed, 0.0F, 4.0F);
      sprintHorizontalSpeed = finite(sprintHorizontalSpeed, 0.0F, 4.0F);
      sneakJumpVelocity = finite(sneakJumpVelocity, 0.0, 4.0);
      walkJumpVelocity = finite(walkJumpVelocity, 0.0, 4.0);
      sprintJumpVelocity = finite(sprintJumpVelocity, 0.0, 4.0);
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

  public record Instinct(
      boolean enabled,
      Wander wander,
      Intervention intervention,
      Social social,
      Senses senses,
      Hunting hunting,
      Feeding feeding,
      VisualEffect visualEffect) {}

  public record Wander(
      int horizontalRange, int verticalRange, int intervalTicks, float gazeWeight) {
    public Wander {
      horizontalRange = Math.clamp(horizontalRange, 1, 32);
      verticalRange = Math.clamp(verticalRange, 1, 16);
      intervalTicks = Math.clamp(intervalTicks, 1, 1200);
      gazeWeight = finite(gazeWeight, 0.0F, 1.0F);
    }
  }

  public record Intervention(
      float forwardWanderChance,
      int forwardWanderCooldownTicks,
      int forwardWanderDurationMinTicks,
      int forwardWanderDurationMaxTicks,
      int decayPauseTicks) {
    public Intervention {
      forwardWanderChance = finite(forwardWanderChance, 0.0F, 1.0F);
      forwardWanderCooldownTicks = Math.clamp(forwardWanderCooldownTicks, 1, 1200);
      forwardWanderDurationMinTicks = Math.clamp(forwardWanderDurationMinTicks, 1, 1200);
      forwardWanderDurationMaxTicks =
          Math.clamp(
              Math.max(forwardWanderDurationMinTicks, forwardWanderDurationMaxTicks), 1, 1200);
      decayPauseTicks = Math.clamp(decayPauseTicks, 0, 1200);
    }
  }

  public record Social(
      boolean enabled, double searchRange, double preferredRange, int minimumGroupSize) {
    public Social {
      searchRange = finite(searchRange, 1.0, 64.0);
      preferredRange = finite(preferredRange, 1.0, searchRange);
      minimumGroupSize = Math.clamp(minimumGroupSize, 1, 16);
    }
  }

  public record Senses(
      double preyRange, double predatorRange, int memoryTicks, int scanIntervalTicks) {
    public Senses {
      preyRange = finite(preyRange, 0.0, 128.0);
      predatorRange = finite(predatorRange, 0.0, 128.0);
      memoryTicks = Math.clamp(memoryTicks, 0, 1200);
      scanIntervalTicks = Math.clamp(scanIntervalTicks, 5, 200);
    }
  }

  public record Hunting(
      int eatDurationTicks,
      int attackCooldownTicks,
      int postKillCooldownTicks,
      int pursuitTimeoutTicks,
      int abandonedHuntCooldownTicks,
      double felineSprintStartDistance,
      List<Prey> prey) {
    public Hunting {
      eatDurationTicks = Math.clamp(eatDurationTicks, 0, 1200);
      attackCooldownTicks = Math.clamp(attackCooldownTicks, 1, 1200);
      postKillCooldownTicks = Math.clamp(postKillCooldownTicks, 0, 12000);
      pursuitTimeoutTicks = Math.clamp(pursuitTimeoutTicks, 20, 12000);
      abandonedHuntCooldownTicks = Math.clamp(abandonedHuntCooldownTicks, 0, 12000);
      felineSprintStartDistance = finite(felineSprintStartDistance, 0.0, 128.0);
      prey = List.copyOf(prey);
    }
  }

  public record Prey(String selector, int nutrition) {
    public Prey {
      selector = Objects.requireNonNull(selector);
      nutrition = Math.clamp(nutrition, 0, 100);
    }
  }

  public record Feeding(FeedingAction eatBlock, FeedingAction raidGarden) {
    public Feeding {
      eatBlock = Objects.requireNonNull(eatBlock);
      raidGarden = Objects.requireNonNull(raidGarden);
    }
  }

  public record FeedingAction(boolean enabled, int nutrition, int cooldownTicks) {
    public FeedingAction {
      nutrition = Math.clamp(nutrition, 0, 100);
      cooldownTicks = Math.clamp(cooldownTicks, 0, 12000);
    }
  }

  public record VisualEffect(boolean enabled, float strength) {
    public VisualEffect {
      strength = finite(strength, 0.0F, 1.0F);
    }
  }

  public record Outline(boolean enabled, double range) {
    public Outline {
      range = finite(range, 0.0, 128.0);
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
    return Objects.requireNonNull(
        BUILTIN_CONFIGS.get(morph), "Missing builtin config for " + morph.id());
  }

  public static MorphConfig fromJson(MorphType morph, JsonObject root) {
    return MorphConfigCodec.fromJson(root, defaults(morph));
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
