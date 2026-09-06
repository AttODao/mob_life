package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.client.network.ClientLocomotionPackets;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.jump.ChargedJumpingPlayer;
import cc.attodao.mob_life.gameplay.jump.GaitType;
import cc.attodao.mob_life.gameplay.jump.MobChargedJump;
import cc.attodao.mob_life.gameplay.movement.MorphGaitControl;
import cc.attodao.mob_life.gameplay.movement.MorphMovementSpeed;
import cc.attodao.mob_life.gameplay.movement.RabbitHopMovement;
import cc.attodao.mob_life.gameplay.view.MorphViewControl;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class ClientLocomotionController {
  private static final ClientLocomotionController INSTANCE = new ClientLocomotionController();
  private static final float EQUINE_SIDEWAYS_INPUT = 0.5F;
  private static final float EQUINE_FORWARD_JUMP = 0.4F;

  private Input keys = Input.EMPTY;
  private MorphType morph;
  private boolean baby;
  private boolean instinctActive;
  private LocalPlayer player;
  private MorphViewControl.Normal.State viewState = MorphViewControl.Normal.initial();
  private MorphGaitControl.RabbitState rabbitState = MorphGaitControl.RabbitState.INITIAL;
  private MorphGaitControl.EquineState equineState = MorphGaitControl.EquineState.initial(false);
  private int bodyYawSentTick = Integer.MIN_VALUE;
  private boolean sentBodyYawKnown;
  private float sentBodyYaw;

  private ClientLocomotionController() {}

  public static ClientLocomotionController get() {
    return INSTANCE;
  }

  public void selectMorph(MorphType morph, boolean baby) {
    this.morph = morph;
    this.baby = baby;
    resetProgress();
  }

  public void setInstinctActive(boolean active) {
    if (instinctActive == active) {
      return;
    }
    instinctActive = active;
    resetProgress();
  }

  public void profilesReloaded() {
    resetProgress();
  }

  public void teleported() {
    resetProgress();
  }

  private void capture(Input input) {
    keys = input != null ? input : Input.EMPTY;
  }

  public PolledInput captureAndFilter(LocalPlayer currentPlayer, Input input, Vec2 movement) {
    if (instinctActive) {
      return new PolledInput(Input.EMPTY, Vec2.ZERO, true);
    }
    capture(input);
    if (morph == null) {
      return new PolledInput(input, movement, false);
    }

    Input filtered =
        new Input(
            input.forward(),
            false,
            input.left(),
            input.right(),
            input.jump(),
            input.shift(),
            input.sprint());
    Vec2 filteredMovement =
        new Vec2(
            (input.left() ? 1.0F : 0.0F) - (input.right() ? 1.0F : 0.0F),
            input.forward() ? 1.0F : 0.0F);
    if (usesRestrictedVehicle(currentPlayer)) {
      filtered = dismountOnly(filtered);
      return new PolledInput(filtered, filteredMovement, true);
    }
    return new PolledInput(filtered, filteredMovement, false);
  }

  public PolledInput filterOngoingVehicleInput(
      LocalPlayer currentPlayer, Input input, Vec2 movement) {
    if (instinctActive) {
      return new PolledInput(Input.EMPTY, Vec2.ZERO, true);
    }
    if (morph == null || !usesRestrictedVehicle(currentPlayer)) {
      return new PolledInput(input, movement, false);
    }
    return new PolledInput(dismountOnly(input), movement, true);
  }

  public MotionInput apply(LocalPlayer currentPlayer) {
    bindPlayer(currentPlayer);
    if (morph == null || instinctActive) {
      return MotionInput.VANILLA;
    }

    if (usesVanillaLocomotion(currentPlayer)) {
      resetGait();
      viewState =
          MorphViewControl.Normal.reduce(
                  viewState,
                  new MorphViewControl.Normal.BodyTick(
                      currentView(currentPlayer), 0.0F, MorphViewControl.Normal.BodyMode.SUSPEND))
              .state();
      return MotionInput.VANILLA;
    }

    if (currentPlayer.isInWater()) {
      currentPlayer.setSprinting(false);
      resetGait();
    }

    boolean left = keys.left() && !keys.right();
    boolean right = keys.right() && !keys.left();
    float steering = left ? 1.0F : right ? -1.0F : 0.0F;
    boolean forward = keys.forward();
    boolean sprinting = currentPlayer.isSprinting() && !currentPlayer.isInWater();
    boolean equineSprint = morph.isEquine() && sprinting;

    MorphViewControl.Normal.Transition bodyTransition =
        MorphViewControl.Normal.reduce(
            viewState,
            new MorphViewControl.Normal.BodyTick(
                currentView(currentPlayer),
                steering,
                equineSprint
                    ? MorphViewControl.Normal.BodyMode.ALIGN
                    : MorphViewControl.Normal.BodyMode.TURN));
    viewState = bodyTransition.state();
    applyRotation(currentPlayer, bodyTransition.rotation());

    MorphConfig.Movement movement = MorphConfigManager.get(morph).movement();
    MorphConfig.MovementState state =
        sprinting
            ? MorphConfig.MovementState.SPRINT
            : keys.shift() && movement.states().containsKey(MorphConfig.MovementState.SNEAK)
                ? MorphConfig.MovementState.SNEAK
                : MorphConfig.MovementState.WALK;
    float speed =
        (float)
            MorphMovementSpeed.controllerSpeed(
                morph,
                state,
                currentPlayer.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
    currentPlayer.setSpeed(speed);

    boolean allowVanillaJump = currentPlayer.isInWater() || currentPlayer.isInLava();
    if (!allowVanillaJump) {
      if (morph == MorphType.RABBIT) {
        forward = applyRabbit(currentPlayer, sprinting, forward, baby);
      } else if (morph.isEquine()) {
        applyEquine(currentPlayer, sprinting, forward);
      } else {
        applyOrdinaryJump(currentPlayer);
      }
    } else if (morph.isEquine()) {
      equineState = MorphGaitControl.observeEquineJump(equineState, keys.jump());
    }

    float sidewaysInput = equineSprint ? steering * EQUINE_SIDEWAYS_INPUT * speed : 0.0F;
    float forwardInput = 0.0F;
    if (forward) {
      MorphMovementSpeed.RelativeInput bodyForward =
          MorphMovementSpeed.bodyForwardInput(viewState.bodyYaw(), currentPlayer.getYRot(), speed);
      sidewaysInput += bodyForward.sideways();
      forwardInput = bodyForward.forward();
    }
    return new MotionInput(sidewaysInput, forwardInput, allowVanillaJump && keys.jump());
  }

  public void afterTick(LocalPlayer currentPlayer) {
    bindPlayer(currentPlayer);
    if (morph == null || instinctActive || usesVanillaLocomotion(currentPlayer)) {
      return;
    }
    viewState =
        MorphViewControl.Normal.reduce(
                viewState,
                new MorphViewControl.Normal.BodyTick(
                    currentView(currentPlayer), 0.0F, MorphViewControl.Normal.BodyMode.TURN))
            .state();
    currentPlayer.setYHeadRot(currentPlayer.getYRot());
    sendBodyYaw(currentPlayer);

    if (morph == MorphType.CHICKEN
        && !currentPlayer.onGround()
        && !currentPlayer.isInWater()
        && !currentPlayer.getAbilities().flying) {
      Vec3 movement = currentPlayer.getDeltaMovement();
      if (movement.y < 0.0) {
        currentPlayer.setDeltaMovement(movement.x, movement.y * 0.6, movement.z);
      }
    }
  }

  public void recoverView(LocalPlayer currentPlayer, boolean aimingInteractionActive) {
    bindPlayer(currentPlayer);
    boolean active = morph != null && !instinctActive && !usesVanillaLocomotion(currentPlayer);
    MorphViewControl.Normal.Transition transition =
        MorphViewControl.Normal.reduce(
            viewState,
            new MorphViewControl.Normal.RecoveryTick(
                currentView(currentPlayer),
                active,
                keys.left() == keys.right() && !aimingInteractionActive));
    viewState = transition.state();
    applyRotation(currentPlayer, transition.rotation());
  }

  public boolean captureLook(LocalPlayer currentPlayer, double yaw, double pitch) {
    if (morph == null || instinctActive) {
      return false;
    }
    bindPlayer(currentPlayer);
    MorphViewControl.Normal.Transition transition =
        MorphViewControl.Normal.reduce(
            viewState,
            new MorphViewControl.Normal.LookInput(
                currentView(currentPlayer), previousView(currentPlayer), yaw, pitch));
    viewState = transition.state();
    applyRotation(currentPlayer, transition.rotation());
    return true;
  }

  public boolean shouldShowJumpBar() {
    return equineState.chargeTicks() >= 0
        || player != null && player.level().getGameTime() < equineState.jumpBarUntilTick();
  }

  public float jumpBarScale() {
    return equineState.chargeTicks() >= 0
        ? MobChargedJump.chargeScale(equineState.chargeTicks())
        : 0.0F;
  }

  public boolean isJumpBarCoolingDown() {
    return equineState.chargeTicks() < 0
        && player != null
        && player.level().getGameTime() < equineState.jumpBarUntilTick();
  }

  public float bodyYaw() {
    return viewState.bodyYaw();
  }

  public void clear() {
    morph = null;
    baby = false;
    instinctActive = false;
    resetProgress();
  }

  private boolean applyRabbit(
      LocalPlayer currentPlayer, boolean sprinting, boolean forward, boolean baby) {
    boolean grounded = currentPlayer.onGround();
    MorphGaitControl.RabbitFrame frame =
        MorphGaitControl.advanceRabbit(rabbitState, grounded, sprinting, keys.jump(), forward);
    rabbitState = frame.state();
    if (frame.requestJump()) {
      boolean jumped =
          ((ChargedJumpingPlayer) currentPlayer)
              .mobLife$performMorphJump(
                  RabbitHopMovement.jumpScale(frame.sourcePower()), 0.0F, false);
      if (jumped) {
        if (currentPlayer.getDeltaMovement().horizontalDistanceSqr() < 0.01) {
          currentPlayer.moveRelative(0.1F, new Vec3(0.0, baby ? 0.5 : 1.5, 1.0));
        }
        sendGait(GaitType.RABBIT);
      }
    }
    return frame.allowForward();
  }

  private void applyOrdinaryJump(LocalPlayer currentPlayer) {
    if (keys.jump() && currentPlayer.onGround()) {
      if (((ChargedJumpingPlayer) currentPlayer).mobLife$performMorphJump(1.0F, 0.0F, false)) {
        sendGait(GaitType.NORMAL);
      }
    }
  }

  private void applyEquine(LocalPlayer currentPlayer, boolean sprinting, boolean forward) {
    MorphGaitControl.EquineFrame frame =
        MorphGaitControl.advanceEquine(
            equineState, sprinting, currentPlayer.onGround(), keys.jump());
    equineState = frame.state();
    if (frame.requestJump()) {
      float charge = frame.charge();
      boolean jumped =
          ((ChargedJumpingPlayer) currentPlayer)
              .mobLife$performMorphJump(charge, EQUINE_FORWARD_JUMP * charge, forward);
      if (jumped) {
        sendGait(GaitType.EQUINE);
        equineState =
            MorphGaitControl.completeEquineJump(
                equineState, currentPlayer.level().getGameTime() + MobChargedJump.COOLDOWN_TICKS);
      }
    }
  }

  private void bindPlayer(LocalPlayer currentPlayer) {
    if (player == currentPlayer) {
      return;
    }
    player = currentPlayer;
    viewState = MorphViewControl.Normal.initial();
    resetGait();
  }

  private void resetGait() {
    rabbitState = MorphGaitControl.RabbitState.INITIAL;
    equineState = MorphGaitControl.EquineState.initial(keys.jump());
  }

  private void resetProgress() {
    player = null;
    keys = Input.EMPTY;
    viewState = MorphViewControl.Normal.initial();
    resetGait();
    bodyYawSentTick = Integer.MIN_VALUE;
    sentBodyYawKnown = false;
  }

  private static boolean usesVanillaLocomotion(LocalPlayer player) {
    return player.isPassenger() || player.isFallFlying() || player.getAbilities().flying;
  }

  private static boolean usesRestrictedVehicle(LocalPlayer player) {
    return player.getVehicle() instanceof AbstractBoat
        || player.getVehicle() instanceof AbstractMinecart;
  }

  private static Input dismountOnly(Input input) {
    return new Input(false, false, false, false, false, input.shift(), false);
  }

  private static void sendGait(GaitType type) {
    ClientLocomotionPackets.sendGait(type);
  }

  private void sendBodyYaw(LocalPlayer currentPlayer) {
    if (bodyYawSentTick == currentPlayer.tickCount) {
      return;
    }
    float normalized = Mth.wrapDegrees(viewState.bodyYaw());
    if (sentBodyYawKnown && Math.abs(Mth.wrapDegrees(normalized - sentBodyYaw)) < 1.0E-4F) {
      return;
    }
    bodyYawSentTick = currentPlayer.tickCount;
    sentBodyYawKnown = true;
    sentBodyYaw = normalized;
    ClientLocomotionPackets.sendBodyYaw(normalized);
  }

  private static MorphViewControl.View currentView(LocalPlayer player) {
    return new MorphViewControl.View(player.getYRot(), player.getXRot());
  }

  private static MorphViewControl.View previousView(LocalPlayer player) {
    return new MorphViewControl.View(player.yRotO, player.xRotO);
  }

  private static void applyRotation(LocalPlayer player, MorphViewControl.Rotation rotation) {
    if (!rotation.apply()) {
      return;
    }
    player.setYRot(rotation.current().yaw());
    player.setXRot(rotation.current().pitch());
    player.setYHeadRot(player.getYRot());
    if (rotation.history() == MorphViewControl.History.SNAP) {
      player.yRotO = rotation.previous().yaw();
      player.xRotO = rotation.previous().pitch();
      player.yHeadRotO += rotation.previousHeadYawDelta();
    }
  }

  public record MotionInput(float sideways, float forward, boolean jumping) {
    private static final MotionInput VANILLA = new MotionInput(Float.NaN, Float.NaN, false);

    public boolean isVanilla() {
      return Float.isNaN(sideways);
    }
  }

  public record PolledInput(Input keys, Vec2 movement, boolean disableSprinting) {}
}
