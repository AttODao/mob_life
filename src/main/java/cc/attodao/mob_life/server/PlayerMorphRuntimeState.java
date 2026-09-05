package cc.attodao.mob_life.server;

/**
 * Ephemeral per-player state owned by {@link ServerMorphManager}. Nothing in this class is
 * persisted: reconnects, respawns, and morph changes must start from a clean runtime state.
 */
final class PlayerMorphRuntimeState {

  int ambientSoundTime;
  int grassEatingTicks;
  long grassEatingCooldownUntilTick;
  boolean bedlessSleepPending;
  Float lastSyncedAwkwardness;
  long damageAwkwardnessCooldownUntilTick;
}
