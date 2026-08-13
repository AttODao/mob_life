package cc.attodao.mob_life.gameplay.instinct;

/**
 * One movement-owning action is selected per tick. The ordering mirrors the relevant vanilla goal
 * families: panic/avoid goals preempt retaliation, feeding, prey pursuit, social movement, and
 * stroll.
 */
enum InstinctAction {
  PANIC,
  FLEE,
  RETALIATE,
  EAT,
  HUNT,
  HERD,
  WANDER,
  REST;

  boolean isThreatResponse() {
    return this == PANIC || this == FLEE;
  }

  boolean blocksFeeding() {
    return isThreatResponse() || this == RETALIATE;
  }
}
