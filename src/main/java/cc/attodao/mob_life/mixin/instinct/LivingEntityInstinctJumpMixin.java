package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityInstinctJumpMixin {
  @Inject(method = "jumpFromGround", at = @At("TAIL"))
  private void mobLife$captureShadowJumpVelocity(CallbackInfo ci) {
    if ((Object) this instanceof Mob mob) {
      InstinctManager.shadowJumped(mob);
    }
  }
}
