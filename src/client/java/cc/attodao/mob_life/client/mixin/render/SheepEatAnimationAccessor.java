package cc.attodao.mob_life.client.mixin.render;

import net.minecraft.world.entity.animal.sheep.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Sheep.class)
public interface SheepEatAnimationAccessor {
  @Accessor("eatAnimationTick")
  void mobLife$setEatAnimationTick(int ticks);
}
