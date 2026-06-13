package cc.attodao.mob_life.gameplay.food;

import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

public final class MorphFoodCapacity {
	public static final int PLAYER_MAX_FOOD = 20;
	public static final int MIN_MOB_MAX_FOOD = 10;
	private static final float PLAYER_HEIGHT =
			EntityType.PLAYER.getDimensions().height();

	private MorphFoodCapacity() {
	}

	public static int forMorph(MorphType morph) {
		if (morph == null || morph.isPlayer()) {
			return PLAYER_MAX_FOOD;
		}

		float heightRatio =
				morph.entityType().getDimensions().height() / PLAYER_HEIGHT;
		return Mth.clamp(
				(int) Math.floor(PLAYER_MAX_FOOD * heightRatio + 1.0E-4F),
				MIN_MOB_MAX_FOOD,
				PLAYER_MAX_FOOD
		);
	}

	public static int maxFood(Player player) {
		return ((MorphFoodDataHolder) player.getFoodData())
				.mobLife$getMaxFood();
	}

	public static void apply(Player player, MorphType morph) {
		FoodData foodData = player.getFoodData();
		int maximum = forMorph(morph);
		((MorphFoodDataHolder) foodData).mobLife$setMaxFood(maximum);
		foodData.setFoodLevel(Math.min(foodData.getFoodLevel(), maximum));
		foodData.setSaturation(
				Math.min(foodData.getSaturationLevel(), foodData.getFoodLevel())
		);
	}
}
