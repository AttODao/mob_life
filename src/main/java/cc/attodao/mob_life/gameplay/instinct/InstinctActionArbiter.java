package cc.attodao.mob_life.gameplay.instinct;

/**
 * Centralizes the action order so separate controllers cannot fight over navigation in one tick.
 */
final class InstinctActionArbiter {

  private InstinctActionArbiter() {}

  static InstinctAction select(
      boolean panicking,
      boolean fleeing,
      boolean retaliating,
      boolean eating,
      boolean hunting,
      boolean herding,
      boolean wandering) {
    if (panicking) {
      return InstinctAction.PANIC;
    }
    if (fleeing) {
      return InstinctAction.FLEE;
    }
    if (retaliating) {
      return InstinctAction.RETALIATE;
    }
    if (eating) {
      return InstinctAction.EAT;
    }
    if (hunting) {
      return InstinctAction.HUNT;
    }
    if (herding) {
      return InstinctAction.HERD;
    }
    return wandering ? InstinctAction.WANDER : InstinctAction.REST;
  }
}
