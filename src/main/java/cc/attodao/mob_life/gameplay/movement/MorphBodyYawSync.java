package cc.attodao.mob_life.gameplay.movement;

import cc.attodao.mob_life.network.MobLifeNetworking;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public final class MorphBodyYawSync {
  private static final Map<UUID, Float> BODY_YAWS = new HashMap<>();

  private MorphBodyYawSync() {}

  public static void registerEvents() {
    EntityTrackingEvents.START_TRACKING.register(
        (entity, trackingPlayer) -> {
          if (entity instanceof ServerPlayer source) {
            send(
                trackingPlayer, source, BODY_YAWS.getOrDefault(source.getUUID(), source.getYRot()));
          }
        });
    ServerPlayConnectionEvents.DISCONNECT.register(
        (handler, server) -> BODY_YAWS.remove(handler.getPlayer().getUUID()));
  }

  public static void receive(ServerPlayer source, float bodyYaw) {
    if (!Float.isFinite(bodyYaw)) {
      return;
    }
    bodyYaw = Mth.wrapDegrees(bodyYaw);
    Float previous = BODY_YAWS.put(source.getUUID(), bodyYaw);
    if (previous != null && Float.compare(previous, bodyYaw) == 0) {
      return;
    }
    for (ServerPlayer trackingPlayer : PlayerLookup.tracking(source)) {
      if (trackingPlayer != source) {
        send(trackingPlayer, source, bodyYaw);
      }
    }
    send(source, source, bodyYaw);
  }

  private static void send(ServerPlayer recipient, ServerPlayer source, float bodyYaw) {
    ServerPlayNetworking.send(
        recipient, new MobLifeNetworking.MorphBodyYawPayload(source.getId(), bodyYaw));
  }
}
