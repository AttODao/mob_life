package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerInventoryMixin {
  @Shadow public ServerPlayer player;

  @Redirect(
      method = "handleSetCarriedItem",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/entity/player/Inventory;getSelectionSize()I"))
  private int mobLife$getActiveHotbarSize() {
    return MorphInventoryCapacity.hotbarSlots(player);
  }
}
