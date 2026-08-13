package cc.attodao.mob_life.world;

import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.DyeColor;

/**
 * A deliberately small selection-time request for cosmetic mob variants and baby state.
 *
 * <p>The SNBT editor is client-side UI only. Its result is reduced to these fields before it
 * crosses a client/server boundary, and the server validates registry-backed values again before
 * creating a morph definition.
 */
public record MorphVariantRequest(
    String variantId,
    String soundVariantId,
    int rabbitType,
    int sheepColor,
    int horseVariant,
    boolean baby) {
  public static final int UNSPECIFIED = -1;
  private static final int BABY_AGE = -24_000;
  private static final int MAX_IDENTIFIER_LENGTH = 128;
  private static final MorphVariantRequest EMPTY =
      new MorphVariantRequest("", "", UNSPECIFIED, UNSPECIFIED, UNSPECIFIED, false);

  public MorphVariantRequest {
    variantId = normalizeIdentifier(variantId);
    soundVariantId = normalizeIdentifier(soundVariantId);
    rabbitType = normalizeValue(rabbitType, 255);
    sheepColor = normalizeValue(sheepColor, 15);
    horseVariant = normalizeValue(horseVariant, 0xFFFF);
  }

  public static MorphVariantRequest empty() {
    return EMPTY;
  }

  public static MorphVariantRequest fromNbt(MorphType morph, CompoundTag nbt) {
    if (morph == null || nbt == null || nbt.isEmpty()) {
      return EMPTY;
    }

    boolean baby = isBaby(nbt);
    return switch (morph) {
      case CAT, COW, CHICKEN, PIG, WOLF ->
          new MorphVariantRequest(
              string(nbt, "variant", "Variant"),
              string(nbt, "sound_variant"),
              UNSPECIFIED,
              UNSPECIFIED,
              UNSPECIFIED,
              baby);
      case RABBIT ->
          new MorphVariantRequest(
              "", "", integer(nbt, "RabbitType"), UNSPECIFIED, UNSPECIFIED, baby);
      case SHEEP ->
          new MorphVariantRequest("", "", UNSPECIFIED, integer(nbt, "Color"), UNSPECIFIED, baby);
      case HORSE ->
          new MorphVariantRequest("", "", UNSPECIFIED, UNSPECIFIED, integer(nbt, "Variant"), baby);
      case OCELOT, DONKEY, MULE ->
          new MorphVariantRequest("", "", UNSPECIFIED, UNSPECIFIED, UNSPECIFIED, baby);
      default -> EMPTY;
    };
  }

  /**
   * Validates the limited SNBT accepted by the selection screen before it is reduced to a request.
   */
  public static boolean isSupportedNbt(MorphType morph, CompoundTag nbt) {
    if (nbt == null || nbt.isEmpty()) {
      return true;
    }
    if (!nbt.keySet().stream().allMatch(key -> isSupportedKey(morph, key)) || !validAge(nbt)) {
      return false;
    }

    return switch (morph) {
      case CAT, COW, CHICKEN, PIG, WOLF ->
          validIdentifier(nbt, "variant")
              && validIdentifier(nbt, "Variant")
              && validIdentifier(nbt, "sound_variant");
      case RABBIT -> nbt.getInt("RabbitType").map(MorphVariantRequest::isRabbitType).orElse(true);
      case SHEEP ->
          nbt.getInt("Color")
              .map(color -> color >= 0 && color < DyeColor.values().length)
              .orElse(true);
      case HORSE -> nbt.getInt("Variant").map(MorphVariantRequest::isHorseVariant).orElse(true);
      default -> true;
    };
  }

  /**
   * Produces a restricted temporary definition for local seed and structure preparation. The server
   * still calls {@link #resolve(ServerLevel, MorphType)} before persisting it.
   */
  public MorphDefinition toPendingDefinition(MorphType morph) {
    return new MorphDefinition(morph, requestedNbt(morph));
  }

  /**
   * Validates the requested appearance against server registries and emits only accepted fields.
   */
  public MorphDefinition resolve(ServerLevel level, MorphType morph) {
    CompoundTag nbt = new CompoundTag();
    switch (morph) {
      case CAT -> {
        addRegistryIdentifier(level, nbt, "variant", variantId, Registries.CAT_VARIANT);
        addRegistryIdentifier(
            level, nbt, "sound_variant", soundVariantId, Registries.CAT_SOUND_VARIANT);
      }
      case COW -> {
        addRegistryIdentifier(level, nbt, "variant", variantId, Registries.COW_VARIANT);
        addRegistryIdentifier(
            level, nbt, "sound_variant", soundVariantId, Registries.COW_SOUND_VARIANT);
      }
      case CHICKEN -> {
        addRegistryIdentifier(level, nbt, "variant", variantId, Registries.CHICKEN_VARIANT);
        addRegistryIdentifier(
            level, nbt, "sound_variant", soundVariantId, Registries.CHICKEN_SOUND_VARIANT);
      }
      case PIG -> {
        addRegistryIdentifier(level, nbt, "variant", variantId, Registries.PIG_VARIANT);
        addRegistryIdentifier(
            level, nbt, "sound_variant", soundVariantId, Registries.PIG_SOUND_VARIANT);
      }
      case WOLF -> {
        addRegistryIdentifier(level, nbt, "variant", variantId, Registries.WOLF_VARIANT);
        addRegistryIdentifier(
            level, nbt, "sound_variant", soundVariantId, Registries.WOLF_SOUND_VARIANT);
      }
      case RABBIT -> {
        if (isRabbitType(rabbitType)) {
          nbt.putInt("RabbitType", rabbitType);
        }
      }
      case SHEEP -> {
        if (sheepColor >= 0 && sheepColor < DyeColor.values().length) {
          nbt.putInt("Color", sheepColor);
        }
      }
      case HORSE -> {
        if (isHorseVariant(horseVariant)) {
          nbt.putInt("Variant", horseVariant);
        }
      }
      default -> {
        // This morph has no supported selection-time cosmetic NBT.
      }
    }
    addBabyAge(nbt, morph);
    return new MorphDefinition(morph, nbt);
  }

  private CompoundTag requestedNbt(MorphType morph) {
    CompoundTag nbt = new CompoundTag();
    switch (morph) {
      case CAT, COW, CHICKEN, PIG, WOLF -> {
        addIdentifier(nbt, "variant", variantId);
        addIdentifier(nbt, "sound_variant", soundVariantId);
      }
      case RABBIT -> {
        if (isRabbitType(rabbitType)) {
          nbt.putInt("RabbitType", rabbitType);
        }
      }
      case SHEEP -> {
        if (sheepColor >= 0 && sheepColor < DyeColor.values().length) {
          nbt.putInt("Color", sheepColor);
        }
      }
      case HORSE -> {
        if (isHorseVariant(horseVariant)) {
          nbt.putInt("Variant", horseVariant);
        }
      }
      default -> {
        // This morph has no supported selection-time cosmetic NBT.
      }
    }
    addBabyAge(nbt, morph);
    return nbt;
  }

  private static boolean isSupportedKey(MorphType morph, String key) {
    return switch (morph) {
      case CAT, COW, CHICKEN, PIG, WOLF ->
          key.equals("variant")
              || key.equals("Variant")
              || key.equals("sound_variant")
              || key.equals("Age");
      case RABBIT -> key.equals("RabbitType") || key.equals("Age");
      case SHEEP -> key.equals("Color") || key.equals("Age");
      case HORSE -> key.equals("Variant") || key.equals("Age");
      case OCELOT, DONKEY, MULE -> key.equals("Age");
      default -> false;
    };
  }

  private static String string(CompoundTag nbt, String... keys) {
    for (String key : keys) {
      if (nbt.contains(key)) {
        return nbt.getStringOr(key, "");
      }
    }
    return "";
  }

  private static int integer(CompoundTag nbt, String key) {
    return nbt.contains(key) ? nbt.getIntOr(key, UNSPECIFIED) : UNSPECIFIED;
  }

  private static boolean isRabbitType(int value) {
    for (Rabbit.Variant variant : Rabbit.Variant.values()) {
      if (variant.id() == value) {
        return true;
      }
    }
    return false;
  }

  private static boolean isHorseVariant(int value) {
    return value >= 0
        && value <= 0xFFFF
        && (value & 0xFF) < Variant.values().length
        && ((value >>> 8) & 0xFF) < Markings.values().length;
  }

  private static boolean validAge(CompoundTag nbt) {
    return nbt.getInt("Age").map(age -> age >= BABY_AGE && age <= 0).orElse(!nbt.contains("Age"));
  }

  private static boolean isBaby(CompoundTag nbt) {
    return nbt.getInt("Age").map(age -> age < 0).orElse(false);
  }

  private static boolean validIdentifier(CompoundTag nbt, String key) {
    return nbt.getString(key)
        .map(value -> value.length() <= MAX_IDENTIFIER_LENGTH && Identifier.tryParse(value) != null)
        .orElse(!nbt.contains(key));
  }

  private void addBabyAge(CompoundTag nbt, MorphType morph) {
    if (baby && morph != null && !morph.isPlayer()) {
      nbt.putInt("Age", BABY_AGE);
    }
  }

  private static <T> void addRegistryIdentifier(
      ServerLevel level,
      CompoundTag nbt,
      String key,
      String value,
      ResourceKey<? extends Registry<T>> registryKey) {
    Identifier identifier = Identifier.tryParse(value);
    if (identifier != null
        && level.registryAccess().lookupOrThrow(registryKey).get(identifier).isPresent()) {
      nbt.putString(key, identifier.toString());
    }
  }

  private static void addIdentifier(CompoundTag nbt, String key, String value) {
    Identifier identifier = Identifier.tryParse(value);
    if (identifier != null) {
      nbt.putString(key, identifier.toString());
    }
  }

  private static String normalizeIdentifier(String value) {
    if (value == null) {
      return "";
    }
    String result = value.trim();
    return result.length() <= MAX_IDENTIFIER_LENGTH ? result : "";
  }

  private static int normalizeValue(int value, int maximum) {
    return value >= UNSPECIFIED && value <= maximum ? value : UNSPECIFIED;
  }
}
