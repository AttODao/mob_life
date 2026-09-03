package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

  @Inject(method = "handleSetCarriedItem", at = @At("HEAD"), cancellable = true)
  private void mobLife$blockInstinctHotbar(
      ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
    if (MorphInstinct.blocksActions(player)) {
      ci.cancel();
    }
  }

  @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
  private void mobLife$blockInstinctEntityInteraction(
      ServerboundInteractPacket packet, CallbackInfo ci) {
    if (MorphInstinct.blocksActions(player)) {
      ci.cancel();
    }
  }

  @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
  private void mobLife$blockInstinctContainer(
      ServerboundContainerClickPacket packet, CallbackInfo ci) {
    if (MorphInstinct.blocksActions(player)) {
      player.containerMenu.sendAllDataToRemote();
      ci.cancel();
    }
  }

  @Inject(method = "handleContainerButtonClick", at = @At("HEAD"), cancellable = true)
  private void mobLife$blockInstinctContainerButton(
      ServerboundContainerButtonClickPacket packet, CallbackInfo ci) {
    if (MorphInstinct.blocksActions(player)) {
      ci.cancel();
    }
  }

  @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
  private void mobLife$blockInstinctCreativeSlot(
      ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
    if (MorphInstinct.blocksActions(player)) {
      ci.cancel();
    }
  }

  @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
  private void mobLife$blockInstinctInventoryAction(
      ServerboundPlayerActionPacket packet, CallbackInfo ci) {
    if (!MorphInstinct.blocksActions(player)) {
      return;
    }
    switch (packet.getAction()) {
      case DROP_ALL_ITEMS, DROP_ITEM, SWAP_ITEM_WITH_OFFHAND, STAB -> ci.cancel();
      default -> {
        // Breaking and releasing use are handled by their authoritative gameplay handlers.
      }
    }
  }
}
