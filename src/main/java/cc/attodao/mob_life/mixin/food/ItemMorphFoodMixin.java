package cc.attodao.mob_life.mixin.food;

import cc.attodao.mob_life.gameplay.food.MorphDiet;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMorphFoodMixin {
  private static final Consumable MOB_LIFE_CONSUMABLE = Consumable.builder().build();

  @Inject(method = "use", at = @At("HEAD"), cancellable = true)
  private void mobLife$useMorphFood(
      Level level,
      Player player,
      InteractionHand hand,
      CallbackInfoReturnable<InteractionResult> cir) {
    ItemStack stack = player.getItemInHand(hand);
    if (MorphDiet.isHuntedMeat(player, stack)) {
      return;
    }
    if (MorphDiet.canEatBreedingFood(player, stack)) {
      cir.setReturnValue(MOB_LIFE_CONSUMABLE.startConsuming(player, stack, hand));
    } else if (MorphDiet.isBreedingFood(player, stack)
        || MorphDiet.isBlockedNormalFood(player, stack)) {
      cir.setReturnValue(InteractionResult.FAIL);
    }
  }

  @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
  private void mobLife$finishMorphFood(
      ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
    if (entity instanceof Player player && MorphDiet.isHuntedMeat(player, stack)) {
      if (level.isClientSide()) {
        cir.setReturnValue(stack);
        return;
      }

      FoodProperties cookedFood = mobLife$cookedFood(stack);
      Consumable consumable = stack.get(DataComponents.CONSUMABLE);
      if (cookedFood != null && consumable != null) {
        FoodProperties originalFood = stack.get(DataComponents.FOOD);
        stack.set(DataComponents.FOOD, cookedFood);
        ItemStack result = consumable.onConsume(level, player, stack);
        if (!result.isEmpty() && originalFood != null) {
          result.set(DataComponents.FOOD, originalFood);
        }
        if (player instanceof ServerPlayer serverPlayer) {
          ServerMorphManager.adjustAwkwardness(serverPlayer, -10.0F);
        }
        cir.setReturnValue(result);
      } else if (player instanceof ServerPlayer serverPlayer) {
        ServerMorphManager.adjustAwkwardness(serverPlayer, -10.0F);
      }
      return;
    }
    if (entity instanceof Player player && MorphDiet.isBreedingFood(player, stack)) {
      if (!level.isClientSide()) {
        player.getFoodData().eat(MorphDiet.foodProperties(player));
        if (player instanceof ServerPlayer serverPlayer) {
          ServerMorphManager.adjustAwkwardness(serverPlayer, -10.0F);
        }
        cir.setReturnValue(MOB_LIFE_CONSUMABLE.onConsume(level, player, stack));
      } else {
        cir.setReturnValue(stack);
      }
    }
  }

  @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
  private void mobLife$getMorphFoodUseDuration(
      ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
    if (entity instanceof Player player
        && MorphDiet.isBreedingFood(player, stack)
        && !MorphDiet.isHuntedMeat(player, stack)) {
      cir.setReturnValue(MOB_LIFE_CONSUMABLE.consumeTicks());
    }
  }

  @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
  private void mobLife$getMorphFoodUseAnimation(
      ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
    if (MorphDiet.isConfiguredFood(stack)) {
      cir.setReturnValue(ItemUseAnimation.EAT);
    }
  }

  private static FoodProperties mobLife$cookedFood(ItemStack stack) {
    ItemStack cooked;
    if (stack.is(Items.RABBIT)) {
      cooked = new ItemStack(Items.COOKED_RABBIT);
    } else if (stack.is(Items.CHICKEN)) {
      cooked = new ItemStack(Items.COOKED_CHICKEN);
    } else if (stack.is(Items.MUTTON)) {
      cooked = new ItemStack(Items.COOKED_MUTTON);
    } else {
      return null;
    }
    return cooked.get(DataComponents.FOOD);
  }
}
