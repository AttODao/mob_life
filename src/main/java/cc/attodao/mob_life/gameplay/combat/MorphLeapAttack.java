package cc.attodao.mob_life.gameplay.combat;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.morph.MorphType;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class MorphLeapAttack {

  private static final Map<Player, Integer> LAST_LEAP_TICKS =
      Collections.synchronizedMap(new WeakHashMap<>());

  private MorphLeapAttack() {}

  public static void tryLeap(Player player, Entity target, MorphType morph) {
    if (morph == null || morph.isPlayer()) {
      return;
    }
    MorphConfig.LeapAttack leap = MorphConfigManager.get(morph).combat().leapAttack();
    double verticalSpeed = leap.verticalSpeed();
    double maximumDistanceSquared = leap.maximumDistance() * leap.maximumDistance();
    if (verticalSpeed <= 0.0
        || !(target instanceof LivingEntity livingTarget)
        || !livingTarget.isAlive()
        || !player.onGround()
        || player.isPassenger()
        || player.getAbilities().flying
        || player.distanceToSqr(target) > maximumDistanceSquared
        || LAST_LEAP_TICKS.getOrDefault(player, Integer.MIN_VALUE) == player.tickCount) {
      return;
    }

    Vec3 direction = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
    if (direction.lengthSqr() <= 1.0E-7) {
      return;
    }

    Vec3 movement = player.getDeltaMovement();
    Vec3 horizontal = direction.normalize().scale(leap.horizontalSpeed()).add(movement.scale(0.2));
    player.setDeltaMovement(horizontal.x, verticalSpeed, horizontal.z);
    LAST_LEAP_TICKS.put(player, player.tickCount);
  }
}
