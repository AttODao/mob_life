package cc.attodao.mob_life.client.mixin.inventory;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.gameplay.inventory.MorphChestInventory;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.morph.MorphType;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
  private static final int IMAGE_WIDTH = 176;
  private static final int DEFAULT_IMAGE_HEIGHT = 166;
  private static final int SLOT_SIZE = 18;
  private static final int MAIN_Y = 84;
  private static final int CHEST_CRAFT_Y = 18;
  private static final int CHEST_STORAGE_Y = 52;
  private static final int CHEST_MAIN_Y = 124;
  private static final int PANEL_COLOR = 0xFFC6C6C6;
  private static final int PANEL_DARK = 0xFF373737;
  private static final int PANEL_SHADOW = 0xFF555555;
  private static final int PANEL_HIGHLIGHT = 0xFFFFFFFF;
  private static final int ARROW_COLOR = 0xFF8B8B8B;

  @Inject(method = "init", at = @At("TAIL"))
  private void mobLife$layoutMorphInventory(CallbackInfo ci) {
    mobLife$updateSlotPositions();
  }

  @Inject(method = "containerTick", at = @At("TAIL"))
  private void mobLife$refreshMorphInventoryLayout(CallbackInfo ci) {
    mobLife$updateSlotPositions();
  }

  @Redirect(
      method = "extractBackground",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
  private void mobLife$skipVanillaMorphBackground(
      GuiGraphicsExtractor graphics,
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int textureWidth,
      int textureHeight) {
    if (ClientMorphState.morph() == null) {
      graphics.blit(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
  }

  @Redirect(
      method = "extractBackground",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"))
  private void mobLife$renderInventoryEntityFacingForward(
      GuiGraphicsExtractor graphics,
      int x0,
      int y0,
      int x1,
      int y1,
      int size,
      float offsetY,
      float mouseX,
      float mouseY,
      LivingEntity entity) {
    MorphType morph = ClientMorphState.morph();
    if (morph != null) {
      Player player = entity instanceof Player currentPlayer ? currentPlayer : null;
      boolean chested = mobLife$isChested(morph, player);
      mobLife$drawMorphBackground(
          graphics, x0 - 26, y0 - 8, mobLife$imageHeight(player, morph, chested), chested);
    }
    InventoryScreen.extractEntityInInventoryFollowsMouse(
        graphics, x0, y0, x1, y1, size, offsetY, (x0 + x1) / 2.0F, (y0 + y1) / 2.0F, entity);
  }

  private void mobLife$updateSlotPositions() {
    InventoryScreen screen = (InventoryScreen) (Object) this;
    InventoryMenu menu = screen.getMenu();
    MorphType morph = ClientMorphState.morph();
    Inventory playerInventory = null;

    mobLife$moveSlot(menu.slots.get(0), 154, 28);
    for (int index = 0; index < 4; index++) {
      mobLife$moveSlot(
          menu.slots.get(index + 1), 98 + index % 2 * SLOT_SIZE, 18 + index / 2 * SLOT_SIZE);
    }

    for (Slot slot : menu.slots) {
      if (slot.container instanceof Inventory
          && slot.getContainerSlot() >= 0
          && slot.getContainerSlot() < Inventory.SELECTION_SIZE) {
        mobLife$moveSlot(slot, 8 + slot.getContainerSlot() * SLOT_SIZE, 142);
      }
      if (slot.container instanceof Inventory
          && slot.getContainerSlot() == Inventory.SLOT_OFFHAND) {
        mobLife$moveSlot(slot, 77, 62);
      }
      if (slot.container instanceof Inventory inventory) {
        playerInventory = inventory;
      }
      if (slot.container instanceof Inventory
          && slot.getContainerSlot() >= Inventory.SELECTION_SIZE
          && slot.getContainerSlot() < Inventory.INVENTORY_SIZE) {
        int offset = slot.getContainerSlot() - Inventory.SELECTION_SIZE;
        mobLife$moveSlot(slot, 8 + offset % 9 * 18, 84 + offset / 9 * 18);
      }
    }

    if (morph == null || playerInventory == null) {
      mobLife$updateScreenHeight(DEFAULT_IMAGE_HEIGHT);
      return;
    }

    Player player = playerInventory.player;
    boolean chested = mobLife$isChested(morph, player);
    int imageHeight = mobLife$imageHeight(player, morph, chested);
    mobLife$updateScreenHeight(imageHeight);
    AbstractContainerScreenAccessor screenAccessor = (AbstractContainerScreenAccessor) screen;
    if (chested) {
      screenAccessor.mobLife$setTitleLabelX(80);
      screenAccessor.mobLife$setTitleLabelY(6);
      mobLife$moveSlot(menu.slots.get(0), 127, CHEST_CRAFT_Y);
      mobLife$moveSlot(menu.slots.get(1), 80, CHEST_CRAFT_Y);
    } else {
      screenAccessor.mobLife$setTitleLabelX(97);
      screenAccessor.mobLife$setTitleLabelY(6);
      mobLife$moveSlot(menu.slots.get(0), 154, 28);
      mobLife$moveSlot(menu.slots.get(1), 107, 28);
    }
    if (morph.isEquine()) {
      mobLife$moveInventorySlot(menu, Inventory.SLOT_OFFHAND, 8, 62);
    }

    int inventorySlots = MorphInventoryCapacity.inventorySlots(player);
    int columns = Math.min(9, inventorySlots);
    int rows = (inventorySlots + columns - 1) / columns;
    for (Slot slot : menu.slots) {
      int index = slot.getContainerSlot() - Inventory.SELECTION_SIZE;
      if (slot.container instanceof Inventory && index >= 0 && index < inventorySlots) {
        int row = index / columns;
        int column = index % columns;
        int rowSlots = Math.min(columns, inventorySlots - row * columns);
        mobLife$moveSlot(
            slot,
            mobLife$centeredX(rowSlots) + column * SLOT_SIZE,
            (chested ? CHEST_MAIN_Y : MAIN_Y) + row * SLOT_SIZE);
      }
    }

    if (chested) {
      mobLife$layoutChestStorage(menu);
    }

    int hotbarSlots = MorphInventoryCapacity.hotbarSlots(player);
    int hotbarY = chested ? CHEST_MAIN_Y + rows * SLOT_SIZE + 4 : MAIN_Y + rows * SLOT_SIZE + 4;
    for (Slot slot : menu.slots) {
      int index = slot.getContainerSlot();
      if (slot.container instanceof Inventory && index >= 0 && index < hotbarSlots) {
        mobLife$moveSlot(slot, mobLife$centeredX(hotbarSlots) + index * SLOT_SIZE, hotbarY);
      }
    }
  }

  private void mobLife$updateScreenHeight(int imageHeight) {
    InventoryScreen screen = (InventoryScreen) (Object) this;
    AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
    accessor.mobLife$setImageHeight(imageHeight);
    accessor.mobLife$setTopPos((screen.height - imageHeight) / 2);
  }

  private static int mobLife$imageHeight(Player player, MorphType morph, boolean chested) {
    if (morph == null || player == null) {
      return DEFAULT_IMAGE_HEIGHT;
    }
    int inventorySlots = MorphInventoryCapacity.inventorySlots(player);
    int rows = Math.max(1, (inventorySlots + 8) / 9);
    return (chested ? CHEST_MAIN_Y : MAIN_Y) + rows * SLOT_SIZE + 28;
  }

  private static boolean mobLife$isChested(MorphType morph, Player player) {
    return morph != null
        && morph.canEquipChest()
        && player != null
        && player.getItemBySlot(EquipmentSlot.BODY).is(Items.CHEST);
  }

  private static void mobLife$layoutChestStorage(InventoryMenu menu) {
    for (Slot slot : menu.slots) {
      if (slot.container instanceof MorphChestInventory) {
        int index = slot.getContainerSlot();
        mobLife$moveSlot(slot, 80 + index % 5 * SLOT_SIZE, CHEST_STORAGE_Y + index / 5 * SLOT_SIZE);
      }
    }
  }

  private static void mobLife$drawMorphBackground(
      GuiGraphicsExtractor graphics, int left, int top, int height, boolean chested) {
    graphics.fill(left, top, left + IMAGE_WIDTH, top + height, PANEL_DARK);
    graphics.fill(left + 3, top + 3, left + IMAGE_WIDTH - 3, top + height - 3, PANEL_COLOR);
    graphics.fill(left + 3, top + 3, left + IMAGE_WIDTH - 3, top + 5, PANEL_HIGHLIGHT);
    graphics.fill(left + 3, top + 3, left + 5, top + height - 3, PANEL_HIGHLIGHT);
    graphics.fill(
        left + 3, top + height - 5, left + IMAGE_WIDTH - 3, top + height - 3, PANEL_SHADOW);
    graphics.fill(
        left + IMAGE_WIDTH - 5, top + 3, left + IMAGE_WIDTH - 3, top + height - 3, PANEL_SHADOW);
    graphics.fill(left + 26, top + 8, left + 75, top + 78, 0xFF000000);
    if (chested) {
      mobLife$drawArrow(graphics, left + 103, top + CHEST_CRAFT_Y + 7);
    } else {
      mobLife$drawArrow(graphics, left + 135, top + 35);
    }
  }

  private static void mobLife$drawArrow(GuiGraphicsExtractor graphics, int x, int y) {
    graphics.fill(x, y - 2, x + 16, y + 2, ARROW_COLOR);
    graphics.fill(x + 12, y - 6, x + 16, y + 6, ARROW_COLOR);
    graphics.fill(x + 16, y - 4, x + 18, y + 4, ARROW_COLOR);
  }

  private static void mobLife$moveInventorySlot(
      InventoryMenu menu, int inventorySlot, int x, int y) {
    for (Slot slot : menu.slots) {
      if (slot.container instanceof Inventory && slot.getContainerSlot() == inventorySlot) {
        mobLife$moveSlot(slot, x, y);
        return;
      }
    }
  }

  private static int mobLife$centeredX(int slots) {
    return (IMAGE_WIDTH - slots * SLOT_SIZE) / 2 + 1;
  }

  private static void mobLife$moveSlot(Slot slot, int x, int y) {
    SlotPositionAccessor accessor = (SlotPositionAccessor) slot;
    accessor.mobLife$setX(x);
    accessor.mobLife$setY(y);
  }
}
