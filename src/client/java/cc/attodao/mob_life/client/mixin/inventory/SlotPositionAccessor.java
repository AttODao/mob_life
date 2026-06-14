package cc.attodao.mob_life.client.mixin.inventory;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface SlotPositionAccessor {

  @Accessor("x")
  @Mutable
  void mobLife$setX(int x);

  @Accessor("y")
  @Mutable
  void mobLife$setY(int y);
}
