package cc.attodao.mob_life.gameplay.ability;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.movement.MorphMovementSpeed;
import cc.attodao.mob_life.network.MobLifeNetworking;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MorphAbility {
  private static final long DAY_TICKS = 24000L;
  public static final int FAST_SPRINT_DURATION_TICKS = 160;
  public static final int FAST_SPRINT_COOLDOWN_TICKS = 600;
  public static final float FAST_SPRINT_EXHAUSTION_MULTIPLIER = 4.0F;
  public static final int EGG_FOOD_COST = 3;
  public static final int MAXIMUM_EGGS_PER_DAY = 1;

  private MorphAbility() {}

  public static void request(ServerPlayer player) {
    if (!ServerMorphManager.hasMobForm() || !player.isAlive()) {
      return;
    }

    switch (ServerMorphManager.activeConfig().abilities().value()) {
      case EGG_LAYING -> layEgg(player);
      case FAST_SPRINT -> startFastSprint(player);
      default -> {}
    }
  }

  public static void tick(ServerPlayer player) {
    MorphAbilityHolder holder = holder(player);
    if (!holder.mobLife$isFastSprintActive()) {
      return;
    }

    long gameTime = player.level().getGameTime();
    if (ServerMorphManager.activeConfig().abilities().value() != MorphConfig.Ability.FAST_SPRINT
        || gameTime >= holder.mobLife$getFastSprintEndsAt()) {
      setFastSprintActive(player, false);
    }
  }

  public static boolean isFastSprintActive(net.minecraft.world.entity.player.Player player) {
    return ((MorphAbilityHolder) player).mobLife$isFastSprintActive();
  }

  public static void restore(ServerPlayer player) {
    MorphAbilityHolder holder = holder(player);
    boolean active =
        ServerMorphManager.hasMobForm()
            && ServerMorphManager.activeConfig().abilities().value()
                == MorphConfig.Ability.FAST_SPRINT
            && player.level().getGameTime() < holder.mobLife$getFastSprintEndsAt();
    setFastSprintActive(player, active);
  }

  public static void copy(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
    MorphAbilityHolder oldHolder = holder(oldPlayer);
    MorphAbilityHolder newHolder = holder(newPlayer);
    newHolder.mobLife$setFastSprintEndsAt(oldHolder.mobLife$getFastSprintEndsAt());
    newHolder.mobLife$setFastSprintReadyAt(oldHolder.mobLife$getFastSprintReadyAt());
    newHolder.mobLife$setEggDay(oldHolder.mobLife$getEggDay());
    newHolder.mobLife$setEggsLaidToday(oldHolder.mobLife$getEggsLaidToday());
  }

  public static void clearFastSprint(ServerPlayer player) {
    MorphAbilityHolder holder = holder(player);
    holder.mobLife$setFastSprintEndsAt(0L);
    holder.mobLife$setFastSprintReadyAt(0L);
    setFastSprintActive(player, false);
  }

  private static void startFastSprint(ServerPlayer player) {
    MorphAbilityHolder holder = holder(player);
    long gameTime = player.level().getGameTime();
    if (holder.mobLife$isFastSprintActive()) {
      return;
    }
    if (gameTime < holder.mobLife$getFastSprintReadyAt()) {
      player.sendOverlayMessage(Component.translatable("mob_life.ability.fast_sprint.cooldown"));
      return;
    }

    holder.mobLife$setFastSprintEndsAt(gameTime + FAST_SPRINT_DURATION_TICKS);
    holder.mobLife$setFastSprintReadyAt(
        gameTime + FAST_SPRINT_DURATION_TICKS + FAST_SPRINT_COOLDOWN_TICKS);
    setFastSprintActive(player, true);
  }

  private static void layEgg(ServerPlayer player) {
    MorphAbilityHolder holder = holder(player);
    long day = Math.floorDiv(player.level().getDefaultClockTime(), DAY_TICKS);
    if (holder.mobLife$getEggDay() != day) {
      holder.mobLife$setEggDay(day);
      holder.mobLife$setEggsLaidToday(0);
    }

    if (holder.mobLife$getEggsLaidToday() >= MAXIMUM_EGGS_PER_DAY) {
      player.sendOverlayMessage(Component.translatable("mob_life.ability.egg.daily_limit"));
      return;
    }

    FoodData food = player.getFoodData();
    if (food.getFoodLevel() < EGG_FOOD_COST) {
      player.sendOverlayMessage(
          Component.translatable("mob_life.ability.egg.not_enough_food", EGG_FOOD_COST));
      return;
    }

    int remainingFood = food.getFoodLevel() - EGG_FOOD_COST;
    food.setFoodLevel(remainingFood);
    food.setSaturation(Math.min(food.getSaturationLevel(), remainingFood));
    player.drop(new ItemStack(Items.EGG), true);
    player
        .level()
        .playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.CHICKEN_EGG,
            SoundSource.PLAYERS,
            1.0F,
            1.0F);
    holder.mobLife$setEggsLaidToday(holder.mobLife$getEggsLaidToday() + 1);
  }

  private static void setFastSprintActive(ServerPlayer player, boolean active) {
    MorphAbilityHolder holder = holder(player);
    if (holder.mobLife$isFastSprintActive() == active) {
      ServerPlayNetworking.send(player, new MobLifeNetworking.FastSprintStatePayload(active));
      return;
    }

    holder.mobLife$setFastSprintActive(active);
    MorphMovementSpeed.refresh(player);
    ServerPlayNetworking.send(player, new MobLifeNetworking.FastSprintStatePayload(active));
  }

  private static MorphAbilityHolder holder(ServerPlayer player) {
    return (MorphAbilityHolder) player;
  }
}
