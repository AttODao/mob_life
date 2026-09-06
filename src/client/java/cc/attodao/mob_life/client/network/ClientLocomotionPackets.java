package cc.attodao.mob_life.client.network;

import cc.attodao.mob_life.gameplay.jump.GaitType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Packet adapter for normal-mode locomotion events. */
public final class ClientLocomotionPackets {
  private ClientLocomotionPackets() {}

  public static void sendGait(GaitType type) {
    ClientPlayNetworking.send(new MobLifeNetworking.GaitEventPayload(type));
  }

  public static void sendBodyYaw(float bodyYaw) {
    ClientPlayNetworking.send(new MobLifeNetworking.MorphBodyYawUpdatePayload(bodyYaw));
  }
}
