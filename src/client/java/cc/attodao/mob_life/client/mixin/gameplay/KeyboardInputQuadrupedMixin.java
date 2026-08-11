package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputQuadrupedMixin extends ClientInput {
  @Inject(method = "tick", at = @At("TAIL"))
  private void mobLife$turnInsteadOfStrafing(CallbackInfo ci) {
    if (ClientInstinctState.enabled() || ClientMorphState.morph() == null) {
      return;
    }
    Input raw = keyPresses;
    MorphConfig.Movement movement = MorphConfigManager.get(ClientMorphState.morph()).movement();
    if (!movement.quadrupedTurning() || raw.left() == raw.right()) {
      return;
    }

    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null || player.isPassenger()) {
      return;
    }
    keyPresses = new Input(true, false, false, false, raw.jump(), raw.shift(), raw.sprint());
    moveVector = new Vec2(0.0F, 1.0F);
  }
}
