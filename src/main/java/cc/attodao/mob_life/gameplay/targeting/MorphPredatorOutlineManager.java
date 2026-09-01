package cc.attodao.mob_life.gameplay.targeting;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import java.util.ArrayList;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class MorphPredatorOutlineManager {
  private static final int UPDATE_INTERVAL_TICKS = 20;
  private static final int MAX_ENTITIES = 256;
  private static final Map<UUID, OutlineSnapshot> LAST_SENT = new HashMap<>();

  private MorphPredatorOutlineManager() {}

  public static void tick(ServerPlayer player, MorphType morph) {
    ResourceKey<Level> dimension = player.level().dimension();
    OutlineSnapshot previous = LAST_SENT.get(player.getUUID());
    if (previous != null && !previous.dimension().equals(dimension)) {
      sendIfChanged(player, new OutlineSnapshot(dimension, Set.of()));
    }
    if ((player.tickCount + Math.floorMod(player.getId(), UPDATE_INTERVAL_TICKS))
            % UPDATE_INTERVAL_TICKS
        != 0) {
      return;
    }

    MorphConfig config = MorphConfigManager.get(morph);
    if (!config.outline().enabled() || config.outline().range() <= 0.0) {
      sendIfChanged(player, new OutlineSnapshot(dimension, Set.of()));
      return;
    }

    double range = Math.min(128.0, config.outline().range());
    List<LivingEntity> nearby = new ArrayList<>(MorphNearbyEntities.living(player, range));
    nearby.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));

    LinkedHashSet<Integer> predators = new LinkedHashSet<>();
    for (LivingEntity entity : nearby) {
      if (entity instanceof Mob mob
          && !MorphRelations.isSameSpecies(entity, morph)
          && MorphPredation.isPredatorFor(mob, morph)) {
        predators.add(entity.getId());
        if (predators.size() >= MAX_ENTITIES) {
          break;
        }
      }
    }
    sendIfChanged(player, new OutlineSnapshot(dimension, Set.copyOf(predators)));
  }

  public static void clear(MinecraftServer server) {
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      ServerPlayNetworking.send(player, new MobLifeNetworking.PredatorOutlinePayload(List.of()));
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
        player, new MobLifeNetworking.PredatorOutlinePayload(List.copyOf(snapshot.predators())));
  }

  private record OutlineSnapshot(ResourceKey<Level> dimension, Set<Integer> predators) {}
}
