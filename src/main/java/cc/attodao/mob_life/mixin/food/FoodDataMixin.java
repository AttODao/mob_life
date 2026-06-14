package cc.attodao.mob_life.mixin.food;

import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.food.MorphFoodDataHolder;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin implements MorphFoodDataHolder {
  @Shadow private int foodLevel;

  @Shadow private float saturationLevel;

  @Unique private int mobLife$maxFood = MorphFoodCapacity.PLAYER_MAX_FOOD;

  @Override
  public int mobLife$getMaxFood() {
    return mobLife$maxFood;
  }

  @Override
  public void mobLife$setMaxFood(int maximum) {
    mobLife$maxFood = maximum;
    foodLevel = Math.min(foodLevel, maximum);
    saturationLevel = Math.min(saturationLevel, foodLevel);
  }

  @ModifyArg(
      method = "add",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"),
      index = 2)
  private int mobLife$useMaximumWhenEating(int vanillaMaximum) {
    return mobLife$maxFood;
  }

  @ModifyConstant(method = "needsFood", constant = @Constant(intValue = 20))
  private int mobLife$useMaximumForNeedsFood(int vanillaMaximum) {
    return mobLife$maxFood;
  }

  @ModifyConstant(method = "tick", constant = @Constant(intValue = 20))
  private int mobLife$useMaximumForFastRegeneration(int vanillaMaximum) {
    return mobLife$maxFood;
  }

  @ModifyConstant(method = "tick", constant = @Constant(intValue = 18))
  private int mobLife$useMaximumForRegeneration(int vanillaThreshold) {
    return Math.max(1, mobLife$maxFood - 2);
  }

  @ModifyConstant(method = "hasEnoughFood", constant = @Constant(floatValue = 6.0F))
  private float mobLife$scaleExhaustiveActionThreshold(float vanillaThreshold) {
    return mobLife$maxFood * 0.3F;
  }

  @ModifyVariable(method = "setFoodLevel", at = @At("HEAD"), argsOnly = true)
  private int mobLife$clampFoodLevel(int food) {
    return Mth.clamp(food, 0, mobLife$maxFood);
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$clampLoadedFood(ValueInput input, CallbackInfo ci) {
    foodLevel = Mth.clamp(foodLevel, 0, mobLife$maxFood);
    saturationLevel = Mth.clamp(saturationLevel, 0.0F, foodLevel);
  }
}
