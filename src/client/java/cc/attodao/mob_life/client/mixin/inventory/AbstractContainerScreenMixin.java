package cc.attodao.mob_life.client.mixin.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Inject(
			method = "extractContents",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"
			)
	)
	private void mobLife$shadeInactiveInventorySlots(
			GuiGraphicsExtractor graphics,
			int mouseX,
			int mouseY,
			float partialTick,
			CallbackInfo ci
	) {
		AbstractContainerScreen<?> screen =
				(AbstractContainerScreen<?>) (Object) this;
		for (Slot slot : screen.getMenu().slots) {
			if (!slot.isActive()) {
				graphics.fill(
						slot.x - 1,
						slot.y - 1,
						slot.x + 17,
						slot.y + 17,
						0xC0202020
				);
			}
		}
	}
}
