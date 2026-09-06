package cc.attodao.mob_life.gameplay.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MorphGaitControlTest {
  @Test
  void rabbitLandingCooldownProgressesAcrossTicksBeforeTheNextHop() {
    MorphGaitControl.RabbitState state = MorphGaitControl.RabbitState.INITIAL;
    state = MorphGaitControl.advanceRabbit(state, true, true, false, true).state();
    state = MorphGaitControl.advanceRabbit(state, false, true, false, true).state();

    MorphGaitControl.RabbitFrame landing =
        MorphGaitControl.advanceRabbit(state, true, true, false, true);
    assertEquals(3, landing.state().cooldown());
    assertFalse(landing.requestJump());
    assertFalse(landing.allowForward());

    MorphGaitControl.RabbitFrame frame = landing;
    for (int expected = 2; expected >= 0; expected--) {
      frame = MorphGaitControl.advanceRabbit(frame.state(), true, true, false, true);
      assertEquals(expected, frame.state().cooldown());
    }
    assertTrue(frame.requestJump());
    assertTrue(frame.allowForward());
    assertEquals(RabbitHopMovement.SPRINT_JUMP_POWER, frame.sourcePower());
  }

  @Test
  void equineChargeStartsOnANewPressAndRequestsJumpOnRelease() {
    MorphGaitControl.EquineState state = MorphGaitControl.EquineState.initial(false);
    MorphGaitControl.EquineFrame frame = MorphGaitControl.advanceEquine(state, true, true, true);
    assertEquals(1, frame.state().chargeTicks());

    for (int tick = 0; tick < 4; tick++) {
      frame = MorphGaitControl.advanceEquine(frame.state(), true, true, true);
    }
    assertEquals(5, frame.state().chargeTicks());

    frame = MorphGaitControl.advanceEquine(frame.state(), true, true, false);
    assertTrue(frame.requestJump());
    assertEquals(0.5F, frame.charge(), 0.0001F);
    assertEquals(-1, frame.state().chargeTicks());

    state = MorphGaitControl.completeEquineJump(frame.state(), 120L);
    assertEquals(120L, state.jumpBarUntilTick());
  }

  @Test
  void equineChargeIsDiscardedWhenSprintOrGroundIsLost() {
    MorphGaitControl.EquineState charging =
        MorphGaitControl.advanceEquine(
                MorphGaitControl.EquineState.initial(false), true, true, true)
            .state();

    MorphGaitControl.EquineFrame stopped =
        MorphGaitControl.advanceEquine(charging, false, true, true);
    assertEquals(-1, stopped.state().chargeTicks());
    assertFalse(stopped.requestJump());

    MorphGaitControl.EquineFrame airborne =
        MorphGaitControl.advanceEquine(charging, true, false, true);
    assertEquals(-1, airborne.state().chargeTicks());
    assertFalse(airborne.requestJump());
  }

  @Test
  void observingVanillaJumpUpdatesOnlyTheReleaseEdge() {
    MorphGaitControl.EquineState charging = new MorphGaitControl.EquineState(4, 120L, false);

    MorphGaitControl.EquineState observed = MorphGaitControl.observeEquineJump(charging, true);

    assertEquals(4, observed.chargeTicks());
    assertEquals(120L, observed.jumpBarUntilTick());
    assertTrue(observed.jumpWasDown());
  }
}
