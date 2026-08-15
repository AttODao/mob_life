package cc.attodao.mob_life.server;

/**
 * Ephemeral per-player state owned by {@link ServerMorphManager}. Nothing in this class is
 * persisted: reconnects, respawns, and morph changes must start from a clean runtime state.
 */
final class PlayerMorphRuntimeState {

  long jumpCooldownUntilTick;
  boolean jumpGrounded;
  boolean jumpGroundedKnown;
  int rabbitHopCooldown;
  boolean rabbitHopGrounded;
  boolean rabbitHopGroundedKnown;
  int ambientSoundTime;
  int normalGrassEatingTicks;
  long normalGrassEatingCooldownUntilTick;
  boolean bedlessSleepPending;
  Float lastSyncedAwkwardness;
  long damageAwkwardnessCooldownUntilTick;
}
