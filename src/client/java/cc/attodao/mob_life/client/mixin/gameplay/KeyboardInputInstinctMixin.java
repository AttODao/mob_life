package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputInstinctMixin extends ClientInput {
  @Inject(method = "tick", at = @At("TAIL"))
  private void mobLife$applyMorphInputRules(CallbackInfo ci) {
    Input raw = keyPresses;
    if (ClientInstinctState.enabled()) {
      boolean manualForward = raw.forward() && ClientInstinctState.state().acceptsForward();
      boolean manualJump = raw.jump() && ClientInstinctState.state().acceptsJump();
      ClientInstinctState.recordKeyboard(manualForward, manualJump);
      // Native shadow-mob velocity controls movement; keys only record permitted intervention.
      keyPresses = new Input(false, false, false, false, false, false, false);
      moveVector = Vec2.ZERO;
      return;
    }
  }
}
