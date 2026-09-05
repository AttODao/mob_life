package cc.attodao.mob_life.gameplay.movement;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.inventory.MorphEquipment;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class MorphMovementSpeed {
  private static final Identifier VANILLA_SPRINTING = Identifier.withDefaultNamespace("sprinting");

  private MorphMovementSpeed() {}

  public static double controllerSpeed(
      MorphType morph, MorphConfig.MovementState state, double effectiveMovementSpeed) {
    return MorphConfigManager.get(morph)
        .movement()
        .value(state)
        .controllerSpeed(effectiveMovementSpeed);
  }

  /** Expresses body-forward movement in the view-relative input coordinates used by travel. */
  public static RelativeInput bodyForwardInput(float bodyYaw, float viewYaw, float speed) {
    float angle = Mth.wrapDegrees(bodyYaw - viewYaw) * Mth.DEG_TO_RAD;
    return new RelativeInput(-Mth.sin(angle) * speed, Mth.cos(angle) * speed);
  }

  public static boolean canSprint(Player player) {
    MorphType morph = MorphEquipment.morph(player);
    return canSprint(morph);
  }

  public static boolean canSprint(MorphType morph) {
    return !morph.isPlayer()
        && MorphConfigManager.get(morph)
            .movement()
            .states()
            .containsKey(MorphConfig.MovementState.SPRINT);
  }

  public static void refresh(Player player) {
    AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (movementSpeed == null) {
      return;
    }

    MorphType morph = MorphEquipment.morph(player);
    if (morph.isPlayer()) {
      movementSpeed.removeModifier(MorphAttributeModifiers.SPRINT_SPEED);
      return;
    }
    movementSpeed.removeModifier(MorphAttributeModifiers.SPRINT_SPEED);
    movementSpeed.removeModifier(VANILLA_SPRINTING);
  }

  public record RelativeInput(float sideways, float forward) {}
}
