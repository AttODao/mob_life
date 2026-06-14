package cc.attodao.mob_life.client.mixin.inventory;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.gameplay.inventory.MorphChestInventory;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
  private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
  private static final int INVENTORY_BACKGROUND_COLOR = 0xFFC6C6C6;

  @Inject(
      method = "extractContents",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
  private void mobLife$shadeInactiveInventorySlots(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
    AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
    boolean inventoryScreen = screen instanceof InventoryScreen;
    MorphType morph = ClientMorphState.morph();
    if (inventoryScreen && morph != null) {
      mobLife$renderMorphInventorySlots(graphics, screen, morph);
      return;
    }

    for (Slot slot : screen.getMenu().slots) {
      if (!slot.isActive()) {
        mobLife$eraseSlot(graphics, slot.x, slot.y);
      }
    }
  }

  private static void mobLife$renderMorphInventorySlots(
      GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, MorphType morph) {
    for (Slot slot : screen.getMenu().slots) {
      if (slot.isActive()
          && ((slot.container instanceof Inventory && mobLife$isVisiblePlayerSlot(slot))
              || slot.container instanceof MorphChestInventory
              || slot.container instanceof CraftingContainer
              || slot.container instanceof ResultContainer)) {
        mobLife$drawSlot(graphics, slot.x, slot.y);
      }
    }
  }

  private static boolean mobLife$isVisiblePlayerSlot(Slot slot) {
    int inventorySlot = slot.getContainerSlot();
    return inventorySlot < Inventory.INVENTORY_SIZE
        || inventorySlot == Inventory.SLOT_OFFHAND
        || inventorySlot == Inventory.SLOT_BODY_ARMOR
        || inventorySlot == Inventory.SLOT_SADDLE;
  }

  private static void mobLife$drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x - 1, y - 1, 18, 18);
  }

  private static void mobLife$eraseSlot(GuiGraphicsExtractor graphics, int x, int y) {
    graphics.fill(x - 1, y - 1, x + 17, y + 17, INVENTORY_BACKGROUND_COLOR);
  }
}
