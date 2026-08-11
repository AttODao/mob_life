package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerInstinctMixin {
  @Shadow private double accumulatedDX;

  @Shadow private double accumulatedDY;

  @Inject(method = "turnPlayer", at = @At("HEAD"))
  private void mobLife$controlInstinctView(double frameTime, CallbackInfo ci) {
    boolean hasViewInput = Math.abs(accumulatedDX) > 1.0E-4 || Math.abs(accumulatedDY) > 1.0E-4;
    if (hasViewInput) {
      ClientInstinctState.recordActivity();
    }
    if (hasViewInput && !ClientInstinctState.locksView() && ClientInstinctState.enabled()) {
      ClientInstinctState.recordViewInput();
    }
  }
}
