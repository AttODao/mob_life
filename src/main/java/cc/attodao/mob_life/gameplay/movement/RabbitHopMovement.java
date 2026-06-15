package cc.attodao.mob_life.gameplay.movement;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.ability.MorphAbility;
import cc.attodao.mob_life.gameplay.inventory.MorphEquipment;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class RabbitHopMovement {

  private RabbitHopMovement() {}

  public static void launch(Player player, Input input) {
    MorphConfig.RabbitHop config = config(player);
    boolean fastSprint = MorphAbility.isFastSprintActive(player);
    boolean sprinting = player.isSprinting() || fastSprint;
    double jumpVelocity =
        input.jump()
            ? player.getAttributeValue(Attributes.JUMP_STRENGTH) + player.getJumpBoostPower()
            : sprinting ? config.sprintJumpVelocity() : config.walkJumpVelocity();
    float sideways = (input.left() ? 1.0F : 0.0F) - (input.right() ? 1.0F : 0.0F);
    float forward = (input.forward() ? 1.0F : 0.0F) - (input.backward() ? 1.0F : 0.0F);

    player.setDeltaMovement(0.0, jumpVelocity, 0.0);
    float horizontalSpeed =
        sprinting ? config.sprintHorizontalSpeed() : config.walkHorizontalSpeed();
    player.moveRelative(horizontalSpeed, new Vec3(sideways, 0.0, forward));
  }

  public static int cooldown(Player player) {
    MorphConfig.RabbitHop config = config(player);
    return player.isSprinting() || MorphAbility.isFastSprintActive(player)
        ? config.sprintCooldown()
        : config.walkCooldown();
  }

  private static MorphConfig.RabbitHop config(Player player) {
    return MorphConfigManager.get(MorphEquipment.morph(player)).movement().rabbitHop();
  }
}
