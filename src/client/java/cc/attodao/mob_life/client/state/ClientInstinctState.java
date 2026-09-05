package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.gameplay.instinct.InstinctInput;
import cc.attodao.mob_life.gameplay.instinct.InstinctState;
import cc.attodao.mob_life.network.MobLifeNetworking;
import java.util.ArrayDeque;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

public final class ClientInstinctState {
  private static final int MAX_VIEW_BOB_SAMPLES = 5;
  private static final int MAX_VIEW_BOB_SAMPLES_PER_TICK = 2;
  private static final int VIEW_BOB_SPEED_HOLD_TICKS = 5;
  private static final float VANILLA_WALK_DISTANCE_SCALE = 0.6F;
  private static final float VANILLA_MAX_BOB_SPEED = 0.1F;
  private static final ArrayDeque<ViewBobSample> VIEW_BOB_SAMPLES = new ArrayDeque<>();
  private static Vec2 rawMovement = Vec2.ZERO;
  private static Input rawKeys = Input.EMPTY;
  private static boolean active;
  private static float level;
  private static float bodyYaw;
  private static float headYaw;
  private static float headPitch;
  private static int activity;
  private static boolean lookingAtTarget;
  private static int lockedHotbarSlot;
  private static float cameraYaw;
  private static float cameraPitch;
  private static float pendingCameraDelta;
  private static double authoritativeX;
  private static double authoritativeY;
  private static double authoritativeZ;
  private static boolean authoritativeOnGround;
  private static boolean hasAuthoritativePosition;
  private static float lastViewBobSpeed;
  private static int viewBobTicksWithoutSample = VIEW_BOB_SPEED_HOLD_TICKS + 1;
  private static LocalPlayer viewBobPlayer;
  private static Level viewBobLevel;
  private static boolean viewBobPlayerWasDead;

  private ClientInstinctState() {}

  public static boolean active() {
    return active;
  }

  public static float level() {
    return level;
  }

  public static int activity() {
    return activity;
  }

  public static void capture(ClientInput input) {
    rawMovement = input.getMoveVector();
    rawKeys = input.keyPresses;
  }

  public static boolean captureLookInput(
      LocalPlayer player, double rawYawInput, double rawPitchInput) {
    float yawDelta = (float) rawYawInput * 0.15F;
    float pitchDelta = (float) rawPitchInput * 0.15F;
    if (!Float.isFinite(yawDelta) || !Float.isFinite(pitchDelta)) {
      return active;
    }
    pendingCameraDelta =
        Mth.clamp(pendingCameraDelta + Math.abs(yawDelta) + Math.abs(pitchDelta), 0.0F, 360.0F);
    if (!active) {
      return false;
    }

    float currentYawOffset = Mth.wrapDegrees(cameraYaw - bodyYaw);
    float wantedYawOffset = Mth.wrapDegrees(currentYawOffset + yawDelta);
    if (Math.abs(wantedYawOffset) > 75.0F
        && Math.abs(wantedYawOffset) > Math.abs(currentYawOffset)) {
      yawDelta = 0.0F;
    }
    float wantedPitch = cameraPitch + pitchDelta;
    if (Math.abs(wantedPitch) > 40.0F && Math.abs(wantedPitch) > Math.abs(cameraPitch)) {
      pitchDelta = 0.0F;
    }
    cameraYaw += yawDelta;
    cameraPitch = Mth.clamp(cameraPitch + pitchDelta, -40.0F, 40.0F);
    applyImmediateCamera(player);
    return true;
  }

  public static void update(MobLifeNetworking.InstinctStatePayload payload) {
    Minecraft client = Minecraft.getInstance();
    LocalPlayer player = client.player;
    boolean wasActive = active;
    active = payload.active();
    if (player != null) {
      InstinctState.get(player).setActive(active);
    }
    if (active && !wasActive && player != null) {
      lockedHotbarSlot = player.getInventory().getSelectedSlot();
      cameraYaw = player.getYRot();
      cameraPitch = Mth.clamp(player.getXRot(), -40.0F, 40.0F);
      viewBobPlayer = player;
      viewBobLevel = player.level();
      viewBobPlayerWasDead = player.isDeadOrDying();
    }
    level = Mth.clamp(payload.level(), 0.0F, 100.0F);
    bodyYaw += Mth.wrapDegrees(payload.bodyYaw() - bodyYaw);
    headYaw += Mth.wrapDegrees(payload.headYaw() - headYaw);
    headPitch = payload.headPitch();
    authoritativeX = payload.x();
    authoritativeY = payload.y();
    authoritativeZ = payload.z();
    authoritativeOnGround = payload.onGround();
    hasAuthoritativePosition = active;
    lookingAtTarget = payload.lookingAtTarget();
    activity = payload.activity();
    if (active != wasActive) {
      ClientMorphState.resetLocomotion();
      clearViewBobSamples();
    }
    if (active && !ClientMorphState.rabbitHopEnabled()) {
      enqueueViewBobSample(payload.horizontalDisplacement(), payload.horizontalSpeed());
    }
    if (!active) {
      rawMovement = Vec2.ZERO;
      rawKeys = Input.EMPTY;
      cameraYaw = 0.0F;
      cameraPitch = 0.0F;
      hasAuthoritativePosition = false;
    }
  }

  public static void tick(Minecraft client) {
    LocalPlayer player = client.player;
    if (player == null) {
      return;
    }
    if (!active || ClientMorphState.morph() == null) {
      sendInput(client, player, consumeCameraDelta());
      return;
    }

    client.options.setCameraType(CameraType.FIRST_PERSON);
    player.getInventory().setSelectedSlot(lockedHotbarSlot);
    if (client.gui.screen() != null && !isSafeScreen(client)) {
      client.gui.setScreen(null);
    }

    cameraYaw = Mth.rotateIfNecessary(cameraYaw, headYaw, 30.0F);
    cameraPitch = Mth.approach(cameraPitch, headPitch, 30.0F);
    applyInterpolatedCamera(player);
    player.yBodyRot = bodyYaw;
    player.yBodyRotO = bodyYaw;
    player.setYHeadRot(Mth.clamp(headYaw, bodyYaw - 75.0F, bodyYaw + 75.0F));
    player.yHeadRotO = player.getYHeadRot();
    sendInput(client, player, consumeCameraDelta());
  }

  public static void clear() {
    LocalPlayer player = Minecraft.getInstance().player;
    boolean resetViewBob = active || ClientMorphState.rabbitHopEnabled();
    if (player != null) {
      InstinctState.get(player).setActive(false);
      if (resetViewBob) {
        resetViewBobImmediately(player);
      }
    }
    active = false;
    level = 0.0F;
    bodyYaw = 0.0F;
    headYaw = 0.0F;
    headPitch = 0.0F;
    activity = 0;
    lookingAtTarget = false;
    rawMovement = Vec2.ZERO;
    rawKeys = Input.EMPTY;
    cameraYaw = 0.0F;
    cameraPitch = 0.0F;
    pendingCameraDelta = 0.0F;
    hasAuthoritativePosition = false;
    clearViewBobSamples();
    viewBobPlayer = null;
    viewBobLevel = null;
    viewBobPlayerWasDead = false;
  }

  public static boolean updateViewBob(LocalPlayer player) {
    boolean rabbitHop = ClientMorphState.rabbitHopEnabled();
    if (viewBobPlayer != null || active || rabbitHop) {
      trackViewBobLifecycle(player);
    }
    if (!active && !rabbitHop) {
      return false;
    }

    if (rabbitHop) {
      clearViewBobSamples();
      resetViewBobImmediately(player);
      return true;
    }

    float walkedDistance = 0.0F;
    ViewBobSample newest = null;
    for (int consumed = 0;
        consumed < MAX_VIEW_BOB_SAMPLES_PER_TICK && !VIEW_BOB_SAMPLES.isEmpty();
        consumed++) {
      newest = VIEW_BOB_SAMPLES.removeFirst();
      walkedDistance += newest.horizontalDisplacement();
    }
    if (walkedDistance > 0.0F) {
      player.avatarState().addWalkDistance(walkedDistance * VANILLA_WALK_DISTANCE_SCALE);
    }
    if (newest != null) {
      lastViewBobSpeed = newest.horizontalSpeed();
      viewBobTicksWithoutSample = 0;
    } else if (++viewBobTicksWithoutSample > VIEW_BOB_SPEED_HOLD_TICKS) {
      lastViewBobSpeed = 0.0F;
    }

    float target =
        player.onGround()
                && !player.isDeadOrDying()
                && !player.isSwimming()
                && !player.isPassenger()
            ? Math.min(VANILLA_MAX_BOB_SPEED, lastViewBobSpeed)
            : 0.0F;
    player.avatarState().updateBob(target);
    return true;
  }

  public static boolean freezesViewBobWalkDistance() {
    return ClientMorphState.rabbitHopEnabled();
  }

  public static void applyAuthoritativePosition(LocalPlayer player) {
    if (!active || !hasAuthoritativePosition || player.isPassenger()) {
      return;
    }
    player.setPos(authoritativeX, authoritativeY, authoritativeZ);
    player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    player.setOnGround(authoritativeOnGround);
  }

  private static void sendInput(Minecraft client, LocalPlayer player, float cameraDelta) {
    int buttons = 0;
    buttons |= rawKeys.jump() ? InstinctInput.JUMP : 0;
    buttons |= client.options.keyAttack.isDown() ? InstinctInput.ATTACK : 0;
    buttons |= client.options.keyUse.isDown() ? InstinctInput.USE : 0;
    buttons |= client.options.keyInventory.isDown() ? InstinctInput.INVENTORY : 0;
    buttons |= rawKeys.sprint() ? InstinctInput.SPRINT : 0;
    buttons |= rawKeys.shift() ? InstinctInput.SNEAK : 0;
    buttons |= client.options.keyDrop.isDown() ? InstinctInput.DROP : 0;
    buttons |= client.options.keySwapOffhand.isDown() ? InstinctInput.SWAP : 0;
    buttons |= client.options.keyTogglePerspective.isDown() ? InstinctInput.PERSPECTIVE : 0;
    buttons |=
        active && player.getInventory().getSelectedSlot() != lockedHotbarSlot
            ? InstinctInput.HOTBAR
            : 0;
    int screenMode =
        client.gui.screen() == null
            ? InstinctInput.SCREEN_NONE
            : isSafeScreen(client) ? InstinctInput.SCREEN_SAFE : InstinctInput.SCREEN_GAMEPLAY;
    ClientPlayNetworking.send(
        new MobLifeNetworking.InstinctInputPayload(
            -rawMovement.x,
            rawMovement.y,
            player.getYRot(),
            player.getXRot(),
            cameraDelta,
            buttons,
            screenMode));
  }

  private static void enqueueViewBobSample(float horizontalDisplacement, float horizontalSpeed) {
    if (VIEW_BOB_SAMPLES.size() >= MAX_VIEW_BOB_SAMPLES) {
      VIEW_BOB_SAMPLES.removeFirst();
    }
    VIEW_BOB_SAMPLES.addLast(new ViewBobSample(horizontalDisplacement, horizontalSpeed));
  }

  private static void clearViewBobSamples() {
    VIEW_BOB_SAMPLES.clear();
    lastViewBobSpeed = 0.0F;
    viewBobTicksWithoutSample = VIEW_BOB_SPEED_HOLD_TICKS + 1;
  }

  private static void trackViewBobLifecycle(LocalPlayer player) {
    boolean dead = player.isDeadOrDying();
    if (viewBobPlayer != player
        || viewBobLevel != player.level()
        || dead && !viewBobPlayerWasDead) {
      clearViewBobSamples();
      resetViewBobImmediately(player);
    }
    viewBobPlayer = player;
    viewBobLevel = player.level();
    viewBobPlayerWasDead = dead;
  }

  private static void resetViewBobImmediately(LocalPlayer player) {
    player.avatarState().resetBob();
    player.avatarState().resetBob();
  }

  private static float consumeCameraDelta() {
    float result = pendingCameraDelta;
    pendingCameraDelta = 0.0F;
    return result;
  }

  private static void applyInterpolatedCamera(LocalPlayer player) {
    float yawDelta = Mth.wrapDegrees(cameraYaw - player.getYRot());
    player.setYRot(player.getYRot() + yawDelta);
    player.setXRot(cameraPitch);
  }

  private static void applyImmediateCamera(LocalPlayer player) {
    float yawDelta = Mth.wrapDegrees(cameraYaw - player.getYRot());
    float pitchDelta = cameraPitch - player.getXRot();
    applyInterpolatedCamera(player);
    player.yRotO += yawDelta;
    player.xRotO += pitchDelta;
  }

  private static boolean isSafeScreen(Minecraft client) {
    return client.gui.screen() instanceof ChatScreen
        || client.gui.screen() instanceof PauseScreen
        || client.gui.screen() instanceof OptionsScreen
        || client.gui.screen() instanceof OptionsSubScreen;
  }

  private record ViewBobSample(float horizontalDisplacement, float horizontalSpeed) {}
}
