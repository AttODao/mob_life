package cc.attodao.mob_life.gameplay.jump;

import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.util.Mth;

public final class MobChargedJump {

  public static final int COOLDOWN_TICKS = 10;
  private static final int EQUINE_FULL_CHARGE_TICKS = 8;
  private static final int NON_EQUINE_FULL_CHARGE_TICKS = 4;

  private MobChargedJump() {}

  public static int fullChargeTicks(MorphType morph) {
    return morph.isEquine() ? EQUINE_FULL_CHARGE_TICKS : NON_EQUINE_FULL_CHARGE_TICKS;
  }

  public static float chargeScale(MorphType morph, int chargeTicks) {
    return Mth.clamp((float) chargeTicks / fullChargeTicks(morph), 0.0F, 1.0F);
  }

  public static int chargeAmount(MorphType morph, int chargeTicks) {
    return Mth.floor(chargeScale(morph, chargeTicks) * 100.0F);
  }

  public static float jumpScale(int chargeAmount) {
    int clampedAmount = Mth.clamp(chargeAmount, 0, 100);
    return clampedAmount >= 90 ? 1.0F : 0.4F + (0.4F * clampedAmount) / 90.0F;
  }
}
