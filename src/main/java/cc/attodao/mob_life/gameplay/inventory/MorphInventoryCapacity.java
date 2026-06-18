package cc.attodao.mob_life.gameplay.inventory;

import cc.attodao.mob_life.config.MobLifeConfig;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.morph.MorphBodyScale;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record MorphInventoryCapacity(int hotbarSlots, int inventorySlots) {
  public static final int MAX_HOTBAR_SLOTS = 9;
  public static final int MAX_INVENTORY_SLOTS = 27;
  private static final float PLAYER_HEIGHT = EntityType.PLAYER.getDimensions().height();

  public static MorphInventoryCapacity forMorph(MorphType morph) {
    float height =
        morph == null || morph.isPlayer()
            ? PLAYER_HEIGHT
            : morph.entityType().getDimensions().height();
    return forMorph(morph, height);
  }

  public static MorphInventoryCapacity forMorph(MorphType morph, float morphHeight) {
    return forMorph(morph, morphHeight, false);
  }

  public static MorphInventoryCapacity forMorph(
      MorphType morph, float morphHeight, boolean hasChest) {
    if (morph == null || morph.isPlayer()) {
      return new MorphInventoryCapacity(MAX_HOTBAR_SLOTS, MAX_INVENTORY_SLOTS);
    }
    MorphConfig.Inventory config = MorphConfigManager.get(morph).inventory();
    float capacityRatio =
        MorphBodyScale.relativeTo(morphHeight, morph.entityType().getDimensions().height());
    int hotbarSlots =
        MobLifeConfig.hotbarLimitEnabled()
            ? scaledSlots(config.hotbarSlots(), capacityRatio)
            : MAX_HOTBAR_SLOTS;
    int inventorySlots =
        MobLifeConfig.inventorySlotLimitEnabled()
            ? scaledSlots(
                config.inventorySlots() + (hasChest ? config.chestBonusSlots() : 0), capacityRatio)
            : MAX_INVENTORY_SLOTS;
    return new MorphInventoryCapacity(
        Math.min(MAX_HOTBAR_SLOTS, hotbarSlots), Math.min(MAX_INVENTORY_SLOTS, inventorySlots));
  }

  private static int scaledSlots(int maximum, float ratio) {
    return Math.max(1, (int) Math.floor(maximum * ratio + 1.0E-4F));
  }

  public static int hotbarSlots(Player player) {
    return ((MorphInventoryCapacityHolder) player).mobLife$getHotbarSlots();
  }

  public static int inventorySlots(Player player) {
    return ((MorphInventoryCapacityHolder) player).mobLife$getInventorySlots();
  }

  public static boolean isActiveInventorySlot(Player player, int slot) {
    if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
      return true;
    }
    if (slot < Inventory.SELECTION_SIZE) {
      return slot < hotbarSlots(player);
    }
    return slot - Inventory.SELECTION_SIZE < inventorySlots(player);
  }

  public static void apply(Player player, MorphType morph) {
    float height =
        morph == null || morph.isPlayer()
            ? PLAYER_HEIGHT
            : morph.entityType().getDimensions().height();
    apply(player, morph, height);
  }

  public static void apply(Player player, MorphType morph, float morphHeight) {
    apply(player, morph, morphHeight, false);
  }

  public static void apply(Player player, MorphType morph, float morphHeight, boolean hasChest) {
    MorphInventoryCapacity capacity = forMorph(morph, morphHeight, hasChest);
    MorphInventoryCapacityHolder holder = (MorphInventoryCapacityHolder) player;
    holder.mobLife$setInventoryCapacity(morph, capacity.hotbarSlots(), capacity.inventorySlots());

    Inventory inventory = player.getInventory();
    if (inventory.getSelectedSlot() >= capacity.hotbarSlots()) {
      inventory.setSelectedSlot(0);
    }
    if (!player.level().isClientSide()) {
      moveInactiveItems(inventory, capacity);
    }
  }

  private static void moveInactiveItems(Inventory inventory, MorphInventoryCapacity capacity) {
    for (int slot = capacity.hotbarSlots(); slot < Inventory.SELECTION_SIZE; slot++) {
      moveItemToActiveSlot(inventory, slot);
    }
    for (int slot = Inventory.SELECTION_SIZE + capacity.inventorySlots();
        slot < Inventory.INVENTORY_SIZE;
        slot++) {
      moveItemToActiveSlot(inventory, slot);
    }
  }

  private static void moveItemToActiveSlot(Inventory inventory, int slot) {
    ItemStack stack = inventory.removeItemNoUpdate(slot);
    if (!stack.isEmpty()) {
      inventory.placeItemBackInInventory(stack);
    }
  }
}
