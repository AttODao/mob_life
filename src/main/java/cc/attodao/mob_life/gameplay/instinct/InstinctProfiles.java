package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.gameplay.targeting.MorphRelations;
import cc.attodao.mob_life.morph.MorphType;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

public final class InstinctProfiles {
  private static final Set<String> REGISTERED =
      Set.of(
          "mob_life:cow",
          "mob_life:sheep",
          "mob_life:chicken",
          "mob_life:cat",
          "mob_life:ocelot",
          "mob_life:wolf",
          "mob_life:pig",
          "mob_life:horse",
          "mob_life:donkey",
          "mob_life:mule",
          "mob_life:rabbit");

  private InstinctProfiles() {}

  public static boolean supportsProfile(String profile) {
    return REGISTERED.contains(profile);
  }

  public static boolean panicsFromDamage(MorphType morph, DamageSource source) {
    if (morph == null || source == null) {
      return false;
    }
    return switch (morph) {
      case COW, SHEEP, CHICKEN, CAT, PIG, RABBIT -> source.is(DamageTypeTags.PANIC_CAUSES);
      case WOLF -> source.is(DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES);
      default -> false;
    };
  }

  public static boolean isNaturalPrey(MorphType morph, LivingEntity target) {
    if (morph == null || target == null || !target.isAlive()) {
      return false;
    }
    EntityType<?> type = biologicalType(target);
    return switch (morph) {
      case CAT -> type == EntityTypes.RABBIT || type == EntityTypes.TURTLE && isBaby(target);
      case OCELOT -> type == EntityTypes.CHICKEN || type == EntityTypes.TURTLE && isBaby(target);
      case WOLF ->
          type == EntityTypes.SHEEP
              || type == EntityTypes.RABBIT
              || type == EntityTypes.FOX
              || type == EntityTypes.TURTLE && isBaby(target);
      default -> false;
    };
  }

  public static boolean isEdiblePrey(MorphType morph, LivingEntity target) {
    return !(target instanceof net.minecraft.world.entity.player.Player)
        && isNaturalPrey(morph, target)
        && isEnabledPrey(target);
  }

  public static boolean isEnabledNaturalPrey(MorphType morph, LivingEntity target) {
    return isNaturalPrey(morph, target) && isEnabledPrey(target);
  }

  public static boolean isEnabledPrey(LivingEntity target) {
    return InstinctPreyManager.isEnabled(biologicalType(target));
  }

  public static boolean hasAvoidThreat(
      Mob proxy, MorphType morph, ServerPlayer player, float awkwardness) {
    if (awkwardness <= 30.0F) {
      return false;
    }
    if (morph == MorphType.RABBIT
        && proxy instanceof Rabbit rabbit
        && rabbit.getVariant() == Rabbit.Variant.EVIL) {
      return false;
    }
    double scale = Mth.clamp((awkwardness - 30.0F) / 70.0F, 0.0F, 1.0F);
    double maximum =
        switch (morph) {
          case CAT, OCELOT -> 16.0;
          case RABBIT -> 10.0;
          case WOLF -> 24.0;
          default -> 0.0;
        };
    if (maximum <= 0.0) {
      return false;
    }
    AABB area = player.getBoundingBox().inflate(maximum * scale);
    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
      double range = avoidRange(proxy, morph, target);
      if (range > 0.0 && player.distanceToSqr(target) <= range * range * scale * scale) {
        return true;
      }
    }
    return false;
  }

  private static double avoidRange(Mob proxy, MorphType morph, LivingEntity target) {
    if (target instanceof net.minecraft.world.entity.player.Player player) {
      MorphType targetMorph = MorphRelations.morphOf(player);
      if (targetMorph != null) {
        return morph == MorphType.RABBIT && targetMorph == MorphType.WOLF ? 10.0 : 0.0;
      }
    }
    return switch (morph) {
      case CAT, OCELOT -> target instanceof net.minecraft.world.entity.player.Player ? 16.0 : 0.0;
      case RABBIT ->
          target instanceof net.minecraft.world.entity.player.Player
              ? 8.0
              : target instanceof Wolf ? 10.0 : target instanceof Monster ? 4.0 : 0.0;
      case WOLF ->
          target instanceof Llama llama && llama.getStrength() >= proxy.getRandom().nextInt(5)
              ? 24.0
              : 0.0;
      default -> 0.0;
    };
  }

  private static boolean isBaby(LivingEntity entity) {
    return entity instanceof AgeableMob ageable && ageable.isBaby();
  }

  private static EntityType<?> biologicalType(LivingEntity entity) {
    if (entity instanceof net.minecraft.world.entity.player.Player player) {
      MorphType morph = MorphRelations.morphOf(player);
      if (morph != null) {
        return morph.entityType();
      }
    }
    return entity.getType();
  }
}
