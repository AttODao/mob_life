package cc.attodao.mob_life.client.mixin.world;

import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.world.PendingWorldSelection;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
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
	private EditBox mobLife$nbtInput;

	@Unique
	private CompoundTag mobLife$parsedNbt = new CompoundTag();

	@Unique
	private boolean mobLife$nbtValid = true;

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
						(button, value) -> {
							mobLife$selectedMorph = value;
							mobLife$nbtInput.setEditable(!value.isPlayer());
						}
				);
	}

	@Unique
	private EditBox mobLife$createNbtInput() {
		EditBox input = new EditBox(
				font,
				0,
				0,
				210,
				20,
				Component.translatable("mob_life.create_world.nbt")
		);
		input.setMaxLength(2048);
		input.setHint(Component.translatable(
				"mob_life.create_world.nbt.hint"
		));
		input.setEditable(!mobLife$selectedMorph.isPlayer());
		input.setResponder(this::mobLife$parseNbt);
		input.setTooltip(Tooltip.create(Component.translatable(
				"mob_life.create_world.nbt.tooltip"
		)));
		return input;
	}

	@Unique
	private void mobLife$parseNbt(String value) {
		if (value.isBlank()) {
			mobLife$parsedNbt = new CompoundTag();
			mobLife$nbtValid = true;
			mobLife$nbtInput.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
			mobLife$nbtInput.setTooltip(Tooltip.create(Component.translatable(
					"mob_life.create_world.nbt.tooltip"
			)));
			return;
		}

		try {
			mobLife$parsedNbt = TagParser.parseCompoundFully(value);
			mobLife$nbtValid = true;
			mobLife$nbtInput.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
			mobLife$nbtInput.setTooltip(Tooltip.create(Component.translatable(
					"mob_life.create_world.nbt.tooltip"
			)));
		} catch (CommandSyntaxException exception) {
			mobLife$nbtValid = false;
			mobLife$nbtInput.setTextColor(0xFFFF5555);
			mobLife$nbtInput.setTooltip(Tooltip.create(
					Component.translatable(
							"mob_life.create_world.nbt.invalid",
							exception.getRawMessage()
					)
			));
		}
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
		mobLife$nbtInput = mobLife$createNbtInput();
		layout.addChild(mobLife$createMorphButton(), lastRow, 0);
		layout.addChild(mobLife$nbtInput, lastRow + 1, 0);
	}

	@Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
	private void mobLife$rememberSelection(CallbackInfo ci) {
		if (!mobLife$selectedMorph.isPlayer() && !mobLife$nbtValid) {
			mobLife$nbtInput.setFocused(true);
			ci.cancel();
			return;
		}

		PendingWorldSelection.setForNextWorld(new MorphDefinition(
				mobLife$selectedMorph,
				mobLife$selectedMorph.isPlayer()
						? new CompoundTag()
						: mobLife$parsedNbt
		));
	}
}
