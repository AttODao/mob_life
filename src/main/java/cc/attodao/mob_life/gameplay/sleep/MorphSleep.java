package cc.attodao.mob_life.gameplay.sleep;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.server.ServerMorphManager;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MorphSleep {
  private static final long DAY_END_TICK = 12000L;
  private static final long DAY_LENGTH_TICKS = 24000L;

  private MorphSleep() {}

  public static void requestSleep(ServerPlayer player) {
    InstinctManager.recordActivity(player);
    if (!ServerMorphManager.hasMobForm()) {
      return;
    }
    if (player.isSleeping()) {
      player.stopSleepInBed(true, true);
      return;
    }
    MorphConfig.Sleep config = config();
    if (!config.withoutBed() || !MorphAwkwardness.canSleepWithoutBed(player)) {
      player.sendOverlayMessage(Component.translatable("mob_life.sleep.too_awkward"));
      return;
    }

    if (config.schedule() == MorphConfig.SleepSchedule.DAY && !isDaytime(player)) {
      player.sendOverlayMessage(Component.translatable("mob_life.sleep.nocturnal_night"));
      return;
    }
    if (config.schedule() == MorphConfig.SleepSchedule.NEVER) {
      player.sendOverlayMessage(Component.translatable("mob_life.sleep.not_possible"));
      return;
    }
    if (config.schedule() == MorphConfig.SleepSchedule.NORMAL) {
      var bedRule =
          player
              .level()
              .environmentAttributes()
              .getValue(EnvironmentAttributes.BED_RULE, player.position());
      if (!bedRule.canSleep(player.level())) {
        Component problem = bedRule.asProblem().message();
        player.sendOverlayMessage(
            problem != null ? problem : Component.translatable("mob_life.sleep.not_possible"));
        return;
      }
    }

    Optional<BlockPos> surface = findSoftSurface(player);
    if (surface.isEmpty()) {
      player.sendOverlayMessage(Component.translatable("mob_life.sleep.soft_surface_required"));
      return;
    }
    if (hasNearbyMonster(player, surface.get())) {
      player.sendOverlayMessage(Component.translatable("block.minecraft.bed.not_safe"));
      return;
    }

    BlockPos sleepPos = surface.get();
    BlockState state = player.level().getBlockState(sleepPos);
    double surfaceHeight = state.getCollisionShape(player.level(), sleepPos).max(Direction.Axis.Y);
    player.startSleeping(sleepPos);
    ServerMorphManager.markBedlessSleepStarted(player);
    player.setPos(
        sleepPos.getX() + 0.5, sleepPos.getY() + surfaceHeight + 0.1, sleepPos.getZ() + 0.5);
    player.level().updateSleepingPlayerList();
  }

  public static boolean isCustomSleep(Player player) {
    return MorphInventoryCapacity.hasMobForm(player)
        && player
            .getSleepingPos()
            .map(
                pos -> {
                  BlockState state = player.level().getBlockState(pos);
                  return !(state.getBlock() instanceof BedBlock) && isSoftSurface(state);
                })
            .orElse(false);
  }

  public static boolean isValidSleepingSurface(Player player) {
    return player
        .getSleepingPos()
        .map(
            pos -> {
              BlockState state = player.level().getBlockState(pos);
              return state.getBlock() instanceof BedBlock
                  || MorphInventoryCapacity.hasMobForm(player) && isSoftSurface(state);
            })
        .orElse(false);
  }

  public static boolean isDaytime(Player player) {
    long timeOfDay = Math.floorMod(player.level().getDefaultClockTime(), DAY_LENGTH_TICKS);
    return timeOfDay < DAY_END_TICK;
  }

  public static boolean isSoftSurface(BlockState state) {
    return state.is(Blocks.GRASS_BLOCK)
        || state.is(Blocks.HAY_BLOCK)
        || state.is(Blocks.MOSS_BLOCK)
        || state.is(BlockTags.WOOL)
        || state.is(BlockTags.WOOL_CARPETS)
        || state.is(BlockTags.BEDS);
  }

  private static Optional<BlockPos> findSoftSurface(Player player) {
    BlockPos current = player.blockPosition();
    if (isUsableSurface(player, current)) {
      return Optional.of(current);
    }

    BlockPos below = current.below();
    return isUsableSurface(player, below) ? Optional.of(below) : Optional.empty();
  }

  private static boolean isUsableSurface(Player player, BlockPos pos) {
    BlockState state = player.level().getBlockState(pos);
    return isSoftSurface(state) && !state.getCollisionShape(player.level(), pos).isEmpty();
  }

  private static boolean hasNearbyMonster(ServerPlayer player, BlockPos pos) {
    Vec3 center = Vec3.atBottomCenterOf(pos);
    AABB area =
        new AABB(
            center.x - 8.0,
            center.y - 5.0,
            center.z - 8.0,
            center.x + 8.0,
            center.y + 5.0,
            center.z + 8.0);
    return !player
        .level()
        .getEntitiesOfClass(
            Monster.class, area, monster -> monster.isPreventingPlayerRest(player.level(), player))
        .isEmpty();
  }

  public static int requiredSleepTicks() {
    return config().requiredTicks();
  }

  public static Player.BedSleepingProblem nocturnalBedProblem() {
    return new Player.BedSleepingProblem(Component.translatable("mob_life.sleep.nocturnal_night"));
  }

  // Nocturnal forms sleep through the day and should wake at night instead of dawn.
  public static ResourceKey<ClockTimeMarker> wakeUpMarker(ResourceKey<ClockTimeMarker> original) {
    if (original.equals(ClockTimeMarkers.WAKE_UP_FROM_SLEEP)
        && ServerMorphManager.hasMobForm()
        && ServerMorphManager.activeMorph().isNocturnal()) {
      return ClockTimeMarkers.NIGHT;
    }
    return original;
  }

  private static MorphConfig.Sleep config() {
    return MorphConfigManager.get(ServerMorphManager.activeMorph()).sleep();
  }
}
