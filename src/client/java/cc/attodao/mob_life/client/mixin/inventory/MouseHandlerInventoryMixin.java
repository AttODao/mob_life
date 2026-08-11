package cc.attodao.mob_life.client.mixin.inventory;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerInventoryMixin {
  @Shadow @Final private Minecraft minecraft;

  @Redirect(
      method = "onScroll",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/entity/player/Inventory;getSelectionSize()I"))
  private int mobLife$getActiveHotbarSize() {
    return minecraft.player == null
        ? MorphInventoryCapacity.MAX_HOTBAR_SLOTS
        : MorphInventoryCapacity.hotbarSlots(minecraft.player);
  }

  @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventInstinctScroll(
      long window, double horizontal, double vertical, CallbackInfo ci) {
    if (ClientInstinctState.enabled() && minecraft.screen == null) {
      ci.cancel();
    }
  }
}
