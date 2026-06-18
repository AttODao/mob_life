package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.render.MorphJumpBarRenderer;
import cc.attodao.mob_life.client.render.SizedHotbarRenderer;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.config.MobLifeConfig;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
  @Shadow @Final private Minecraft minecraft;

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
      int seed);

  @Redirect(
      method = "extractHotbarAndDecorations",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/Gui;extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
  private void mobLife$renderSizedHotbar(
      Gui gui, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
    Player player = getCameraPlayer();
    if (player == null) {
      return;
    }

    SizedHotbarRenderer.render(graphics, deltaTracker, minecraft, player, this::extractSlot);
  }

  @ModifyConstant(method = "extractFood", constant = @Constant(intValue = 10))
  private int mobLife$renderMaximumFoodIcons(
      int vanillaIcons, GuiGraphicsExtractor graphics, Player player, int yLineBase, int xRight) {
    return (MorphFoodCapacity.maxFood(player) + 1) / 2;
  }

  @Redirect(
      method = "extractHotbarAndDecorations",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
  private void mobLife$renderChargedJumpBar(
      ContextualBarRenderer renderer, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
    if (ClientMorphState.shouldShowChargedJumpBar()) {
      MorphJumpBarRenderer.render(graphics);
    } else {
      renderer.extractBackground(graphics, deltaTracker);
    }
  }

  @Redirect(
      method = "extractHotbarAndDecorations",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
  private void mobLife$moveExperienceLevelAboveAwkwardness(
      GuiGraphicsExtractor graphics, Font font, int experienceLevel) {
    if (ClientMorphState.morph() == null) {
      ContextualBarRenderer.extractExperienceLevel(graphics, font, experienceLevel);
      return;
    }

    Component text = Component.translatable("gui.experience.level", experienceLevel);
    int x = (graphics.guiWidth() - font.width(text)) / 2;
    int y = graphics.guiHeight() - 40;
    graphics.text(font, text, x + 1, y, 0xFF000000, false);
    graphics.text(font, text, x - 1, y, 0xFF000000, false);
    graphics.text(font, text, x, y + 1, 0xFF000000, false);
    graphics.text(font, text, x, y - 1, 0xFF000000, false);
    graphics.text(font, text, x, y, 0xFF80FF20, false);
  }

  @Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
  private void mobLife$renderAwkwardness(
      GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    if (ClientMorphState.morph() == null) {
      return;
    }

    float awkwardness = ClientMorphState.awkwardness();
    mobLife$drawAwkwardnessIndicator(graphics, awkwardness);
    if (!MobLifeConfig.showAwkwardnessDebug()) {
      return;
    }

    int color = awkwardness >= 90.0F ? 0xFFFF5555 : awkwardness >= 70.0F ? 0xFFFFAA00 : 0xFFFFFFFF;
    graphics.text(
        minecraft.font,
        Component.translatable("mob_life.hud.awkwardness", Math.round(awkwardness)),
        6,
        6,
        color);
  }

  private static void mobLife$drawAwkwardnessIndicator(
      GuiGraphicsExtractor graphics, float awkwardness) {
    float ratio = Math.clamp(awkwardness / 100.0F, 0.0F, 1.0F);
    int red = ratio <= 0.5F ? Math.round(ratio * 2.0F * 255.0F) : 255;
    int green = ratio <= 0.5F ? 255 : Math.round((1.0F - ratio) * 2.0F * 255.0F);
    int color = 0xFF000000 | red << 16 | green << 8;
    int centerX = graphics.guiWidth() / 2;
    int top = graphics.guiHeight() - 52;

    graphics.fill(centerX - 2, top, centerX + 3, top + 1, 0xFF000000);
    graphics.fill(centerX - 3, top + 1, centerX + 4, top + 2, 0xFF000000);
    graphics.fill(centerX - 4, top + 2, centerX + 5, top + 7, 0xFF000000);
    graphics.fill(centerX - 3, top + 7, centerX + 4, top + 8, 0xFF000000);
    graphics.fill(centerX - 2, top + 8, centerX + 3, top + 9, 0xFF000000);

    graphics.fill(centerX - 2, top + 1, centerX + 3, top + 2, color);
    graphics.fill(centerX - 3, top + 2, centerX + 4, top + 7, color);
    graphics.fill(centerX - 2, top + 7, centerX + 3, top + 8, color);
  }
}
