package cc.attodao.mob_life.gameplay.food;

import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.morph.MorphBodyScale;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

public final class MorphFoodCapacity {

  public static final int PLAYER_MAX_FOOD = 20;
  public static final int ABSOLUTE_MIN_MOB_MAX_FOOD = 8;
  private static final float PLAYER_HEIGHT = EntityTypes.PLAYER.getDimensions().height();

  private MorphFoodCapacity() {}

  public static int forMorph(MorphType morph) {
    float height =
        morph == null || morph.isPlayer()
            ? PLAYER_HEIGHT
            : morph.entityType().getDimensions().height();
    return forMorph(morph, height);
  }

  public static int forMorph(MorphType morph, float morphHeight) {
    if (morph == null || morph.isPlayer()) {
      return PLAYER_MAX_FOOD;
    }

    float adultHeight = morph.entityType().getDimensions().height();
    float maximum =
        MorphConfigManager.get(morph).attributes().maximumFood()
            * MorphBodyScale.relativeTo(morphHeight, adultHeight);
    return Math.max(ABSOLUTE_MIN_MOB_MAX_FOOD, (int) Math.floor(maximum + 1.0E-4F));
  }

  public static int maxFood(Player player) {
    return ((MorphFoodDataHolder) player.getFoodData()).mobLife$getMaxFood();
  }

  public static void apply(Player player, MorphType morph) {
    float height =
        morph == null || morph.isPlayer()
            ? PLAYER_HEIGHT
            : morph.entityType().getDimensions().height();
    apply(player, morph, height);
  }

  public static void apply(Player player, MorphType morph, float morphHeight) {
    FoodData foodData = player.getFoodData();
    int maximum = forMorph(morph, morphHeight);
    ((MorphFoodDataHolder) foodData).mobLife$setMaxFood(maximum);
    foodData.setFoodLevel(Math.min(foodData.getFoodLevel(), maximum));
    foodData.setSaturation(Math.min(foodData.getSaturationLevel(), foodData.getFoodLevel()));
  }
}
