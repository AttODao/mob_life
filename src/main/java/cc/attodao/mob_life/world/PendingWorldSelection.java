package cc.attodao.mob_life.world;

import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;

public final class PendingWorldSelection {

    private static MorphDefinition pending;

    private PendingWorldSelection() {}

    public static synchronized void setForNextWorld(
        MorphDefinition definition
    ) {
        pending = definition;
    }

    public static synchronized MorphDefinition consumeOrDefault() {
        MorphDefinition result =
            pending != null ? pending : MorphDefinition.of(MorphType.PLAYER);
        pending = null;
        return result;
    }
}
