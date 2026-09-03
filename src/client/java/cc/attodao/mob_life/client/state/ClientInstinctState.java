package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.gameplay.instinct.InstinctInput;
import cc.attodao.mob_life.gameplay.instinct.InstinctState;
import cc.attodao.mob_life.network.MobLifeNetworking;
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
import net.minecraft.world.phys.Vec2;

public final class ClientInstinctState {
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
  private static float cameraYawOffset;
  private static float cameraPitchOffset;
  private static float pendingCameraDelta;
  private static double authoritativeX;
  private static double authoritativeY;
  private static double authoritativeZ;
  private static boolean authoritativeOnGround;
  private static boolean hasAuthoritativePosition;

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

    float previousYawOffset = cameraYawOffset;
    float previousPitchOffset = cameraPitchOffset;
    cameraYawOffset = Mth.clamp(previousYawOffset + yawDelta, -75.0F, 75.0F);
    cameraPitchOffset = Mth.clamp(previousPitchOffset + pitchDelta, -40.0F, 40.0F);
    applyCameraInput(
        player, cameraYawOffset - previousYawOffset, cameraPitchOffset - previousPitchOffset);
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
      cameraYawOffset =
          Mth.clamp(Mth.wrapDegrees(player.getYRot() - payload.bodyYaw()), -75.0F, 75.0F);
      cameraPitchOffset = Mth.clamp(player.getXRot(), -40.0F, 40.0F);
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
    if (!active) {
      rawMovement = Vec2.ZERO;
      rawKeys = Input.EMPTY;
      cameraYawOffset = 0.0F;
      cameraPitchOffset = 0.0F;
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

    float desiredYawOffset =
        lookingAtTarget ? Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -75.0F, 75.0F) : 0.0F;
    float desiredPitch = lookingAtTarget ? headPitch : 0.0F;
    cameraYawOffset = approachCamera(cameraYawOffset, desiredYawOffset);
    cameraPitchOffset = approachCamera(cameraPitchOffset, desiredPitch);
    applyCamera(player);
    player.yBodyRot = bodyYaw;
    player.yBodyRotO = bodyYaw;
    player.setYHeadRot(Mth.clamp(headYaw, bodyYaw - 75.0F, bodyYaw + 75.0F));
    player.yHeadRotO = player.getYHeadRot();
    sendInput(client, player, consumeCameraDelta());
  }

  public static void clear() {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null) {
      InstinctState.get(player).setActive(false);
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
    cameraYawOffset = 0.0F;
    cameraPitchOffset = 0.0F;
    pendingCameraDelta = 0.0F;
    hasAuthoritativePosition = false;
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

  private static float consumeCameraDelta() {
    float result = pendingCameraDelta;
    pendingCameraDelta = 0.0F;
    return result;
  }

  private static float approachCamera(float current, float target) {
    return Mth.approach(current, target, Math.min(0.5F, Math.abs(target - current) * 0.05F));
  }

  private static void applyCamera(LocalPlayer player) {
    float yaw = bodyYaw + cameraYawOffset;
    float pitch = cameraPitchOffset;
    player.setYRot(player.getYRot() + Mth.wrapDegrees(yaw - player.getYRot()));
    player.setXRot(pitch);
  }

  private static void applyCameraInput(LocalPlayer player, float yawDelta, float pitchDelta) {
    player.setYRot(player.getYRot() + yawDelta);
    player.setXRot(player.getXRot() + pitchDelta);
    player.yRotO += yawDelta;
    player.xRotO += pitchDelta;
  }

  private static boolean isSafeScreen(Minecraft client) {
    return client.gui.screen() instanceof ChatScreen
        || client.gui.screen() instanceof PauseScreen
        || client.gui.screen() instanceof OptionsScreen
        || client.gui.screen() instanceof OptionsSubScreen;
  }
}
