package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityInstinctHandsMixin {
  @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
  private void mobLife$emptyInstinctHands(
      EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
    if ((Object) this instanceof ServerPlayer player
        && MorphInstinct.isActive(player)
        && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)) {
      cir.setReturnValue(ItemStack.EMPTY);
    }
  }
}
