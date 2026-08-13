package cc.attodao.mob_life.mixin.food;

import cc.attodao.mob_life.gameplay.food.MorphDiet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemStack.class)
public abstract class ItemStackMorphFoodSoundMixin {
  @Redirect(
      method = "onUseTick",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/item/component/Consumable;emitParticlesAndSounds(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;I)V"))
  private void mobLife$keepMorphFoodUseSoundLocal(
      Consumable consumable,
      RandomSource random,
      LivingEntity user,
      ItemStack stack,
      int particleCount) {
    if (user instanceof ServerPlayer player && MorphDiet.isBreedingFood(player, stack)) {
      return;
    }
    consumable.emitParticlesAndSounds(random, user, stack, particleCount);
  }
}
