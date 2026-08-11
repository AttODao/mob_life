package cc.attodao.mob_life.mixin.instinct;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityFluidInteractionInvoker {
  @Invoker("updateFluidInteraction")
  boolean mobLife$invokeUpdateFluidInteraction();
}
