package cc.attodao.mob_life.client.mixin.inventory;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class ClientSlotMixin {

  @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
  private void mobLife$limitMorphCraftingSlots(CallbackInfoReturnable<Boolean> cir) {
    MorphType morph = ClientMorphState.morph();
    Slot slot = (Slot) (Object) this;
    if (morph != null
        && slot.container instanceof CraftingContainer crafting
        && crafting.getWidth() == 2
        && crafting.getHeight() == 2
        && slot.getContainerSlot() != 0) {
      cir.setReturnValue(false);
    }
  }
}
