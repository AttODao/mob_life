package cc.attodao.mob_life.gameplay.inventory;

import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MorphChestInventory extends SimpleContainer {
  public static final int SIZE = 15;
  private static final String SAVE_KEY = "MobLifeChestInventory";

  public MorphChestInventory() {
    super(SIZE);
  }

  public static MorphChestInventory get(Player player) {
    return ((MorphChestInventoryHolder) player).mobLife$getMorphChestInventory();
  }

  public static boolean isAvailable(Player player) {
    return MorphEquipment.morph(player).canEquipChest()
        && player.getItemBySlot(EquipmentSlot.BODY).is(Items.CHEST);
  }

  public void load(ValueInput input) {
    clearContent();
    for (ItemStackWithSlot entry : input.listOrEmpty(SAVE_KEY, ItemStackWithSlot.CODEC)) {
      if (entry.isValidInContainer(SIZE)) {
        setItem(entry.slot(), entry.stack());
      }
    }
  }

  public void save(ValueOutput output) {
    ValueOutput.TypedOutputList<ItemStackWithSlot> entries =
        output.list(SAVE_KEY, ItemStackWithSlot.CODEC);
    for (int slot = 0; slot < SIZE; slot++) {
      ItemStack stack = getItem(slot);
      if (!stack.isEmpty()) {
        entries.add(new ItemStackWithSlot(slot, stack));
      }
    }
  }

  public void returnTo(Player player) {
    if (player.level().isClientSide()) {
      return;
    }
    for (int slot = 0; slot < SIZE; slot++) {
      ItemStack stack = removeItemNoUpdate(slot);
      if (!stack.isEmpty()) {
        player.getInventory().placeItemBackInInventory(stack);
      }
    }
  }

  public void dropAll(Player player) {
    for (int slot = 0; slot < SIZE; slot++) {
      ItemStack stack = removeItemNoUpdate(slot);
      if (!stack.isEmpty()) {
        player.drop(stack, true, false);
      }
    }
  }

  public void copyFrom(MorphChestInventory source) {
    clearContent();
    for (int slot = 0; slot < SIZE; slot++) {
      setItem(slot, source.getItem(slot).copy());
    }
  }
}
