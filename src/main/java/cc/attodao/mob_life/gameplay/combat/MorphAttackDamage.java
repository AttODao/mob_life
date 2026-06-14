package cc.attodao.mob_life.gameplay.combat;

import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.rabbit.Rabbit;

public final class MorphAttackDamage {

  private MorphAttackDamage() {}

  public static double fromMorph(MorphType morph, LivingEntity livingMorph) {
    return hasAttackAi(morph, livingMorph)
        ? livingMorph.getAttributeValue(Attributes.ATTACK_DAMAGE)
        : 0.0;
  }

  public static boolean hasAttackAi(MorphType morph, LivingEntity livingMorph) {
    return switch (morph) {
      case CAT, OCELOT, WOLF -> true;
      case RABBIT ->
          livingMorph instanceof Rabbit rabbit && rabbit.getVariant() == Rabbit.Variant.EVIL;
      default -> false;
    };
  }
}
