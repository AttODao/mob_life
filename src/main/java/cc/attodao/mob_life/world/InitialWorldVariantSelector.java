package cc.attodao.mob_life.world;

import cc.attodao.mob_life.morph.MorphDefinition;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.storage.TagValueOutput;

public final class InitialWorldVariantSelector {

  private static final WeightedList<DyeColor> SHEEP_COLORS =
      WeightedList.<DyeColor>builder()
          .add(DyeColor.WHITE, 92)
          .add(DyeColor.BROWN, 88)
          .add(DyeColor.BLACK, 90)
          .add(DyeColor.GRAY, 15)
          .add(DyeColor.LIGHT_GRAY, 15)
          .add(DyeColor.PINK, 3)
          .build();

  private InitialWorldVariantSelector() {}

  public static MorphDefinition randomize(
      RegistryAccess registryAccess, MorphDefinition definition) {
    if (!definition.hasMobForm()) {
      return definition;
    }

    CompoundTag nbt = definition.nbt();
    RandomSource random = RandomSource.create();
    boolean changed =
        switch (definition.type()) {
          case CAT ->
              randomizeRegistryKey(nbt, registryAccess, "variant", Registries.CAT_VARIANT, random)
                  | randomizeRegistryKey(
                      nbt, registryAccess, "sound_variant", Registries.CAT_SOUND_VARIANT, random);
          case COW ->
              randomizeRegistryKey(nbt, registryAccess, "variant", Registries.COW_VARIANT, random)
                  | randomizeRegistryKey(
                      nbt, registryAccess, "sound_variant", Registries.COW_SOUND_VARIANT, random);
          case CHICKEN ->
              randomizeRegistryKey(
                      nbt, registryAccess, "variant", Registries.CHICKEN_VARIANT, random)
                  | randomizeRegistryKey(
                      nbt,
                      registryAccess,
                      "sound_variant",
                      Registries.CHICKEN_SOUND_VARIANT,
                      random);
          case PIG ->
              randomizeRegistryKey(nbt, registryAccess, "variant", Registries.PIG_VARIANT, random)
                  | randomizeRegistryKey(
                      nbt, registryAccess, "sound_variant", Registries.PIG_SOUND_VARIANT, random);
          case WOLF ->
              randomizeRegistryKey(nbt, registryAccess, "variant", Registries.WOLF_VARIANT, random)
                  | randomizeRegistryKey(
                      nbt, registryAccess, "sound_variant", Registries.WOLF_SOUND_VARIANT, random);
          case SHEEP -> randomizeSheepColor(nbt, random);
          case RABBIT -> randomizeRabbitVariant(nbt, random);
          case HORSE -> randomizeHorseVariant(nbt, random) | randomizeEquineAttributes(nbt, random);
          case DONKEY, MULE -> randomizeEquineAttributes(nbt, random);
          default -> false;
        };

    return changed ? new MorphDefinition(definition.type(), nbt) : definition;
  }

  private static boolean randomizeSheepColor(CompoundTag nbt, RandomSource random) {
    if (nbt.contains("Color")) {
      return false;
    }

    DyeColor color = SHEEP_COLORS.getRandomOrThrow(random);
    TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
    output.store("Color", DyeColor.LEGACY_ID_CODEC, color);
    nbt.merge(output.buildResult());
    return true;
  }

  private static boolean randomizeRabbitVariant(CompoundTag nbt, RandomSource random) {
    if (nbt.contains("RabbitType") || nbt.contains("variant")) {
      return false;
    }

    Rabbit.Variant[] variants = Rabbit.Variant.values();
    Rabbit.Variant selected = variants[random.nextInt(variants.length)];
    nbt.putInt("RabbitType", selected.id());
    return true;
  }

  private static boolean randomizeHorseVariant(CompoundTag nbt, RandomSource random) {
    if (nbt.contains("Variant")) {
      return false;
    }

    Variant variant = Util.getRandom(Variant.values(), random);
    Markings markings = Util.getRandom(Markings.values(), random);
    nbt.putInt("Variant", variant.getId() & 255 | markings.getId() << 8 & 65280);
    return true;
  }

  private static boolean randomizeEquineAttributes(CompoundTag nbt, RandomSource random) {
    if (nbt.contains("attributes")) {
      return false;
    }

    TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
    output.store(
        "attributes",
        AttributeInstance.Packed.LIST_CODEC,
        List.of(
            new AttributeInstance.Packed(
                Attributes.MAX_HEALTH, randomEquineMaxHealth(random), List.<AttributeModifier>of()),
            new AttributeInstance.Packed(
                Attributes.MOVEMENT_SPEED, randomEquineSpeed(random), List.<AttributeModifier>of()),
            new AttributeInstance.Packed(
                Attributes.JUMP_STRENGTH,
                randomEquineJumpStrength(random),
                List.<AttributeModifier>of())));
    nbt.merge(output.buildResult());
    return true;
  }

  private static double randomEquineMaxHealth(RandomSource random) {
    return 15.0F + random.nextInt(8) + random.nextInt(9);
  }

  private static double randomEquineSpeed(RandomSource random) {
    return (0.45F
            + random.nextDouble() * 0.3
            + random.nextDouble() * 0.3
            + random.nextDouble() * 0.3)
        * 0.25;
  }

  private static double randomEquineJumpStrength(RandomSource random) {
    return 0.4F + random.nextDouble() * 0.2 + random.nextDouble() * 0.2 + random.nextDouble() * 0.2;
  }

  private static <T> boolean randomizeRegistryKey(
      CompoundTag nbt,
      RegistryAccess registryAccess,
      String tagName,
      ResourceKey<? extends Registry<T>> registryKey,
      RandomSource random) {
    if (nbt.contains(tagName)) {
      return false;
    }

    String identifier =
        registryAccess
            .lookupOrThrow(registryKey)
            .getRandom(random)
            .flatMap(holder -> holder.unwrapKey())
            .orElseThrow()
            .identifier()
            .toString();
    nbt.putString(tagName, identifier);
    return true;
  }
}
