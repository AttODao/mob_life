package cc.attodao.mob_life.gameplay.targeting;

import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;

public final class MorphPredation {
  private static final double TARGET_RANGE = 24.0;

  private MorphPredation() {}

  public static void acquirePredators(ServerPlayer player, MorphType morph) {
    for (Mob mob :
        player
            .level()
            .getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(TARGET_RANGE),
                candidate -> isPredatorFor(candidate, morph))) {
      if (mob.getTarget() == null
          || mob.distanceToSqr(player) < mob.distanceToSqr(mob.getTarget())) {
        mob.setTarget(player);
      }
    }
  }

  public static boolean isPredatorFor(Mob mob, MorphType morph) {
    Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
    if (!MorphConfigManager.get(morph).combat().predators().contains(id.toString())) {
      return false;
    }
    return !(mob instanceof Wolf wolf) || !wolf.isTame();
  }

  public static boolean isPredatorForPlayer(Mob mob, ServerPlayer player, MorphType morph) {
    return isPredatorFor(mob, morph) && player.isAlive();
  }
}
