package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctAiContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomStrollGoal.class)
public abstract class RandomStrollInstinctMixin {
  @Shadow @Final protected PathfinderMob mob;

  @Shadow protected double wantedX;

  @Shadow protected double wantedY;

  @Shadow protected double wantedZ;

  @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
  private void mobLife$biasInstinctExploration(CallbackInfoReturnable<Boolean> cir) {
    if (!cir.getReturnValueZ()) {
      return;
    }
    float centerYaw = InstinctAiContext.explorationYaw(mob);
    if (!Float.isFinite(centerYaw)) {
      return;
    }

    float radians = centerYaw * Mth.DEG_TO_RAD;
    double xDirection = -Mth.sin(radians);
    double zDirection = Mth.cos(radians);
    boolean restricted = GoalUtils.mobRestricted(mob, 10.0);
    Vec3 destination =
        RandomPos.generateRandomPos(
            mob,
            () -> {
              BlockPos relative =
                  RandomPos.generateRandomDirectionWithinRadians(
                      mob.getRandom(),
                      0.0,
                      10.0,
                      7,
                      0,
                      xDirection,
                      zDirection,
                      15.0 * Mth.DEG_TO_RAD);
              if (relative == null) {
                return null;
              }
              BlockPos candidate =
                  LandRandomPos.generateRandomPosTowardDirection(mob, 10.0, restricted, relative);
              return candidate != null ? LandRandomPos.movePosUpOutOfSolid(mob, candidate) : null;
            });
    if (destination == null) {
      cir.setReturnValue(false);
      return;
    }
    wantedX = destination.x;
    wantedY = destination.y;
    wantedZ = destination.z;
  }
}
