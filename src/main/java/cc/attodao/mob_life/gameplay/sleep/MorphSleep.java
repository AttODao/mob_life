package cc.attodao.mob_life.gameplay.sleep;

import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class MorphSleep {
	public static final int REQUIRED_SLEEP_TICKS = 200;
	private static final int FOOD_COST = 6;

	private MorphSleep() {
	}

	public static void requestSleep(ServerPlayer player) {
		if (!ServerMorphManager.hasMobForm()) {
			return;
		}
		if (player.isSleeping()) {
			player.stopSleepInBed(true, true);
			return;
		}
		if (!MorphAwkwardness.canSleepWithoutBed(player)) {
			player.sendOverlayMessage(Component.translatable(
					"mob_life.sleep.too_awkward"
			));
			return;
		}
		if (player.getFoodData().getFoodLevel() <= FOOD_COST) {
			player.sendOverlayMessage(Component.translatable(
					"mob_life.sleep.not_enough_food",
					FOOD_COST
			));
			return;
		}

		var bedRule = player.level().environmentAttributes().getValue(
				EnvironmentAttributes.BED_RULE,
				player.position()
		);
		if (!bedRule.canSleep(player.level())) {
			Component problem = bedRule.asProblem().message();
			player.sendOverlayMessage(problem != null
					? problem
					: Component.translatable("mob_life.sleep.not_possible"));
			return;
		}

		Optional<BlockPos> surface = findSoftSurface(player);
		if (surface.isEmpty()) {
			player.sendOverlayMessage(Component.translatable(
					"mob_life.sleep.soft_surface_required"
			));
			return;
		}
		if (hasNearbyMonster(player, surface.get())) {
			player.sendOverlayMessage(Component.translatable(
					"block.minecraft.bed.not_safe"
			));
			return;
		}

		BlockPos sleepPos = surface.get();
		BlockState state = player.level().getBlockState(sleepPos);
		double surfaceHeight = state.getCollisionShape(
				player.level(),
				sleepPos
		).max(Direction.Axis.Y);
		player.startSleeping(sleepPos);
		player.setPos(
				sleepPos.getX() + 0.5,
				sleepPos.getY() + surfaceHeight + 0.1,
				sleepPos.getZ() + 0.5
		);
		consumeFood(player);
		player.level().updateSleepingPlayerList();
	}

	public static boolean isCustomSleep(Player player) {
		return player.getSleepingPos()
				.map(pos -> {
					BlockState state = player.level().getBlockState(pos);
					return !(state.getBlock() instanceof BedBlock)
							&& isSoftSurface(state);
				})
				.orElse(false);
	}

	public static boolean isValidSleepingSurface(Player player) {
		return player.getSleepingPos()
				.map(pos -> {
					BlockState state = player.level().getBlockState(pos);
					return state.getBlock() instanceof BedBlock
							|| isSoftSurface(state);
				})
				.orElse(false);
	}

	public static boolean isSoftSurface(BlockState state) {
		return state.is(Blocks.GRASS_BLOCK)
				|| state.is(Blocks.HAY_BLOCK)
				|| state.is(Blocks.MOSS_BLOCK)
				|| state.is(BlockTags.WOOL)
				|| state.is(BlockTags.WOOL_CARPETS)
				|| state.is(BlockTags.BEDS);
	}

	private static Optional<BlockPos> findSoftSurface(Player player) {
		BlockPos current = player.blockPosition();
		if (isUsableSurface(player, current)) {
			return Optional.of(current);
		}

		BlockPos below = current.below();
		return isUsableSurface(player, below)
				? Optional.of(below)
				: Optional.empty();
	}

	private static boolean isUsableSurface(Player player, BlockPos pos) {
		BlockState state = player.level().getBlockState(pos);
		return isSoftSurface(state)
				&& !state.getCollisionShape(player.level(), pos).isEmpty();
	}

	private static boolean hasNearbyMonster(
			ServerPlayer player,
			BlockPos pos
	) {
		Vec3 center = Vec3.atBottomCenterOf(pos);
		AABB area = new AABB(
				center.x - 8.0,
				center.y - 5.0,
				center.z - 8.0,
				center.x + 8.0,
				center.y + 5.0,
				center.z + 8.0
		);
		return !player.level().getEntitiesOfClass(
				Monster.class,
				area,
				monster -> monster.isPreventingPlayerRest(
						player.level(),
						player
				)
		).isEmpty();
	}

	private static void consumeFood(ServerPlayer player) {
		FoodData food = player.getFoodData();
		int remaining = Math.max(0, food.getFoodLevel() - FOOD_COST);
		food.setFoodLevel(remaining);
		food.setSaturation(Math.min(food.getSaturationLevel(), remaining));
	}
}
