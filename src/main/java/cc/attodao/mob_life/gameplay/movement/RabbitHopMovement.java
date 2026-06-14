package cc.attodao.mob_life.gameplay.movement;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class RabbitHopMovement {

  public static final int WALK_COOLDOWN_TICKS = 20;
  public static final int SPRINT_COOLDOWN_TICKS = 3;
  private static final float WALK_HORIZONTAL_SPEED = 0.2F;
  private static final float SPRINT_HORIZONTAL_SPEED = 0.35F;

  private RabbitHopMovement() {}

  public static void launch(Player player, Input input) {
    boolean sprinting = player.isSprinting();
    double jumpVelocity =
        input.jump()
            ? player.getAttributeValue(Attributes.JUMP_STRENGTH) + player.getJumpBoostPower()
            : sprinting ? 0.3 : 0.2;
    float sideways = (input.right() ? 1.0F : 0.0F) - (input.left() ? 1.0F : 0.0F);
    float forward = (input.forward() ? 1.0F : 0.0F) - (input.backward() ? 1.0F : 0.0F);

    player.setDeltaMovement(0.0, jumpVelocity, 0.0);
    player.moveRelative(
        sprinting ? SPRINT_HORIZONTAL_SPEED : WALK_HORIZONTAL_SPEED,
        new Vec3(sideways, 0.0, forward));
  }

  public static int cooldown(Player player) {
    return player.isSprinting() ? SPRINT_COOLDOWN_TICKS : WALK_COOLDOWN_TICKS;
  }
}
