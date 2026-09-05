package cc.attodao.mob_life.gameplay.instinct;

import net.minecraft.util.Mth;

public final class InstinctAngles {
  private InstinctAngles() {}

  public static float approachYaw(float currentYaw, float targetYaw, float maximumChange) {
    return Mth.approachDegrees(currentYaw, targetYaw, maximumChange);
  }

  public static float clampHeadYawToBody(float headYaw, float bodyYaw, float maximumOffset) {
    float limit = Math.abs(maximumOffset);
    return bodyYaw + Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -limit, limit);
  }
}
