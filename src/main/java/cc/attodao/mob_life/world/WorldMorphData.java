package cc.attodao.mob_life.world;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WorldMorphData extends SavedData {

    private static final Codec<WorldMorphData> CODEC =
        MorphDefinition.CODEC.xmap(
            WorldMorphData::new,
            WorldMorphData::definition
        );

    public static final SavedDataType<WorldMorphData> TYPE =
        new SavedDataType<>(
            Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "world_morph"),
            () -> new WorldMorphData(PendingWorldSelection.consumeOrDefault()),
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        );

    private MorphDefinition definition;

    public WorldMorphData(MorphDefinition definition) {
        this.definition = definition;
    }

    public MorphType morph() {
        return definition.type();
    }

    public MorphDefinition definition() {
        return definition;
    }

    public void setDefinition(MorphDefinition definition) {
        if (this.definition.equals(definition)) {
            return;
        }

        this.definition = definition;
        setDirty();
    }
}
