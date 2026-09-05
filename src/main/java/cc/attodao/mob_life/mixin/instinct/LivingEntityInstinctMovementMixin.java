package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityInstinctMovementMixin {
  @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
  private void mobLife$leaveMovementToInstinctProxy(Vec3 input, CallbackInfo ci) {
    if ((Object) this instanceof ServerPlayer player && MorphInstinct.isActive(player)) {
      ci.cancel();
    }
  }
}
