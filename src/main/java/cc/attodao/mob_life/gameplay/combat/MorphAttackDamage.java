package cc.attodao.mob_life.gameplay.combat;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.rabbit.Rabbit;

public final class MorphAttackDamage {

  private MorphAttackDamage() {}

  public static double fromMorph(MorphType morph, LivingEntity livingMorph) {
    MorphConfig.Combat combat = MorphConfigManager.get(morph).combat();
    if (!hasAttackAi(morph, livingMorph)) {
      return 0.0;
    }
    return combat.attackDamage() >= 0.0
        ? combat.attackDamage()
        : livingMorph.getAttributeValue(Attributes.ATTACK_DAMAGE);
  }

  public static boolean hasAttackAi(MorphType morph, LivingEntity livingMorph) {
    return switch (MorphConfigManager.get(morph).combat().attackMode()) {
      case ALWAYS -> true;
      case EVIL_RABBIT ->
          livingMorph instanceof Rabbit rabbit && rabbit.getVariant() == Rabbit.Variant.EVIL;
      case NONE -> false;
    };
  }
}
