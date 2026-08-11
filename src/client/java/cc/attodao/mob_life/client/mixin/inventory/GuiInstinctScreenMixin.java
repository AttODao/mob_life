package cc.attodao.mob_life.client.mixin.inventory;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiInstinctScreenMixin {
  @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventInstinctInventory(Screen screen, CallbackInfo ci) {
    if (ClientInstinctState.enabled() && screen instanceof AbstractContainerScreen<?>) {
      ci.cancel();
    }
  }
}
