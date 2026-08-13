package cc.attodao.mob_life.world;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MorphInitialSpawn {

  private static final int PLACEMENT_SEARCH_RADIUS = 48;
  private static final int PLACEMENT_SEARCH_STEP = 16;
  private static final int MAX_NEW_CHUNKS_PER_SEARCH = 4;
  private static final int BIOME_SEARCH_RADIUS = 8_192;
  private static final int BIOME_SEARCH_HORIZONTAL_INTERVAL = 64;
  private static final int BIOME_SEARCH_VERTICAL_INTERVAL = 32;
  private static final int NEARBY_MOB_RADIUS = 32;
  private static final int MAX_GROUP_SIZE = 4;
  private static final int SPAWN_ATTEMPTS_PER_MOB = 24;

  private MorphInitialSpawn() {}

  public static void configure(ServerLevel level, WorldMorphData morphData) {
    if (morphData.initialSpawnConfigured()) {
      return;
    }

    MorphType morph = morphData.morph();
    if (morph.isPlayer()) {
      morphData.markInitialSpawnConfigured();
      return;
    }

    EntityType<?> entityType = morph.entityType();
    BlockPos origin = level.getRespawnData().pos();
    BlockPos spawnPos =
        MorphInitialStructures.withForcedDefinition(
            morphData.definition(), () -> findSpawnPosition(level, morphData, entityType, origin));

    if (spawnPos == null) {
      // The biome lookup is deliberately generation-free and placement only opens a small,
      // bounded number of chunks. Keep the state pending so a later retry can finish safely.
      MobLife.LOGGER.warn(
          "Could not find a valid initial spawn for {} form yet; it will be retried", morph.id());
      return;
    }

    level.setRespawnData(LevelData.RespawnData.of(level.dimension(), spawnPos, 0.0F, 0.0F));
    MorphDefinition randomizedDefinition =
        MorphEntityFactory.randomizeAt(morphData.definition(), level, spawnPos);
    morphData.setDefinition(randomizedDefinition);
    if (shouldSpawnNearbyGroup(morph)) {
      ensureNearbyGroup(level, spawnPos, randomizedDefinition, entityType);
    }
    morphData.markInitialSpawnConfigured();
    MobLife.LOGGER.info("Initial {} spawn set to {}", morph.id(), spawnPos);
  }

  private static BlockPos findSpawnPosition(
      ServerLevel level, WorldMorphData morphData, EntityType<?> entityType, BlockPos origin) {
    boolean isCat = morphData.morph() == MorphType.CAT;
    boolean allBlackCat = isCat && MorphInitialStructures.isAllBlackCat(morphData.definition());
    if (isCat) {
      return findCatSpawnPosition(level, morphData.definition(), entityType, origin, allBlackCat);
    }

    return findNaturalSpawnPosition(level, morphData.definition(), entityType, origin);
  }

  private static BlockPos findCatSpawnPosition(
      ServerLevel level,
      MorphDefinition definition,
      EntityType<?> entityType,
      BlockPos origin,
      boolean allBlackCat) {
    BlockPos forcedStructureCenter =
        MorphInitialStructures.forcedStructureCenter(level, definition).orElse(null);
    if (forcedStructureCenter != null) {
      BlockPos spawnPos =
          findInitialSpawn(
              level,
              forcedStructureCenter,
              entityType,
              pos -> isCatSpawnPosition(level, definition, entityType, pos, allBlackCat),
              PLACEMENT_SEARCH_RADIUS);
      if (spawnPos != null) {
        return spawnPos;
      }
    }

    BlockPos catStructure =
        level.findNearestMapStructure(
            allBlackCat ? StructureTags.CATS_SPAWN_AS_BLACK : StructureTags.CATS_SPAWN_IN,
            origin,
            16,
            false);
    if (catStructure != null) {
      BlockPos spawnPos =
          findInitialSpawn(
              level,
              catStructure,
              entityType,
              pos -> isCatSpawnPosition(level, definition, entityType, pos, allBlackCat),
              PLACEMENT_SEARCH_RADIUS);
      if (spawnPos != null) {
        return spawnPos;
      }
    }

    // A village around the existing spawn can satisfy the same vanilla condition without a
    // structure search result, so retain it as a final local placement attempt.
    return findInitialSpawn(
        level,
        origin,
        entityType,
        pos -> isCatSpawnPosition(level, definition, entityType, pos, allBlackCat),
        PLACEMENT_SEARCH_RADIUS);
  }

  private static BlockPos findNaturalSpawnPosition(
      ServerLevel level, MorphDefinition definition, EntityType<?> entityType, BlockPos origin) {
    Pair<BlockPos, Holder<Biome>> biomeMatch =
        level.findClosestBiome3d(
            biome -> biomeSupportsInitialSpawn(biome, definition, entityType),
            origin,
            BIOME_SEARCH_RADIUS,
            BIOME_SEARCH_HORIZONTAL_INTERVAL,
            BIOME_SEARCH_VERTICAL_INTERVAL);
    if (biomeMatch == null) {
      return null;
    }

    return findInitialSpawn(
        level,
        biomeMatch.getFirst(),
        entityType,
        pos -> isInitialSpawnPosition(level, definition, entityType, pos),
        PLACEMENT_SEARCH_RADIUS);
  }

  private static BlockPos findInitialSpawn(
      ServerLevel level,
      BlockPos spawnSuggestion,
      EntityType<?> entityType,
      Predicate<BlockPos> spawnPredicate,
      int radius) {
    int[] newChunkBudget = {MAX_NEW_CHUNKS_PER_SEARCH};
    BlockPos spawnPos =
        testInitialSpawn(level, spawnSuggestion, entityType, spawnPredicate, newChunkBudget);
    if (spawnPos != null) {
      return spawnPos;
    }

    for (int currentRadius = PLACEMENT_SEARCH_STEP;
        currentRadius <= radius;
        currentRadius += PLACEMENT_SEARCH_STEP) {
      for (int deltaX = -currentRadius; deltaX <= currentRadius; deltaX += PLACEMENT_SEARCH_STEP) {
        spawnPos =
            testInitialSpawn(
                level,
                spawnSuggestion.offset(deltaX, 0, -currentRadius),
                entityType,
                spawnPredicate,
                newChunkBudget);
        if (spawnPos != null) {
          return spawnPos;
        }
        spawnPos =
            testInitialSpawn(
                level,
                spawnSuggestion.offset(deltaX, 0, currentRadius),
                entityType,
                spawnPredicate,
                newChunkBudget);
        if (spawnPos != null) {
          return spawnPos;
        }
      }

      for (int deltaZ = -currentRadius + PLACEMENT_SEARCH_STEP;
          deltaZ <= currentRadius - PLACEMENT_SEARCH_STEP;
          deltaZ += PLACEMENT_SEARCH_STEP) {
        spawnPos =
            testInitialSpawn(
                level,
                spawnSuggestion.offset(-currentRadius, 0, deltaZ),
                entityType,
                spawnPredicate,
                newChunkBudget);
        if (spawnPos != null) {
          return spawnPos;
        }
        spawnPos =
            testInitialSpawn(
                level,
                spawnSuggestion.offset(currentRadius, 0, deltaZ),
                entityType,
                spawnPredicate,
                newChunkBudget);
        if (spawnPos != null) {
          return spawnPos;
        }
      }
    }

    return null;
  }

  private static BlockPos testInitialSpawn(
      ServerLevel level,
      BlockPos suggestion,
      EntityType<?> entityType,
      Predicate<BlockPos> spawnPredicate,
      int[] newChunkBudget) {
    if (!level.getWorldBorder().isWithinBounds(suggestion)) {
      return null;
    }

    int chunkX = SectionPos.blockToSectionCoord(suggestion.getX());
    int chunkZ = SectionPos.blockToSectionCoord(suggestion.getZ());
    LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
    if (chunk == null) {
      if (newChunkBudget[0] <= 0) {
        return null;
      }
      newChunkBudget[0]--;
      chunk = level.getChunk(chunkX, chunkZ);
    }

    BlockPos spawnPos = getOverworldRespawnPos(level, suggestion.getX(), suggestion.getZ(), chunk);
    if (spawnPos == null) {
      return null;
    }
    spawnPos = SpawnPlacements.getPlacementType(entityType).adjustSpawnPosition(level, spawnPos);
    return spawnPredicate.test(spawnPos) ? spawnPos : null;
  }

  public static boolean biomeSupportsInitialSpawn(
      Holder<Biome> biome, MorphDefinition definition, EntityType<?> entityType) {
    if (definition.type() == MorphType.RABBIT) {
      return rabbitVariantSupportsBiome(rabbitVariant(definition), biome, entityType);
    }

    String variant = definition.nbt().getStringOr("variant", "");
    if (!variant.isEmpty()) {
      if (definition.type() == MorphType.COW
          || definition.type() == MorphType.CHICKEN
          || definition.type() == MorphType.PIG) {
        if (variant.endsWith(":warm")) {
          return biome.is(BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
        }
        if (variant.endsWith(":cold")) {
          return biome.is(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
        }
      }
      if (definition.type() == MorphType.WOLF) {
        return wolfVariantMatchesBiome(variant, biome);
      }
    }

    if (definition.type() == MorphType.CAT) {
      return MorphInitialStructures.biomeSupportsRequiredStructure(biome, definition);
    }
    if (definition.type() == MorphType.MULE) {
      return biome.is(Biomes.PLAINS) || biome.is(BiomeTags.IS_SAVANNA);
    }
    return matchingSpawnerData(biome, entityType).isPresent();
  }

  private static BlockPos getOverworldRespawnPos(
      final ServerLevel level, final int x, final int z, final LevelChunk chunk) {
    boolean caveWorld = level.dimensionType().hasCeiling();
    int topY =
        caveWorld
            ? level.getChunkSource().getGenerator().getSpawnHeight(level)
            : chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
    if (topY < level.getMinY()) {
      return null;
    }

    int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
    if (surface <= topY && surface > chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15)) {
      return null;
    }

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    for (int y = topY + 1; y >= level.getMinY(); y--) {
      pos.set(x, y, z);
      BlockState blockState = level.getBlockState(pos);
      if (!blockState.getFluidState().isEmpty()) {
        break;
      }

      if (Block.isFaceFull(blockState.getCollisionShape(level, pos), Direction.UP)) {
        return pos.above().immutable();
      }
    }

    return null;
  }

  private static boolean noCollisionNoLiquid(final CollisionGetter level, final BlockPos pos) {
    return level.noCollision(
        null, EntityTypes.PLAYER.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(pos)), true);
  }

  private static boolean isInitialSpawnPosition(
      ServerLevel level, MorphDefinition definition, EntityType<?> entityType, BlockPos pos) {
    return noCollisionNoLiquid(level, pos)
        && level.noCollision(
            entityType.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))
        && biomeSupportsInitialSpawn(level.getBiome(pos), definition, entityType)
        && SpawnPlacements.isSpawnPositionOk(entityType, level, pos)
        && SpawnPlacements.checkSpawnRules(
            entityType, level, EntitySpawnReason.NATURAL, pos, level.getRandom());
  }

  private static boolean isCatSpawnPosition(
      ServerLevel level,
      MorphDefinition definition,
      EntityType<?> entityType,
      BlockPos pos,
      boolean allBlackCat) {
    boolean structureMatch =
        allBlackCat
            ? level
                .structureManager()
                .getStructureWithPieceAt(pos, StructureTags.CATS_SPAWN_AS_BLACK)
                .isValid()
            : level.isCloseToVillage(pos, 2)
                || level
                    .structureManager()
                    .getStructureWithPieceAt(pos, StructureTags.CATS_SPAWN_IN)
                    .isValid();
    return noCollisionNoLiquid(level, pos)
        && level.noCollision(
            entityType.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))
        && MorphInitialStructures.biomeSupportsRequiredStructure(level.getBiome(pos), definition)
        && SpawnPlacements.isSpawnPositionOk(entityType, level, pos)
        && structureMatch;
  }

  private static boolean wolfVariantMatchesBiome(String variant, Holder<Biome> biome) {
    if (variant.endsWith(":spotted")) {
      return biome.is(BiomeTags.IS_SAVANNA);
    }
    if (variant.endsWith(":snowy")) {
      return biome.is(Biomes.GROVE);
    }
    if (variant.endsWith(":black")) {
      return biome.is(Biomes.OLD_GROWTH_PINE_TAIGA);
    }
    if (variant.endsWith(":ashen")) {
      return biome.is(Biomes.SNOWY_TAIGA);
    }
    if (variant.endsWith(":rusty")) {
      return biome.is(BiomeTags.IS_JUNGLE);
    }
    if (variant.endsWith(":woods")) {
      return biome.is(Biomes.FOREST);
    }
    if (variant.endsWith(":chestnut")) {
      return biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA);
    }
    if (variant.endsWith(":striped")) {
      return biome.is(BiomeTags.IS_BADLANDS);
    }
    return matchingSpawnerData(biome, EntityTypes.WOLF).isPresent();
  }

  private static boolean rabbitVariantSupportsBiome(
      Rabbit.Variant variant, Holder<Biome> biome, EntityType<?> entityType) {
    return switch (variant) {
      case WHITE, WHITE_SPLOTCHED -> biome.is(BiomeTags.SPAWNS_WHITE_RABBITS);
      case GOLD -> biome.is(BiomeTags.SPAWNS_GOLD_RABBITS);
      default -> matchingSpawnerData(biome, entityType).isPresent();
    };
  }

  private static Rabbit.Variant rabbitVariant(MorphDefinition definition) {
    if (definition.nbt().contains("RabbitType")) {
      return Rabbit.Variant.byId(
          definition.nbt().getIntOr("RabbitType", Rabbit.Variant.DEFAULT.id()));
    }
    return Rabbit.Variant.DEFAULT;
  }

  private static Optional<MobSpawnSettings.SpawnerData> matchingSpawnerData(
      Holder<Biome> biome, EntityType<?> entityType) {
    return biome.value().getMobSettings().getMobs(entityType.getCategory()).unwrap().stream()
        .map(Weighted::value)
        .filter(data -> data.type() == entityType)
        .findFirst();
  }

  private static void ensureNearbyGroup(
      ServerLevel level, BlockPos spawnPos, MorphDefinition definition, EntityType<?> entityType) {
    MobSpawnSettings.SpawnerData spawnerData =
        matchingSpawnerData(level.getBiome(spawnPos), entityType).orElse(null);
    if (spawnerData == null) {
      return;
    }

    AABB nearbyArea = new AABB(spawnPos).inflate(NEARBY_MOB_RADIUS);
    int existingCount = level.getEntities(entityType, nearbyArea, Entity::isAlive).size();
    int targetCount =
        Math.min(
            MAX_GROUP_SIZE,
            level
                .getRandom()
                .nextIntBetweenInclusive(spawnerData.minCount(), spawnerData.maxCount()));
    SpawnGroupData groupData = null;
    for (int index = existingCount; index < targetCount; index++) {
      SpawnResult result = spawnNearby(level, spawnPos, definition, entityType, groupData);
      if (result == null) {
        break;
      }
      groupData = result.groupData();
    }
  }

  private static SpawnResult spawnNearby(
      ServerLevel level,
      BlockPos center,
      MorphDefinition definition,
      EntityType<?> entityType,
      SpawnGroupData groupData) {
    for (int attempt = 0; attempt < SPAWN_ATTEMPTS_PER_MOB; attempt++) {
      int x = center.getX() + level.getRandom().nextIntBetweenInclusive(-12, 12);
      int z = center.getZ() + level.getRandom().nextIntBetweenInclusive(-12, 12);
      BlockPos pos =
          new BlockPos(x, level.getHeight(SpawnPlacements.getHeightmapType(entityType), x, z), z);
      pos = SpawnPlacements.getPlacementType(entityType).adjustSpawnPosition(level, pos);
      if (!biomeSupportsInitialSpawn(level.getBiome(pos), definition, entityType)
          || !SpawnPlacements.isSpawnPositionOk(entityType, level, pos)
          || !SpawnPlacements.checkSpawnRules(
              entityType, level, EntitySpawnReason.NATURAL, pos, level.getRandom())
          || !level.noCollision(
              entityType.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
        continue;
      }

      Entity entity = entityType.create(level, EntitySpawnReason.NATURAL);
      if (!(entity instanceof Mob mob)) {
        continue;
      }

      mob.snapTo(Vec3.atBottomCenterOf(pos), level.getRandom().nextFloat() * 360.0F, 0.0F);
      if (!mob.checkSpawnRules(level, EntitySpawnReason.NATURAL)
          || !mob.checkSpawnObstruction(level)) {
        mob.discard();
        continue;
      }

      SpawnGroupData nextGroupData =
          mob.finalizeSpawn(
              level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, groupData);
      level.addFreshEntityWithPassengers(mob);
      return new SpawnResult(nextGroupData);
    }
    return null;
  }

  private static boolean shouldSpawnNearbyGroup(MorphType morph) {
    return morph != MorphType.CAT && morph != MorphType.OCELOT && morph != MorphType.MULE;
  }

  private record SpawnResult(SpawnGroupData groupData) {}
}
