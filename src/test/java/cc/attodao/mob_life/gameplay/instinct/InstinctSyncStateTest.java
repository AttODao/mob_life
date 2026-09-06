package cc.attodao.mob_life.gameplay.instinct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.attodao.mob_life.gameplay.view.MorphViewControl;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class InstinctSyncStateTest {
  @Test
  void inactiveStateResetsEveryPresentationChannel() {
    InstinctSyncState state = InstinctSyncState.INACTIVE;

    assertEquals(false, state.active());
    assertEquals(0.0F, state.level());
    assertEquals(Vec3.ZERO, state.position());
    assertEquals(false, state.onGround());
    assertEquals(MorphViewControl.Pose.ZERO, state.pose());
    assertEquals(false, state.lookingAtTarget());
    assertEquals(InstinctActivity.REST, state.activity());
    assertEquals(InstinctSyncState.Motion.STATIONARY, state.motion());
  }

  @Test
  void transportAdapterRoundTripsTheGroupedPresentationState() {
    InstinctSyncState state =
        new InstinctSyncState(
            true,
            64.0F,
            new Vec3(1.25, -3.5, 8.75),
            true,
            new MorphViewControl.Pose(170.0F, -175.0F, 22.0F),
            true,
            InstinctActivity.HUNT,
            new InstinctSyncState.Motion(0.75F, 0.4F));

    MobLifeNetworking.InstinctStatePayload payload =
        MobLifeNetworking.InstinctStatePayload.fromState(state);

    assertEquals(state, payload.toState());
    assertEquals(InstinctActivity.HUNT.ordinal(), payload.activity());
    assertEquals(13, payload.getClass().getRecordComponents().length);
  }

  @Test
  void motionSampleSanitizesInvalidDerivedValuesOnce() {
    assertEquals(
        InstinctSyncState.Motion.STATIONARY,
        new InstinctSyncState.Motion(Float.NaN, Float.POSITIVE_INFINITY));
    assertEquals(InstinctSyncState.Motion.STATIONARY, new InstinctSyncState.Motion(-1.0F, -0.1F));
  }

  @Test
  void inboundAdapterClampsLevelAndDefaultsAnUnknownActivity() {
    MobLifeNetworking.InstinctStatePayload payload =
        new MobLifeNetworking.InstinctStatePayload(
            true,
            120.0F,
            1.0,
            2.0,
            3.0,
            false,
            10.0F,
            20.0F,
            30.0F,
            false,
            Integer.MAX_VALUE,
            0.0F,
            0.0F);

    InstinctSyncState state = payload.toState();

    assertEquals(100.0F, state.level());
    assertEquals(InstinctActivity.REST, state.activity());
  }
}
