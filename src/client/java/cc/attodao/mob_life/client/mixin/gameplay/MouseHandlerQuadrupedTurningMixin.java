package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerQuadrupedTurningMixin {
  @Inject(method = "turnPlayer", at = @At("TAIL"))
  private void mobLife$turnQuadrupedPerFrame(double frameTime, CallbackInfo ci) {
    if (ClientMorphState.morph() == null) {
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
