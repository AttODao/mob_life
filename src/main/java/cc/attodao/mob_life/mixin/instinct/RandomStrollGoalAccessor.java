package cc.attodao.mob_life.mixin.instinct;

import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RandomStrollGoal.class)
public interface RandomStrollGoalAccessor {
  @Accessor("speedModifier")
  double mobLife$getSpeedModifier();
}
