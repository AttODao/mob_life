package cc.attodao.mob_life.client.mixin.world;

import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.world.PendingWorldSelection;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen {
	@Unique
	private MorphType mobLife$selectedMorph = MorphType.PLAYER;

	@Unique
	private CycleButton<MorphType> mobLife$createMorphButton() {
		return CycleButton.builder(
						type -> Component.translatable(type.translationKey()),
						mobLife$selectedMorph
				)
				.withValues(List.of(MorphType.values()))
				.withTooltip(type -> Tooltip.create(
						Component.translatable("mob_life.create_world.morph.locked")
				))
				.create(
						0,
						0,
						210,
						20,
						Component.translatable("mob_life.create_world.morph"),
						(button, value) -> mobLife$selectedMorph = value
				);
	}

	protected CreateWorldScreenMixin(Component title) {
		super(title);
	}

	@ModifyArgs(
			method = "init",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;addTabs([Lnet/minecraft/client/gui/components/tabs/Tab;)Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;"
			)
	)
	private void mobLife$addMorphSelectionToGameSettings(Args args) {
		Tab[] tabs = args.get(0);
		if (!(tabs[0] instanceof GridLayoutTab gameTab)) {
			return;
		}

		GridLayout layout = ((GridLayoutTabAccessor) gameTab).mobLife$getLayout();
		int lastRow = ((GridLayoutAccessor) layout).mobLife$getChildren().size();
		layout.addChild(mobLife$createMorphButton(), lastRow, 0);
	}

	@Inject(method = "onCreate", at = @At("HEAD"))
	private void mobLife$rememberSelection(CallbackInfo ci) {
		PendingWorldSelection.setForNextWorld(mobLife$selectedMorph);
	}
}
