package cc.attodao.mob_life.gameplay.instinct;

/** Admission rules for player input without allowing it to override a higher-priority instinct. */
final class InstinctPlayerIntervention {

  private InstinctPlayerIntervention() {}

  static boolean canTurnAtRest(InstinctState state, InstinctAction action) {
    return state == InstinctState.REST && action == InstinctAction.REST;
  }

  static boolean canRequestWander(
      InstinctState state, InstinctAction action, int promptedWanderTicks, boolean herdFollowing) {
    return (state == InstinctState.REST || state == InstinctState.LOOK)
        && action == InstinctAction.REST
        && promptedWanderTicks <= 0
        && !herdFollowing;
  }

  static boolean canContinueWander(
      InstinctState state, InstinctAction action, int promptedWanderTicks, boolean herdFollowing) {
    return (state == InstinctState.WANDER || promptedWanderTicks > 0)
        && (action == InstinctAction.REST || action == InstinctAction.WANDER)
        && !herdFollowing;
  }

  static boolean isOverridden(InstinctAction action) {
    return action != InstinctAction.REST && action != InstinctAction.WANDER;
  }
}
