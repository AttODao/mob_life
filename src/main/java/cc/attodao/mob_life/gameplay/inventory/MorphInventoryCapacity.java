package cc.attodao.mob_life.gameplay.inventory;

import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record MorphInventoryCapacity(int hotbarSlots, int inventorySlots) {
	public static final int MAX_HOTBAR_SLOTS = 9;
	public static final int MAX_INVENTORY_SLOTS = 27;
	private static final float PLAYER_HEIGHT =
			EntityType.PLAYER.getDimensions().height();

	public static MorphInventoryCapacity forMorph(MorphType morph) {
		if (morph == null || morph.isPlayer()) {
			return new MorphInventoryCapacity(
					MAX_HOTBAR_SLOTS,
					MAX_INVENTORY_SLOTS
			);
		}

		float heightRatio =
				morph.entityType().getDimensions().height() / PLAYER_HEIGHT;
		float capacityRatio = heightRatio <= 0.5F
				? 1.0F / 3.0F
				: heightRatio;
		return new MorphInventoryCapacity(
				scaledSlots(MAX_HOTBAR_SLOTS, capacityRatio),
				scaledSlots(MAX_INVENTORY_SLOTS, capacityRatio)
		);
	}

	private static int scaledSlots(int maximum, float ratio) {
		return Math.max(1, (int) Math.floor(maximum * ratio + 1.0E-4F));
	}

	public static int hotbarSlots(Player player) {
		return ((MorphInventoryCapacityHolder) player)
				.mobLife$getHotbarSlots();
	}

	public static int inventorySlots(Player player) {
		return ((MorphInventoryCapacityHolder) player)
				.mobLife$getInventorySlots();
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
		MorphInventoryCapacity capacity = forMorph(morph);
		MorphInventoryCapacityHolder holder =
				(MorphInventoryCapacityHolder) player;
		holder.mobLife$setInventoryCapacity(
				morph,
				capacity.hotbarSlots(),
				capacity.inventorySlots()
		);

		Inventory inventory = player.getInventory();
		if (inventory.getSelectedSlot() >= capacity.hotbarSlots()) {
			inventory.setSelectedSlot(0);
		}
		if (!player.level().isClientSide()) {
			moveInactiveItems(inventory, capacity);
		}
	}

	private static void moveInactiveItems(
			Inventory inventory,
			MorphInventoryCapacity capacity
	) {
		for (
				int slot = capacity.hotbarSlots();
				slot < Inventory.SELECTION_SIZE;
				slot++
		) {
			moveItemToActiveSlot(inventory, slot);
		}
		for (
				int slot =
						Inventory.SELECTION_SIZE + capacity.inventorySlots();
				slot < Inventory.INVENTORY_SIZE;
				slot++
		) {
			moveItemToActiveSlot(inventory, slot);
		}
	}

	private static void moveItemToActiveSlot(
			Inventory inventory,
			int slot
	) {
		ItemStack stack = inventory.removeItemNoUpdate(slot);
		if (!stack.isEmpty()) {
			inventory.placeItemBackInInventory(stack);
		}
	}
}
