package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityInstinctViewMixin {
  @Unique private boolean mobLife$capturingInstinctView;

  @Unique private float mobLife$yawBeforeInstinctTurn;

  @Unique private float mobLife$pitchBeforeInstinctTurn;

  @Unique private float mobLife$headYawBeforeInstinctTurn;

  @Unique private float mobLife$bodyYawBeforeInstinctTurn;

  @Inject(method = "turn", at = @At("HEAD"))
  private void mobLife$captureInstinctViewTurn(double yaw, double pitch, CallbackInfo ci) {
    if (!((Object) this instanceof LocalPlayer player)) {
      return;
    }

    mobLife$capturingInstinctView = true;
    mobLife$yawBeforeInstinctTurn = player.getYRot();
    mobLife$pitchBeforeInstinctTurn = player.getXRot();
    mobLife$headYawBeforeInstinctTurn = player.getYHeadRot();
    mobLife$bodyYawBeforeInstinctTurn = player.yBodyRot;
  }

  @Inject(method = "turn", at = @At("TAIL"))
  private void mobLife$applyInstinctViewTurn(double yaw, double pitch, CallbackInfo ci) {
    if (!mobLife$capturingInstinctView || !((Object) this instanceof LocalPlayer player)) {
      return;
    }

    mobLife$capturingInstinctView = false;
    float yawDelta = Mth.wrapDegrees(player.getYRot() - mobLife$yawBeforeInstinctTurn);
    float pitchDelta = player.getXRot() - mobLife$pitchBeforeInstinctTurn;
    if (Math.abs(yawDelta) > 1.0E-4F || Math.abs(pitchDelta) > 1.0E-4F) {
      ClientInstinctState.recordActivity();
    }
    if (!ClientInstinctState.enabled() || ClientInstinctState.locksView()) {
      return;
    }

    ClientInstinctState.recordViewInput(yawDelta, pitchDelta);
    player.setYRot(mobLife$yawBeforeInstinctTurn);
    player.setXRot(mobLife$pitchBeforeInstinctTurn);
    player.setYHeadRot(mobLife$headYawBeforeInstinctTurn);
    player.setYBodyRot(mobLife$bodyYawBeforeInstinctTurn);
  }
}
