package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityInstinctPickupMixin {
  @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventInstinctPickup(Player player, CallbackInfo ci) {
    if (InstinctManager.isEnabled(player)) {
      ci.cancel();
    }
  }
}
