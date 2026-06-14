package cc.attodao.mob_life.mixin.food;

import cc.attodao.mob_life.gameplay.food.MorphDiet;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMorphFoodMixin {
  private static final Consumable MOB_LIFE_CONSUMABLE = Consumable.builder().build();
  private static final FoodProperties MOB_LIFE_FOOD =
      new FoodProperties(
          MorphDiet.BREEDING_FOOD_NUTRITION,
          MorphDiet.BREEDING_FOOD_NUTRITION * MorphDiet.BREEDING_FOOD_SATURATION_MODIFIER * 2.0F,
          false);

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
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
        ServerMorphManager.adjustAwkwardness(serverPlayer, -10.0F);
      }
      return;
    }
    if (entity instanceof Player player && MorphDiet.isBreedingFood(player, stack)) {
      if (!level.isClientSide()) {
        player.getFoodData().eat(MOB_LIFE_FOOD);
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
    if (stack.is(net.minecraft.tags.ItemTags.COW_FOOD)
        || stack.is(net.minecraft.tags.ItemTags.SHEEP_FOOD)
        || stack.is(net.minecraft.tags.ItemTags.CHICKEN_FOOD)) {
      cir.setReturnValue(ItemUseAnimation.EAT);
    }
  }
}
