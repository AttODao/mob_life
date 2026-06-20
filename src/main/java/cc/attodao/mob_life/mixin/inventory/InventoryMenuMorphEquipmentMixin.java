package cc.attodao.mob_life.mixin.inventory;

import cc.attodao.mob_life.gameplay.inventory.MorphChestInventory;
import cc.attodao.mob_life.gameplay.inventory.MorphEquipment;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMorphEquipmentMixin {
  private static final int MORPH_BODY_SLOT = 46;
  private static final int MORPH_SADDLE_SLOT = 47;
  private static final int MORPH_CHEST_SLOT_START = 48;
  private static final int MORPH_CHEST_SLOT_END = MORPH_CHEST_SLOT_START + MorphChestInventory.SIZE;
  private static final Identifier HORSE_ARMOR_SLOT_SPRITE =
      Identifier.withDefaultNamespace("container/slot/horse_armor");
  private static final Identifier SADDLE_SLOT_SPRITE =
      Identifier.withDefaultNamespace("container/slot/saddle");

  @Shadow @Final private Player owner;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void mobLife$addMorphEquipmentSlots(
      Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
    mobLife$menu()
        .mobLife$addSlot(
            new Slot(inventory, Inventory.SLOT_BODY_ARMOR, 8, 36) {
              @Override
              public boolean isActive() {
                return MorphEquipment.isSlotActive(owner, Inventory.SLOT_BODY_ARMOR);
              }

              @Override
              public boolean mayPlace(ItemStack stack) {
                return MorphEquipment.mayPlaceBody(owner, stack);
              }

              @Override
              public int getMaxStackSize() {
                return 1;
              }

              @Override
              public Identifier getNoItemIcon() {
                return MorphEquipment.morph(owner).isEquine()
                        && !MorphEquipment.morph(owner).canEquipChest()
                    ? HORSE_ARMOR_SLOT_SPRITE
                    : null;
              }

              @Override
              public void setByPlayer(ItemStack stack, ItemStack previous) {
                if (previous.is(Items.CHEST) && !stack.is(Items.CHEST)) {
                  MorphChestInventory.get(owner).returnTo(owner);
                }
                owner.onEquipItem(EquipmentSlot.BODY, previous, stack);
                super.setByPlayer(stack, previous);
              }
            });
    mobLife$menu()
        .mobLife$addSlot(
            new Slot(inventory, Inventory.SLOT_SADDLE, 8, 18) {
              @Override
              public boolean isActive() {
                return MorphEquipment.isSlotActive(owner, Inventory.SLOT_SADDLE);
              }

              @Override
              public boolean mayPlace(ItemStack stack) {
                return MorphEquipment.mayPlaceSaddle(owner, stack);
              }

              @Override
              public int getMaxStackSize() {
                return 1;
              }

              @Override
              public Identifier getNoItemIcon() {
                return SADDLE_SLOT_SPRITE;
              }

              @Override
              public void setByPlayer(ItemStack stack, ItemStack previous) {
                owner.onEquipItem(EquipmentSlot.SADDLE, previous, stack);
                super.setByPlayer(stack, previous);
              }
            });
    MorphChestInventory chestInventory = MorphChestInventory.get(owner);
    for (int slot = 0; slot < MorphChestInventory.SIZE; slot++) {
      int index = slot;
      mobLife$menu()
          .mobLife$addSlot(
              new Slot(chestInventory, index, 80 + index % 5 * 18, 18 + index / 5 * 18) {
                @Override
                public boolean isActive() {
                  return MorphChestInventory.isAvailable(owner);
                }
              });
    }
  }

  @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
  private void mobLife$quickMoveMorphEquipment(
      Player player,
      int slotIndex,
      org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<ItemStack> cir) {
    if (slotIndex < 0
        || slotIndex >= mobLife$container().slots.size()
        || slotIndex == MORPH_BODY_SLOT
        || slotIndex == MORPH_SADDLE_SLOT) {
      return;
    }

    Slot source = mobLife$container().slots.get(slotIndex);
    if (!source.hasItem()) {
      return;
    }

    ItemStack stack = source.getItem();
    if (source.container instanceof MorphChestInventory) {
      ItemStack original = stack.copy();
      if (!mobLife$menu()
          .mobLife$moveItemStackTo(
              stack, InventoryMenu.INV_SLOT_START, InventoryMenu.USE_ROW_SLOT_END, false)) {
        cir.setReturnValue(ItemStack.EMPTY);
        return;
      }
      if (stack.isEmpty()) {
        source.setByPlayer(ItemStack.EMPTY, original);
      } else {
        source.setChanged();
      }
      cir.setReturnValue(original);
      return;
    }

    int target =
        MorphEquipment.mayPlaceBody(player, stack)
            ? MORPH_BODY_SLOT
            : MorphEquipment.mayPlaceSaddle(player, stack) ? MORPH_SADDLE_SLOT : -1;
    if (target >= 0
        && (!mobLife$container().slots.get(target).isActive()
            || mobLife$container().slots.get(target).hasItem())) {
      target = -1;
    }
    if (target < 0
        && MorphChestInventory.isAvailable(player)
        && slotIndex >= InventoryMenu.INV_SLOT_START
        && slotIndex < InventoryMenu.USE_ROW_SLOT_END) {
      ItemStack original = stack.copy();
      if (mobLife$menu()
          .mobLife$moveItemStackTo(stack, MORPH_CHEST_SLOT_START, MORPH_CHEST_SLOT_END, false)) {
        if (stack.isEmpty()) {
          source.setByPlayer(ItemStack.EMPTY, original);
        } else {
          source.setChanged();
        }
        cir.setReturnValue(original);
      }
      return;
    }
    if (target < 0) {
      return;
    }

    ItemStack original = stack.copy();
    if (!mobLife$menu().mobLife$moveItemStackTo(stack, target, target + 1, false)) {
      return;
    }
    if (stack.isEmpty()) {
      source.setByPlayer(ItemStack.EMPTY, original);
    } else {
      source.setChanged();
    }
    cir.setReturnValue(original);
  }

  @Inject(method = "slotsChanged", at = @At("TAIL"))
  private void mobLife$showInventoryLogCraftingTable(Container container, CallbackInfo ci) {
    if (!ServerMorphManager.hasMobForm()
        || !(owner instanceof ServerPlayer serverPlayer)
        || !(owner.level() instanceof ServerLevel)) {
      return;
    }

    AbstractCraftingMenuAccessor craftingMenu = (AbstractCraftingMenuAccessor) this;
    CraftingContainer input = craftingMenu.mobLife$getCraftSlots();
    if (!input.getItem(0).is(ItemTags.LOGS)
        || !input.getItem(1).isEmpty()
        || !input.getItem(2).isEmpty()
        || !input.getItem(3).isEmpty()) {
      return;
    }

    ResultContainer result = craftingMenu.mobLife$getResultSlots();
    ItemStack output = new ItemStack(Items.CRAFTING_TABLE);
    result.setItem(0, output);
    mobLife$container().setRemoteSlot(0, output);
    serverPlayer.connection.send(
        new ClientboundContainerSetSlotPacket(
            mobLife$container().containerId, mobLife$container().incrementStateId(), 0, output));
  }

  private AbstractContainerMenu mobLife$container() {
    return (AbstractContainerMenu) (Object) this;
  }

  private AbstractContainerMenuInvoker mobLife$menu() {
    return (AbstractContainerMenuInvoker) (Object) this;
  }
}
