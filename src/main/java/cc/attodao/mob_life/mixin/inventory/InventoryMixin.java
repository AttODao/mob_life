package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.gameplay.inventory.MorphEquipment;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
  @Shadow @Final private NonNullList<ItemStack> items;

  @Shadow @Final public Player player;

  @Shadow
  public abstract int getSelectedSlot();

  @Shadow
  public abstract ItemStack getItem(int slot);

  @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
  private void mobLife$getActiveFreeSlot(CallbackInfoReturnable<Integer> cir) {
    if (!MorphInventoryCapacity.hasMobForm(player)) {
      return;
    }
    for (int slot = 0; slot < items.size(); slot++) {
      if (MorphInventoryCapacity.isActiveInventorySlot(player, slot) && items.get(slot).isEmpty()) {
        cir.setReturnValue(slot);
        return;
      }
    }
    cir.setReturnValue(-1);
  }

  @Inject(method = "getSlotWithRemainingSpace", at = @At("HEAD"), cancellable = true)
  private void mobLife$getActiveSlotWithRemainingSpace(
      ItemStack newStack, CallbackInfoReturnable<Integer> cir) {
    if (!MorphInventoryCapacity.hasMobForm(player)) {
      return;
    }
    int selected = getSelectedSlot();
    if (mobLife$hasRemainingSpace(getItem(selected), newStack)) {
      cir.setReturnValue(selected);
      return;
    }
    if (MorphEquipment.isSlotActive(player, Inventory.SLOT_OFFHAND)
        && mobLife$hasRemainingSpace(getItem(Inventory.SLOT_OFFHAND), newStack)) {
      cir.setReturnValue(Inventory.SLOT_OFFHAND);
      return;
    }
    for (int slot = 0; slot < items.size(); slot++) {
      if (MorphInventoryCapacity.isActiveInventorySlot(player, slot)
          && mobLife$hasRemainingSpace(items.get(slot), newStack)) {
        cir.setReturnValue(slot);
        return;
      }
    }
    cir.setReturnValue(-1);
  }

  @Inject(method = "getSuitableHotbarSlot", at = @At("HEAD"), cancellable = true)
  private void mobLife$getSuitableActiveHotbarSlot(CallbackInfoReturnable<Integer> cir) {
    if (!MorphInventoryCapacity.hasMobForm(player)) {
      return;
    }
    int selected = getSelectedSlot();
    int size = MorphInventoryCapacity.hotbarSlots(player);
    for (int offset = 0; offset < size; offset++) {
      int slot = (selected + offset) % size;
      if (items.get(slot).isEmpty()) {
        cir.setReturnValue(slot);
        return;
      }
    }
    for (int offset = 0; offset < size; offset++) {
      int slot = (selected + offset) % size;
      if (!items.get(slot).isEnchanted()) {
        cir.setReturnValue(slot);
        return;
      }
    }
    cir.setReturnValue(Math.min(selected, size - 1));
  }

  @ModifyVariable(method = "setSelectedSlot", at = @At("HEAD"), argsOnly = true)
  private int mobLife$clampSelectedSlot(int selected) {
    return MorphInventoryCapacity.hasMobForm(player)
        ? Math.min(selected, MorphInventoryCapacity.hotbarSlots(player) - 1)
        : selected;
  }

  private boolean mobLife$hasRemainingSpace(ItemStack slotStack, ItemStack newStack) {
    return !slotStack.isEmpty()
        && ItemStack.isSameItemSameComponents(slotStack, newStack)
        && slotStack.isStackable()
        && slotStack.getCount() < ((Inventory) (Object) this).getMaxStackSize(slotStack);
  }
}
