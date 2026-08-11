package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.gameplay.instinct.InstinctState;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ClientInstinctState {
  private static final int VISUAL_TRANSITION_TICKS = 10;
  private static final float MAX_YAW_CHANGE = 4.0F;
  private static final float MAX_PITCH_CHANGE = 3.0F;
  private static final float MAX_BODY_YAW_CHANGE = 3.0F;
  private static final float BODY_YAW_DEAD_ZONE = 0.35F;
  private static final float LOCKED_VIEW_MAX_YAW_OFFSET = 14.0F;
  private static final float LOCKED_VIEW_MAX_PITCH_OFFSET = 10.0F;
  private static final float TARGET_PITCH_RESPONSE = 0.25F;
  private static final float MANUAL_VIEW_SENSITIVITY = 0.25F;
  private static final float VIEW_YAW_RECENTER_RESPONSE = 0.05F;
  private static final float FREE_INSTINCT_MAX_YAW_OFFSET = 30.0F;
  private static final float FREE_INSTINCT_MIN_PITCH = -30.0F;
  private static final float FREE_INSTINCT_MAX_PITCH = 30.0F;

  private static boolean enabled;
  private static InstinctState state = InstinctState.REST;
  private static float desiredYaw;
  private static float desiredPitch;
  private static float bodyYaw;
  private static float viewYawOffset;
  private static float viewPitch;
  private static boolean viewInitialized;
  private static int pendingInterventions;
  private static float pendingViewYawInput;
  private static float pendingViewPitchInput;
  private static int selectedSlot = -1;
  private static Vec3 nativeMovement = Vec3.ZERO;
  private static int idleTicks;
  private static boolean entryRequestSent;
  private static int exitHoldTicks;
  private static boolean exitRequestSent;
  private static float visualBlend;

  private ClientInstinctState() {}

  public static void apply(MobLifeNetworking.InstinctControlPayload payload) {
    boolean wasEnabled = enabled;
    enabled = payload.enabled();
    InstinctState nextState = InstinctState.byOrdinal(payload.state());
    state = nextState;
    if (!nextState.acceptsView()) {
      pendingViewYawInput = 0.0F;
      pendingViewPitchInput = 0.0F;
    }
    desiredYaw = payload.targetYaw();
    desiredPitch = payload.targetPitch();
    nativeMovement = new Vec3(payload.movementX(), payload.movementY(), payload.movementZ());
    if (enabled) {
      if (!wasEnabled) {
        resetEntryTimer();
        exitHoldTicks = 0;
        exitRequestSent = false;
      }
    } else {
      pendingInterventions = 0;
      pendingViewYawInput = 0.0F;
      pendingViewPitchInput = 0.0F;
      viewInitialized = false;
      nativeMovement = Vec3.ZERO;
      resetEntryTimer();
      exitHoldTicks = 0;
      exitRequestSent = false;
    }

    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null) {
      if (enabled && !wasEnabled) {
        selectedSlot = player.getInventory().getSelectedSlot();
        bodyYaw = payload.targetYaw();
        viewYawOffset =
            Mth.clamp(
                Mth.wrapDegrees(player.getYRot() - bodyYaw),
                -FREE_INSTINCT_MAX_YAW_OFFSET,
                FREE_INSTINCT_MAX_YAW_OFFSET);
        viewPitch = Mth.clamp(player.getXRot(), FREE_INSTINCT_MIN_PITCH, FREE_INSTINCT_MAX_PITCH);
        viewInitialized = true;
      } else if (!enabled) {
        selectedSlot = -1;
      }
      ClientMorphState.setGrassEatingTicks(player.getId(), payload.eatTicks());
    }
  }

  public static boolean enabled() {
    return enabled;
  }

  public static InstinctState state() {
    return state;
  }

  public static boolean locksView() {
    return enabled && state.locksView();
  }

  public static boolean isWandering() {
    return enabled && state == InstinctState.WANDER;
  }

  public static float visualBlend() {
    return visualBlend;
  }

  public static void recordActivity() {
    if (!enabled) {
      resetEntryTimer();
    }
  }

  public static boolean shouldRequestEntry(Minecraft client) {
    if (enabled
        || client.player == null
        || client.isPaused()
        || client.gui.screen() != null
        || ClientMorphState.morph() == null) {
      resetEntryTimer();
      return false;
    }
    if (entryRequestSent) {
      return false;
    }
    idleTicks++;
    if (idleTicks < MorphAwkwardness.instinctEntryDelayTicks(ClientMorphState.awkwardness())) {
      return false;
    }
    entryRequestSent = true;
    return true;
  }

  public static boolean shouldRequestExit(Minecraft client) {
    if (!enabled
        || !state.acceptsView()
        || client.player == null
        || client.isPaused()
        || client.gui.screen() != null
        || !client.options.keyAttack.isDown()
        || MorphAwkwardness.isMaximum(ClientMorphState.awkwardness())) {
      exitHoldTicks = 0;
      exitRequestSent = false;
      return false;
    }
    if (exitRequestSent) {
      return false;
    }
    exitHoldTicks++;
    if (exitHoldTicks < MorphAwkwardness.instinctExitHoldTicks(ClientMorphState.awkwardness())) {
      return false;
    }
    exitRequestSent = true;
    return true;
  }

  public static boolean shouldHoldRestForExit(Minecraft client) {
    return enabled
        && client.player != null
        && !client.isPaused()
        && client.gui.screen() == null
        && client.options.keyAttack.isDown()
        && !MorphAwkwardness.isMaximum(ClientMorphState.awkwardness());
  }

  public static void recordKeyboard(boolean forward, boolean left, boolean right) {
    if (!enabled) {
      return;
    }
    if (forward && (state.acceptsForward() || isWandering())) {
      pendingInterventions |= InstinctManager.INTERVENE_FORWARD;
    }
    if ((state == InstinctState.REST || isWandering()) && left != right) {
      pendingInterventions |=
          left ? InstinctManager.INTERVENE_LEFT : InstinctManager.INTERVENE_RIGHT;
    }
  }

  public static void recordViewInput(float yawDelta, float pitchDelta) {
    if (!enabled || !state.acceptsView()) {
      return;
    }
    pendingViewYawInput += yawDelta;
    pendingViewPitchInput += pitchDelta;
  }

  public static Intervention consumeInterventions() {
    Intervention result =
        new Intervention(pendingInterventions, Mth.wrapDegrees(bodyYaw + viewYawOffset));
    pendingInterventions = 0;
    return result;
  }

  public static void applyNativeMovement(LocalPlayer player) {
    if (enabled && player != null) {
      player.setDeltaMovement(nativeMovement);
    }
  }

  public static void applyView(LocalPlayer player, double frameTime) {
    if (!enabled || player == null) {
      return;
    }

    if (!viewInitialized) {
      bodyYaw = desiredYaw;
      viewYawOffset =
          Mth.clamp(
              Mth.wrapDegrees(player.getYRot() - bodyYaw),
              -FREE_INSTINCT_MAX_YAW_OFFSET,
              FREE_INSTINCT_MAX_YAW_OFFSET);
      viewPitch = Mth.clamp(player.getXRot(), FREE_INSTINCT_MIN_PITCH, FREE_INSTINCT_MAX_PITCH);
      viewInitialized = true;
    }
    float frameTicks = (float) Math.clamp(frameTime * 20.0, 0.0, 3.0);
    bodyYaw = approachBodyYaw(bodyYaw, desiredYaw, MAX_BODY_YAW_CHANGE * frameTicks);

    float yaw;
    float pitch;
    if (state.acceptsView()) {
      if (Math.abs(pendingViewYawInput) > 1.0E-4F) {
        viewYawOffset =
            Mth.clamp(
                viewYawOffset + pendingViewYawInput * MANUAL_VIEW_SENSITIVITY,
                -FREE_INSTINCT_MAX_YAW_OFFSET,
                FREE_INSTINCT_MAX_YAW_OFFSET);
      } else {
        float recenterResponse = perFrameResponse(VIEW_YAW_RECENTER_RESPONSE, frameTicks);
        viewYawOffset = Mth.rotLerp(recenterResponse, viewYawOffset, 0.0F);
      }
      pendingViewYawInput = 0.0F;
      viewPitch =
          Mth.clamp(
              viewPitch + pendingViewPitchInput * MANUAL_VIEW_SENSITIVITY,
              FREE_INSTINCT_MIN_PITCH,
              FREE_INSTINCT_MAX_PITCH);
      pendingViewPitchInput = 0.0F;
      yaw = Mth.wrapDegrees(bodyYaw + viewYawOffset);
      pitch = viewPitch;
    } else {
      yaw =
          player.getYRot()
              + Mth.clamp(
                  Mth.wrapDegrees(bodyYaw - player.getYRot()),
                  -MAX_YAW_CHANGE * frameTicks,
                  MAX_YAW_CHANGE * frameTicks);
      float pitchResponse = perFrameResponse(TARGET_PITCH_RESPONSE, frameTicks);
      viewPitch = Mth.lerp(pitchResponse, viewPitch, desiredPitch);
      pitch =
          player.getXRot()
              + Mth.clamp(
                  viewPitch - player.getXRot(),
                  -MAX_PITCH_CHANGE * frameTicks,
                  MAX_PITCH_CHANGE * frameTicks);
      yaw =
          desiredYaw
              + Mth.clamp(
                  Mth.wrapDegrees(yaw - desiredYaw),
                  -LOCKED_VIEW_MAX_YAW_OFFSET,
                  LOCKED_VIEW_MAX_YAW_OFFSET);
      pitch =
          Mth.clamp(
              pitch,
              Math.max(-90.0F, desiredPitch - LOCKED_VIEW_MAX_PITCH_OFFSET),
              Math.min(90.0F, desiredPitch + LOCKED_VIEW_MAX_PITCH_OFFSET));
    }
    player.setYRot(yaw);
    player.setYHeadRot(yaw);
    player.setYBodyRot(bodyYaw);
    player.setXRot(pitch);
  }

  public static void tick(Minecraft client) {
    float targetBlend = enabled ? 1.0F : 0.0F;
    float step = 1.0F / VISUAL_TRANSITION_TICKS;
    visualBlend =
        targetBlend > visualBlend
            ? Math.min(targetBlend, visualBlend + step)
            : Math.max(targetBlend, visualBlend - step);
    if (client.player == null || client.isPaused()) {
      return;
    }
    if (!enabled) {
      return;
    }
    if (selectedSlot >= 0) {
      client.player.getInventory().setSelectedSlot(selectedSlot);
    }
  }

  public static void clear() {
    enabled = false;
    state = InstinctState.REST;
    desiredYaw = 0.0F;
    desiredPitch = 0.0F;
    bodyYaw = 0.0F;
    viewYawOffset = 0.0F;
    viewPitch = 0.0F;
    viewInitialized = false;
    pendingInterventions = 0;
    pendingViewYawInput = 0.0F;
    pendingViewPitchInput = 0.0F;
    selectedSlot = -1;
    nativeMovement = Vec3.ZERO;
    visualBlend = 0.0F;
    resetEntryTimer();
    exitHoldTicks = 0;
    exitRequestSent = false;
  }

  private static void resetEntryTimer() {
    idleTicks = 0;
    entryRequestSent = false;
  }

  private static float perFrameResponse(float perTickResponse, float frameTicks) {
    return 1.0F - (float) Math.pow(1.0F - perTickResponse, frameTicks);
  }

  private static float approachBodyYaw(float currentYaw, float targetYaw, float maximumChange) {
    float difference = Mth.wrapDegrees(targetYaw - currentYaw);
    if (Math.abs(difference) <= BODY_YAW_DEAD_ZONE) {
      return currentYaw;
    }
    return Mth.wrapDegrees(currentYaw + Mth.clamp(difference, -maximumChange, maximumChange));
  }

  public record Intervention(int flags, float viewYaw) {}
}
