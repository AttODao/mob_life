package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientLocomotionController;
import cc.attodao.mob_life.client.state.ClientMorphState;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMovementMixin extends LivingEntity {
  @Shadow public ClientInput input;

  private LocalPlayerMovementMixin() {
    super(null, null);
  }

  @Inject(
      method = "aiStep",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/player/ClientInput;tick()V",
              shift = At.Shift.AFTER))
  private void mobLife$restrictVehicleInputAfterPolling(CallbackInfo ci) {
    ClientInstinctState.capture(input);
    ClientLocomotionController.PolledInput filtered =
        ClientLocomotionController.get()
            .captureAndFilter((LocalPlayer) (Object) this, input.keyPresses, input.getMoveVector());
    input.keyPresses = filtered.keys();
    ((ClientInputMoveVectorAccessor) input).mobLife$setMoveVector(filtered.movement());
    if (filtered.disableSprinting()) {
      setSprinting(false);
    }
  }

  @Inject(method = "aiStep", at = @At("TAIL"))
  private void mobLife$restrictVehicleInputAfterMovement(CallbackInfo ci) {
    mobLife$applyOngoingInputPolicy();
    ClientLocomotionController.get().afterTick((LocalPlayer) (Object) this);
  }

  @WrapWithCondition(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V"))
  private boolean mobLife$applyInstinctPositionInsteadOfSending(LocalPlayer player) {
    ClientInstinctState.applyAuthoritativePosition(player);
    return !ClientInstinctState.active();
  }

  @Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true)
  private void mobLife$interpolateMorphCameraYaw(
      float partialTick, CallbackInfoReturnable<Float> cir) {
    if (ClientMorphState.morph() != null) {
      cir.setReturnValue(Mth.rotLerp(partialTick, yRotO, getYRot()));
    }
  }

  @Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true)
  private void mobLife$interpolateMorphCameraPitch(
      float partialTick, CallbackInfoReturnable<Float> cir) {
    if (ClientMorphState.morph() != null) {
      cir.setReturnValue(Mth.lerp(partialTick, xRotO, getXRot()));
    }
  }

  @Inject(method = "rideTick", at = @At("HEAD"))
  private void mobLife$restrictVehicleInputWhileRiding(CallbackInfo ci) {
    mobLife$applyOngoingInputPolicy();
  }

  @Inject(method = "applyInput", at = @At("TAIL"))
  private void mobLife$applyMorphLocomotion(CallbackInfo ci) {
    ClientLocomotionController.MotionInput movement =
        ClientLocomotionController.get().apply((LocalPlayer) (Object) this);
    if (!movement.isVanilla()) {
      xxa = movement.sideways();
      zza = movement.forward();
      jumping = movement.jumping();
    }
  }

  @Inject(method = "canSpawnSprintParticle", at = @At("HEAD"), cancellable = true)
  private void mobLife$disableSprintParticlesWhileTransformed(CallbackInfoReturnable<Boolean> cir) {
    if (ClientMorphState.morph() != null) {
      cir.setReturnValue(false);
    }
  }

  private void mobLife$applyOngoingInputPolicy() {
    ClientLocomotionController.PolledInput filtered =
        ClientLocomotionController.get()
            .filterOngoingVehicleInput(
                (LocalPlayer) (Object) this, input.keyPresses, input.getMoveVector());
    input.keyPresses = filtered.keys();
    ((ClientInputMoveVectorAccessor) input).mobLife$setMoveVector(filtered.movement());
    if (filtered.disableSprinting()) {
      setSprinting(false);
    }
  }
}
