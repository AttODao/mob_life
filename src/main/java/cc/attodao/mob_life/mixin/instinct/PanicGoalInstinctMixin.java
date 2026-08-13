package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PanicGoal.class)
public abstract class PanicGoalInstinctMixin {
  @Shadow protected PathfinderMob mob;

  @Shadow protected double posX;

  @Shadow protected double posY;

  @Shadow protected double posZ;

  @Inject(method = "findRandomPosition", at = @At("HEAD"), cancellable = true)
  private void mobLife$findPositionAwayFromAttacker(CallbackInfoReturnable<Boolean> cir) {
    Vec3 source = InstinctManager.panicEscapeSource(mob);
    if (source == null) {
      return;
    }

    // Give attack-triggered panic enough horizontal room to find a route around nearby obstacles.
    Vec3 destination = LandRandomPos.getPosAway(mob, 16, 4, source);
    if (destination == null) {
      return;
    }
    posX = destination.x;
    posY = destination.y;
    posZ = destination.z;
    cir.setReturnValue(true);
  }
}
