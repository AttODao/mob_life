package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.gameplay.inventory.MorphChestInventory;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMorphChestInventoryMixin {
  @Inject(method = "restoreFrom", at = @At("TAIL"))
  private void mobLife$restoreMorphChestInventory(
      ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
    if (keepEverything) {
      MorphChestInventory.get((ServerPlayer) (Object) this)
          .copyFrom(MorphChestInventory.get(oldPlayer));
    }
  }
}
