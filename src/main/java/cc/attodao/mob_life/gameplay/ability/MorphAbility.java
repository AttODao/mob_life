package cc.attodao.mob_life.gameplay.ability;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MorphAbility {
  private static final long DAY_TICKS = 24000L;
  public static final int EGG_FOOD_COST = 3;
  public static final int MAXIMUM_EGGS_PER_DAY = 1;

  private MorphAbility() {}

  public static void request(ServerPlayer player) {
    InstinctManager.recordActivity(player);
    if (!ServerMorphManager.hasMobForm() || !player.isAlive()) {
      return;
    }

    if (ServerMorphManager.activeConfig().abilities().value() == MorphConfig.Ability.EGG_LAYING) {
      layEgg(player);
    }
  }

  public static void copy(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
    MorphAbilityHolder oldHolder = holder(oldPlayer);
    MorphAbilityHolder newHolder = holder(newPlayer);
    newHolder.mobLife$setEggDay(oldHolder.mobLife$getEggDay());
    newHolder.mobLife$setEggsLaidToday(oldHolder.mobLife$getEggsLaidToday());
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

  private static MorphAbilityHolder holder(ServerPlayer player) {
    return (MorphAbilityHolder) player;
  }
}
