package cc.attodao.mob_life.gameplay.jump;

public final class MobChargedJump {

  public static final int COOLDOWN_TICKS = 10;

  private MobChargedJump() {}

  /** Exact curve used by LocalPlayer while charging a vanilla rideable jump. */
  public static float chargeScale(int chargeTicks) {
    if (chargeTicks < 0) {
      return 0.0F;
    }
    return chargeTicks < 10 ? chargeTicks * 0.1F : 0.8F + 2.0F / (chargeTicks - 9) * 0.1F;
  }
}
