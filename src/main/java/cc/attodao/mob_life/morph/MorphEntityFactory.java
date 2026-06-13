package cc.attodao.mob_life.morph;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;

public final class MorphEntityFactory {

    private MorphEntityFactory() {}

    public static Entity create(MorphDefinition definition, Level level) {
        Entity entity = definition
            .type()
            .entityType()
            .create(level, EntitySpawnReason.LOAD);
        if (entity != null && !definition.nbt().isEmpty()) {
            entity.load(
                TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    entity.registryAccess(),
                    definition.nbt()
                )
            );
            entity.refreshDimensions();
        }
        return entity;
    }
}
