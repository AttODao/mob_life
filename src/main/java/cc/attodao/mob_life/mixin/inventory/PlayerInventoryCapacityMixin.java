package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacityHolder;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerInventoryCapacityMixin
		implements MorphInventoryCapacityHolder {
	@Unique
	private MorphType mobLife$morph = MorphType.PLAYER;
	@Unique
	private int mobLife$hotbarSlots =
			MorphInventoryCapacity.MAX_HOTBAR_SLOTS;
	@Unique
	private int mobLife$inventorySlots =
			MorphInventoryCapacity.MAX_INVENTORY_SLOTS;

	@Override
	public MorphType mobLife$getMorph() {
		return mobLife$morph;
	}

	@Override
	public int mobLife$getHotbarSlots() {
		return mobLife$hotbarSlots;
	}

	@Override
	public int mobLife$getInventorySlots() {
		return mobLife$inventorySlots;
	}

	@Override
	public void mobLife$setInventoryCapacity(
			MorphType morph,
			int hotbarSlots,
			int inventorySlots
	) {
		mobLife$morph = morph;
		mobLife$hotbarSlots = hotbarSlots;
		mobLife$inventorySlots = inventorySlots;
	}
}
