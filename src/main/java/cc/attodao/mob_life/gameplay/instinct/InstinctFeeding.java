package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;

/** Feeding goal activation policy. Threat and retaliation responses preempt feeding. */
final class InstinctFeeding {

  private InstinctFeeding() {}

  static boolean shouldEnable(
      MorphConfig.FeedingAction feedingAction,
      boolean coolingDown,
      boolean eatingMeal,
      InstinctAction action,
      boolean needsFood) {
    return feedingAction.enabled()
        && !coolingDown
        && !eatingMeal
        && !action.blocksFeeding()
        && needsFood;
  }

  static boolean isEating(
      boolean eatingMeal, boolean eatBlockGoalRunning, boolean gardenGoalRunning) {
    return eatingMeal || eatBlockGoalRunning || gardenGoalRunning;
  }
}
