package cc.attodao.mob_life.gameplay.movement;

import net.minecraft.util.Mth;

public final class MorphViewRecovery {
  public static final float CAMERA_INPUT_THRESHOLD_DEGREES = 0.5F;

  private MorphViewRecovery() {}

  public static float accumulateCameraDelta(
      float accumulatedDelta, float yawDelta, float pitchDelta) {
    if (!Float.isFinite(yawDelta) || !Float.isFinite(pitchDelta)) {
      return accumulatedDelta;
    }
    return Mth.clamp(accumulatedDelta + Math.abs(yawDelta) + Math.abs(pitchDelta), 0.0F, 360.0F);
  }

  public static boolean cameraInputBlocksRecovery(float cameraDelta) {
    return Float.isFinite(cameraDelta) && cameraDelta >= CAMERA_INPUT_THRESHOLD_DEGREES;
  }
}
