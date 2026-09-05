package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientMorphState;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec2;
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
    if (ClientInstinctState.active()) {
      input.keyPresses = Input.EMPTY;
      ((ClientInputMoveVectorAccessor) input).mobLife$setMoveVector(Vec2.ZERO);
      setSprinting(false);
      return;
    }
    ClientMorphState.captureMovementInput(input.keyPresses);
    mobLife$discardBackwardInput();
    mobLife$keepOnlyDismountInput();
  }

  @Inject(method = "aiStep", at = @At("TAIL"))
  private void mobLife$restrictVehicleInputAfterMovement(CallbackInfo ci) {
    mobLife$keepOnlyDismountInput();
    ClientMorphState.afterMovement((LocalPlayer) (Object) this);
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
    mobLife$keepOnlyDismountInput();
  }

  @Inject(method = "applyInput", at = @At("TAIL"))
  private void mobLife$applyMorphLocomotion(CallbackInfo ci) {
    ClientMorphState.MovementInput movement =
        ClientMorphState.applyMovement((LocalPlayer) (Object) this);
    if (!movement.vanilla()) {
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

  private void mobLife$keepOnlyDismountInput() {
    if (ClientInstinctState.active()) {
      input.keyPresses = Input.EMPTY;
      ((ClientInputMoveVectorAccessor) input).mobLife$setMoveVector(Vec2.ZERO);
      setSprinting(false);
      return;
    }
    if (ClientMorphState.morph() == null || !mobLife$isRestrictedVehicle()) {
      return;
    }

    boolean dismount = input.keyPresses.shift();
    input.keyPresses = new Input(false, false, false, false, false, dismount, false);
    setSprinting(false);
  }

  private void mobLife$discardBackwardInput() {
    if (ClientMorphState.morph() == null) {
      return;
    }
    Input raw = input.keyPresses;
    input.keyPresses =
        new Input(
            raw.forward(), false, raw.left(), raw.right(), raw.jump(), raw.shift(), raw.sprint());
    float sideways = (raw.left() ? 1.0F : 0.0F) - (raw.right() ? 1.0F : 0.0F);
    ((ClientInputMoveVectorAccessor) input)
        .mobLife$setMoveVector(new Vec2(sideways, raw.forward() ? 1.0F : 0.0F));
  }

  private boolean mobLife$isRestrictedVehicle() {
    return getVehicle() instanceof AbstractBoat || getVehicle() instanceof AbstractMinecart;
  }
}
