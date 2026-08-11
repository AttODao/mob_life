package cc.attodao.mob_life.gameplay.targeting;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.instinct.InstinctRelations;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class MorphOutlineManager {
  private static final int UPDATE_INTERVAL_TICKS = 20;
  private static final int MAX_ENTITIES_PER_CATEGORY = 256;
  private static final Map<UUID, OutlineSnapshot> LAST_SENT = new HashMap<>();

  private MorphOutlineManager() {}

  public static void tick(ServerPlayer player, MorphType morph, boolean canHunt) {
    ResourceKey<Level> dimension = player.level().dimension();
    OutlineSnapshot previous = LAST_SENT.get(player.getUUID());
    if (previous != null && !previous.dimension().equals(dimension)) {
      sendIfChanged(player, new OutlineSnapshot(dimension, Set.of(), Set.of()));
    }
    if ((player.tickCount + Math.floorMod(player.getId(), UPDATE_INTERVAL_TICKS))
            % UPDATE_INTERVAL_TICKS
        != 0) {
      return;
    }

    MorphConfig config = MorphConfigManager.get(morph);
    if (!config.outline().enabled() || config.outline().range() <= 0.0) {
      sendIfChanged(player, new OutlineSnapshot(dimension, Set.of(), Set.of()));
      return;
    }

    double range = Math.min(128.0, config.outline().range());
    List<LivingEntity> nearby =
        player
            .level()
            .getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                entity ->
                    entity != player
                        && entity.isAlive()
                        && entity.distanceToSqr(player) <= range * range);
    nearby.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));

    LinkedHashSet<Integer> predators = new LinkedHashSet<>();
    LinkedHashSet<Integer> prey = new LinkedHashSet<>();
    for (LivingEntity entity : nearby) {
      if (InstinctRelations.isPredator(entity, morph)) {
        if (predators.size() < MAX_ENTITIES_PER_CATEGORY) {
          predators.add(entity.getId());
        }
      } else if (canHunt
          && InstinctRelations.isPrey(entity, morph)
          && prey.size() < MAX_ENTITIES_PER_CATEGORY) {
        prey.add(entity.getId());
      }
    }
    sendIfChanged(player, new OutlineSnapshot(dimension, Set.copyOf(predators), Set.copyOf(prey)));
  }

  public static void clear(MinecraftServer server) {
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      ServerPlayNetworking.send(player, new MobLifeNetworking.OutlinePayload(List.of(), List.of()));
    }
    LAST_SENT.clear();
  }

  public static void remove(ServerPlayer player) {
    LAST_SENT.remove(player.getUUID());
  }

  private static void sendIfChanged(ServerPlayer player, OutlineSnapshot snapshot) {
    if (snapshot.equals(LAST_SENT.put(player.getUUID(), snapshot))) {
      return;
    }
    ServerPlayNetworking.send(
        player,
        new MobLifeNetworking.OutlinePayload(
            List.copyOf(snapshot.predators()), List.copyOf(snapshot.prey())));
  }

  private record OutlineSnapshot(
      ResourceKey<Level> dimension, Set<Integer> predators, Set<Integer> prey) {}
}
