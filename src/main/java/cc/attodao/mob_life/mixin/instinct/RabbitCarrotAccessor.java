package cc.attodao.mob_life.mixin.instinct;

import net.minecraft.world.entity.animal.rabbit.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Rabbit.class)
public interface RabbitCarrotAccessor {
  @Accessor("moreCarrotTicks")
  int mobLife$getMoreCarrotTicks();

  @Accessor("moreCarrotTicks")
  void mobLife$setMoreCarrotTicks(int ticks);
}
