package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void mobLife$preventFinishingRestrictedBreak(
			BlockPos pos,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (mobLife$isBlockActionRestricted()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
	private void mobLife$preventStartingRestrictedBreak(
			BlockPos pos,
			Direction direction,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (mobLife$isBlockActionRestricted()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
	private void mobLife$preventContinuingRestrictedBreak(
			BlockPos pos,
			Direction direction,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (!mobLife$isBlockActionRestricted()) {
			return;
		}

		((MultiPlayerGameMode) (Object) this).stopDestroyBlock();
		cir.setReturnValue(false);
	}

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void mobLife$preventRestrictedPlacement(
			LocalPlayer player,
			InteractionHand hand,
			BlockHitResult blockHit,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (
				player.getItemInHand(hand).getItem() instanceof BlockItem
						&& mobLife$isBlockActionRestricted()
		) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	private boolean mobLife$isBlockActionRestricted() {
		LocalPlayer player = minecraft.player;
		return ClientMorphState.morph() != null
				&& player != null
				&& (!player.onGround() || player.isSprinting());
	}
}
