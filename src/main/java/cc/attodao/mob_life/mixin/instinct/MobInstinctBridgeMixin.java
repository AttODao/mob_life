package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobInstinctBridgeMixin {
  @Inject(method = "isEffectiveAi", at = @At("HEAD"), cancellable = true)
  private void mobLife$runShadowAi(CallbackInfoReturnable<Boolean> cir) {
    if (InstinctManager.isShadow((Mob) (Object) this)) {
      cir.setReturnValue(true);
    }
  }

  @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
  private void mobLife$redirectInstinctAttack(
      ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
    Boolean result = InstinctManager.attackFromShadow((Mob) (Object) this, level, target);
    if (result != null) {
      cir.setReturnValue(result);
    }
  }

  @Inject(method = "ate", at = @At("TAIL"))
  private void mobLife$applyInstinctBlockNutrition(CallbackInfo ci) {
    InstinctManager.shadowAte((Mob) (Object) this);
  }

  @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
  private void mobLife$rejectControllingPlayer(
      LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
    Boolean allowed = InstinctManager.allowsShadowTarget((Mob) (Object) this, target);
    if (allowed != null && !allowed) {
      cir.setReturnValue(false);
    }
  }
}
