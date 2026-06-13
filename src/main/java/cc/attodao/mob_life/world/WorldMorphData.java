package cc.attodao.mob_life.world;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.world.PendingWorldSelection;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WorldMorphData extends SavedData {
	private static final Codec<WorldMorphData> CODEC = MorphType.CODEC
			.fieldOf("morph")
			.xmap(WorldMorphData::new, WorldMorphData::morph)
			.codec();

	public static final SavedDataType<WorldMorphData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "world_morph"),
			() -> new WorldMorphData(PendingWorldSelection.consumeOrDefault()),
			CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private MorphType morph;

	public WorldMorphData(MorphType morph) {
		this.morph = morph;
	}

	public MorphType morph() {
		return morph;
	}

	public void setMorph(MorphType morph) {
		if (this.morph == morph) {
			return;
		}

		this.morph = morph;
		setDirty();
	}
}
