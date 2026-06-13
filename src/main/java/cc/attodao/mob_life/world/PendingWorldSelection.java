package cc.attodao.mob_life.world;

import cc.attodao.mob_life.morph.MorphType;

public final class PendingWorldSelection {
	private static MorphType pending;

	private PendingWorldSelection() {
	}

	public static synchronized void setForNextWorld(MorphType morphType) {
		pending = morphType;
	}

	public static synchronized MorphType consumeOrDefault() {
		MorphType result = pending != null ? pending : MorphType.PLAYER;
		pending = null;
		return result;
	}
}
