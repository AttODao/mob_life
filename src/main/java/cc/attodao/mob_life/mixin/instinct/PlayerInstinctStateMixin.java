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

  @Override
  public boolean mobLife$shouldRestoreInstinct() {
    return mobLife$restoreInstinct;
  }

  @Override
  public void mobLife$setRestoreInstinct(boolean restore) {
    mobLife$restoreInstinct = restore;
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$readInstinctState(ValueInput input, CallbackInfo ci) {
    mobLife$restoreInstinct = input.getBooleanOr("MobLifeInstinctEnabled", false);
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void mobLife$writeInstinctState(ValueOutput output, CallbackInfo ci) {
    output.putBoolean("MobLifeInstinctEnabled", mobLife$restoreInstinct);
  }
}
