package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.gameplay.instinct.InstinctState;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ClientInstinctState {
  private static final int IDLE_ENTRY_TICKS = 20 * 10;
  private static final int EXIT_HOLD_TICKS = 20 * 3;
  private static final int MANUAL_VIEW_GRACE_TICKS = 20;
  private static final float MAX_YAW_CHANGE = 4.0F;
  private static final float MAX_PITCH_CHANGE = 3.0F;
  private static final float LOCKED_VIEW_MAX_YAW_OFFSET = 14.0F;
  private static final float LOCKED_VIEW_MAX_PITCH_OFFSET = 10.0F;
  private static final float TARGET_YAW_RESPONSE = 0.2F;
  private static final float TARGET_PITCH_RESPONSE = 0.25F;

  private static boolean enabled;
  private static InstinctState state = InstinctState.REST;
  private static float desiredYaw;
  private static float desiredPitch;
  private static float viewYaw;
  private static float viewPitch;
  private static boolean viewInitialized;
  private static int manualViewTicks;
  private static int pendingInterventions;
  private static boolean jumpWasDown;
  private static int selectedSlot = -1;
  private static Vec3 nativeMovement = Vec3.ZERO;
  private static int idleTicks;
  private static boolean entryRequestSent;
  private static int exitHoldTicks;
  private static boolean exitRequestSent;

  private ClientInstinctState() {}

  public static void apply(MobLifeNetworking.InstinctControlPayload payload) {
    boolean wasEnabled = enabled;
    enabled = payload.enabled();
    InstinctState nextState = InstinctState.byOrdinal(payload.state());
    state = nextState;
    if (!wasEnabled) {
      desiredYaw = payload.targetYaw();
      desiredPitch = payload.targetPitch();
    } else {
      desiredYaw = Mth.rotLerp(TARGET_YAW_RESPONSE, desiredYaw, payload.targetYaw());
      desiredPitch = Mth.lerp(TARGET_PITCH_RESPONSE, desiredPitch, payload.targetPitch());
    }
    nativeMovement = new Vec3(payload.movementX(), payload.movementY(), payload.movementZ());
    if (enabled) {
      if (!wasEnabled) {
        resetEntryTimer();
        exitHoldTicks = 0;
        exitRequestSent = false;
      }
    } else {
      pendingInterventions = 0;
      jumpWasDown = false;
      manualViewTicks = 0;
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
        viewYaw = player.getYRot();
        viewPitch = player.getXRot();
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

  public static void recordActivity() {
    if (!enabled) {
      resetEntryTimer();
    }
  }

  public static boolean shouldRequestEntry(Minecraft client) {
    if (enabled
        || client.player == null
        || client.isPaused()
        || client.screen != null
        || ClientMorphState.morph() == null) {
      resetEntryTimer();
      return false;
    }
    if (entryRequestSent) {
      return false;
    }
    idleTicks++;
    if (idleTicks < IDLE_ENTRY_TICKS) {
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
        || client.screen != null
        || !client.options.keyAttack.isDown()) {
      exitHoldTicks = 0;
      exitRequestSent = false;
      return false;
    }
    if (exitRequestSent) {
      return false;
    }
    exitHoldTicks++;
    if (exitHoldTicks < EXIT_HOLD_TICKS) {
      return false;
    }
    exitRequestSent = true;
    return true;
  }

  public static void recordKeyboard(boolean forward, boolean jump) {
    if (!enabled) {
      jumpWasDown = jump;
      return;
    }
    if (forward && state.acceptsForward()) {
      pendingInterventions |= InstinctManager.INTERVENE_FORWARD;
    }
    if (jump && !jumpWasDown && state.acceptsJump()) {
      pendingInterventions |= InstinctManager.INTERVENE_JUMP;
    }
    jumpWasDown = jump;
  }

  public static void recordViewInput() {
    if (!enabled || !state.acceptsView()) {
      return;
    }
    manualViewTicks = MANUAL_VIEW_GRACE_TICKS;
    pendingInterventions |= InstinctManager.INTERVENE_VIEW;
  }

  public static int consumeInterventions() {
    int result = pendingInterventions;
    pendingInterventions = 0;
    return result;
  }

  public static void applyNativeMovement(LocalPlayer player) {
    if (enabled && player != null) {
      player.setDeltaMovement(nativeMovement);
    }
  }

  public static void tick(Minecraft client) {
    if (client.player == null || client.isPaused()) {
      return;
    }
    if (!enabled) {
      return;
    }
    if (manualViewTicks > 0) {
      manualViewTicks--;
    }
    LocalPlayer player = client.player;
    if (selectedSlot >= 0) {
      player.getInventory().setSelectedSlot(selectedSlot);
    }
    if (!state.locksView() && manualViewTicks > 0) {
      // Keep the interpolation anchor at the player's manual view so it does not
      // snap back to a stale server target when the grace period expires.
      viewYaw = player.getYRot();
      viewPitch = player.getXRot();
      desiredYaw = player.getYRot();
      desiredPitch = player.getXRot();
      player.setYHeadRot(player.getYRot());
      player.setYBodyRot(player.getYRot());
      return;
    }

    if (state == InstinctState.REST) {
      viewYaw = player.getYRot();
      viewPitch = player.getXRot();
      desiredYaw = player.getYRot();
      desiredPitch = player.getXRot();
      player.setYHeadRot(player.getYRot());
      player.setYBodyRot(player.getYRot());
      return;
    }

    if (!viewInitialized) {
      viewYaw = player.getYRot();
      viewPitch = player.getXRot();
      viewInitialized = true;
    }
    viewYaw = Mth.rotLerp(TARGET_YAW_RESPONSE, viewYaw, desiredYaw);
    viewPitch = Mth.lerp(TARGET_PITCH_RESPONSE, viewPitch, desiredPitch);

    float yaw =
        player.getYRot()
            + Mth.clamp(
                Mth.wrapDegrees(viewYaw - player.getYRot()), -MAX_YAW_CHANGE, MAX_YAW_CHANGE);
    float pitch =
        player.getXRot()
            + Mth.clamp(viewPitch - player.getXRot(), -MAX_PITCH_CHANGE, MAX_PITCH_CHANGE);
    if (state.locksView()) {
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
    player.setYBodyRot(yaw);
    player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
  }

  public static void clear() {
    enabled = false;
    state = InstinctState.REST;
    desiredYaw = 0.0F;
    desiredPitch = 0.0F;
    viewYaw = 0.0F;
    viewPitch = 0.0F;
    viewInitialized = false;
    manualViewTicks = 0;
    pendingInterventions = 0;
    jumpWasDown = false;
    selectedSlot = -1;
    nativeMovement = Vec3.ZERO;
    resetEntryTimer();
    exitHoldTicks = 0;
    exitRequestSent = false;
  }

  private static void resetEntryTimer() {
    idleTicks = 0;
    entryRequestSent = false;
  }
}
