package cc.attodao.mob_life.gameplay.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

class MorphViewControlTest {
  private static final float EPSILON = 0.0001F;

  @Test
  void normalSequenceTurnsBlocksRecoveryThenRecenters() {
    MorphViewControl.Normal.State state = MorphViewControl.Normal.initial();

    MorphViewControl.Normal.Transition bodyTurn =
        MorphViewControl.Normal.reduce(
            state,
            new MorphViewControl.Normal.BodyTick(
                new MorphViewControl.View(170.0F, 45.0F),
                1.0F,
                MorphViewControl.Normal.BodyMode.TURN));
    state = bodyTurn.state();
    assertEquals(160.0F, state.bodyYaw(), EPSILON);
    assertRotation(bodyTurn.rotation(), 160.0F, 40.0F, MorphViewControl.History.INTERPOLATE);

    MorphViewControl.Normal.Transition look =
        MorphViewControl.Normal.reduce(
            state,
            new MorphViewControl.Normal.LookInput(
                new MorphViewControl.View(160.0F, 39.0F),
                new MorphViewControl.View(159.0F, 38.0F),
                100.0,
                20.0));
    state = look.state();
    assertEquals(18.0F, state.pendingCameraInput(), EPSILON);
    assertRotation(look.rotation(), 175.0F, 39.0F, MorphViewControl.History.SNAP);
    assertView(look.rotation().previous(), 174.0F, 38.0F);
    assertEquals(15.0F, look.rotation().previousHeadYawDelta(), EPSILON);

    MorphViewControl.Normal.Transition blockedRecovery =
        MorphViewControl.Normal.reduce(
            state, new MorphViewControl.Normal.RecoveryTick(look.rotation().current(), true, true));
    state = blockedRecovery.state();
    assertFalse(blockedRecovery.rotation().apply());
    assertEquals(0.0F, state.pendingCameraInput(), EPSILON);

    MorphViewControl.Normal.Transition recovery =
        MorphViewControl.Normal.reduce(
            state, new MorphViewControl.Normal.RecoveryTick(look.rotation().current(), true, true));
    assertRotation(recovery.rotation(), 173.0F, 37.0F, MorphViewControl.History.INTERPOLATE);
  }

  @Test
  void normalLookTreatsNonFiniteAxesIndependentlyAndSnapsPreviousPitch() {
    MorphViewControl.Normal.State state =
        MorphViewControl.Normal.reduce(
                MorphViewControl.Normal.initial(),
                new MorphViewControl.Normal.BodyTick(
                    MorphViewControl.View.ZERO, 0.0F, MorphViewControl.Normal.BodyMode.ALIGN))
            .state();

    MorphViewControl.Normal.Transition transition =
        MorphViewControl.Normal.reduce(
            state,
            new MorphViewControl.Normal.LookInput(
                new MorphViewControl.View(0.0F, 45.0F),
                new MorphViewControl.View(12.0F, 10.0F),
                Double.NaN,
                -10.0));

    assertEquals(1.5F, transition.state().pendingCameraInput(), EPSILON);
    assertRotation(transition.rotation(), 0.0F, 40.0F, MorphViewControl.History.SNAP);
    assertView(transition.rotation().previous(), 12.0F, 8.5F);
  }

  @Test
  void instinctClientSequencePreservesInputUntilTheFollowingQuietTick() {
    MorphViewControl.InstinctClient.State state = MorphViewControl.InstinctClient.initial();

    MorphViewControl.InstinctClient.LookCapture inactiveCapture =
        MorphViewControl.InstinctClient.reduce(
            state, new MorphViewControl.InstinctClient.LookInput(1.0, 1.0));
    state = inactiveCapture.state();
    assertFalse(inactiveCapture.handled());
    assertEquals(0.3F, state.pendingCameraInput(), EPSILON);

    state =
        MorphViewControl.InstinctClient.reduce(
            state,
            new MorphViewControl.InstinctClient.Snapshot(
                true,
                new MorphViewControl.Pose(10.0F, 20.0F, 5.0F),
                true,
                new MorphViewControl.View(70.0F, 39.0F)));
    MorphViewControl.InstinctClient.LookCapture activeCapture =
        MorphViewControl.InstinctClient.reduce(
            state, new MorphViewControl.InstinctClient.LookInput(1.0, 1.0));
    state = activeCapture.state();
    assertTrue(activeCapture.handled());
    assertView(state.requested(), 70.15F, 39.15F);

    MorphViewControl.InstinctClient.Frame activeFrame =
        MorphViewControl.InstinctClient.reduce(
            state,
            new MorphViewControl.InstinctClient.Tick(
                true, new MorphViewControl.View(70.0F, 39.0F)));
    state = activeFrame.state();
    assertEquals(0.6F, activeFrame.cameraInput(), EPSILON);
    assertView(activeFrame.cameraRotation().current(), 40.0F, 9.0F);
    assertView(activeFrame.requested(), 70.15F, 39.15F);
    assertPose(activeFrame.renderedPose(), 10.0F, 20.0F, 5.0F);

    MorphViewControl.InstinctClient.Frame quietFrame =
        MorphViewControl.InstinctClient.reduce(
            state,
            new MorphViewControl.InstinctClient.Tick(true, activeFrame.cameraRotation().current()));
    assertEquals(0.0F, quietFrame.cameraInput(), EPSILON);
    assertView(quietFrame.cameraRotation().current(), 20.0F, 5.0F);
    assertView(quietFrame.requested(), 20.0F, 5.0F);
  }

  @Test
  void instinctClientRejectsTheWholeNonFiniteInputPair() {
    MorphViewControl.InstinctClient.State state =
        MorphViewControl.InstinctClient.reduce(
            MorphViewControl.InstinctClient.initial(),
            new MorphViewControl.InstinctClient.Snapshot(
                true, MorphViewControl.Pose.ZERO, true, new MorphViewControl.View(3.0F, 4.0F)));

    MorphViewControl.InstinctClient.LookCapture capture =
        MorphViewControl.InstinctClient.reduce(
            state, new MorphViewControl.InstinctClient.LookInput(Double.NaN, 10.0));

    assertTrue(capture.handled());
    assertEquals(state, capture.state());
  }

  @Test
  void instinctServerSequenceAppliesRabbitFacingThenCameraAuthority() {
    MorphViewControl.InstinctServer.State state =
        new MorphViewControl.InstinctServer.State(
            new MorphViewControl.Pose(170.0F, -170.0F, 20.0F));

    state =
        MorphViewControl.InstinctServer.reduce(
            state, new MorphViewControl.InstinctServer.FaceRabbitMotion(170.0F, -170.0F));
    assertPose(state.pose(), 185.0F, 205.0F, 20.0F);

    state =
        MorphViewControl.InstinctServer.reduce(
            state,
            new MorphViewControl.InstinctServer.ResolveAuthority(
                new MorphViewControl.Pose(170.0F, 170.0F, 0.0F),
                new MorphViewControl.View(-120.0F, -30.0F),
                0.5F,
                false));
    assertPose(state.pose(), 185.0F, 180.0F, -10.0F);
  }

  @Test
  void instinctServerGivesAiLookPriorityAndTracksFeedingTarget() {
    MorphViewControl.InstinctServer.State state =
        new MorphViewControl.InstinctServer.State(new MorphViewControl.Pose(120.0F, 80.0F, 30.0F));
    state =
        MorphViewControl.InstinctServer.reduce(
            state,
            new MorphViewControl.InstinctServer.ResolveAuthority(
                MorphViewControl.Pose.ZERO,
                new MorphViewControl.View(-60.0F, -30.0F),
                100.0F,
                true));
    assertPose(state.pose(), 90.0F, 15.0F, 10.0F);

    state =
        MorphViewControl.InstinctServer.reduce(
            state,
            new MorphViewControl.InstinctServer.TrackFeedingTarget(
                new MorphViewControl.View(70.0F, -30.0F)));
    assertPose(state.pose(), 90.0F, 25.0F, 0.0F);
  }

  @Test
  void instinctServerUsesDirectInputAtExactlyTheRecoveryThreshold() {
    MorphViewControl.InstinctServer.State nativeState =
        new MorphViewControl.InstinctServer.State(new MorphViewControl.Pose(20.0F, 30.0F, 30.0F));
    MorphViewControl.Pose previous = MorphViewControl.Pose.ZERO;
    MorphViewControl.View requested = new MorphViewControl.View(60.0F, 25.0F);

    MorphViewControl.Pose belowThreshold =
        MorphViewControl.InstinctServer.reduce(
                nativeState,
                new MorphViewControl.InstinctServer.ResolveAuthority(
                    previous, requested, 0.499F, false))
            .pose();
    assertPose(belowThreshold, 20.0F, 2.0F, 0.0F);

    MorphViewControl.Pose atThreshold =
        MorphViewControl.InstinctServer.reduce(
                nativeState,
                new MorphViewControl.InstinctServer.ResolveAuthority(
                    previous, requested, 0.5F, false))
            .pose();
    assertPose(atThreshold, 20.0F, 10.0F, 10.0F);
  }

  @Test
  void restingTurnKeepsTheOriginalDeadZoneAndWrapsYaw() {
    MorphViewControl.InstinctServer.State state =
        new MorphViewControl.InstinctServer.State(new MorphViewControl.Pose(30.0F, 40.0F, 5.0F));
    state =
        MorphViewControl.InstinctServer.reduce(
            state, new MorphViewControl.InstinctServer.TurnRestingBody(179.0F, 0.1F));
    assertEquals(179.0F, state.pose().bodyYaw(), EPSILON);

    state =
        MorphViewControl.InstinctServer.reduce(
            state, new MorphViewControl.InstinctServer.TurnRestingBody(179.0F, 1.0F));
    assertEquals(-171.0F, state.pose().bodyYaw(), EPSILON);
  }

  @Test
  void serverRecoveryConvergesAcrossEveryWrappedHeadYaw() {
    for (int startingYaw = -180; startingYaw <= 180; startingYaw++) {
      MorphViewControl.Pose pose = new MorphViewControl.Pose(0.0F, startingYaw, 0.0F);
      float previousDistance = Math.abs(Mth.wrapDegrees(pose.headYaw()));
      for (int tick = 0; tick < 90; tick++) {
        pose =
            MorphViewControl.InstinctServer.reduce(
                    new MorphViewControl.InstinctServer.State(pose),
                    new MorphViewControl.InstinctServer.ResolveAuthority(
                        pose, MorphViewControl.View.ZERO, 0.0F, false))
                .pose();
        float distance = Math.abs(Mth.wrapDegrees(pose.headYaw()));
        assertTrue(distance <= previousDistance);
        previousDistance = distance;
      }
      assertEquals(0.0F, Mth.wrapDegrees(pose.headYaw()), EPSILON);
    }
  }

  private static void assertRotation(
      MorphViewControl.Rotation rotation,
      float yaw,
      float pitch,
      MorphViewControl.History history) {
    assertTrue(rotation.apply());
    assertEquals(history, rotation.history());
    assertView(rotation.current(), yaw, pitch);
  }

  private static void assertView(MorphViewControl.View view, float yaw, float pitch) {
    assertEquals(yaw, view.yaw(), EPSILON);
    assertEquals(pitch, view.pitch(), EPSILON);
  }

  private static void assertPose(
      MorphViewControl.Pose pose, float bodyYaw, float headYaw, float headPitch) {
    assertEquals(bodyYaw, pose.bodyYaw(), EPSILON);
    assertEquals(headYaw, pose.headYaw(), EPSILON);
    assertEquals(headPitch, pose.headPitch(), EPSILON);
  }
}
