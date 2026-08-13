package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.config.ClientMobLifeConfig;
import cc.attodao.mob_life.client.render.AwkwardnessColor;
import cc.attodao.mob_life.client.render.MorphJumpBarRenderer;
import cc.attodao.mob_life.client.render.SizedHotbarRenderer;
import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
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

@Mixin(Hud.class)
public abstract class GuiMixin {
  private static final Identifier EXPERIENCE_ORB =
      Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");
  private static final int EXPERIENCE_ORB_TEXTURE_SIZE = 64;
  private static final int EXPERIENCE_ORB_FRAME_SIZE = 16;
  private static final int AWKWARDNESS_ORB_SIZE = 13;

  @Shadow @Final private Minecraft minecraft;

  @Shadow private int tickCount;

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

  @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
  private void mobLife$hideInstinctCrosshair(
      GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    if (ClientInstinctState.enabled()) {
      ci.cancel();
    }
  }

  @Redirect(
      method = "extractHotbarAndDecorations",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/Hud;extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
  private void mobLife$renderSizedHotbar(
      Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
    Player player = getCameraPlayer();
    if (player == null) {
      return;
    }
    if (ClientInstinctState.enabled()) {
      SizedHotbarRenderer.renderLocked(
          graphics,
          deltaTracker,
          player,
          this::extractSlot,
          ClientInstinctState.instinctLevelRatio());
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
                  "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
  private void mobLife$renderChargedJumpBar(
      ContextualBar renderer, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
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
                  "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
  private void mobLife$moveExperienceLevelAboveAwkwardness(
      GuiGraphicsExtractor graphics, Font font, int experienceLevel) {
    if (ClientMorphState.morph() == null) {
      ContextualBar.extractExperienceLevel(graphics, font, experienceLevel);
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
    mobLife$drawAwkwardnessIndicator(
        graphics, awkwardness, tickCount + deltaTracker.getGameTimeDeltaPartialTick(false));
    if (!ClientMobLifeConfig.showAwkwardnessDebug()) {
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
      GuiGraphicsExtractor graphics, float awkwardness, float age) {
    float ratio = Mth.clamp(awkwardness / 100.0F, 0.0F, 1.0F);
    float pulse = (Mth.sin(age / 2.0F) + 1.0F) * 0.5F;
    int alpha = 0xD0 + Math.round(pulse * 0x2F);
    int color = AwkwardnessColor.argb(awkwardness, alpha);
    int centerX = graphics.guiWidth() / 2;
    int left = centerX - AWKWARDNESS_ORB_SIZE / 2;
    int top = graphics.guiHeight() - 57;
    int glow = AwkwardnessColor.argb(awkwardness, 0x26 + Math.round(pulse * 0x1A));

    graphics.fill(
        left + 3, top - 1, left + AWKWARDNESS_ORB_SIZE - 3, top + AWKWARDNESS_ORB_SIZE + 1, glow);
    graphics.fill(
        left + 1, top + 1, left + AWKWARDNESS_ORB_SIZE - 1, top + AWKWARDNESS_ORB_SIZE - 1, glow);

    int frame = Mth.clamp(Math.round(ratio * 10.0F), 0, 10);
    float u = (frame % 4) * EXPERIENCE_ORB_FRAME_SIZE;
    float v = (frame / 4) * EXPERIENCE_ORB_FRAME_SIZE;
    graphics.blit(
        RenderPipelines.GUI_TEXTURED,
        EXPERIENCE_ORB,
        left,
        top,
        u,
        v,
        AWKWARDNESS_ORB_SIZE,
        AWKWARDNESS_ORB_SIZE,
        EXPERIENCE_ORB_FRAME_SIZE,
        EXPERIENCE_ORB_FRAME_SIZE,
        EXPERIENCE_ORB_TEXTURE_SIZE,
        EXPERIENCE_ORB_TEXTURE_SIZE,
        color);
  }
}
