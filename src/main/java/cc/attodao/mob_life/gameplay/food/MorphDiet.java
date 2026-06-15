package cc.attodao.mob_life.gameplay.food;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacityHolder;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public final class MorphDiet {

  private MorphDiet() {}

  public static MorphType morph(Player player) {
    return ((MorphInventoryCapacityHolder) player).mobLife$getMorph();
  }

  public static boolean isBreedingFood(Player player, ItemStack stack) {
    return matchesAny(stack, config(player).foods());
  }

  public static boolean isBlockedNormalFood(Player player, ItemStack stack) {
    return (!morph(player).isPlayer()
        && stack.has(DataComponents.FOOD)
        && !isBreedingFood(player, stack)
        && !isHuntedMeat(player, stack));
  }

  public static boolean isHuntedMeat(Player player, ItemStack stack) {
    return matchesAny(stack, config(player).huntedFoods());
  }

  public static boolean canEatBreedingFood(Player player, ItemStack stack) {
    return (isBreedingFood(player, stack) && player.getFoodData().needsFood());
  }

  public static FoodProperties foodProperties(Player player) {
    MorphConfig.Diet diet = config(player);
    return new FoodProperties(
        diet.nutrition(), diet.nutrition() * diet.saturationModifier() * 2.0F, false);
  }

  public static boolean isConfiguredFood(ItemStack stack) {
    for (MorphType morph : MorphType.values()) {
      if (matchesAny(stack, MorphConfigManager.get(morph).diet().foods())) {
        return true;
      }
    }
    return false;
  }

  private static MorphConfig.Diet config(Player player) {
    return MorphConfigManager.get(morph(player)).diet();
  }

  private static boolean matchesAny(ItemStack stack, Iterable<String> entries) {
    for (String entry : entries) {
      if (entry.startsWith("#")) {
        Identifier id = Identifier.tryParse(entry.substring(1));
        if (id != null && stack.is(TagKey.create(Registries.ITEM, id))) {
          return true;
        }
      } else {
        Identifier id = Identifier.tryParse(entry);
        if (id != null
            && BuiltInRegistries.ITEM.getOptional(id).isPresent()
            && stack.is(BuiltInRegistries.ITEM.getValue(id))) {
          return true;
        }
      }
    }
    return false;
  }
}
