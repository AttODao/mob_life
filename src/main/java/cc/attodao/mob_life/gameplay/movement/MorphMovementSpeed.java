package cc.attodao.mob_life.gameplay.movement;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.inventory.MorphEquipment;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class MorphMovementSpeed {
  private static final Identifier SPRINT_MODIFIER_ID = MobLife.id("morph_sprint_speed");
  private static final double PLAYER_BLOCKS_PER_SECOND_PER_MOVEMENT_SPEED = 43.2;
  private static final double MOB_BLOCKS_PER_SECOND_PER_MOVEMENT_SPEED = 10.8;
  private static final double PLAYER_SPRINT_MULTIPLIER = 1.3;

  private MorphMovementSpeed() {}

  public static double walkingSpeed(MorphType morph, double movementSpeed) {
    MorphConfig.Movement movement = MorphConfigManager.get(morph).movement();
    return movement.walkSpeed()
        * speedScale(movement, movementSpeed)
        * blocksPerSecondPerMovementSpeed(morph)
        / PLAYER_BLOCKS_PER_SECOND_PER_MOVEMENT_SPEED;
  }

  public static boolean canSprint(Player player) {
    MorphType morph = MorphEquipment.morph(player);
    return canSprint(morph);
  }

  public static boolean canSprint(MorphType morph) {
    MorphConfig.Movement movement = MorphConfigManager.get(morph).movement();
    return movement.sprintSpeed() != movement.walkSpeed();
  }

  public static void refresh(Player player) {
    AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (movementSpeed == null) {
      return;
    }

    MorphType morph = MorphEquipment.morph(player);
    if (morph.isPlayer()) {
      movementSpeed.removeModifier(SPRINT_MODIFIER_ID);
      return;
    }
    MorphConfig.Movement movement = MorphConfigManager.get(morph).movement();
    boolean sprinting = player.isSprinting();
    boolean sprintAllowed = canSprint(morph);

    if (!sprintAllowed && sprinting) {
      player.setSprinting(false);
      sprinting = false;
    }

    double walkSpeed = movement.walkSpeed();
    double targetSpeed = sprinting && sprintAllowed ? movement.sprintSpeed() : walkSpeed;

    if (Double.compare(targetSpeed, walkSpeed) == 0) {
      movementSpeed.removeModifier(SPRINT_MODIFIER_ID);
      return;
    }

    double vanillaSprintMultiplier = sprinting ? PLAYER_SPRINT_MULTIPLIER : 1.0;
    double extraMultiplier = targetSpeed / walkSpeed / vanillaSprintMultiplier - 1.0;
    movementSpeed.addOrUpdateTransientModifier(
        new AttributeModifier(
            SPRINT_MODIFIER_ID, extraMultiplier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
  }

  private static double speedScale(MorphConfig.Movement movement, double movementSpeed) {
    return movementSpeed / movement.referenceMobSpeed();
  }

  private static double blocksPerSecondPerMovementSpeed(MorphType morph) {
    return morph.isEquine()
        ? PLAYER_BLOCKS_PER_SECOND_PER_MOVEMENT_SPEED
        : MOB_BLOCKS_PER_SECOND_PER_MOVEMENT_SPEED;
  }
}
