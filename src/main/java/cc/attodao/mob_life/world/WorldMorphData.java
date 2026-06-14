package cc.attodao.mob_life.world;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WorldMorphData extends SavedData {

  private static final Codec<WorldMorphData> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      MorphType.CODEC.fieldOf("morph").forGetter(data -> data.definition.type()),
                      CompoundTag.CODEC
                          .optionalFieldOf("nbt", new CompoundTag())
                          .forGetter(data -> data.definition.nbt()),
                      Codec.BOOL
                          .optionalFieldOf("initial_spawn_configured", true)
                          .forGetter(WorldMorphData::initialSpawnConfigured))
                  .apply(
                      instance,
                      (type, nbt, configured) ->
                          new WorldMorphData(new MorphDefinition(type, nbt), configured)));

  public static final SavedDataType<WorldMorphData> TYPE =
      new SavedDataType<>(
          Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "world_morph"),
          () -> new WorldMorphData(PendingWorldSelection.consumeOrDefault()),
          CODEC,
          DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

  private MorphDefinition definition;
  private boolean initialSpawnConfigured;

  public WorldMorphData(MorphDefinition definition) {
    this(definition, false);
  }

  private WorldMorphData(MorphDefinition definition, boolean initialSpawnConfigured) {
    this.definition = definition;
    this.initialSpawnConfigured = initialSpawnConfigured;
  }

  public MorphType morph() {
    return definition.type();
  }

  public MorphDefinition definition() {
    return definition;
  }

  public boolean initialSpawnConfigured() {
    return initialSpawnConfigured;
  }

  public void markInitialSpawnConfigured() {
    if (!initialSpawnConfigured) {
      initialSpawnConfigured = true;
      setDirty();
    }
  }

  public void setDefinition(MorphDefinition definition) {
    if (this.definition.equals(definition)) {
      return;
    }

    this.definition = definition;
    setDirty();
  }
}
