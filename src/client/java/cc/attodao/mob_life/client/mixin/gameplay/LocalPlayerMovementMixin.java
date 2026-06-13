package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMovementMixin extends LivingEntity {
	@Shadow
	public ClientInput input;

	private LocalPlayerMovementMixin() {
		super(null, null);
	}

	@Inject(
			method = "aiStep",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/ClientInput;tick()V",
					shift = At.Shift.AFTER
			)
	)
	private void mobLife$restrictVehicleInputAfterPolling(
			CallbackInfo ci
	) {
		mobLife$keepOnlyDismountInput();
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void mobLife$restrictVehicleInputAfterMovement(
			CallbackInfo ci
	) {
		mobLife$keepOnlyDismountInput();
	}

	@Inject(method = "rideTick", at = @At("HEAD"))
	private void mobLife$restrictVehicleInputWhileRiding(
			CallbackInfo ci
	) {
		mobLife$keepOnlyDismountInput();
	}

	@Inject(method = "applyInput", at = @At("TAIL"))
	private void mobLife$slowNonForwardMovement(CallbackInfo ci) {
		if (ClientMorphState.morph() == null || isPassenger()) {
			return;
		}

		if (zza <= 0.0F) {
			xxa *= 0.25F;
			zza *= 0.25F;
		} else {
			xxa *= 0.25F;
		}
	}

	private void mobLife$keepOnlyDismountInput() {
		if (
				ClientMorphState.morph() == null
						|| !mobLife$isRestrictedVehicle()
		) {
			return;
		}

		boolean dismount = input.keyPresses.shift();
		input.keyPresses = new Input(
				false,
				false,
				false,
				false,
				false,
				dismount,
				false
		);
		setSprinting(false);
	}

	private boolean mobLife$isRestrictedVehicle() {
		return getVehicle() instanceof AbstractBoat
				|| getVehicle() instanceof AbstractMinecart;
	}
}
