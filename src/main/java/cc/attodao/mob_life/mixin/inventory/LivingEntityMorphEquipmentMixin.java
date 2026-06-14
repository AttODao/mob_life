package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMorphEquipmentMixin {

  @Inject(method = "canUseSlot", at = @At("HEAD"), cancellable = true)
  private void mobLife$restrictMorphEquipment(
      EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
    if (!((Object) this instanceof ServerPlayer) || !ServerMorphManager.hasMobForm()) {
      return;
    }

    MorphType morph = ServerMorphManager.activeMorph();
    if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
      cir.setReturnValue(false);
    } else if (slot == EquipmentSlot.BODY) {
      cir.setReturnValue(morph.canEquipBodyArmor() || morph.canEquipChest());
    } else if (slot == EquipmentSlot.SADDLE) {
      cir.setReturnValue(morph.canEquipSaddle());
    }
  }
}
