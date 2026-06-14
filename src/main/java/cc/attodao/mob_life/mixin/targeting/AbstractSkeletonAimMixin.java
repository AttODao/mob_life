package cc.attodao.mob_life.mixin.targeting;

import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonAimMixin {
  @Redirect(
      method = "performRangedAttack",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getY(D)D"))
  private double mobLife$aimAtMorphHead(
      net.minecraft.world.entity.LivingEntity target, double relativeHeight) {
    if (target instanceof ServerPlayer && ServerMorphManager.hasMobForm()) {
      return target.getY() + ServerMorphManager.activeEyeHeight();
    }
    return target.getY(relativeHeight);
  }
}
