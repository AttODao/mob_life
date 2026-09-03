package cc.attodao.mob_life.gameplay.instinct;

import net.minecraft.util.Mth;

public final class InstinctStateData {
  public static final float MAXIMUM = 100.0F;

  private boolean active;
  private float level;
  private int idleTicks;
  private int eggTimer;
  private boolean eggTimerInitialized;
  private long lastEggDay = Long.MIN_VALUE;
  private int loveTicks;
  private int breedingCooldown;

  public boolean active() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public float level() {
    return level;
  }

  public void setLevel(float level) {
    this.level = Mth.clamp(level, 0.0F, MAXIMUM);
  }

  public int idleTicks() {
    return idleTicks;
  }

  public void setIdleTicks(int idleTicks) {
    this.idleTicks = Math.max(0, idleTicks);
  }

  public int eggTimer() {
    return eggTimer;
  }

  public void setEggTimer(int eggTimer) {
    this.eggTimer = Math.max(0, eggTimer);
  }

  public boolean eggTimerInitialized() {
    return eggTimerInitialized;
  }

  public void setEggTimerInitialized(boolean eggTimerInitialized) {
    this.eggTimerInitialized = eggTimerInitialized;
  }

  public long lastEggDay() {
    return lastEggDay;
  }

  public void setLastEggDay(long lastEggDay) {
    this.lastEggDay = lastEggDay;
  }

  public int loveTicks() {
    return loveTicks;
  }

  public void setLoveTicks(int loveTicks) {
    this.loveTicks = Math.max(0, loveTicks);
  }

  public int breedingCooldown() {
    return breedingCooldown;
  }

  public void setBreedingCooldown(int breedingCooldown) {
    this.breedingCooldown = Math.max(0, breedingCooldown);
  }

  public void enter() {
    active = true;
    level = MAXIMUM;
  }

  public void exit() {
    active = false;
    level = 0.0F;
    idleTicks = 0;
  }

  public void clearForMorphChange() {
    exit();
    loveTicks = 0;
    breedingCooldown = 0;
  }

  public void copyEggStateFrom(InstinctStateData source) {
    eggTimer = source.eggTimer;
    eggTimerInitialized = source.eggTimerInitialized;
    lastEggDay = source.lastEggDay;
  }
}
