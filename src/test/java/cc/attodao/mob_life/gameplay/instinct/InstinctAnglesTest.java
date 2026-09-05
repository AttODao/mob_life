package cc.attodao.mob_life.gameplay.instinct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

class InstinctAnglesTest {
  @Test
  void recoversToBodyForwardFromEveryHeadYaw() {
    for (int startingYaw = -180; startingYaw <= 180; startingYaw++) {
      float headYaw = startingYaw;
      float previousDistance = Math.abs(Mth.wrapDegrees(headYaw));
      for (int tick = 0; tick < 90; tick++) {
        headYaw = InstinctAngles.approachYaw(headYaw, 0.0F, 2.0F);
        float distance = Math.abs(Mth.wrapDegrees(headYaw));
        assertTrue(distance <= previousDistance);
        previousDistance = distance;
      }
      assertEquals(0.0F, Mth.wrapDegrees(headYaw), 0.0001F);
    }
  }

  @Test
  void approachesAcrossWrappedYawBoundary() {
    assertEquals(-179.0F, Mth.wrapDegrees(InstinctAngles.approachYaw(179.0F, -179.0F, 2.0F)));
    assertEquals(179.0F, Mth.wrapDegrees(InstinctAngles.approachYaw(-179.0F, 179.0F, 2.0F)));
  }

  @Test
  void synchronizedHeadFollowRemainsCappedAtThirtyDegrees() {
    assertEquals(30.0F, InstinctAngles.approachYaw(0.0F, 90.0F, 30.0F));
  }

  @Test
  void clampsHeadOffsetAcrossWrappedYawBoundary() {
    float clamped = InstinctAngles.clampHeadYawToBody(-179.0F, 179.0F, 75.0F);
    assertEquals(2.0F, Mth.wrapDegrees(clamped - 179.0F));
  }
}
