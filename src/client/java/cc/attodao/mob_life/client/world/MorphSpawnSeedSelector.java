package cc.attodao.mob_life.client.world;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.world.MorphInitialSpawn;
import java.util.OptionalLong;
import java.util.SplittableRandom;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public final class MorphSpawnSeedSelector {

  private static final int MAX_ATTEMPTS = 64;
  private static final int BIOME_SEARCH_RADIUS = 128;
  private static final int BIOME_SEARCH_STEP = 32;
  private static final int SPAWN_BIOME_Y = 64;

  private MorphSpawnSeedSelector() {}

  public static OptionalLong findSeed(
      WorldCreationContext context, MorphDefinition definition, IntConsumer progress) {
    if (!definition.hasMobForm()) {
      return OptionalLong.empty();
    }

    ChunkGenerator generator = context.selectedDimensions().overworld();
    if (!hasAnySupportedBiome(generator, definition)) {
      return OptionalLong.empty();
    }

    long baseSeed = context.options().seed();
    SplittableRandom random =
        new SplittableRandom(baseSeed ^ ((long) definition.type().id().hashCode() << 32));
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      progress.accept(attempt + 1);
      long seed = attempt == 0 ? baseSeed : random.nextLong();
      if (supportsSpawnNearPredictedSpawn(context, generator, definition, seed)) {
        return OptionalLong.of(seed);
      }
    }

    return OptionalLong.empty();
  }

  public static int maxAttempts() {
    return MAX_ATTEMPTS;
  }

  private static boolean hasAnySupportedBiome(
      ChunkGenerator generator, MorphDefinition definition) {
    return generator.getBiomeSource().possibleBiomes().stream()
        .anyMatch(
            biome ->
                MorphInitialSpawn.biomeSupportsInitialSpawn(
                    biome, definition, definition.type().entityType()));
  }

  private static boolean supportsSpawnNearPredictedSpawn(
      WorldCreationContext context,
      ChunkGenerator generator,
      MorphDefinition definition,
      long seed) {
    try {
      Climate.Sampler sampler = samplerFor(context, generator, seed);
      BlockPos predictedSpawn = predictedSpawn(sampler);
      return hasSupportedBiomeNear(generator.getBiomeSource(), sampler, definition, predictedSpawn);
    } catch (RuntimeException exception) {
      MobLife.LOGGER.debug("Could not evaluate morph spawn seed {}", seed, exception);
      return false;
    }
  }

  private static Climate.Sampler samplerFor(
      WorldCreationContext context, ChunkGenerator generator, long seed) {
    if (generator instanceof NoiseBasedChunkGenerator noiseGenerator) {
      var noises = context.worldgenLoadContext().lookupOrThrow(Registries.NOISE);
      RandomState randomState =
          RandomState.create(noiseGenerator.generatorSettings().value(), noises, seed);
      return randomState.sampler();
    }
    return Climate.empty();
  }

  private static BlockPos predictedSpawn(Climate.Sampler sampler) {
    ChunkPos spawnChunk = ChunkPos.containing(sampler.findSpawnPosition());
    return spawnChunk.getWorldPosition().offset(8, SPAWN_BIOME_Y, 8);
  }

  private static boolean hasSupportedBiomeNear(
      BiomeSource biomeSource,
      Climate.Sampler sampler,
      MorphDefinition definition,
      BlockPos center) {
    if (isSupportedBiome(biomeSource, sampler, definition, center.getX(), center.getZ())) {
      return true;
    }

    for (int radius = BIOME_SEARCH_STEP;
        radius <= BIOME_SEARCH_RADIUS;
        radius += BIOME_SEARCH_STEP) {
      for (int deltaX = -radius; deltaX <= radius; deltaX += BIOME_SEARCH_STEP) {
        if (isSupportedBiome(
                biomeSource, sampler, definition, center.getX() + deltaX, center.getZ() - radius)
            || isSupportedBiome(
                biomeSource, sampler, definition, center.getX() + deltaX, center.getZ() + radius)) {
          return true;
        }
      }

      for (int deltaZ = -radius + BIOME_SEARCH_STEP;
          deltaZ <= radius - BIOME_SEARCH_STEP;
          deltaZ += BIOME_SEARCH_STEP) {
        if (isSupportedBiome(
                biomeSource, sampler, definition, center.getX() - radius, center.getZ() + deltaZ)
            || isSupportedBiome(
                biomeSource, sampler, definition, center.getX() + radius, center.getZ() + deltaZ)) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isSupportedBiome(
      BiomeSource biomeSource,
      Climate.Sampler sampler,
      MorphDefinition definition,
      int blockX,
      int blockZ) {
    Holder<Biome> biome =
        biomeSource.getNoiseBiome(
            QuartPos.fromBlock(blockX),
            QuartPos.fromBlock(SPAWN_BIOME_Y),
            QuartPos.fromBlock(blockZ),
            sampler);
    return MorphInitialSpawn.biomeSupportsInitialSpawn(
        biome, definition, definition.type().entityType());
  }
}
