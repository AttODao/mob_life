package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowInstinctPickupMixin {
  @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventInstinctProjectilePickup(Player player, CallbackInfo ci) {
    if (InstinctManager.isEnabled(player)) {
      ci.cancel();
    }
  }
}
