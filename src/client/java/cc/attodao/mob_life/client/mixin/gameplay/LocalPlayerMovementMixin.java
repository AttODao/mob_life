package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMovementMixin extends LivingEntity {
	private LocalPlayerMovementMixin() {
		super(null, null);
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
}
