package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
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
    mobLife$processInstinctInput();
    mobLife$keepOnlyDismountInput();
  }

  @Inject(method = "aiStep", at = @At("TAIL"))
  private void mobLife$restrictVehicleInputAfterMovement(CallbackInfo ci) {
    mobLife$keepOnlyDismountInput();
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
    if (ClientMorphState.morph() == null || !mobLife$isRestrictedVehicle()) {
      return;
    }

    boolean dismount = input.keyPresses.shift();
    input.keyPresses = new Input(false, false, false, false, false, dismount, false);
    setSprinting(false);
  }

  private void mobLife$processInstinctInput() {
    Input raw = input.keyPresses;
    Vec2 movement = input.getMoveVector();
    if (mobLife$hasInput(raw, movement)) {
      ClientInstinctState.recordActivity();
    }
    if (!ClientInstinctState.enabled()) {
      return;
    }

    ClientInstinctState.recordMovement(
        raw.forward() || movement.y > 1.0E-4F,
        raw.left() || movement.x > 1.0E-4F,
        raw.right() || movement.x < -1.0E-4F);
    // Controlify replaces the player's ClientInput, so neutralize the common result after it polls.
    input.keyPresses = Input.EMPTY;
    ((ClientInputAccessor) input).mobLife$setMoveVector(Vec2.ZERO);
    setSprinting(false);
  }

  private static boolean mobLife$hasInput(Input input, Vec2 movement) {
    return input.forward()
        || input.backward()
        || input.left()
        || input.right()
        || input.jump()
        || input.shift()
        || input.sprint()
        || movement.lengthSquared() > 1.0E-8F;
  }

  private boolean mobLife$isRestrictedVehicle() {
    return getVehicle() instanceof AbstractBoat || getVehicle() instanceof AbstractMinecart;
  }
}
