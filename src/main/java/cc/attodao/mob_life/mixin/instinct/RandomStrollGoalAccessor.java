package cc.attodao.mob_life.mixin.instinct;

import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RandomStrollGoal.class)
public interface RandomStrollGoalAccessor {
  @Accessor("speedModifier")
  double mobLife$getSpeedModifier();

  @Invoker("getPosition")
  Vec3 mobLife$getPosition();
}
