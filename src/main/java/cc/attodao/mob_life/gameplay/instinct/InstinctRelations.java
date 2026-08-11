package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.targeting.MorphPredation;
import cc.attodao.mob_life.morph.MorphType;
import java.util.OptionalInt;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.turtle.Turtle;

public final class InstinctRelations {
  private InstinctRelations() {}

  public static boolean isPredator(Entity entity, MorphType morph) {
    return entity instanceof net.minecraft.world.entity.Mob mob
        && MorphPredation.isPredatorFor(mob, morph);
  }

  public static boolean isPrey(Entity entity, MorphType morph) {
    return nutrition(entity, MorphConfigManager.get(morph)).isPresent();
  }

  public static OptionalInt nutrition(Entity entity, MorphConfig config) {
    if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
      return OptionalInt.empty();
    }

    MorphConfig.Prey tagMatch = null;
    for (MorphConfig.Prey prey : config.instinct().hunting().prey()) {
      if (!matches(prey.selector(), entity)) {
        continue;
      }
      if (!prey.selector().startsWith("#")) {
        return eligibleAge(entity) ? OptionalInt.of(prey.nutrition()) : OptionalInt.empty();
      }
      if (tagMatch == null || prey.nutrition() > tagMatch.nutrition()) {
        tagMatch = prey;
      }
    }
    return tagMatch != null && eligibleAge(entity)
        ? OptionalInt.of(tagMatch.nutrition())
        : OptionalInt.empty();
  }

  public static boolean matches(String selector, Entity entity) {
    String value = selector.startsWith("#") ? selector.substring(1) : selector;
    Identifier id = Identifier.tryParse(value);
    if (id == null) {
      return false;
    }
    if (selector.startsWith("#")) {
      TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, id);
      return entity.getType().builtInRegistryHolder().is(tag);
    }
    return EntityType.getKey(entity.getType()).equals(id);
  }

  private static boolean eligibleAge(Entity entity) {
    if (entity instanceof Turtle turtle) {
      return turtle.isBaby();
    }
    return !(entity instanceof Animal animal) || animal.isAlive();
  }
}
