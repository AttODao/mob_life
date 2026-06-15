package cc.attodao.mob_life.gameplay.movement;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.ability.MorphAbility;
import cc.attodao.mob_life.gameplay.inventory.MorphEquipment;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class MorphMovementSpeed {
  private static final Identifier SPRINT_MODIFIER_ID = MobLife.id("morph_sprint_speed");
  private static final double PLAYER_SPRINT_MULTIPLIER = 1.3;

  private MorphMovementSpeed() {}

  public static double walkingSpeed(MorphType morph, double movementSpeed) {
    MorphConfig.Movement movement = MorphConfigManager.get(morph).movement();
    return movementSpeed * movement.attributeScale() * movement.walkMultiplier();
  }

  public static void refresh(Player player) {
    AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (movementSpeed == null) {
      return;
    }

    MorphType morph = MorphEquipment.morph(player);
    MorphConfig.Movement movement = MorphConfigManager.get(morph).movement();
    if (!morph.isPlayer() && (player.isSprinting() || MorphAbility.isFastSprintActive(player))) {
      double sprintMultiplier = movement.sprintMultiplier();
      if (MorphAbility.isFastSprintActive(player) && morph == MorphType.CAT) {
        sprintMultiplier = MorphAbility.CAT_PANIC_SPEED_MULTIPLIER;
      }
      double vanillaSprintMultiplier = player.isSprinting() ? PLAYER_SPRINT_MULTIPLIER : 1.0;
      double extraMultiplier =
          sprintMultiplier / movement.walkMultiplier() / vanillaSprintMultiplier - 1.0;
      movementSpeed.addOrUpdateTransientModifier(
          new AttributeModifier(
              SPRINT_MODIFIER_ID,
              extraMultiplier,
              AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    } else {
      movementSpeed.removeModifier(SPRINT_MODIFIER_ID);
    }
  }
}
