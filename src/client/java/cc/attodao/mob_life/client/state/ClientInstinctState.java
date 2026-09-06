package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.gameplay.instinct.InstinctInput;
import cc.attodao.mob_life.gameplay.instinct.InstinctState;
import cc.attodao.mob_life.gameplay.instinct.InstinctSyncState;
import cc.attodao.mob_life.gameplay.view.MorphViewControl;
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
  private static MorphViewControl.InstinctClient.State viewState =
      MorphViewControl.InstinctClient.initial();
  private static InstinctSyncState syncState = InstinctSyncState.INACTIVE;
  private static int lockedHotbarSlot;
  private static float lastViewBobSpeed;
  private static int viewBobTicksWithoutSample = VIEW_BOB_SPEED_HOLD_TICKS + 1;
  private static LocalPlayer viewBobPlayer;
  private static Level viewBobLevel;
  private static boolean viewBobPlayerWasDead;

  private ClientInstinctState() {}

  public static boolean active() {
    return viewState.active();
  }

  public static float level() {
    return syncState.level();
  }

  public static int activity() {
    return syncState.activity().ordinal();
  }

  public static void capture(ClientInput input) {
    rawMovement = input.getMoveVector();
    rawKeys = input.keyPresses;
  }

  public static boolean captureLookInput(
      LocalPlayer player, double rawYawInput, double rawPitchInput) {
    MorphViewControl.InstinctClient.LookCapture capture =
        MorphViewControl.InstinctClient.reduce(
            viewState, new MorphViewControl.InstinctClient.LookInput(rawYawInput, rawPitchInput));
    viewState = capture.state();
    return capture.handled();
  }

  public static void update(InstinctSyncState state) {
    Minecraft client = Minecraft.getInstance();
    LocalPlayer player = client.player;
    boolean wasActive = viewState.active();
    viewState =
        MorphViewControl.InstinctClient.reduce(
            viewState,
            new MorphViewControl.InstinctClient.Snapshot(
                state.active(),
                state.pose(),
                player != null,
                player != null
                    ? new MorphViewControl.View(player.getYRot(), player.getXRot())
                    : MorphViewControl.View.ZERO));
    syncState = state;
    boolean active = viewState.active();
    if (player != null) {
      InstinctState.get(player).setActive(active);
    }
    if (active && !wasActive && player != null) {
      lockedHotbarSlot = player.getInventory().getSelectedSlot();
      viewBobPlayer = player;
      viewBobLevel = player.level();
      viewBobPlayerWasDead = player.isDeadOrDying();
    }
    if (active != wasActive) {
      ClientLocomotionController.get().setInstinctActive(active);
      clearViewBobSamples();
    }
    if (active && !ClientMorphState.rabbitHopEnabled()) {
      enqueueViewBobSample(
          state.motion().horizontalDisplacement(), state.motion().horizontalSpeed());
    }
    if (!active) {
      rawMovement = Vec2.ZERO;
      rawKeys = Input.EMPTY;
    }
  }

  public static void tick(Minecraft client) {
    LocalPlayer player = client.player;
    if (player == null) {
      return;
    }
    MorphViewControl.InstinctClient.Frame frame =
        MorphViewControl.InstinctClient.reduce(
            viewState,
            new MorphViewControl.InstinctClient.Tick(
                ClientMorphState.morph() != null,
                new MorphViewControl.View(player.getYRot(), player.getXRot())));
    viewState = frame.state();
    if (!frame.applyAuthoritativeView()) {
      sendInput(
          client, player, frame.requested().yaw(), frame.requested().pitch(), frame.cameraInput());
      return;
    }

    client.options.setCameraType(CameraType.FIRST_PERSON);
    player.getInventory().setSelectedSlot(lockedHotbarSlot);
    if (client.gui.screen() != null && !isSafeScreen(client)) {
      client.gui.setScreen(null);
    }

    applyRotation(player, frame.cameraRotation());
    MorphViewControl.Pose rendered = frame.renderedPose();
    player.yBodyRot = rendered.bodyYaw();
    player.yBodyRotO = rendered.bodyYaw();
    player.setYHeadRot(rendered.headYaw());
    player.yHeadRotO = player.getYHeadRot();
    sendInput(
        client, player, frame.requested().yaw(), frame.requested().pitch(), frame.cameraInput());
  }

  public static void clear() {
    LocalPlayer player = Minecraft.getInstance().player;
    boolean resetViewBob = active() || ClientMorphState.rabbitHopEnabled();
    if (player != null) {
      InstinctState.get(player).setActive(false);
      if (resetViewBob) {
        resetViewBobImmediately(player);
      }
    }
    viewState = MorphViewControl.InstinctClient.initial();
    syncState = InstinctSyncState.INACTIVE;
    ClientLocomotionController.get().setInstinctActive(false);
    rawMovement = Vec2.ZERO;
    rawKeys = Input.EMPTY;
    clearViewBobSamples();
    viewBobPlayer = null;
    viewBobLevel = null;
    viewBobPlayerWasDead = false;
  }

  public static boolean updateViewBob(LocalPlayer player) {
    boolean rabbitHop = ClientMorphState.rabbitHopEnabled();
    if (viewBobPlayer != null || active() || rabbitHop) {
      trackViewBobLifecycle(player);
    }
    if (!active() && !rabbitHop) {
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
    if (!active() || player.isPassenger()) {
      return;
    }
    player.setPos(syncState.position());
    player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    player.setOnGround(syncState.onGround());
  }

  private static void sendInput(
      Minecraft client,
      LocalPlayer player,
      float requestedHeadYaw,
      float requestedHeadPitch,
      float cameraDelta) {
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
        active() && player.getInventory().getSelectedSlot() != lockedHotbarSlot
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
            requestedHeadYaw,
            requestedHeadPitch,
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

  private static void applyRotation(LocalPlayer player, MorphViewControl.Rotation rotation) {
    if (!rotation.apply()) {
      return;
    }
    player.setYRot(rotation.current().yaw());
    player.setXRot(rotation.current().pitch());
    if (rotation.history() == MorphViewControl.History.SNAP) {
      player.yRotO = rotation.previous().yaw();
      player.xRotO = rotation.previous().pitch();
    }
  }

  private static boolean isSafeScreen(Minecraft client) {
    return client.gui.screen() instanceof ChatScreen
        || client.gui.screen() instanceof PauseScreen
        || client.gui.screen() instanceof OptionsScreen
        || client.gui.screen() instanceof OptionsSubScreen;
  }

  private record ViewBobSample(float horizontalDisplacement, float horizontalSpeed) {}
}
