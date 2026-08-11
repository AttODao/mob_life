package cc.attodao.mob_life.gameplay.targeting;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Avoids transformed players without depending on hostile target eligibility. */
public final class TransformedPlayerAvoidGoal extends Goal {
  private final PathfinderMob mob;
  private final Predicate<ServerPlayer> predicate;
  private final float maxDistance;
  private final double walkSpeed;
  private final double sprintSpeed;
  private ServerPlayer player;
  private Path path;

  public TransformedPlayerAvoidGoal(
      PathfinderMob mob,
      Predicate<ServerPlayer> predicate,
      float maxDistance,
      double walkSpeed,
      double sprintSpeed) {
    this.mob = mob;
    this.predicate = predicate;
    this.maxDistance = maxDistance;
    this.walkSpeed = walkSpeed;
    this.sprintSpeed = sprintSpeed;
    setFlags(EnumSet.of(Flag.MOVE));
  }

  @Override
  public boolean canUse() {
    player =
        mob
            .level()
            .getEntitiesOfClass(
                ServerPlayer.class,
                mob.getBoundingBox().inflate(maxDistance, 3.0, maxDistance),
                predicate)
            .stream()
            .min(Comparator.comparingDouble(mob::distanceToSqr))
            .orElse(null);
    if (player == null) {
      return false;
    }

    Vec3 destination = DefaultRandomPos.getPosAway(mob, 16, 7, player.position());
    if (destination == null
        || player.distanceToSqr(destination.x, destination.y, destination.z)
            < player.distanceToSqr(mob)) {
      return false;
    }

    path = mob.getNavigation().createPath(destination.x, destination.y, destination.z, 0);
    return path != null;
  }

  @Override
  public boolean canContinueToUse() {
    return player != null && player.isAlive() && !mob.getNavigation().isDone();
  }

  @Override
  public void start() {
    mob.getNavigation().moveTo(path, walkSpeed);
  }

  @Override
  public void stop() {
    player = null;
  }

  @Override
  public void tick() {
    if (player == null) {
      return;
    }
    if (mob.getTarget() == player) {
      mob.setTarget(null);
    }
    mob.getNavigation()
        .setSpeedModifier(mob.distanceToSqr(player) < 49.0 ? sprintSpeed : walkSpeed);
  }
}
