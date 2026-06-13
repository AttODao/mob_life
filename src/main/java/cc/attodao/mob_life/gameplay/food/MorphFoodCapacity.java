package cc.attodao.mob_life.gameplay.food;

import cc.attodao.mob_life.morph.MorphBodyScale;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

public final class MorphFoodCapacity {

    public static final int PLAYER_MAX_FOOD = 20;
    public static final int MIN_MOB_MAX_FOOD = 10;
    public static final int ABSOLUTE_MIN_MOB_MAX_FOOD = 8;
    private static final float PLAYER_HEIGHT =
        EntityType.PLAYER.getDimensions().height();
    private static final float SMALL_FORM_HEIGHT =
        EntityType.CHICKEN.getDimensions().height();

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

        float maximum =
            morphHeight <= SMALL_FORM_HEIGHT
                ? MIN_MOB_MAX_FOOD *
                  MorphBodyScale.relativeTo(morphHeight, SMALL_FORM_HEIGHT)
                : Math.max(
                      MIN_MOB_MAX_FOOD,
                      PLAYER_MAX_FOOD *
                          MorphBodyScale.relativeTo(morphHeight, PLAYER_HEIGHT)
                  );
        return Mth.clamp(
            (int) Math.floor(maximum + 1.0E-4F),
            ABSOLUTE_MIN_MOB_MAX_FOOD,
            PLAYER_MAX_FOOD
        );
    }

    public static int maxFood(Player player) {
        return (
            (MorphFoodDataHolder) player.getFoodData()
        ).mobLife$getMaxFood();
    }

    public static void apply(Player player, MorphType morph) {
        float height =
            morph == null || morph.isPlayer()
                ? PLAYER_HEIGHT
                : morph.entityType().getDimensions().height();
        apply(player, morph, height);
    }

    public static void apply(
        Player player,
        MorphType morph,
        float morphHeight
    ) {
        FoodData foodData = player.getFoodData();
        int maximum = forMorph(morph, morphHeight);
        ((MorphFoodDataHolder) foodData).mobLife$setMaxFood(maximum);
        foodData.setFoodLevel(Math.min(foodData.getFoodLevel(), maximum));
        foodData.setSaturation(
            Math.min(foodData.getSaturationLevel(), foodData.getFoodLevel())
        );
    }
}
