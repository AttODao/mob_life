package cc.attodao.mob_life.mixin.gameplay;

import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
	@Shadow
	protected ServerLevel level;

	@Shadow
	@Final
	protected ServerPlayer player;

	@Shadow
	private boolean isDestroyingBlock;

	@Shadow
	private BlockPos destroyPos;

	@Shadow
	private boolean hasDelayedDestroy;

	@Shadow
	private BlockPos delayedDestroyPos;

	@Shadow
	private int lastSentState;

	@Inject(method = "tick", at = @At("HEAD"))
	private void mobLife$stopRestrictedBreak(CallbackInfo ci) {
		if (mobLife$isBlockActionRestricted()) {
			mobLife$clearDestroyState();
		}
	}

	@Inject(
			method = "handleBlockBreakAction",
			at = @At("HEAD"),
			cancellable = true
	)
	private void mobLife$preventRestrictedBreakAction(
			BlockPos pos,
			ServerboundPlayerActionPacket.Action action,
			Direction direction,
			int maxY,
			int sequence,
			CallbackInfo ci
	) {
		if (
				action != ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
						&& mobLife$isBlockActionRestricted()
		) {
			mobLife$clearDestroyState();
			player.connection.send(
					new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos))
			);
			ci.cancel();
		}
	}

	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void mobLife$preventRestrictedBreakCompletion(
			BlockPos pos,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (mobLife$isBlockActionRestricted()) {
			mobLife$clearDestroyState();
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "destroyBlock", at = @At("RETURN"))
	private void mobLife$exhaustAfterBreakingBlock(
			BlockPos pos,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (ServerMorphManager.hasMobForm() && cir.getReturnValue()) {
			player.causeFoodExhaustion(0.2F);
			ServerMorphManager.adjustAwkwardness(player, 2.0F);
		}
	}

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void mobLife$preventRestrictedPlacement(
			ServerPlayer player,
			Level level,
			ItemStack itemStack,
			InteractionHand hand,
			BlockHitResult hitResult,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (
				mobLife$isAnyInteractionRestricted()
						|| itemStack.getItem() instanceof BlockItem
						&& mobLife$isMovementRestricted()
		) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "useItemOn", at = @At("RETURN"))
	private void mobLife$exhaustAfterPlacingBlock(
			ServerPlayer player,
			Level level,
			ItemStack itemStack,
			InteractionHand hand,
			BlockHitResult hitResult,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (
				ServerMorphManager.hasMobForm()
						&& cir.getReturnValue().consumesAction()
		) {
			if (itemStack.getItem() instanceof BlockItem) {
				player.causeFoodExhaustion(0.1F);
				ServerMorphManager.adjustAwkwardness(player, 2.0F);
			} else {
				ServerMorphManager.adjustAwkwardness(player, 4.0F);
			}
		}
	}

	private boolean mobLife$isBlockActionRestricted() {
		return ServerMorphManager.hasMobForm()
				&& (
						mobLife$isMovementRestricted()
								|| mobLife$isAnyInteractionRestricted()
				);
	}

	private boolean mobLife$isMovementRestricted() {
		return !player.onGround() || player.isSprinting();
	}

	private boolean mobLife$isAnyInteractionRestricted() {
		return ServerMorphManager.hasMobForm()
				&& MorphAwkwardness.blocksWorldInteraction(player);
	}

	private void mobLife$clearDestroyState() {
		if (isDestroyingBlock) {
			level.destroyBlockProgress(player.getId(), destroyPos, -1);
			isDestroyingBlock = false;
		}
		if (hasDelayedDestroy) {
			level.destroyBlockProgress(player.getId(), delayedDestroyPos, -1);
			hasDelayedDestroy = false;
		}
		lastSentState = -1;
	}
}
