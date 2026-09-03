package cc.attodao.mob_life.morph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;

public record MorphDefinition(MorphType type, CompoundTag nbt) {
  public static final Codec<MorphDefinition> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      MorphType.CODEC.fieldOf("morph").forGetter(MorphDefinition::type),
                      CompoundTag.CODEC
                          .optionalFieldOf("nbt", new CompoundTag())
                          .forGetter(MorphDefinition::nbt))
                  .apply(instance, MorphDefinition::new));

  private static final List<String> ENTITY_IDENTITY_TAGS =
      List.of(
          "id",
          "UUID",
          "Pos",
          "Motion",
          "Rotation",
          "Passengers",
          "Brain",
          "Leash",
          "PortalCooldown",
          "Owner",
          "owner",
          "Sitting",
          "Tame",
          "tame",
          "Trusting",
          "trusting",
          "leash");

  public MorphDefinition {
    Objects.requireNonNull(type, "type");
    nbt = type.isPlayer() ? new CompoundTag() : sanitize(type, nbt);
  }

  public static MorphDefinition of(MorphType type) {
    return new MorphDefinition(type, new CompoundTag());
  }

  @Override
  public CompoundTag nbt() {
    return nbt.copy();
  }

  public boolean hasMobForm() {
    return !type.isPlayer();
  }

  public boolean hasHealthOverride() {
    return nbt.contains("Health");
  }

  private static CompoundTag sanitize(MorphType type, CompoundTag source) {
    CompoundTag sanitized = source != null ? source.copy() : new CompoundTag();
    if (type != MorphType.HORSE
        && !sanitized.contains("variant")
        && sanitized.contains("Variant")) {
      sanitized.put("variant", sanitized.get("Variant").copy());
      sanitized.remove("Variant");
    }
    for (String key : ENTITY_IDENTITY_TAGS) {
      sanitized.remove(key);
    }
    return sanitized;
  }
}
