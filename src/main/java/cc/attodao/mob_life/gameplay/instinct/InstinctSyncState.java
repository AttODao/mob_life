package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.gameplay.view.MorphViewControl;
import java.util.Objects;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Transport-independent presentation state produced by Instinct proxy synchronization. */
public record InstinctSyncState(
    boolean active,
    float level,
    Vec3 position,
    boolean onGround,
    MorphViewControl.Pose pose,
    boolean lookingAtTarget,
    InstinctActivity activity,
    Motion motion) {
  public static final InstinctSyncState INACTIVE =
      new InstinctSyncState(
          false,
          0.0F,
          Vec3.ZERO,
          false,
          MorphViewControl.Pose.ZERO,
          false,
          InstinctActivity.REST,
          Motion.STATIONARY);

  public InstinctSyncState {
    level = Mth.clamp(level, 0.0F, 100.0F);
    position = Objects.requireNonNull(position, "position");
    pose = Objects.requireNonNull(pose, "pose");
    activity = Objects.requireNonNull(activity, "activity");
    motion = Objects.requireNonNull(motion, "motion");
  }

  public record Motion(float horizontalDisplacement, float horizontalSpeed) {
    public static final Motion STATIONARY = new Motion(0.0F, 0.0F);

    public Motion {
      if (!Float.isFinite(horizontalDisplacement) || horizontalDisplacement < 0.0F) {
        horizontalDisplacement = 0.0F;
      }
      if (!Float.isFinite(horizontalSpeed) || horizontalSpeed < 0.0F) {
        horizontalSpeed = 0.0F;
      }
    }
  }
}
