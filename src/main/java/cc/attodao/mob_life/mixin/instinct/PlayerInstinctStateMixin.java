package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctPersistenceHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerInstinctStateMixin implements InstinctPersistenceHolder {
  @Unique private boolean mobLife$restoreInstinct;
  @Unique private long mobLife$postKillHuntCooldownUntil;
  @Unique private long mobLife$abandonedHuntCooldownUntil;
  @Unique private long mobLife$eatBlockCooldownUntil;
  @Unique private long mobLife$raidGardenCooldownUntil;

  @Override
  public boolean mobLife$shouldRestoreInstinct() {
    return mobLife$restoreInstinct;
  }

  @Override
  public void mobLife$setRestoreInstinct(boolean restore) {
    mobLife$restoreInstinct = restore;
  }

  @Override
  public long mobLife$getPostKillHuntCooldownUntil() {
    return mobLife$postKillHuntCooldownUntil;
  }

  @Override
  public void mobLife$setPostKillHuntCooldownUntil(long gameTime) {
    mobLife$postKillHuntCooldownUntil = gameTime;
  }

  @Override
  public long mobLife$getAbandonedHuntCooldownUntil() {
    return mobLife$abandonedHuntCooldownUntil;
  }

  @Override
  public void mobLife$setAbandonedHuntCooldownUntil(long gameTime) {
    mobLife$abandonedHuntCooldownUntil = gameTime;
  }

  @Override
  public long mobLife$getEatBlockCooldownUntil() {
    return mobLife$eatBlockCooldownUntil;
  }

  @Override
  public void mobLife$setEatBlockCooldownUntil(long gameTime) {
    mobLife$eatBlockCooldownUntil = gameTime;
  }

  @Override
  public long mobLife$getRaidGardenCooldownUntil() {
    return mobLife$raidGardenCooldownUntil;
  }

  @Override
  public void mobLife$setRaidGardenCooldownUntil(long gameTime) {
    mobLife$raidGardenCooldownUntil = gameTime;
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$readInstinctState(ValueInput input, CallbackInfo ci) {
    mobLife$restoreInstinct = input.getBooleanOr("MobLifeInstinctEnabled", false);
    mobLife$postKillHuntCooldownUntil = input.getLongOr("MobLifePostKillHuntCooldownUntil", 0L);
    mobLife$abandonedHuntCooldownUntil = input.getLongOr("MobLifeAbandonedHuntCooldownUntil", 0L);
    mobLife$eatBlockCooldownUntil = input.getLongOr("MobLifeEatBlockCooldownUntil", 0L);
    mobLife$raidGardenCooldownUntil = input.getLongOr("MobLifeRaidGardenCooldownUntil", 0L);
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$writeInstinctState(ValueOutput output, CallbackInfo ci) {
    output.putBoolean("MobLifeInstinctEnabled", mobLife$restoreInstinct);
    output.putLong("MobLifePostKillHuntCooldownUntil", mobLife$postKillHuntCooldownUntil);
    output.putLong("MobLifeAbandonedHuntCooldownUntil", mobLife$abandonedHuntCooldownUntil);
    output.putLong("MobLifeEatBlockCooldownUntil", mobLife$eatBlockCooldownUntil);
    output.putLong("MobLifeRaidGardenCooldownUntil", mobLife$raidGardenCooldownUntil);
  }
}
