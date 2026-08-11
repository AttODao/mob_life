package cc.attodao.mob_life.morph;

import cc.attodao.mob_life.config.MorphConfigManager;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

public enum MorphType {
  PLAYER("player", EntityTypes.PLAYER),
  COW("cow", EntityTypes.COW),
  SHEEP("sheep", EntityTypes.SHEEP),
  CHICKEN("chicken", EntityTypes.CHICKEN),
  CAT("cat", EntityTypes.CAT),
  OCELOT("ocelot", EntityTypes.OCELOT),
  WOLF("wolf", EntityTypes.WOLF),
  PIG("pig", EntityTypes.PIG),
  HORSE("horse", EntityTypes.HORSE),
  DONKEY("donkey", EntityTypes.DONKEY),
  MULE("mule", EntityTypes.MULE),
  RABBIT("rabbit", EntityTypes.RABBIT);

  public static final Codec<MorphType> CODEC = Codec.STRING.xmap(MorphType::fromId, MorphType::id);

  private final String id;
  private final EntityType<?> entityType;

  MorphType(String id, EntityType<?> entityType) {
    this.id = id;
    this.entityType = entityType;
  }

  public String id() {
    return id;
  }

  public EntityType<?> entityType() {
    return entityType;
  }

  public boolean isPlayer() {
    return this == PLAYER;
  }

  public boolean isNocturnal() {
    return MorphConfigManager.get(this).sleep().schedule().equals("day");
  }

  public boolean canEquipSaddle() {
    return MorphConfigManager.get(this).traits().canEquipSaddle();
  }

  public boolean canEquipHorseArmor() {
    return MorphConfigManager.get(this).traits().canEquipHorseArmor();
  }

  public boolean canEquipWolfArmor() {
    return MorphConfigManager.get(this).traits().canEquipWolfArmor();
  }

  public boolean canEquipAnimalArmor() {
    return canEquipHorseArmor() || canEquipWolfArmor();
  }

  public boolean canEquipChest() {
    return MorphConfigManager.get(this).traits().canEquipChest();
  }

  public boolean isEquine() {
    return this == HORSE || this == DONKEY || this == MULE;
  }

  public String visionProfileId() {
    return MorphConfigManager.get(this).vision().profile();
  }

  public String translationKey() {
    return "mob_life.morph." + id;
  }

  public static MorphType fromId(String id) {
    return Arrays.stream(values()).filter(type -> type.id.equals(id)).findFirst().orElse(PLAYER);
  }

  public static Optional<MorphType> fromEntityType(EntityType<?> entityType) {
    return Arrays.stream(values()).filter(type -> type.entityType == entityType).findFirst();
  }
}
