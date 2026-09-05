package cc.attodao.mob_life.gameplay.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MorphViewRecoveryTest {
  @Test
  void blocksRecoveryAtAndAboveCameraInputThreshold() {
    assertFalse(MorphViewRecovery.cameraInputBlocksRecovery(0.499F));
    assertTrue(MorphViewRecovery.cameraInputBlocksRecovery(0.5F));
    assertTrue(MorphViewRecovery.cameraInputBlocksRecovery(0.501F));
  }

  @Test
  void accumulatesYawAndPitchInputWithinOneTick() {
    float accumulated = MorphViewRecovery.accumulateCameraDelta(0.0F, 0.2F, -0.1F);
    accumulated = MorphViewRecovery.accumulateCameraDelta(accumulated, -0.1F, 0.1F);

    assertEquals(0.5F, accumulated, 0.0001F);
    assertTrue(MorphViewRecovery.cameraInputBlocksRecovery(accumulated));
  }
}
