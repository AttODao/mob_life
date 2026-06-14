package cc.attodao.mob_life.world;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MorphInitialSpawn {

  private static final int BIOME_SEARCH_RADIUS = 8192;
  private static final int HORIZONTAL_SAMPLE_RESOLUTION = 32;
  private static final int VERTICAL_SAMPLE_RESOLUTION = 64;
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
    Pair<BlockPos, Holder<Biome>> located =
        level.findClosestBiome3d(
            biome -> biomeSupports(biome, morphData.definition(), entityType),
            origin,
            BIOME_SEARCH_RADIUS,
            HORIZONTAL_SAMPLE_RESOLUTION,
            VERTICAL_SAMPLE_RESOLUTION);
    if (located == null) {
      MobLife.LOGGER.warn("Could not find a spawn biome for initial {} form", morph.id());
      morphData.markInitialSpawnConfigured();
      return;
    }

    BlockPos spawnPos;
    try {
      var spawnFuture = PlayerSpawnFinder.findSpawn(level, located.getFirst());
      level.getServer().managedBlock(spawnFuture::isDone);
      spawnPos = BlockPos.containing(spawnFuture.join());
    } catch (RuntimeException exception) {
      MobLife.LOGGER.warn("Could not find a safe initial spawn for {} form", morph.id(), exception);
      morphData.markInitialSpawnConfigured();
      return;
    }

    level.setRespawnData(LevelData.RespawnData.of(level.dimension(), spawnPos, 0.0F, 0.0F));
    MorphDefinition randomizedDefinition =
        MorphEntityFactory.randomizeAt(morphData.definition(), level, spawnPos);
    morphData.setDefinition(randomizedDefinition);
    ensureNearbyGroup(level, spawnPos, randomizedDefinition, entityType, located.getSecond());
    morphData.markInitialSpawnConfigured();
    MobLife.LOGGER.info("Initial {} spawn set to {} in a matching biome", morph.id(), spawnPos);
  }

  private static boolean biomeSupports(
      Holder<Biome> biome, MorphDefinition definition, EntityType<?> entityType) {
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
      return biome.is(Biomes.PLAINS) || biome.is(BiomeTags.IS_FOREST);
    }
    if (definition.type() == MorphType.MULE) {
      return biome.is(Biomes.PLAINS) || biome.is(BiomeTags.IS_SAVANNA);
    }
    return matchingSpawnerData(biome, entityType).isPresent();
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
    return matchingSpawnerData(biome, EntityType.WOLF).isPresent();
  }

  private static Optional<MobSpawnSettings.SpawnerData> matchingSpawnerData(
      Holder<Biome> biome, EntityType<?> entityType) {
    return biome.value().getMobSettings().getMobs(entityType.getCategory()).unwrap().stream()
        .map(Weighted::value)
        .filter(data -> data.type() == entityType)
        .findFirst();
  }

  private static void ensureNearbyGroup(
      ServerLevel level,
      BlockPos spawnPos,
      MorphDefinition definition,
      EntityType<?> entityType,
      Holder<Biome> spawnBiome) {
    MobSpawnSettings.SpawnerData spawnerData =
        matchingSpawnerData(spawnBiome, entityType).orElse(null);
    AABB nearbyArea = new AABB(spawnPos).inflate(NEARBY_MOB_RADIUS);
    int existingCount = level.getEntities(entityType, nearbyArea, Entity::isAlive).size();
    int targetCount =
        Math.min(MAX_GROUP_SIZE, spawnerData == null ? 2 : Math.max(1, spawnerData.minCount()));
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
      if (!biomeSupports(level.getBiome(pos), definition, entityType)
          || !SpawnPlacements.isSpawnPositionOk(entityType, level, pos)
          || !SpawnPlacements.checkSpawnRules(
              entityType, level, EntitySpawnReason.CHUNK_GENERATION, pos, level.getRandom())
          || !level.noCollision(
              entityType.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
        continue;
      }

      Entity entity = entityType.create(level, EntitySpawnReason.CHUNK_GENERATION);
      if (!(entity instanceof Mob mob)) {
        continue;
      }

      mob.snapTo(Vec3.atBottomCenterOf(pos), level.getRandom().nextFloat() * 360.0F, 0.0F);
      if (!mob.checkSpawnRules(level, EntitySpawnReason.CHUNK_GENERATION)
          || !mob.checkSpawnObstruction(level)) {
        mob.discard();
        continue;
      }

      SpawnGroupData nextGroupData =
          mob.finalizeSpawn(
              level,
              level.getCurrentDifficultyAt(pos),
              EntitySpawnReason.CHUNK_GENERATION,
              groupData);
      level.addFreshEntityWithPassengers(mob);
      return new SpawnResult(nextGroupData);
    }
    return null;
  }

  private record SpawnResult(SpawnGroupData groupData) {}
}
