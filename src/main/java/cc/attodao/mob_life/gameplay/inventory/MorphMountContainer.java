package cc.attodao.mob_life.gameplay.inventory;

import cc.attodao.mob_life.gameplay.targeting.MorphRelations;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class MorphMountContainer extends SimpleContainer {
  private static final int SADDLE_SLOT = 0;
  private static final int BODY_SLOT = 1;
  private static final int STORAGE_START = 2;

  private final Player target;
  private final MorphType morph;
  private final int rows;
  private boolean initialized;

  MorphMountContainer(Player target, MorphType morph) {
    super(morph.canEquipChest() ? 18 : 9);
    this.target = target;
    this.morph = morph;
    rows = getContainerSize() / 9;
    items.set(SADDLE_SLOT, target.getItemBySlot(EquipmentSlot.SADDLE));
    items.set(BODY_SLOT, target.getItemBySlot(EquipmentSlot.BODY));
    if (MorphChestInventory.isAvailable(target)) {
      MorphChestInventory chest = MorphChestInventory.get(target);
      for (int index = 0; index < MorphChestInventory.SIZE; index++) {
        items.set(STORAGE_START + index, chest.getItem(index));
      }
    }
    initialized = true;
  }

  int rows() {
    return rows;
  }

  @Override
  public boolean canPlaceItem(int slot, ItemStack stack) {
    if (slot == SADDLE_SLOT) {
      return MorphEquipment.mayPlaceSaddle(target, stack);
    }
    if (slot == BODY_SLOT) {
      return MorphEquipment.mayPlaceBody(target, stack);
    }
    return slot >= STORAGE_START
        && slot < STORAGE_START + MorphChestInventory.SIZE
        && items.get(BODY_SLOT).is(Items.CHEST);
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    super.setItem(
        slot, slot <= BODY_SLOT ? stack.copyWithCount(Math.min(1, stack.getCount())) : stack);
  }

  @Override
  public void setChanged() {
    if (!initialized) {
      return;
    }
    boolean removedChest =
        target.getItemBySlot(EquipmentSlot.BODY).is(Items.CHEST)
            && !items.get(BODY_SLOT).is(Items.CHEST);
    target.setItemSlot(EquipmentSlot.SADDLE, items.get(SADDLE_SLOT));
    target.setItemSlot(EquipmentSlot.BODY, items.get(BODY_SLOT));
    if (removedChest) {
      MorphChestInventory.get(target).returnTo(target);
      for (int index = 0; index < MorphChestInventory.SIZE; index++) {
        items.set(STORAGE_START + index, ItemStack.EMPTY);
      }
    } else if (items.get(BODY_SLOT).is(Items.CHEST)) {
      MorphChestInventory chest = MorphChestInventory.get(target);
      for (int index = 0; index < MorphChestInventory.SIZE; index++) {
        chest.setItem(index, items.get(STORAGE_START + index));
      }
    }
  }

  @Override
  public boolean stillValid(Player player) {
    return target.isAlive()
        && target.level() == player.level()
        && MorphRelations.morphOf(target) == morph
        && player.distanceToSqr(target) <= 64.0;
  }
}
