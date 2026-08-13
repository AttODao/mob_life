package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerInstinctMixin {
  @Shadow private double accumulatedDX;

  @Shadow private double accumulatedDY;

  @Unique private boolean mobLife$suppressInstinctView;

  @Unique private float mobLife$yawBeforeInstinctTurn;

  @Unique private float mobLife$pitchBeforeInstinctTurn;

  @Unique private float mobLife$headYawBeforeInstinctTurn;

  @Unique private float mobLife$bodyYawBeforeInstinctTurn;

  @Inject(method = "turnPlayer", at = @At("HEAD"))
  private void mobLife$controlInstinctView(double frameTime, CallbackInfo ci) {
    if (ClientInstinctState.enabled() && !ClientInstinctState.locksView()) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
        mobLife$suppressInstinctView = true;
        mobLife$yawBeforeInstinctTurn = player.getYRot();
        mobLife$pitchBeforeInstinctTurn = player.getXRot();
        mobLife$headYawBeforeInstinctTurn = player.getYHeadRot();
        mobLife$bodyYawBeforeInstinctTurn = player.yBodyRot;
      }
      return;
    }
  }

  @Inject(method = "turnPlayer", at = @At("TAIL"))
  private void mobLife$applyInstinctView(double frameTime, CallbackInfo ci) {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null) {
      mobLife$suppressInstinctView = false;
      return;
    }
    if (mobLife$suppressInstinctView) {
      mobLife$suppressInstinctView = false;
      ClientInstinctState.recordViewInput(
          Mth.wrapDegrees(player.getYRot() - mobLife$yawBeforeInstinctTurn),
          player.getXRot() - mobLife$pitchBeforeInstinctTurn);
      player.setYRot(mobLife$yawBeforeInstinctTurn);
      player.setXRot(mobLife$pitchBeforeInstinctTurn);
      player.setYHeadRot(mobLife$headYawBeforeInstinctTurn);
      player.setYBodyRot(mobLife$bodyYawBeforeInstinctTurn);
    }
    ClientInstinctState.applyView(player, frameTime);
  }

  @Inject(method = "turnPlayer", at = @At("TAIL"))
  private void mobLife$turnQuadrupedPerFrame(double frameTime, CallbackInfo ci) {
    if (ClientInstinctState.enabled() || ClientMorphState.morph() == null) {
      return;
    }

    float turnInput = ClientMorphState.quadrupedTurnInput();
    if (Math.abs(turnInput) <= 1.0E-4F) {
      return;
    }

    MorphConfig.Movement movement = MorphConfigManager.get(ClientMorphState.morph()).movement();
    if (!movement.quadrupedTurning()) {
      return;
    }

    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null || player.isPassenger()) {
      return;
    }

    // Match the configured degrees per game tick while updating with each rendered frame.
    float turn = -turnInput * movement.quadrupedTurnSpeed() * (float) (frameTime * 20.0);
    float yaw = player.getYRot() + turn;
    player.setYRot(yaw);
    player.setYHeadRot(yaw);
    player.setYBodyRot(yaw);
  }
}
