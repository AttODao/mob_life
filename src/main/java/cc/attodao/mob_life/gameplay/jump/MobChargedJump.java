package cc.attodao.mob_life.gameplay.jump;

import net.minecraft.util.Mth;

public final class MobChargedJump {

    public static final int COOLDOWN_TICKS = 10;
    private static final int FULL_CHARGE_TICKS = 8;

    private MobChargedJump() {}

    public static float chargeScale(int chargeTicks) {
        return Mth.clamp((float) chargeTicks / FULL_CHARGE_TICKS, 0.0F, 1.0F);
    }

    public static int chargeAmount(int chargeTicks) {
        return Mth.floor(chargeScale(chargeTicks) * 100.0F);
    }

    public static float jumpScale(int chargeAmount) {
        int clampedAmount = Mth.clamp(chargeAmount, 0, 100);
        return clampedAmount >= 90
            ? 1.0F
            : 0.4F + (0.4F * clampedAmount) / 90.0F;
    }
}
