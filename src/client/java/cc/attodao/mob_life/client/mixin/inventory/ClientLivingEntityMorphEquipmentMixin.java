package cc.attodao.mob_life.client.mixin.inventory;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityMorphEquipmentMixin {

  @Inject(method = "canUseSlot", at = @At("HEAD"), cancellable = true)
  private void mobLife$restrictClientMorphEquipment(
      EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
    MorphType morph = ClientMorphState.morph();
    if (!((Object) this instanceof Player) || morph == null) {
      return;
    }

    if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
      cir.setReturnValue(false);
    } else if (slot == EquipmentSlot.BODY) {
      cir.setReturnValue(morph.canEquipAnimalArmor() || morph.canEquipChest());
    } else if (slot == EquipmentSlot.SADDLE) {
      cir.setReturnValue(morph.canEquipSaddle());
    }
  }
}
