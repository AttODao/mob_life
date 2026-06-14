package cc.attodao.mob_life.gameplay.movement;

import cc.attodao.mob_life.MobLife;
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
  private static final double MOB_TO_PLAYER_MOVEMENT_SCALE = 0.25;

  private MorphMovementSpeed() {}

  public static double walkingSpeed(MorphType morph, double movementSpeed) {
    SpeedProfile profile = profile(morph);
    return movementSpeed * profile.playerMovementScale() * profile.walkMultiplier();
  }

  public static void refresh(Player player) {
    AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (movementSpeed == null) {
      return;
    }

    MorphType morph = MorphEquipment.morph(player);
    SpeedProfile profile = profile(morph);
    if (!morph.isPlayer() && player.isSprinting()) {
      double extraMultiplier =
          profile.sprintMultiplier() / profile.walkMultiplier() / PLAYER_SPRINT_MULTIPLIER - 1.0;
      movementSpeed.addOrUpdateTransientModifier(
          new AttributeModifier(
              SPRINT_MODIFIER_ID,
              extraMultiplier,
              AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    } else {
      movementSpeed.removeModifier(SPRINT_MODIFIER_ID);
    }
  }

  private static SpeedProfile profile(MorphType morph) {
    return switch (morph) {
      case PLAYER -> new SpeedProfile(1.0, 1.0, PLAYER_SPRINT_MULTIPLIER);
      case COW -> new SpeedProfile(MOB_TO_PLAYER_MOVEMENT_SCALE, 1.0, 2.0);
      case SHEEP -> new SpeedProfile(MOB_TO_PLAYER_MOVEMENT_SCALE, 1.0, 1.25);
      case CHICKEN -> new SpeedProfile(MOB_TO_PLAYER_MOVEMENT_SCALE, 1.0, 1.4);
      case CAT, OCELOT -> new SpeedProfile(MOB_TO_PLAYER_MOVEMENT_SCALE, 0.8, 1.33);
      case WOLF -> new SpeedProfile(MOB_TO_PLAYER_MOVEMENT_SCALE, 1.0, 1.5);
      case PIG -> new SpeedProfile(MOB_TO_PLAYER_MOVEMENT_SCALE, 1.0, 1.25);
      case RABBIT -> new SpeedProfile(MOB_TO_PLAYER_MOVEMENT_SCALE, 0.6, 2.2);
      case HORSE, DONKEY, MULE -> new SpeedProfile(1.0, 1.0, PLAYER_SPRINT_MULTIPLIER);
    };
  }

  private record SpeedProfile(
      double playerMovementScale, double walkMultiplier, double sprintMultiplier) {}
}
