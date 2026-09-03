package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import cc.attodao.mob_life.gameplay.inventory.MorphEquipment;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
  @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
  private void mobLife$hideInactiveInventorySlot(CallbackInfoReturnable<Boolean> cir) {
    if (!mobLife$isActiveInventorySlot()) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventPlacingInInactiveSlot(
      ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
    if (!mobLife$isActiveInventorySlot()) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventTakingFromInactiveSlot(
      Player player, CallbackInfoReturnable<Boolean> cir) {
    if (MorphInstinct.blocksActions(player) || !mobLife$isActiveInventorySlot()) {
      cir.setReturnValue(false);
    }
  }

  private boolean mobLife$isActiveInventorySlot() {
    Slot slot = (Slot) (Object) this;
    if (slot.container instanceof CraftingContainer crafting
        && crafting.getWidth() == 2
        && crafting.getHeight() == 2
        && slot.getContainerSlot() != 0
        && ServerMorphManager.hasMobForm()) {
      return false;
    }
    if (!(slot.container instanceof Inventory inventory)) {
      return true;
    }
    return MorphInventoryCapacity.isActiveInventorySlot(inventory.player, slot.getContainerSlot())
        && MorphEquipment.isSlotActive(inventory.player, slot.getContainerSlot());
  }
}
