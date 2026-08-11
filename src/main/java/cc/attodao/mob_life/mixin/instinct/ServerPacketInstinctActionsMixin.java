package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPacketInstinctActionsMixin {
  @Shadow public ServerPlayer player;

  @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
  private void mobLife$discardInstinctActionPacket(
      ServerboundPlayerActionPacket packet, CallbackInfo ci) {
    if (InstinctManager.isEnabled(player)) {
      ci.cancel();
    }
  }
}
