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
      RabbitHop rabbitHop) {}

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
      double sprintJumpVelocity) {}

  public record Diet(List<String> foods, int nutrition, float saturationModifier) {}

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

  public record Instinct(
      boolean enabled,
      Wander wander,
      Senses senses,
      Hunting hunting,
      Feeding feeding,
      VisualEffect visualEffect) {}

  public record Wander(
      int horizontalRange, int verticalRange, int intervalTicks, float gazeWeight) {}

  public record Senses(
      double preyRange, double predatorRange, int memoryTicks, int scanIntervalTicks) {}

  public record Hunting(
      float startFoodRatio,
      float stopFoodRatio,
      int eatDurationTicks,
      int attackCooldownTicks,
      int postKillCooldownTicks,
      double felineSprintStartDistance,
      List<Prey> prey) {
    public Hunting {
      startFoodRatio = Math.clamp(startFoodRatio, 0.0F, 1.0F);
      stopFoodRatio = Math.clamp(stopFoodRatio, 0.0F, 1.0F);
      if (stopFoodRatio <= startFoodRatio) {
        startFoodRatio = Math.min(startFoodRatio, 0.95F);
        stopFoodRatio = startFoodRatio + 0.05F;
      }
      felineSprintStartDistance = Math.clamp(felineSprintStartDistance, 0.0, 128.0);
      prey = List.copyOf(prey);
    }
  }

  public record Prey(String selector, int nutrition) {}

  public record Feeding(FeedingAction eatBlock, FeedingAction raidGarden) {}

  public record FeedingAction(boolean enabled, int nutrition, int cooldownTicks) {}

  public record VisualEffect(boolean enabled, float strength) {
    public VisualEffect {
      strength = Math.clamp(strength, 0.0F, 1.0F);
    }
  }

  public record Outline(boolean enabled, double range) {}

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
}
