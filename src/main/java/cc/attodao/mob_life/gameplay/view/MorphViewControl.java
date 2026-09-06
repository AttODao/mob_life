package cc.attodao.mob_life.gameplay.view;

import net.minecraft.util.Mth;

/** Deterministic body, head, and camera transitions shared by every morph view adapter. */
public final class MorphViewControl {
  private static final float RAW_LOOK_SCALE = 0.15F;
  private static final float CAMERA_INPUT_THRESHOLD = 0.5F;
  private static final float MAX_ACCUMULATED_CAMERA_INPUT = 360.0F;
  private static final float MAX_HEAD_YAW = 75.0F;
  private static final float MAX_HEAD_PITCH = 40.0F;
  private static final float NORMAL_BODY_TURN = 10.0F;
  private static final float NORMAL_RECOVERY = 2.0F;
  private static final float INSTINCT_CAMERA_FOLLOW = 30.0F;
  private static final float INSTINCT_BODY_FOLLOW = 90.0F;
  private static final float INSTINCT_HEAD_FOLLOW = 10.0F;
  private static final float INSTINCT_HEAD_RECOVERY = 2.0F;
  private static final float RABBIT_BODY_FOLLOW = 15.0F;
  private static final float FEEDING_HEAD_FOLLOW = 10.0F;

  private MorphViewControl() {}

  public record View(float yaw, float pitch) {
    public static final View ZERO = new View(0.0F, 0.0F);
  }

  public record Pose(float bodyYaw, float headYaw, float headPitch) {
    public static final Pose ZERO = new Pose(0.0F, 0.0F, 0.0F);
  }

  public enum History {
    INTERPOLATE,
    SNAP
  }

  public record Rotation(
      View current, History history, View previous, float previousHeadYawDelta, boolean apply) {
    private static final Rotation NONE =
        new Rotation(View.ZERO, History.INTERPOLATE, View.ZERO, 0.0F, false);
  }

  public static final class Normal {
    private Normal() {}

    public record State(boolean bodyKnown, float bodyYaw, float pendingCameraInput) {}

    public enum BodyMode {
      TURN,
      ALIGN,
      SUSPEND
    }

    public record LookInput(View current, View previous, double rawYaw, double rawPitch) {}

    public record BodyTick(View current, float steering, BodyMode mode) {}

    public record RecoveryTick(View current, boolean active, boolean conditionsAllowRecovery) {}

    public record Transition(State state, Rotation rotation) {}

    public static State initial() {
      return new State(false, 0.0F, 0.0F);
    }

    public static Transition reduce(State state, LookInput input) {
      State oriented = ensureBody(state, input.current().yaw());
      float yawDelta = finiteRawDelta(input.rawYaw());
      float pitchDelta = finiteRawDelta(input.rawPitch());
      float accumulated =
          accumulateCameraInput(oriented.pendingCameraInput(), yawDelta, pitchDelta);

      float currentYawOffset = Mth.wrapDegrees(input.current().yaw() - oriented.bodyYaw());
      float wantedYawOffset = Mth.wrapDegrees(currentYawOffset + yawDelta);
      if (Math.abs(wantedYawOffset) > MAX_HEAD_YAW
          && Math.abs(wantedYawOffset) > Math.abs(currentYawOffset)) {
        yawDelta = 0.0F;
      }
      float wantedPitch = input.current().pitch() + pitchDelta;
      if (Math.abs(wantedPitch) > MAX_HEAD_PITCH
          && Math.abs(wantedPitch) > Math.abs(input.current().pitch())) {
        pitchDelta = 0.0F;
      }
      View current =
          new View(
              input.current().yaw() + yawDelta,
              Mth.clamp(input.current().pitch() + pitchDelta, -MAX_HEAD_PITCH, MAX_HEAD_PITCH));
      View previous =
          new View(
              input.previous().yaw() + yawDelta,
              Mth.clamp(input.previous().pitch() + pitchDelta, -MAX_HEAD_PITCH, MAX_HEAD_PITCH));
      return new Transition(
          new State(true, oriented.bodyYaw(), accumulated),
          new Rotation(current, History.SNAP, previous, yawDelta, true));
    }

    public static Transition reduce(State state, BodyTick tick) {
      if (tick.mode() == BodyMode.SUSPEND) {
        return new Transition(
            new State(false, state.bodyYaw(), state.pendingCameraInput()), Rotation.NONE);
      }

      State oriented = ensureBody(state, tick.current().yaw());
      if (tick.mode() == BodyMode.ALIGN) {
        return new Transition(
            new State(true, tick.current().yaw(), oriented.pendingCameraInput()), Rotation.NONE);
      }

      float turn = -tick.steering() * NORMAL_BODY_TURN;
      return new Transition(
          new State(true, oriented.bodyYaw() + turn, oriented.pendingCameraInput()),
          turn == 0.0F
              ? Rotation.NONE
              : new Rotation(
                  new View(
                      tick.current().yaw() + turn,
                      Mth.clamp(tick.current().pitch(), -MAX_HEAD_PITCH, MAX_HEAD_PITCH)),
                  History.INTERPOLATE,
                  View.ZERO,
                  0.0F,
                  true));
    }

    public static Transition reduce(State state, RecoveryTick tick) {
      float cameraInput = state.pendingCameraInput();
      State next = new State(state.bodyKnown(), state.bodyYaw(), 0.0F);
      if (!tick.active()) {
        return new Transition(next, Rotation.NONE);
      }

      next = ensureBody(next, tick.current().yaw());
      if (!tick.conditionsAllowRecovery() || blocksRecovery(cameraInput)) {
        return new Transition(next, Rotation.NONE);
      }

      float yawDelta =
          Mth.clamp(
              Mth.wrapDegrees(next.bodyYaw() - tick.current().yaw()),
              -NORMAL_RECOVERY,
              NORMAL_RECOVERY);
      float pitchStep = Mth.clamp(-tick.current().pitch(), -NORMAL_RECOVERY, NORMAL_RECOVERY);
      return new Transition(
          next,
          new Rotation(
              new View(
                  tick.current().yaw() + yawDelta,
                  Mth.clamp(tick.current().pitch() + pitchStep, -MAX_HEAD_PITCH, MAX_HEAD_PITCH)),
              History.INTERPOLATE,
              View.ZERO,
              0.0F,
              true));
    }

    private static State ensureBody(State state, float currentViewYaw) {
      return state.bodyKnown()
          ? state
          : new State(true, currentViewYaw, state.pendingCameraInput());
    }
  }

  public static final class InstinctClient {
    private InstinctClient() {}

    public record State(
        boolean active,
        Pose authoritative,
        View camera,
        View requested,
        float pendingCameraInput) {}

    public record LookInput(double rawYaw, double rawPitch) {}

    public record Snapshot(
        boolean active, Pose authoritative, boolean hasCurrentView, View current) {}

    public record Tick(boolean morphPresent, View current) {}

    public record LookCapture(State state, boolean handled) {}

    public record Frame(
        State state,
        boolean applyAuthoritativeView,
        Rotation cameraRotation,
        Pose renderedPose,
        View requested,
        float cameraInput) {}

    public static State initial() {
      return new State(false, Pose.ZERO, View.ZERO, View.ZERO, 0.0F);
    }

    public static LookCapture reduce(State state, LookInput input) {
      float yawDelta = (float) input.rawYaw() * RAW_LOOK_SCALE;
      float pitchDelta = (float) input.rawPitch() * RAW_LOOK_SCALE;
      if (!Float.isFinite(yawDelta) || !Float.isFinite(pitchDelta)) {
        return new LookCapture(state, state.active());
      }

      float accumulated = accumulateCameraInput(state.pendingCameraInput(), yawDelta, pitchDelta);
      if (!state.active()) {
        return new LookCapture(
            new State(false, state.authoritative(), state.camera(), state.requested(), accumulated),
            false);
      }

      float currentYawOffset =
          Mth.wrapDegrees(state.requested().yaw() - state.authoritative().bodyYaw());
      float wantedYawOffset = Mth.wrapDegrees(currentYawOffset + yawDelta);
      if (Math.abs(wantedYawOffset) > MAX_HEAD_YAW
          && Math.abs(wantedYawOffset) > Math.abs(currentYawOffset)) {
        yawDelta = 0.0F;
      }
      float wantedPitch = state.requested().pitch() + pitchDelta;
      if (Math.abs(wantedPitch) > MAX_HEAD_PITCH
          && Math.abs(wantedPitch) > Math.abs(state.requested().pitch())) {
        pitchDelta = 0.0F;
      }
      View requested =
          new View(
              state.requested().yaw() + yawDelta,
              Mth.clamp(state.requested().pitch() + pitchDelta, -MAX_HEAD_PITCH, MAX_HEAD_PITCH));
      return new LookCapture(
          new State(true, state.authoritative(), state.camera(), requested, accumulated), true);
    }

    public static State reduce(State state, Snapshot snapshot) {
      boolean entering = snapshot.active() && !state.active() && snapshot.hasCurrentView();
      View camera =
          entering
              ? new View(
                  snapshot.current().yaw(),
                  Mth.clamp(snapshot.current().pitch(), -MAX_HEAD_PITCH, MAX_HEAD_PITCH))
              : state.camera();
      View requested = entering ? camera : state.requested();
      Pose authoritative =
          new Pose(
              state.authoritative().bodyYaw()
                  + Mth.wrapDegrees(
                      snapshot.authoritative().bodyYaw() - state.authoritative().bodyYaw()),
              state.authoritative().headYaw()
                  + Mth.wrapDegrees(
                      snapshot.authoritative().headYaw() - state.authoritative().headYaw()),
              snapshot.authoritative().headPitch());
      if (!snapshot.active()) {
        camera = View.ZERO;
        requested = View.ZERO;
      }
      return new State(
          snapshot.active(), authoritative, camera, requested, state.pendingCameraInput());
    }

    public static Frame reduce(State state, Tick tick) {
      float cameraInput = state.pendingCameraInput();
      State drained =
          new State(state.active(), state.authoritative(), state.camera(), state.requested(), 0.0F);
      if (!state.active() || !tick.morphPresent()) {
        return new Frame(
            drained, false, Rotation.NONE, state.authoritative(), tick.current(), cameraInput);
      }

      View camera =
          new View(
              approachYaw(
                  state.camera().yaw(), state.authoritative().headYaw(), INSTINCT_CAMERA_FOLLOW),
              Mth.approach(
                  state.camera().pitch(),
                  state.authoritative().headPitch(),
                  INSTINCT_CAMERA_FOLLOW));
      View requested =
          blocksRecovery(cameraInput)
              ? state.requested()
              : new View(state.authoritative().headYaw(), state.authoritative().headPitch());
      Pose rendered =
          new Pose(
              state.authoritative().bodyYaw(),
              clampHeadYawToBody(state.authoritative().headYaw(), state.authoritative().bodyYaw()),
              state.authoritative().headPitch());
      Rotation rotation =
          new Rotation(
              new View(
                  tick.current().yaw() + Mth.wrapDegrees(camera.yaw() - tick.current().yaw()),
                  camera.pitch()),
              History.INTERPOLATE,
              View.ZERO,
              0.0F,
              true);
      State next = new State(true, state.authoritative(), camera, requested, 0.0F);
      return new Frame(next, true, rotation, rendered, requested, cameraInput);
    }
  }

  public static final class InstinctServer {
    private InstinctServer() {}

    public record State(Pose pose) {}

    public sealed interface Event
        permits RestoreBodyPreservingOffset,
            RestoreBodyKeepingHead,
            TurnRestingBody,
            FaceRabbitMotion,
            ResolveAuthority,
            TrackFeedingTarget {}

    public record RestoreBodyPreservingOffset(float bodyYaw) implements Event {}

    public record RestoreBodyKeepingHead(float bodyYaw) implements Event {}

    public record TurnRestingBody(float startingBodyYaw, float sideways) implements Event {}

    public record FaceRabbitMotion(float startingBodyYaw, float motionYaw) implements Event {}

    public record ResolveAuthority(
        Pose previous, View requested, float cameraInput, boolean lookingAtTarget)
        implements Event {}

    public record TrackFeedingTarget(View target) implements Event {}

    public static State reduce(State state, Event event) {
      if (event instanceof RestoreBodyPreservingOffset restore) {
        return restoreBodyPreservingOffset(state, restore.bodyYaw());
      }
      if (event instanceof RestoreBodyKeepingHead restore) {
        return restoreBodyKeepingHead(state, restore.bodyYaw());
      }
      if (event instanceof TurnRestingBody turn) {
        Pose pose = state.pose();
        float bodyYaw =
            Math.abs(turn.sideways()) >= 0.2F
                ? Mth.wrapDegrees(turn.startingBodyYaw() + turn.sideways() * NORMAL_BODY_TURN)
                : turn.startingBodyYaw();
        return new State(new Pose(bodyYaw, pose.headYaw(), pose.headPitch()));
      }
      if (event instanceof FaceRabbitMotion motion) {
        float bodyYaw =
            approachYaw(motion.startingBodyYaw(), motion.motionYaw(), RABBIT_BODY_FOLLOW);
        return restoreBodyPreservingOffset(state, bodyYaw);
      }
      if (event instanceof ResolveAuthority resolve) {
        Pose nativePose = state.pose();
        Pose previous = resolve.previous();
        float bodyYaw = approachYaw(previous.bodyYaw(), nativePose.bodyYaw(), INSTINCT_BODY_FOLLOW);
        boolean directCameraInput = blocksRecovery(resolve.cameraInput());
        float desiredHeadYaw =
            resolve.lookingAtTarget()
                ? nativePose.headYaw()
                : directCameraInput ? resolve.requested().yaw() : bodyYaw;
        float desiredHeadPitch =
            resolve.lookingAtTarget()
                ? nativePose.headPitch()
                : directCameraInput ? resolve.requested().pitch() : 0.0F;
        float headStep =
            resolve.lookingAtTarget() || directCameraInput
                ? INSTINCT_HEAD_FOLLOW
                : INSTINCT_HEAD_RECOVERY;
        float headYaw = approachYaw(previous.headYaw(), desiredHeadYaw, headStep);
        headYaw = clampHeadYawToBody(headYaw, bodyYaw);
        float headPitch =
            Mth.approach(
                previous.headPitch(),
                Mth.clamp(desiredHeadPitch, -MAX_HEAD_PITCH, MAX_HEAD_PITCH),
                headStep);
        return new State(new Pose(bodyYaw, headYaw, headPitch));
      }
      if (event instanceof TrackFeedingTarget feeding) {
        Pose pose = state.pose();
        float headYaw = approachYaw(pose.headYaw(), feeding.target().yaw(), FEEDING_HEAD_FOLLOW);
        headYaw = clampHeadYawToBody(headYaw, pose.bodyYaw());
        float headPitch =
            Mth.approach(
                pose.headPitch(),
                Mth.clamp(feeding.target().pitch(), -MAX_HEAD_PITCH, MAX_HEAD_PITCH),
                FEEDING_HEAD_FOLLOW);
        return new State(new Pose(pose.bodyYaw(), headYaw, headPitch));
      }
      throw new IllegalArgumentException("Unknown Instinct server view event " + event);
    }

    private static State restoreBodyPreservingOffset(State state, float bodyYaw) {
      Pose pose = state.pose();
      float headOffset = Mth.wrapDegrees(pose.headYaw() - pose.bodyYaw());
      return new State(
          new Pose(bodyYaw, clampHeadYawToBody(bodyYaw + headOffset, bodyYaw), pose.headPitch()));
    }

    private static State restoreBodyKeepingHead(State state, float bodyYaw) {
      Pose pose = state.pose();
      return new State(
          new Pose(bodyYaw, clampHeadYawToBody(pose.headYaw(), bodyYaw), pose.headPitch()));
    }
  }

  private static float finiteRawDelta(double rawInput) {
    float delta = (float) rawInput * RAW_LOOK_SCALE;
    return Float.isFinite(delta) ? delta : 0.0F;
  }

  private static float accumulateCameraInput(
      float accumulatedInput, float yawDelta, float pitchDelta) {
    if (!Float.isFinite(yawDelta) || !Float.isFinite(pitchDelta)) {
      return accumulatedInput;
    }
    return Mth.clamp(
        accumulatedInput + Math.abs(yawDelta) + Math.abs(pitchDelta),
        0.0F,
        MAX_ACCUMULATED_CAMERA_INPUT);
  }

  private static boolean blocksRecovery(float cameraInput) {
    return Float.isFinite(cameraInput) && cameraInput >= CAMERA_INPUT_THRESHOLD;
  }

  private static float approachYaw(float currentYaw, float targetYaw, float maximumChange) {
    return Mth.approachDegrees(currentYaw, targetYaw, maximumChange);
  }

  private static float clampHeadYawToBody(float headYaw, float bodyYaw) {
    return bodyYaw + Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -MAX_HEAD_YAW, MAX_HEAD_YAW);
  }
}
