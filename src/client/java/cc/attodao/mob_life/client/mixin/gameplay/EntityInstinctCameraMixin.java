package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientLocomotionController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityInstinctCameraMixin {
  @Inject(method = "absSnapTo(DDDFF)V", at = @At("TAIL"))
  private void mobLife$resetLocomotionAfterTeleport(
      double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
    if ((Object) this instanceof LocalPlayer) {
      ClientLocomotionController.get().teleported();
    }
  }

  @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
  private void mobLife$captureInstinctCamera(double yawInput, double pitchInput, CallbackInfo ci) {
    if ((Object) this instanceof LocalPlayer player
        && ClientInstinctState.captureLookInput(player, yawInput, pitchInput)) {
      ci.cancel();
    } else if ((Object) this instanceof LocalPlayer player
        && ClientLocomotionController.get().captureLook(player, yawInput, pitchInput)) {
      ci.cancel();
    }
  }
}
