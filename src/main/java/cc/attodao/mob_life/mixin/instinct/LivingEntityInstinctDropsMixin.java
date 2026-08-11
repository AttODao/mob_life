package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityInstinctDropsMixin {
  @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
  private void mobLife$suppressConsumedMobDrops(
      ServerLevel level, DamageSource source, CallbackInfo ci) {
    if (InstinctManager.shouldSuppressDrops((LivingEntity) (Object) this)) {
      ci.cancel();
    }
  }
}
