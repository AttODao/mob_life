package cc.attodao.mob_life.gameplay.food;

import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacityHolder;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MorphDiet {

  public static final int BREEDING_FOOD_NUTRITION = 4;
  public static final float BREEDING_FOOD_SATURATION_MODIFIER = 0.3F;

  private MorphDiet() {}

  public static MorphType morph(Player player) {
    return ((MorphInventoryCapacityHolder) player).mobLife$getMorph();
  }

  public static boolean isBreedingFood(Player player, ItemStack stack) {
    MorphType morph = morph(player);
    return switch (morph) {
      case COW -> stack.is(ItemTags.COW_FOOD);
      case SHEEP -> stack.is(ItemTags.SHEEP_FOOD);
      case CHICKEN -> stack.is(ItemTags.CHICKEN_FOOD);
      case CAT -> stack.is(ItemTags.CAT_FOOD);
      case OCELOT -> stack.is(ItemTags.OCELOT_FOOD);
      case WOLF -> stack.is(ItemTags.WOLF_FOOD);
      case PIG -> stack.is(ItemTags.PIG_FOOD);
      case HORSE, DONKEY, MULE -> stack.is(ItemTags.HORSE_FOOD);
      case RABBIT -> stack.is(ItemTags.RABBIT_FOOD);
      case PLAYER -> false;
    };
  }

  public static boolean isBlockedNormalFood(Player player, ItemStack stack) {
    return (!morph(player).isPlayer()
        && stack.has(DataComponents.FOOD)
        && !isBreedingFood(player, stack)
        && !isHuntedMeat(player, stack));
  }

  public static boolean isHuntedMeat(Player player, ItemStack stack) {
    return switch (morph(player)) {
      case CAT -> stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT);
      case OCELOT -> stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN);
      case WOLF ->
          stack.is(Items.MUTTON)
              || stack.is(Items.COOKED_MUTTON)
              || stack.is(Items.RABBIT)
              || stack.is(Items.COOKED_RABBIT);
      default -> false;
    };
  }

  public static boolean canEatBreedingFood(Player player, ItemStack stack) {
    return (isBreedingFood(player, stack) && player.getFoodData().needsFood());
  }
}
