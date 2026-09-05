package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerViewBobMixin {
  @Inject(method = "updateBob", at = @At("HEAD"), cancellable = true)
  private void mobLife$updateInstinctViewBob(CallbackInfo ci) {
    if ((Object) this instanceof LocalPlayer player && ClientInstinctState.updateViewBob(player)) {
      ci.cancel();
    }
  }

  @Inject(method = "addWalkedDistance", at = @At("HEAD"), cancellable = true)
  private void mobLife$freezeRabbitViewBobPhase(float distance, CallbackInfo ci) {
    if ((Object) this instanceof LocalPlayer && ClientInstinctState.freezesViewBobWalkDistance()) {
      ci.cancel();
    }
  }
}
