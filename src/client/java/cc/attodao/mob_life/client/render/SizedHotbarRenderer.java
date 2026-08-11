package cc.attodao.mob_life.client.render;

import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SizedHotbarRenderer {
  private static final Identifier HOTBAR = Identifier.withDefaultNamespace("hud/hotbar");
  private static final Identifier SELECTION =
      Identifier.withDefaultNamespace("hud/hotbar_selection");
  private static final Identifier OFFHAND_LEFT =
      Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
  private static final Identifier OFFHAND_RIGHT =
      Identifier.withDefaultNamespace("hud/hotbar_offhand_right");
  private static final Identifier ATTACK_BACKGROUND =
      Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_background");
  private static final Identifier ATTACK_PROGRESS =
      Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_progress");

  private SizedHotbarRenderer() {}

  public static void render(
      GuiGraphicsExtractor graphics,
      DeltaTracker deltaTracker,
      Minecraft minecraft,
      Player player,
      SlotRenderer slotRenderer) {
    int slotCount = MorphInventoryCapacity.hotbarSlots(player);
    int width = slotCount * 20 + 2;
    int left = (graphics.guiWidth() - width) / 2;
    int top = graphics.guiHeight() - 22;

    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 0, 0, left, top, width, 22);
    graphics.blitSprite(
        RenderPipelines.GUI_TEXTURED,
        SELECTION,
        left - 1 + player.getInventory().getSelectedSlot() * 20,
        top - 1,
        24,
        23);

    int seed = 1;
    int itemY = graphics.guiHeight() - 19;
    for (int slot = 0; slot < slotCount; slot++) {
      slotRenderer.render(
          graphics,
          left + 3 + slot * 20,
          itemY,
          deltaTracker,
          player,
          player.getInventory().getItem(slot),
          seed++);
    }

    ItemStack offhand = player.getOffhandItem();
    HumanoidArm offhandArm = player.getMainArm().getOpposite();
    if (!offhand.isEmpty()) {
      renderOffhand(
          graphics,
          deltaTracker,
          player,
          slotRenderer,
          offhand,
          offhandArm,
          left,
          top,
          width,
          itemY,
          seed);
    }
    renderAttackIndicator(graphics, minecraft, player, offhandArm, left, width);
  }

  public static void renderLocked(
      GuiGraphicsExtractor graphics,
      DeltaTracker deltaTracker,
      Player player,
      SlotRenderer slotRenderer) {
    int slotCount = MorphInventoryCapacity.hotbarSlots(player);
    int width = slotCount * 20 + 2;
    int left = (graphics.guiWidth() - width) / 2;
    int top = graphics.guiHeight() - 22;

    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 0, 0, left, top, width, 22);
    int seed = 1;
    int itemY = graphics.guiHeight() - 19;
    for (int slot = 0; slot < slotCount; slot++) {
      slotRenderer.render(
          graphics,
          left + 3 + slot * 20,
          itemY,
          deltaTracker,
          player,
          player.getInventory().getItem(slot),
          seed++);
    }

    // Keep the stored hotbar visible, but make its unavailable state unambiguous.
    graphics.fill(left + 2, top + 2, left + width - 2, top + 20, 0x9C0A0E12);
    renderLock(graphics, left + width / 2, top + 11);
  }

  private static void renderLock(GuiGraphicsExtractor graphics, int centerX, int centerY) {
    int outline = 0xFF10171C;
    int metal = 0xFFC3CCD0;
    int shadow = 0xFF677278;

    graphics.fill(centerX - 4, centerY - 6, centerX + 4, centerY + 1, outline);
    graphics.fill(centerX - 3, centerY - 5, centerX + 3, centerY + 1, metal);
    graphics.fill(centerX - 1, centerY - 4, centerX + 1, centerY + 1, outline);
    graphics.fill(centerX - 5, centerY, centerX + 5, centerY + 7, outline);
    graphics.fill(centerX - 4, centerY + 1, centerX + 4, centerY + 6, metal);
    graphics.fill(centerX - 3, centerY + 5, centerX + 4, centerY + 6, shadow);
    graphics.fill(centerX - 1, centerY + 2, centerX + 1, centerY + 5, outline);
  }

  private static void renderOffhand(
      GuiGraphicsExtractor graphics,
      DeltaTracker deltaTracker,
      Player player,
      SlotRenderer slotRenderer,
      ItemStack offhand,
      HumanoidArm offhandArm,
      int left,
      int top,
      int width,
      int itemY,
      int seed) {
    boolean leftHand = offhandArm == HumanoidArm.LEFT;
    graphics.blitSprite(
        RenderPipelines.GUI_TEXTURED,
        leftHand ? OFFHAND_LEFT : OFFHAND_RIGHT,
        leftHand ? left - 29 : left + width,
        top - 1,
        29,
        24);
    slotRenderer.render(
        graphics,
        leftHand ? left - 26 : left + width + 10,
        itemY,
        deltaTracker,
        player,
        offhand,
        seed);
  }

  private static void renderAttackIndicator(
      GuiGraphicsExtractor graphics,
      Minecraft minecraft,
      Player player,
      HumanoidArm offhandArm,
      int left,
      int width) {
    if (minecraft.options.attackIndicator().get() != AttackIndicatorStatus.HOTBAR) {
      return;
    }

    float scale = player.getAttackStrengthScale(0.0F);
    if (scale >= 1.0F) {
      return;
    }

    int x = offhandArm == HumanoidArm.RIGHT ? left - 22 : left + width + 6;
    int y = graphics.guiHeight() - 20;
    int progress = (int) (scale * 19.0F);
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ATTACK_BACKGROUND, x, y, 18, 18);
    graphics.blitSprite(
        RenderPipelines.GUI_TEXTURED,
        ATTACK_PROGRESS,
        18,
        18,
        0,
        18 - progress,
        x,
        y + 18 - progress,
        18,
        progress);
  }

  @FunctionalInterface
  public interface SlotRenderer {
    void render(
        GuiGraphicsExtractor graphics,
        int x,
        int y,
        DeltaTracker deltaTracker,
        Player player,
        ItemStack stack,
        int seed);
  }
}
