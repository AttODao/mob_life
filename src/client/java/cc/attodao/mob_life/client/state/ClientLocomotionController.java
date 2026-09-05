package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.jump.ChargedJumpingPlayer;
import cc.attodao.mob_life.gameplay.jump.GaitType;
import cc.attodao.mob_life.gameplay.jump.MobChargedJump;
import cc.attodao.mob_life.gameplay.movement.MorphMovementSpeed;
import cc.attodao.mob_life.gameplay.movement.MorphViewRecovery;
import cc.attodao.mob_life.gameplay.movement.RabbitHopMovement;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

final class ClientLocomotionController {
  private static final float BODY_TURN_PER_TICK = 10.0F;
  private static final float VIEW_RECOVERY_PER_TICK = 2.0F;
  private static final float MAX_HEAD_YAW = 75.0F;
  private static final float MAX_HEAD_PITCH = 40.0F;
  private static final float EQUINE_SIDEWAYS_INPUT = 0.5F;
  private static final float EQUINE_FORWARD_JUMP = 0.4F;

  private Input keys = Input.EMPTY;
  private MorphType morph;
  private LocalPlayer player;
  private boolean orientationKnown;
  private float bodyYaw;
  private int rabbitCooldown;
  private boolean rabbitGrounded;
  private boolean rabbitGroundedKnown;
  private int chargeTicks = -1;
  private long jumpBarUntilTick;
  private boolean jumpWasDown;
  private int bodyYawSentTick = Integer.MIN_VALUE;
  private boolean sentBodyYawKnown;
  private float sentBodyYaw;
  private float pendingCameraDelta;

  void capture(Input input) {
    keys = input != null ? input : Input.EMPTY;
  }

  MotionInput apply(LocalPlayer currentPlayer, MorphType currentMorph, boolean baby) {
    resetForIdentity(currentPlayer, currentMorph);
    if (currentMorph == null || ClientInstinctState.active()) {
      return MotionInput.VANILLA;
    }

    ensureOrientation(currentPlayer);
    if (usesVanillaLocomotion(currentPlayer)) {
      resetGait();
      orientationKnown = false;
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
    boolean equineSprint = currentMorph.isEquine() && sprinting;

    if (equineSprint) {
      bodyYaw = currentPlayer.getYRot();
    } else if (steering != 0.0F) {
      float turn = -steering * BODY_TURN_PER_TICK;
      bodyYaw += turn;
      turnInterpolatedView(currentPlayer, turn, 0.0F);
    }

    MorphConfig.Movement movement = MorphConfigManager.get(currentMorph).movement();
    MorphConfig.MovementState state =
        sprinting
            ? MorphConfig.MovementState.SPRINT
            : keys.shift() && movement.states().containsKey(MorphConfig.MovementState.SNEAK)
                ? MorphConfig.MovementState.SNEAK
                : MorphConfig.MovementState.WALK;
    float speed =
        (float)
            MorphMovementSpeed.controllerSpeed(
                currentMorph,
                state,
                currentPlayer.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
    currentPlayer.setSpeed(speed);

    boolean allowVanillaJump = currentPlayer.isInWater() || currentPlayer.isInLava();
    if (!allowVanillaJump) {
      if (currentMorph == MorphType.RABBIT) {
        forward = applyRabbit(currentPlayer, sprinting, forward, baby);
      } else if (currentMorph.isEquine()) {
        applyEquine(currentPlayer, sprinting, forward);
      } else {
        applyOrdinaryJump(currentPlayer);
      }
    } else if (currentMorph.isEquine()) {
      jumpWasDown = keys.jump();
    }

    float sidewaysInput = equineSprint ? steering * EQUINE_SIDEWAYS_INPUT * speed : 0.0F;
    float forwardInput = 0.0F;
    if (forward) {
      MorphMovementSpeed.RelativeInput bodyForward =
          MorphMovementSpeed.bodyForwardInput(bodyYaw, currentPlayer.getYRot(), speed);
      sidewaysInput += bodyForward.sideways();
      forwardInput = bodyForward.forward();
    }
    return new MotionInput(sidewaysInput, forwardInput, allowVanillaJump && keys.jump());
  }

  void afterTick(LocalPlayer currentPlayer, MorphType currentMorph) {
    resetForIdentity(currentPlayer, currentMorph);
    if (currentMorph == null
        || ClientInstinctState.active()
        || usesVanillaLocomotion(currentPlayer)) {
      return;
    }
    ensureOrientation(currentPlayer);
    currentPlayer.setYHeadRot(currentPlayer.getYRot());
    sendBodyYaw(currentPlayer);

    if (currentMorph == MorphType.CHICKEN
        && !currentPlayer.onGround()
        && !currentPlayer.isInWater()
        && !currentPlayer.getAbilities().flying) {
      Vec3 movement = currentPlayer.getDeltaMovement();
      if (movement.y < 0.0) {
        currentPlayer.setDeltaMovement(movement.x, movement.y * 0.6, movement.z);
      }
    }
  }

  void recoverView(
      LocalPlayer currentPlayer, MorphType currentMorph, boolean aimingInteractionActive) {
    resetForIdentity(currentPlayer, currentMorph);
    boolean cameraInputBlocksRecovery =
        MorphViewRecovery.cameraInputBlocksRecovery(consumeCameraDelta());
    if (currentMorph == null
        || ClientInstinctState.active()
        || usesVanillaLocomotion(currentPlayer)) {
      return;
    }
    ensureOrientation(currentPlayer);
    if (keys.left() == keys.right() && !aimingInteractionActive && !cameraInputBlocksRecovery) {
      float yawDelta =
          Mth.clamp(
              Mth.wrapDegrees(bodyYaw - currentPlayer.getYRot()),
              -VIEW_RECOVERY_PER_TICK,
              VIEW_RECOVERY_PER_TICK);
      float pitchDelta =
          Mth.clamp(-currentPlayer.getXRot(), -VIEW_RECOVERY_PER_TICK, VIEW_RECOVERY_PER_TICK);
      turnInterpolatedView(currentPlayer, yawDelta, pitchDelta);
    }
  }

  boolean captureLook(LocalPlayer currentPlayer, MorphType currentMorph, double yaw, double pitch) {
    if (currentMorph == null || ClientInstinctState.active()) {
      return false;
    }
    resetForIdentity(currentPlayer, currentMorph);
    ensureOrientation(currentPlayer);
    float yawDelta = finiteDelta(yaw);
    float pitchDelta = finiteDelta(pitch);
    pendingCameraDelta =
        MorphViewRecovery.accumulateCameraDelta(pendingCameraDelta, yawDelta, pitchDelta);
    float currentYawOffset = Mth.wrapDegrees(currentPlayer.getYRot() - bodyYaw);
    float wantedYawOffset = Mth.wrapDegrees(currentYawOffset + yawDelta);
    if (Math.abs(wantedYawOffset) > MAX_HEAD_YAW
        && Math.abs(wantedYawOffset) > Math.abs(currentYawOffset)) {
      yawDelta = 0.0F;
    }
    float wantedPitch = currentPlayer.getXRot() + pitchDelta;
    if (Math.abs(wantedPitch) > MAX_HEAD_PITCH
        && Math.abs(wantedPitch) > Math.abs(currentPlayer.getXRot())) {
      pitchDelta = 0.0F;
    }
    turnImmediateView(currentPlayer, yawDelta, pitchDelta);
    return true;
  }

  boolean shouldShowJumpBar() {
    return chargeTicks >= 0 || player != null && player.level().getGameTime() < jumpBarUntilTick;
  }

  float jumpBarScale() {
    return chargeTicks >= 0 ? MobChargedJump.chargeScale(chargeTicks) : 0.0F;
  }

  boolean isJumpBarCoolingDown() {
    return chargeTicks < 0 && player != null && player.level().getGameTime() < jumpBarUntilTick;
  }

  float bodyYaw() {
    return bodyYaw;
  }

  void reset() {
    player = null;
    morph = null;
    keys = Input.EMPTY;
    orientationKnown = false;
    bodyYaw = 0.0F;
    resetGait();
    jumpWasDown = false;
    bodyYawSentTick = Integer.MIN_VALUE;
    sentBodyYawKnown = false;
    pendingCameraDelta = 0.0F;
  }

  private boolean applyRabbit(
      LocalPlayer currentPlayer, boolean sprinting, boolean forward, boolean baby) {
    boolean grounded = currentPlayer.onGround();
    if (!rabbitGroundedKnown) {
      rabbitGrounded = grounded;
      rabbitGroundedKnown = true;
    } else if (grounded && !rabbitGrounded) {
      rabbitCooldown = RabbitHopMovement.landingCooldown(sprinting);
    } else if (rabbitCooldown > 0) {
      rabbitCooldown--;
    }
    rabbitGrounded = grounded;

    if (grounded && rabbitCooldown == 0 && (keys.jump() || forward)) {
      float sourcePower =
          keys.jump()
              ? RabbitHopMovement.MANUAL_JUMP_POWER
              : sprinting ? RabbitHopMovement.SPRINT_JUMP_POWER : RabbitHopMovement.WALK_JUMP_POWER;
      boolean jumped =
          ((ChargedJumpingPlayer) currentPlayer)
              .mobLife$performMorphJump(RabbitHopMovement.jumpScale(sourcePower), 0.0F, false);
      if (jumped) {
        if (currentPlayer.getDeltaMovement().horizontalDistanceSqr() < 0.01) {
          currentPlayer.moveRelative(0.1F, new Vec3(0.0, baby ? 0.5 : 1.5, 1.0));
        }
        sendGait(GaitType.RABBIT);
      }
    }
    return forward && !(grounded && rabbitCooldown > 0);
  }

  private void applyOrdinaryJump(LocalPlayer currentPlayer) {
    if (keys.jump() && currentPlayer.onGround()) {
      if (((ChargedJumpingPlayer) currentPlayer).mobLife$performMorphJump(1.0F, 0.0F, false)) {
        sendGait(GaitType.NORMAL);
      }
    }
  }

  private void applyEquine(LocalPlayer currentPlayer, boolean sprinting, boolean forward) {
    if (!sprinting || !currentPlayer.onGround()) {
      chargeTicks = -1;
    } else if (keys.jump()) {
      if (!jumpWasDown) {
        chargeTicks = 1;
      } else if (chargeTicks >= 0) {
        chargeTicks++;
      }
    } else if (jumpWasDown && chargeTicks >= 0) {
      float charge = MobChargedJump.chargeScale(chargeTicks);
      boolean jumped =
          ((ChargedJumpingPlayer) currentPlayer)
              .mobLife$performMorphJump(charge, EQUINE_FORWARD_JUMP * charge, forward);
      if (jumped) {
        sendGait(GaitType.EQUINE);
        jumpBarUntilTick = currentPlayer.level().getGameTime() + MobChargedJump.COOLDOWN_TICKS;
      }
      chargeTicks = -1;
    }
    jumpWasDown = keys.jump();
  }

  private void resetForIdentity(LocalPlayer currentPlayer, MorphType currentMorph) {
    if (player == currentPlayer && morph == currentMorph) {
      return;
    }
    player = currentPlayer;
    morph = currentMorph;
    orientationKnown = false;
    pendingCameraDelta = 0.0F;
    resetGait();
    jumpWasDown = keys.jump();
  }

  private void ensureOrientation(LocalPlayer currentPlayer) {
    if (!orientationKnown) {
      bodyYaw = currentPlayer.getYRot();
      orientationKnown = true;
    }
  }

  private void resetGait() {
    rabbitCooldown = 0;
    rabbitGroundedKnown = false;
    chargeTicks = -1;
    jumpBarUntilTick = 0L;
    jumpWasDown = keys.jump();
  }

  private static boolean usesVanillaLocomotion(LocalPlayer player) {
    return player.isPassenger() || player.isFallFlying() || player.getAbilities().flying;
  }

  private static void sendGait(GaitType type) {
    ClientPlayNetworking.send(new MobLifeNetworking.GaitEventPayload(type));
  }

  private void sendBodyYaw(LocalPlayer currentPlayer) {
    if (bodyYawSentTick == currentPlayer.tickCount) {
      return;
    }
    float normalized = Mth.wrapDegrees(bodyYaw);
    if (sentBodyYawKnown && Math.abs(Mth.wrapDegrees(normalized - sentBodyYaw)) < 1.0E-4F) {
      return;
    }
    bodyYawSentTick = currentPlayer.tickCount;
    sentBodyYawKnown = true;
    sentBodyYaw = normalized;
    ClientPlayNetworking.send(new MobLifeNetworking.MorphBodyYawUpdatePayload(normalized));
  }

  private static float finiteDelta(double input) {
    float delta = (float) input * 0.15F;
    return Float.isFinite(delta) ? delta : 0.0F;
  }

  private float consumeCameraDelta() {
    float result = pendingCameraDelta;
    pendingCameraDelta = 0.0F;
    return result;
  }

  private static void turnInterpolatedView(LocalPlayer player, float yawDelta, float pitchDelta) {
    player.setYRot(player.getYRot() + yawDelta);
    player.setXRot(Mth.clamp(player.getXRot() + pitchDelta, -MAX_HEAD_PITCH, MAX_HEAD_PITCH));
    player.setYHeadRot(player.getYRot());
  }

  private static void turnImmediateView(LocalPlayer player, float yawDelta, float pitchDelta) {
    turnInterpolatedView(player, yawDelta, pitchDelta);
    player.yRotO += yawDelta;
    player.xRotO = Mth.clamp(player.xRotO + pitchDelta, -MAX_HEAD_PITCH, MAX_HEAD_PITCH);
    player.yHeadRotO += yawDelta;
  }

  record MotionInput(float sideways, float forward, boolean jumping) {
    private static final MotionInput VANILLA = new MotionInput(Float.NaN, Float.NaN, false);

    boolean isVanilla() {
      return Float.isNaN(sideways);
    }
  }
}
