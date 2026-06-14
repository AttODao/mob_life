package cc.attodao.mob_life.morph;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class MorphEntityFactory {
  private static final String RANDOMIZED_TAG = "MobLifeRandomized";

  private MorphEntityFactory() {}

  public static Entity create(MorphDefinition definition, Level level) {
    Entity entity = definition.type().entityType().create(level, EntitySpawnReason.LOAD);
    if (entity != null && !definition.nbt().isEmpty()) {
      entity.load(
          TagValueInput.create(
              ProblemReporter.DISCARDING, entity.registryAccess(), definition.nbt()));
      entity.refreshDimensions();
    }
    return entity;
  }

  public static MorphDefinition randomizeAt(
      MorphDefinition definition, ServerLevel level, BlockPos pos) {
    CompoundTag requestedNbt = definition.nbt();
    if (!definition.hasMobForm() || requestedNbt.getBooleanOr(RANDOMIZED_TAG, false)) {
      return definition;
    }

    Entity entity =
        definition.type().entityType().create(level, EntitySpawnReason.CHUNK_GENERATION);
    if (!(entity instanceof Mob mob)) {
      return definition;
    }

    mob.snapTo(pos, level.getRandom().nextFloat() * 360.0F, 0.0F);
    mob.finalizeSpawn(
        level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.CHUNK_GENERATION, null);
    if (definition.type() == MorphType.DONKEY || definition.type() == MorphType.MULE) {
      mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(randomHorseSpeed(level));
      mob.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(randomHorseJumpStrength(level));
    }
    if (!requestedNbt.isEmpty()) {
      mob.load(
          TagValueInput.create(ProblemReporter.DISCARDING, mob.registryAccess(), requestedNbt));
      mob.refreshDimensions();
    }

    TagValueOutput output =
        TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
    mob.saveWithoutId(output);
    CompoundTag randomizedNbt = output.buildResult();
    if (!requestedNbt.contains("Health")) {
      randomizedNbt.remove("Health");
    }
    randomizedNbt.putBoolean(RANDOMIZED_TAG, true);
    return new MorphDefinition(definition.type(), randomizedNbt);
  }

  private static double randomHorseSpeed(ServerLevel level) {
    return (0.45
            + level.getRandom().nextDouble() * 0.3
            + level.getRandom().nextDouble() * 0.3
            + level.getRandom().nextDouble() * 0.3)
        * 0.25;
  }

  private static double randomHorseJumpStrength(ServerLevel level) {
    return (0.4
        + level.getRandom().nextDouble() * 0.2
        + level.getRandom().nextDouble() * 0.2
        + level.getRandom().nextDouble() * 0.2);
  }
}
