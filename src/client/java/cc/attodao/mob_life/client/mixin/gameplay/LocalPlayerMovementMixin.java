package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
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
      ClientMorphState.setQuadrupedTurnInput(0.0F);
      input.keyPresses = Input.EMPTY;
      ((ClientInputMoveVectorAccessor) input).mobLife$setMoveVector(Vec2.ZERO);
      setSprinting(false);
      return;
    }
    mobLife$applyQuadrupedInput();
    mobLife$keepOnlyDismountInput();
  }

  @Inject(method = "aiStep", at = @At("TAIL"))
  private void mobLife$restrictVehicleInputAfterMovement(CallbackInfo ci) {
    mobLife$keepOnlyDismountInput();
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
  private void mobLife$interpolateInstinctCameraYaw(
      float partialTick, CallbackInfoReturnable<Float> cir) {
    if (ClientInstinctState.active()) {
      cir.setReturnValue(Mth.rotLerp(partialTick, yRotO, getYRot()));
    }
  }

  @Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true)
  private void mobLife$interpolateInstinctCameraPitch(
      float partialTick, CallbackInfoReturnable<Float> cir) {
    if (ClientInstinctState.active()) {
      cir.setReturnValue(Mth.lerp(partialTick, xRotO, getXRot()));
    }
  }

  @Inject(method = "rideTick", at = @At("HEAD"))
  private void mobLife$restrictVehicleInputWhileRiding(CallbackInfo ci) {
    mobLife$keepOnlyDismountInput();
  }

  @Inject(method = "applyInput", at = @At("TAIL"))
  private void mobLife$slowNonForwardMovement(CallbackInfo ci) {
    if (ClientMorphState.morph() == null || isPassenger()) {
      return;
    }

    MorphConfig.Movement movement = MorphConfigManager.get(ClientMorphState.morph()).movement();
    if (movement.rabbitHop().enabled() && onGround() && !isInWater() && !isInLava()) {
      xxa = 0.0F;
      zza = 0.0F;
      return;
    }

    if (zza <= 0.0F) {
      xxa *= movement.sidewaysMultiplier();
      zza *= movement.backwardMultiplier();
    } else {
      xxa *= movement.sidewaysMultiplier();
    }

    if (isInWater()) {
      float waterInputScale = ClientMorphState.waterMovementInputScale();
      xxa *= waterInputScale;
      zza *= waterInputScale;
      setSprinting(false);
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

  private void mobLife$applyQuadrupedInput() {
    ClientMorphState.setQuadrupedTurnInput(0.0F);
    if (ClientMorphState.morph() == null || isPassenger()) {
      return;
    }

    MorphConfig.Movement movement = MorphConfigManager.get(ClientMorphState.morph()).movement();
    if (!movement.quadrupedTurning()) {
      return;
    }

    Input raw = input.keyPresses;
    Vec2 moveVector = input.getMoveVector();
    float turnInput = moveVector.x;
    if (Math.abs(turnInput) <= 1.0E-4F && raw.left() != raw.right()) {
      turnInput = raw.left() ? 1.0F : -1.0F;
    }
    if (Math.abs(turnInput) <= 1.0E-4F) {
      return;
    }

    ClientMorphState.setQuadrupedTurnInput(turnInput);
    float forwardInput = Mth.clamp(Math.max(moveVector.y, Math.abs(turnInput)), 0.0F, 1.0F);
    input.keyPresses =
        new Input(
            forwardInput > 1.0E-4F, false, false, false, raw.jump(), raw.shift(), raw.sprint());
    ((ClientInputMoveVectorAccessor) input).mobLife$setMoveVector(new Vec2(0.0F, forwardInput));
  }

  private boolean mobLife$isRestrictedVehicle() {
    return getVehicle() instanceof AbstractBoat || getVehicle() instanceof AbstractMinecart;
  }
}
