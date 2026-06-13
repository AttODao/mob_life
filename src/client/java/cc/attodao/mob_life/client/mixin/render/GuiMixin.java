package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.render.MorphJumpBarRenderer;
import cc.attodao.mob_life.client.render.SizedHotbarRenderer;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class GuiMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Nullable
	protected abstract Player getCameraPlayer();

	@Shadow
	protected abstract void extractSlot(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			DeltaTracker deltaTracker,
			Player player,
			ItemStack stack,
			int seed
	);

	@Redirect(
			method = "extractHotbarAndDecorations",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/Gui;extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
			)
	)
	private void mobLife$renderSizedHotbar(
			Gui gui,
			GuiGraphicsExtractor graphics,
			DeltaTracker deltaTracker
	) {
		Player player = getCameraPlayer();
		if (player == null) {
			return;
		}

		SizedHotbarRenderer.render(
				graphics,
				deltaTracker,
				minecraft,
				player,
				this::extractSlot
		);
	}

	@ModifyConstant(
			method = "extractFood",
			constant = @Constant(intValue = 10)
	)
	private int mobLife$renderMaximumFoodIcons(
			int vanillaIcons,
			GuiGraphicsExtractor graphics,
			Player player,
			int yLineBase,
			int xRight
	) {
		return (MorphFoodCapacity.maxFood(player) + 1) / 2;
	}

	@Redirect(
			method = "extractHotbarAndDecorations",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
			)
	)
	private void mobLife$renderChargedJumpBar(
			ContextualBarRenderer renderer,
			GuiGraphicsExtractor graphics,
			DeltaTracker deltaTracker
	) {
		if (ClientMorphState.shouldShowChargedJumpBar()) {
			MorphJumpBarRenderer.render(graphics);
		} else {
			renderer.extractBackground(graphics, deltaTracker);
		}
	}
}
