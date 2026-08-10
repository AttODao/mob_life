package cc.attodao.mob_life.mixin.ability;

import cc.attodao.mob_life.gameplay.ability.MorphAbilityHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMorphAbilityMixin implements MorphAbilityHolder {
  @Unique private long mobLife$eggDay = Long.MIN_VALUE;
  @Unique private int mobLife$eggsLaidToday;

  @Override
  public long mobLife$getEggDay() {
    return mobLife$eggDay;
  }

  @Override
  public void mobLife$setEggDay(long day) {
    mobLife$eggDay = day;
  }

  @Override
  public int mobLife$getEggsLaidToday() {
    return mobLife$eggsLaidToday;
  }

  @Override
  public void mobLife$setEggsLaidToday(int count) {
    mobLife$eggsLaidToday = count;
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$readAbilityState(ValueInput input, CallbackInfo ci) {
    mobLife$eggDay = input.getLongOr("MobLifeEggDay", Long.MIN_VALUE);
    mobLife$eggsLaidToday = input.getIntOr("MobLifeEggsLaidToday", 0);
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$writeAbilityState(ValueOutput output, CallbackInfo ci) {
    output.putLong("MobLifeEggDay", mobLife$eggDay);
    output.putInt("MobLifeEggsLaidToday", mobLife$eggsLaidToday);
  }
}
