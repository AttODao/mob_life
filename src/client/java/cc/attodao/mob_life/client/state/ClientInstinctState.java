package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.gameplay.instinct.InstinctState;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ClientInstinctState {
  private static final int VISUAL_TRANSITION_TICKS = 10;
  private static final int INTERVENTION_VISUAL_TRANSITION_TICKS = 5;
  private static final float MAX_YAW_CHANGE = 4.0F;
  private static final float MAX_PITCH_CHANGE = 3.0F;
  private static final float MAX_BODY_YAW_CHANGE = 3.0F;
  private static final float RABBIT_MAX_BODY_YAW_CHANGE = 6.0F;
  private static final float RABBIT_LOCKED_VIEW_MAX_YAW_CHANGE = 8.0F;
  private static final float RABBIT_LOCKED_VIEW_MAX_PITCH_CHANGE = 6.0F;
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
  private static int heldEscapeInputs;
  private static int pendingEscapeInputs;
  private static int pendingInterventions;
  private static float pendingViewYawInput;
  private static float pendingViewPitchInput;
  private static int selectedSlot = -1;
  private static Vec3 nativeMovement = Vec3.ZERO;
  private static float visualBlend;
  private static float instinctLevel = InstinctManager.MAXIMUM_LEVEL;
  private static boolean playerInterventionAllowed;
  private static float interventionBlockedVisualBlend;

  private ClientInstinctState() {}

  public static void apply(MobLifeNetworking.InstinctControlPayload payload) {
    boolean wasEnabled = enabled;
    enabled = payload.enabled();
    InstinctState nextState = InstinctState.byOrdinal(payload.state());
    state = nextState;
    if (!nextState.allowsEscape()) {
      pendingEscapeInputs = 0;
    }
    if (!nextState.acceptsView()) {
      pendingViewYawInput = 0.0F;
      pendingViewPitchInput = 0.0F;
    }
    desiredYaw = payload.targetYaw();
    desiredPitch = payload.targetPitch();
    instinctLevel =
        Float.isFinite(payload.instinctLevel())
            ? Mth.clamp(payload.instinctLevel(), 0.0F, InstinctManager.MAXIMUM_LEVEL)
            : InstinctManager.MAXIMUM_LEVEL;
    playerInterventionAllowed = payload.playerInterventionAllowed();
    Minecraft client = Minecraft.getInstance();
    nativeMovement = new Vec3(payload.movementX(), payload.movementY(), payload.movementZ());
    if (enabled && !wasEnabled) {
      heldEscapeInputs = currentHeldEscapeInputs(client);
      pendingEscapeInputs = 0;
    }
    if (!enabled) {
      pendingInterventions = 0;
      pendingViewYawInput = 0.0F;
      pendingViewPitchInput = 0.0F;
      viewInitialized = false;
      heldEscapeInputs = 0;
      pendingEscapeInputs = 0;
      nativeMovement = Vec3.ZERO;
      playerInterventionAllowed = false;
    }

    LocalPlayer player = client.player;
    if (player != null) {
      if (enabled && !wasEnabled) {
        selectedSlot = player.getInventory().getSelectedSlot();
        bodyYaw = isRabbitMorph() ? player.getYRot() : payload.targetYaw();
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

  public static float visualBlend() {
    return visualBlend;
  }

  public static float instinctLevelRatio() {
    return Mth.clamp(instinctLevel / InstinctManager.MAXIMUM_LEVEL, 0.0F, 1.0F);
  }

  public static float interventionBlockedVisualBlend() {
    return interventionBlockedVisualBlend;
  }

  public static void recordEscapeAction(int input, boolean held, boolean pressed) {
    int validInput = input & InstinctManager.ESCAPE_ALL;
    if (validInput == 0) {
      return;
    }
    boolean wasHeld = (heldEscapeInputs & validInput) != 0;
    if (held) {
      heldEscapeInputs |= validInput;
    } else {
      heldEscapeInputs &= ~validInput;
    }
    if (enabled && state.allowsEscape() && (pressed || held && !wasHeld)) {
      pendingEscapeInputs |= validInput;
    }
  }

  public static int consumeEscapeInputs(Minecraft client) {
    int inputs =
        enabled
                && state.allowsEscape()
                && client.player != null
                && !client.isPaused()
                && client.gui.screen() == null
            ? pendingEscapeInputs
            : 0;
    pendingEscapeInputs = 0;
    return inputs;
  }

  public static void recordMovement(boolean forward, boolean left, boolean right) {
    if (!enabled || !playerInterventionAllowed) {
      return;
    }
    if (forward) {
      pendingInterventions |= InstinctManager.INTERVENE_FORWARD;
    }
    if (left != right) {
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
      bodyYaw = isRabbitMorph() ? player.getYRot() : desiredYaw;
      viewYawOffset =
          Mth.clamp(
              Mth.wrapDegrees(player.getYRot() - bodyYaw),
              -FREE_INSTINCT_MAX_YAW_OFFSET,
              FREE_INSTINCT_MAX_YAW_OFFSET);
      viewPitch = Mth.clamp(player.getXRot(), FREE_INSTINCT_MIN_PITCH, FREE_INSTINCT_MAX_PITCH);
      viewInitialized = true;
    }
    float frameTicks = (float) Math.clamp(frameTime * 20.0, 0.0, 3.0);
    float bodyYawChange =
        (isRabbitMorph() ? RABBIT_MAX_BODY_YAW_CHANGE : MAX_BODY_YAW_CHANGE) * frameTicks;
    bodyYaw = approachBodyYaw(bodyYaw, desiredYaw, bodyYawChange);

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
    } else if (isRabbitMorph()) {
      yaw =
          approachYaw(player.getYRot(), desiredYaw, RABBIT_LOCKED_VIEW_MAX_YAW_CHANGE * frameTicks);
      pitch =
          approachPitch(
              player.getXRot(), desiredPitch, RABBIT_LOCKED_VIEW_MAX_PITCH_CHANGE * frameTicks);
      viewPitch = pitch;
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
    float interventionStep = 1.0F / INTERVENTION_VISUAL_TRANSITION_TICKS;
    float targetInterventionBlockedBlend = enabled && !playerInterventionAllowed ? 1.0F : 0.0F;
    interventionBlockedVisualBlend =
        targetInterventionBlockedBlend > interventionBlockedVisualBlend
            ? Math.min(
                targetInterventionBlockedBlend, interventionBlockedVisualBlend + interventionStep)
            : Math.max(
                targetInterventionBlockedBlend, interventionBlockedVisualBlend - interventionStep);
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
    heldEscapeInputs = 0;
    pendingEscapeInputs = 0;
    pendingInterventions = 0;
    pendingViewYawInput = 0.0F;
    pendingViewPitchInput = 0.0F;
    selectedSlot = -1;
    nativeMovement = Vec3.ZERO;
    visualBlend = 0.0F;
    instinctLevel = InstinctManager.MAXIMUM_LEVEL;
    playerInterventionAllowed = false;
    interventionBlockedVisualBlend = 0.0F;
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

  private static float approachYaw(float currentYaw, float targetYaw, float maximumChange) {
    return Mth.wrapDegrees(
        currentYaw
            + Mth.clamp(Mth.wrapDegrees(targetYaw - currentYaw), -maximumChange, maximumChange));
  }

  private static float approachPitch(float currentPitch, float targetPitch, float maximumChange) {
    return Mth.clamp(
        currentPitch + Mth.clamp(targetPitch - currentPitch, -maximumChange, maximumChange),
        -90.0F,
        90.0F);
  }

  private static boolean isRabbitMorph() {
    return ClientMorphState.morph() == MorphType.RABBIT;
  }

  private static int currentHeldEscapeInputs(Minecraft client) {
    int inputs = 0;
    if (client.options.keyAttack.isDown()) {
      inputs |= InstinctManager.ESCAPE_ATTACK;
    }
    if (client.options.keyUse.isDown()) {
      inputs |= InstinctManager.ESCAPE_USE;
    }
    if (client.options.keyJump.isDown()) {
      inputs |= InstinctManager.ESCAPE_JUMP;
    }
    return inputs;
  }

  public record Intervention(int flags, float viewYaw) {}
}
