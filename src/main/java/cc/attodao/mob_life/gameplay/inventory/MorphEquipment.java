package cc.attodao.mob_life.gameplay.inventory;

import cc.attodao.mob_life.config.ServerMobLifeConfig;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

public final class MorphEquipment {

  private MorphEquipment() {}

  public static boolean isSlotActive(Player player, int inventorySlot) {
    MorphType morph = morph(player);
    if (morph == null || morph.isPlayer()) {
      return inventorySlot != Inventory.SLOT_BODY_ARMOR && inventorySlot != Inventory.SLOT_SADDLE;
    }
    return switch (inventorySlot) {
      case Inventory.SLOT_OFFHAND -> ServerMobLifeConfig.offhandLimitEnabled();
      case 36, 37, 38, 39 -> false;
      case Inventory.SLOT_BODY_ARMOR -> morph.canEquipAnimalArmor() || morph.canEquipChest();
      case Inventory.SLOT_SADDLE -> morph.canEquipSaddle();
      default -> true;
    };
  }

  public static boolean mayPlaceBody(Player player, ItemStack stack) {
    MorphType morph = morph(player);
    if (morph.canEquipChest() && stack.is(Items.CHEST)) {
      return true;
    }
    Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
    if (equippable == null
        || equippable.slot() != EquipmentSlot.BODY
        || !equippable.canBeEquippedBy(morph.entityType().builtInRegistryHolder())) {
      return false;
    }
    return (morph.canEquipHorseArmor()
            && equippable.canBeEquippedBy(EntityTypes.HORSE.builtInRegistryHolder()))
        || (morph.canEquipWolfArmor()
            && equippable.canBeEquippedBy(EntityTypes.WOLF.builtInRegistryHolder()));
  }

  public static boolean mayPlaceSaddle(Player player, ItemStack stack) {
    MorphType morph = morph(player);
    return (morph.canEquipSaddle() && isEquippableForMorph(morph, stack, EquipmentSlot.SADDLE));
  }

  public static void removeUnsupportedEquipment(Player player) {
    MorphType morph = morph(player);
    if (!morph.canEquipChest()) {
      MorphChestInventory.get(player).returnTo(player);
    }
    if (morph.isPlayer()) {
      return;
    }

    if (!isSlotActive(player, Inventory.SLOT_OFFHAND)) {
      moveToInventory(player, EquipmentSlot.OFFHAND);
    }
    moveToInventory(player, EquipmentSlot.HEAD);
    moveToInventory(player, EquipmentSlot.CHEST);
    moveToInventory(player, EquipmentSlot.LEGS);
    moveToInventory(player, EquipmentSlot.FEET);
    if (!mayPlaceBody(player, player.getItemBySlot(EquipmentSlot.BODY))) {
      moveToInventory(player, EquipmentSlot.BODY);
    }
    if (!mayPlaceSaddle(player, player.getItemBySlot(EquipmentSlot.SADDLE))) {
      moveToInventory(player, EquipmentSlot.SADDLE);
    }
  }

  private static boolean isEquippableForMorph(
      MorphType morph, ItemStack stack, EquipmentSlot slot) {
    Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
    return (equippable != null
        && equippable.slot() == slot
        && equippable.canBeEquippedBy(morph.entityType().builtInRegistryHolder()));
  }

  public static MorphType morph(Player player) {
    return ((MorphInventoryCapacityHolder) player).mobLife$getMorph();
  }

  private static void moveToInventory(Player player, EquipmentSlot equipmentSlot) {
    ItemStack stack = player.getItemBySlot(equipmentSlot);
    if (stack.isEmpty()) {
      return;
    }

    player.setItemSlot(equipmentSlot, ItemStack.EMPTY);
    player.getInventory().placeItemBackInInventory(stack);
  }
}
