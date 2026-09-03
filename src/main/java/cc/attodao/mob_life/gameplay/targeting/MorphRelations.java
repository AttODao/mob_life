package cc.attodao.mob_life.gameplay.targeting;

import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacityHolder;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Shared species and herd rules for real mobs and transformed players. */
public final class MorphRelations {

  private MorphRelations() {}

  public static boolean isSameSpecies(Entity entity, MorphType morph) {
    if (entity == null || morph == null) {
      return false;
    }
    if (entity.getType() == morph.entityType()) {
      return true;
    }
    return entity instanceof Player player && morphOf(player) == morph;
  }

  public static MorphType morphOf(Player player) {
    MorphType morph = ((MorphInventoryCapacityHolder) player).mobLife$getMorph();
    return morph != null && !morph.isPlayer() ? morph : null;
  }

  public static boolean isHerdMember(Entity entity, MorphType morph) {
    return entity instanceof LivingEntity living
        && living.isAlive()
        && isSameSpecies(living, morph);
  }
}
