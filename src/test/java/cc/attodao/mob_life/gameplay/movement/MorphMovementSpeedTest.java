package cc.attodao.mob_life.gameplay.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MorphMovementSpeedTest {
  @Test
  void convertsBodyForwardIntoViewRelativeInput() {
    assertInput(0.0F, 0.0F, 0.25F, 0.0F, 0.25F);
    assertInput(0.0F, 90.0F, 0.25F, 0.25F, 0.0F);
    assertInput(90.0F, 0.0F, 0.25F, -0.25F, 0.0F);
  }

  private static void assertInput(
      float bodyYaw, float viewYaw, float speed, float expectedSideways, float expectedForward) {
    MorphMovementSpeed.RelativeInput input =
        MorphMovementSpeed.bodyForwardInput(bodyYaw, viewYaw, speed);
    assertEquals(expectedSideways, input.sideways(), 0.0001F);
    assertEquals(expectedForward, input.forward(), 0.0001F);
  }
}
