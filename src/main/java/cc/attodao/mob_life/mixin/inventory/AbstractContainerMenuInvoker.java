package cc.attodao.mob_life.mixin.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuInvoker {

  @Invoker("addSlot")
  Slot mobLife$addSlot(Slot slot);

  @Invoker("moveItemStackTo")
  boolean mobLife$moveItemStackTo(
      ItemStack stack, int startIndex, int endIndex, boolean reverseDirection);
}
