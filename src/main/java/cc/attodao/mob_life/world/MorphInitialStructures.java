package cc.attodao.mob_life.world;

import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public final class MorphInitialStructures {

  public static final int SPAWN_BIOME_Y = 64;

  private static final int FORCED_STRUCTURE_SEARCH_RADIUS = 128;
  private static final int FORCED_STRUCTURE_SEARCH_STEP = 16;

  private static volatile MorphDefinition scopedDefinition;

  private MorphInitialStructures() {}

  public static <T> T withForcedDefinition(MorphDefinition definition, Supplier<T> action) {
    MorphDefinition previous = scopedDefinition;
    scopedDefinition = definition;
    try {
      return action.get();
    } finally {
      scopedDefinition = previous;
    }
  }

  public static Optional<MorphDefinition> activeDefinition() {
    Optional<MorphDefinition> pendingDefinition = PendingWorldSelection.peek();
    return pendingDefinition.isPresent()
        ? pendingDefinition
        : Optional.ofNullable(scopedDefinition);
  }

  public static Optional<ResourceKey<StructureSet>> requiredStructureSet(
      MorphDefinition definition) {
    if (definition.type() != MorphType.CAT) {
      return Optional.empty();
    }

    return Optional.of(
        isAllBlackCat(definition)
            ? BuiltinStructureSets.SWAMP_HUTS
            : BuiltinStructureSets.VILLAGES);
  }

  public static boolean requiresStructure(MorphDefinition definition) {
    return requiredStructureSet(definition).isPresent();
  }

  public static boolean biomeSupportsRequiredStructure(
      Holder<Biome> biome, MorphDefinition definition) {
    Optional<ResourceKey<StructureSet>> structureSet = requiredStructureSet(definition);
    if (structureSet.isEmpty()) {
      return false;
    }

    if (structureSet.get().equals(BuiltinStructureSets.SWAMP_HUTS)) {
      return biome.is(BiomeTags.HAS_SWAMP_HUT);
    }

    return biome.is(BiomeTags.HAS_VILLAGE_DESERT)
        || biome.is(BiomeTags.HAS_VILLAGE_PLAINS)
        || biome.is(BiomeTags.HAS_VILLAGE_SAVANNA)
        || biome.is(BiomeTags.HAS_VILLAGE_SNOWY)
        || biome.is(BiomeTags.HAS_VILLAGE_TAIGA);
  }

  public static Optional<ChunkPos> findForcedStructureChunk(
      BiomeSource biomeSource, Climate.Sampler sampler, MorphDefinition definition) {
    return findForcedStructureChunk(
        biomeSource,
        sampler,
        definition,
        predictedSpawn(sampler),
        FORCED_STRUCTURE_SEARCH_RADIUS,
        FORCED_STRUCTURE_SEARCH_STEP);
  }

  public static Optional<ChunkPos> findForcedStructureChunk(
      BiomeSource biomeSource,
      Climate.Sampler sampler,
      MorphDefinition definition,
      BlockPos center,
      int radius,
      int step) {
    if (!requiresStructure(definition)) {
      return Optional.empty();
    }

    ChunkPos centerChunk = ChunkPos.containing(center);
    if (chunkSupportsRequiredStructure(biomeSource, sampler, definition, centerChunk)) {
      return Optional.of(centerChunk);
    }

    int chunkRadius = Math.max(1, Math.ceilDiv(radius, 16));
    int chunkStep = Math.max(1, Math.ceilDiv(step, 16));
    for (int currentRadius = chunkStep; currentRadius <= chunkRadius; currentRadius += chunkStep) {
      for (int deltaX = -currentRadius; deltaX <= currentRadius; deltaX += chunkStep) {
        Optional<ChunkPos> north =
            supportedChunk(
                biomeSource,
                sampler,
                definition,
                new ChunkPos(centerChunk.x() + deltaX, centerChunk.z() - currentRadius));
        if (north.isPresent()) {
          return north;
        }

        Optional<ChunkPos> south =
            supportedChunk(
                biomeSource,
                sampler,
                definition,
                new ChunkPos(centerChunk.x() + deltaX, centerChunk.z() + currentRadius));
        if (south.isPresent()) {
          return south;
        }
      }

      for (int deltaZ = -currentRadius + chunkStep;
          deltaZ <= currentRadius - chunkStep;
          deltaZ += chunkStep) {
        Optional<ChunkPos> west =
            supportedChunk(
                biomeSource,
                sampler,
                definition,
                new ChunkPos(centerChunk.x() - currentRadius, centerChunk.z() + deltaZ));
        if (west.isPresent()) {
          return west;
        }

        Optional<ChunkPos> east =
            supportedChunk(
                biomeSource,
                sampler,
                definition,
                new ChunkPos(centerChunk.x() + currentRadius, centerChunk.z() + deltaZ));
        if (east.isPresent()) {
          return east;
        }
      }
    }

    return Optional.empty();
  }

  public static Optional<BlockPos> forcedStructureCenter(
      ServerLevel level, MorphDefinition definition) {
    return findForcedStructureChunk(
            level.getChunkSource().getGenerator().getBiomeSource(),
            level.getChunkSource().randomState().sampler(),
            definition)
        .map(
            chunk -> new BlockPos(chunk.getMiddleBlockX(), SPAWN_BIOME_Y, chunk.getMiddleBlockZ()));
  }

  public static boolean shouldForceStructurePlacement(
      StructurePlacement placement, ChunkGeneratorStructureState state, int sourceX, int sourceZ) {
    MorphDefinition definition = activeDefinition().orElse(null);
    if (definition == null) {
      return false;
    }

    ResourceKey<StructureSet> requiredSet = requiredStructureSet(definition).orElse(null);
    if (requiredSet == null || !usesPlacementForSet(state, requiredSet, placement)) {
      return false;
    }

    if (!(state instanceof ChunkGeneratorStructureStateAccess stateAccess)) {
      return false;
    }

    return findForcedStructureChunk(
            stateAccess.mobLife$getBiomeSource(), state.randomState().sampler(), definition)
        .filter(chunk -> chunk.x() == sourceX && chunk.z() == sourceZ)
        .isPresent();
  }

  public static boolean isAllBlackCat(MorphDefinition definition) {
    String variant = definition.nbt().getStringOr("variant", "");
    return variant.endsWith(":all_black") || variant.equals("all_black");
  }

  private static Optional<ChunkPos> supportedChunk(
      BiomeSource biomeSource,
      Climate.Sampler sampler,
      MorphDefinition definition,
      ChunkPos chunk) {
    return chunkSupportsRequiredStructure(biomeSource, sampler, definition, chunk)
        ? Optional.of(chunk)
        : Optional.empty();
  }

  private static boolean chunkSupportsRequiredStructure(
      BiomeSource biomeSource,
      Climate.Sampler sampler,
      MorphDefinition definition,
      ChunkPos chunk) {
    return biomeSupportsRequiredStructure(
            noiseBiome(biomeSource, sampler, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ()),
            definition)
        || biomeSupportsRequiredStructure(
            noiseBiome(biomeSource, sampler, chunk.getMinBlockX(), chunk.getMinBlockZ()),
            definition);
  }

  private static Holder<Biome> noiseBiome(
      BiomeSource biomeSource, Climate.Sampler sampler, int blockX, int blockZ) {
    return biomeSource.getNoiseBiome(
        QuartPos.fromBlock(blockX),
        QuartPos.fromBlock(SPAWN_BIOME_Y),
        QuartPos.fromBlock(blockZ),
        sampler);
  }

  private static boolean usesPlacementForSet(
      ChunkGeneratorStructureState state,
      ResourceKey<StructureSet> structureSetKey,
      StructurePlacement placement) {
    return state.possibleStructureSets().stream()
        .anyMatch(
            holder ->
                holder.unwrapKey().filter(structureSetKey::equals).isPresent()
                    && holder.value().placement() == placement);
  }

  private static BlockPos predictedSpawn(Climate.Sampler sampler) {
    ChunkPos spawnChunk = ChunkPos.containing(sampler.findSpawnPosition());
    return spawnChunk.getWorldPosition().offset(8, SPAWN_BIOME_Y, 8);
  }
}
