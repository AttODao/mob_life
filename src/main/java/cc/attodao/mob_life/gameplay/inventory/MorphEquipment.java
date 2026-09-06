package cc.attodao.mob_life.gameplay.inventory;

import cc.attodao.mob_life.config.ServerMobLifeConfig;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
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
        || !equippable.canBeEquippedBy(entityTypeHolder(morph.entityType()))) {
      return false;
    }
    return (morph.canEquipHorseArmor()
            && equippable.canBeEquippedBy(entityTypeHolder(EntityTypes.HORSE)))
        || (morph.canEquipWolfArmor()
            && equippable.canBeEquippedBy(entityTypeHolder(EntityTypes.WOLF)));
  }

  public static boolean mayPlaceSaddle(Player player, ItemStack stack) {
    MorphType morph = morph(player);
    return (morph.canEquipSaddle() && isEquippableForMorph(morph, stack, EquipmentSlot.SADDLE));
  }

  public static void removeUnsupportedEquipment(Player player) {
    MorphType morph = morph(player);
    if (!morph.canEquipChest()) {
      MorphChestInventory.get(player).dropAll(player);
    }
    if (morph.isPlayer()) {
      dropAtFeet(player, EquipmentSlot.BODY);
      dropAtFeet(player, EquipmentSlot.SADDLE);
      return;
    }

    if (!isSlotActive(player, Inventory.SLOT_OFFHAND)) {
      dropAtFeet(player, EquipmentSlot.OFFHAND);
    }
    dropAtFeet(player, EquipmentSlot.HEAD);
    dropAtFeet(player, EquipmentSlot.CHEST);
    dropAtFeet(player, EquipmentSlot.LEGS);
    dropAtFeet(player, EquipmentSlot.FEET);
    if (!mayPlaceBody(player, player.getItemBySlot(EquipmentSlot.BODY))) {
      dropAtFeet(player, EquipmentSlot.BODY);
    }
    if (!mayPlaceSaddle(player, player.getItemBySlot(EquipmentSlot.SADDLE))) {
      dropAtFeet(player, EquipmentSlot.SADDLE);
    }
  }

  private static boolean isEquippableForMorph(
      MorphType morph, ItemStack stack, EquipmentSlot slot) {
    Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
    return (equippable != null
        && equippable.slot() == slot
        && equippable.canBeEquippedBy(entityTypeHolder(morph.entityType())));
  }

  private static Holder<EntityType<?>> entityTypeHolder(EntityType<?> entityType) {
    return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entityType);
  }

  public static MorphType morph(Player player) {
    return ((MorphInventoryCapacityHolder) player).mobLife$getMorph();
  }

  private static void dropAtFeet(Player player, EquipmentSlot equipmentSlot) {
    ItemStack stack = player.getItemBySlot(equipmentSlot);
    if (stack.isEmpty()) {
      return;
    }

    player.setItemSlot(equipmentSlot, ItemStack.EMPTY);
    player.drop(stack, false, false);
  }
}
