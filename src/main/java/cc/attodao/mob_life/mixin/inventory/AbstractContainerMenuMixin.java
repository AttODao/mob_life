package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void mobLife$preventSwapToInactiveHotbarSlot(
			int slotIndex,
			int buttonNum,
			ContainerInput containerInput,
			Player player,
			CallbackInfo ci
	) {
		if (
				containerInput == ContainerInput.SWAP
						&& buttonNum >= MorphInventoryCapacity.hotbarSlots(player)
						&& buttonNum < MorphInventoryCapacity.MAX_HOTBAR_SLOTS
		) {
			ci.cancel();
		}
	}
}
