package cc.attodao.mob_life.gameplay.movement;

public final class RabbitHopMovement {
  public static final int WALK_COOLDOWN_TICKS = 10;
  public static final int SPRINT_COOLDOWN_TICKS = 3;
  public static final double SOURCE_JUMP_STRENGTH = 0.42;
  public static final float WALK_JUMP_POWER = 0.2F;
  public static final float SPRINT_JUMP_POWER = 0.3F;
  public static final float MANUAL_JUMP_POWER = 0.5F;

  private RabbitHopMovement() {}

  public static int landingCooldown(boolean sprinting) {
    return sprinting ? SPRINT_COOLDOWN_TICKS : WALK_COOLDOWN_TICKS;
  }

  public static float jumpScale(float desiredSourcePower) {
    return (float) (desiredSourcePower / SOURCE_JUMP_STRENGTH);
  }
}
