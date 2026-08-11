package cc.attodao.mob_life.client.mixin.world;

import cc.attodao.mob_life.client.screen.MorphSelectionScreen;
import cc.attodao.mob_life.world.PendingWorldSelection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen {

  protected CreateWorldScreenMixin(Component title) {
    super(title);
  }

  @Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
  private void mobLife$openMorphSelectionBeforeCreate(CallbackInfo ci) {
    if (PendingWorldSelection.peek().isPresent()) {
      return;
    }

    if (minecraft != null) {
      minecraft.gui.setScreen(new MorphSelectionScreen((CreateWorldScreen) (Object) this));
    }
    ci.cancel();
  }

  @Inject(method = "popScreen", at = @At("HEAD"))
  private void mobLife$clearPendingWorldSelection(CallbackInfo ci) {
    PendingWorldSelection.clearIfWorldCreationNotStarted();
  }

  @Inject(method = "createNewWorld", at = @At("HEAD"))
  private void mobLife$markPendingWorldCreationStarted(CallbackInfoReturnable<Boolean> cir) {
    PendingWorldSelection.markWorldCreationStarted();
  }

  @Inject(method = "createNewWorld", at = @At("RETURN"))
  private void mobLife$clearPendingWorldSelectionOnCreateFailure(
      CallbackInfoReturnable<Boolean> cir) {
    if (!cir.getReturnValueZ()) {
      PendingWorldSelection.clear();
    }
  }
}
