package cc.attodao.mob_life.gameplay.instinct;

public interface InstinctPersistenceHolder {
  boolean mobLife$shouldRestoreInstinct();

  void mobLife$setRestoreInstinct(boolean restore);

  long mobLife$getPostKillHuntCooldownUntil();

  void mobLife$setPostKillHuntCooldownUntil(long gameTime);

  long mobLife$getAbandonedHuntCooldownUntil();

  void mobLife$setAbandonedHuntCooldownUntil(long gameTime);

  long mobLife$getEatBlockCooldownUntil();

  void mobLife$setEatBlockCooldownUntil(long gameTime);

  long mobLife$getRaidGardenCooldownUntil();

  void mobLife$setRaidGardenCooldownUntil(long gameTime);
}
