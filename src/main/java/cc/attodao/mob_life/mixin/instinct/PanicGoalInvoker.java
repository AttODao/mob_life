package cc.attodao.mob_life.mixin.instinct;

import net.minecraft.world.entity.ai.goal.PanicGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes the source mob's panic predicate without requiring a path to exist first. */
@Mixin(PanicGoal.class)
public interface PanicGoalInvoker {
  @Invoker("shouldPanic")
  boolean mobLife$shouldPanic();
}
