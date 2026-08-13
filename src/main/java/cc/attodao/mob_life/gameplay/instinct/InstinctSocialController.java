package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.targeting.MorphRelations;
import cc.attodao.mob_life.morph.MorphDefinition;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Owns herd membership and cohesion decisions; player morphs use the same relation as real mobs.
 */
final class InstinctSocialController {
  private final ServerPlayer player;
  private final MorphDefinition definition;
  private final MorphConfig.Social social;
  private Vec3 herdCenter;

  InstinctSocialController(
      ServerPlayer player, MorphDefinition definition, MorphConfig.Social social) {
    this.player = player;
    this.definition = definition;
    this.social = social;
  }

  void update(List<LivingEntity> nearby) {
    if (!social.enabled()) {
      herdCenter = null;
      return;
    }

    Vec3 sum = Vec3.ZERO;
    int members = 0;
    double maximumDistanceSqr = social.searchRange() * social.searchRange();
    for (LivingEntity entity : nearby) {
      if (!MorphRelations.isHerdMember(entity, definition.type())
          || entity.distanceToSqr(player) > maximumDistanceSqr) {
        continue;
      }
      sum = sum.add(entity.position());
      members++;
    }
    herdCenter =
        members >= social.minimumGroupSize()
            ? new Vec3(sum.x / members, player.getY(), sum.z / members)
            : null;
  }

  boolean canFollow(boolean fleeing, boolean huntingTarget, boolean eating) {
    if (herdCenter == null || fleeing || huntingTarget || eating) {
      return false;
    }
    Vec3 delta = herdCenter.subtract(player.position()).multiply(1.0, 0.0, 1.0);
    return delta.lengthSqr() > social.preferredRange() * social.preferredRange();
  }

  Vec3 herdCenter() {
    return herdCenter;
  }
}
