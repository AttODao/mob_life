package cc.attodao.mob_life.mixin.gameplay;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerVehicleInputMixin {
  @Shadow public ServerPlayer player;

  @ModifyArg(
      method = "handlePlayerInput",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/server/level/ServerPlayer;setLastClientInput(Lnet/minecraft/world/entity/player/Input;)V"),
      index = 0)
  private Input mobLife$keepOnlyDismountInput(Input input) {
    if (InstinctManager.isEnabled(player)) {
      player.setSprinting(false);
      return new Input(false, false, false, false, false, false, false);
    }
    if (!mobLife$isRestrictedVehicle()) {
      return input;
    }

    player.setSprinting(false);
    return new Input(false, false, false, false, false, input.shift(), false);
  }

  @Inject(
      method = "handlePlayerCommand",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V",
              shift = At.Shift.AFTER),
      cancellable = true)
  private void mobLife$preventVehicleMovementCommands(
      ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
    if (!mobLife$isRestrictedVehicle()) {
      return;
    }

    switch (packet.getAction()) {
      case START_SPRINTING, START_RIDING_JUMP, STOP_RIDING_JUMP -> ci.cancel();
      default -> {}
    }
  }

  private boolean mobLife$isRestrictedVehicle() {
    return ServerMorphManager.hasMobForm()
        && (player.getVehicle() instanceof AbstractBoat
            || player.getVehicle() instanceof AbstractMinecart);
  }
}
