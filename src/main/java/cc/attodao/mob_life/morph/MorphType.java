package cc.attodao.mob_life.morph;

import com.mojang.serialization.Codec;
import net.minecraft.world.entity.EntityType;

import java.util.Arrays;
import java.util.Optional;

public enum MorphType {
	PLAYER("player", EntityType.PLAYER),
	COW("cow", EntityType.COW),
	SHEEP("sheep", EntityType.SHEEP),
	CHICKEN("chicken", EntityType.CHICKEN);

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

	public String translationKey() {
		return "mob_life.morph." + id;
	}

	public static MorphType fromId(String id) {
		return Arrays.stream(values())
				.filter(type -> type.id.equals(id))
				.findFirst()
				.orElse(PLAYER);
	}

	public static Optional<MorphType> fromEntityType(EntityType<?> entityType) {
		return Arrays.stream(values())
				.filter(type -> type.entityType == entityType)
				.findFirst();
	}
}
