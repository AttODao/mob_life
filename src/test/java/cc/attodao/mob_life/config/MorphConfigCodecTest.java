package cc.attodao.mob_life.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MorphConfigCodecTest {
  @Test
  void movementStatesUseExactConfiguredValues() {
    MorphConfig.Movement cat =
        new MorphConfig.Movement(
            Map.of(
                MorphConfig.MovementState.SNEAK,
                new MorphConfig.MovementValue(0.6, 1.0),
                MorphConfig.MovementState.WALK,
                new MorphConfig.MovementValue(0.8, 1.0),
                MorphConfig.MovementState.SPRINT,
                new MorphConfig.MovementValue(1.33, 1.3)));
    assertEquals(0.6, cat.value(MorphConfig.MovementState.SNEAK).goalSpeedModifier());
    assertEquals(0.8, cat.value(MorphConfig.MovementState.WALK).goalSpeedModifier());
    assertEquals(1.33, cat.value(MorphConfig.MovementState.SPRINT).goalSpeedModifier());
    assertEquals(
        1.3, cat.value(MorphConfig.MovementState.SPRINT).movementSpeedAttributeMultiplier());
    MorphConfig.Movement cow =
        new MorphConfig.Movement(
            Map.of(
                MorphConfig.MovementState.WALK,
                new MorphConfig.MovementValue(1.0, 1.0),
                MorphConfig.MovementState.SPRINT,
                new MorphConfig.MovementValue(2.0, 1.0)));
    assertEquals(
        cow.value(MorphConfig.MovementState.WALK), cow.value(MorphConfig.MovementState.SNEAK));
  }

  @Test
  void walkStateIsRequired() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MorphConfig.Movement(
                Map.of(MorphConfig.MovementState.SPRINT, new MorphConfig.MovementValue(1.0, 1.0))));
  }

  @Test
  void rejectsNonFiniteAndOutOfRangeMovementValues() {
    assertThrows(
        IllegalArgumentException.class, () -> new MorphConfig.MovementValue(Double.NaN, 1.0));
    assertThrows(IllegalArgumentException.class, () -> new MorphConfig.MovementValue(1.0, 4.1));
  }

  @Test
  void controllerSpeedKeepsMoveControlMultiplicationOrder() {
    MorphConfig.MovementValue value = new MorphConfig.MovementValue(1.33, 1.3);
    assertEquals(0.5187, value.controllerSpeed(0.3), 0.0000001);
  }
}
