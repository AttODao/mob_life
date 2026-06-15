package cc.attodao.mob_life.gameplay.ability;

public interface MorphAbilityHolder {
  long mobLife$getFastSprintEndsAt();

  void mobLife$setFastSprintEndsAt(long gameTime);

  long mobLife$getFastSprintReadyAt();

  void mobLife$setFastSprintReadyAt(long gameTime);

  boolean mobLife$isFastSprintActive();

  void mobLife$setFastSprintActive(boolean active);

  long mobLife$getEggDay();

  void mobLife$setEggDay(long day);

  int mobLife$getEggsLaidToday();

  void mobLife$setEggsLaidToday(int count);
}
