package cc.attodao.mob_life.gameplay.movement;

import cc.attodao.mob_life.gameplay.jump.MobChargedJump;

/** Deterministic multi-tick gait progression for normal-mode morph locomotion. */
public final class MorphGaitControl {
  private MorphGaitControl() {}

  public record RabbitState(boolean grounded, boolean groundedKnown, int cooldown) {
    public static final RabbitState INITIAL = new RabbitState(false, false, 0);
  }

  public record RabbitFrame(
      RabbitState state,
      boolean requestJump,
      float sourcePower,
      boolean allowForward,
      boolean manualJump) {}

  public static RabbitFrame advanceRabbit(
      RabbitState state, boolean grounded, boolean sprinting, boolean jumpDown, boolean forward) {
    int cooldown = state.cooldown();
    if (state.groundedKnown()) {
      if (grounded && !state.grounded()) {
        cooldown = RabbitHopMovement.landingCooldown(sprinting);
      } else if (cooldown > 0) {
        cooldown--;
      }
    }

    boolean requestJump = grounded && cooldown == 0 && (jumpDown || forward);
    boolean manualJump = jumpDown;
    float sourcePower =
        manualJump
            ? RabbitHopMovement.MANUAL_JUMP_POWER
            : sprinting ? RabbitHopMovement.SPRINT_JUMP_POWER : RabbitHopMovement.WALK_JUMP_POWER;
    return new RabbitFrame(
        new RabbitState(grounded, true, cooldown),
        requestJump,
        sourcePower,
        forward && !(grounded && cooldown > 0),
        manualJump);
  }

  public record EquineState(int chargeTicks, long jumpBarUntilTick, boolean jumpWasDown) {
    public static EquineState initial(boolean jumpDown) {
      return new EquineState(-1, 0L, jumpDown);
    }
  }

  public record EquineFrame(EquineState state, boolean requestJump, float charge) {}

  public static EquineFrame advanceEquine(
      EquineState state, boolean sprinting, boolean grounded, boolean jumpDown) {
    int chargeTicks = state.chargeTicks();
    boolean requestJump = false;
    float charge = 0.0F;
    if (!sprinting || !grounded) {
      chargeTicks = -1;
    } else if (jumpDown) {
      if (!state.jumpWasDown()) {
        chargeTicks = 1;
      } else if (chargeTicks >= 0) {
        chargeTicks++;
      }
    } else if (state.jumpWasDown() && chargeTicks >= 0) {
      charge = MobChargedJump.chargeScale(chargeTicks);
      requestJump = true;
      chargeTicks = -1;
    }
    return new EquineFrame(
        new EquineState(chargeTicks, state.jumpBarUntilTick(), jumpDown), requestJump, charge);
  }

  public static EquineState observeEquineJump(EquineState state, boolean jumpDown) {
    return new EquineState(state.chargeTicks(), state.jumpBarUntilTick(), jumpDown);
  }

  public static EquineState completeEquineJump(EquineState state, long jumpBarUntilTick) {
    return new EquineState(state.chargeTicks(), jumpBarUntilTick, state.jumpWasDown());
  }
}
