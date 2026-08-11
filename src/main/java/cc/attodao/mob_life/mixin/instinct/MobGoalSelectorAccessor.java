package cc.attodao.mob_life.mixin.instinct;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobGoalSelectorAccessor {
  @Accessor("goalSelector")
  GoalSelector mobLife$getGoalSelector();

  @Accessor("targetSelector")
  GoalSelector mobLife$getTargetSelector();
}
