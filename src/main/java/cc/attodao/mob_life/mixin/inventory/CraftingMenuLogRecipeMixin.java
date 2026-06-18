package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.MobLife;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuLogRecipeMixin {
  private static final ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>
      LOG_TO_CRAFTING_TABLE =
          ResourceKey.create(Registries.RECIPE, MobLife.id("log_to_crafting_table"));

  @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"), cancellable = true)
  private static void mobLife$restrictLogToInventoryCrafting(
      AbstractContainerMenu menu,
      ServerLevel level,
      Player player,
      CraftingContainer craftSlots,
      ResultContainer resultSlots,
      RecipeHolder<CraftingRecipe> recipe,
      CallbackInfo ci) {
    RecipeHolder<? extends CraftingRecipe> selected = recipe;
    if (selected == null) {
      Optional<RecipeHolder<CraftingRecipe>> resolved =
          level
              .getServer()
              .getRecipeManager()
              .getRecipeFor(RecipeType.CRAFTING, craftSlots.asCraftInput(), level);
      if (resolved.isEmpty()) {
        return;
      }
      selected = resolved.get();
    }

    if (!LOG_TO_CRAFTING_TABLE.equals(selected.id())) {
      return;
    }

    ItemStack empty = ItemStack.EMPTY;
    resultSlots.setItem(0, empty);
    menu.setRemoteSlot(0, empty);
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.connection.send(
          new ClientboundContainerSetSlotPacket(
              menu.containerId, menu.incrementStateId(), 0, empty));
    }
    ci.cancel();
  }
}
