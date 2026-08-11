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
    if (raw.forward()
        || raw.backward()
        || raw.left()
        || raw.right()
        || raw.jump()
        || raw.shift()
        || raw.sprint()) {
      ClientInstinctState.recordActivity();
    }
    if (ClientInstinctState.enabled()) {
      boolean manualForward = raw.forward() && ClientInstinctState.state().acceptsForward();
      ClientInstinctState.recordKeyboard(manualForward, raw.left(), raw.right());
      // Native shadow-mob velocity controls movement; keys only record permitted intervention.
      keyPresses = new Input(false, false, false, false, false, false, false);
      moveVector = Vec2.ZERO;
      return;
    }
  }
}
