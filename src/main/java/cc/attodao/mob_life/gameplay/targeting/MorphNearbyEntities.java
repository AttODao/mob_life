package cc.attodao.mob_life.gameplay.targeting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Shares short-lived nearby-entity scans among morph sensing systems. */
public final class MorphNearbyEntities {
  private static final int CACHE_TICKS = 5;
  private static final double MAXIMUM_RANGE = 128.0;
  private static final Map<UUID, Snapshot> SNAPSHOTS = new HashMap<>();

  private MorphNearbyEntities() {}

  public static List<LivingEntity> living(ServerPlayer player, double range) {
    double boundedRange = Math.clamp(range, 0.0, MAXIMUM_RANGE);
    if (boundedRange <= 0.0) {
      return List.of();
    }

    Snapshot snapshot = SNAPSHOTS.get(player.getUUID());
    if (!isUsable(snapshot, player, boundedRange)) {
      snapshot = scan(player, boundedRange);
      SNAPSHOTS.put(player.getUUID(), snapshot);
    }

    double rangeSqr = boundedRange * boundedRange;
    return snapshot.entities().stream()
        .filter(entity -> entity.isAlive() && !entity.isRemoved())
        .filter(entity -> entity.distanceToSqr(player) <= rangeSqr)
        .toList();
  }

  public static void remove(ServerPlayer player) {
    SNAPSHOTS.remove(player.getUUID());
  }

  public static void clear() {
    SNAPSHOTS.clear();
  }

  private static boolean isUsable(Snapshot snapshot, ServerPlayer player, double range) {
    return snapshot != null
        && snapshot.level() == player.level()
        && snapshot.range() >= range
        && player.tickCount - snapshot.tick() <= CACHE_TICKS;
  }

  private static Snapshot scan(ServerPlayer player, double range) {
    ServerLevel level = (ServerLevel) player.level();
    List<LivingEntity> entities =
        level.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(range),
            entity -> entity != player && entity.isAlive() && !entity.isRemoved());
    return new Snapshot(level, player.tickCount, range, List.copyOf(entities));
  }

  private record Snapshot(ServerLevel level, int tick, double range, List<LivingEntity> entities) {}
}
