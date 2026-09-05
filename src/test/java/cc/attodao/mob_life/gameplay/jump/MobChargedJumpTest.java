package cc.attodao.mob_life.gameplay.jump;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MobChargedJumpTest {
  @Test
  void usesVanillaRideableChargeCurve() {
    assertEquals(0.0F, MobChargedJump.chargeScale(0));
    assertEquals(0.1F, MobChargedJump.chargeScale(1), 0.0001F);
    assertEquals(0.9F, MobChargedJump.chargeScale(9), 0.0001F);
    assertEquals(1.0F, MobChargedJump.chargeScale(10), 0.0001F);
    assertEquals(0.9F, MobChargedJump.chargeScale(11), 0.0001F);
    assertEquals(0.82F, MobChargedJump.chargeScale(19), 0.0001F);
  }
}
