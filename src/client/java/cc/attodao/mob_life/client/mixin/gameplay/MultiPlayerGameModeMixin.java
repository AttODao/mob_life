package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
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
				mobLife$isAnyInteractionRestricted()
						|| player.getItemInHand(hand).getItem()
						instanceof BlockItem
						&& mobLife$isMovementRestricted()
		) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void mobLife$preventMobInteraction(
			net.minecraft.world.entity.player.Player player,
			Entity entity,
			EntityHitResult hitResult,
			InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (
				ClientMorphState.morph() != null
						&& entity instanceof Mob
		) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	private boolean mobLife$isBlockActionRestricted() {
		LocalPlayer player = minecraft.player;
		return ClientMorphState.morph() != null
				&& player != null
				&& (
						mobLife$isMovementRestricted()
								|| mobLife$isAnyInteractionRestricted()
				);
	}

	private boolean mobLife$isMovementRestricted() {
		LocalPlayer player = minecraft.player;
		return player != null
				&& (!player.onGround() || player.isSprinting());
	}

	private boolean mobLife$isAnyInteractionRestricted() {
		return ClientMorphState.morph() != null
				&& ClientMorphState.awkwardness()
				>= MorphAwkwardness.ACTION_LOCK_THRESHOLD;
	}
}
